package com.example.mock.parser.service.llm;

import java.io.IOException;
import java.util.List;

/**
 * LLM 模型提供者接口
 * 统一不同模型（Zhipu、OpenAI等）的调用方式
 */
public interface LLMProvider {
    
    /**
     * 阶段一：使用文件提取 API 信息（文本格式）
     * @param fileUrl 文件URL（对于 OpenAI，需要先上传文件获取 fileId）
     * @param systemPrompt 系统提示词（如果为 null，从配置文件加载）
     * @param userPrompt 用户提示词（如果为 null，从配置文件加载）
     * @param sceneKeywords 场景关键词（可选，用于提示词定制）
     * @return 识别结果文本（阶段一输出）
     * @throws IOException 调用失败时抛出
     */
    String callPhaseA(String fileUrl, String systemPrompt, String userPrompt, String sceneKeywords) throws IOException;
    
    /**
     * 阶段二：将识别文本转换为标准 JSON
     * @param recognitionText 阶段一的识别结果文本
     * @param prompt 转换提示词
     * @return JSON 响应文本（阶段二输出）
     * @throws IOException 调用失败时抛出
     */
    String callPhaseB(String recognitionText, String prompt) throws IOException;
    
    /**
     * 聊天模式：支持多模态输入（文本+图片），用于手动录入接口
     * @param messages 消息列表，每个消息可以是：
     *                 - String: 纯文本消息
     *                 - JsonNode: 多模态消息（包含 text 和 image_url）
     * @return 识别结果文本
     * @throws IOException 调用失败时抛出
     */
    String callChat(List<Object> messages) throws IOException;
    
    /**
     * 获取提供者名称（用于日志和配置）
     */
    String getProviderName();
}
