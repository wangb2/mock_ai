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
                                                String sourceFileUrl) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Missing zhipu.api-key");
        }

        List<Chunk> chunks = chunk(parsedDocument);
        MockEndpointResult result = new MockEndpointResult();

        logger.info("Generating endpoints from document. fileId={}, fileName={}, chunks={}", sourceFileId, sourceFileName, chunks.size());
        for (Chunk chunk : chunks) {
            String prompt = buildPrompt(chunk.text.toString());
            String raw = callZhipu(prompt);
            MockEndpointItem item = parseEndpoint(raw, chunk.title);
            if (item == null) {
                logger.warn("Skip empty endpoint. title={}, fileId={}", chunk.title, sourceFileId);
                continue;
            }
            if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
                item.setTitle(chunk.title);
            }
            if (!hasMeaningfulContent(item)) {
                logger.warn("Skip endpoint without examples. title={}, fileId={}", item.getTitle(), sourceFileId);
                continue;
            }
            String id = UUID.randomUUID().toString().replace("-", "");
            item.setId(id);
            item.setMockUrl("/parse/mock/" + id);
            item.setRaw(raw);
            item.setSourceFileId(sourceFileId);
            item.setSourceFileName(sourceFileName);
            item.setSourceFileUrl(sourceFileUrl);
            repository.save(toEntity(item));
            result.getItems().add(item);
        }

        logOperation("UPLOAD_MOCK", null, sourceFileName, "生成mock接口: " + result.getItems().size());
        logger.info("Generated endpoints done. fileId={}, fileName={}, count={}", sourceFileId, sourceFileName, result.getItems().size());
        return result;
    }

    public MockEndpointItem getById(String id) {
        return repository.findById(id).map(this::toItem).orElse(null);
    }

    public List<MockEndpointItem> getAll() {
        List<MockEndpointItem> items = new ArrayList<>();
        for (MockEndpointEntity entity : repository.findAllByOrderByCreatedAtDesc()) {
            items.add(toItem(entity));
        }
        return items;
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
            if (request != null && !request.isMissingNode()) {
                entity.setRequestExample(stringify(request));
            }
            if (response != null && !response.isMissingNode()) {
                entity.setResponseExample(stringify(response));
            }
            if (error != null && !error.isMissingNode()) {
                entity.setErrorResponseExample(stringify(error));
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

    private MockEndpointItem parseEndpoint(String raw, String fallbackTitle) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        JsonNode node = readJsonLoosely(raw);
        if (node == null) {
            return null;
        }

        MockEndpointItem item = new MockEndpointItem();
        item.setTitle(textOr(node.path("title"), fallbackTitle));
        item.setMethod(textOr(node.path("method"), ""));
        item.setRequestExample(node.path("requestExample").isMissingNode() ? null : node.path("requestExample"));
        item.setResponseExample(node.path("responseExample").isMissingNode() ? null : node.path("responseExample"));
        item.setErrorResponseExample(node.path("errorResponseExample").isMissingNode() ? null : node.path("errorResponseExample"));

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

    private boolean hasMeaningfulContent(MockEndpointItem item) {
        if (item == null) {
            return false;
        }
        boolean hasMethod = item.getMethod() != null && !item.getMethod().trim().isEmpty();
        boolean hasRequired = item.getRequiredFields() != null && !item.getRequiredFields().isEmpty();
        boolean hasRequest = !isEmptyJson(item.getRequestExample());
        boolean hasResponse = !isEmptyJson(item.getResponseExample());
        boolean hasError = !isEmptyJson(item.getErrorResponseExample());
        boolean hasData = hasRequest || hasResponse || hasError || hasRequired;
        boolean isGenericTitle = item.getTitle() == null
                || item.getTitle().trim().isEmpty()
                || isGenericTitle(item.getTitle());
        if (hasMethod && !hasData && isGenericTitle) {
            return false;
        }
        return hasMethod || hasRequired || hasRequest || hasResponse || hasError;
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
        item.setRequestExample(parseJson(entity.getRequestExample()));
        item.setResponseExample(parseJson(entity.getResponseExample()));
        item.setErrorResponseExample(parseJson(entity.getErrorResponseExample()));
        item.setRequiredFields(parseList(entity.getRequiredFields()));
        return item;
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
            // Try to extract JSON from code fences or surrounding text.
            String extracted = extractJsonObject(trimmed);
            if (extracted == null) {
                return null;
            }
            try {
                return objectMapper.readTree(extracted);
            } catch (IOException inner) {
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

    private List<Chunk> chunk(ParsedDocument parsedDocument) {
        List<Chunk> chunks = new ArrayList<>();
        if (parsedDocument == null || parsedDocument.getSections() == null) {
            return chunks;
        }

        Chunk current = null;
        for (Section section : parsedDocument.getSections()) {
            String title = safe(section.getTitle());
            boolean isNewApi = isEndpointSection(section);
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

        private Chunk(String title) {
            this.title = title == null ? "Document" : title;
        }
    }
}
