package com.example.mock.parser.repository;

import com.example.mock.parser.entity.MockEndpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockEndpointRepository extends JpaRepository<MockEndpointEntity, String> {
    List<MockEndpointEntity> findAllByOrderByCreatedAtDesc();
    List<MockEndpointEntity> findBySourceFileId(String sourceFileId);
}
