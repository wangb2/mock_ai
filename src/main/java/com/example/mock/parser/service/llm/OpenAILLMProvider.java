package com.example.mock.parser.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI 模型提供者实现
 * 支持文件上传和两步调用
 */
@Component
public class OpenAILLMProvider implements LLMProvider {
    
    private static final String OPENAI_FILES_URL = "https://api.openai.com/v1/files";
    private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OpenAILLMProvider.class);
    
    @Value("${openai.api-key:}")
    private String apiKey;
    
    @Value("${openai.model:gpt-4.1}")
    private String model;
    
    @Value("${openai.temperature:0}")
    private int temperature;
    
    public OpenAILLMProvider(RestTemplate restTemplate, ObjectMapper objectMapper, PromptService promptService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }
    
    /**
     * 下载文件到临时目录
     */
    private Path downloadToTemp(String remoteFileUrl) throws IOException {
        // 注意：这里不需要 Authorization header，因为是从公网 URL 下载
        ResponseEntity<byte[]> response = restTemplate.getForEntity(remoteFileUrl, byte[].class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Download failed: HTTP " + response.getStatusCode() + ", url=" + remoteFileUrl);
        }
        
        String filename = guessFilename(remoteFileUrl, response.getHeaders().getFirst("content-disposition"));
        if (filename == null || filename.trim().isEmpty()) {
            filename = "document.bin";
        }
        
        Path tmp = Files.createTempFile("openai_doc_", "_" + sanitizeFilename(filename));
        Files.write(tmp, response.getBody(), StandardOpenOption.TRUNCATE_EXISTING);
        logger.info("OpenAILLMProvider: Downloaded file to temp: {}", tmp);
        return tmp;
    }
    
    /**
     * 上传文件到 OpenAI，返回 file_id
     */
    private String uploadToOpenAI(Path filePath) throws IOException {
        String boundary = "----JavaBoundary" + UUID.randomUUID();
        String filename = filePath.getFileName().toString();
        String contentType = guessContentType(filePath);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // purpose part
        writePart(baos, boundary, "Content-Disposition: form-data; name=\"purpose\"\r\n\r\nassistants");
        
        // file part
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(Files.readAllBytes(filePath));
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
        
        // end boundary
        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary));
        
        HttpEntity<byte[]> request = new HttpEntity<>(baos.toByteArray(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_FILES_URL, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Upload failed: HTTP " + response.getStatusCode() + "\n" + response.getBody());
        }
        
        String fileId = extractJsonString(response.getBody(), "id");
        if (fileId == null) {
            throw new IOException("Upload succeeded but file id not found. Response:\n" + response.getBody());
        }
        
        logger.info("OpenAILLMProvider: Uploaded file, fileId: {}", fileId);
        return fileId;
    }
    
    @Override
    public String callPhaseA(String fileUrl, String systemPrompt, String userPrompt, String sceneKeywords) throws IOException {
        // 如果 userPrompt 为空，从配置文件加载
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            userPrompt = promptService.getOpenAIPhaseA();
        }
        
        // OpenAI 需要先下载文件，然后上传到 OpenAI 获取 fileId
        Path downloaded = null;
        try {
            downloaded = downloadToTemp(fileUrl);
            String fileId = uploadToOpenAI(downloaded);
            
            // 调用 Responses API with file
            String json = buildResponsesJsonWithFile(model, fileId, userPrompt, temperature);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<String> request = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_RESPONSES_URL, request, String.class);
            
            logger.info("OpenAILLMProvider PhaseA Response - Status: {}", response.getStatusCode());
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IOException("OpenAI Responses API failed: HTTP " + response.getStatusCode() + "\n" + response.getBody());
            }
            
            String result = extractBestEffortText(response.getBody());
            logger.info("OpenAILLMProvider PhaseA Response Content Length: {}", result != null ? result.length() : 0);
            return result;
            
        } finally {
            if (downloaded != null) {
                try {
                    Files.deleteIfExists(downloaded);
                } catch (Exception e) {
                    logger.warn("Failed to delete temp file: {}", downloaded, e);
                }
            }
        }
    }
    
    @Override
    public String callPhaseB(String recognitionText, String prompt) throws IOException {
        // 如果 prompt 为空，从配置文件加载
        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = promptService.getOpenAIPhaseB(recognitionText);
        }
        
        // 调用 Responses API text-only
        String json = buildResponsesJsonTextOnly(model, prompt, temperature);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<String> request = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_RESPONSES_URL, request, String.class);
        
        logger.info("OpenAILLMProvider PhaseB Response - Status: {}", response.getStatusCode());
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("OpenAI Responses API failed: HTTP " + response.getStatusCode() + "\n" + response.getBody());
        }
        
        String result = extractBestEffortText(response.getBody());
        logger.info("OpenAILLMProvider PhaseB Response Content Length: {}", result != null ? result.length() : 0);
        return result;
    }
    
    @Override
    public String callChat(List<Object> messages) throws IOException {
        // OpenAI 的 Responses API 支持图片，但需要先上传图片到 OpenAI
        // 这里简化处理，假设图片已经上传到七牛云，直接使用 URL
        String json = buildResponsesJsonWithMessages(model, messages, temperature);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<String> request = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_RESPONSES_URL, request, String.class);
        
        logger.info("OpenAILLMProvider Chat Response - Status: {}", response.getStatusCode());
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("OpenAI Responses API failed: HTTP " + response.getStatusCode() + "\n" + response.getBody());
        }
        
        String result = extractBestEffortText(response.getBody());
        logger.info("OpenAILLMProvider Chat Response Content Length: {}", result != null ? result.length() : 0);
        return result;
    }
    
    @Override
    public String getProviderName() {
        return "OpenAI";
    }
    
    // =========================
    // Utility methods
    // =========================
    
    private static String buildResponsesJsonWithMessages(String model, List<Object> messages, int temperature) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(jsonEscape(model)).append("\",");
        sb.append("\"temperature\":").append(temperature).append(",");
        sb.append("\"input\":[");
        
        boolean first = true;
        for (Object msg : messages) {
            if (!first) sb.append(",");
            first = false;
            
            sb.append("{");
            sb.append("\"role\":\"user\",");
            sb.append("\"content\":[");
            
            if (msg instanceof String) {
                sb.append("{\"type\":\"text\",\"text\":\"").append(jsonEscape((String) msg)).append("\"}");
            } else if (msg instanceof com.fasterxml.jackson.databind.JsonNode) {
                com.fasterxml.jackson.databind.JsonNode contentNode = (com.fasterxml.jackson.databind.JsonNode) msg;
                if (contentNode.isArray()) {
                    boolean firstItem = true;
                    for (com.fasterxml.jackson.databind.JsonNode item : contentNode) {
                        if (!firstItem) sb.append(",");
                        firstItem = false;
                        
                        String type = item.path("type").asText("");
                        if ("text".equals(type)) {
                            sb.append("{\"type\":\"text\",\"text\":\"").append(jsonEscape(item.path("text").asText(""))).append("\"}");
                        } else if ("image_url".equals(type)) {
                            sb.append("{\"type\":\"input_file\",\"file_url\":\"");
                            com.fasterxml.jackson.databind.JsonNode imageUrlNode = item.path("image_url");
                            if (imageUrlNode.isMissingNode()) {
                                imageUrlNode = item.path("url");
                            }
                            String url = imageUrlNode.isTextual() ? imageUrlNode.asText() : 
                                         imageUrlNode.path("url").asText("");
                            sb.append(jsonEscape(url)).append("\"}");
                        }
                    }
                } else {
                    sb.append("{\"type\":\"text\",\"text\":\"").append(jsonEscape(contentNode.asText(""))).append("\"}");
                }
            }
            
            sb.append("]");
            sb.append("}");
        }
        
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }
    
    private static String buildResponsesJsonWithFile(String model, String fileId, String promptText, int temperature) {
        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"temperature\":" + temperature + ","
                + "\"input\":[{"
                + "\"role\":\"user\","
                + "\"content\":["
                + "{\"type\":\"input_file\",\"file_id\":\"" + jsonEscape(fileId) + "\"},"
                + "{\"type\":\"text\",\"text\":\"" + jsonEscape(promptText) + "\"}"
                + "]"
                + "}]"
                + "}";
    }
    
    private static String buildResponsesJsonTextOnly(String model, String promptText, int temperature) {
        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"temperature\":" + temperature + ","
                + "\"input\":[{"
                + "\"role\":\"user\","
                + "\"content\":["
                + "{\"type\":\"text\",\"text\":\"" + jsonEscape(promptText) + "\"}"
                + "]"
                + "}]"
                + "}";
    }
    
    private static void writePart(ByteArrayOutputStream baos, String boundary, String content) throws IOException {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(content.getBytes(StandardCharsets.UTF_8));
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
    
    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Best-effort extraction of model text from Responses API JSON
     */
    private static String extractBestEffortText(String responsesJson) {
        if (responsesJson == null) return "";
        
        // 1) Common shortcut: output_text field
        String out = extractJsonString(responsesJson, "output_text");
        if (out != null) return unescapeJsonString(out);
        
        // 2) Try to find "text":"..."
        List<String> texts = extractAllJsonStringValues(responsesJson, "text");
        if (!texts.isEmpty()) {
            texts.sort((a, b) -> Integer.compare(b.length(), a.length())); // 降序
            return unescapeJsonString(texts.get(0));
        }
        
        // 3) Fallback: return raw JSON
        return responsesJson;
    }
    
    private static String unescapeJsonString(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
    
    private static String extractJsonString(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
    private static List<String> extractAllJsonStringValues(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        Matcher m = p.matcher(json);
        List<String> out = new java.util.ArrayList<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }
    
    private static String guessContentType(Path filePath) {
        try {
            String probe = Files.probeContentType(filePath);
            if (probe != null) return probe;
        } catch (Exception ignored) {}
        
        String name = filePath.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".txt")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }
    
    private static String guessFilename(String url, String contentDisposition) {
        if (contentDisposition != null) {
            Pattern p = Pattern.compile("filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?");
            Matcher m = p.matcher(contentDisposition);
            if (m.find()) {
                String a = m.group(1);
                String b = m.group(2);
                String raw = (a != null) ? a : b;
                if (raw != null && !raw.trim().isEmpty()) return raw;
            }
        }
        try {
            java.net.URI u = java.net.URI.create(url);
            String path = u.getPath();
            if (path != null && path.contains("/")) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
