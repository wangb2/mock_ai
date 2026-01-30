package com.example.mock.parser.service;

import com.example.mock.parser.entity.UploadedFileEntity;
import com.example.mock.parser.model.MockEndpointResult;
import com.example.mock.parser.model.ParsedDocument;
import com.example.mock.parser.repository.UploadedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步Mock处理服务
 * 管理文件处理队列和并发控制
 */
@Service
public class AsyncMockProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMockProcessingService.class);
    
    private final DocumentParserService documentParserService;
    private final MockEndpointService mockEndpointService;
    private final UploadedFileRepository uploadedFileRepository;
    
    // 处理队列
    private final BlockingQueue<ProcessingTask> processingQueue = new LinkedBlockingQueue<>();
    
    // 并发控制信号量
    private Semaphore processingSemaphore;
    
    // 当前正在处理的任务数
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    
    @Value("${mock.processing.concurrent-limit:2}")
    private int concurrentLimit;
    
    public AsyncMockProcessingService(
            DocumentParserService documentParserService,
            MockEndpointService mockEndpointService,
            UploadedFileRepository uploadedFileRepository) {
        this.documentParserService = documentParserService;
        this.mockEndpointService = mockEndpointService;
        this.uploadedFileRepository = uploadedFileRepository;
    }
    
    /**
     * 在依赖注入完成后初始化
     */
    @PostConstruct
    public void init() {
        // 初始化并发控制（此时 @Value 已经注入）
        this.processingSemaphore = new Semaphore(concurrentLimit);
        logger.info("AsyncMockProcessingService initialized. concurrentLimit={}, availablePermits={}", 
                concurrentLimit, processingSemaphore.availablePermits());
        // 启动队列处理线程
        startQueueProcessor();
    }
    
    /**
     * 启动队列处理线程
     */
    private void startQueueProcessor() {
        Thread processorThread = new Thread(() -> {
            while (true) {
                try {
                    ProcessingTask task = processingQueue.take();
                    processTaskAsync(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Queue processor interrupted", e);
                    break;
                } catch (Exception e) {
                    logger.error("Error in queue processor", e);
                }
            }
        }, "MockProcessingQueue");
        processorThread.setDaemon(true);
        processorThread.start();
        logger.info("Queue processor thread started");
    }
    
    /**
     * 添加处理任务到队列
     */
    public void addTask(ProcessingTask task) {
        try {
            processingQueue.put(task);
            logger.info("Task added to queue. fileId={}, fileName={}, queueSize={}", 
                    task.getFileId(), task.getFileName(), processingQueue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to add task to queue", e);
        }
    }
    
    /**
     * 处理任务（在队列线程中直接执行，不需要@Async）
     */
    private void processTaskAsync(ProcessingTask task) {
        String fileId = task.getFileId();
        logger.info("开始处理任务. fileId={}, fileName={}, activeTasks={}", 
                fileId, task.getFileName(), activeTasks.get());
        
        try {
            // 获取信号量（控制并发）
            logger.info("等待获取处理信号量. fileId={}, availablePermits={}", fileId, processingSemaphore.availablePermits());
            processingSemaphore.acquire();
            activeTasks.incrementAndGet();
            logger.info("已获取处理信号量，开始处理. fileId={}, activeTasks={}", fileId, activeTasks.get());
            
            // 更新状态为处理中
            updateStatus(fileId, UploadedFileEntity.ProcessingStatus.PROCESSING, null);
            logger.info("状态已更新为处理中. fileId={}", fileId);
            
            // 处理文件
            logger.info("开始调用processFile. fileId={}", fileId);
            processFile(task);
            logger.info("processFile调用完成. fileId={}", fileId);
            
            // 更新状态为完成
            updateStatus(fileId, UploadedFileEntity.ProcessingStatus.COMPLETED, null);
            logger.info("任务处理完成. fileId={}, fileName={}", fileId, task.getFileName());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("处理任务被中断. fileId={}", fileId, e);
            updateStatus(fileId, UploadedFileEntity.ProcessingStatus.FAILED, "处理被中断");
        } catch (Exception e) {
            logger.error("处理任务失败. fileId={}, error={}", fileId, e.getMessage(), e);
            updateStatus(fileId, UploadedFileEntity.ProcessingStatus.FAILED, e.getMessage());
        } finally {
            activeTasks.decrementAndGet();
            processingSemaphore.release();
            logger.info("已释放处理信号量. fileId={}, activeTasks={}, availablePermits={}", 
                    fileId, activeTasks.get(), processingSemaphore.availablePermits());
        }
    }
    
    /**
     * 处理文件
     * 直接使用文件URL，不下载文件到本地，大模型可以直接使用URL
     */
    private void processFile(ProcessingTask task) throws IOException {
        String fileId = task.getFileId();
        String fileName = task.getFileName();
        String fileUrl = task.getFileUrl();
        boolean fullAi = task.isFullAi();
        String sceneId = task.getSceneId();
        String sceneName = task.getSceneName();
        String sceneKeywords = task.getSceneKeywords();
        
        logger.info("处理文件. fileId={}, fileName={}, fileUrl={}, fullAi={}", 
                fileId, fileName, fileUrl, fullAi);
        
        // 检查fileUrl是否是HTTP/HTTPS URL（七牛云）
        boolean isHttpUrl = fileUrl != null && (fileUrl.startsWith("http://") || fileUrl.startsWith("https://"));
        
        if (isHttpUrl) {
            // 对于HTTP/HTTPS URL，直接使用URL，大模型可以直接访问，不需要下载文件
            logger.info("使用文件URL直接处理，不下载文件. fileUrl={}", fileUrl);
            
            // 创建一个空的ParsedDocument（实际上不会被使用，因为会走generateEndpointsWithFileUrl路径）
            ParsedDocument parsedDocument = new ParsedDocument();
            parsedDocument.setFileName(fileName);
            
            // 生成Mock接口（generateEndpoints会检测到HTTP URL，直接使用generateEndpointsWithFileUrl）
            MockEndpointResult result = mockEndpointService.generateEndpoints(
                    parsedDocument, fileId, fileName, fileUrl, fullAi, 
                    sceneId, sceneName, sceneKeywords);
            
            // 更新生成数量
            UploadedFileEntity entity = uploadedFileRepository.findById(fileId).orElse(null);
            if (entity != null) {
                entity.setGeneratedCount(result.getItems() != null ? result.getItems().size() : 0);
                entity.setProcessedAt(LocalDateTime.now());
                uploadedFileRepository.save(entity);
            }
            
            logger.info("文件处理完成. fileId={}, fileName={}, generatedCount={}", 
                    fileId, fileName, result.getItems() != null ? result.getItems().size() : 0);
        } else {
            // 对于本地路径（/parse/endpoint/file/{fileId}），也直接使用URL
            // 因为大模型可能支持本地路径，或者我们需要确保所有文件都上传到七牛云
            logger.warn("文件URL是本地路径，尝试直接使用URL. fileUrl={}", fileUrl);
            
            // 创建一个空的ParsedDocument
            ParsedDocument parsedDocument = new ParsedDocument();
            parsedDocument.setFileName(fileName);
            
            // 尝试直接使用URL（如果大模型不支持，会走传统路径，但这种情况应该很少）
            MockEndpointResult result = mockEndpointService.generateEndpoints(
                    parsedDocument, fileId, fileName, fileUrl, fullAi, 
                    sceneId, sceneName, sceneKeywords);
            
            UploadedFileEntity entity = uploadedFileRepository.findById(fileId).orElse(null);
            if (entity != null) {
                entity.setGeneratedCount(result.getItems() != null ? result.getItems().size() : 0);
                entity.setProcessedAt(LocalDateTime.now());
                uploadedFileRepository.save(entity);
            }
        }
    }
    
    /**
     * 更新文件状态
     */
    private void updateStatus(String fileId, UploadedFileEntity.ProcessingStatus status, String errorMessage) {
        UploadedFileEntity entity = uploadedFileRepository.findById(fileId).orElse(null);
        if (entity != null) {
            entity.setStatus(status);
            if (errorMessage != null) {
                entity.setErrorMessage(errorMessage);
            }
            if (status == UploadedFileEntity.ProcessingStatus.COMPLETED || 
                status == UploadedFileEntity.ProcessingStatus.FAILED) {
                entity.setProcessedAt(LocalDateTime.now());
            }
            uploadedFileRepository.save(entity);
        }
    }
    
    /**
     * 处理任务数据类
     */
    public static class ProcessingTask {
        private String fileId;
        private String fileName;
        private String fileUrl;  // 文件URL，大模型直接使用URL，不需要下载
        private boolean fullAi;
        private String sceneId;
        private String sceneName;
        private String sceneKeywords;
        
        public ProcessingTask(String fileId, String fileName, String fileUrl, 
                             boolean fullAi, String sceneId, String sceneName, 
                             String sceneKeywords) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.fullAi = fullAi;
            this.sceneId = sceneId;
            this.sceneName = sceneName;
            this.sceneKeywords = sceneKeywords;
        }
        
        // Getters
        public String getFileId() { return fileId; }
        public String getFileName() { return fileName; }
        public String getFileUrl() { return fileUrl; }
        public boolean isFullAi() { return fullAi; }
        public String getSceneId() { return sceneId; }
        public String getSceneName() { return sceneName; }
        public String getSceneKeywords() { return sceneKeywords; }
    }
}
