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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Zhipu AI 模型提供者实现
 */
@Component
public class ZhipuLLMProvider implements LLMProvider {
    
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ZhipuLLMProvider.class);
    
    @Value("${zhipu.api-key:}")
    private String apiKey;
    
    @Value("${zhipu.api-keys:}")
    private String apiKeys;
    
    @Value("${zhipu.file-model:glm-4.6v-flash}")
    private String fileModel;
    
    @Value("${zhipu.model:glm-4.7}")
    private String model;
    
    @Value("${zhipu.max-tokens:2048}")
    private int maxTokens;
    
    @Value("${zhipu.temperature:0.2}")
    private double temperature;
    
    // API key 轮询相关
    private List<String> apiKeyList = new ArrayList<>();
    private final AtomicInteger apiKeyIndex = new AtomicInteger(0);
    
    public ZhipuLLMProvider(RestTemplate restTemplate, ObjectMapper objectMapper, PromptService promptService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
        // 延迟初始化 API key 列表，只在真正使用时才检查
    }
    
    private void initApiKeyList() {
        // 如果已经初始化过，直接返回
        if (!apiKeyList.isEmpty()) {
            return;
        }
        
        if (apiKeys != null && !apiKeys.trim().isEmpty()) {
            String[] keys = apiKeys.split(",");
            for (String key : keys) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    apiKeyList.add(trimmed);
                }
            }
            logger.info("ZhipuLLMProvider: Initialized {} API keys for round-robin", apiKeyList.size());
        } else if (apiKey != null && !apiKey.trim().isEmpty()) {
            apiKeyList.add(apiKey.trim());
            logger.info("ZhipuLLMProvider: Using single API key (backward compatibility)");
        } else {
            logger.warn("ZhipuLLMProvider: No API key configured!");
        }
        
        // 不再在初始化时抛出异常，延迟到实际使用时检查
    }
    
    private String getNextApiKey() {
        // 延迟初始化
        initApiKeyList();
        
        if (apiKeyList.isEmpty()) {
            throw new IllegalStateException("No API key available");
        }
        int index = apiKeyIndex.getAndIncrement() % apiKeyList.size();
        String key = apiKeyList.get(index);
        logger.debug("ZhipuLLMProvider: Using API key index: {}/{}", index, apiKeyList.size());
        return key;
    }
    
    @Override
    public String callPhaseA(String fileUrl, String systemPrompt, String userPrompt, String sceneKeywords) throws IOException {
        // 如果 systemPrompt 和 userPrompt 为空，从配置文件加载
        if ((systemPrompt == null || systemPrompt.trim().isEmpty()) && 
            (userPrompt == null || userPrompt.trim().isEmpty())) {
            systemPrompt = promptService.getZhipuPhaseASystem();
            userPrompt = promptService.getZhipuPhaseAUser(sceneKeywords);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getNextApiKey());
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", fileModel);
        body.put("temperature", 0);
        ArrayNode messages = body.putArray("messages");
        
        // 添加system message
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
        }
        
        // 添加user message
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        
        // 构建多模态内容：文件URL + 文本提示
        ArrayNode contentArray = user.putArray("content");
        
        // 添加文件URL
        ObjectNode fileUrlContent = contentArray.addObject();
        fileUrlContent.put("type", "file_url");
        ObjectNode fileUrlObj = fileUrlContent.putObject("file_url");
        fileUrlObj.put("url", fileUrl);
        
        // 添加文本提示
        ObjectNode textContent = contentArray.addObject();
        textContent.put("type", "text");
        textContent.put("text", userPrompt);
        
        // 添加assistant message（用于引导输出格式）
        ObjectNode assistant = messages.addObject();
        assistant.put("role", "assistant");
        assistant.put("content", "请严格按照以下顺序执行：\n1. 先输出【阶段一：接口识别结果】（纯文本格式，按照要求的格式输出每个接口的详细信息）\n2. 阶段一完成后，必须立即输出【阶段二：JSON数组】（严格JSON格式，不要包含任何解释文字）\n3. 不得跳过阶段一，也不得在阶段一完成后停止输出\n4. 如果阶段一未识别到接口，阶段一说明原因，阶段二仅输出 []\n5. 阶段一和阶段二必须连续输出，中间不要有停顿\n\n现在开始执行【阶段一：接口识别】，完成后立即输出【阶段二：JSON数组】。");
        
        body.put("max_tokens", maxTokens);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "enabled");
        
        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("ZhipuLLMProvider PhaseA - URL: {}, Model: {}, FileUrl: {}", API_URL, fileModel, fileUrl);
        logger.debug("ZhipuLLMProvider PhaseA Request Body: {}", requestBody);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("ZhipuLLMProvider PhaseA Response - Status: {}", response.getStatusCode());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("ZhipuLLMProvider PhaseA error: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }
        
        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode choice = node.path("choices").path(0);
        JsonNode message = choice.path("message");
        
        String result = null;
        
        // 优先使用 message.content
        JsonNode content = message.path("content");
        if (!content.isMissingNode() && content.isTextual()) {
            String contentText = content.asText();
            if (contentText != null && !contentText.trim().isEmpty()) {
                result = contentText;
                logger.info("ZhipuLLMProvider PhaseA: Using message.content. Length: {}", result.length());
            }
        }
        
        // 如果 content 为空，尝试使用 reasoning_content
        if ((result == null || result.trim().isEmpty())) {
            JsonNode reasoningContent = message.path("reasoning_content");
            if (!reasoningContent.isMissingNode() && reasoningContent.isTextual()) {
                result = reasoningContent.asText();
                logger.info("ZhipuLLMProvider PhaseA: Using message.reasoning_content. Length: {}", result != null ? result.length() : 0);
            }
        }
        
        // 如果还是为空，尝试从 choice 对象中获取
        if ((result == null || result.trim().isEmpty())) {
            JsonNode choiceReasoning = choice.path("reasoning_content");
            if (!choiceReasoning.isMissingNode() && choiceReasoning.isTextual()) {
                result = choiceReasoning.asText();
                logger.info("ZhipuLLMProvider PhaseA: Using choice.reasoning_content. Length: {}", result != null ? result.length() : 0);
            }
        }
        
        // 如果还是为空，使用整个响应体
        if (result == null || result.trim().isEmpty()) {
            logger.warn("ZhipuLLMProvider PhaseA: Content and reasoning_content are both empty, using full response body");
            result = response.getBody();
        }
        
        logger.info("ZhipuLLMProvider PhaseA Response Content Length: {}", result != null ? result.length() : 0);
        return result;
    }
    
    @Override
    public String callPhaseB(String recognitionText, String prompt) throws IOException {
        // 如果 prompt 为空，从配置文件加载
        if (prompt == null || prompt.trim().isEmpty()) {
            prompt = promptService.getZhipuPhaseB(recognitionText);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getNextApiKey());
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);
        body.put("temperature", temperature);
        // 第二步需要更大的 max_tokens
        int jsonConversionMaxTokens = Math.max(maxTokens * 2, 4096);
        body.put("max_tokens", jsonConversionMaxTokens);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "disabled");
        
        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("ZhipuLLMProvider PhaseB - URL: {}, Model: {}, MaxTokens: {}", API_URL, model, jsonConversionMaxTokens);
        logger.debug("ZhipuLLMProvider PhaseB Request Prompt (first 500 chars): {}", 
                prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("ZhipuLLMProvider PhaseB Response - Status: {}", response.getStatusCode());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }
        
        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode choice = node.path("choices").path(0);
        String finishReason = choice.path("finish_reason").asText("");
        JsonNode content = choice.path("message").path("content");
        String result = content.isMissingNode() ? response.getBody() : content.asText();
        
        logger.info("ZhipuLLMProvider PhaseB Response - FinishReason: {}, Content Length: {}", 
                finishReason, result != null ? result.length() : 0);
        
        if ("length".equals(finishReason)) {
            logger.warn("ZhipuLLMProvider PhaseB: Response was truncated due to max_tokens limit.");
        }
        
        return result;
    }
    
    @Override
    public String callChat(List<Object> messages) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getNextApiKey());
        
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", fileModel); // 使用支持图片的模型
        ArrayNode messagesArray = body.putArray("messages");
        
        // 处理消息列表
        for (Object msg : messages) {
            ObjectNode messageNode = messagesArray.addObject();
            if (msg instanceof String) {
                // 纯文本消息
                messageNode.put("role", "user");
                messageNode.put("content", (String) msg);
            } else if (msg instanceof com.fasterxml.jackson.databind.JsonNode) {
                // 多模态消息（文本+图片）
                com.fasterxml.jackson.databind.JsonNode contentNode = (com.fasterxml.jackson.databind.JsonNode) msg;
                messageNode.put("role", "user");
                if (contentNode.isArray()) {
                    // content 是数组，包含 text 和 image_url
                    ArrayNode contentArray = messageNode.putArray("content");
                    for (com.fasterxml.jackson.databind.JsonNode item : contentNode) {
                        ObjectNode contentItem = contentArray.addObject();
                        String type = item.path("type").asText("");
                        if ("text".equals(type)) {
                            contentItem.put("type", "text");
                            contentItem.put("text", item.path("text").asText(""));
                        } else if ("image_url".equals(type)) {
                            contentItem.put("type", "image_url");
                            ObjectNode imageUrl = contentItem.putObject("image_url");
                            // 支持两种格式：image_url.url 或 image_url.image_url.url
                            com.fasterxml.jackson.databind.JsonNode imageUrlNode = item.path("image_url");
                            if (imageUrlNode.isMissingNode()) {
                                imageUrlNode = item.path("url");
                            }
                            String url = imageUrlNode.isTextual() ? imageUrlNode.asText() : 
                                         imageUrlNode.path("url").asText("");
                            imageUrl.put("url", url);
                        }
                    }
                } else {
                    // 如果不是数组，尝试作为文本处理
                    messageNode.put("content", contentNode.asText(""));
                }
            }
        }
        
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "enabled");
        
        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("ZhipuLLMProvider Chat - URL: {}, Model: {}, Messages: {}", API_URL, fileModel, messagesArray.size());
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("ZhipuLLMProvider Chat Response - Status: {}", response.getStatusCode());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }
        
        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode choice = node.path("choices").path(0);
        JsonNode message = choice.path("message");
        JsonNode content = message.path("content");
        String result = content.isMissingNode() ? response.getBody() : content.asText();
        
        logger.info("ZhipuLLMProvider Chat Response Content Length: {}", result != null ? result.length() : 0);
        return result;
    }
    
    @Override
    public String getProviderName() {
        return "Zhipu";
    }
}
