package com.example.mock.parser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 飞书入口配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {

    private boolean enabled = true;
    private String appId = "";
    private String appSecret = "";
    private String verificationToken = "";
    private String encryptKey = "";
    private String defaultSceneId = "";
    private int previewCacheTtlMinutes = 30;

    /**
     * 是否已配置飞书应用（app-id 非空则视为已配置）。
     */
    public boolean isConfigured() {
        return enabled && appId != null && !appId.trim().isEmpty();
    }
}
