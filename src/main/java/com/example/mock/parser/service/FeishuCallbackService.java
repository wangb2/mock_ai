package com.example.mock.parser.service;

import com.example.mock.parser.config.FeishuProperties;
import com.example.mock.parser.config.MockProperties;
import com.example.mock.parser.model.MockEndpointItem;
import com.example.mock.parser.model.MockSceneItem;
import com.example.mock.parser.model.feishu.FeishuCachedPreview;
import com.example.mock.parser.model.feishu.FeishuCallbackPayload;
import com.example.mock.parser.model.feishu.FeishuOperator;
import com.example.mock.parser.service.feishu.CallbackActionValueParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 飞书 Callback 处理：根据预览缓存创建接口并回复接口详情，返回符合飞书回调响应格式的 JSON。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuCallbackService {

    private static final String CHAT_TYPE_GROUP = "group";

    private final FeishuProperties feishuProperties;
    private final MockProperties mockProperties;
    private final FeishuApiService feishuApiService;
    private final FeishuPreviewCache feishuPreviewCache;
    private final MockEndpointService mockEndpointService;
    private final MockSceneService mockSceneService;
    private final CallbackActionValueParser callbackActionValueParser;
    private final ObjectMapper objectMapper;
    private final FeishuEventService feishuEventService;

    /**
     * 处理 Callback：根据 preview_result_id 取缓存，创建接口，回复接口详情。
     * 返回符合飞书回调响应格式的 JSON（含 toast），供 Controller 原样返回。
     *
     * @return 飞书要求的回调响应体（toast 等），不可为 null
     */
    public ObjectNode handleCallback(FeishuCallbackPayload payload) {
        ObjectNode toastResponse = objectMapper.createObjectNode();
        if (payload == null) {
            return buildCallbackToast(toastResponse, "error", "参数错误", "Invalid request");
        }
        String previewResultId = callbackActionValueParser.parsePreviewResultId(payload);
        if (previewResultId == null || previewResultId.isEmpty()) {
            log.warn("Feishu callback missing action value");
            return buildCallbackToast(toastResponse, "error", "参数错误", "Invalid request");
        }
        log.info("Feishu callback. preview_result_id={}", previewResultId);

        FeishuCachedPreview cached = feishuPreviewCache.get(previewResultId);
        if (cached == null) {
            log.warn("Feishu callback cache miss (expired). preview_result_id={}", previewResultId);
            String chatId = extractChatIdFromCallback(payload);
            String userId = extractUserIdFromCallback(payload);
            if (chatId != null && !chatId.isEmpty()) {
                replyTextToChat(chatId, userId != null ? userId : "", "group", "预览已过期，请重新描述接口后再点击创建。");
            }
            return buildCallbackToast(toastResponse, "error", "预览已过期，请重新描述接口后再点击创建。", "Preview expired, please describe again.");
        }

        String sceneId = feishuProperties.getDefaultSceneId() != null && !feishuProperties.getDefaultSceneId().trim().isEmpty()
                ? feishuProperties.getDefaultSceneId().trim()
                : null;
        String sceneName = null;
        if (sceneId == null) {
            List<MockSceneItem> scenes = mockSceneService.listScenes();
            if (scenes != null && !scenes.isEmpty()) {
                sceneId = scenes.get(0).getId();
                sceneName = scenes.get(0).getName();
            }
        } else {
            MockSceneItem scene = mockSceneService.getScene(sceneId);
            if (scene != null) {
                sceneName = scene.getName();
            }
        }
        if (sceneId == null || sceneName == null) {
            replyTextToChat(cached.getChatId(), cached.getSenderUserId(), cached.getChatType(), "创建失败：未配置默认场景，请在后台添加场景或配置 feishu.default-scene-id。");
            return buildCallbackToast(toastResponse, "error", "创建失败：未配置默认场景。", "Create failed: no default scene.");
        }

        MockEndpointItem created = mockEndpointService.createManualEndpoint(
                cached.getTitle(),
                cached.getMethod(),
                cached.getRequestExample(),
                cached.getResponseExample(),
                cached.getErrorResponseExample(),
                cached.getRequiredFields(),
                sceneId,
                sceneName,
                cached.getErrorHttpStatus(),
                0,
                "template",
                "");

        if (created == null) {
            replyTextToChat(cached.getChatId(), cached.getSenderUserId(), cached.getChatType(), "创建失败：请求/响应数据无效。");
            return buildCallbackToast(toastResponse, "error", "创建失败：请求/响应数据无效。", "Create failed: invalid data.");
        }

        String successCard = buildCreateSuccessCard(created, cached.getChatType(), cached.getSenderUserId());
        feishuApiService.sendInteractiveToChat(cached.getChatId(), successCard);
        feishuPreviewCache.remove(previewResultId);
        // 创建接口卡片后，清除该用户的会话缓存，表示会话完成（使用unionId作为key）
        if (payload.getUser() != null) {
            FeishuOperator user = payload.getUser();
            feishuEventService.clearSession(user.getUnionId());
            feishuEventService.clearSession(user.getOpenId());
            feishuEventService.clearSession(user.getUserId());
        }
        log.info("Feishu callback created. preview_result_id={}, endpoint_id={}, title={}", previewResultId, created.getId(), created.getTitle());
        return buildCallbackToast(toastResponse, "success", "创建成功，接口详情已发送。", "Created successfully, details sent.");
    }

    private ObjectNode buildCallbackToast(ObjectNode root, String type, String contentZh, String contentEn) {
        ObjectNode toast = root.putObject("toast");
        toast.put("type", type);
        toast.put("content", contentZh);
        ObjectNode i18n = toast.putObject("i18n");
        i18n.put("zh_cn", contentZh);
        i18n.put("en_us", contentEn);
        return root;
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

    /**
     * 构建「接口创建成功」交互卡片（飞书卡片 JSON 2.0），全部使用飞书官方卡片元素。
     */
    private String buildCreateSuccessCard(MockEndpointItem item, String chatType, String senderUserId) {
        ObjectNode card = objectMapper.createObjectNode();
        ObjectNode config = objectMapper.createObjectNode();
        config.put("wide_screen_mode", true);
        card.set("config", config);

        ObjectNode header = objectMapper.createObjectNode();
        header.put("template", "green");
        ObjectNode headerTitle = objectMapper.createObjectNode();
        headerTitle.put("tag", "plain_text");
        headerTitle.put("content", "接口创建成功");
        headerTitle.put("lines", 1);
        header.set("title", headerTitle);
        card.set("header", header);

        ArrayNode elements = card.putArray("elements");

        // 群聊时 @ 用户（lark_md）
        if (CHAT_TYPE_GROUP.equals(chatType) && senderUserId != null && !senderUserId.isEmpty()) {
            String atContent = "<at id=\"" + escapeLarkMd(senderUserId) + "\"></at> 接口已创建，地址如下：";
            addDivLarkMd(elements, atContent);
        }

        // 接口地址：使用 mock.base-url 转为可直接访问的完整 URL，未配置则用 path
        String path = item.getMockUrl() != null ? item.getMockUrl() : "";
        String mockUrl = mockProperties.toAbsoluteMockUrl(path);
        addDivLarkMd(elements, "**接口地址**\n");
        ObjectNode urlDiv = elements.addObject();
        urlDiv.put("tag", "div");
        ObjectNode urlText = urlDiv.putObject("text");
        urlText.put("tag", "plain_text");
        urlText.put("content", mockUrl);
        urlText.put("lines", 3);

        // curl 脚本（可直接复制执行）
        String curlScript = buildCurlScript(mockUrl, item);
        if (!curlScript.isEmpty()) {
            addDivLarkMd(elements, "**curl**\n");
            ObjectNode curlDiv = elements.addObject();
            curlDiv.put("tag", "div");
            ObjectNode curlText = curlDiv.putObject("text");
            curlText.put("tag", "plain_text");
            curlText.put("content", curlScript);
            curlText.put("lines", 10);
        }

        return card.toString();
    }

    /**
     * 根据接口方法与请求示例生成 curl 命令（可直接复制执行）。
     */
    private String buildCurlScript(String mockUrl, MockEndpointItem item) {
        if (mockUrl == null || mockUrl.isEmpty()) {
            return "";
        }
        String method = item.getMethod() != null ? item.getMethod().trim().toUpperCase() : "POST";
        if ("GET".equals(method)) {
            return "curl -X GET '" + escapeShell(mockUrl) + "'";
        }
        // POST：带 Content-Type 与 -d body，美化格式
        JsonNode requestExample = item.getRequestExample();
        String bodyJson = "{}";
        if (requestExample != null && !requestExample.isNull()) {
            try {
                if (requestExample.has("body") && requestExample.get("body").isObject()) {
                    bodyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestExample.get("body"));
                } else {
                    bodyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestExample);
                }
            } catch (Exception e) {
                log.warn("Build curl: serialize request body failed", e);
            }
        }
        // 转义单引号用于 shell
        bodyJson = bodyJson.replace("\\", "\\\\").replace("'", "'\\''");
        // 美化：使用反斜杠换行，每部分独立一行
        StringBuilder sb = new StringBuilder();
        sb.append("curl -X POST '").append(escapeShell(mockUrl)).append("' \\\n");
        sb.append("  -H 'Content-Type: application/json' \\\n");
        sb.append("  -d '").append(bodyJson).append("'");
        return sb.toString();
    }

    private static String escapeShell(String s) {
        if (s == null) return "";
        return s.replace("'", "'\\''");
    }

    /** 飞书卡片：div + lark_md（官方元素） */
    private void addDivLarkMd(ArrayNode elements, String content) {
        ObjectNode div = elements.addObject();
        div.put("tag", "div");
        div.putObject("text").put("tag", "lark_md").put("content", content);
    }

    private static String escapeLarkMd(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String buildEndpointDetailText(MockEndpointItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("接口：").append(item.getTitle()).append("\n");
        sb.append("方法：").append(item.getMethod()).append("\n");
        sb.append("Mock 地址：").append(item.getMockUrl()).append("\n");
        if (item.getRequiredFields() != null && !item.getRequiredFields().isEmpty()) {
            sb.append("必填参数：").append(String.join("、", item.getRequiredFields())).append("\n");
        }
        if (item.getRequestExample() != null && !item.getRequestExample().isNull()) {
            String req = item.getRequestExample().toString();
            if (req.length() > 200) {
                req = req.substring(0, 200) + "...";
            }
            sb.append("请求示例：").append(req).append("\n");
        }
        if (item.getResponseExample() != null && !item.getResponseExample().isNull()) {
            String resp = item.getResponseExample().toString();
            if (resp.length() > 200) {
                resp = resp.substring(0, 200) + "...";
            }
            sb.append("响应示例：").append(resp);
        }
        return sb.toString();
    }

    private void replyTextToChat(String chatId, String userId, String chatType, String text) {
        String postContent = buildPostContent(chatType, userId, text);
        feishuApiService.sendPostToChat(chatId, postContent);
    }

    /** Schema 2.0 在 event.context，否则在 payload.context 或 payload.open_chat_id。 */
    private String extractChatIdFromCallback(FeishuCallbackPayload payload) {
        if (payload.getEvent() != null && payload.getEvent().getContext() != null) {
            String id = payload.getEvent().getContext().getOpenChatId();
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }
        if (payload.getContext() != null) {
            String id = payload.getContext().getOpenChatId();
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }
        return payload.getOpenChatId();
    }

    /** Schema 2.0 在 event.operator，否则在 payload.open_id 或 payload.user。 */
    private String extractUserIdFromCallback(FeishuCallbackPayload payload) {
        if (payload.getEvent() != null && payload.getEvent().getOperator() != null) {
            String userId = payload.getEvent().getOperator().getUserId();
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }
            String openId = payload.getEvent().getOperator().getOpenId();
            if (openId != null && !openId.isEmpty()) {
                return openId;
            }
        }
        if (payload.getOpenId() != null && !payload.getOpenId().isEmpty()) {
            return payload.getOpenId();
        }
        if (payload.getUser() != null && payload.getUser().getOpenId() != null && !payload.getUser().getOpenId().isEmpty()) {
            return payload.getUser().getOpenId();
        }
        return null;
    }
}
