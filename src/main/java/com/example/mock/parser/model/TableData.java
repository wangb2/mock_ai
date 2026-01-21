package com.example.mock.parser.model;

import java.util.ArrayList;
import java.util.List;

public class TableData {
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
    }
}
