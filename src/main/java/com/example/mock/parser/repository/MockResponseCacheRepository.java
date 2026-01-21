package com.example.mock.parser.repository;

import com.example.mock.parser.entity.MockResponseCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MockResponseCacheRepository extends JpaRepository<MockResponseCacheEntity, String> {
    Optional<MockResponseCacheEntity> findFirstByMockIdAndRequestSignature(String mockId, String requestSignature);
    void deleteByMockId(String mockId);
}
