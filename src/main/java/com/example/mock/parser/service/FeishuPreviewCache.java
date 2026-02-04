package com.example.mock.parser.service;

import com.example.mock.parser.config.FeishuProperties;
import com.example.mock.parser.model.feishu.FeishuCachedPreview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书预览结果缓存：preview_result_id -> FeishuCachedPreview，带 TTL。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuPreviewCache {

    private final FeishuProperties feishuProperties;
    private final ConcurrentHashMap<String, FeishuCachedPreview> cache = new ConcurrentHashMap<>();

    public void put(String previewResultId, FeishuCachedPreview value) {
        if (previewResultId == null || value == null) {
            return;
        }
        cache.put(previewResultId, value);
        log.debug("Feishu preview cached. previewResultId={}, chatId={}", previewResultId, value.getChatId());
    }

    public FeishuCachedPreview get(String previewResultId) {
        if (previewResultId == null) {
            return null;
        }
        FeishuCachedPreview entry = cache.get(previewResultId);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(feishuProperties.getPreviewCacheTtlMinutes())) {
            cache.remove(previewResultId);
            log.debug("Feishu preview expired. previewResultId={}", previewResultId);
            return null;
        }
        return entry;
    }

    public FeishuCachedPreview remove(String previewResultId) {
        return previewResultId == null ? null : cache.remove(previewResultId);
    }
}
