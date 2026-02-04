package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书 Event/Callback 请求体解析结果：解密后的 body 与 challenge（URL 校验用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuParseResult {

    private JsonNode body;
    private String challenge;
}
