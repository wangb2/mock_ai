package com.example.mock.parser.service;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class QiniuService {

    @Value("${qiniu.access-key:}")
    private String accessKey;

    @Value("${qiniu.secret-key:}")
    private String secretKey;

    @Value("${qiniu.bucket:}")
    private String bucket;

    @Value("${qiniu.domain:}")
    private String domain;

    @Value("${qiniu.enabled:false}")
    private boolean enabled;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QiniuService.class);

    /**
     * 上传文件到七牛云
     * @param file 文件
     * @param fileId 文件ID（用于生成唯一key）
     * @param originalName 原始文件名
     * @return 文件访问URL，如果上传失败返回null
     */
    public String uploadFile(MultipartFile file, String fileId, String originalName) {
        if (!enabled || accessKey == null || accessKey.isEmpty() || 
            secretKey == null || secretKey.isEmpty() || 
            bucket == null || bucket.isEmpty()) {
            logger.warn("Qiniu upload is disabled or not configured properly");
            return null;
        }

        try {
            // 构造一个带指定Region对象的配置类
            Configuration cfg = new Configuration(Region.autoRegion());
            UploadManager uploadManager = new UploadManager(cfg);

            // 生成上传凭证
            Auth auth = Auth.create(accessKey, secretKey);
            String upToken = auth.uploadToken(bucket);

            // 生成唯一key：fileId_originalName
            String key = fileId + "_" + originalName;

            // 上传文件
            byte[] fileBytes = file.getBytes();
            Response response = uploadManager.put(fileBytes, key, upToken);

            // 解析上传成功的结果
            DefaultPutRet putRet = response.jsonToObject(DefaultPutRet.class);
            logger.info("Qiniu upload success. key={}, hash={}", putRet.key, putRet.hash);

            // 返回文件访问URL
            if (domain != null && !domain.isEmpty()) {
                // 如果配置了域名，使用域名访问
                String url = domain.endsWith("/") ? domain + putRet.key : domain + "/" + putRet.key;
                // 确保使用 https
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                return url;
            } else {
                // 如果没有配置域名，需要构建完整的七牛云访问URL
                // 格式：https://{bucket}.qiniucdn.com/{key} 或使用默认域名
                // 这里需要根据实际bucket配置来构建，暂时返回带key的URL
                // 注意：这需要bucket配置了公开访问或使用正确的域名
                String qiniuUrl = "http://t9ig9nbye.hn-bkt.clouddn.com/" + putRet.key;
                logger.info("Qiniu URL generated without domain. bucket={}, key={}, url={}", bucket, putRet.key, qiniuUrl);
                return qiniuUrl;
            }
        } catch (QiniuException ex) {
            Response r = ex.response;
            logger.error("Qiniu upload failed. error: {}", r != null ? r.toString() : ex.getMessage());
            try {
                if (r != null) {
                    logger.error("Qiniu error body: {}", r.bodyString());
                }
            } catch (QiniuException ex2) {
                // ignore
            }
            return null;
        } catch (IOException ex) {
            logger.error("Qiniu upload IO error: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 上传字节数组到七牛云（用于飞书图片等）。
     *
     * @param data        文件二进制
     * @param key         存储 key，建议带路径如 feishu/xxx.jpg
     * @param contentType 可选，用于日志；七牛根据 key 后缀推断类型
     * @return 公网访问 URL，失败返回 null
     */
    public String uploadBytes(byte[] data, String key, String contentType) {
        if (!enabled || accessKey == null || accessKey.isEmpty()
                || secretKey == null || secretKey.isEmpty()
                || bucket == null || bucket.isEmpty()) {
            logger.warn("Qiniu upload is disabled or not configured properly");
            return null;
        }
        if (data == null || data.length == 0 || key == null || key.trim().isEmpty()) {
            return null;
        }
        try {
            Configuration cfg = new Configuration(Region.autoRegion());
            UploadManager uploadManager = new UploadManager(cfg);
            Auth auth = Auth.create(accessKey, secretKey);
            String upToken = auth.uploadToken(bucket);
            Response response = uploadManager.put(data, key.trim(), upToken);
            DefaultPutRet putRet = response.jsonToObject(DefaultPutRet.class);
            logger.info("Qiniu upload bytes success. key={}, size={}", putRet.key, data.length);
            if (domain != null && !domain.isEmpty()) {
                String url = domain.endsWith("/") ? domain + putRet.key : domain + "/" + putRet.key;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                return url;
            }
            String qiniuUrl = "http://t9ig9nbye.hn-bkt.clouddn.com/" + putRet.key;
            logger.info("Qiniu URL generated without domain. bucket={}, key={}", bucket, putRet.key);
            return qiniuUrl;
        } catch (QiniuException ex) {
            Response r = ex.response;
            logger.error("Qiniu upload bytes failed. key={}, error={}", key, r != null ? r.toString() : ex.getMessage());
            return null;
        }
    }

    /**
     * 删除七牛云文件
     * @param key 文件key
     * @return 是否删除成功
     */
    public boolean deleteFile(String key) {
        if (!enabled || accessKey == null || accessKey.isEmpty() || 
            secretKey == null || secretKey.isEmpty() || 
            bucket == null || bucket.isEmpty()) {
            return false;
        }

        try {
            Auth auth = Auth.create(accessKey, secretKey);
            Configuration cfg = new Configuration(Region.autoRegion());
            BucketManager bucketManager = new BucketManager(auth, cfg);
            bucketManager.delete(bucket, key);
            logger.info("Qiniu delete success. key={}", key);
            return true;
        } catch (QiniuException ex) {
            logger.error("Qiniu delete failed. key={}, error: {}", key, ex.getMessage());
            return false;
        }
    }
}
