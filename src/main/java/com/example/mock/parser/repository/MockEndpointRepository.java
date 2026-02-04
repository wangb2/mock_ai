package com.example.mock.parser.repository;

import com.example.mock.parser.entity.MockEndpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MockEndpointRepository extends JpaRepository<MockEndpointEntity, String> {
    List<MockEndpointEntity> findAllByOrderByCreatedAtDesc();
    List<MockEndpointEntity> findBySourceFileId(String sourceFileId);
    List<MockEndpointEntity> findBySceneId(String sceneId);
    List<MockEndpointEntity> findBySourceFileName(String sourceFileName);
    MockEndpointEntity findFirstByApiPathAndMethod(String apiPath, String method);
    MockEndpointEntity findFirstByApiPathIgnoreCaseAndMethod(String apiPath, String method);
    MockEndpointEntity findFirstByApiPathIgnoreCase(String apiPath);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query(value = "SELECT scene_id, scene_name, COUNT(1) AS cnt " +
            "FROM mock_endpoint " +
            "WHERE scene_id IS NOT NULL AND scene_id <> '' " +
            "GROUP BY scene_id, scene_name " +
            "ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> countBySceneIdAll();
}
