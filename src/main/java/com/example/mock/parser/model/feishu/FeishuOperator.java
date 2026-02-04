package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书卡片回调中的操作者（user_id、open_id 等）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuOperator {

    @JsonProperty("tenant_key")
    private String tenantKey;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("open_id")
    private String openId;
    @JsonProperty("union_id")
    private String unionId;
}
