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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
