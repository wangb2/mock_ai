# LLM 模型提供者使用说明

## 概述

系统现在支持多个 LLM 模型提供者，包括 Zhipu AI、OpenAI 和豆包（Doubao）。所有模型通过统一的接口调用，实现完全解耦。

## 配置

### 1. 模型切换配置

在 `application.yml` 中配置：

```yaml
# LLM 模型提供者配置（zhipu、openai 或 doubao）
llm:
  provider: "zhipu"  # 可选值: zhipu, openai, doubao
```

### 2. Zhipu AI 配置

```yaml
zhipu:
  api-key: "your-api-key"  # 单个 API key（向后兼容）
  api-keys: "key1,key2,key3"  # 多个 API key，用逗号分隔，用于轮询提高并发
  model: "glm-4.7"  # 阶段二使用的模型
  file-model: "GLM-4.6V-FlashX"  # 阶段一使用的模型
  temperature: 0
  max-tokens: 2048
```

### 3. OpenAI 配置

```yaml
openai:
  api-key: "your-openai-api-key"  # OpenAI API Key
  model: "gpt-4.1"  # OpenAI 模型名称
  temperature: 0
```

### 4. 豆包（Doubao）配置

```yaml
doubao:
  api-key: "your-doubao-api-key"  # 豆包（火山引擎）API Key
  phase-a-model: "doubao-seed-1-6-251015"  # 阶段一使用的模型（Responses API）
  phase-b-model: "doubao-1-5-pro-32k-250115"  # 阶段二使用的模型（Chat Completions API）
  temperature: 0
```

**注意**：豆包模型使用 Zhipu 的提示词配置，无需单独配置提示词文件。

## 提示词配置

提示词存储在独立的配置文件中，方便查看和修改：

- `src/main/resources/zhipu-prompts.yml` - Zhipu 模型的提示词
- `src/main/resources/openai-prompts.yml` - OpenAI 模型的提示词
- 豆包模型复用 Zhipu 的提示词配置

### 提示词结构

每个模型的提示词包含：
- `phase-a-system`: 阶段一的系统提示词（仅 Zhipu 使用）
- `phase-a-user` / `phase-a`: 阶段一的用户提示词
- `phase-b`: 阶段二的 JSON 转换提示词

### 占位符

- `{recognitionText}`: 在阶段二提示词中，会自动替换为阶段一的识别结果文本

## 架构设计

### 核心接口

- `LLMProvider`: 统一的模型提供者接口
  - `callPhaseA()`: 阶段一调用（文件识别）
  - `callPhaseB()`: 阶段二调用（JSON 转换）
  - `getProviderName()`: 获取提供者名称

### 实现类

- `ZhipuLLMProvider`: Zhipu AI 实现
  - 支持文件 URL 直接调用
  - 支持 API key 轮询
  - 使用 `fileModel` 进行阶段一，`model` 进行阶段二

- `OpenAILLMProvider`: OpenAI 实现
  - 自动下载文件并上传到 OpenAI
  - 使用 Responses API 进行调用
  - 自动提取响应文本

- `DoubaoLLMProvider`: 豆包（火山引擎）实现
  - 支持文件 URL 直接调用（使用七牛云 URL）
  - 阶段一使用 Responses API（`/api/v3/responses`）
  - 阶段二使用 Chat Completions API（`/api/v3/chat/completions`）
  - 复用 Zhipu 的提示词配置

### 工厂类

- `LLMProviderFactory`: 根据配置选择对应的提供者

### 提示词服务

- `PromptService`: 从 YAML 配置文件加载提示词

## 使用流程

1. 系统启动时，`LLMProviderFactory` 根据 `llm.provider` 配置选择提供者
2. `PromptService` 加载对应模型的提示词配置文件
3. 当需要处理文件时：
   - 调用 `provider.callPhaseA()` 进行阶段一识别
   - 提取识别结果文本
   - 调用 `provider.callPhaseB()` 进行阶段二 JSON 转换
   - 解析并保存结果

## 扩展新模型

要添加新的模型提供者：

1. 实现 `LLMProvider` 接口
2. 在 `LLMProviderFactory` 中添加选择逻辑
3. 创建对应的提示词配置文件（如 `newmodel-prompts.yml`）
4. 在 `PromptService` 中添加加载逻辑
5. 在 `application.yml` 中添加配置项

## 注意事项

1. **OpenAI 文件上传**：OpenAI 不能直接读取公网 URL，系统会自动下载文件并上传到 OpenAI
2. **豆包文件处理**：豆包可以直接使用七牛云的 URL，无需额外上传步骤
3. **提示词加载**：如果提示词文件不存在或加载失败，会使用空字符串，可能导致调用失败
4. **API Key 轮询**：Zhipu 支持多个 API key 轮询，提高并发能力
5. **场景关键词**：阶段一调用时，可以传递场景关键词用于提示词定制（目前主要用于 Zhipu）
6. **豆包提示词**：豆包模型复用 Zhipu 的提示词配置，无需单独配置
