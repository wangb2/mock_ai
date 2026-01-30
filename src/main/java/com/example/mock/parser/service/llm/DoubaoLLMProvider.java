package com.example.mock.parser.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

/**
 * 豆包（Doubao）模型提供者实现
 * 使用火山引擎的 ARK API
 */
@Component
public class DoubaoLLMProvider implements LLMProvider {
    
    private static final String ARK_RESPONSES_URL = "https://ark.cn-beijing.volces.com/api/v3/responses";
    private static final String ARK_CHAT_COMPLETIONS_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DoubaoLLMProvider.class);
    
    @Value("${doubao.api-key:}")
    private String apiKey;
    
    @Value("${doubao.phase-a-model:doubao-seed-1-6-251015}")
    private String phaseAModel;
    
    @Value("${doubao.phase-b-model:doubao-1-5-pro-32k-250115}")
    private String phaseBModel;
    
    @Value("${doubao.vision-model:doubao-seed-1-6-vision-250815}")
    private String visionModel;
    
    @Value("${doubao.temperature:0}")
    private double temperature;
    
    public DoubaoLLMProvider(RestTemplate restTemplate, ObjectMapper objectMapper, PromptService promptService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }
    
    @Override
    public String callPhaseA(String fileUrl, String systemPrompt, String userPrompt, String sceneKeywords) throws IOException {
        logger.info("    [Doubao PhaseA] 开始调用豆包 Responses API");
        logger.info("    [Doubao PhaseA] 参数: fileUrl={}, sceneKeywords={}, hasSystemPrompt={}, hasUserPrompt={}", 
                fileUrl, sceneKeywords, systemPrompt != null, userPrompt != null);
        
        // 如果 userPrompt 为空，从配置文件加载（使用 Zhipu 的提示词格式）
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            logger.info("    [Doubao PhaseA] 从配置文件加载提示词");
            // 豆包第一步使用 input_text，所以需要合并 system 和 user prompt
            String system = promptService.getZhipuPhaseASystem();
            String user = promptService.getZhipuPhaseAUser(sceneKeywords);
            // 合并为完整的用户提示词
            userPrompt = system + "\n\n" + user;
            logger.info("    [Doubao PhaseA] 提示词加载完成. systemLength={}, userLength={}, totalLength={}", 
                    system.length(), user.length(), userPrompt.length());
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", phaseAModel);
        
        // 构建 input 数组（根据官方示例，input 数组中的每个元素包含 role 和 content）
        ArrayNode inputArray = body.putArray("input");
        
        // 添加 user message 对象
        ObjectNode userMessage = inputArray.addObject();
        userMessage.put("role", "user");
        
        // 构建 content 数组
        ArrayNode contentArray = userMessage.putArray("content");
        
        // 添加文件 URL（根据官方示例，type 是 "input_file"，file_url 是字符串）
        ObjectNode fileContent = contentArray.addObject();
        fileContent.put("type", "input_file");
        fileContent.put("file_url", fileUrl);  // file_url 是字符串，不是对象
        
        // 添加文本提示（根据官方示例，type 是 "input_text"）
        ObjectNode textContent = contentArray.addObject();
        textContent.put("type", "input_text");
        textContent.put("text", userPrompt);
        
        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("    [Doubao PhaseA] 请求准备完成. URL={}, Model={}, FileUrl={}, RequestBodyLength={}", 
                ARK_RESPONSES_URL, phaseAModel, fileUrl, requestBody.length());
        logger.debug("    [Doubao PhaseA] Request Body: {}", requestBody);
        
        long startTime = System.currentTimeMillis();
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(ARK_RESPONSES_URL, request, String.class);
        long endTime = System.currentTimeMillis();
        
        logger.info("    [Doubao PhaseA] API 调用完成. Status={}, 耗时={}ms, ResponseBodyLength={}", 
                response.getStatusCode(), endTime - startTime, 
                response.getBody() != null ? response.getBody().length() : 0);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("    [Doubao PhaseA] API 调用失败. status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Doubao API error: " + response.getStatusCode());
        }
        
        // 提取响应文本
        logger.info("    [Doubao PhaseA] 开始提取响应文本");
        String result = extractResponseText(response.getBody());
        logger.info("    [Doubao PhaseA] 响应提取完成. resultLength={}", result != null ? result.length() : 0);
        if (result != null && result.length() > 500) {
            logger.info("    [Doubao PhaseA] 响应预览（前500字符）: {}", result.substring(0, 500) + "...");
        } else {
            logger.info("    [Doubao PhaseA] 响应内容: {}", result);
        }
        return result;
    }
    
    @Override
    public String callPhaseB(String recognitionText, String prompt) throws IOException {
        logger.info("    [Doubao PhaseB] 开始调用豆包 Chat Completions API");
        logger.info("    [Doubao PhaseB] 参数: recognitionTextLength={}, hasPrompt={}", 
                recognitionText != null ? recognitionText.length() : 0, prompt != null);
        
        // 如果 prompt 为空，从配置文件加载（使用 Zhipu 的提示词格式）
        if (prompt == null || prompt.trim().isEmpty()) {
            logger.info("    [Doubao PhaseB] 从配置文件加载提示词");
            prompt = promptService.getZhipuPhaseB(recognitionText);
            logger.info("    [Doubao PhaseB] 提示词加载完成. promptLength={}", prompt.length());
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", phaseBModel);
        body.put("temperature", temperature);
        
        // 构建 messages 数组
        ArrayNode messagesArray = body.putArray("messages");
        
        // 添加 system message（可选）
        ObjectNode systemMsg = messagesArray.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个接口Mock数据生成助手。请根据接口识别结果生成标准的JSON数组。");
        
        // 添加 user message
        ObjectNode userMsg = messagesArray.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        
        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("    [Doubao PhaseB] 请求准备完成. URL={}, Model={}, RequestBodyLength={}", 
                ARK_CHAT_COMPLETIONS_URL, phaseBModel, requestBody.length());
        logger.debug("    [Doubao PhaseB] Request Prompt (first 500 chars): {}", 
                prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);
        
        long startTime = System.currentTimeMillis();
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(ARK_CHAT_COMPLETIONS_URL, request, String.class);
        long endTime = System.currentTimeMillis();
        
        logger.info("    [Doubao PhaseB] API 调用完成. Status={}, 耗时={}ms, ResponseBodyLength={}", 
                response.getStatusCode(), endTime - startTime, 
                response.getBody() != null ? response.getBody().length() : 0);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("    [Doubao PhaseB] API 调用失败. status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Doubao API error: " + response.getStatusCode());
        }
        
        // 提取响应文本
        logger.info("    [Doubao PhaseB] 开始提取响应文本");
        String result = extractChatResponseText(response.getBody());
        logger.info("    [Doubao PhaseB] 响应提取完成. resultLength={}", result != null ? result.length() : 0);
        if (result != null && result.length() > 500) {
            logger.info("    [Doubao PhaseB] 响应预览（前500字符）: {}", result.substring(0, 500) + "...");
        } else {
            logger.info("    [Doubao PhaseB] 响应内容: {}", result);
        }
        return result;
    }
    
    @Override
    public String callChat(List<Object> messages) throws IOException {
        logger.info("    [Doubao Chat] 开始调用豆包 Responses API（图片识别）");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", visionModel); // 使用支持图片的 vision 模型
        
        // 构建 input 数组
        ArrayNode inputArray = body.putArray("input");
        ObjectNode userMessage = inputArray.addObject();
        userMessage.put("role", "user");
        ArrayNode contentArray = userMessage.putArray("content");
        
        // 处理消息列表
        for (Object msg : messages) {
            if (msg instanceof String) {
                // 纯文本消息
                ObjectNode textContent = contentArray.addObject();
                textContent.put("type", "input_text");
                textContent.put("text", (String) msg);
            } else if (msg instanceof com.fasterxml.jackson.databind.JsonNode) {
                // 多模态消息（文本+图片）
                com.fasterxml.jackson.databind.JsonNode contentNode = (com.fasterxml.jackson.databind.JsonNode) msg;
                if (contentNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : contentNode) {
                        String type = item.path("type").asText("");
                        if ("text".equals(type)) {
                            ObjectNode textContent = contentArray.addObject();
                            textContent.put("type", "input_text");
                            textContent.put("text", item.path("text").asText(""));
                        } else if ("image_url".equals(type)) {
                            ObjectNode imageContent = contentArray.addObject();
                            imageContent.put("type", "input_image");
                            // 支持两种格式：image_url.url 或 image_url.image_url.url
                            com.fasterxml.jackson.databind.JsonNode imageUrlNode = item.path("image_url");
                            if (imageUrlNode.isMissingNode()) {
                                imageUrlNode = item.path("url");
                            }
                            String url = imageUrlNode.isTextual() ? imageUrlNode.asText() : 
                                         imageUrlNode.path("url").asText("");
                            imageContent.put("image_url", url);
                        }
                    }
                } else {
                    // 如果不是数组，尝试作为文本处理
                    ObjectNode textContent = contentArray.addObject();
                    textContent.put("type", "input_text");
                    textContent.put("text", contentNode.asText(""));
                }
            }
        }
        
        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("    [Doubao Chat] 请求准备完成. URL={}, Model={}, RequestBodyLength={}", 
                ARK_RESPONSES_URL, phaseAModel, requestBody.length());
        
        long startTime = System.currentTimeMillis();
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(ARK_RESPONSES_URL, request, String.class);
        long endTime = System.currentTimeMillis();
        
        logger.info("    [Doubao Chat] API 调用完成. Status={}, 耗时={}ms, ResponseBodyLength={}", 
                response.getStatusCode(), endTime - startTime, 
                response.getBody() != null ? response.getBody().length() : 0);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("    [Doubao Chat] API 调用失败. status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Doubao API error: " + response.getStatusCode());
        }
        
        String result = extractResponseText(response.getBody());
        logger.info("    [Doubao Chat] 响应提取完成. resultLength={}", result != null ? result.length() : 0);
        return result;
    }
    
    @Override
    public String getProviderName() {
        return "Doubao";
    }
    
    /**
     * 从 Responses API 响应中提取文本
     */
    private String extractResponseText(String responseBody) throws IOException {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            // 尝试多种可能的字段路径
            JsonNode outputText = node.path("output_text");
            if (!outputText.isMissingNode() && outputText.isTextual()) {
                return outputText.asText();
            }
            
            // 尝试 choices[0].message.content
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode message = choice.path("message");
                JsonNode content = message.path("content");
                if (!content.isMissingNode() && content.isTextual()) {
                    return content.asText();
                }
            }
            
            // 尝试直接查找 text 字段
            JsonNode text = node.path("text");
            if (!text.isMissingNode() && text.isTextual()) {
                return text.asText();
            }
            
            // 如果都找不到，返回原始响应（可能是 JSON 字符串）
            logger.warn("DoubaoLLMProvider: Could not extract text from response, returning raw body");
            return responseBody;
        } catch (Exception e) {
            logger.error("DoubaoLLMProvider: Failed to parse response JSON", e);
            return responseBody;
        }
    }
    
    /**
     * 从 Chat Completions API 响应中提取文本
     */
    private String extractChatResponseText(String responseBody) throws IOException {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode choices = node.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode choice = choices.get(0);
                JsonNode message = choice.path("message");
                JsonNode content = message.path("content");
                if (!content.isMissingNode() && content.isTextual()) {
                    return content.asText();
                }
            }
            
            logger.warn("DoubaoLLMProvider: Could not extract text from chat response, returning raw body");
            return responseBody;
        } catch (Exception e) {
            logger.error("DoubaoLLMProvider: Failed to parse chat response JSON", e);
            return responseBody;
        }
    }
}
