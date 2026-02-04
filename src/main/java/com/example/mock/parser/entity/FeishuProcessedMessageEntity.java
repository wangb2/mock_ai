package com.example.mock.parser.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 飞书已处理事件记录表：按 event_id 去重，兼容通用事件结构（schema 2.0 header.event_id / event_type），保存完整 payload。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "feishu_processed_message")
public class FeishuProcessedMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 飞书事件唯一 ID（schema 2.0 为 header.event_id，1.0 为 uuid），用于去重与查询。 */
    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    private String eventId;

    /** 事件类型（如 im.message.receive_v1、contact.user_group.created_v3）。 */
    @Column(name = "event_type", length = 64)
    private String eventType;

    /** 事件请求体 JSON（完整 payload）。 */
    @Lob
    @Column(name = "payload")
    private String payload;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
