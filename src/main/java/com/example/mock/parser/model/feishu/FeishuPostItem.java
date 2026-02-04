package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书 post 消息 content 中单行内的一个元素（tag=text 或 tag=img）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuPostItem {

    private String tag;
    private String text;
    @JsonProperty("image_key")
    private String imageKey;
}
