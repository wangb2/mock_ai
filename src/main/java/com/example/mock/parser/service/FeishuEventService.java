package com.example.mock.parser.service;

import com.example.mock.parser.config.FeishuProperties;
import com.example.mock.parser.entity.FeishuProcessedMessageEntity;
import com.example.mock.parser.model.ManualPreviewResult;
import com.example.mock.parser.model.MockEndpointItem;
import com.example.mock.parser.model.feishu.FeishuCachedPreview;
import com.example.mock.parser.model.feishu.FeishuEventBody;
import com.example.mock.parser.model.feishu.FeishuEventPayload;
import com.example.mock.parser.model.feishu.FeishuImageContent;
import com.example.mock.parser.model.feishu.FeishuImageDownloadResult;
import com.example.mock.parser.model.feishu.FeishuMessage;
import com.example.mock.parser.model.feishu.FeishuPostContent;
import com.example.mock.parser.model.feishu.FeishuSender;
import com.example.mock.parser.model.feishu.FeishuTextContent;
import com.example.mock.parser.repository.FeishuProcessedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 飞书 Event 处理：接收消息事件（im.message.receive_v1）生成预览并回复 post + 交互卡片。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuEventService {

    private static final String EVENT_IM_MESSAGE_RECEIVE = "im.message.receive_v1";
    private static final String MESSAGE_TYPE_IMAGE = "image";
    private static final String MESSAGE_TYPE_POST = "post";
    private static final String CHAT_TYPE_GROUP = "group";
    private static final String DEFAULT_MSG = "请发送文字或图片描述接口需求，例如：GET 用户查询接口，返回用户信息。";

    private final FeishuProperties feishuProperties;
    private final FeishuApiService feishuApiService;
    private final FeishuPreviewCache feishuPreviewCache;
    private final MockEndpointService mockEndpointService;
    private final MockSceneService mockSceneService;
    private final QiniuService qiniuService;
    private final FeishuProcessedMessageRepository feishuProcessedMessageRepository;
    private final ObjectMapper objectMapper;

    @Value("${llm.chat-provider:doubao}")
    private String defaultChatProvider;

    /**
     * 处理飞书事件。根据 event_id 查询是否已存在，已存在则直接返回；否则先插入事件记录（占位），再处理，利用唯一约束防止并发重复处理。
     */
    public void handleEvent(FeishuEventPayload payload) {
        if (payload == null) {
            return;
        }
        String eventId = resolveEventId(payload);
        String eventType = resolveEventType(payload);
        if (eventId == null || eventId.isEmpty()) {
            return;
        }

        if (feishuProcessedMessageRepository.existsByEventId(eventId)) {
            log.debug("Feishu event already processed. event_id={}", eventId);
            return;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            feishuProcessedMessageRepository.save(FeishuProcessedMessageEntity.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .build());
        } catch (Exception e) {
            log.warn("Feishu save payload failed", e);
            return;
        }

        FeishuEventBody eventBody = payload.getEvent();
        if (eventBody == null) {
            return;
        }
        if (!EVENT_IM_MESSAGE_RECEIVE.equals(eventType)) {
            if (eventType != null && !eventType.isEmpty()) {
                log.debug("Feishu event ignored. event_type={}", eventType);
            }
            return;
        }

        log.info("Feishu event dispatch. event_type=im.message.receive_v1");
        handleMessageEvent(eventBody);
    }

    /**
     * Schema 2.0 从 header.event_type 取，Schema 1.0 从 event.type 取。
     */
    private String resolveEventType(FeishuEventPayload payload) {
        if ("2.0".equals(payload.getSchema()) && payload.getHeader() != null) {
            return payload.getHeader().getEventType();
        }
        if ("event_callback".equals(payload.getType()) && payload.getEvent() != null) {
            return payload.getEvent().getType();
        }
        return null;
    }

    private String resolveEventId(FeishuEventPayload payload) {
        if (payload.getHeader() != null && payload.getHeader().getEventId() != null && !payload.getHeader().getEventId().isEmpty()) {
            return payload.getHeader().getEventId();
        }
        return payload.getUuid();
    }

    /**
     * 处理接收消息事件。
     */
    public void handleMessageEvent(FeishuEventBody eventBody) {
        FeishuMessage message = eventBody != null ? eventBody.getMessage() : null;
        FeishuSender sender = eventBody != null ? eventBody.getSender() : null;
        if (message == null) {
            log.warn("Feishu event missing message");
            return;
        }
        String chatId = nullToEmpty(message.getChatId());
        String chatType = nullToEmpty(message.getChatType());
        String senderUserId = sender != null && sender.getSenderId() != null ? nullToEmpty(sender.getSenderId().getUserId()) : "";
        if (chatId.isEmpty()) {
            log.warn("Feishu event missing chat_id");
            return;
        }

        String messageType = message.getMessageType() != null ? message.getMessageType() : "text";
        log.info("Feishu message event. chat_id={}, message_type={}", chatId, messageType);

        MessageExtractResult extracted = extractMessages(message);
        if (extracted.errorMessage != null) {
            replyText(chatId, chatType, senderUserId, extracted.errorMessage);
            return;
        }
        if (extracted.messages == null || extracted.messages.isEmpty()) {
            replyText(chatId, chatType, senderUserId, DEFAULT_MSG);
            return;
        }

        ManualPreviewResult previewResult;
        try {
            previewResult = mockEndpointService.generateManualPreviewResult(extracted.messages, defaultChatProvider);
        } catch (IOException e) {
            log.error("Feishu manual preview failed", e);
            replyText(chatId, chatType, senderUserId, "生成预览失败，请稍后重试。");
            return;
        }

        if (previewResult == null) {
            replyText(chatId, chatType, senderUserId, "无法生成预览，请补充接口描述（如：请求方式、接口名称、请求/响应字段）。");
            return;
        }
        if (previewResult.isNeedMoreInfo()) {
            String msg = previewResult.getMessage() != null && !previewResult.getMessage().trim().isEmpty()
                    ? previewResult.getMessage()
                    : "请补充以下信息后再生成：" + (previewResult.getMissingFields() != null ? String.join("、", previewResult.getMissingFields()) : "");
            replyText(chatId, chatType, senderUserId, msg);
            return;
        }

        MockEndpointItem item = previewResult.getItem();
        if (item == null) {
            replyText(chatId, chatType, senderUserId, "无法生成预览。");
            return;
        }

        String previewResultId = UUID.randomUUID().toString().replace("-", "");
        FeishuCachedPreview cached = FeishuCachedPreview.builder()
                .createdAtMillis(System.currentTimeMillis())
                .chatId(chatId)
                .chatType(chatType)
                .senderUserId(senderUserId)
                .title(item.getTitle())
                .method(item.getMethod())
                .requestExample(item.getRequestExample())
                .responseExample(item.getResponseExample())
                .errorResponseExample(item.getErrorResponseExample())
                .requiredFields(item.getRequiredFields() != null ? item.getRequiredFields() : Collections.emptyList())
                .errorHttpStatus(item.getErrorHttpStatus())
                .build();
        feishuPreviewCache.put(previewResultId, cached);

        feishuApiService.sendInteractiveToChat(chatId, buildCreateButtonCard(previewResultId, item, chatType, senderUserId));
        log.info("Feishu preview sent. chat_id={}, title={}, method={}, preview_result_id={}", chatId, item.getTitle(), item.getMethod(), previewResultId);
    }

    /**
     * 按消息类型解析为供 LLM 使用的 messages，或返回错误提示。
     */
    private MessageExtractResult extractMessages(FeishuMessage message) {
        String raw = message.getContent();
        if (raw == null || raw.isEmpty()) {
            return MessageExtractResult.error(DEFAULT_MSG);
        }
        String messageType = message.getMessageType();
        if (MESSAGE_TYPE_IMAGE.equals(messageType)) {
            return extractImageMessages(message, raw);
        }
        if (MESSAGE_TYPE_POST.equals(messageType)) {
            return extractPostMessages(message, raw);
        }
        return extractTextMessages(raw);
    }

    private MessageExtractResult extractTextMessages(String raw) {
        try {
            FeishuTextContent content = objectMapper.readValue(raw, FeishuTextContent.class);
            String text = content != null ? content.getText() : null;
            if (text != null && !text.trim().isEmpty()) {
                return MessageExtractResult.ok(Collections.singletonList(text.trim()));
            }
            return MessageExtractResult.error(DEFAULT_MSG);
        } catch (Exception e) {
            log.debug("Feishu text parse failed, use raw", e);
            return MessageExtractResult.ok(Collections.singletonList(raw.trim()));
        }
    }

    private MessageExtractResult extractImageMessages(FeishuMessage message, String raw) {
        try {
            FeishuImageContent content = objectMapper.readValue(raw, FeishuImageContent.class);
            String imageKey = content != null ? content.getImageKey() : null;
            if (imageKey == null || imageKey.isEmpty()) {
                return MessageExtractResult.error("无法解析图片，请发送文字描述接口需求或重发图片。");
            }
            String messageId = message.getMessageId();
            if (messageId == null || messageId.isEmpty()) {
                return MessageExtractResult.error("无法获取消息 ID，请重试。");
            }
            String imageUrl = resolveImageUrl(messageId, imageKey);
            if (imageUrl == null || imageUrl.isEmpty()) {
                return MessageExtractResult.error("无法获取图片地址，请重试或发送文字描述接口需求。");
            }
            ArrayNode imageContent = objectMapper.createArrayNode();
            imageContent.add(objectMapper.createObjectNode()
                    .put("type", "image_url")
                    .set("image_url", objectMapper.createObjectNode().put("url", imageUrl)));
            return MessageExtractResult.ok(Collections.singletonList(imageContent));
        } catch (Exception e) {
            log.warn("Feishu image parse failed", e);
            return MessageExtractResult.error("无法解析图片，请重发或发送文字描述。");
        }
    }

    private MessageExtractResult extractPostMessages(FeishuMessage message, String raw) {
        try {
            FeishuPostContent content = objectMapper.readValue(raw, FeishuPostContent.class);
            if (content == null) {
                return MessageExtractResult.error(DEFAULT_MSG);
            }
            List<Object> messages = new ArrayList<>();
            String text = content.collectText();
            if (text != null && !text.trim().isEmpty()) {
                messages.add(text.trim());
            }
            String messageId = message.getMessageId();
            List<String> imageKeys = content.collectImageKeys();
            if (imageKeys != null && !imageKeys.isEmpty() && messageId != null && !messageId.isEmpty()) {
                ArrayNode imageContent = objectMapper.createArrayNode();
                for (String fileKey : imageKeys) {
                    String imageUrl = resolveImageUrl(messageId, fileKey);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        imageContent.add(objectMapper.createObjectNode()
                                .put("type", "image_url")
                                .set("image_url", objectMapper.createObjectNode().put("url", imageUrl)));
                    }
                }
                if (imageContent.size() > 0) {
                    messages.add(imageContent);
                }
            }
            if (messages.isEmpty()) {
                return MessageExtractResult.error(DEFAULT_MSG);
            }
            return MessageExtractResult.ok(messages);
        } catch (Exception e) {
            log.debug("Feishu post parse failed", e);
            return MessageExtractResult.error(DEFAULT_MSG);
        }
    }

    private String resolveImageUrl(String messageId, String fileKey) {
        FeishuImageDownloadResult downloaded = feishuApiService.downloadFeishuImage(messageId, fileKey);
        if (downloaded == null || downloaded.getData() == null || downloaded.getData().length == 0) {
            return null;
        }
        String ext = downloaded.getFileExtension();
        String qiniuKey = "feishu/" + fileKey.replaceAll("[^a-zA-Z0-9_.-]", "_") + "." + ext;
        String qiniuUrl = qiniuService.uploadBytes(downloaded.getData(), qiniuKey, downloaded.getContentType());
        if (qiniuUrl != null && !qiniuUrl.isEmpty()) {
            log.info("Feishu image uploaded to Qiniu. message_id={}, file_key={}, url={}", messageId, fileKey, qiniuUrl);
            return qiniuUrl;
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private String buildPreviewSummary(MockEndpointItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("接口：").append(item.getTitle()).append("，方法：").append(item.getMethod());
        if (item.getRequiredFields() != null && !item.getRequiredFields().isEmpty()) {
            sb.append("，必填：").append(String.join("、", item.getRequiredFields()));
        }
        sb.append("。点击下方「创建接口」保存到 Mock 服务。");
        return sb.toString();
    }

    private String buildPostContent(String chatType, String senderUserId, String text) {
        ObjectNode zhCn = objectMapper.createObjectNode();
        zhCn.put("title", "");
        ArrayNode content = zhCn.putArray("content");
        ArrayNode line = content.addArray();
        if (CHAT_TYPE_GROUP.equals(chatType) && senderUserId != null && !senderUserId.isEmpty()) {
            ObjectNode at = line.addObject();
            at.put("tag", "at");
            at.put("user_id", senderUserId);
        }
        ObjectNode textNode = line.addObject();
        textNode.put("tag", "text");
        textNode.put("text", " " + (text != null ? text : ""));
        ObjectNode root = objectMapper.createObjectNode();
        root.set("zh_cn", zhCn);
        return root.toString();
    }

    private static final int CARD_JSON_MAX_LEN = 400;

    /**
     * 构建「创建接口」交互卡片（飞书卡片 JSON 2.0），包含接口全部预览信息。
     * 群聊时在卡片内用 lark_md 直接 @ 用户，仅发一条消息。
     */
    private String buildCreateButtonCard(String previewResultId, MockEndpointItem item, String chatType, String senderUserId) {
        ObjectNode card = objectMapper.createObjectNode();
        ObjectNode config = objectMapper.createObjectNode();
        config.put("wide_screen_mode", true);
        card.set("config", config);

        ObjectNode header = objectMapper.createObjectNode();
        header.put("template", "blue");
        ObjectNode headerTitle = objectMapper.createObjectNode();
        headerTitle.put("tag", "plain_text");
        headerTitle.put("content", "接口预览");
        headerTitle.put("lines", 1);
        header.set("title", headerTitle);
        card.set("header", header);

        ArrayNode elements = card.putArray("elements");

        // 群聊时在卡片内 @ 用户（lark_md 语法 <at id="ou_xxx"></at>，@所有人用 id=all）
        if (CHAT_TYPE_GROUP.equals(chatType) && senderUserId != null && !senderUserId.isEmpty()) {
            String atContent = "<at id=\"" + escapeLarkMd(senderUserId) + "\"></at> 请查看以下接口预览，确认无误后点击下方「创建接口」保存到 Mock 服务。";
            addDivLarkMd(elements, atContent);
        }

        // 1. 基本信息：接口名称 · 请求方法（同一行更紧凑）
        String title = item.getTitle() != null ? item.getTitle() : "";
        String method = item.getMethod() != null ? item.getMethod() : "";
        addDivLarkMd(elements, "**接口名称**：" + title + "　·　**请求方法**：`" + method + "`");

        // 2. 必填参数
        if (item.getRequiredFields() != null && !item.getRequiredFields().isEmpty()) {
            addDivLarkMd(elements, "**必填参数**：" + String.join("、", item.getRequiredFields()));
        }

        // 3. 错误状态码（有则显示，放在示例前）
        if (item.getErrorHttpStatus() != null) {
            addDivLarkMd(elements, "**错误状态码**：`" + item.getErrorHttpStatus() + "`");
        }

        elements.addObject().put("tag", "hr");

        // 4. 请求示例（有内容才展示）
        if (item.getRequestExample() != null && !item.getRequestExample().isNull()) {
            String req = truncateForCard(item.getRequestExample().toString());
            if (!isBlankJson(req)) {
                addDivWithLabel(elements, "请求示例", req);
            }
        }

        // 5. 响应示例（有内容才展示）
        if (item.getResponseExample() != null && !item.getResponseExample().isNull()) {
            String resp = truncateForCard(item.getResponseExample().toString());
            if (!isBlankJson(resp)) {
                addDivWithLabel(elements, "响应示例", resp);
            }
        }

        // 6. 错误响应示例（仅当非空且不是 {} 时展示）
        if (item.getErrorResponseExample() != null && !item.getErrorResponseExample().isNull()) {
            String err = truncateForCard(item.getErrorResponseExample().toString());
            if (!isBlankJson(err)) {
                addDivWithLabel(elements, "错误响应示例", err);
            }
        }

        elements.addObject().put("tag", "hr");

        // 非群聊或未 @ 时仍显示说明文案
        if (!CHAT_TYPE_GROUP.equals(chatType) || senderUserId == null || senderUserId.isEmpty()) {
            ObjectNode note = elements.addObject();
            note.put("tag", "note");
            ArrayNode noteElements = note.putArray("elements");
            ObjectNode noteText = noteElements.addObject();
            noteText.put("tag", "plain_text");
            noteText.put("content", "确认无误后点击下方「创建接口」，将接口保存到 Mock 服务。");
            noteText.put("lines", 1);
        }

        ObjectNode action = elements.addObject();
        action.put("tag", "action");
        ArrayNode actions = action.putArray("actions");
        ObjectNode button = actions.addObject();
        button.put("tag", "button").put("type", "primary");
        button.putObject("text").put("tag", "plain_text").put("content", "创建接口");
        ObjectNode valueObj = objectMapper.createObjectNode();
        valueObj.put("preview_result_id", previewResultId);
        button.put("value", valueObj.toString());
        return card.toString();
    }

    private void addDivLarkMd(ArrayNode elements, String content) {
        ObjectNode div = elements.addObject();
        div.put("tag", "div");
        div.putObject("text").put("tag", "lark_md").put("content", content);
    }

    private void addDivWithLabel(ArrayNode elements, String label, String body) {
        addDivLarkMd(elements, "**" + label + "**\n```\n" + escapeCodeBlock(body) + "\n```");
    }

    private static String escapeCodeBlock(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("```", "` ` `");
    }

    /**
     * lark_md 中防止 at 的 id 含引号等破坏标签。
     */
    private static String escapeLarkMd(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 判断是否为无意义的空 JSON，此类内容不展示。
     */
    private static boolean isBlankJson(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        return t.isEmpty() || "{}".equals(t) || "null".equals(t);
    }

    private String truncateForCard(String json) {
        if (json == null) {
            return "";
        }
        json = json.trim();
        if (json.length() <= CARD_JSON_MAX_LEN) {
            return json;
        }
        return json.substring(0, CARD_JSON_MAX_LEN) + "\n...";
    }

    private void replyText(String chatId, String chatType, String senderUserId, String text) {
        feishuApiService.sendPostToChat(chatId, buildPostContent(chatType, senderUserId, text));
    }

    /**
     * 消息解析结果：成功时 messages 非空、errorMessage 为 null；失败时反之。
     */
    private static class MessageExtractResult {
        final List<Object> messages;
        final String errorMessage;

        MessageExtractResult(List<Object> messages, String errorMessage) {
            this.messages = messages;
            this.errorMessage = errorMessage;
        }

        static MessageExtractResult ok(List<Object> messages) {
            return new MessageExtractResult(messages, null);
        }

        static MessageExtractResult error(String errorMessage) {
            return new MessageExtractResult(null, errorMessage);
        }
    }
}
