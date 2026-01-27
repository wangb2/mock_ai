package com.example.mock.parser.service;

import com.example.mock.parser.entity.MockEndpointEntity;
import com.example.mock.parser.entity.MockSceneEntity;
import com.example.mock.parser.model.MockSceneItem;
import com.example.mock.parser.repository.MockEndpointRepository;
import com.example.mock.parser.repository.MockSceneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MockSceneService {
    private final MockSceneRepository sceneRepository;
    private final MockEndpointRepository endpointRepository;

    public MockSceneService(MockSceneRepository sceneRepository, MockEndpointRepository endpointRepository) {
        this.sceneRepository = sceneRepository;
        this.endpointRepository = endpointRepository;
    }

    @PostConstruct
    public void ensureDefaultScene() {
        if (sceneRepository.count() == 0) {
            MockSceneEntity entity = new MockSceneEntity();
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setName("默认场景");
            entity.setDescription("系统默认场景");
            sceneRepository.save(entity);
        }
    }

    public List<MockSceneItem> listScenes() {
        List<MockSceneItem> items = new ArrayList<>();
        for (MockSceneEntity entity : sceneRepository.findAllByOrderByCreatedAtDesc()) {
            items.add(toItem(entity));
        }
        return items;
    }

    public MockSceneItem getScene(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return sceneRepository.findById(id).map(this::toItem).orElse(null);
    }

    @Transactional
    public MockSceneItem createScene(String name, String description) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        MockSceneEntity entity = new MockSceneEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(trimmed);
        entity.setDescription(description == null ? null : description.trim());
        sceneRepository.save(entity);
        return toItem(entity);
    }

    @Transactional
    public MockSceneItem createScene(String name, String description, String keywords) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        MockSceneEntity entity = new MockSceneEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(trimmed);
        entity.setDescription(description == null ? null : description.trim());
        entity.setKeywords(keywords == null ? null : keywords.trim());
        sceneRepository.save(entity);
        return toItem(entity);
    }

    @Transactional
    public MockSceneItem updateScene(String id, String name, String description) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        MockSceneEntity entity = sceneRepository.findById(id).orElse(null);
        if (entity == null) {
            return null;
        }
        if (name != null && !name.trim().isEmpty()) {
            entity.setName(name.trim());
        }
        if (description != null) {
            entity.setDescription(description.trim());
        }
        sceneRepository.save(entity);
        // Sync endpoint scene name for display.
        List<MockEndpointEntity> endpoints = endpointRepository.findBySceneId(entity.getId());
        for (MockEndpointEntity endpoint : endpoints) {
            endpoint.setSceneName(entity.getName());
        }
        endpointRepository.saveAll(endpoints);
        return toItem(entity);
    }

    @Transactional
    public MockSceneItem updateScene(String id, String name, String description, String keywords) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        MockSceneEntity entity = sceneRepository.findById(id).orElse(null);
        if (entity == null) {
            return null;
        }
        if (name != null && !name.trim().isEmpty()) {
            entity.setName(name.trim());
        }
        if (description != null) {
            entity.setDescription(description.trim());
        }
        if (keywords != null) {
            entity.setKeywords(keywords.trim());
        }
        sceneRepository.save(entity);
        // Sync endpoint scene name for display.
        List<MockEndpointEntity> endpoints = endpointRepository.findBySceneId(entity.getId());
        for (MockEndpointEntity endpoint : endpoints) {
            endpoint.setSceneName(entity.getName());
        }
        endpointRepository.saveAll(endpoints);
        return toItem(entity);
    }

    @Transactional
    public boolean deleteScene(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        if (!sceneRepository.existsById(id)) {
            return false;
        }
        List<MockEndpointEntity> endpoints = endpointRepository.findBySceneId(id);
        for (MockEndpointEntity endpoint : endpoints) {
            endpoint.setSceneId(null);
            endpoint.setSceneName(null);
        }
        endpointRepository.saveAll(endpoints);
        sceneRepository.deleteById(id);
        return true;
    }

    private MockSceneItem toItem(MockSceneEntity entity) {
        MockSceneItem item = new MockSceneItem();
        item.setId(entity.getId());
        item.setName(entity.getName());
        item.setDescription(entity.getDescription());
        item.setKeywords(entity.getKeywords());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }
}
