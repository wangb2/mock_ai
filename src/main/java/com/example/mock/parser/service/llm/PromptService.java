package com.example.mock.parser.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        reload();
    }

    public synchronized void reload() {
        try {
            // 加载 Zhipu 提示词
            try (InputStream is = resolvePromptStream("zhipu-prompts.yml")) {
                if (is != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(is);
                    zhipuPrompts = (Map<String, Object>) ((Map<String, Object>) data.get("zhipu")).get("prompts");
                    logger.info("Loaded Zhipu prompts successfully");
                } else {
                    logger.warn("Zhipu prompts file not found: zhipu-prompts.yml");
                }
            }

            // 加载 OpenAI 提示词
            try (InputStream is = resolvePromptStream("openai-prompts.yml")) {
                if (is != null) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(is);
                    openaiPrompts = (Map<String, Object>) ((Map<String, Object>) data.get("openai")).get("prompts");
                    logger.info("Loaded OpenAI prompts successfully");
                } else {
                    logger.warn("OpenAI prompts file not found: openai-prompts.yml");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load prompts from configuration files", e);
        }
    }

    private InputStream resolvePromptStream(String filename) {
        try {
            Path cwd = Paths.get(System.getProperty("user.dir"));
            Path config = cwd.resolve("config").resolve(filename);
            if (Files.exists(config)) {
                return Files.newInputStream(config);
            }
            Path direct = cwd.resolve(filename);
            if (Files.exists(direct)) {
                return Files.newInputStream(direct);
            }
            Path src = cwd.resolve("src").resolve("main").resolve("resources").resolve(filename);
            if (Files.exists(src)) {
                return Files.newInputStream(src);
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve prompt file path: {}", filename, e);
        }
        try {
            Resource resource = resourceLoader.getResource("classpath:" + filename);
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (Exception e) {
            logger.warn("Failed to read classpath prompt file: {}", filename, e);
        }
        return null;
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
