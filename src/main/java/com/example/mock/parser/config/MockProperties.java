package com.example.mock.parser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mock 服务相关配置（如接口根地址，用于返回可直接访问的完整 URL）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mock")
public class MockProperties {

    /** Mock 接口根地址，如 http://10.18.40.27:8080。为空则仅返回 path。 */
    private String baseUrl = "";

    /**
     * 将 path（如 /parse/mock/xxx）转为可直接访问的完整 URL。
     * 若 baseUrl 未配置或 path 已是 http(s) 开头，则返回 path 本身。
     */
    public String toAbsoluteMockUrl(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String base = baseUrl != null ? baseUrl.trim() : "";
        if (base.isEmpty()) {
            return path;
        }
        String baseNorm = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String pathNorm = path.startsWith("/") ? path : "/" + path;
        return baseNorm + pathNorm;
    }
}
