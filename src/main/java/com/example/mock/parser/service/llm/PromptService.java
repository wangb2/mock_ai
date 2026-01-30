package com.example.mock.parser.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Map;

/**
 * 提示词服务
 * 从配置文件加载不同模型的提示词
 */
@Service
public class PromptService {
    
    private final ResourceLoader resourceLoader;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PromptService.class);
    
    private Map<String, Object> zhipuPrompts;
    private Map<String, Object> openaiPrompts;
    
    public PromptService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @PostConstruct
    public void init() {
        try {
            // 加载 Zhipu 提示词
            Resource zhipuResource = resourceLoader.getResource("classpath:zhipu-prompts.yml");
            if (zhipuResource.exists()) {
                try (InputStream is = zhipuResource.getInputStream()) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(is);
                    zhipuPrompts = (Map<String, Object>) ((Map<String, Object>) data.get("zhipu")).get("prompts");
                    logger.info("Loaded Zhipu prompts successfully");
                }
            } else {
                logger.warn("Zhipu prompts file not found: zhipu-prompts.yml");
            }
            
            // 加载 OpenAI 提示词
            Resource openaiResource = resourceLoader.getResource("classpath:openai-prompts.yml");
            if (openaiResource.exists()) {
                try (InputStream is = openaiResource.getInputStream()) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(is);
                    openaiPrompts = (Map<String, Object>) ((Map<String, Object>) data.get("openai")).get("prompts");
                    logger.info("Loaded OpenAI prompts successfully");
                }
            } else {
                logger.warn("OpenAI prompts file not found: openai-prompts.yml");
            }
        } catch (Exception e) {
            logger.error("Failed to load prompts from configuration files", e);
        }
    }
    
    /**
     * 获取 Zhipu 阶段一的 System Prompt
     */
    public String getZhipuPhaseASystem() {
        return getPrompt(zhipuPrompts, "phase-a-system", "");
    }
    
    /**
     * 获取 Zhipu 阶段一的 User Prompt（支持场景关键词替换）
     */
    public String getZhipuPhaseAUser(String sceneKeywords) {
        String prompt = getPrompt(zhipuPrompts, "phase-a-user", "");
        // 如果需要，可以在这里替换场景关键词相关的占位符
        return prompt;
    }
    
    /**
     * 获取 Zhipu 阶段二的 Prompt（支持识别文本替换）
     */
    public String getZhipuPhaseB(String recognitionText) {
        String prompt = getPrompt(zhipuPrompts, "phase-b", "");
        return prompt.replace("{recognitionText}", recognitionText != null ? recognitionText : "");
    }
    
    /**
     * 获取 OpenAI 阶段一的 Prompt
     */
    public String getOpenAIPhaseA() {
        return getPrompt(openaiPrompts, "phase-a", "");
    }
    
    /**
     * 获取 OpenAI 阶段二的 Prompt（支持识别文本替换）
     */
    public String getOpenAIPhaseB(String recognitionText) {
        String prompt = getPrompt(openaiPrompts, "phase-b", "");
        return prompt.replace("{recognitionText}", recognitionText != null ? recognitionText : "");
    }
    
    private String getPrompt(Map<String, Object> prompts, String key, String defaultValue) {
        if (prompts == null) {
            logger.warn("Prompts map is null for key: {}", key);
            return defaultValue;
        }
        Object value = prompts.get(key);
        if (value == null) {
            logger.warn("Prompt not found for key: {}", key);
            return defaultValue;
        }
        return value.toString();
    }
}
