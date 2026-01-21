package com.example.mock.parser.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedDocument {
    private String fileName;
    private String fileType;
    private List<Section> sections = new ArrayList<>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }
}
