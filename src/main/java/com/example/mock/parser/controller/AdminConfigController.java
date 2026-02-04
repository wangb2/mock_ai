package com.example.mock.parser.controller;

import com.example.mock.parser.service.ConfigFileService;
import com.example.mock.parser.service.llm.PromptService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {
    private final ConfigFileService configFileService;
    private final PromptService promptService;

    public AdminConfigController(ConfigFileService configFileService, PromptService promptService) {
        this.configFileService = configFileService;
        this.promptService = promptService;
    }

    @GetMapping
    public Map<String, Object> getConfig() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> app = configFileService.readYamlAsMap("application.yml");
        Map<String, Object> appSubset = new LinkedHashMap<>();
        copyIfPresent(app, appSubset, "llm");
        copyIfPresent(app, appSubset, "zhipu");
        copyIfPresent(app, appSubset, "openai");
        copyIfPresent(app, appSubset, "doubao");
        copyIfPresent(app, appSubset, "mock");
        copyIfPresent(app, appSubset, "qiniu");
        copyIfPresent(app, appSubset, "filter");
        copyIfPresent(app, appSubset, "spring");
        copyIfPresent(app, appSubset, "logging");
        result.put("application", appSubset);

        Map<String, Object> prompts = new LinkedHashMap<>();
        prompts.put("openai-prompts.yml", configFileService.readRaw("openai-prompts.yml"));
        prompts.put("zhipu-prompts.yml", configFileService.readRaw("zhipu-prompts.yml"));
        result.put("prompts", prompts);
        return result;
    }

    @PutMapping
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> payload) {
        Map<String, Object> app = configFileService.readYamlAsMap("application.yml");
        Object appObj = payload.get("application");
        if (appObj instanceof Map) {
            Map<String, Object> incoming = (Map<String, Object>) appObj;
            mergeSection(app, incoming, "llm");
            mergeSection(app, incoming, "zhipu");
            mergeSection(app, incoming, "openai");
            mergeSection(app, incoming, "doubao");
            mergeSection(app, incoming, "mock");
            mergeSection(app, incoming, "qiniu");
            mergeSection(app, incoming, "filter");
            mergeSection(app, incoming, "spring");
            mergeSection(app, incoming, "logging");
            configFileService.writeYaml("application.yml", app);
        }

        Object promptsObj = payload.get("prompts");
        if (promptsObj instanceof Map) {
            Map<String, Object> prompts = (Map<String, Object>) promptsObj;
            writePromptIfPresent(prompts, "openai-prompts.yml");
            writePromptIfPresent(prompts, "zhipu-prompts.yml");
        }
        promptService.reload();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        return resp;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private void mergeSection(Map<String, Object> root, Map<String, Object> incoming, String key) {
        if (incoming.containsKey(key)) {
            root.put(key, incoming.get(key));
        }
    }

    private void writePromptIfPresent(Map<String, Object> prompts, String filename) {
        Object content = prompts.get(filename);
        if (content != null) {
            configFileService.writeRaw(filename, content.toString());
        }
    }
}
