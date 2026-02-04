package com.example.mock.parser.model.feishu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书事件订阅请求体中的 event（im.message.receive_v1：message + sender）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuEventBody {

    private FeishuMessage message;
    private FeishuSender sender;
    /** schema 1.0 时 event 内可能有 type。 */
    private String type;
}
