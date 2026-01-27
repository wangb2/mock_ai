package com.example.mock.parser.service;

import com.example.mock.parser.entity.MockEndpointEntity;
import com.example.mock.parser.entity.MockOperationLogEntity;
import com.example.mock.parser.entity.MockResponseCacheEntity;
import com.example.mock.parser.model.MockEndpointItem;
import com.example.mock.parser.model.MockEndpointResult;
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
    private static final String API_URL = "https://open.bigmodel.cn/api/coding/paas/v4/chat/completions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MockEndpointRepository repository;
    private final MockResponseCacheRepository responseCacheRepository;
    private final MockOperationLogRepository logRepository;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MockEndpointService.class);

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.model:glm-4.7}")
    private String model;

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
                               MockOperationLogRepository logRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.responseCacheRepository = responseCacheRepository;
        this.logRepository = logRepository;
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

            logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: " + result.getItems().size());
            logger.info("Generated endpoints done. fileId={}, fileName={}, count={}", sourceFileId, sourceFileName, result.getItems().size());
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
        for (MockEndpointEntity entity : repository.findBySceneId(sceneId)) {
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
        JsonNode generated = generateResponseWithAi(item, requestBody);
        if (generated == null) {
            generated = buildDynamicResponse(item.getResponseExample(), requestBody);
        }
        persistResponseCache(item.getId(), signature, requestBody, generated);
        logOperation("MOCK_GEN", item.getId(), item.getSourceFileName(), "AI生成响应");
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
                                                 Integer errorHttpStatus) {
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

    public MockEndpointItem generateManualPreview(java.util.List<Object> messages) throws IOException {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        String raw = callZhipuWithMessages(messages);
        if (raw == null || raw.trim().isEmpty()) {
            logger.warn("LLM returned empty response");
            return null;
        }
        // 记录原始响应（前500字符用于调试）
        String preview = raw.length() > 500 ? raw.substring(0, 500) + "..." : raw;
        logger.info("LLM raw response preview: {}", preview);
        MockEndpointItem item = parseEndpoint(raw, "");
        if (item == null) {
            logger.warn("Failed to parse endpoint from LLM response. Raw length: {}", raw.length());
            // 尝试记录更多信息
            JsonNode node = readJsonLoosely(raw);
            if (node == null) {
                logger.warn("readJsonLoosely returned null. Raw preview: {}", preview);
            } else {
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
            }
        } else {
            logger.info("Successfully parsed endpoint. title={}, method={}, hasRequest={}, hasResponse={}", 
                item.getTitle(), item.getMethod(), 
                item.getRequestExample() != null, item.getResponseExample() != null);
        }
        return item;
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
        JsonNode node = readJsonLoosely(raw);
        if (node == null) {
            return java.util.Collections.emptyList();
        }
        List<MockEndpointItem> items = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode itemNode : node) {
                MockEndpointItem item = parseEndpointNode(itemNode, "");
                if (item != null) {
                    items.add(item);
                }
            }
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

    private MockEndpointItem parseEndpointNode(JsonNode node, String fallbackTitle) {
        if (node == null || !node.isObject()) {
            return null;
        }
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

        JsonNode required = node.path("requiredFields");
        if (required.isArray()) {
            List<String> requiredFields = new ArrayList<>();
            for (JsonNode field : required) {
                if (field.isTextual() && !field.asText().trim().isEmpty()) {
                    requiredFields.add(field.asText().trim());
                }
            }
            item.setRequiredFields(requiredFields);
        }
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

    private JsonNode generateResponseWithAi(MockEndpointItem item, JsonNode requestBody) throws IOException {
        if (item.getResponseExample() == null) {
            return null;
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("request", requestBody);
        payload.set("responseExample", item.getResponseExample());
        if (item.getErrorResponseExample() != null) {
            payload.set("errorResponseExample", item.getErrorResponseExample());
        }
        if (item.getRequiredFields() != null) {
            payload.putPOJO("requiredFields", item.getRequiredFields());
        }
        String prompt = "你是接口响应生成助手。根据输入的请求参数与响应示例，生成新的响应JSON。\n"
                + "要求：\n"
                + "1. 输出严格JSON，不要包含解释文字。\n"
                + "2. 尽量保持响应结构与 responseExample 一致。\n"
                + "3. 对于查询类接口，响应中与请求相关的字段需要体现请求值变化。\n"
                + "4. 如果无法确定字段含义，保持示例值不变。\n\n"
                + "输入：\n"
                + objectMapper.writeValueAsString(payload);
        String raw = callZhipu(prompt);
        return readJsonLoosely(raw);
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
            // Try to extract JSON from code fences or surrounding text.
            String extracted = extractJsonObject(trimmed);
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

    private String extractJsonObject(String text) {
        String cleaned = stripCodeFence(text);
        int start = cleaned.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        int end = -1;
        for (int i = start; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    end = i;
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

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Zhipu API error: " + response.getStatusCode());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode content = node.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() ? response.getBody() : content.asText();
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
        logger.debug("Zhipu API request body length: {}", requestBody.length());
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("Zhipu API error: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new IOException("Zhipu API error: " + response.getStatusCode() + ", body: " + response.getBody());
        }

        JsonNode node = objectMapper.readTree(response.getBody());
        JsonNode content = node.path("choices").path(0).path("message").path("content");
        String result = content.isMissingNode() ? response.getBody() : content.asText();
        logger.info("Zhipu API response length: {}", result != null ? result.length() : 0);
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
                + "4. 请求示例必须拆分为 headers / query / body 三部分。\n"
                + "   - 文档中出现 HTTP Header / Request Header / Header 的字段放入 requestExample.headers。\n"
                + "   - 文档中出现 Query Params / URL Params / Query 的字段放入 requestExample.query。\n"
                + "   - 文档中出现 Request Body / Body 的字段放入 requestExample.body。\n"
                + "   - 不要把 header 字段放进 body。\n"
                + "5. 响应示例必须拆分为 headers / body 两部分。\n"
                + "   - 文档中出现 HTTP Header / Response Header / Header 的字段放入 responseExample.headers。\n"
                + "   - 其他响应字段放入 responseExample.body。\n"
                + "6. method 必须是 GET 或 POST，来自文档里的 Request Verb / Method / 请求方式。\n"
                + "7. 如果片段内存在多个接口，只输出当前标题对应的一个接口。\n"
                + "8. requiredFields 使用点号路径，并带上前缀，例如 headers.SourceSystemID, query.msisdn, body.orderId。\n"
                + "9. 只输出下面固定结构，不要增加字段。\n\n"
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
