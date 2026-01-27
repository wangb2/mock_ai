package com.example.mock.parser.repository;

import com.example.mock.parser.entity.MockSceneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockSceneRepository extends JpaRepository<MockSceneEntity, String> {
    List<MockSceneEntity> findAllByOrderByCreatedAtDesc();
}
