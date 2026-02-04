package com.example.mock.parser.model.feishu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 飞书图片下载结果：二进制数据与 Content-Type。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuImageDownloadResult {

    private byte[] data;
    private String contentType;

    /** 根据 Content-Type 返回文件扩展名。 */
    public String getFileExtension() {
        if (contentType == null) {
            return "jpg";
        }
        String lower = contentType.toLowerCase();
        if (lower.contains("png")) {
            return "png";
        }
        if (lower.contains("gif")) {
            return "gif";
        }
        if (lower.contains("webp")) {
            return "webp";
        }
        return "jpg";
    }
}
