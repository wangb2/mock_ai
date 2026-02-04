package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书事件消息体（im.message.receive_v1 中的 message）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuMessage {

    @JsonProperty("chat_id")
    private String chatId;
    @JsonProperty("chat_type")
    private String chatType;
    private String content;
    @JsonProperty("create_time")
    private String createTime;
    @JsonProperty("message_id")
    private String messageId;
    @JsonProperty("message_type")
    private String messageType;
    @JsonProperty("update_time")
    private String updateTime;
}
