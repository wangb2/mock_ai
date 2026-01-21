package com.example.mock.parser.repository;

import com.example.mock.parser.entity.MockOperationLogEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MockOperationLogRepository extends JpaRepository<MockOperationLogEntity, String> {
    List<MockOperationLogEntity> findTop50ByOrderByCreatedAtDesc();
    long countByType(String type);
    long countByTypeAndCreatedAtAfter(String type, LocalDateTime since);
    void deleteByMockId(String mockId);

    @Query(value = "SELECT mock_id, COUNT(1) AS cnt " +
            "FROM mock_operation_log " +
            "WHERE type IN ('MOCK_HIT','MOCK_GEN','MOCK_ERROR','MOCK_VALIDATION_FAIL') " +
            "AND DATE(created_at) = CURDATE() " +
            "GROUP BY mock_id " +
            "ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByMockIdToday();

    @Query(value = "SELECT mock_id, COUNT(1) AS cnt " +
            "FROM mock_operation_log " +
            "WHERE type IN ('MOCK_HIT','MOCK_GEN','MOCK_ERROR','MOCK_VALIDATION_FAIL') " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY mock_id " +
            "ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countByMockIdLast7Days();
}
