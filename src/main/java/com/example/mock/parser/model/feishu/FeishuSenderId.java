package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书事件发送者 ID。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuSenderId {

    @JsonProperty("open_id")
    private String openId;
    @JsonProperty("union_id")
    private String unionId;
    @JsonProperty("user_id")
    private String userId;
}
