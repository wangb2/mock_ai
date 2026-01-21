package com.example.mock.parser.model;

import java.util.ArrayList;
import java.util.List;

public class MockEndpointResult {
    private List<MockEndpointItem> items = new ArrayList<>();

    public List<MockEndpointItem> getItems() {
        return items;
    }

    public void setItems(List<MockEndpointItem> items) {
        this.items = items;
    }
}
