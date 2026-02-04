package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书卡片回调中的 context（open_message_id、open_chat_id）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuCallbackContext {

    @JsonProperty("open_message_id")
    private String openMessageId;
    @JsonProperty("open_chat_id")
    private String openChatId;
}
