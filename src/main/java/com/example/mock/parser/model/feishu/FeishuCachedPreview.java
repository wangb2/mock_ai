package com.example.mock.parser.model.feishu;

import com.example.mock.parser.config.FeishuProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 飞书预览结果缓存项：预览数据 + 会话信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuCachedPreview {

    private long createdAtMillis;
    private String chatId;
    private String chatType;
    private String senderUserId;
    private String title;
    private String method;
    private JsonNode requestExample;
    private JsonNode responseExample;
    private JsonNode errorResponseExample;
    private List<String> requiredFields;
    private Integer errorHttpStatus;

    public boolean isExpired(int ttlMinutes) {
        return ttlMinutes <= 0 || (System.currentTimeMillis() - createdAtMillis) > ttlMinutes * 60_000L;
    }
}
