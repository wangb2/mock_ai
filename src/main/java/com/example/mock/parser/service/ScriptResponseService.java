package com.example.mock.parser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 执行脚本响应模式下的 JavaScript（Nashorn），根据请求上下文返回 Mock 响应。
 * 脚本可访问 request.headers / request.query / request.body，需返回 { headers: {}, body: {} }。
 */
@Service
public class ScriptResponseService {

    private static final Logger logger = LoggerFactory.getLogger(ScriptResponseService.class);
    private static final int SCRIPT_TIMEOUT_SECONDS = 5;

    private final ObjectMapper objectMapper;
    private final ExecutorService scriptExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mock-script-runner");
        t.setDaemon(true);
        return t;
    });

    public ScriptResponseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行响应脚本，传入请求上下文，返回标准响应结构 JsonNode（含 headers 与 body），
     * 异常或超时时返回 null，由调用方回退到 errorResponseExample 或固定错误。
     */
    public JsonNode execute(String responseScript, JsonNode requestNode) {
        if (responseScript == null || responseScript.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> requestMap = jsonNodeToMap(requestNode);
        final Map<String, Object> requestForScript = requestMap != null ? requestMap : new java.util.HashMap<>();
        String wrappedScript = "(function(){ " + responseScript + " })();";
        Callable<Object> task = () -> {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("nashorn");
            if (engine == null) {
                logger.warn("Nashorn script engine not available");
                return null;
            }
            engine.put("request", requestForScript);
            try {
                Object result = engine.eval(wrappedScript);
                return result;
            } catch (ScriptException e) {
                logger.warn("Script execution error: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        };
        try {
            Future<Object> future = scriptExecutor.submit(task);
            Object result = future.get(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result == null) {
                return null;
            }
            JsonNode node = objectMapper.valueToTree(result);
            return normalizeResponse(node);
        } catch (TimeoutException e) {
            logger.warn("Script execution timeout after {} seconds", SCRIPT_TIMEOUT_SECONDS);
            return null;
        } catch (Exception e) {
            logger.warn("Script execution failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return objectMapper.convertValue(node, Map.class);
    }

    /**
     * 确保返回结构为 { headers: {}, body: {} }；若脚本只返回了 body，则包一层 headers。
     */
    private JsonNode normalizeResponse(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        ObjectNode obj = (ObjectNode) node;
        if (!obj.has("body") && obj.size() > 0) {
            ObjectNode wrapped = objectMapper.createObjectNode();
            wrapped.set("headers", obj.has("headers") ? obj.get("headers") : objectMapper.createObjectNode());
            wrapped.set("body", obj);
            return wrapped;
        }
        if (!obj.has("headers")) {
            obj.set("headers", objectMapper.createObjectNode());
        }
        if (!obj.has("body")) {
            obj.set("body", objectMapper.createObjectNode());
        }
        return obj;
    }
}
