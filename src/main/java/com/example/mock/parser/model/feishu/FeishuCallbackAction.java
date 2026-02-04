package com.example.mock.parser.model.feishu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书卡片回调中的 action（按钮 value、tag）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuCallbackAction {

    private String value;
    private String tag;
}
