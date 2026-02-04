package com.example.mock.parser.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ConfigFileService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFileService.class);
    private final ResourceLoader resourceLoader;
    private final Yaml yaml = new Yaml();

    public ConfigFileService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Map<String, Object> readYamlAsMap(String filename) {
        Path path = resolveWritablePath(filename);
        if (path != null && Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                Object data = yaml.load(is);
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            } catch (IOException e) {
                logger.error("Failed to read yaml: {}", path, e);
            }
        }
        Resource resource = resourceLoader.getResource("classpath:" + filename);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                Object data = yaml.load(is);
                if (data instanceof Map) {
                    return (Map<String, Object>) data;
                }
            } catch (IOException e) {
                logger.error("Failed to read classpath yaml: {}", filename, e);
            }
        }
        return new LinkedHashMap<>();
    }

    public void writeYaml(String filename, Map<String, Object> data) {
        Path path = resolveWritablePath(filename);
        if (path == null) {
            throw new IllegalStateException("No writable path for " + filename);
        }
        try {
            String output = yaml.dump(data);
            Files.write(path, output.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write yaml: " + filename, e);
        }
    }

    public String readRaw(String filename) {
        Path path = resolveWritablePath(filename);
        if (path != null && Files.exists(path)) {
            try {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Failed to read file: {}", path, e);
            }
        }
        Resource resource = resourceLoader.getResource("classpath:" + filename);
        if (resource.exists()) {
            try (InputStream is = resource.getInputStream()) {
                return new String(readAllBytes(is), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Failed to read classpath file: {}", filename, e);
            }
        }
        return "";
    }

    public void writeRaw(String filename, String content) {
        Path path = resolveWritablePath(filename);
        if (path == null) {
            throw new IllegalStateException("No writable path for " + filename);
        }
        try {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write file: " + filename, e);
        }
    }

    private Path resolveWritablePath(String filename) {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path config = cwd.resolve("config").resolve(filename);
        if (Files.exists(config) || Files.isDirectory(config.getParent())) {
            return config;
        }
        Path direct = cwd.resolve(filename);
        if (Files.exists(direct) || Files.isRegularFile(direct)) {
            return direct;
        }
        Path src = cwd.resolve("src").resolve("main").resolve("resources").resolve(filename);
        if (Files.exists(src) || Files.isRegularFile(src)) {
            return src;
        }
        return null;
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }
}
