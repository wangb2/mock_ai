package com.example.mock.parser.service;

import com.example.mock.parser.entity.MockEndpointEntity;
import com.example.mock.parser.entity.MockOperationLogEntity;
import com.example.mock.parser.entity.MockResponseCacheEntity;
import com.example.mock.parser.model.MockEndpointItem;
import com.example.mock.parser.model.MockEndpointResult;
import com.example.mock.parser.model.ManualPreviewResult;
import com.example.mock.parser.model.ParsedDocument;
import com.example.mock.parser.model.Section;
import com.example.mock.parser.model.TableData;
import com.example.mock.parser.repository.MockEndpointRepository;
import com.example.mock.parser.repository.MockOperationLogRepository;
import com.example.mock.parser.repository.MockResponseCacheRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.UUID;
import java.security.MessageDigest;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;

@Service
public class MockEndpointService {
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MockEndpointRepository repository;
    private final MockResponseCacheRepository responseCacheRepository;
    private final MockOperationLogRepository logRepository;
    private final com.example.mock.parser.service.llm.LLMProviderFactory llmProviderFactory;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MockEndpointService.class);

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.model:glm-4.7}")
    private String model;

    @Value("${zhipu.file-model:glm-4.6v-flash}")
    private String fileModel;

    @Value("${zhipu.max-tokens:2048}")
    private int maxTokens;

    @Value("${zhipu.temperature:0.2}")
    private double temperature;

    @Value("${filter.keywords:}")
    private String keywords;

    @Value("${filter.url-regex:(https?://|/)[\\w\\-./:?&=%#]+}")
    private String urlRegex;

    private List<String> keywordList = new ArrayList<>();
    private Pattern urlPattern = Pattern.compile("(https?://|/)[\\w\\-./:?&=%#]+");

    public MockEndpointService(RestTemplate restTemplate,
                               ObjectMapper objectMapper,
                               MockEndpointRepository repository,
                               MockResponseCacheRepository responseCacheRepository,
                               MockOperationLogRepository logRepository,
                               com.example.mock.parser.service.llm.LLMProviderFactory llmProviderFactory) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.responseCacheRepository = responseCacheRepository;
        this.logRepository = logRepository;
        this.llmProviderFactory = llmProviderFactory;
    }

    @PostConstruct
    public void init() {
        keywordList = splitKeywords(keywords);
        urlPattern = Pattern.compile(urlRegex);
    }

    public MockEndpointResult generateEndpoints(ParsedDocument parsedDocument,
                                                String sourceFileId,
                                                String sourceFileName,
                                                String sourceFileUrl,
                                                boolean fullAi,
                                                String sceneId,
                                                String sceneName,
                                                String sceneKeywords) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing zhipu.api-key");
        }

        // 如果 sourceFileUrl 是七牛云URL（以 http:// 或 https:// 开头），使用新模型直接处理文件
        boolean isQiniuUrl = sourceFileUrl != null && (sourceFileUrl.startsWith("http://") || sourceFileUrl.startsWith("https://"));
        logger.info("Checking file URL for model selection. fileUrl={}, isQiniuUrl={}, willUseFileModel={}", 
                sourceFileUrl, isQiniuUrl, isQiniuUrl);
        
        if (isQiniuUrl) {
            // 检查文件类型：如果不是PDF，强制使用智谱模型（因为豆包只支持PDF）
            boolean isPdf = sourceFileName != null && sourceFileName.toLowerCase().endsWith(".pdf");
            com.example.mock.parser.service.llm.LLMProvider provider;
            if (isPdf) {
                // PDF文件：使用当前配置的LLM Provider（可能是豆包或智谱）
                provider = llmProviderFactory.getProvider();
                logger.info("File is PDF, using configured LLM provider. provider={}, fileUrl={}", 
                        provider.getProviderName(), sourceFileUrl);
            } else {
                // 非PDF文件（如DOCX）：强制使用智谱模型（因为豆包不支持DOCX）
                provider = llmProviderFactory.getProvider("zhipu");
                logger.info("File is not PDF (type: {}), forcing Zhipu LLM provider. fileUrl={}", 
                        sourceFileName != null ? sourceFileName.substring(sourceFileName.lastIndexOf('.') + 1) : "unknown", 
                        sourceFileUrl);
            }
            return generateEndpointsWithFileUrl(sourceFileUrl, sourceFileId, sourceFileName, sceneId, sceneName, sceneKeywords, provider);
        } else {
            logger.info("File not uploaded to Qiniu or URL is local, using traditional model. fileUrl={}, model={}", sourceFileUrl, model);
        }

        List<String> originalKeywords = keywordList;
        try {
            if (sceneKeywords != null && !sceneKeywords.trim().isEmpty()) {
                String combined = sceneKeywords.trim() + "," + keywords;
                keywordList = splitKeywords(combined);
            } else {
                keywordList = splitKeywords(keywords);
            }
            if (fullAi) {
                return generateEndpointsFullAi(parsedDocument, sourceFileId, sourceFileName, sourceFileUrl,
                        sceneId, sceneName);
            }

            List<Chunk> chunks = chunk(parsedDocument);
            MockEndpointResult result = new MockEndpointResult();
            java.util.Set<String> seenKeys = new java.util.HashSet<>();

            logger.info("Generating endpoints from document. fileId={}, fileName={}, chunks={}", sourceFileId, sourceFileName, chunks.size());
            for (Chunk chunk : chunks) {
                logger.info("LLM chunk ready. title={}, textLength={}, tableCount={}",
                        chunk.title, chunk.text.length(), chunk.tableCount);
                String preview = buildPreview(chunk.text.toString(), 8, 1200);
                if (!preview.isEmpty()) {
                    logger.info("LLM chunk preview (first 8 lines):\n{}", preview);
                }
                String prompt = buildPrompt(chunk.text.toString());
                String raw = callZhipu(prompt);
                MockEndpointItem item = parseEndpoint(raw, chunk.title);
                if (item == null) {
                    logger.warn("Skip empty endpoint. title={}, fileId={}, rawLength={}",
                            chunk.title, sourceFileId, raw == null ? 0 : raw.length());
                    continue;
                }
                item.setTitle(chunk.title);
                String apiPath = extractApiPath(chunk.text.toString());
                if (apiPath != null && !apiPath.isEmpty()) {
                    item.setApiPath(apiPath);
                }
                normalizeTitle(item, chunk.text.toString());
                if (!hasApiKeyword(item.getTitle())) {
                logger.info("Skip non-api title. title={}, fileId={}", item.getTitle(), sourceFileId);
                continue;
            }
                String dedupeKey = buildDedupeKey(item);
                if (!dedupeKey.isEmpty() && !seenKeys.add(dedupeKey)) {
                    logger.info("Skip duplicate endpoint. key={}, title={}, fileId={}", dedupeKey, item.getTitle(), sourceFileId);
                    continue;
                }
                if (!hasMeaningfulContent(item)) {
                    logger.warn("Skip endpoint without examples. title={}, fileId={}, method={}, requiredFields={}",
                            item.getTitle(), sourceFileId, item.getMethod(),
                            item.getRequiredFields() == null ? 0 : item.getRequiredFields().size());
                    continue;
                }
                String id = UUID.randomUUID().toString().replace("-", "");
                item.setId(id);
                if (item.getApiPath() != null && !item.getApiPath().isEmpty()) {
                    item.setMockUrl("/mock" + item.getApiPath());
                } else {
                    item.setMockUrl("/parse/mock/" + id);
                }
                item.setRaw(raw);
                item.setSourceFileId(sourceFileId);
                item.setSourceFileName(sourceFileName);
                item.setSourceFileUrl(sourceFileUrl);
                item.setSceneId(sceneId);
                item.setSceneName(sceneName);
                repository.save(toEntity(item));
                result.getItems().add(item);
            }

            logger.info("  -> ========== 接口处理和保存完成 ==========");
            logger.info("  -> 最终结果: 成功保存 {} 个接口", result.getItems().size());
            logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: " + result.getItems().size());
            logger.info("Generated endpoints done. fileId={}, fileName={}, count={}", sourceFileId, sourceFileName, result.getItems().size());
            return result;
        } finally {
            keywordList = originalKeywords;
        }
    }

    /**
     * 使用文件URL直接调用新模型处理文档
     * 两步处理：
     * 1. 使用 fileModel (glm-4.6v-flash) 提取 API 信息（文本格式）
     * 2. 使用 model (glm-4.7) 将文本转换为标准 JSON
     * 
     * @param provider LLM提供者实例（已根据文件类型选择，非PDF强制使用智谱）
     */
    private MockEndpointResult generateEndpointsWithFileUrl(String fileUrl,
                                                             String sourceFileId,
                                                             String sourceFileName,
                                                             String sceneId,
                                                             String sceneName,
                                                             String sceneKeywords,
                                                             com.example.mock.parser.service.llm.LLMProvider provider) throws IOException {
        List<String> originalKeywords = keywordList;
        try {
            if (sceneKeywords != null && !sceneKeywords.trim().isEmpty()) {
                String combined = sceneKeywords.trim() + "," + keywords;
                keywordList = splitKeywords(combined);
            } else {
                keywordList = splitKeywords(keywords);
            }
            
            // 使用传入的 LLM 提供者（已根据文件类型选择）
            logger.info("  -> 使用的 LLM 提供者: {}", provider.getProviderName());
            logger.info("  -> 文件URL: {}", fileUrl);
            logger.info("  -> 场景关键词: {}", sceneKeywords);
            
            // 第一步：提取 API 信息（文本格式）
            logger.info("  -> ========== 阶段一：开始提取 API 信息 ==========");
            logger.info("  -> 调用大模型进行文档识别. provider={}, fileUrl={}", provider.getProviderName(), fileUrl);
            long phaseAStartTime = System.currentTimeMillis();
            // 传递 null 让 provider 从配置文件加载提示词，传递 sceneKeywords 用于提示词定制
            String recognitionText = provider.callPhaseA(fileUrl, null, null, sceneKeywords);
            long phaseAEndTime = System.currentTimeMillis();
            logger.info("  -> 阶段一完成，耗时: {}ms", phaseAEndTime - phaseAStartTime);
            
            // 提取识别结果文本（阶段一输出）
            logger.info("  -> 提取阶段一识别结果文本. rawResponseLength={}", 
                    recognitionText != null ? recognitionText.length() : 0);
            String extractedRecognition = extractRecognitionText(recognitionText);
            if (extractedRecognition == null || extractedRecognition.trim().isEmpty()) {
                logger.warn("  -> 警告：未提取到识别文本，使用完整响应");
                extractedRecognition = recognitionText;
            }
            
            logger.info("  -> 阶段一识别文本长度: {}", extractedRecognition.length());
            logger.info("  -> 阶段一识别文本预览（前500字符）: {}", 
                    extractedRecognition.length() > 500 ? extractedRecognition.substring(0, 500) + "..." : extractedRecognition);
            
            // 第二步：将文本转换为标准 JSON
            logger.info("  -> ========== 阶段二：开始转换为 JSON ==========");
            logger.info("  -> 调用大模型进行 JSON 转换. provider={}, recognitionTextLength={}", 
                    provider.getProviderName(), extractedRecognition.length());
            long phaseBStartTime = System.currentTimeMillis();
            // 传递 null 让 provider 从配置文件加载提示词（会自动替换 recognitionText）
            String jsonResponse = provider.callPhaseB(extractedRecognition, null);
            long phaseBEndTime = System.currentTimeMillis();
            logger.info("  -> 阶段二完成，耗时: {}ms", phaseBEndTime - phaseBStartTime);
            logger.info("  -> 阶段二 JSON 响应长度: {}", jsonResponse != null ? jsonResponse.length() : 0);
            logger.info("  -> 阶段二 JSON 响应预览（前500字符）: {}", 
                    jsonResponse != null && jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse);
            
            // 解析 JSON 响应
            logger.info("  -> ========== 开始解析 JSON 响应 ==========");
            logger.info("  -> 调用 parseEndpoints 解析 JSON. jsonResponseLength={}", 
                    jsonResponse != null ? jsonResponse.length() : 0);
            List<MockEndpointItem> items = parseEndpoints(jsonResponse);
            logger.info("  -> JSON 解析完成. parsedItemsCount={}", items != null ? items.size() : 0);

            MockEndpointResult result = new MockEndpointResult();
            java.util.Set<String> seenKeys = new java.util.HashSet<>();
            if (items == null || items.isEmpty()) {
                logger.warn("  -> 警告：解析结果为空，未生成任何接口");
                logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: 0");
                return result;
            }
            
            logger.info("  -> ========== 开始处理和保存接口 ==========");
            logger.info("  -> 待处理接口数量: {}", items.size());
            
            for (MockEndpointItem item : items) {
                if (item == null) {
                    continue;
                }
                String dedupeKey = buildDedupeKey(item);
                if (!dedupeKey.isEmpty() && !seenKeys.add(dedupeKey)) {
                    logger.info("Skip duplicate endpoint. key={}, title={}", dedupeKey, item.getTitle());
                    continue;
                }
                // 详细记录检查前的状态
                logger.debug("Checking hasMeaningfulContent. title={}, method={}, hasRequest={}, hasResponse={}, hasError={}, requiredFields={}", 
                        item.getTitle(), item.getMethod(),
                        item.getRequestExample() != null, item.getResponseExample() != null, 
                        item.getErrorResponseExample() != null,
                        item.getRequiredFields() != null ? item.getRequiredFields().size() : 0);
                
                if (!hasMeaningfulContent(item)) {
                    logger.warn("Skip endpoint without examples. title={}, method={}, hasRequest={}, hasResponse={}, hasError={}", 
                            item.getTitle(), item.getMethod(),
                            item.getRequestExample() != null, item.getResponseExample() != null, 
                            item.getErrorResponseExample() != null);
                    continue;
                }
                
                logger.debug("Endpoint passed hasMeaningfulContent check. title={}, method={}", item.getTitle(), item.getMethod());
                String id = UUID.randomUUID().toString().replace("-", "");
                item.setId(id);
                if (item.getApiPath() != null && !item.getApiPath().isEmpty()) {
                    item.setMockUrl("/mock" + item.getApiPath());
                } else {
                    item.setMockUrl("/parse/mock/" + id);
                }
                item.setRaw(jsonResponse != null ? jsonResponse : recognitionText);
                item.setSourceFileId(sourceFileId);
                item.setSourceFileName(sourceFileName);
                item.setSourceFileUrl(fileUrl);
                item.setSceneId(sceneId);
                item.setSceneName(sceneName);
                
                logger.info("  -> 准备保存接口到数据库. id={}, title={}, method={}, apiPath={}, sceneId={}, hasRequest={}, hasResponse={}, hasError={}", 
                        item.getId(), item.getTitle(), item.getMethod(), item.getApiPath(), item.getSceneId(),
                        item.getRequestExample() != null, item.getResponseExample() != null, 
                        item.getErrorResponseExample() != null);
                try {
                    MockEndpointEntity entity = toEntity(item);
                    repository.save(entity);
                    logger.info("  -> ✓ 接口保存成功. id={}, title={}, mockUrl={}", 
                            item.getId(), item.getTitle(), item.getMockUrl());
                    result.getItems().add(item);
                } catch (Exception ex) {
                    logger.error("  -> ✗ 接口保存失败. id={}, title={}, error={}", 
                            item.getId(), item.getTitle(), ex.getMessage(), ex);
                    continue;
                }
                result.getItems().add(item);
            }

            logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: " + result.getItems().size());
            logger.info("Generated endpoints with file URL. fileId={}, fileName={}, count={}", 
                    sourceFileId, sourceFileName, result.getItems().size());
            return result;
        } finally {
            keywordList = originalKeywords;
        }
    }

    private MockEndpointResult generateEndpointsFullAi(ParsedDocument parsedDocument,
                                                       String sourceFileId,
                                                       String sourceFileName,
                                                       String sourceFileUrl,
                                                       String sceneId,
                                                       String sceneName) throws IOException {
        String fullText = buildFullDocumentText(parsedDocument);
        String prompt = buildFullDocumentPrompt(fullText);
        String raw = callZhipu(prompt);
        List<MockEndpointItem> items = parseEndpoints(raw);

        MockEndpointResult result = new MockEndpointResult();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        if (items == null || items.isEmpty()) {
            logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: 0");
            return result;
        }
        for (MockEndpointItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
                item.setTitle("API");
            }
            normalizeTitle(item, "");
            if (!hasApiKeyword(item.getTitle())) {
                logger.info("Skip non-api title(fullAi). title={}, fileId={}", item.getTitle(), sourceFileId);
                continue;
            }
            String dedupeKey = buildDedupeKey(item);
            if (!dedupeKey.isEmpty() && !seenKeys.add(dedupeKey)) {
                logger.info("Skip duplicate endpoint(fullAi). key={}, title={}, fileId={}", dedupeKey, item.getTitle(), sourceFileId);
                continue;
            }
            if (!hasMeaningfulContent(item)) {
                logger.warn("Skip endpoint without examples. title={}, fileId={}, method={}, requiredFields={}",
                        item.getTitle(), sourceFileId, item.getMethod(),
                        item.getRequiredFields() == null ? 0 : item.getRequiredFields().size());
                continue;
            }
            String id = UUID.randomUUID().toString().replace("-", "");
            item.setId(id);
            item.setMockUrl("/parse/mock/" + id);
            item.setRaw(raw);
            item.setSourceFileId(sourceFileId);
            item.setSourceFileName(sourceFileName);
            item.setSourceFileUrl(sourceFileUrl);
            item.setSceneId(sceneId);
            item.setSceneName(sceneName);
            repository.save(toEntity(item));
            result.getItems().add(item);
        }
        logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: " + result.getItems().size());
        logger.info("Generated endpoints done (fullAi). fileId={}, fileName={}, count={}",
                sourceFileId, sourceFileName, result.getItems().size());
        return result;
    }

    public MockEndpointItem getById(String id) {
        return repository.findById(id).map(this::toItem).orElse(null);
    }

    public MockEndpointItem getByPath(String apiPath, String method) {
        if (apiPath == null || apiPath.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeApiPath(apiPath);
        String normalizedMethod = method == null ? null : method.toUpperCase(Locale.ROOT);
        MockEndpointEntity entity = repository.findFirstByApiPathAndMethod(normalized, normalizedMethod);
        if (entity == null) {
            entity = repository.findFirstByApiPathIgnoreCaseAndMethod(normalized, normalizedMethod);
        }
        if (entity == null) {
            entity = repository.findFirstByApiPathIgnoreCase(normalized);
        }
        return entity == null ? null : toItem(entity);
    }

    public List<MockEndpointItem> getAll() {
        List<MockEndpointItem> items = new ArrayList<>();
        List<MockEndpointEntity> entities = repository.findAllByOrderByCreatedAtDesc();
        logger.info("getAll: found {} entities in database", entities.size());
        for (MockEndpointEntity entity : entities) {
            MockEndpointItem item = toItem(entity);
            if (item != null) {
                items.add(item);
                logger.debug("getAll: added item. id={}, title={}, sceneId={}, method={}", 
                    item.getId(), item.getTitle(), item.getSceneId(), item.getMethod());
            }
        }
        logger.info("getAll: returning {} items", items.size());
        return items;
    }

    public List<MockEndpointItem> getBySceneId(String sceneId) {
        List<MockEndpointItem> items = new ArrayList<>();
        if (sceneId == null || sceneId.trim().isEmpty()) {
            return items;
        }
        for (MockEndpointEntity entity : repository.findBySceneIdOrderByCreatedAtDesc(sceneId)) {
            items.add(toItem(entity));
        }
        return items;
    }

    @Transactional
    public int deleteBySourceFileName(String sourceFileName) {
        if (sourceFileName == null || sourceFileName.trim().isEmpty()) {
            return 0;
        }
        List<MockEndpointEntity> entities = repository.findBySourceFileName(sourceFileName);
        for (MockEndpointEntity entity : entities) {
            responseCacheRepository.deleteByMockId(entity.getId());
            logRepository.deleteByMockId(entity.getId());
        }
        repository.deleteAll(entities);
        if (!entities.isEmpty()) {
            logOperation("DOC_DELETE", null, sourceFileName, "重复文档删除接口: " + entities.size());
        }
        return entities.size();
    }

    @Transactional
    public boolean deleteMockById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        if (!repository.existsById(id)) {
            return false;
        }
        responseCacheRepository.deleteByMockId(id);
        logRepository.deleteByMockId(id);
        repository.deleteById(id);
        logOperation("MOCK_DELETE", id, null, "删除Mock接口");
        return true;
    }

    @Transactional
    public int deleteBySourceFileId(String sourceFileId) {
        if (sourceFileId == null || sourceFileId.trim().isEmpty()) {
            return 0;
        }
        List<MockEndpointEntity> entities = repository.findBySourceFileId(sourceFileId);
        for (MockEndpointEntity entity : entities) {
            responseCacheRepository.deleteByMockId(entity.getId());
            logRepository.deleteByMockId(entity.getId());
        }
        repository.deleteAll(entities);
        logOperation("DOC_DELETE", null, null, "删除文档关联Mock: " + entities.size());
        return entities.size();
    }

    @Transactional
    public boolean updateMockExamples(String id, JsonNode body) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        MockEndpointEntity entity = repository.findById(id).orElse(null);
        if (entity == null) {
            return false;
        }
        if (body != null && body.isObject()) {
            JsonNode request = body.get("requestExample");
            JsonNode response = body.get("responseExample");
            JsonNode error = body.get("errorResponseExample");
            JsonNode required = body.get("requiredFields");
            JsonNode delayNode = body.get("responseDelayMs");
            // 修复字符串化的 JSON（与 createManualEndpoint 保持一致）
            if (request != null && !request.isMissingNode()) {
                entity.setRequestExample(stringify(fixStringifiedJson(request)));
            }
            if (response != null && !response.isMissingNode()) {
                entity.setResponseExample(stringify(fixStringifiedJson(response)));
            }
            if (error != null && !error.isMissingNode()) {
                entity.setErrorResponseExample(stringify(fixStringifiedJson(error)));
            }
            if (required != null && required.isArray()) {
                entity.setRequiredFields(stringify(required));
            }
            if (delayNode == null || delayNode.isNull()) {
                entity.setResponseDelayMs(null);
            } else if (delayNode.isNumber()) {
                entity.setResponseDelayMs(delayNode.asInt());
            } else if (delayNode != null && delayNode.isTextual()) {
                try {
                    entity.setResponseDelayMs(Integer.parseInt(delayNode.asText().trim()));
                } catch (NumberFormatException ex) {
                    // ignore invalid
                }
            }
            repository.save(entity);
            responseCacheRepository.deleteByMockId(id);
            logOperation("MOCK_UPDATE", id, entity.getSourceFileName(), "更新请求/响应示例");
            logger.info("Updated mock examples. id={}", id);
            return true;
        }
        return false;
    }

    public List<MockOperationLogEntity> getRecentLogs() {
        return logRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public ObjectNode getLogStats() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("uploadMock", logRepository.countByType("UPLOAD_MOCK"));
        node.put("mockHit", logRepository.countByType("MOCK_HIT"));
        node.put("mockGen", logRepository.countByType("MOCK_GEN"));
        node.put("mockError", logRepository.countByType("MOCK_ERROR"));
        node.put("mockValidationFail", logRepository.countByType("MOCK_VALIDATION_FAIL"));
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        node.put("mockHit24h", logRepository.countByTypeAndCreatedAtAfter("MOCK_HIT", since));
        node.put("mockGen24h", logRepository.countByTypeAndCreatedAtAfter("MOCK_GEN", since));
        node.put("mockError24h", logRepository.countByTypeAndCreatedAtAfter("MOCK_ERROR", since));
        node.put("mockValidationFail24h", logRepository.countByTypeAndCreatedAtAfter("MOCK_VALIDATION_FAIL", since));
        return node;
    }

    public List<ObjectNode> getEndpointTodayCounts() {
        return buildEndpointCounts(logRepository.countByMockIdToday());
    }

    public List<ObjectNode> getEndpointWeekCounts() {
        return buildEndpointCounts(logRepository.countByMockIdLast7Days());
    }

    public List<ObjectNode> getSceneTodayCounts() {
        return buildSceneCounts(logRepository.countBySceneIdToday());
    }

    public List<ObjectNode> getSceneWeekCounts() {
        return buildSceneCounts(logRepository.countBySceneIdLast7Days());
    }

    private List<ObjectNode> buildEndpointCounts(List<Object[]> rows) {
        List<ObjectNode> list = new ArrayList<>();
        for (Object[] row : rows) {
            String mockId = row[0] == null ? "" : String.valueOf(row[0]);
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            String title = repository.findById(mockId).map(MockEndpointEntity::getTitle).orElse("-");
            ObjectNode node = objectMapper.createObjectNode();
            node.put("mockId", mockId);
            node.put("title", title);
            node.put("count", count);
            list.add(node);
        }
        return list;
    }

    private List<ObjectNode> buildSceneCounts(List<Object[]> rows) {
        List<ObjectNode> list = new ArrayList<>();
        for (Object[] row : rows) {
            String sceneId = row[0] == null ? "" : String.valueOf(row[0]);
            String sceneName = row.length > 1 && row[1] != null ? String.valueOf(row[1]) : "-";
            long count = row.length > 2 && row[2] != null ? ((Number) row[2]).longValue() : 0L;
            ObjectNode node = objectMapper.createObjectNode();
            node.put("sceneId", sceneId);
            node.put("sceneName", sceneName);
            node.put("count", count);
            list.add(node);
        }
        return list;
    }

    public JsonNode getDynamicResponse(MockEndpointItem item, JsonNode requestBody) throws IOException {
        if (item == null) {
            return null;
        }
        String signature = buildRequestSignature(item, requestBody);
        if (signature == null || signature.isEmpty()) {
            return item.getResponseExample();
        }
        MockResponseCacheEntity cached = responseCacheRepository
                .findFirstByMockIdAndRequestSignature(item.getId(), signature)
                .orElse(null);
        if (cached != null) {
            logOperation("MOCK_HIT", item.getId(), item.getSourceFileName(), "命中缓存");
            return parseJson(cached.getResponseBody());
        }
        JsonNode generated = buildDynamicResponse(item.getResponseExample(), requestBody);
        if (generated != null) {
            persistResponseCache(item.getId(), signature, requestBody, generated);
            logOperation("MOCK_GEN", item.getId(), item.getSourceFileName(), "示例响应");
        }
        return generated;
    }

    public MockEndpointItem createManualEndpoint(String title,
                                                 String method,
                                                 JsonNode requestExample,
                                                 JsonNode responseExample,
                                                 JsonNode errorResponseExample,
                                                 List<String> requiredFields,
                                                 String sceneId,
                                                 String sceneName,
                                                 Integer errorHttpStatus,
                                                 Integer responseDelayMs) {
        MockEndpointItem item = new MockEndpointItem();
        item.setTitle(title == null ? "" : title.trim());
        item.setMethod(method == null ? "" : method.trim().toUpperCase(Locale.ROOT));
        // 修复字符串化的 JSON（与 parseEndpointNode 保持一致）
        item.setRequestExample(requestExample != null && !requestExample.isNull() && !requestExample.isMissingNode() 
            ? fixStringifiedJson(requestExample) : null);
        item.setResponseExample(responseExample != null && !responseExample.isNull() && !responseExample.isMissingNode() 
            ? fixStringifiedJson(responseExample) : null);
        item.setErrorResponseExample(errorResponseExample != null && !errorResponseExample.isNull() && !errorResponseExample.isMissingNode() 
            ? fixStringifiedJson(errorResponseExample) : null);
        if (requiredFields != null) {
            item.setRequiredFields(requiredFields);
        }
        item.setSceneId(sceneId);
        item.setSceneName(sceneName);
        if (responseDelayMs != null) {
            item.setResponseDelayMs(responseDelayMs);
        }
        item.setErrorHttpStatus(errorHttpStatus);
        
        // 对于手动录入，使用更宽松的检查：只要有标题和方法，或者有请求/响应示例之一即可
        if (item.getTitle().trim().isEmpty()) {
            logger.warn("Manual endpoint creation failed: title is empty");
            return null;
        }
        boolean hasRequest = !isEffectivelyEmptyRequestExample(item.getRequestExample());
        boolean hasResponse = !isEffectivelyEmptyResponseExample(item.getResponseExample());
        boolean hasError = !isEmptyJson(item.getErrorResponseExample());
        if (!hasRequest && !hasResponse && !hasError) {
            logger.warn("Manual endpoint creation failed: no request/response/error example. title={}", item.getTitle());
            return null;
        }
        
        String id = UUID.randomUUID().toString().replace("-", "");
        item.setId(id);
        item.setMockUrl("/parse/mock/" + id);
        repository.save(toEntity(item));
        logger.info("Manual endpoint created successfully. id={}, title={}, method={}, hasRequest={}, hasResponse={}", 
                id, item.getTitle(), item.getMethod(), hasRequest, hasResponse);
        logOperation("MOCK_CREATE", id, null, "手动新增接口");
        return item;
    }

    public MockEndpointItem generateManualPreview(String userInput) throws IOException {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        String prompt = buildManualPrompt(userInput);
        String raw = callZhipu(prompt);
        return parseEndpoint(raw, "");
    }

    public ManualPreviewResult generateManualPreviewResult(java.util.List<Object> messages, String providerName) throws IOException {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        
        // 获取 LLM Provider
        com.example.mock.parser.service.llm.LLMProvider provider = llmProviderFactory.getProvider(providerName);
        logger.info("Using LLM provider: {} for manual preview", provider.getProviderName());
        
        // 收集所有图片和文本消息
        java.util.List<Object> imageMessages = new java.util.ArrayList<>();
        java.util.List<String> textMessages = new java.util.ArrayList<>();
        
        for (Object msg : messages) {
            if (msg instanceof String) {
                // 纯文本消息
                textMessages.add((String) msg);
            } else if (msg instanceof com.fasterxml.jackson.databind.JsonNode) {
                // 多模态消息
                com.fasterxml.jackson.databind.JsonNode node = (com.fasterxml.jackson.databind.JsonNode) msg;
                if (node.isArray()) {
                    // 检查是否包含图片
                    boolean hasImage = false;
                    for (com.fasterxml.jackson.databind.JsonNode item : node) {
                        String type = item.path("type").asText("");
                        if ("image_url".equals(type) || "input_image".equals(type)) {
                            hasImage = true;
                            break;
                        }
                    }
                    if (hasImage) {
                        // 包含图片的消息，需要单独识别
                        imageMessages.add(msg);
                    } else {
                        // 只有文本，提取文本内容
                        for (com.fasterxml.jackson.databind.JsonNode item : node) {
                            if ("text".equals(item.path("type").asText(""))) {
                                textMessages.add(item.path("text").asText(""));
                            }
                        }
                    }
                }
            }
        }
        
        // 第一步：分别识别每个图片，然后合并结果
        java.util.List<String> recognitionResults = new java.util.ArrayList<>();
        
        // 添加文本消息到识别结果
        if (!textMessages.isEmpty()) {
            recognitionResults.add(String.join("\n", textMessages));
        }
        
        // 如果有多个图片，分别识别每个图片
        if (!imageMessages.isEmpty()) {
            logger.info("Found {} image(s), will recognize each separately", imageMessages.size());
            for (int i = 0; i < imageMessages.size(); i++) {
                Object imageMsg = imageMessages.get(i);
                logger.info("Recognizing image {}/{}", i + 1, imageMessages.size());
                
                // 为每个图片创建单独的消息列表
                java.util.List<Object> singleImageMessage = new java.util.ArrayList<>();
                singleImageMessage.add(buildImageRecognitionPrompt());
                singleImageMessage.add(imageMsg);
                
                // 调用模型识别单个图片
                String imageRecognition = provider.callChat(singleImageMessage);
                if (imageRecognition != null && !imageRecognition.trim().isEmpty()) {
                    recognitionResults.add("=== 图片 " + (i + 1) + " 识别结果 ===\n" + imageRecognition);
                    logger.info("Image {} recognition completed. Length: {}", i + 1, imageRecognition.length());
                } else {
                    logger.warn("Image {} recognition returned empty result", i + 1);
                }
            }
        }
        
        // 合并所有识别结果
        String recognitionText = String.join("\n\n", recognitionResults);
        if (recognitionText == null || recognitionText.trim().isEmpty()) {
            logger.warn("All recognition results are empty");
            return null;
        }
        
        logger.info("Step 1 completed. Total recognition text length: {}, images: {}, text messages: {}", 
                recognitionText.length(), imageMessages.size(), textMessages.size());
        
        // 第二步：将合并后的识别结果转换为标准 JSON
        logger.info("Step 2: Converting merged recognition text to standard JSON");
        String manualPrompt = buildManualPreviewPrompt(recognitionText);
        String jsonResponse = provider.callPhaseB(recognitionText, manualPrompt);
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            logger.warn("LLM returned empty response in step 2");
            return null;
        }
        logger.info("Step 2 completed. JSON response length: {}", jsonResponse.length());
        
        // 记录原始响应（前500字符用于调试）
        String preview = jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse;
        logger.info("LLM raw response preview: {}", preview);
        JsonNode node = readJsonLoosely(jsonResponse);
        if (node == null) {
            logger.warn("readJsonLoosely returned null. Raw preview: {}", preview);
            return null;
        }

        // 兼容模型误输出数组：取第一个对象
        if (node.isArray() && node.size() > 0) {
            JsonNode first = node.get(0);
            if (first != null && first.isObject()) {
                node = first;
            }
        }

        ManualPreviewResult result = new ManualPreviewResult();
        boolean needMoreInfo = node.path("needMoreInfo").asBoolean(false);
        if (needMoreInfo) {
            result.setNeedMoreInfo(true);
            result.setMissingFields(readStringArray(node.path("missingFields")));
            result.setMessage(textOr(node.get("message"), "信息不完整，请补充关键信息后再生成。"));
            JsonNode draftNode = node.path("draft");
            MockEndpointItem draftItem = null;
            if (draftNode != null && draftNode.isObject()) {
                draftItem = parseEndpointNode(draftNode, "");
            } else if (node.isObject()) {
                draftItem = parseEndpointNode(node, "");
            }
            result.setItem(draftItem);
            logger.info("Manual preview needs more info. missingFields={}", String.join(", ", result.getMissingFields()));
            return result;
        }

        MockEndpointItem item = parseEndpointNode(node, "");
        if (item == null) {
            logger.warn("Failed to parse endpoint from LLM response. Raw length: {}", jsonResponse.length());
            // 尝试记录更多信息
            if (node.isObject()) {
                java.util.Iterator<String> fieldNames = node.fieldNames();
                java.util.List<String> keys = new java.util.ArrayList<>();
                while (fieldNames.hasNext()) {
                    keys.add(fieldNames.next());
                }
                logger.warn("readJsonLoosely succeeded but parseEndpointNode returned null. Node keys: {}", 
                    String.join(", ", keys));
            } else {
                logger.warn("readJsonLoosely succeeded but parseEndpointNode returned null. Node is not an object");
            }
        } else {
            logger.info("Successfully parsed endpoint. title={}, method={}, hasRequest={}, hasResponse={}", 
                item.getTitle(), item.getMethod(), 
                item.getRequestExample() != null, item.getResponseExample() != null);
        }
        result.setItem(item);
        return result;
    }
    
    // 向后兼容：如果没有提供 providerName，使用默认的智谱
    public MockEndpointItem generateManualPreview(java.util.List<Object> messages) throws IOException {
        ManualPreviewResult result = generateManualPreviewResult(messages, "zhipu");
        return result == null ? null : result.getItem();
    }
    
    public MockEndpointItem generateManualPreview(java.util.List<Object> messages, String providerName) throws IOException {
        ManualPreviewResult result = generateManualPreviewResult(messages, providerName);
        return result == null ? null : result.getItem();
    }

    private String buildManualPreviewPrompt(String recognitionText) {
        return "你是一个接口结构化助手。请根据【接口识别结果】生成一个JSON对象。\n"
            + "如果识别结果缺少关键信息，请返回 needMoreInfo=true，并列出缺失项。\n"
            + "禁止生成示例接口或虚构内容，信息不足必须提示缺失。\n"
            + "\n"
            + "【文本聊天规则】\n"
            + "- 若识别结果只是泛泛描述（如“帮我生成一个接口/做个接口”）或缺少接口关键信息，必须返回 needMoreInfo=true。\n"
            + "- 文本中未明确出现 method 或路径或请求/响应字段时，禁止补全为示例值。\n"
            + "- 允许从文本中提取已出现的字段填入 draft，其余缺失项必须提示用户补充。\n"
            + "\n"
            + "【关键信息要求】\n"
            + "1. title（接口标题）\n"
            + "2. method（请求方式 GET/POST）\n"
            + "3. 至少提供 requestExample 或 responseExample 任意一侧\n"
            + "\n"
            + "【标题规则】\n"
            + "- 优先使用 Api Name / API Name / 接口标题 / API 标题\n"
            + "- 若文档有 Resource URL 或路径，允许使用最后一段作为标题（如 /PRNEsim/GetSimType -> GetSimType）\n"
            + "- 若仍无法确定标题，必须返回 needMoreInfo=true\n"
            + "\n"
            + "【缺失提示规范】\n"
            + "- missingFields 仅允许以下枚举值：title、method、request/response\n"
            + "- message 必须包含“可直接填写的模板”，方便用户补充\n"
            + "\n"
            + "【输出规则】\n"
            + "A. 若关键信息不完整，输出格式如下（只输出JSON对象，不要其他文字）：\n"
            + "{\n"
            + "  \"needMoreInfo\": true,\n"
            + "  \"missingFields\": [\"title\", \"method\", \"request/response\"],\n"
            + "  \"message\": \"请补充以下关键信息后再生成：title、method、request/response\\n\\n可直接按此模板补充：\\n1) 请求方式：GET/POST\\n2) 请求参数（headers/query/body）：\\n3) 响应字段：\\n4) 示例（可选）：\",\n"
            + "  \"draft\": {\n"
            + "    \"title\": \"已识别的标题（如有）\",\n"
            + "    \"method\": \"已识别的请求方式（如有）\",\n"
            + "    \"requestExample\": {\"headers\":{},\"query\":{},\"body\":{}},\n"
            + "    \"responseExample\": {\"headers\":{},\"body\":{}},\n"
            + "    \"errorResponseExample\": {},\n"
            + "    \"requiredFields\": []\n"
            + "  }\n"
            + "}\n"
            + "\n"
            + "B. 若关键信息完整，输出标准接口JSON对象（只输出JSON对象）：\n"
            + "{\n"
            + "  \"title\": \"...\",\n"
            + "  \"method\": \"POST\",\n"
            + "  \"requestExample\": {\"headers\":{},\"query\":{},\"body\":{}},\n"
            + "  \"responseExample\": {\"headers\":{},\"body\":{}},\n"
            + "  \"errorResponseExample\": {},\n"
            + "  \"requiredFields\": []\n"
            + "}\n"
            + "\n"
            + "【重要】如果识别结果只有泛泛描述（如“帮我生成一个接口”），必须返回 needMoreInfo=true。\n"
            + "【接口识别结果】\n"
            + recognitionText;
    }

    private String buildImageRecognitionPrompt() {
        return "请从截图中识别接口信息，并输出结构化文本：\n"
            + "1) 接口标题/Api Name\n"
            + "2) 请求方式（GET/POST）\n"
            + "3) Resource URL 或路径\n"
            + "4) Request Headers 表/字段\n"
            + "5) Request Body 表/字段\n"
            + "6) Response Body 表/字段\n"
            + "7) 示例请求/响应（如有）\n"
            + "如果截图中未出现某项，请明确写“未提供”。";
    }

    private java.util.List<String> readStringArray(JsonNode node) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().trim().isEmpty()) {
                    list.add(item.asText().trim());
                }
            }
        }
        return list;
    }

    private MockEndpointItem parseEndpoint(String raw, String fallbackTitle) {
        if (raw == null || raw.trim().isEmpty()) {
            logger.debug("parseEndpoint: raw is null or empty");
            return null;
        }
        JsonNode node = readJsonLoosely(raw);
        if (node == null) {
            logger.warn("parseEndpoint: readJsonLoosely returned null for raw: {}", 
                raw.length() > 200 ? raw.substring(0, 200) + "..." : raw);
            return null;
        }
        if (node.isObject()) {
            java.util.Iterator<String> fieldNames = node.fieldNames();
            java.util.List<String> keys = new java.util.ArrayList<>();
            while (fieldNames.hasNext()) {
                keys.add(fieldNames.next());
            }
            logger.debug("parseEndpoint: readJsonLoosely succeeded. Node type: {}, isObject: {}, keys: {}", 
                node.getNodeType(), node.isObject(), String.join(", ", keys));
        } else {
            logger.debug("parseEndpoint: readJsonLoosely succeeded. Node type: {}, isObject: {}", 
                node.getNodeType(), node.isObject());
        }
        MockEndpointItem item = parseEndpointNode(node, fallbackTitle);
        if (item == null) {
            logger.warn("parseEndpoint: parseEndpointNode returned null. Node type: {}, isObject: {}", 
                node.getNodeType(), node.isObject());
        } else {
            logger.debug("parseEndpoint: parseEndpointNode succeeded. title={}, method={}, hasRequest={}, hasResponse={}", 
                item.getTitle(), item.getMethod(), 
                item.getRequestExample() != null, item.getResponseExample() != null);
        }
        return item;
    }

    private List<MockEndpointItem> parseEndpoints(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        
        // 尝试提取并记录识别结果（第一步的文本输出）
        String recognitionText = extractRecognitionText(raw);
        if (recognitionText != null && !recognitionText.trim().isEmpty()) {
            logger.info("=== 模型识别结果（第一步）===");
            // 只记录前2000字符，避免日志过长
            String preview = recognitionText.length() > 2000 ? recognitionText.substring(0, 2000) + "..." : recognitionText;
            logger.info("识别内容预览：\n{}", preview);
            if (recognitionText.length() > 2000) {
                logger.info("（识别内容总长度：{} 字符，已截断）", recognitionText.length());
            }
        }
        
        JsonNode node = readJsonLoosely(raw);
        if (node == null) {
            logger.warn("parseEndpoints: readJsonLoosely returned null. Raw preview: {}", 
                raw.length() > 500 ? raw.substring(0, 500) + "..." : raw);
            return java.util.Collections.emptyList();
        }
        List<MockEndpointItem> items = new ArrayList<>();
        if (node.isArray()) {
            logger.info("parseEndpoints: 解析到JSON数组，包含 {} 个元素", node.size());
            for (int i = 0; i < node.size(); i++) {
                JsonNode itemNode = node.get(i);
                logger.debug("parseEndpoints: 解析数组元素 {}/{}", i + 1, node.size());
                MockEndpointItem item = parseEndpointNode(itemNode, "");
                if (item != null) {
                    logger.debug("parseEndpoints: 成功解析元素 {}. title={}, method={}, hasRequest={}, hasResponse={}", 
                            i + 1, item.getTitle(), item.getMethod(), 
                            item.getRequestExample() != null, item.getResponseExample() != null);
                    items.add(item);
                } else {
                    logger.warn("parseEndpoints: 解析数组元素 {} 失败，parseEndpointNode返回null", i + 1);
                }
            }
            logger.info("parseEndpoints: 成功解析 {} 个接口", items.size());
            return items;
        }
        JsonNode arr = node.path("items");
        if (arr.isArray()) {
            for (JsonNode itemNode : arr) {
                MockEndpointItem item = parseEndpointNode(itemNode, "");
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        }
        MockEndpointItem single = parseEndpointNode(node, "");
        if (single != null) {
            items.add(single);
        }
        return items;
    }
    
    /**
     * 从混合内容中提取识别结果文本（第一步的输出）
     * 识别结果通常在JSON数组之前，以 "### 第一步" 或 "=== 接口" 等标记开始
     */
    private String extractRecognitionText(String raw) {
        String trimmed = raw.trim();
        // 查找识别结果的开始标记
        int recognitionStart = -1;
        String[] startMarkers = {
            "### 第一步",
            "第一步：",
            "=== 接口",
            "接口1：",
            "接口："
        };
        for (String marker : startMarkers) {
            int idx = trimmed.indexOf(marker);
            if (idx >= 0) {
                recognitionStart = idx;
                break;
            }
        }
        if (recognitionStart < 0) {
            return null;
        }
        
        // 查找JSON数组或对象的开始位置（识别结果的结束位置）
        int jsonStart = Math.max(
            trimmed.indexOf('['),
            trimmed.indexOf('{')
        );
        if (jsonStart < 0) {
            // 如果没有找到JSON，返回从识别开始到结尾的所有内容
            return trimmed.substring(recognitionStart);
        }
        
        // 返回识别结果部分（从识别开始到JSON开始之前）
        if (jsonStart > recognitionStart) {
            return trimmed.substring(recognitionStart, jsonStart).trim();
        }
        return null;
    }

    private MockEndpointItem parseEndpointNode(JsonNode node, String fallbackTitle) {
        if (node == null) {
            logger.warn("parseEndpointNode: node is null");
            return null;
        }
        if (!node.isObject()) {
            logger.warn("parseEndpointNode: node is not an object. Node type: {}, Node value: {}", 
                node.getNodeType(), node.toString().length() > 200 ? node.toString().substring(0, 200) + "..." : node.toString());
            return null;
        }
        
        // 打印节点的所有字段，用于调试
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        logger.debug("parseEndpointNode: node fields: {}", String.join(", ", fields));
        
        MockEndpointItem item = new MockEndpointItem();
        item.setTitle(textOr(node.path("title"), fallbackTitle));
        item.setMethod(textOr(node.path("method"), ""));
        
        // 解析并修复 requestExample（自动修复字符串化的 JSON）
        JsonNode requestExample = node.path("requestExample");
        if (!requestExample.isMissingNode()) {
            item.setRequestExample(fixStringifiedJson(requestExample));
        }
        
        // 解析并修复 responseExample（自动修复字符串化的 JSON）
        JsonNode responseExample = node.path("responseExample");
        if (!responseExample.isMissingNode()) {
            item.setResponseExample(fixStringifiedJson(responseExample));
        }
        
        // 解析并修复 errorResponseExample（自动修复字符串化的 JSON）
        JsonNode errorResponseExample = node.path("errorResponseExample");
        if (!errorResponseExample.isMissingNode()) {
            item.setErrorResponseExample(fixStringifiedJson(errorResponseExample));
        }
        
        JsonNode errorStatus = node.path("errorHttpStatus");
        if (errorStatus != null && errorStatus.isNumber()) {
            item.setErrorHttpStatus(errorStatus.intValue());
        } else if (errorStatus != null && errorStatus.isTextual()) {
            try {
                item.setErrorHttpStatus(Integer.parseInt(errorStatus.asText().trim()));
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        JsonNode delayMs = node.path("responseDelayMs");
        if (delayMs != null && delayMs.isNumber()) {
            item.setResponseDelayMs(delayMs.intValue());
        } else if (delayMs != null && delayMs.isTextual()) {
            try {
                item.setResponseDelayMs(Integer.parseInt(delayMs.asText().trim()));
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        JsonNode required = node.path("requiredFields");
        if (required.isArray()) {
            List<String> requiredFields = new ArrayList<>();
            for (JsonNode field : required) {
                if (field.isTextual() && !field.asText().trim().isEmpty()) {
                    String fieldPath = field.asText().trim();
                    // 修复路径格式：response.headers.xxx -> headers.xxx, response.body.xxx -> body.xxx
                    if (fieldPath.startsWith("response.headers.")) {
                        fieldPath = fieldPath.replace("response.headers.", "headers.");
                    } else if (fieldPath.startsWith("response.body.")) {
                        fieldPath = fieldPath.replace("response.body.", "body.");
                    }
                    requiredFields.add(fieldPath);
                }
            }
            item.setRequiredFields(requiredFields);
        }
        
        // 添加日志以便调试
        logger.info("parseEndpointNode: parsed item. title={}, method={}, hasRequest={}, hasResponse={}, hasError={}, requiredFields={}", 
                item.getTitle(), item.getMethod(), 
                item.getRequestExample() != null, item.getResponseExample() != null, 
                item.getErrorResponseExample() != null,
                item.getRequiredFields() != null ? item.getRequiredFields().size() : 0);
        
        return item;
    }

    /**
     * 修复字符串化的 JSON 对象/数组
     * 如果某个字段的值是字符串形式的 JSON，尝试解析为真正的 JSON 对象/数组
     */
    private JsonNode fixStringifiedJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return node;
        }
        
        if (node.isObject()) {
            ObjectNode fixed = objectMapper.createObjectNode();
            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (value.isTextual()) {
                    String text = value.asText().trim();
                    // 检查是否是字符串化的 JSON（以 [ 或 { 开头）
                    if ((text.startsWith("[") && text.endsWith("]")) || 
                        (text.startsWith("{") && text.endsWith("}"))) {
                        try {
                            // 尝试解析为 JSON
                            JsonNode parsed = objectMapper.readTree(text);
                            fixed.set(key, parsed);
                            logger.debug("Fixed stringified JSON for key: {}", key);
                            continue;
                        } catch (Exception e) {
                            // 解析失败，保持原值
                            logger.debug("Failed to parse stringified JSON for key: {}, keeping original", key);
                        }
                    }
                } else if (value.isObject() || value.isArray()) {
                    // 递归处理嵌套对象/数组
                    fixed.set(key, fixStringifiedJson(value));
                    continue;
                }
                
                // 保持原值
                fixed.set(key, value);
            }
            return fixed;
        } else if (node.isArray()) {
            ArrayNode fixed = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                if (element.isTextual()) {
                    String text = element.asText().trim();
                    if ((text.startsWith("[") && text.endsWith("]")) || 
                        (text.startsWith("{") && text.endsWith("}"))) {
                        try {
                            JsonNode parsed = objectMapper.readTree(text);
                            fixed.add(parsed);
                            logger.debug("Fixed stringified JSON in array");
                            continue;
                        } catch (Exception e) {
                            // 解析失败，保持原值
                        }
                    }
                } else if (element.isObject() || element.isArray()) {
                    fixed.add(fixStringifiedJson(element));
                    continue;
                }
                fixed.add(element);
            }
            return fixed;
        }
        
        return node;
    }

    private boolean hasMeaningfulContent(MockEndpointItem item) {
        if (item == null) {
            return false;
        }
        boolean hasMethod = item.getMethod() != null && !item.getMethod().trim().isEmpty();
        boolean hasRequired = item.getRequiredFields() != null && !item.getRequiredFields().isEmpty();
        boolean hasRequest = !isEffectivelyEmptyRequestExample(item.getRequestExample());
        boolean hasResponse = !isEffectivelyEmptyResponseExample(item.getResponseExample());
        boolean hasError = !isEmptyJson(item.getErrorResponseExample());
        boolean hasData = hasRequest || hasResponse || hasError || hasRequired;
        if (!hasRequest || !hasResponse) {
            return false;
        }
        boolean isGenericTitle = item.getTitle() == null
                || item.getTitle().trim().isEmpty()
                || isGenericTitle(item.getTitle());
        if (!hasData) {
            return false;
        }
        if (hasMethod && !hasData && isGenericTitle) {
            return false;
        }
        return hasMethod || hasRequired || hasRequest || hasResponse || hasError;
    }

    private boolean isEffectivelyEmptyRequestExample(JsonNode node) {
        if (isDeepEmptyJson(node)) {
            return true;
        }
        if (node != null && node.isObject()) {
            JsonNode headers = node.get("headers");
            JsonNode query = node.get("query");
            JsonNode body = node.get("body");
            if (isDeepEmptyJson(headers) && isDeepEmptyJson(query) && isDeepEmptyJson(body)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEffectivelyEmptyResponseExample(JsonNode node) {
        if (isDeepEmptyJson(node)) {
            return true;
        }
        if (node != null && node.isObject()) {
            JsonNode headers = node.get("headers");
            JsonNode body = node.get("body");
            if (isDeepEmptyJson(headers) && isDeepEmptyJson(body)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmptyJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return true;
        }
        if (node.isObject() || node.isArray()) {
            return node.size() == 0;
        }
        return false;
    }

    private boolean isDeepEmptyJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return true;
        }
        if (node.isTextual()) {
            return node.asText().trim().isEmpty();
        }
        if (node.isNumber() || node.isBoolean()) {
            return false;
        }
        if (node.isArray()) {
            if (node.size() == 0) {
                return true;
            }
            for (JsonNode child : node) {
                if (!isDeepEmptyJson(child)) {
                    return false;
                }
            }
            return true;
        }
        if (node.isObject()) {
            if (node.size() == 0) {
                return true;
            }
            java.util.Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                if (!isDeepEmptyJson(it.next())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String buildPreview(String text, int maxLines, int maxChars) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String[] lines = text.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        int lineCount = Math.min(lines.length, maxLines);
        for (int i = 0; i < lineCount; i++) {
            builder.append(lines[i]).append("\n");
            if (builder.length() >= maxChars) {
                break;
            }
        }
        String preview = builder.toString().trim();
        if (preview.length() > maxChars) {
            return preview.substring(0, maxChars) + "...";
        }
        return preview;
    }

    private boolean isGenericTitle(String title) {
        if (title == null) {
            return true;
        }
        String t = title.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty()
                || t.equals("document")
                || t.equals("api")
                || t.equals("interface")
                || t.contains("product introduction")
                || t.contains("terminologies")
                || t.contains("abbreviations")
                || t.contains("overview")
                || t.contains("note")
                || t.contains("introduction");
    }

    private String buildDedupeKey(MockEndpointItem item) {
        if (item == null) {
            return "";
        }
        String method = item.getMethod() == null ? "" : item.getMethod().trim().toUpperCase(Locale.ROOT);
        String path = item.getApiPath() == null ? "" : item.getApiPath().trim();
        if (!path.isEmpty() && !method.isEmpty()) {
            return method + ":" + path;
        }
        String title = item.getTitle() == null ? "" : normalizeTitleKey(item.getTitle());
        if (!title.isEmpty() && !method.isEmpty()) {
            return method + ":" + title;
        }
        return "";
    }

    private String normalizeTitleKey(String title) {
        String t = title.toLowerCase(Locale.ROOT).trim();
        return t.replaceAll("[^a-z0-9]+", "_");
    }

    private boolean hasApiKeyword(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        String t = title.toLowerCase(Locale.ROOT);
        return t.contains("api") || t.contains("接口");
    }

    private void normalizeTitle(MockEndpointItem item, String chunkText) {
        if (item == null) {
            return;
        }
        String current = item.getTitle() == null ? "" : item.getTitle().trim();
        String fromText = extractTitleFromText(chunkText);
        if (fromText != null && !fromText.isEmpty()) {
            item.setTitle(fromText);
            current = fromText;
        }
        if (looksLikeHeaderTitle(current)) {
            String apiPath = item.getApiPath();
            if (apiPath != null && !apiPath.isEmpty()) {
                String name = apiPath.substring(apiPath.lastIndexOf('/') + 1);
                if (!name.isEmpty()) {
                    item.setTitle("API " + name);
                }
            }
        }
    }

    private String extractTitleFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("#")) {
                trimmed = trimmed.replaceFirst("^#+\\s*", "").trim();
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("api name")) {
                String value = trimmed.replaceFirst("(?i)api name\\s*[:\\-]*\\s*", "").trim();
                if (!value.isEmpty()) {
                    return "API - " + value;
                }
            }
            if (lower.contains("api -") || lower.contains("api –") || lower.contains("api—")) {
                String value = trimmed.replaceFirst("(?i).*?api\\s*[\\-–—]\\s*", "").trim();
                if (!value.isEmpty()) {
                    return "API - " + value;
                }
            }
            if (lower.matches("^\\d+(\\.\\d+){0,3}\\s+api\\b.*")) {
                String value = trimmed.replaceFirst("^\\d+(\\.\\d+){0,3}\\s+", "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    private boolean looksLikeHeaderTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        String t = title.toLowerCase(Locale.ROOT);
        return t.contains("content-type")
                || t.contains("api_key")
                || t.contains("x-signature")
                || t.contains("x-auth")
                || t.contains("x-client")
                || t.contains("accept")
                || t.startsWith("api_key")
                || t.startsWith("x-");
    }

    private MockEndpointEntity toEntity(MockEndpointItem item) {
        MockEndpointEntity entity = new MockEndpointEntity();
        entity.setId(item.getId());
        entity.setTitle(item.getTitle());
        entity.setMethod(item.getMethod());
        entity.setMockUrl(item.getMockUrl());
        entity.setRaw(item.getRaw());
        entity.setSourceFileId(item.getSourceFileId());
        entity.setSourceFileName(item.getSourceFileName());
        entity.setSourceFileUrl(item.getSourceFileUrl());
        entity.setSceneId(item.getSceneId());
        entity.setSceneName(item.getSceneName());
        entity.setErrorHttpStatus(item.getErrorHttpStatus());
        entity.setResponseDelayMs(item.getResponseDelayMs());
        entity.setApiPath(item.getApiPath());
        entity.setRequestExample(stringify(item.getRequestExample()));
        entity.setResponseExample(stringify(item.getResponseExample()));
        entity.setErrorResponseExample(stringify(item.getErrorResponseExample()));
        entity.setRequiredFields(stringify(item.getRequiredFields()));
        return entity;
    }

    private MockEndpointItem toItem(MockEndpointEntity entity) {
        MockEndpointItem item = new MockEndpointItem();
        item.setId(entity.getId());
        item.setTitle(entity.getTitle());
        item.setMethod(entity.getMethod());
        item.setMockUrl(entity.getMockUrl());
        item.setRaw(entity.getRaw());
        item.setSourceFileId(entity.getSourceFileId());
        item.setSourceFileName(entity.getSourceFileName());
        item.setSourceFileUrl(entity.getSourceFileUrl());
        item.setSceneId(entity.getSceneId());
        item.setSceneName(entity.getSceneName());
        item.setErrorHttpStatus(entity.getErrorHttpStatus());
        item.setResponseDelayMs(entity.getResponseDelayMs());
        item.setApiPath(entity.getApiPath());
        item.setRequestExample(parseJson(entity.getRequestExample()));
        item.setResponseExample(parseJson(entity.getResponseExample()));
        item.setErrorResponseExample(parseJson(entity.getErrorResponseExample()));
        item.setRequiredFields(parseList(entity.getRequiredFields()));
        return item;
    }

    private String extractApiPath(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        List<String> candidates = new ArrayList<>();
        List<String> urlLineCandidates = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            java.util.regex.Matcher matcher = urlPattern.matcher(line);
            while (matcher.find()) {
                String raw = matcher.group();
                candidates.add(raw);
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.contains("resource url") || lower.contains("url") || lower.contains("path") || lower.contains("endpoint")) {
                    urlLineCandidates.add(raw);
                }
            }
        }
        String picked = pickBestApiPath(urlLineCandidates.isEmpty() ? candidates : urlLineCandidates, text);
        return picked == null ? "" : picked;
    }

    private String normalizeApiPath(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                java.net.URI uri = java.net.URI.create(value);
                String path = uri.getPath();
                return path == null || path.trim().isEmpty() ? "" : path.trim();
            } catch (Exception ex) {
                // fallback below
            }
        }
        int idx = value.indexOf("://");
        if (idx >= 0) {
            int slash = value.indexOf('/', idx + 3);
            if (slash >= 0) {
                value = value.substring(slash);
            }
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return value;
    }

    private String pickBestApiPath(List<String> rawCandidates, String fullText) {
        if (rawCandidates == null || rawCandidates.isEmpty()) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String raw : rawCandidates) {
            String path = normalizeApiPath(raw);
            if (path.isEmpty()) {
                continue;
            }
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.equals("/json") || lower.equals("/a") || lower.equals("/n/a") || lower.equals("/na")) {
                continue;
            }
            if (lower.contains("application/json") || lower.contains("text/plain")) {
                continue;
            }
            normalized.add(path);
        }
        if (normalized.isEmpty()) {
            return "";
        }
        String best = "";
        int bestScore = Integer.MIN_VALUE;
        for (String path : normalized) {
            int score = path.length();
            int segments = path.split("/").length - 1;
            if (segments >= 2) {
                score += 8;
            }
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.contains("/v1") || lower.contains("/v2") || lower.contains("/api")) {
                score += 6;
            }
            if (lower.contains("json")) {
                score -= 5;
            }
            if (score > bestScore) {
                bestScore = score;
                best = path;
            }
        }
        return best;
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            return null;
        }
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (IOException ex) {
            return null;
        }
    }

    private List<String> parseList(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    if (item.isTextual()) {
                        list.add(item.asText());
                    }
                }
            }
        } catch (IOException ex) {
            return list;
        }
        return list;
    }

    private String buildRequestSignature(MockEndpointItem item, JsonNode requestBody) {
        if (requestBody == null || item == null) {
            return "";
        }
        List<String> keyPaths = new ArrayList<>();
        if (item.getRequiredFields() != null && !item.getRequiredFields().isEmpty()) {
            keyPaths.addAll(item.getRequiredFields());
        }
        Map<String, JsonNode> requestKeyMap = new LinkedHashMap<>();
        collectRequestFields(requestBody, requestKeyMap);

        List<String> commonKeys = new ArrayList<>();
        commonKeys.add("id");
        commonKeys.add("msisdn");
        commonKeys.add("orderNo");
        commonKeys.add("order_no");
        commonKeys.add("orderId");
        commonKeys.add("customerId");
        commonKeys.add("accountId");

        for (String key : commonKeys) {
            String lower = key.toLowerCase(Locale.ROOT);
            if (requestKeyMap.containsKey(lower) && !containsKeyPath(keyPaths, lower)) {
                keyPaths.add(lower);
            }
        }

        if (keyPaths.isEmpty()) {
            return sha256(normalizeValue(requestBody));
        }

        List<String> parts = new ArrayList<>();
        for (String path : keyPaths) {
            JsonNode value = path.contains(".") ? getNodeByPath(requestBody, path) : requestKeyMap.get(path.toLowerCase(Locale.ROOT));
            if (value != null && !value.isMissingNode()) {
                parts.add(path + "=" + normalizeValue(value));
            }
        }
        if (parts.isEmpty()) {
            return sha256(normalizeValue(requestBody));
        }
        parts.sort(String::compareTo);
        return sha256(String.join("&", parts));
    }

    private void collectRequestFields(JsonNode node, Map<String, JsonNode> map) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            java.util.Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                JsonNode value = node.get(key);
                String lower = key.toLowerCase(Locale.ROOT);
                if (!map.containsKey(lower)) {
                    map.put(lower, value);
                }
                collectRequestFields(value, map);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectRequestFields(child, map);
            }
        }
    }

    private JsonNode getNodeByPath(JsonNode node, String path) {
        if (node == null || path == null || path.trim().isEmpty()) {
            return null;
        }
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return null;
            }
            current = current.get(part);
        }
        return current;
    }

    private boolean containsKeyPath(List<String> keyPaths, String key) {
        for (String path : keyPaths) {
            if (path.equalsIgnoreCase(key) || path.toLowerCase(Locale.ROOT).endsWith("." + key)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeValue(JsonNode value) {
        if (value.isTextual()) {
            return value.asText().trim();
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return value.toString();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
        } catch (Exception ex) {
            return input;
        }
    }

    private JsonNode buildDynamicResponse(JsonNode responseExample, JsonNode requestBody) {
        if (responseExample == null) {
            return null;
        }
        JsonNode response = responseExample.deepCopy();
        Map<String, JsonNode> requestMap = new LinkedHashMap<>();
        collectRequestFields(requestBody, requestMap);
        applyDynamicValues(response, requestMap);
        return response;
    }

    private void applyDynamicValues(JsonNode node, Map<String, JsonNode> requestMap) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            java.util.Iterator<String> it = obj.fieldNames();
            List<String> fields = new ArrayList<>();
            while (it.hasNext()) {
                fields.add(it.next());
            }
            for (String key : fields) {
                JsonNode value = obj.get(key);
                String lower = key.toLowerCase(Locale.ROOT);
                if (requestMap.containsKey(lower) && !value.isContainerNode()) {
                    obj.set(key, requestMap.get(lower));
                } else {
                    applyDynamicValues(value, requestMap);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                applyDynamicValues(child, requestMap);
            }
        }
    }

    private void persistResponseCache(String mockId,
                                      String signature,
                                      JsonNode requestBody,
                                      JsonNode responseBody) throws IOException {
        if (mockId == null || signature == null || signature.isEmpty() || responseBody == null) {
            return;
        }
        MockResponseCacheEntity entity = new MockResponseCacheEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setMockId(mockId);
        entity.setRequestSignature(signature);
        entity.setRequestBody(objectMapper.writeValueAsString(requestBody));
        entity.setResponseBody(objectMapper.writeValueAsString(responseBody));
        responseCacheRepository.save(entity);
    }

    public void logOperation(String type, String mockId, String sourceFileName, String message) {
        MockOperationLogEntity log = new MockOperationLogEntity();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setType(type);
        log.setMockId(mockId);
        log.setSourceFileName(sourceFileName);
        log.setMessage(message);
        logRepository.save(log);
    }

    private JsonNode readJsonLoosely(String raw) {
        String trimmed = raw.trim();
        try {
            return objectMapper.readTree(trimmed);
        } catch (IOException ex) {
            logger.debug("readJsonLoosely: direct parse failed, trying to extract JSON. Error: {}", ex.getMessage());
            // Try to extract JSON array first (for file URL mode with multiple APIs)
            String extracted = extractJsonArray(trimmed);
            if (extracted != null) {
                logger.debug("readJsonLoosely: extracted JSON array length: {}", extracted.length());
                try {
                    JsonNode node = objectMapper.readTree(extracted);
                    if (node.isArray()) {
                        return node;
                    }
                } catch (IOException inner) {
                    logger.debug("readJsonLoosely: extracted array parse failed, trying object. Error: {}", inner.getMessage());
                }
            }
            // Try to extract JSON object (for single API or chunk mode)
            extracted = extractJsonObject(trimmed);
            if (extracted == null) {
                logger.warn("readJsonLoosely: extractJsonObject returned null. Raw preview: {}", 
                    trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed);
                return null;
            }
            logger.debug("readJsonLoosely: extracted JSON length: {}", extracted.length());
            try {
                return objectMapper.readTree(extracted);
            } catch (IOException inner) {
                logger.warn("readJsonLoosely: extracted JSON parse failed. Error: {}, Extracted preview: {}", 
                    inner.getMessage(), extracted.length() > 300 ? extracted.substring(0, 300) + "..." : extracted);
                return null;
            }
        }
    }

    /**
     * 提取JSON数组（从 '[' 开始，匹配到对应的 ']'）
     * 优先查找包含接口对象的数组（包含 "title" 字段），而不是 requiredFields 数组
     */
    private String extractJsonArray(String text) {
        String cleaned = stripCodeFence(text);
        
        // 查找所有可能的 JSON 数组，选择最大的、包含 "title" 字段的数组
        List<ArrayCandidate> candidates = new ArrayList<>();
        int searchStart = 0;
        
        while (true) {
            int start = cleaned.indexOf('[', searchStart);
            if (start < 0) {
                break;
            }
            
            // 提取这个数组
            int depth = 0;
            int end = -1;
            boolean inString = false;
            char stringChar = 0;
            for (int i = start; i < cleaned.length(); i++) {
                char ch = cleaned.charAt(i);
                // Handle string literals to avoid counting brackets inside strings
                if (!inString && (ch == '"' || ch == '\'')) {
                    inString = true;
                    stringChar = ch;
                } else if (inString && ch == stringChar) {
                    // Check if it's escaped
                    if (i == 0 || cleaned.charAt(i - 1) != '\\') {
                        inString = false;
                    }
                }
                if (!inString) {
                    if (ch == '[') {
                        depth++;
                    } else if (ch == ']') {
                        depth--;
                        if (depth == 0) {
                            end = i;
                            break;
                        }
                    }
                }
            }
            
            if (end > start) {
                String arrayText = cleaned.substring(start, end + 1);
                // 检查这个数组是否包含 "title" 字段（接口数组的特征）
                boolean containsTitle = arrayText.contains("\"title\"") || arrayText.contains("'title'");
                int length = arrayText.length();
                candidates.add(new ArrayCandidate(start, end, arrayText, containsTitle, length));
                logger.debug("extractJsonArray: Found array candidate at [{}, {}], length={}, containsTitle={}", 
                    start, end, length, containsTitle);
            } else {
                // JSON 可能被截断，尝试提取到最后一个完整的对象
                // 从 start 开始，向后查找最后一个完整的对象
                int lastCompleteObjectEnd = findLastCompleteObject(cleaned, start);
                if (lastCompleteObjectEnd > start) {
                    // 尝试构造一个有效的数组（即使不完整）
                    String partialArray = cleaned.substring(start, lastCompleteObjectEnd + 1);
                    // 如果最后一个字符不是 ]，尝试添加 ]
                    if (!partialArray.trim().endsWith("]")) {
                        // 查找最后一个完整的对象
                        int lastBrace = partialArray.lastIndexOf('}');
                        if (lastBrace > 0) {
                            partialArray = partialArray.substring(0, lastBrace + 1);
                            // 尝试添加 ] 使其成为有效的数组
                            partialArray = "[" + partialArray.substring(1) + "]";
                        }
                    }
                    boolean containsTitle = partialArray.contains("\"title\"") || partialArray.contains("'title'");
                    candidates.add(new ArrayCandidate(start, lastCompleteObjectEnd, partialArray, containsTitle, partialArray.length()));
                    logger.warn("extractJsonArray: Found potentially truncated array at [{}, {}], attempting to extract partial array", 
                        start, lastCompleteObjectEnd);
                }
            }
            
            searchStart = start + 1;
        }
        
        if (candidates.isEmpty()) {
            logger.debug("extractJsonArray: No '[' found in text. Text length: {}, Preview: {}", 
                cleaned.length(), cleaned.length() > 200 ? cleaned.substring(0, 200) + "..." : cleaned);
            return null;
        }
        
        // 优先选择包含 "title" 字段的数组，如果多个，选择最大的
        ArrayCandidate best = null;
        for (ArrayCandidate candidate : candidates) {
            if (candidate.containsTitle) {
                if (best == null || candidate.length > best.length) {
                    best = candidate;
                }
            }
        }
        
        // 如果没有找到包含 "title" 的数组，选择最大的数组
        if (best == null) {
            for (ArrayCandidate candidate : candidates) {
                if (best == null || candidate.length > best.length) {
                    best = candidate;
                }
            }
        }
        
        if (best != null) {
            logger.info("extractJsonArray: Selected array at [{}, {}], length={}, containsTitle={}", 
                best.start, best.end, best.length, best.containsTitle);
            return best.text;
        }
        
        return null;
    }
    
    /**
     * 查找最后一个完整的 JSON 对象（用于处理被截断的 JSON）
     * 从指定位置开始，向后查找最后一个完整的对象（以 } 结尾且括号匹配）
     */
    private int findLastCompleteObject(String text, int start) {
        int depth = 0;
        int lastCompleteEnd = start;
        boolean inString = false;
        char stringChar = 0;
        boolean foundFirstBrace = false;
        
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            
            // Handle string literals
            if (!inString && (ch == '"' || ch == '\'')) {
                inString = true;
                stringChar = ch;
            } else if (inString && ch == stringChar) {
                if (i == 0 || text.charAt(i - 1) != '\\') {
                    inString = false;
                }
            }
            
            if (!inString) {
                if (ch == '{' || ch == '[') {
                    if (ch == '{') {
                        foundFirstBrace = true;
                    }
                    depth++;
                } else if (ch == '}' || ch == ']') {
                    depth--;
                    if (depth == 0 && ch == '}' && foundFirstBrace) {
                        // 找到了一个完整的对象（从 { 开始，到 } 结束，且括号匹配）
                        lastCompleteEnd = i;
                    }
                }
            }
        }
        
        // 如果没有找到完整的对象，返回 start（表示没有找到）
        if (lastCompleteEnd == start && !foundFirstBrace) {
            return start;
        }
        
        return lastCompleteEnd;
    }
    
    private static class ArrayCandidate {
        final int start;
        final int end;
        final String text;
        final boolean containsTitle;
        final int length;
        
        ArrayCandidate(int start, int end, String text, boolean containsTitle, int length) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.containsTitle = containsTitle;
            this.length = length;
        }
    }

    /**
     * 提取JSON对象（从 '{' 开始，匹配到对应的 '}'）
     */
    private String extractJsonObject(String text) {
        String cleaned = stripCodeFence(text);
        int start = cleaned.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        int end = -1;
        boolean inString = false;
        char stringChar = 0;
        for (int i = start; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            // Handle string literals to avoid counting braces inside strings
            if (!inString && (ch == '"' || ch == '\'')) {
                inString = true;
                stringChar = ch;
            } else if (inString && ch == stringChar) {
                // Check if it's escaped
                if (i == 0 || cleaned.charAt(i - 1) != '\\') {
                    inString = false;
                }
            }
            if (!inString) {
                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }
        if (end > start) {
            return cleaned.substring(start, end + 1);
        }
        return null;
    }

    private String stripCodeFence(String text) {
        String cleaned = text;
        if (cleaned.startsWith("```")) {
            int firstLineEnd = cleaned.indexOf('\n');
            if (firstLineEnd >= 0) {
                cleaned = cleaned.substring(firstLineEnd + 1);
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String callZhipu(String prompt) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "disabled");

        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("Zhipu API Request - URL: {}, Model: {}", API_URL, model);
        logger.info("Zhipu API Request Body: {}", requestBody);
        logger.debug("Zhipu API Request Prompt (first 500 chars): {}", 
                prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("Zhipu API Response - Status: {}", response.getStatusCode());
        logger.info("Zhipu API Response Body: {}", response.getBody());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode content = node.path("choices").path(0).path("message").path("content");
        String result = content.isMissingNode() ? response.getBody() : content.asText();
        logger.info("Zhipu API Response Content Length: {}", result != null ? result.length() : 0);
        return result;
    }

    /**
     * 专门用于第二步 JSON 转换的调用方法，使用更大的 max_tokens
     */
    private String callZhipuForJsonConversion(String prompt) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);
        body.put("temperature", temperature);
        // 第二步需要更大的 max_tokens，因为要生成完整的 JSON 数组（包含多个接口）
        // 默认 maxTokens 是 2048，这里增加到 4096
        int jsonConversionMaxTokens = Math.max(maxTokens * 2, 4096);
        body.put("max_tokens", jsonConversionMaxTokens);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "disabled");

        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("Zhipu JSON Conversion API Request - URL: {}, Model: {}, MaxTokens: {}", API_URL, model, jsonConversionMaxTokens);
        logger.debug("Zhipu JSON Conversion API Request Prompt (first 500 chars): {}", 
                prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("Zhipu JSON Conversion API Response - Status: {}", response.getStatusCode());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode choice = node.path("choices").path(0);
        String finishReason = choice.path("finish_reason").asText("");
        JsonNode content = choice.path("message").path("content");
        String result = content.isMissingNode() ? response.getBody() : content.asText();
        
        logger.info("Zhipu JSON Conversion API Response - FinishReason: {}, Content Length: {}", 
                finishReason, result != null ? result.length() : 0);
        
        if ("length".equals(finishReason)) {
            logger.warn("JSON conversion response was truncated due to max_tokens limit. Consider increasing max_tokens or simplifying the prompt.");
        }
        
        return result;
    }

    /**
     * 使用文件URL调用新模型API
     */
    private String callZhipuWithFileUrl(String fileUrl, String systemPrompt, String userPrompt) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

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
        fileUrlObj.put("url",fileUrl );
        
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
        logger.info("Zhipu File URL API Request - URL: {}, Model: {}", API_URL, fileModel);
        logger.info("Zhipu File URL API Request - FileUrl: {}", fileUrl);
        logger.info("Zhipu File URL API Request Body: {}", requestBody);
        logger.debug("Zhipu File URL API Request - System Prompt (first 500 chars): {}", 
                systemPrompt != null && systemPrompt.length() > 500 ? systemPrompt.substring(0, 500) + "..." : systemPrompt);
        logger.debug("Zhipu File URL API Request - User Prompt (first 500 chars): {}", 
                userPrompt != null && userPrompt.length() > 500 ? userPrompt.substring(0, 500) + "..." : userPrompt);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("Zhipu File URL API Response - Status: {}", response.getStatusCode());
        logger.info("Zhipu File URL API Response Body: {}", response.getBody());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("Zhipu file URL API error: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode choice = node.path("choices").path(0);
        JsonNode message = choice.path("message");
        
        // 调试：打印message对象的所有字段
        if (message.isObject()) {
            java.util.Iterator<String> fieldNames = message.fieldNames();
            List<String> fields = new ArrayList<>();
            while (fieldNames.hasNext()) {
                fields.add(fieldNames.next());
            }
            logger.debug("Message fields: {}", String.join(", ", fields));
        }
        
        String result = null;
        
        // 优先使用 message.content（包含阶段一文本 + 阶段二JSON数组）
        // 注意：在启用thinking模式时，content包含最终输出，reasoning_content只包含思考过程
        JsonNode content = message.path("content");
        if (!content.isMissingNode() && content.isTextual()) {
            String contentText = content.asText();
            if (contentText != null && !contentText.trim().isEmpty()) {
                result = contentText;
                logger.info("Using message.content as response content. Length: {}", result.length());
            } else {
                logger.debug("message.content exists but is empty");
            }
        } else {
            logger.debug("message.content is missing or not textual");
        }
        
        // 如果 content 为空，尝试使用 reasoning_content（某些情况下可能在这里）
        if ((result == null || result.trim().isEmpty())) {
            JsonNode reasoningContent = message.path("reasoning_content");
            if (!reasoningContent.isMissingNode()) {
                if (reasoningContent.isTextual()) {
                    result = reasoningContent.asText();
                    logger.info("Using message.reasoning_content as response content. Length: {}", result != null ? result.length() : 0);
                } else {
                    logger.warn("message.reasoning_content exists but is not textual. Type: {}", reasoningContent.getNodeType());
                }
            } else {
                logger.debug("message.reasoning_content is missing");
            }
        }
        
        // 如果还是为空，尝试从 choice 对象中获取 reasoning_content（某些API版本可能在这里）
        if ((result == null || result.trim().isEmpty())) {
            JsonNode choiceReasoning = choice.path("reasoning_content");
            if (!choiceReasoning.isMissingNode() && choiceReasoning.isTextual()) {
                result = choiceReasoning.asText();
                logger.info("Using choice.reasoning_content as response content. Length: {}", result != null ? result.length() : 0);
            }
        }
        
        // 如果还是为空，使用整个响应体（不应该发生）
        if (result == null || result.trim().isEmpty()) {
            logger.warn("Content and reasoning_content are both empty, using full response body");
            result = response.getBody();
        }
        
        logger.info("Zhipu File URL API Response Content Length: {}", result != null ? result.length() : 0);
        
        // 打印完整响应内容用于调试
        if (result != null) {
            logger.info("=== Zhipu File URL API Full Response Content ===");
            logger.info("Response Content (first 2000 chars): {}", 
                    result.length() > 2000 ? result.substring(0, 2000) + "..." : result);
            if (result.length() > 2000) {
                logger.info("Response Content (last 500 chars): {}", result.substring(result.length() - 500));
            }
            // 检查是否包含JSON数组
            int jsonStart = Math.max(result.indexOf('['), result.indexOf('{'));
            if (jsonStart >= 0) {
                logger.info("Found JSON at position: {}, JSON preview: {}", 
                        jsonStart, result.substring(jsonStart, Math.min(jsonStart + 500, result.length())));
            } else {
                logger.warn("No JSON array or object found in response! Response only contains stage 1 text.");
            }
        }
        
        return result;
    }

    private String callZhipuWithMessages(java.util.List<Object> messages) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        ObjectNode body = objectMapper.createObjectNode();
        // 使用支持视觉的模型（如果配置了 vision 模型，否则使用原模型）
        String visionModel = model.contains("vision") || model.contains("v") ? model : 
                            (model.startsWith("glm-4") ? "glm-4v" : model);
        body.put("model", visionModel);
        ArrayNode messagesArray = body.putArray("messages");
        
        // 构建系统提示词
        ObjectNode systemMsg = messagesArray.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildManualPromptWithImages());
        
        // 处理用户消息（支持文本和多模态）
        for (Object msgObj : messages) {
            ObjectNode userMsg = messagesArray.addObject();
            userMsg.put("role", "user");
            
            if (msgObj instanceof String) {
                // 纯文本消息
                userMsg.put("content", (String) msgObj);
                logger.debug("Added text message: {}", ((String) msgObj).substring(0, Math.min(100, ((String) msgObj).length())));
            } else if (msgObj instanceof com.fasterxml.jackson.databind.JsonNode) {
                // 多模态消息（文本+图片）
                JsonNode contentNode = (JsonNode) msgObj;
                if (contentNode.isArray()) {
                    userMsg.set("content", contentNode);
                    logger.info("Added multimodal message with {} items", contentNode.size());
                    // 记录每个内容项的类型
                    for (int i = 0; i < contentNode.size(); i++) {
                        JsonNode item = contentNode.get(i);
                        if (item.has("type")) {
                            String type = item.get("type").asText();
                            logger.info("Content item {}: type={}", i, type);
                            if ("image_url".equals(type) && item.has("image_url")) {
                                String url = item.get("image_url").get("url").asText();
                                logger.info("Image URL length: {}", url.length());
                            }
                        }
                    }
                } else {
                    userMsg.put("content", contentNode.asText());
                    logger.debug("Added text from JsonNode: {}", contentNode.asText().substring(0, Math.min(100, contentNode.asText().length())));
                }
            } else {
                // 尝试转换为 JSON
                try {
                    JsonNode contentNode = objectMapper.valueToTree(msgObj);
                    userMsg.set("content", contentNode);
                    logger.info("Converted message to JsonNode: isArray={}", contentNode.isArray());
                } catch (Exception e) {
                    logger.warn("Failed to convert message to JsonNode: {}", e.getMessage());
                    userMsg.put("content", String.valueOf(msgObj));
                }
            }
        }
        
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        ObjectNode thinking = body.putObject("thinking");
        thinking.put("type", "disabled");

        String requestBody = objectMapper.writeValueAsString(body);
        logger.info("Zhipu Messages API Request - URL: {}, Model: {}", API_URL, visionModel);
        logger.info("Zhipu Messages API Request Body: {}", requestBody);
        logger.debug("Zhipu Messages API request body length: {}", requestBody.length());
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        
        logger.info("Zhipu Messages API Response - Status: {}", response.getStatusCode());
        logger.info("Zhipu Messages API Response Body: {}", response.getBody());
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("Zhipu API error: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Zhipu API error: " + response.getStatusCode() + ", body: " + response.getBody());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode content = node.path("choices").path(0).path("message").path("content");
        String result = content.isMissingNode() ? response.getBody() : content.asText();
        logger.info("Zhipu Messages API Response Content Length: {}", result != null ? result.length() : 0);
        return result;
    }

    private String buildManualPromptWithImages() {
        return "你是接口Mock结构生成助手。请根据用户提供的文本描述或截图识别接口信息，生成接口结构。\n"
                + "重要：如果用户提供了截图，请仔细识别截图中的接口文档内容，包括：\n"
                + "- 接口名称/标题（从 API 路径或标题中提取）\n"
                + "- 请求方式（GET/POST，从 HTTP Method 或 Operation 中识别）\n"
                + "- 请求头（Request Headers / HTTP Header 表格中的所有字段）\n"
                + "- 请求参数（Query Parameters / Request Body 表格中的所有字段）\n"
                + "- 响应头（Response Headers / HTTP Header 表格中的所有字段）\n"
                + "- 响应体（Response Body 表格中的所有字段，包括数组结构）\n"
                + "- 错误响应示例\n"
                + "- 必填字段标识（M/O 列中标记为 M 的字段）\n\n"
                + "关键要求：\n"
                + "1. 只能输出严格JSON，不要包含任何解释文字、markdown代码块标记。\n"
                + "2. 必须识别表格中的所有字段，不要遗漏任何字段。\n"
                + "3. 如果表格中有 \"HTTP Header\" 或 \"Request Headers\" 部分，这些字段必须放入 headers。\n"
                + "4. 如果表格中有 \"Request\" 或 \"Request Body\" 部分，这些字段必须放入 body（GET 请求放入 query）。\n"
                + "5. 如果表格中有 \"Response\" 或 \"Response Body\" 部分，这些字段必须放入 body。\n"
                + "6. 如果表格中有数组结构（如 plans(Array)），必须正确识别为数组，数组元素必须包含所有子字段。\n"
                + "7. requestExample 必须拆分为 headers/query/body 三部分：\n"
                + "   - HTTP Header / Request Headers 表格中的字段 -> headers\n"
                + "   - GET 请求的请求参数 -> query\n"
                + "   - POST 请求的请求参数 -> body\n"
                + "8. responseExample 必须拆分为 headers/body 两部分：\n"
                + "   - HTTP Header / Response Headers 表格中的字段 -> headers\n"
                + "   - Response Body 表格中的字段 -> body\n"
                + "9. requiredFields 使用点号路径并带前缀 headers./query./body.，只包含 M/O 列中标记为 M 的字段。\n"
                + "10. method 只能是 GET 或 POST，从文档中的 HTTP Method / Operation 识别。\n"
                + "11. 字段值必须根据 REMARKS 列中的示例生成，如果没有示例，根据字段名和类型生成合理值。\n"
                + "12. 数组字段必须生成完整的数组结构，不能是字符串 \"[object Object]\"。\n"
                + "13. 所有 JSON 值必须是有效的 JSON 类型（对象、数组、字符串、数字、布尔值），不能是字符串化的对象或数组。\n"
                + "14. 特别注意：数组和对象必须直接作为 JSON 值，不要用引号包裹，不要转义。例如：\n"
                + "    正确：\"plans\": [{\"planId\": \"PLAN001\"}]\n"
                + "    错误：\"plans\": \"[{\\\"planId\\\": \\\"PLAN001\\\"}]\" （不要这样做）\n"
                + "15. 结构固定，不要新增字段。\n\n"
                + "输出JSON结构（示例）：\n"
                + "{\n"
                + "  \"title\": \"Get Applicable Plans\",\n"
                + "  \"method\": \"GET\",\n"
                + "  \"requestExample\": {\n"
                + "    \"headers\": {\n"
                + "      \"SourceSystemID\": \"CRM\",\n"
                + "      \"ReferenceID\": \"KSK20120530221525000839\",\n"
                + "      \"ChannelMedia\": \"WEB\",\n"
                + "      \"GUID\": \"d3979e33-2252-465a-924c-ef175429637e\"\n"
                + "    },\n"
                + "    \"query\": {\n"
                + "      \"primarylmei\": \"89602342343423\",\n"
                + "      \"primarylmsi\": \"123456789012345\"\n"
                + "    },\n"
                + "    \"body\": {}\n"
                + "  },\n"
                + "  \"responseExample\": {\n"
                + "    \"headers\": {\n"
                + "      \"SourceSystemID\": \"CRM\",\n"
                + "      \"ReferenceID\": \"KSK20120530221525000839\",\n"
                + "      \"ChannelMedia\": \"WEB\"\n"
                + "    },\n"
                + "    \"body\": {\n"
                + "      \"BusinessUnit\": \"Digi\",\n"
                + "      \"OperationResult\": \"SUCCESS\",\n"
                + "      \"plans\": [\n"
                + "        {\n"
                + "          \"planId\": \"PLAN001\",\n"
                + "          \"planLabel\": \"Plan A\",\n"
                + "          \"planValue\": \"100\",\n"
                + "          \"maxDevices\": \"5\",\n"
                + "          \"profileType\": \"Type1\",\n"
                + "          \"planDetails\": \"Details of Plan A\"\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"errorResponseExample\": {},\n"
                + "  \"requiredFields\": [\"headers.SourceSystemID\", \"headers.ReferenceID\", \"headers.ChannelMedia\"],\n"
                + "  \"errorHttpStatus\": 400\n"
                + "}";
    }

    private String buildPrompt(String chunkText) {
        return "你是接口Mock数据生成助手。请根据输入的接口文档片段生成：请求参数示例、响应参数示例、错误响应示例，并给出必填字段清单。\n"
                + "要求：\n"
                + "1. 只能输出严格JSON，不要包含任何解释文字。\n"
                + "2. 优先使用文档里的 Sample Request / Sample Response / Error Response 示例。\n"
                + "3. 如果缺少示例，按字段语义生成合理示例。\n"
                + "4. 请求示例必须拆分为 headers / query / body 三部分：\n"
                + "   - **文档中所有 Request Headers / HTTP Header / Header 表格中的字段必须全部提取到 requestExample.headers，包括字段名和对应的值/示例值，不要遗漏任何字段**\n"
                + "   - 文档中出现 Query Params / URL Params / Query 的字段放入 requestExample.query。\n"
                + "   - 文档中出现 Request Body / Body 的字段放入 requestExample.body。\n"
                + "   - 不要把 header 字段放进 body。\n"
                + "   - 特别注意：如果文档中有 Request Headers 表格，必须将所有字段提取到 headers 中\n"
                + "5. 响应示例必须拆分为 headers / body 两部分：\n"
                + "   - **文档中所有 Response Headers / HTTP Header / Header 表格中的字段必须全部提取到 responseExample.headers，包括字段名和对应的值/示例值，不要遗漏任何字段**\n"
                + "   - 其他响应字段放入 responseExample.body。\n"
                + "   - 特别注意：如果文档中有 Response Headers 表格，必须将所有字段提取到 headers 中\n"
                + "6. 错误响应示例也应该包含 headers 和 body 两部分（如果文档中有 Error Response Headers，必须提取）\n"
                + "7. method 必须是 GET 或 POST，来自文档里的 Request Verb / Method / 请求方式。\n"
                + "8. 如果片段内存在多个接口，只输出当前标题对应的一个接口。\n"
                + "9. requiredFields 使用点号路径，并带上前缀，例如 headers.SourceSystemID, query.msisdn, body.orderId。\n"
                + "10. 只输出下面固定结构，不要增加字段。\n"
                + "11. 关键：必须从文档中提取所有请求头和响应头字段，包括文档中明确列出的所有 Header 字段，不要遗漏任何字段。\n\n"
                + "输出JSON结构：\n"
                + "{\n"
                + "  \"title\": \"\",\n"
                + "  \"method\": \"\",\n"
                + "  \"requestExample\": {\n"
                + "    \"headers\": {},\n"
                + "    \"query\": {},\n"
                + "    \"body\": {}\n"
                + "  },\n"
                + "  \"responseExample\": {\n"
                + "    \"headers\": {},\n"
                + "    \"body\": {}\n"
                + "  },\n"
                + "  \"errorResponseExample\": {},\n"
                + "  \"requiredFields\": []\n"
                + "}\n\n"
                + "接口文档片段：\n"
                + chunkText;
    }

    /**
     * 构建文件URL模式的提示词（支持多API场景）
     */
    /**
     * 构建文件URL模式的System Prompt
     */
    private String buildFileUrlSystemPrompt() {
        return "你是一个【Integration Design Document（IDD）接口识别引擎】，\n" +
                "用于从 Telco / ESB / CRM / Camel / Fuse 类型的设计文档中\n" +
                "识别真实存在的 API 接口定义。\n" +
                "\n" +
                "【最高优先级规则（Hard Rules）】\n" +
                "1. 只允许基于用户提供的接口文档原文进行信息提取\n" +
                "2. 严禁编造、补充、猜测任何文档中不存在的接口、字段或参数\n" +
                "3. 不得合并、拆分、改写文档中原有的接口定义\n" +
                "4. 不得引入行业通用字段、默认字段或常识字段\n" +
                "5. 所有输出必须严格遵守 User Prompt 中定义的输出结构\n" +
                "6. 若某字段或接口在文档中未出现，必须明确标注为“文档未提供”\n" +
                "【合法归因授权（仅限 IDD 文档）】\n" +
                "以下行为不视为“猜测”，属于合法解析：\n" +
                "- 根据章节标题（如 Request / Response / Interface Message Specification）\n" +
                "  判断字段所属阶段\n" +
                "- 根据表格上下文中出现的分类（如 HTTP Header、Query Parameters、\n" +
                "  deviceList(Array)、OperationResult）确定字段归属\n" +
                "- 根据字段语义判断 headers / query / body 的归属，\n" +
                "  前提是字段名称和说明在文档中明确出现\n";
    }

    /**
     * 构建文件URL模式的User Prompt
     */
    private String buildFileUrlUserPrompt(String sceneKeywords) {
      return "【重要说明（必须遵守）】\n" +
              "\n" +
              "你将接收到的所有 file_url 文件，\n" +
              "即为【接口文档原文的完整内容】。\n" +
              "\n" +
              "这些文件：\n" +
              "- 是唯一且完整的接口文档事实来源\n" +
              "- 包含接口路径、请求方式、字段规范等关键信息\n" +
              "- 不得被视为示例、背景或参考资料\n" +
              "\n" +
              "你必须完整阅读并解析这些文件内容，\n" +
              "并严格基于文件中的信息执行接口识别。\n" +
              "\n" +
              "====================\n" +
              "【文档类型说明】\n" +
              "====================\n" +
              "这些文件为 Integration Design Document（IDD）类型文档，\n" +
              "属于 Telco / ESB / CRM / Camel / Fuse 风格接口设计文档。\n" +
              "\n" +
              "文档特点：\n" +
              "- 接口定义分散在章节与表格中\n" +
              "- Request / Response 以字段规范表形式给出\n" +
              "- Header / Query / Body 可能未显式拆分\n" +
              "\n" +
              "以下解析规则视为【合法归因】，不属于猜测：\n" +
              "- 根据章节标题（如 Request / Response / Interface Message Specification）\n" +
              "- 根据表格上下文（如 HTTP Header、Query Parameters、deviceList(Array)）\n" +
              "- 根据字段语义（如 SourceSystemID、ReferenceID 属于 Header）\n" +
              "\n" +
              "====================\n" +
              "【任务目标】\n" +
              "====================\n" +
              "从上述文件中识别【所有实际存在的 REST API 接口】。\n" +
              "\n" +
              "====================\n" +
              "【强制输出流程（必须遵守）】\n" +
              "====================\n" +
              "你必须按照以下顺序执行，不得跳过任何阶段：\n" +
              "\n" +
              "【阶段一：接口识别结果（文本输出）】\n" +
              "- 在生成任何 JSON 之前，必须先输出你从文档中【实际识别到的接口信息】\n" +
              "- 该阶段仅允许输出【纯文本】，用于人工校验\n" +
              "- 不得输出 JSON、示例代码或结构化数据\n" +
              "\n" +
              "【阶段二：生成 JSON 数组】\n" +
              "- **必须**在阶段一完成后立即输出JSON数组，不得停止或跳过\n" +
              "- 只能基于【阶段一已识别并输出的接口信息】生成 JSON\n" +
              "- 不得新增任何接口、字段或参数\n" +
              "- JSON数组必须紧跟在阶段一内容之后，中间不要有任何解释文字\n" +
              "- 如果阶段一识别到接口，阶段二必须输出对应的JSON数组；如果阶段一未识别到接口，阶段二输出空数组 []\n" +
              "\n" +
              "【禁止事项】\n" +
              "- 禁止补全文档中不存在的字段或接口\n" +
              "- 禁止在阶段一完成后停止输出，必须继续输出阶段二的JSON数组\n" +
              "- 禁止只输出阶段一而不输出阶段二\n" +
              "\n" +
              "====================\n" +
              "【阶段一：接口识别输出格式】\n" +
              "====================\n" +
              "\n" +
              "对每一个识别到的接口，按以下格式输出（纯文本）：\n" +
              "\n" +
              "=== 接口序号：接口标题 / 名称 ===\n" +
              "请求方式：GET / POST\n" +
              "请求路径：/xxx/yyy（仅路径，不含域名）\n" +
              "\n" +
              "【请求头字段（Request Headers）】\n" +
              "- 字段名 | 是否必填（M / O） | 示例值（仅当文档中明确给出）\n" +
              "\n" +
              "【请求参数（Query Parameters）】\n" +
              "- 字段名 | 是否必填 | 示例值（仅当文档中明确给出）\n" +
              "\n" +
              "【请求体字段（Request Body）】\n" +
              "- 字段名 | 是否必填 | 示例值\n" +
              "- 若文档未定义 Request Body，请明确写“文档未提供”\n" +
              "\n" +
              "【响应头字段（Response Headers）】\n" +
              "- 字段名 | 是否必填 | 示例值（仅当文档中明确给出）\n" +
              "\n" +
              "【响应体字段（Response Body）】\n" +
              "- 字段名 | 是否必填 | 示例值 / 枚举值\n" +
              "- Array 字段需列出子字段\n" +
              "\n" +
              "【错误响应信息】\n" +
              "- 错误字段（如 ErrorCode、ErrorDescription）\n" +
              "- HTTP 状态码（若文档中提供）\n" +
              "\n" +
              "【必填字段路径】\n" +
              "- headers.xxx\n" +
              "- query.xxx\n" +
              "- body.xxx\n" +
              "\n" +
              "====================\n" +
              "【阶段二：JSON 结构定义（固定，不可修改）】\n" +
              "====================\n" +
              "[\n" +
              "  {\n" +
              "    \"title\": \"\",\n" +
              "    \"method\": \"GET | POST\",\n" +
              "    \"apiPath\": \"\",\n" +
              "    \"requestExample\": {\n" +
              "      \"headers\": {\"SourceSystemID\": \"CRM\", \"ReferenceID\": \"KSK20120530221525000839\"},\n" +
              "      \"query\": {\"param1\": \"value1\"},\n" +
              "      \"body\": {\"field1\": \"value1\"}\n" +
              "    },\n" +
              "    \"responseExample\": {\n" +
              "      \"headers\": {\"SourceSystemID\": \"CRM\", \"Status\": \"200 OK\"},\n" +
              "      \"body\": {\"result\": \"success\", \"data\": [{\"id\": \"123\"}]}\n" +
              "    },\n" +
              "    \"errorResponseExample\": {\n" +
              "      \"headers\": {\"Status\": \"400 Bad Request\", \"ErrorCode\": \"ERROR001\"},\n" +
              "      \"body\": {\"ErrorCode\": \"ERROR001\", \"ErrorDescription\": \"Validation failed\"}\n" +
              "    },\n" +
              "    \"requiredFields\": [],\n" +
              "    \"errorHttpStatus\": 400\n" +
              "  }\n" +
              "]\n\n" +
              "====================\n" +
              "【阶段二：JSON 生成规则】\n" +
              "====================\n" +
              "- 只能使用阶段一中已识别的接口和字段\n" +
              "- requestExample.headers：必须包含所有请求头字段，**每个字段都必须填充合理的Mock值**\n" +
              "- GET 请求参数只能放在 query，POST 请求参数只能放在 body，**每个参数都必须填充合理的Mock值**\n" +
              "- responseExample.headers：必须包含所有响应头字段，**每个字段都必须填充合理的Mock值**\n" +
              "- responseExample.body：仅包含响应体字段（不含响应头），**每个字段都必须填充合理的Mock值**\n" +
              "- requiredFields：仅包含 Mandatory / Required 字段，使用路径格式 headers.xxx / query.xxx / body.xxx\n" +
              "- **重要：所有字段都必须有值，不能是空字符串、null或undefined**\n\n" +
              "【字段值规则】\n" +
              "- 文档中存在示例值 → 必须使用文档值\n" +
              "- 文档中不存在示例值 → **必须**根据字段名和字段语义生成合理 Mock 值，不能为空或null\n" +
              "- 数组字段必须生成数组结构，对象字段必须生成对象结构\n" +
              "- **所有字段都必须填充Mock数据，包括：**\n" +
              "  * 请求头字段（headers）：必须为每个字段生成合理的值\n" +
              "  * 请求参数（query/body）：必须为每个字段生成合理的值\n" +
              "  * 响应头字段（headers）：必须为每个字段生成合理的值\n" +
              "  * 响应体字段（body）：必须为每个字段生成合理的值，包括数组中的子字段\n" +
              "- 如果字段是枚举类型，使用第一个枚举值作为示例\n" +
              "- 如果字段是数组类型，至少生成1-2个元素的数组，每个元素包含所有子字段\n" +
              "- 如果字段是对象类型，必须包含所有子字段并填充值\n\n" +
              "【禁止事项】\n" +
              "- 禁止新增字段\n" +
              "- 禁止将 header 字段放入 body\n" +
              "- 禁止输出 null / undefined\n" +
              "- 禁止输出任何解释性文字\n" +
              "- 禁止在阶段一完成后停止输出，必须继续输出阶段二的JSON数组\n\n" +
              "====================\n" +
              "【失败兜底规则】\n" +
              "====================\n" +
              "如果在文件中未识别到任何 API 接口，\n" +
              "阶段一请明确输出：\n" +
              "文档中未识别到任何 API 接口\n" +
              "阶段二必须输出空数组：[]\n\n" +
              "====================\n" +
              "【重要提醒】\n" +
              "====================\n" +
              "- 阶段一和阶段二必须连续输出，不能只输出阶段一就停止\n" +
              "- 阶段一的文本输出完成后，必须立即输出阶段二的JSON数组\n" +
              "- 如果只输出阶段一而不输出阶段二，视为未完成任务\n" +
              "- 阶段二JSON数组的输出格式：直接以 [ 开始，以 ] 结束，不要有任何前置说明\n" +
              "- 示例：阶段一内容结束后，立即输出 [{\"title\":\"...\",...}]\n" +
              "- **关键：阶段一完成后，必须立即开始输出JSON数组，不要有任何停顿、说明或换行**\n";

    }

    /**
     * 构建将识别结果转换为JSON的提示词（第二步）
     */
    private String buildJsonConversionPrompt(String recognitionText) {
        return "你是接口Mock数据生成助手。请根据以下接口识别结果，生成标准的JSON数组格式的接口Mock数据。\n\n"
                + "**重要要求：**\n"
                + "1. 只能输出严格JSON数组，不要包含任何解释文字、markdown代码块标记\n"
                + "2. JSON数组必须直接以 [ 开始，以 ] 结束\n"
                + "3. 必须严格按照以下JSON结构生成，不要新增或删除字段\n"
                + "4. 所有字段都必须填充合理的Mock值，不能为空、null或undefined\n"
                + "5. 数组字段必须生成完整的数组结构，对象字段必须生成完整的对象结构\n\n"
                + "**JSON结构（固定格式）：**\n"
                + "[\n"
                + "  {\n"
                + "    \"title\": \"接口名称\",\n"
                + "    \"method\": \"GET | POST\",\n"
                + "    \"apiPath\": \"接口路径\",\n"
                + "    \"requestExample\": {\n"
                + "      \"headers\": {\"字段名\": \"Mock值\"},\n"
                + "      \"query\": {\"字段名\": \"Mock值\"},\n"
                + "      \"body\": {\"字段名\": \"Mock值\"}\n"
                + "    },\n"
                + "    \"responseExample\": {\n"
                + "      \"headers\": {\"字段名\": \"Mock值\"},\n"
                + "      \"body\": {\"字段名\": \"Mock值\", \"数组字段\": [{\"子字段\": \"Mock值\"}]}\n"
                + "    },\n"
                + "    \"errorResponseExample\": {\n"
                + "      \"headers\": {\"Status\": \"400 Bad Request\", \"ErrorCode\": \"ERROR001\"},\n"
                + "      \"body\": {\"ErrorCode\": \"ERROR001\", \"ErrorDescription\": \"Validation failed\"}\n"
                + "    },\n"
                + "    \"requiredFields\": [\"headers.字段名\", \"query.字段名\", \"body.字段名\"],\n"
                + "    \"errorHttpStatus\": 400\n"
                + "  }\n"
                + "]\n\n"
                + "**字段映射规则：**\n"
                + "- 识别结果中的【请求头字段】→ requestExample.headers\n"
                + "- 识别结果中的【请求参数（Query Parameters）】→ requestExample.query（GET请求）\n"
                + "- 识别结果中的【请求体字段（Request Body）】→ requestExample.body（POST请求）\n"
                + "- 识别结果中的【响应头字段】→ responseExample.headers\n"
                + "- 识别结果中的【响应体字段】→ responseExample.body\n"
                + "- 识别结果中的【必填字段路径】→ requiredFields（格式：headers.xxx / query.xxx / body.xxx）\n"
                + "- 识别结果中的【HTTP 状态码】→ errorHttpStatus\n\n"
                + "**字段值填充规则（重要：生成简洁的Mock值，避免过长字符串）：**\n"
                + "- 如果识别结果中有示例值，使用示例值（但如果示例值过长，使用简化版本）\n"
                + "- 如果识别结果中没有示例值，根据字段名和语义生成**简洁合理**的Mock值\n"
                + "- **字符串字段值长度限制：**\n"
                + "  * ID类字段（如 id, userId, request-id）：6-20个字符，如 \"123456\" 或 \"USER001\"\n"
                + "  * 名称类字段（如 name, title, label）：5-30个字符，如 \"Test Name\" 或 \"示例名称\"\n"
                + "  * 长ID类字段（如 eid, iccid, imei）：20-32个字符，如 \"A1B2C3D4E5F6G7H8I9J0\"\n"
                + "  * URL类字段：50-100个字符，如 \"https://example.com/api/v1/endpoint\"\n"
                + "  * 描述类字段：20-50个字符，如 \"This is a test description\"\n"
                + "  * **禁止生成超过100个字符的字符串值**\n"
                + "- 数组字段必须生成至少1-2个元素的数组，每个元素包含所有子字段\n"
                + "- 对象字段必须包含所有子字段并填充值\n"
                + "- 所有字段都不能为空、null或undefined\n\n"
                + "**接口识别结果：**\n"
                + recognitionText
                + "\n\n"
                + "**请根据以上识别结果，生成标准的JSON数组。只输出JSON，不要有任何解释文字。**";
    }

    private String buildManualPrompt(String userInput) {
        return "你是接口Mock结构生成助手。请根据输入的描述生成接口结构。\n"
                + "要求：\n"
                + "1. 只能输出严格JSON，不要包含任何解释文字。\n"
                + "2. requestExample 必须拆分为 headers/query/body。\n"
                + "3. responseExample 必须拆分为 headers/body。\n"
                + "4. requiredFields 使用点号路径并带前缀 headers./query./body.\n"
                + "5. method 只能是 GET 或 POST。\n"
                + "6. 若缺少示例，按字段语义生成合理值。\n"
                + "7. 不要把 header 字段放进 body。\n"
                + "8. 结构固定，不要新增字段。\n\n"
                + "输出JSON结构：\n"
                + "{\n"
                + "  \"title\": \"\",\n"
                + "  \"method\": \"\",\n"
                + "  \"requestExample\": {\n"
                + "    \"headers\": {},\n"
                + "    \"query\": {},\n"
                + "    \"body\": {}\n"
                + "  },\n"
                + "  \"responseExample\": {\n"
                + "    \"headers\": {},\n"
                + "    \"body\": {}\n"
                + "  },\n"
                + "  \"errorResponseExample\": {},\n"
                + "  \"requiredFields\": [],\n"
                + "  \"errorHttpStatus\": 400\n"
                + "}\n\n"
                + "用户描述：\n"
                + userInput;
    }

    private String buildManualPromptWithHistory(java.util.List<String> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是接口Mock结构生成助手。请根据多轮对话内容生成或更新接口结构。\n")
                .append("重要：这是多轮对话，你需要综合所有历史信息，生成完整的接口结构。\n")
                .append("如果用户补充了新信息，请更新之前的结构；如果信息冲突，以最新信息为准。\n\n")
                .append("要求：\n")
                .append("1. 只能输出严格JSON，不要包含任何解释文字。\n")
                .append("2. requestExample 必须拆分为 headers/query/body。\n")
                .append("3. responseExample 必须拆分为 headers/body。\n")
                .append("4. requiredFields 使用点号路径并带前缀 headers./query./body.\n")
                .append("5. method 只能是 GET 或 POST。\n")
                .append("6. 若缺少示例，按字段语义生成合理值。\n")
                .append("7. 不要把 header 字段放进 body。\n")
                .append("8. 结构固定，不要新增字段。\n")
                .append("9. 综合所有轮次的信息，生成完整的接口定义。\n\n")
                .append("输出JSON结构：\n")
                .append("{\n")
                .append("  \"title\": \"\",\n")
                .append("  \"method\": \"\",\n")
                .append("  \"requestExample\": {\n")
                .append("    \"headers\": {},\n")
                .append("    \"query\": {},\n")
                .append("    \"body\": {}\n")
                .append("  },\n")
                .append("  \"responseExample\": {\n")
                .append("    \"headers\": {},\n")
                .append("    \"body\": {}\n")
                .append("  },\n")
                .append("  \"errorResponseExample\": {},\n")
                .append("  \"requiredFields\": [],\n")
                .append("  \"errorHttpStatus\": 400\n")
                .append("}\n\n")
                .append("多轮对话历史（按时间顺序）：\n");
        int index = 1;
        for (String msg : messages) {
            if (msg == null || msg.trim().isEmpty()) {
                continue;
            }
            builder.append("第").append(index++).append("轮：").append(msg.trim()).append("\n");
        }
        builder.append("\n请综合以上所有信息，生成完整的接口结构JSON。");
        return builder.toString();
    }

    private String buildFullDocumentPrompt(String docText) {
        return "你是接口Mock数据生成助手。请根据输入的完整接口文档抽取所有接口定义，生成请求示例、响应示例、错误响应示例与必填字段。\n"
                + "核心目标：识别每个接口并输出 items 数组，每个接口一条。\n"
                + "只输出严格JSON，不要包含任何解释、标题、代码块。\n"
                + "不要输出文档版本、修订记录、目录、术语、版权、联系方式等非接口内容。\n\n"
                + "接口识别规则（需要综合判断）：\n"
                + "- 出现 HTTP 方法 + 路径（如 GET/POST + /xxx 或 {rootPath}）\n"
                + "- 出现 API 标题/编号（如 5.2.1.10 GetProfileOTAKeys）\n"
                + "- 出现 Request/Response 示例或参数表\n\n"
                + "字段结构要求：\n"
                + "1) method 必须是 GET 或 POST（来自 Request Verb/Method/请求方式）。\n"
                + "2) requestExample 必须拆分为 headers/query/body 三部分：\n"
                + "   - HTTP Header / Request Header / Header -> requestExample.headers\n"
                + "   - Query Params / URL Params / Query -> requestExample.query\n"
                + "   - Request Body / Body -> requestExample.body\n"
                + "   - 不要把 header 放进 body。\n"
                + "3) responseExample 必须拆分为 headers/body。\n"
                + "4) requiredFields 为数组，使用点号路径并带前缀：headers.X, query.y, body.z。\n"
                + "5) 若文档提供 Sample Request/Response，必须优先使用；没有示例时，按字段语义生成合理值。\n"
                + "6) 不能混淆请求与响应字段。\n\n"
                + "输出JSON结构（固定，不要加字段）：\n"
                + "{\n"
                + "  \"items\": [\n"
                + "    {\n"
                + "      \"title\": \"\",\n"
                + "      \"method\": \"\",\n"
                + "      \"requestExample\": {\n"
                + "        \"headers\": {},\n"
                + "        \"query\": {},\n"
                + "        \"body\": {}\n"
                + "      },\n"
                + "      \"responseExample\": {\n"
                + "        \"headers\": {},\n"
                + "        \"body\": {}\n"
                + "      },\n"
                + "      \"errorResponseExample\": {},\n"
                + "      \"requiredFields\": []\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "完整接口文档：\n"
                + docText;
    }

    private String buildFullDocumentText(ParsedDocument parsedDocument) {
        if (parsedDocument == null || parsedDocument.getSections() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Section section : parsedDocument.getSections()) {
            String title = safe(section.getTitle());
            if (!title.isEmpty()) {
                builder.append("# ").append(title).append("\n");
            }
            if (section.getContent() != null && !section.getContent().trim().isEmpty()) {
                builder.append(section.getContent().trim()).append("\n");
            }
            if (section.getTables() != null && !section.getTables().isEmpty()) {
                for (TableData table : section.getTables()) {
                    builder.append(formatTable(table)).append("\n");
                }
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private List<Chunk> chunk(ParsedDocument parsedDocument) {
        List<Chunk> chunks = new ArrayList<>();
        if (parsedDocument == null || parsedDocument.getSections() == null) {
            return chunks;
        }

        Chunk current = null;
        for (Section section : parsedDocument.getSections()) {
            String title = safe(section.getTitle());
            boolean isNewApi = shouldStartNewApi(section);
            String formatted = formatSectionIfRelevant(section);
            if (formatted.isEmpty()) {
                continue;
            }
            if (isNewApi) {
                if (current != null) {
                    chunks.add(current);
                }
                current = new Chunk(title);
            }
            if (current == null) {
                current = new Chunk(title.isEmpty() ? "Document" : title);
            }
            current.text.append(formatted).append("\n");
            if (section.getTables() != null) {
                current.tableCount += section.getTables().size();
            }
        }
        if (current != null) {
            chunks.add(current);
        }
        return chunks;
    }

    private boolean isEndpointSection(Section section) {
        String title = safe(section.getTitle()).toLowerCase(Locale.ROOT);
        String content = safe(section.getContent()).toLowerCase(Locale.ROOT);
        if (title.isEmpty()) {
            return false;
        }
        if (title.startsWith("v") && title.matches("^v\\d+.*")) {
            return false;
        }
        if ((title.contains("added") || title.contains("updated") || title.contains("update"))
                && title.contains("api")
                && !title.matches("^(get|post|update|create|delete|list|add|remove|release|cancel|handle|confirm|download|generate).*")) {
            return false;
        }
        if (title.contains("annex") || title.contains("appendix")) {
            return false;
        }
        if (title.contains("request headers")
                || title.contains("response headers")
                || title.contains("request body")
                || title.contains("response body")
                || title.contains("request parameters")
                || title.contains("response parameters")
                || title.contains("request & response")
                || title.contains("resource specification")
                || title.contains("interface message specification")) {
            return false;
        }
        if (title.matches("^\\d+(\\.\\d+){1,3}\\s+.+")) {
            if (title.contains("api") || title.contains("接口")) {
                return true;
            }
            return title.contains("download") || title.contains("order") || title.contains("profile")
                    || title.contains("get") || title.contains("create") || title.contains("update");
        }
        if (title.contains("api") || title.contains("接口")) {
            if (title.contains("all api") || title.contains("api call") || title.contains("api calls") || title.contains("https")) {
                return false;
            }
            return true;
        }
        if (title.matches("^(get|post|update|create|delete|list|add|remove|release|cancel|handle|confirm|download|generate).+")) {
            return true;
        }
        return false;
    }

    private boolean shouldStartNewApi(Section section) {
        String title = safe(section.getTitle()).toLowerCase(Locale.ROOT);
        if (!isEndpointSection(section)) {
            return false;
        }
        if (isApiSubSectionTitle(title)) {
            return false;
        }
        if (looksLikeTableRowTitle(title)) {
            return false;
        }
        if (!isStrongApiTitle(title)) {
            return false;
        }
        return true;
    }

    private boolean isApiSubSectionTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        if (title.contains("request specification")
                || title.contains("response specification")
                || title.contains("request header")
                || title.contains("response header")
                || title.contains("request body")
                || title.contains("response body")
                || title.contains("request parameter")
                || title.contains("response parameter")
                || title.contains("parameter request")
                || title.contains("parameter response")
                || title.contains("sample request")
                || title.contains("sample response")
                || title.contains("error code")
                || title.contains("api name")
                || title.contains("resource url")
                || title.contains("content type")
                || title.contains("security")
                || title.contains("diagram flow")
                || title.contains("parameter api response")
                || title.contains("parameter api request")
                || (title.contains("parameter") && title.contains("api"))
                || (title.contains("parameter") && title.contains("response"))
                || (title.contains("parameter") && title.contains("request"))
                || title.contains("api response")
                || title.contains("api request")
                || title.contains("response error code")) {
            return true;
        }
        return false;
    }

    private boolean looksLikeTableRowTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        String t = title.toLowerCase(Locale.ROOT);
        boolean hasTypeWord = t.contains("string")
                || t.contains("number")
                || t.contains("int")
                || t.contains("length")
                || t.contains("required")
                || t.contains("sample")
                || t.contains("type");
        boolean hasDigit = t.matches(".*\\d+.*");
        int tokenCount = t.split("\\s+").length;
        return hasTypeWord && hasDigit && tokenCount >= 5;
    }

    private boolean isStrongApiTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        String t = title.toLowerCase(Locale.ROOT).trim();
        if (t.matches("^\\d+(\\.\\d+){0,3}\\s+api\\b.+")) {
            return true;
        }
        if (t.matches("^(get|post|update|create|delete|list|add|remove|release|cancel|handle|confirm|download|generate)\\b.+")) {
            return true;
        }
        if (t.contains("api") && t.split("\\s+").length <= 4) {
            return true;
        }
        return false;
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
            String tableContext = tableContextHint(section);
            for (TableData table : section.getTables()) {
                if (!tableContext.isEmpty()) {
                    builder.append(tableContext).append("\n");
                    String headerFields = extractHeaderFieldNames(table);
                    if (!headerFields.isEmpty()) {
                        builder.append("Header Fields: ").append(headerFields).append("\n");
                    }
                }
                builder.append(formatTable(table)).append("\n");
            }
        }
        return builder.toString();
    }

    private String tableContextHint(Section section) {
        String title = safe(section.getTitle()).toLowerCase(Locale.ROOT);
        if (title.contains("request header") || title.contains("request headers")) {
            return "Request Headers Table:";
        }
        if (title.contains("response header") || title.contains("response headers")) {
            return "Response Headers Table:";
        }
        if (title.contains("request body") || title.contains("request parameters") || title.contains("request parameter")) {
            return "Request Body Table:";
        }
        if (title.contains("response body") || title.contains("response parameters") || title.contains("response parameter")) {
            return "Response Body Table:";
        }
        if (section.getTables() != null) {
            for (TableData table : section.getTables()) {
                if (looksLikeHeaderTable(table)) {
                    return "Request Headers Table:";
                }
            }
        }
        return "";
    }

    private boolean looksLikeHeaderTable(TableData table) {
        if (table == null) {
            return false;
        }
        if (table.getHeaders() != null) {
            for (String header : table.getHeaders()) {
                String h = safe(header).toLowerCase(Locale.ROOT);
                if (h.contains("header field") || h.contains("header field name") || h.equals("header")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractHeaderFieldNames(TableData table) {
        if (table == null || table.getRows() == null || table.getRows().isEmpty()) {
            return "";
        }
        int nameIndex = 0;
        if (table.getHeaders() != null && !table.getHeaders().isEmpty()) {
            int idx = findHeaderIndex(table.getHeaders(), "header field name", "header field", "header", "name");
            if (idx >= 0) {
                nameIndex = idx;
            }
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (java.util.List<String> row : table.getRows()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (nameIndex >= row.size()) {
                nameIndex = 0;
            }
            String value = safe(row.get(nameIndex));
            if (value.isEmpty()) {
                continue;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.equals("content-type") || lower.equals("accept")
                    || lower.contains("api_key") || lower.contains("x-signature")
                    || lower.contains("authorization") || lower.contains("x-auth")
                    || lower.contains("x-client")) {
                names.add(value);
            } else if (value.matches("^[a-zA-Z0-9\\-_]+$")) {
                names.add(value);
            }
        }
        if (names.isEmpty()) {
            return "";
        }
        return String.join(", ", names);
    }

    private int findHeaderIndex(java.util.List<String> headers, String... candidates) {
        if (headers == null) {
            return -1;
        }
        for (int i = 0; i < headers.size(); i++) {
            String h = safe(headers.get(i)).toLowerCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (h.equals(candidate) || h.contains(candidate)) {
                    return i;
                }
            }
        }
        return -1;
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
                if (containsKeywordsFromTable(table) || containsRequestResponseExampleFromTable(table) || looksLikeApiTable(table)) {
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

    private boolean looksLikeApiTable(TableData table) {
        if (table == null) {
            return false;
        }
        if (table.getHeaders() != null) {
            for (String header : table.getHeaders()) {
                String h = safe(header).toLowerCase(Locale.ROOT);
                if (h.contains("element name")
                        || h.contains("header field")
                        || h.contains("request parameters")
                        || h.contains("response parameters")
                        || h.contains("data type")
                        || h.contains("parameter")
                        || h.contains("m/o")
                        || h.contains("mo")
                        || h.contains("length")) {
                    return true;
                }
            }
        }
        if (table.getRows() != null) {
            for (List<String> row : table.getRows()) {
                for (String cell : row) {
                    String c = safe(cell).toLowerCase(Locale.ROOT);
                    if (c.contains("header") || c.contains("request") || c.contains("response") || c.contains("parameter")) {
                        return true;
                    }
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

    private String textOr(JsonNode node, String fallback) {
        if (node != null && node.isTextual() && !node.asText().trim().isEmpty()) {
            return node.asText().trim();
        }
        return fallback;
    }

    private static class Chunk {
        private final String title;
        private final StringBuilder text = new StringBuilder();
        private int tableCount = 0;

        private Chunk(String title) {
            this.title = title == null ? "Document" : title;
        }
    }
}
