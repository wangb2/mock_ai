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
}
