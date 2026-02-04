package com.example.mock.parser.service.feishu;

import com.example.mock.parser.model.feishu.FeishuCallbackAction;
import com.example.mock.parser.model.feishu.FeishuCallbackActionValue;
import com.example.mock.parser.model.feishu.FeishuCallbackPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 解析回调 action.value 为 FeishuCallbackActionValue（支持双重转义 JSON 字符串）。
 */
@Component
@RequiredArgsConstructor
public class CallbackActionValueParser {

    private final ObjectMapper objectMapper;

    /**
     * 从 payload 解析 preview_result_id；value 可能为双重转义 JSON 字符串。
     */
    public String parsePreviewResultId(FeishuCallbackPayload payload) {
        FeishuCallbackAction action = payload != null && payload.getEvent() != null ? payload.getEvent().getAction() : null;
        if (action == null || action.getValue() == null || action.getValue().isEmpty()) {
            return null;
        }
        return parsePreviewResultIdFromRaw(action.getValue());
    }

    public String parsePreviewResultIdFromRaw(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(raw);
            if (parsed.isTextual()) {
                parsed = objectMapper.readTree(parsed.asText());
            }
            FeishuCallbackActionValue value = objectMapper.treeToValue(parsed, FeishuCallbackActionValue.class);
            return value != null ? value.getPreviewResultId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
