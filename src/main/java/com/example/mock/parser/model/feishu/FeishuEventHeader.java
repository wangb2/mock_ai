package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书事件/回调公共请求头（schema 2.0）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuEventHeader {

    @JsonProperty("event_id")
    private String eventId;
    private String token;
    @JsonProperty("create_time")
    private String createTime;
    @JsonProperty("event_type")
    private String eventType;
    @JsonProperty("tenant_key")
    private String tenantKey;
    @JsonProperty("app_id")
    private String appId;
}
