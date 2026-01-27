package com.example.mock.parser.controller;

import com.example.mock.parser.model.MockResult;
import com.example.mock.parser.model.MockEndpointItem;
import com.example.mock.parser.model.MockEndpointResult;
import com.example.mock.parser.model.MockSceneItem;
import com.example.mock.parser.model.ParsedDocument;
import com.example.mock.parser.service.DocumentParserService;
import com.example.mock.parser.service.MockGenerationService;
import com.example.mock.parser.service.MockEndpointService;
import com.example.mock.parser.service.MockSceneService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parse")
public class ParseController {

    private final DocumentParserService documentParserService;
    private final MockGenerationService mockGenerationService;
    private final MockEndpointService mockEndpointService;
    private final MockSceneService mockSceneService;
    private final ObjectMapper objectMapper;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParseController.class);

    @Value("${mock.upload-dir:uploads}")
    private String uploadDir;

    public ParseController(DocumentParserService documentParserService,
                           MockGenerationService mockGenerationService,
                           MockEndpointService mockEndpointService,
                           MockSceneService mockSceneService,
                           ObjectMapper objectMapper) {
        this.documentParserService = documentParserService;
        this.mockGenerationService = mockGenerationService;
        this.mockEndpointService = mockEndpointService;
        this.mockSceneService = mockSceneService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ParsedDocument parse(@RequestParam("file") MultipartFile file) throws IOException {
        return documentParserService.parse(file);
    }

    @PostMapping(path = "/mock-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MockResult mockFromFile(@RequestParam("file") MultipartFile file) throws IOException {
        ParsedDocument parsedDocument = documentParserService.parse(file);
        return mockGenerationService.generate(parsedDocument);
    }

    @PostMapping(path = "/mock", consumes = MediaType.APPLICATION_JSON_VALUE)
    public MockResult mock(@RequestBody ParsedDocument parsedDocument) throws IOException {
        return mockGenerationService.generate(parsedDocument);
    }

    @PostMapping(path = "/endpoint", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateEndpoints(@RequestParam("file") MultipartFile file,
                                               @RequestParam("sceneId") String sceneId,
                                               @RequestParam(name = "fullAi", defaultValue = "false") boolean fullAi) throws IOException {
        MockSceneItem scene = mockSceneService.getScene(sceneId);
        if (scene == null) {
            return ResponseEntity.badRequest().body("Scene not found");
        }
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        mockEndpointService.deleteBySourceFileName(originalName);
        saveUpload(fileId, originalName, file);

        ParsedDocument parsedDocument = documentParserService.parse(file);
        String fileUrl = "/parse/endpoint/file/" + fileId;
        logger.info("Upload document for mock generation. fileId={}, fileName={}, size={}, fullAi={}",
                fileId, originalName, file.getSize(), fullAi);
        MockEndpointResult result = mockEndpointService.generateEndpoints(parsedDocument, fileId, originalName, fileUrl,
                fullAi, scene.getId(), scene.getName(), scene.getKeywords());
        return ResponseEntity.ok(result);
    }

    @RequestMapping(path = "/mock/**", method = {org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<?> mockByPath(HttpServletRequest request,
                                        @RequestParam java.util.Map<String, String> query,
                                        @RequestBody(required = false) JsonNode body) {
        String uri = request.getRequestURI();
        String apiPath = uri.startsWith("/mock") ? uri.substring("/mock".length()) : uri;
        if (apiPath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String method = request.getMethod();
        MockEndpointItem item = mockEndpointService.getByPath(apiPath, method);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        ObjectNode headersNode = objectMapper.createObjectNode();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headersNode.put(name, request.getHeader(name));
        }
        ObjectNode validation = objectMapper.createObjectNode();
        validation.set("headers", headersNode);
        if ("GET".equalsIgnoreCase(method)) {
            ObjectNode queryNode = objectMapper.createObjectNode();
            for (java.util.Map.Entry<String, String> entry : query.entrySet()) {
                queryNode.put(entry.getKey(), entry.getValue());
            }
            validation.set("query", queryNode);
            return mockByItemInternal(item, validation, validation);
        }
        validation.set("body", body == null ? objectMapper.createObjectNode() : body);
        return mockByItemInternal(item, validation, body);
    }

    @PostMapping(path = "/mock/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mockById(@PathVariable("id") String id,
                                      @RequestBody JsonNode body,
                                      HttpServletRequest request) {
        ObjectNode headersNode = objectMapper.createObjectNode();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headersNode.put(name, request.getHeader(name));
        }
        ObjectNode validation = objectMapper.createObjectNode();
        validation.set("headers", headersNode);
        validation.set("body", body == null ? objectMapper.createObjectNode() : body);
        return mockByIdInternal(id, validation, body);
    }

    @GetMapping(path = "/mock/{id}")
    public ResponseEntity<?> mockByIdGet(@PathVariable("id") String id,
                                         @RequestParam java.util.Map<String, String> query,
                                         HttpServletRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode queryNode = objectMapper.createObjectNode();
        for (java.util.Map.Entry<String, String> entry : query.entrySet()) {
            queryNode.put(entry.getKey(), entry.getValue());
        }
        ObjectNode headersNode = objectMapper.createObjectNode();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headersNode.put(name, request.getHeader(name));
        }
        body.set("query", queryNode);
        body.set("headers", headersNode);
        return mockByIdInternal(id, body, body);
    }

    @GetMapping(path = "/mock/{id}/info")
    public ResponseEntity<?> mockInfo(@PathVariable("id") String id) {
        MockEndpointItem item = mockEndpointService.getById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        ObjectNode info = objectMapper.createObjectNode();
        info.put("id", item.getId());
        info.put("title", item.getTitle());
        info.put("method", item.getMethod());
        info.put("mockUrl", item.getMockUrl());
        info.put("sceneId", item.getSceneId());
        info.put("sceneName", item.getSceneName());
        if (item.getErrorHttpStatus() != null) {
            info.put("errorHttpStatus", item.getErrorHttpStatus());
        }
        info.putPOJO("requiredFields", item.getRequiredFields());
        info.set("requestExample", item.getRequestExample());
        info.set("responseExample", item.getResponseExample());
        info.set("errorResponseExample", item.getErrorResponseExample());
        info.put("hint", "POST JSON to mockUrl to get mock response with validation");
        return ResponseEntity.ok(info);
    }

    private ResponseEntity<?> mockByIdInternal(String id, JsonNode validationNode, JsonNode requestPayload) {
        MockEndpointItem item = mockEndpointService.getById(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return mockByItemInternal(item, validationNode, requestPayload);
    }

    private ResponseEntity<?> mockByItemInternal(MockEndpointItem item, JsonNode validationNode, JsonNode requestPayload) {
        if (isErrorMock(validationNode)) {
            mockEndpointService.logOperation("MOCK_ERROR", item.getId(), item.getSourceFileName(), "错误Mock");
            JsonNode errorResponse = buildErrorResponse(item, validationNode);
            Integer status = normalizeHttpStatus(item.getErrorHttpStatus());
            return buildMockResponse(errorResponse, status);
        }
        List<String> missing = validateRequired(validationNode, filterRequiredFields(item));
        if (!missing.isEmpty()) {
            mockEndpointService.logOperation("MOCK_VALIDATION_FAIL", item.getId(), item.getSourceFileName(), "参数校验失败");
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "validation_failed");
            error.putPOJO("missing", missing);
            return ResponseEntity.badRequest().body(error);
        }
        JsonNode payload = requestPayload != null ? requestPayload : validationNode;
        try {
            JsonNode response = mockEndpointService.getDynamicResponse(item, payload);
            return buildMockResponse(response, null);
        } catch (IOException ex) {
            return buildMockResponse(item.getResponseExample(), null);
        }
    }

    private ResponseEntity<?> buildMockResponse(JsonNode response, Integer httpStatus) {
        if (response == null || !response.isObject()) {
            return httpStatus != null ? ResponseEntity.status(httpStatus).body(response) : ResponseEntity.ok(response);
        }
        ObjectNode obj = (ObjectNode) response;
        JsonNode headersNode = obj.get("headers");
        ObjectNode bodyNode = obj.has("body") && obj.get("body").isObject()
                ? (ObjectNode) obj.get("body")
                : null;
        ObjectNode cleaned = obj.deepCopy();
        cleaned.remove("headers");
        if (cleaned.has("body")) {
            cleaned.remove("body");
        }
        ObjectNode responseBody = bodyNode != null ? bodyNode : cleaned;
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (headersNode != null && headersNode.isObject()) {
            java.util.Iterator<String> it = headersNode.fieldNames();
            while (it.hasNext()) {
                String name = it.next();
                JsonNode value = headersNode.get(name);
                if (value != null && !value.isNull()) {
                    headers.add(name, value.asText());
                }
            }
        }
        if (httpStatus != null) {
            return ResponseEntity.status(httpStatus).headers(headers).body(responseBody);
        }
        return ResponseEntity.ok().headers(headers).body(responseBody);
    }

    private Integer normalizeHttpStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status < 100 || status > 599) {
            return null;
        }
        return status;
    }

    private boolean isErrorMock(JsonNode body) {
        if (body == null || !body.isObject()) {
            return false;
        }
        JsonNode flag = body.get("__mock_error");
        if (flag == null && body.has("query") && body.get("query").isObject()) {
            flag = body.get("query").get("__mock_error");
        }
        if (flag == null && body.has("headers") && body.get("headers").isObject()) {
            flag = body.get("headers").get("__mock_error");
        }
        if (flag != null) {
            if (flag.isBoolean() && flag.asBoolean()) {
                return true;
            }
            if (flag.isTextual() || flag.isInt()) {
                String value = flag.asText().trim();
                if (!value.isEmpty() && !"0".equals(value) && !"false".equalsIgnoreCase(value)) {
                    return true;
                }
            }
        }
        JsonNode code = body.get("__mock_error_code");
        if (code == null && body.has("query") && body.get("query").isObject()) {
            code = body.get("query").get("__mock_error_code");
        }
        if (code == null && body.has("headers") && body.get("headers").isObject()) {
            code = body.get("headers").get("__mock_error_code");
        }
        return code != null && (code.isTextual() || code.isInt());
    }

    private JsonNode buildErrorResponse(MockEndpointItem item, JsonNode body) {
        JsonNode base = item.getErrorResponseExample() != null ? item.getErrorResponseExample() : item.getResponseExample();
        JsonNode response = deepCopy(base);
        String code = textOr(body.get("__mock_error_code"), "ERROR");
        String message = textOr(body.get("__mock_error_message"), "mock error");
        applyErrorFields(response, code, message);
        return response;
    }

    private void applyErrorFields(JsonNode node, String code, String message) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            java.util.Iterator<String> it = obj.fieldNames();
            List<String> fields = new java.util.ArrayList<>();
            while (it.hasNext()) {
                fields.add(it.next());
            }
            for (String key : fields) {
                String lower = key.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("error") && lower.contains("code")) {
                    obj.put(key, code);
                } else if (lower.contains("error") && (lower.contains("message") || lower.contains("desc"))) {
                    obj.put(key, message);
                } else if (lower.equals("message") || lower.equals("msg")) {
                    obj.put(key, message);
                }
                applyErrorFields(obj.get(key), code, message);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                applyErrorFields(child, code, message);
            }
        }
    }

    private JsonNode deepCopy(JsonNode node) {
        return node == null ? null : node.deepCopy();
    }

    private String textOr(JsonNode node, String fallback) {
        if (node == null) {
            return fallback;
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            return value.isEmpty() ? fallback : value;
        }
        if (node.isNumber()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        }
        return fallback;
    }

    @GetMapping(path = "/endpoint/history")
    public MockEndpointResult history() {
        MockEndpointResult result = new MockEndpointResult();
        result.setItems(new java.util.ArrayList<>(mockEndpointService.getAll()));
        return result;
    }

    @GetMapping(path = "/logs")
    public ResponseEntity<?> logs() {
        return ResponseEntity.ok(mockEndpointService.getRecentLogs());
    }

    @GetMapping(path = "/logs/stats")
    public ResponseEntity<?> logStats() {
        return ResponseEntity.ok(mockEndpointService.getLogStats());
    }

    @GetMapping(path = "/logs/stats/endpoint-today")
    public ResponseEntity<?> endpointTodayStats() {
        return ResponseEntity.ok(mockEndpointService.getEndpointTodayCounts());
    }

    @GetMapping(path = "/logs/stats/endpoint-week")
    public ResponseEntity<?> endpointWeekStats() {
        return ResponseEntity.ok(mockEndpointService.getEndpointWeekCounts());
    }

    @GetMapping(path = "/logs/stats/scene-today")
    public ResponseEntity<?> sceneTodayStats() {
        return ResponseEntity.ok(mockEndpointService.getSceneTodayCounts());
    }

    @GetMapping(path = "/logs/stats/scene-week")
    public ResponseEntity<?> sceneWeekStats() {
        return ResponseEntity.ok(mockEndpointService.getSceneWeekCounts());
    }

    @GetMapping(path = "/endpoint/file/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("fileId") String fileId) throws IOException {
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) {
            return ResponseEntity.notFound().build();
        }
        Path match = findFileById(dir, fileId);
        if (match == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(match.toFile());
        String filename = match.getFileName().toString();
        String originalName = filename.substring((fileId + "_").length());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping(path = "/endpoint/{id}")
    public ResponseEntity<?> deleteEndpoint(@PathVariable("id") String id) {
        boolean deleted = mockEndpointService.deleteMockById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping(path = "/endpoint/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateEndpoint(@PathVariable("id") String id, @RequestBody JsonNode body) {
        boolean updated = mockEndpointService.updateMockExamples(id, body);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(path = "/endpoint/file/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable("fileId") String fileId) throws IOException {
        int removed = mockEndpointService.deleteBySourceFileId(fileId);
        Path dir = Paths.get(uploadDir);
        if (Files.exists(dir)) {
            Path match = findFileById(dir, fileId);
            if (match != null) {
                Files.deleteIfExists(match);
            }
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.put("removed", removed);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/scenes")
    public ResponseEntity<?> listScenes() {
        return ResponseEntity.ok(mockSceneService.listScenes());
    }

    @PostMapping(path = "/scenes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createScene(@RequestBody JsonNode body) {
        String name = textOr(body == null ? null : body.get("name"), "");
        String description = textOr(body == null ? null : body.get("description"), "");
        String keywords = textOr(body == null ? null : body.get("keywords"), "");
        MockSceneItem item = mockSceneService.createScene(name, description, keywords);
        if (item == null) {
            return ResponseEntity.badRequest().body("Scene name is required");
        }
        return ResponseEntity.ok(item);
    }

    @PutMapping(path = "/scenes/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateScene(@PathVariable("id") String id, @RequestBody JsonNode body) {
        String name = textOr(body == null ? null : body.get("name"), "");
        String description = textOr(body == null ? null : body.get("description"), "");
        String keywords = textOr(body == null ? null : body.get("keywords"), "");
        MockSceneItem item = mockSceneService.updateScene(id, name, description, keywords);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @DeleteMapping(path = "/scenes/{id}")
    public ResponseEntity<?> deleteScene(@PathVariable("id") String id) {
        boolean deleted = mockSceneService.deleteScene(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/scenes/{id}/endpoints")
    public ResponseEntity<?> listSceneEndpoints(@PathVariable("id") String id) {
        return ResponseEntity.ok(mockEndpointService.getBySceneId(id));
    }

    @PostMapping(path = "/endpoint/manual", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createManualEndpoint(@RequestBody JsonNode body) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParseController.class);
        logger.info("Manual endpoint creation request received");
        
        String sceneId = textOr(body == null ? null : body.get("sceneId"), "");
        MockSceneItem scene = mockSceneService.getScene(sceneId);
        if (scene == null) {
            logger.warn("Manual endpoint creation failed: scene not found. sceneId={}", sceneId);
            return ResponseEntity.badRequest().body("Scene not found");
        }
        String title = textOr(body == null ? null : body.get("title"), "");
        String method = textOr(body == null ? null : body.get("method"), "POST");
        Integer errorHttpStatus = null;
        JsonNode statusNode = body == null ? null : body.get("errorHttpStatus");
        if (statusNode != null && statusNode.isInt()) {
            errorHttpStatus = statusNode.asInt();
        } else if (statusNode != null && statusNode.isTextual()) {
            try {
                errorHttpStatus = Integer.parseInt(statusNode.asText().trim());
            } catch (NumberFormatException ex) {
                errorHttpStatus = null;
            }
        }
        JsonNode requestExample = body == null ? null : body.get("requestExample");
        JsonNode responseExample = body == null ? null : body.get("responseExample");
        JsonNode errorResponseExample = body == null ? null : body.get("errorResponseExample");
        List<String> requiredFields = new java.util.ArrayList<>();
        JsonNode required = body == null ? null : body.get("requiredFields");
        if (required != null && required.isArray()) {
            for (JsonNode item : required) {
                if (item.isTextual() && !item.asText().trim().isEmpty()) {
                    requiredFields.add(item.asText().trim());
                }
            }
        }
        
        logger.info("Manual endpoint creation. title={}, method={}, sceneId={}, hasRequest={}, hasResponse={}, hasError={}, requiredFields={}", 
                title, method, sceneId, 
                requestExample != null && !requestExample.isNull() && !requestExample.isEmpty(), 
                responseExample != null && !responseExample.isNull() && !responseExample.isEmpty(),
                errorResponseExample != null && !errorResponseExample.isNull() && !errorResponseExample.isEmpty(),
                requiredFields.size());
        
        MockEndpointItem item = mockEndpointService.createManualEndpoint(title, method, requestExample,
                responseExample, errorResponseExample, requiredFields, scene.getId(), scene.getName(), errorHttpStatus);
        if (item == null) {
            logger.warn("Manual endpoint creation failed: createManualEndpoint returned null. title={}, method={}", title, method);
            return ResponseEntity.badRequest().body("Empty request/response or invalid data");
        }
        logger.info("Manual endpoint created successfully. id={}, title={}", item.getId(), item.getTitle());
        return ResponseEntity.ok(item);
    }

    @PostMapping(path = "/endpoint/llm-preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> manualPreview(@RequestBody JsonNode body) throws IOException {
        // 优先使用 messages 数组（多轮对话，支持多模态）
        JsonNode messagesNode = body == null ? null : body.get("messages");
        List<Object> messages = new ArrayList<>();
        if (messagesNode != null && messagesNode.isArray()) {
            for (JsonNode msgNode : messagesNode) {
                String role = textOr(msgNode.get("role"), "user");
                JsonNode contentNode = msgNode.get("content");
                if (contentNode == null || contentNode.isMissingNode()) {
                    continue;
                }
                if ("user".equals(role)) {
                    // 支持文本或多模态内容
                    if (contentNode.isTextual()) {
                        String text = contentNode.asText();
                        if (!text.isEmpty()) {
                            messages.add(text);
                        }
                    } else if (contentNode.isArray()) {
                        // 多模态消息（文本+图片），直接传递 JsonNode
                        messages.add(contentNode);
                    } else {
                        // 其他类型，尝试转换为字符串
                        messages.add(contentNode.asText());
                    }
                }
            }
        }
        // 如果没有 messages，尝试使用 userInput（向后兼容）
        if (messages.isEmpty()) {
            String input = textOr(body == null ? null : body.get("userInput"), "");
            if (!input.isEmpty()) {
                messages.add(input);
            }
        }
        if (messages.isEmpty()) {
            return ResponseEntity.badRequest().body("userInput or messages required");
        }
        
        // 记录日志用于调试
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParseController.class);
        logger.info("Manual preview request. messages count: {}", messages.size());
        for (int i = 0; i < messages.size(); i++) {
            Object msg = messages.get(i);
            if (msg instanceof com.fasterxml.jackson.databind.JsonNode) {
                JsonNode node = (JsonNode) msg;
                logger.info("Message {}: isArray={}, size={}", i, node.isArray(), node.isArray() ? node.size() : 0);
            } else {
                logger.info("Message {}: type={}, preview={}", i, msg.getClass().getSimpleName(), 
                    String.valueOf(msg).substring(0, Math.min(100, String.valueOf(msg).length())));
            }
        }
        
        MockEndpointItem item = mockEndpointService.generateManualPreview(messages);
        if (item == null) {
            logger.warn("Manual preview returned null result");
            return ResponseEntity.badRequest().body("Empty result");
        }
        logger.info("Manual preview success. title={}, method={}", item.getTitle(), item.getMethod());
        ObjectNode result = objectMapper.createObjectNode();
        result.put("title", item.getTitle());
        result.put("method", item.getMethod());
        if (item.getErrorHttpStatus() != null) {
            result.put("errorHttpStatus", item.getErrorHttpStatus());
        }
        result.set("requestExample", item.getRequestExample());
        result.set("responseExample", item.getResponseExample());
        result.set("errorResponseExample", item.getErrorResponseExample());
        result.putPOJO("requiredFields", item.getRequiredFields());
        return ResponseEntity.ok(result);
    }

    private String mergeChatMessages(JsonNode body) {
        if (body == null || body.isMissingNode()) {
            return "";
        }
        JsonNode messages = body.get("messages");
        if (messages == null || !messages.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode msg : messages) {
            String role = textOr(msg.get("role"), "user");
            String content = textOr(msg.get("content"), "");
            if (content.isEmpty()) {
                continue;
            }
            builder.append(role).append(": ").append(content).append("\n");
        }
        return builder.toString().trim();
    }

    private void saveUpload(String fileId, String originalName, MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileId + "_" + originalName);
        file.transferTo(target);
    }

    private Path findFileById(Path dir, String fileId) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(path -> path.getFileName().toString().startsWith(fileId + "_"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private List<String> validateRequired(JsonNode body, List<String> requiredFields) {
        List<String> missing = new java.util.ArrayList<>();
        if (requiredFields == null || requiredFields.isEmpty()) {
            return missing;
        }
        for (String path : requiredFields) {
            if (!existsPath(body, path)) {
                missing.add(path);
            }
        }
        return missing;
    }

    private List<String> filterRequiredFields(MockEndpointItem item) {
        List<String> required = item.getRequiredFields();
        JsonNode example = item.getRequestExample();
        if (required == null || required.isEmpty() || example == null) {
            return required;
        }
        List<String> filtered = new java.util.ArrayList<>();
        for (String path : required) {
            if (existsPath(example, path)) {
                filtered.add(path);
            }
        }
        return filtered;
    }

    private boolean existsPath(JsonNode node, String path) {
        if (node == null || path == null || path.trim().isEmpty()) {
            return false;
        }
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current == null || current.isMissingNode()) {
                return false;
            }
            JsonNode next = current.get(part);
            if (next == null && current.isObject()) {
                next = findChildIgnoreCase(current, part);
            }
            current = next;
        }
        return current != null && !current.isMissingNode();
    }

    private JsonNode findChildIgnoreCase(JsonNode objectNode, String field) {
        java.util.Iterator<String> it = objectNode.fieldNames();
        while (it.hasNext()) {
            String name = it.next();
            if (name.equalsIgnoreCase(field)) {
                return objectNode.get(name);
            }
        }
        return null;
    }
}
