package com.example.mock.parser.model;

import java.util.ArrayList;
import java.util.List;

public class MockResult {
    private String outputFile;
    private List<MockItem> items = new ArrayList<>();

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public List<MockItem> getItems() {
        return items;
    }

    public void setItems(List<MockItem> items) {
        this.items = items;
    }
}
