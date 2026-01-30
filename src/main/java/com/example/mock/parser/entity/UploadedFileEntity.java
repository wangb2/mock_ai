package com.example.mock.parser.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件上传记录实体
 */
@Entity
@Table(name = "uploaded_file")
public class UploadedFileEntity {
    
    public enum ProcessingStatus {
        PENDING,      // 未处理
        PROCESSING,   // 处理中
        COMPLETED,    // 处理完成
        FAILED        // 处理失败
    }
    
    @Id
    @Column(length = 64)
    private String fileId;
    
    @Column(nullable = false, length = 512)
    private String fileName;
    
    @Column(length = 1024)
    private String fileUrl;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status = ProcessingStatus.PENDING;
    
    @Column(length = 64)
    private String sceneId;
    
    @Column(length = 256)
    private String sceneName;
    
    private Boolean fullAi;
    
    @Column(length = 1024)
    private String errorMessage;
    
    private Integer generatedCount;  // 生成的接口数量
    
    private LocalDateTime uploadedAt;
    
    private LocalDateTime processedAt;
    
    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public ProcessingStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessingStatus status) {
        this.status = status;
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
    
    public Boolean getFullAi() {
        return fullAi;
    }
    
    public void setFullAi(Boolean fullAi) {
        this.fullAi = fullAi;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getGeneratedCount() {
        return generatedCount;
    }
    
    public void setGeneratedCount(Integer generatedCount) {
        this.generatedCount = generatedCount;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
