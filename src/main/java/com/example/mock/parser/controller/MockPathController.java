package com.example.mock.parser.controller;

import com.example.mock.parser.model.MockEndpointItem;
import com.example.mock.parser.service.MockEndpointService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController
public class MockPathController {
    private final MockEndpointService mockEndpointService;
    private final ObjectMapper objectMapper;

    public MockPathController(MockEndpointService mockEndpointService, ObjectMapper objectMapper) {
        this.mockEndpointService = mockEndpointService;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(path = "/mock/**", method = {RequestMethod.GET, RequestMethod.POST})
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
        JsonNode response = base == null ? null : base.deepCopy();
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

    private Integer normalizeHttpStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status < 100 || status > 599) {
            return null;
        }
        return status;
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
