package com.example.mock.parser.service;

import com.example.mock.parser.config.FeishuProperties;
import com.example.mock.parser.model.feishu.FeishuImageDownloadResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 飞书开放接口调用：使用 RestTemplate 请求飞书 HTTP API（获取 token、发送消息等）。
 * 不依赖飞书 SDK；Event / Callback 由 FeishuController 按开放平台文档编程处理。
 * Event：https://open.feishu.cn/document/event-subscription-guide/event-subscriptions/event-subscription-configure-/choose-a-subscription-mode/send-notifications-to-developers-server
 * Callback：https://open.feishu.cn/document/event-subscription-guide/callback-subscription/step-1-choose-a-subscription-mode/send-callbacks-to-developers-server
 */
@Service
public class FeishuApiService {

    private static final Logger log = LoggerFactory.getLogger(FeishuApiService.class);
    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";
    /**
     * 获取消息中的资源文件（含图片）：GET /open-apis/im/v1/messages/{message_id}/resources/{file_key}?type=image。
     * 见 https://open.feishu.cn/document/server-docs/im-v1/message/get-2
     */
    private static final String MESSAGE_RESOURCE_PREFIX = "https://open.feishu.cn/open-apis/im/v1/messages/";
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300;

    private final FeishuProperties feishuProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile String cachedToken;
    private volatile long tokenExpireAtMillis;

    public FeishuApiService(FeishuProperties feishuProperties,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.feishuProperties = feishuProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取 tenant_access_token。使用 RestTemplate 请求飞书接口并缓存。
     */
    public String getTenantAccessToken() {
        if (!feishuProperties.isConfigured()) {
            log.warn("Feishu not configured, cannot get token");
            return null;
        }
        return getTenantAccessTokenByRest();
    }

    private String getTenantAccessTokenByRest() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireAtMillis - TOKEN_REFRESH_BUFFER_SECONDS * 1000) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && System.currentTimeMillis() < tokenExpireAtMillis - TOKEN_REFRESH_BUFFER_SECONDS * 1000) {
                return cachedToken;
            }
            ObjectNode body = objectMapper.createObjectNode();
            body.put("app_id", feishuProperties.getAppId());
            body.put("app_secret", feishuProperties.getAppSecret());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(TOKEN_URL, entity, String.class);
                if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
                    log.error("Feishu token request failed. status={}", resp.getStatusCode());
                    return null;
                }
                JsonNode node = objectMapper.readTree(resp.getBody());
                int code = node.path("code").asInt(-1);
                if (code != 0) {
                    log.error("Feishu token response error. code={}, msg={}", code, node.path("msg").asText(""));
                    return null;
                }
                cachedToken = node.path("tenant_access_token").asText(null);
                int expire = node.path("expire").asInt(7200);
                tokenExpireAtMillis = System.currentTimeMillis() + expire * 1000L;
                log.info("Feishu tenant_access_token refreshed. expireIn={}s", expire);
                return cachedToken;
            } catch (Exception e) {
                log.error("Feishu token request exception", e);
                return null;
            }
        }
    }

    /**
     * 发送消息。使用 RestTemplate 调用飞书开放接口。
     *
     * @param receiveIdType "chat_id" 或 "user_id"
     * @param receiveId    群 ID（oc_xxx）或用户 ID（ou_xxx）
     * @param msgType      "text"、"post"、"interactive"
     * @param content      JSON 字符串（飞书 content 字段要求）
     * @return 成功时返回消息 ID，失败返回 null
     */
    public String sendMessage(String receiveIdType, String receiveId, String msgType, String content) {
        if (receiveId == null || receiveId.isEmpty()) {
            return null;
        }
        return sendMessageByRest(receiveIdType, receiveId, msgType, content);
    }

    private String sendMessageByRest(String receiveIdType, String receiveId, String msgType, String content) {
        String token = getTenantAccessTokenByRest();
        if (token == null) {
            return null;
        }
        String url = MESSAGE_URL + "?receive_id_type=" + receiveIdType;
        ObjectNode body = objectMapper.createObjectNode();
        body.put("receive_id", receiveId);
        body.put("msg_type", msgType);
        body.put("content", content == null ? "{}" : content);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            if (resp.getStatusCode() != HttpStatus.OK || resp.getBody() == null) {
                log.warn("Feishu send message failed. status={}", resp.getStatusCode());
                return null;
            }
            JsonNode node = objectMapper.readTree(resp.getBody());
            int code = node.path("code").asInt(-1);
            if (code != 0) {
                log.warn("Feishu send message error. code={}, msg={}", code, node.path("msg").asText(""));
                return null;
            }
            JsonNode data = node.path("data");
            return data.path("message_id").asText(null);
        } catch (Exception e) {
            log.error("Feishu send message exception", e);
            return null;
        }
    }

    /**
     * 向群聊发送 post 消息（可含 @ 用户）。
     */
    public String sendPostToChat(String chatId, String postContent) {
        return sendMessage("chat_id", chatId, "post", postContent);
    }

    /**
     * 向群聊发送 interactive（交互卡片）消息。
     */
    public String sendInteractiveToChat(String chatId, String interactiveContent) {
        return sendMessage("chat_id", chatId, "interactive", interactiveContent);
    }

    /**
     * 下载飞书消息中的资源文件（如图片）为二进制，用于上传七牛等后续处理。
     * 使用「获取消息中的资源文件」接口：GET /open-apis/im/v1/messages/{message_id}/resources/{file_key}?type=image（type=image 必填）。
     * 见 <a href="https://open.feishu.cn/document/server-docs/im-v1/message/get-2">获取消息中的资源文件</a>。
     *
     * @param messageId 消息 ID（来自 event.message.message_id）
     * @param fileKey   资源 key（图片消息为 content.image_key）
     * @return 图片二进制与 Content-Type，失败返回 null
     */
    public FeishuImageDownloadResult downloadFeishuImage(String messageId, String fileKey) {
        if (messageId == null || messageId.trim().isEmpty() || fileKey == null || fileKey.trim().isEmpty()) {
            return null;
        }
        String token = getTenantAccessTokenByRest();
        if (token == null) {
            return null;
        }
        String url = MESSAGE_RESOURCE_PREFIX + messageId.trim() + "/resources/" + fileKey.trim() + "?type=image";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null && resp.getBody().length > 0) {
                String ct = resp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                log.info("Feishu message resource downloaded. message_id={}, file_key={}, size={}", messageId, fileKey, resp.getBody().length);
                return FeishuImageDownloadResult.builder()
                        .data(resp.getBody())
                        .contentType(ct != null ? ct : "image/jpeg")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Feishu download message resource failed. message_id={}, file_key={}", messageId, fileKey, e);
        }
        return null;
    }
}
