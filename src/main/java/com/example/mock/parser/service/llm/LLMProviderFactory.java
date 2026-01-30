package com.example.mock.parser.service.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * LLM 提供者工厂
 * 根据配置选择对应的模型提供者
 */
@Component
public class LLMProviderFactory {
    
    @Value("${llm.provider:zhipu}")
    private String providerName;
    
    @Autowired
    private ZhipuLLMProvider zhipuLLMProvider;
    
    @Autowired
    private OpenAILLMProvider openAILLMProvider;
    
    @Autowired
    private DoubaoLLMProvider doubaoLLMProvider;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LLMProviderFactory.class);
    
    @PostConstruct
    public void init() {
        logger.info("LLMProviderFactory initialized. Current provider: {}", providerName);
    }
    
    /**
     * 获取当前配置的 LLM 提供者
     */
    public LLMProvider getProvider() {
        String name = providerName != null ? providerName.toLowerCase().trim() : "zhipu";
        
        switch (name) {
            case "zhipu":
            case "glm":
                logger.debug("Using Zhipu LLM Provider");
                return zhipuLLMProvider;
            case "openai":
            case "gpt":
                logger.debug("Using OpenAI LLM Provider");
                return openAILLMProvider;
            case "doubao":
            case "ark":
                logger.debug("Using Doubao LLM Provider");
                return doubaoLLMProvider;
            default:
                logger.warn("Unknown provider: {}, falling back to Zhipu", providerName);
                return zhipuLLMProvider;
        }
    }
    
    /**
     * 获取指定名称的提供者（用于测试或特殊场景）
     */
    public LLMProvider getProvider(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getProvider();
        }
        
        String lower = name.toLowerCase().trim();
        switch (lower) {
            case "zhipu":
            case "glm":
                return zhipuLLMProvider;
            case "openai":
            case "gpt":
                return openAILLMProvider;
            case "doubao":
            case "ark":
                return doubaoLLMProvider;
            default:
                logger.warn("Unknown provider: {}, falling back to default", name);
                return getProvider();
        }
    }
}
