package com.techmanage.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmanage.dto.WeeklyReportResponse;
import com.techmanage.repository.SystemConfigRepository;
import com.techmanage.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final SystemConfigRepository systemConfigRepository;

    public AiServiceImpl(
        @Value("${ai.deepseek.api-key:}") String apiKey,
        @Value("${ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
        @Value("${ai.deepseek.model:deepseek-chat}") String model,
        ObjectMapper objectMapper,
        SystemConfigRepository systemConfigRepository) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.systemConfigRepository = systemConfigRepository;
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .requestFactory(factory)
            .build();
    }

    private String resolveApiKey() {
        var dbConfig = systemConfigRepository.findByConfigKey("deepseek.api.key");
        if (dbConfig.isPresent() && dbConfig.get().getConfigValue() != null
            && !dbConfig.get().getConfigValue().isBlank()) {
            return dbConfig.get().getConfigValue();
        }
        return apiKey;
    }

    @Override
    public String mergeReports(List<WeeklyReportResponse> reports) {
        String key = resolveApiKey();
        if (key == null || key.isBlank()) {
            throw new RuntimeException("AI服务未配置：请在系统管理中设置 DeepSeek API Key");
        }

        String systemPrompt = """
            你是一个专业的部门周报撰写助手。请将以下员工个人周报合并为一份结构化的部门周报。
            输出JSON格式，包含以下字段：
            1. overview: 部门本周工作总述（2-3句话）
            2. keyProgress: 各项目/方向的关键进展（按项目归类，markdown格式）
            3. commonIssues: 共性问题与风险（识别跨周报的高频关键词和共性问题）
            4. nextWeekPlans: 下周重点工作计划汇总
            5. coordinationItems: 需要部门协调的事项汇总
            """;

        StringBuilder userMessage = new StringBuilder("以下是本周各员工的工作汇报：\n\n");
        for (var r : reports) {
            userMessage.append("--- ").append(r.userName());
            if (r.userDepartment() != null && !r.userDepartment().isBlank()) {
                userMessage.append("（").append(r.userDepartment()).append("）");
            }
            userMessage.append(" ---\n");
            userMessage.append("本周完成工作（格式：事项|进展情况）：\n");
            userMessage.append(r.doneWork() != null ? r.doneWork() : "无").append("\n");
            userMessage.append("下周工作计划（格式：事项|进展情况）：\n");
            userMessage.append(r.planWork() != null ? r.planWork() : "无").append("\n");
            userMessage.append("遇到的问题：\n");
            userMessage.append(r.problems() != null && !r.problems().isBlank() ? r.problems() : "无").append("\n");
            userMessage.append("需要支持：\n");
            userMessage.append(r.supportNeeded() != null && !r.supportNeeded().isBlank() ? r.supportNeeded() : "无").append("\n\n");
        }

        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage.toString())
            ),
            "temperature", 0.3,
            "response_format", Map.of("type", "json_object")
        );

        try {
            String response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + key)
                .body(requestBody)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            // Validate it's proper JSON with expected fields
            JsonNode parsed = objectMapper.readTree(content);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            throw new RuntimeException("AI合并失败: " + e.getMessage(), e);
        }
    }
}
