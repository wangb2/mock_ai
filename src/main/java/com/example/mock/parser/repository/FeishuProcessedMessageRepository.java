package com.example.mock.parser.repository;

import com.example.mock.parser.entity.FeishuProcessedMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 飞书已处理事件记录，按 event_id 去重与查询，保存 event_type 及入站/出站消息。
 */
public interface FeishuProcessedMessageRepository extends JpaRepository<FeishuProcessedMessageEntity, Long> {

    /**
     * 是否存在该 event_id 的记录（用于去重）。
     */
    boolean existsByEventId(String eventId);

    /**
     * 按 event_id 查询一条记录。
     */
    Optional<FeishuProcessedMessageEntity> findByEventId(String eventId);

    /**
     * 删除早于指定时间的记录，可用于按 TTL 清理历史数据。
     */
    void deleteByCreatedAtBefore(LocalDateTime before);
}
