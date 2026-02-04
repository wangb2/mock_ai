package com.example.mock.parser.model.feishu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 飞书 post 消息 content 结构：content 为二维数组，每行多个 FeishuPostItem。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuPostContent {

    private List<List<FeishuPostItem>> content;

    public List<List<FeishuPostItem>> getContent() {
        return content == null ? Collections.emptyList() : content;
    }

    /** 按顺序收集所有 image_key。 */
    public List<String> collectImageKeys() {
        return getContent().stream()
                .flatMap(List::stream)
                .filter(item -> item != null && "img".equals(item.getTag()))
                .map(FeishuPostItem::getImageKey)
                .filter(key -> key != null && !key.isEmpty())
                .collect(Collectors.toList());
    }

    /** 按顺序拼接所有 text。 */
    public String collectText() {
        StringBuilder sb = new StringBuilder();
        for (List<FeishuPostItem> line : getContent()) {
            if (line == null) {
                continue;
            }
            for (FeishuPostItem item : line) {
                if (item != null && "text".equals(item.getTag()) && item.getText() != null && !item.getText().isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(item.getText());
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
