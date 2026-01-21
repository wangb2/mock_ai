package com.example.mock.parser.model;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private String title;
    private int level;
    private String content;
    private List<TableData> tables = new ArrayList<>();

    public Section() {
    }

    public Section(String title, int level) {
        this.title = title;
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<TableData> getTables() {
        return tables;
    }

    public void setTables(List<TableData> tables) {
        this.tables = tables;
    }
}
