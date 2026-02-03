package com.example.mock.parser.model;

import java.util.List;

public class ManualPreviewResult {
    private boolean needMoreInfo;
    private List<String> missingFields;
    private String message;
    private MockEndpointItem item;

    public boolean isNeedMoreInfo() {
        return needMoreInfo;
    }

    public void setNeedMoreInfo(boolean needMoreInfo) {
        this.needMoreInfo = needMoreInfo;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MockEndpointItem getItem() {
        return item;
    }

    public void setItem(MockEndpointItem item) {
        this.item = item;
    }
}
