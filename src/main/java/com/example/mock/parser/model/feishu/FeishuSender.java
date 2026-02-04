package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书事件发送者。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuSender {

    @JsonProperty("sender_id")
    private FeishuSenderId senderId;
    @JsonProperty("sender_type")
    private String senderType;
    @JsonProperty("tenant_key")
    private String tenantKey;
}
