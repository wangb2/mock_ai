package com.example.mock.parser.controller;

import com.example.mock.parser.config.FeishuProperties;
import com.example.mock.parser.model.feishu.FeishuCallbackPayload;
import com.example.mock.parser.model.feishu.FeishuEventPayload;
import com.example.mock.parser.model.feishu.FeishuParseResult;
import com.example.mock.parser.service.FeishuCallbackService;
import com.example.mock.parser.service.FeishuEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 飞书入口：Event（事件订阅）、Callback（回调）。
 * 不使用飞书 SDK，按开放平台文档编程方式处理：解析请求体、解密（可选）、URL 验证（challenge）、事件分发、回调响应。
 * <p>
 * 接口：
 * <ul>
 *   <li>Event：POST /feishu/event — 事件订阅请求 URL，接收 URL 验证（challenge）或 event_callback 推送</li>
 *   <li>Callback：POST /feishu/callback — 回调请求 URL，接收 URL 验证或用户点击交互卡片按钮后的回调</li>
 * </ul>
 * 事件订阅：https://open.feishu.cn/document/event-subscription-guide/event-subscriptions/event-subscription-configure-/choose-a-subscription-mode/send-notifications-to-developers-server
 * 回调订阅：https://open.feishu.cn/document/event-subscription-guide/callback-subscription/step-1-choose-a-subscription-mode/send-callbacks-to-developers-server
 */
@RestController
@RequestMapping("/feishu")
public class FeishuController {

    private static final Logger log = LoggerFactory.getLogger(FeishuController.class);

    private final FeishuProperties feishuProperties;
    private final FeishuEventService feishuEventService;
    private final FeishuCallbackService feishuCallbackService;
    private final ObjectMapper objectMapper;

    public FeishuController(FeishuProperties feishuProperties,
                            FeishuEventService feishuEventService,
                            FeishuCallbackService feishuCallbackService,
                            ObjectMapper objectMapper) {
        this.feishuProperties = feishuProperties;
        this.feishuEventService = feishuEventService;
        this.feishuCallbackService = feishuCallbackService;
        this.objectMapper = objectMapper;
    }

    /**
     * Event 接口：POST /feishu/event。
     * 飞书事件订阅请求 URL；处理 URL 验证（challenge）或事件推送（event_callback）。
     */
    @PostMapping(path = "/event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> eventPost(@RequestBody String rawBody) {
        ResponseEntity<?> early = parseAndCheck(rawBody);
        if (early != null) {
            return early;
        }
        FeishuParseResult result = parseFeishuBody(rawBody);
        if (result == null || result.getBody() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (result.getChallenge() != null) {
            return challengeResponse(result.getChallenge());
        }
        if (!feishuProperties.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        FeishuEventPayload payload;
        try {
            payload = objectMapper.treeToValue(result.getBody(), FeishuEventPayload.class);
        } catch (Exception e) {
            log.warn("Feishu event body deserialize failed", e);
            return ResponseEntity.badRequest().build();
        }
        String eventType = payload.getHeader() != null ? payload.getHeader().getEventType() : null;
        if (eventType == null && payload.getEvent() != null) {
            eventType = payload.getEvent().getType();
        }
        log.info("Feishu event received. event_type={}", eventType == null || eventType.isEmpty() ? "(none)" : eventType);
        if (log.isDebugEnabled()) {
            log.debug("Feishu event body: {}", result.getBody().toString());
        }
        try {
            feishuEventService.handleEvent(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Feishu event error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Callback 接口：POST /feishu/callback。
     * 飞书回调请求 URL；处理 URL 验证（challenge）或用户点击交互卡片按钮后的回调。
     * 文档：https://open.feishu.cn/document/feishu-cards/card-callback-communication
     */
    @PostMapping(path = "/callback", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> callbackPost(@RequestBody String rawBody) {
        ResponseEntity<?> early = parseAndCheck(rawBody);
        if (early != null) {
            return early;
        }
        FeishuParseResult result = parseFeishuBody(rawBody);
        if (result == null || result.getBody() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (result.getChallenge() != null) {
            return challengeResponse(result.getChallenge());
        }
        if (!feishuProperties.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        log.info("Feishu callback received");
        FeishuCallbackPayload payload;
        try {
            payload = objectMapper.treeToValue(result.getBody(), FeishuCallbackPayload.class);
        } catch (Exception e) {
            log.warn("Feishu callback body deserialize failed", e);
            return ResponseEntity.badRequest().build();
        }
        if (log.isDebugEnabled()) {
            log.debug("Feishu callback body: {}", result.getBody().toString());
        }
        try {
            ObjectNode callbackResponse = feishuCallbackService.handleCallback(payload);
            return ResponseEntity.ok(callbackResponse);
        } catch (Exception e) {
            log.error("Feishu callback error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Event / Callback 通用：空 body 时直接返回 400。
     */
    private ResponseEntity<?> parseAndCheck(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            log.warn("Feishu request empty body");
            return ResponseEntity.badRequest().build();
        }
        return null;
    }

    /**
     * Event / Callback 通用：解密（可选）、解析 JSON、提取 challenge。
     *
     * @return 解析结果，body 为 null 表示解密/解析失败；challenge 非空表示 URL 验证请求
     */
    private FeishuParseResult parseFeishuBody(String rawBody) {
        try {
            String json = rawBody.trim();
            if (feishuProperties.getEncryptKey() != null && !feishuProperties.getEncryptKey().trim().isEmpty()) {
                JsonNode node = objectMapper.readTree(json);
                if (node.has("encrypt")) {
                    String encrypt = node.path("encrypt").asText("");
                    if (!encrypt.isEmpty()) {
                        json = decryptFeishuBody(encrypt);
                        if (json == null) {
                            log.warn("Feishu decrypt failed");
                            return null;
                        }
                    }
                }
            }
            JsonNode body = objectMapper.readTree(json);
            String challenge = null;
            if (body.has("challenge")) {
                challenge = body.path("challenge").asText(null);
            } else if (body.has("CHALLENGE")) {
                challenge = body.path("CHALLENGE").asText(null);
            }
            if (challenge != null && challenge.isEmpty()) {
                challenge = null;
            }
            return FeishuParseResult.builder().body(body).challenge(challenge).build();
        } catch (Exception e) {
            log.error("Feishu parse error", e);
            return null;
        }
    }

    private ResponseEntity<?> challengeResponse(String challenge) {
        log.info("Feishu URL verification, returning challenge");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Collections.singletonMap("challenge", challenge));
    }

    /**
     * 飞书加密请求体解密（AES，飞书文档方案）；未配置 encrypt_key 时不调用。
     */
    private String decryptFeishuBody(String encrypt) {
        String key = feishuProperties.getEncryptKey();
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] keyBytes = java.security.MessageDigest.getInstance("SHA-256").digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] raw = java.util.Base64.getDecoder().decode(encrypt);
            if (raw.length < 16) {
                return null;
            }
            javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec(raw, 0, 16);
            javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, spec, iv);
            byte[] decrypted = cipher.doFinal(raw, 16, raw.length - 16);
            int padEnd = decrypted.length;
            while (padEnd > 0 && decrypted[padEnd - 1] == 0) {
                padEnd--;
            }
            return new String(decrypted, 0, padEnd, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Feishu decrypt error", e);
            return null;
        }
    }
}
