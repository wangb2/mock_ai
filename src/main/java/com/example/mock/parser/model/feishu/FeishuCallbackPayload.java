package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书卡片回调请求体（Callback：POST /feishu/callback）。
 * schema 2.0：schema、header、event（operator、action、context）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuCallbackPayload {

    private String schema;
    private FeishuEventHeader header;
    private FeishuCallbackEventBody event;
    /** schema 1.0 兼容 */
    @JsonProperty("open_chat_id")
    private String openChatId;
    @JsonProperty("open_id")
    private String openId;
    private FeishuCallbackContext context;
    private FeishuOperator user;
}
