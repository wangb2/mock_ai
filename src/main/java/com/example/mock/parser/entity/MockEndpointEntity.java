package com.example.mock.parser.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "mock_endpoint")
public class MockEndpointEntity {
    @Id
    @Column(length = 64)
    private String id;

    private String title;
    private String method;
    private String mockUrl;

    @Lob
    private String requestExample;

    @Lob
    private String responseExample;

    @Lob
    private String errorResponseExample;

    @Lob
    private String requiredFields;

    @Lob
    private String raw;

    private String sourceFileId;
    private String sourceFileName;
    private String sourceFileUrl;
    private String sceneId;
    private String sceneName;
    @Column(name = "error_http_status")
    private Integer errorHttpStatus;
    @Column(name = "response_delay_ms")
    private Integer responseDelayMs;
    @Column(length = 512)
    private String apiPath;

    @Column(name = "response_mode", length = 32)
    private String responseMode;

    @Lob
    @Column(name = "response_script")
    private String responseScript;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMockUrl() {
        return mockUrl;
    }

    public void setMockUrl(String mockUrl) {
        this.mockUrl = mockUrl;
    }

    public String getRequestExample() {
        return requestExample;
    }

    public void setRequestExample(String requestExample) {
        this.requestExample = requestExample;
    }

    public String getResponseExample() {
        return responseExample;
    }

    public void setResponseExample(String responseExample) {
        this.responseExample = responseExample;
    }

    public String getErrorResponseExample() {
        return errorResponseExample;
    }

    public void setErrorResponseExample(String errorResponseExample) {
        this.errorResponseExample = errorResponseExample;
    }

    public String getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(String requiredFields) {
        this.requiredFields = requiredFields;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getSourceFileId() {
        return sourceFileId;
    }

    public void setSourceFileId(String sourceFileId) {
        this.sourceFileId = sourceFileId;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSourceFileUrl() {
        return sourceFileUrl;
    }

    public void setSourceFileUrl(String sourceFileUrl) {
        this.sourceFileUrl = sourceFileUrl;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }

    public String getSceneName() {
        return sceneName;
    }

    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }

    public Integer getErrorHttpStatus() {
        return errorHttpStatus;
    }

    public void setErrorHttpStatus(Integer errorHttpStatus) {
        this.errorHttpStatus = errorHttpStatus;
    }

    public Integer getResponseDelayMs() {
        return responseDelayMs;
    }

    public void setResponseDelayMs(Integer responseDelayMs) {
        this.responseDelayMs = responseDelayMs;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getResponseScript() {
        return responseScript;
    }

    public void setResponseScript(String responseScript) {
        this.responseScript = responseScript;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
