package com.example.mock.parser.repository;

import com.example.mock.parser.entity.UploadedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFileEntity, String> {
    
    List<UploadedFileEntity> findAllByOrderByUploadedAtDesc();
    
    List<UploadedFileEntity> findByStatusOrderByUploadedAtAsc(UploadedFileEntity.ProcessingStatus status);
}
