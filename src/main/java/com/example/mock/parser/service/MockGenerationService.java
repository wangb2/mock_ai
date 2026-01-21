package com.example.mock.parser.service;

import com.example.mock.parser.model.MockItem;
import com.example.mock.parser.model.MockResult;
import com.example.mock.parser.model.ParsedDocument;
import com.example.mock.parser.model.Section;
import com.example.mock.parser.model.TableData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class MockGenerationService implements InitializingBean {
    private static final String API_URL = "https://open.bigmodel.cn/api/coding/paas/v4/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${filter.keywords:}")
    private String keywords;

    @Value("${filter.url-regex:(https?://|/)[\\w\\-./:?&=%#]+}")
    private String urlRegex;

    private List<String> keywordList = new ArrayList<>();
    private Pattern urlPattern = Pattern.compile("(https?://|/)[\\w\\-./:?&=%#]+");

    public MockGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() {
        keywordList = splitKeywords(keywords);
        urlPattern = Pattern.compile(urlRegex);
    }

    public MockResult generate(ParsedDocument parsedDocument) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing zhipu.api-key");
        }

        List<Chunk> chunks = chunk(parsedDocument);
        MockResult result = new MockResult();
        result.setOutputFile(new File("mock.json").getAbsolutePath());

        for (Chunk chunk : chunks) {
            String prompt = buildPrompt(chunk.text.toString());
            String raw = callZhipu(prompt);
            MockItem item = new MockItem();
            item.setTitle(chunk.title);
            item.setRaw(raw);
            item.setMock(parseJsonSafely(raw));
            result.getItems().add(item);
        }

        writeOutput(result);
        return result;
    }

    private void writeOutput(MockResult result) throws IOException {
        File outFile = new File("mock.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outFile, result);
    }

    private JsonNode parseJsonSafely(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        } catch (IOException ex) {
            return null;
        }
    }

    private String callZhipu(String prompt) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "glm-4.7");
        ArrayNode messages = body.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);
        body.put("temperature", 0.2);
        body.put("max_tokens", 512);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "disabled");

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode content = node.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() ? response.getBody() : content.asText();
    }

    private String buildPrompt(String chunkText) {
        return "你是接口Mock数据生成助手。请根据输入的解析结果生成一份可用的Mock响应。\n"
                + "要求：\n"
                + "1. 只能输出严格JSON，不要包含任何解释文字。\n"
                + "2. 优先使用解析结果中的“Sample Response”或示例值。\n"
                + "3. 若没有示例值，按字段语义生成合理示例。\n"
                + "4. 数据结构必须与接口真实响应一致（嵌套字段保持一致）。\n"
                + "5. 若出现字段同义（如 SERIALNO / SERIAL_NO），以样例中的字段名为准。\n"
                + "6. 字段类型保持合理（数字/字符串/对象/数组）。\n"
                + "7. 输出字段顺序尽量与文档一致。\n\n"
                + "输入（解析结果片段）：\n"
                + chunkText;
    }

    private List<Chunk> chunk(ParsedDocument parsedDocument) {
        List<Chunk> chunks = new ArrayList<>();
        if (parsedDocument == null || parsedDocument.getSections() == null) {
            return chunks;
        }

        Chunk current = null;
        for (Section section : parsedDocument.getSections()) {
            String title = safe(section.getTitle());
            boolean isNewApi = isApiSection(section);
            if (isNewApi) {
                if (current != null) {
                    chunks.add(current);
                }
                current = new Chunk(title);
            }
            String formatted = formatSectionIfRelevant(section);
            if (formatted.isEmpty()) {
                continue;
            }
            if (current == null) {
                current = new Chunk(title.isEmpty() ? "Document" : title);
            }
            current.text.append(formatted).append("\n");
        }
        if (current != null) {
            chunks.add(current);
        }

        return chunks;
    }

    private boolean isApiSection(Section section) {
        String title = safe(section.getTitle()).toLowerCase(Locale.ROOT);
        return section.getLevel() <= 1 && (title.contains("api") || title.contains("接口"));
    }

    private String formatSectionIfRelevant(Section section) {
        if (!isRelevantSection(section)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (section.getTitle() != null && !section.getTitle().trim().isEmpty()) {
            builder.append("# ").append(section.getTitle().trim()).append("\n");
        }
        if (section.getContent() != null && !section.getContent().trim().isEmpty()) {
            builder.append(section.getContent().trim()).append("\n");
        }
        if (section.getTables() != null && !section.getTables().isEmpty()) {
            for (TableData table : section.getTables()) {
                builder.append(formatTable(table)).append("\n");
            }
        }
        return builder.toString();
    }

    private boolean isRelevantSection(Section section) {
        String title = safe(section.getTitle()).toLowerCase(Locale.ROOT);
        String content = safe(section.getContent()).toLowerCase(Locale.ROOT);
        if (isNoiseSection(title, content)) {
            return false;
        }
        if (containsKeywords(title) || containsKeywords(content)) {
            return true;
        }
        if (containsUrl(content) || containsUrl(title)) {
            return true;
        }
        if (containsRequestResponseExample(content) || containsRequestResponseExample(title)) {
            return true;
        }
        if (section.getTables() != null) {
            for (TableData table : section.getTables()) {
                if (containsKeywordsFromTable(table) || containsRequestResponseExampleFromTable(table)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNoiseSection(String title, String content) {
        return title.contains("document history")
                || title.contains("revision")
                || title.contains("distribution")
                || title.contains("approval")
                || title.contains("table of contents")
                || title.contains("content")
                || content.contains("document history")
                || content.contains("revision")
                || content.contains("distribution")
                || content.contains("approval")
                || content.contains("table of contents")
                || content.contains("content ................................");
    }

    private boolean containsKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String keyword : keywordList) {
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsUrl(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return urlPattern.matcher(text).find();
    }

    private boolean containsRequestResponseExample(String text) {
        return text.contains("request")
                || text.contains("response")
                || text.contains("sample")
                || text.contains("example")
                || text.contains("请求")
                || text.contains("响应")
                || text.contains("示例");
    }

    private boolean containsKeywordsFromTable(TableData table) {
        if (table.getHeaders() != null) {
            for (String header : table.getHeaders()) {
                if (containsKeywords(safe(header).toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsRequestResponseExampleFromTable(TableData table) {
        if (table.getHeaders() != null) {
            for (String header : table.getHeaders()) {
                String h = safe(header).toLowerCase(Locale.ROOT);
                if (containsRequestResponseExample(h)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> splitKeywords(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return list;
        }
        for (String token : raw.split(",")) {
            String value = token.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                list.add(value);
            }
        }
        return list;
    }

    private String formatTable(TableData table) {
        StringBuilder builder = new StringBuilder();
        if (table.getHeaders() != null && !table.getHeaders().isEmpty()) {
            builder.append(String.join(" | ", table.getHeaders())).append("\n");
        }
        if (table.getRows() != null) {
            for (List<String> row : table.getRows()) {
                builder.append(String.join(" | ", row)).append("\n");
            }
        }
        return builder.toString();
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static class Chunk {
        private final String title;
        private final StringBuilder text = new StringBuilder();

        private Chunk(String title) {
            this.title = title == null ? "Document" : title;
        }
    }
}
