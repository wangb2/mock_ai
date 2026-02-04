package com.example.mock.parser.model.feishu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书卡片回调请求体中的 event（operator、action、context 等）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuCallbackEventBody {

    private FeishuOperator operator;
    private String token;
    private FeishuCallbackAction action;
    private String host;
    private FeishuCallbackContext context;
}
