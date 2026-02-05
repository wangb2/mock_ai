package com.example.mock.parser.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class MockEndpointItem {
    private String id;
    private String title;
    private String method;
    private String mockUrl;
    private JsonNode requestExample;
    private JsonNode responseExample;
    private JsonNode errorResponseExample;
    private List<String> requiredFields = new ArrayList<>();
    private String raw;
    private String sourceFileId;
    private String sourceFileName;
    private String sourceFileUrl;
    private String sceneId;
    private String sceneName;
    private Integer errorHttpStatus;
    private Integer responseDelayMs;
    private String apiPath;
    private String responseMode;
    private String responseScript;
    private String createdAt;

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

    public JsonNode getRequestExample() {
        return requestExample;
    }

    public void setRequestExample(JsonNode requestExample) {
        this.requestExample = requestExample;
    }

    public JsonNode getResponseExample() {
        return responseExample;
    }

    public void setResponseExample(JsonNode responseExample) {
        this.responseExample = responseExample;
    }

    public JsonNode getErrorResponseExample() {
        return errorResponseExample;
    }

    public void setErrorResponseExample(JsonNode errorResponseExample) {
        this.errorResponseExample = errorResponseExample;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
