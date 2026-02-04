package com.example.mock.parser.model.feishu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书事件订阅请求体（Event：POST /feishu/event）。
 * schema 2.0：schema、header、event；schema 1.0：type、event、uuid。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuEventPayload {

    private String schema;
    private FeishuEventHeader header;
    private FeishuEventBody event;
    /** schema 1.0：event_callback */
    private String type;
    /** schema 1.0 事件去重用 */
    private String uuid;
}
