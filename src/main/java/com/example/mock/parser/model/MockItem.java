package com.example.mock.parser.model;

import com.fasterxml.jackson.databind.JsonNode;

public class MockItem {
    private String title;
    private JsonNode mock;
    private String raw;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JsonNode getMock() {
        return mock;
    }

    public void setMock(JsonNode mock) {
        this.mock = mock;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }
}
