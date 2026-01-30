package com.dbdoctor.controller;

import com.dbdoctor.model.ConfigDTO;
import com.dbdoctor.model.HotReloadResult;
import com.dbdoctor.service.ConfigService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

/**
 * 系统配置控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /**
     * 获取所有配置（敏感信息脱敏）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllConfigs() {
        Map<String, String> configs = configService.getAllConfigs();

        return Map.of(
                "code", 200,
                "message", "success",
                "data", configs
        );
    }

    /**
     * 根据分类获取配置
     */
    @GetMapping("/category/{category}")
    public Map<String, Object> getConfigsByCategory(@PathVariable String category) {
        Map<String, String> configs = configService.getConfigsByCategory(category);

        return Map.of(
                "code", 200,
                "message", "success",
                "data", configs
        );
    }

    /**
     * 保存配置并触发热重载
     */
    @PostMapping("/save")
    public Map<String, Object> saveAndRefresh(@RequestBody ConfigDTO configDTO) {
        log.info("收到配置保存请求: category={}", configDTO.getCategory());

        HotReloadResult result = configService.saveAndRefresh(
                configDTO.getCategory(),
                configDTO.getConfigs()
        );

        String message = result.isRequiresRestart()
                ? "配置保存成功，但需要重启服务才能生效: " + result.getRestartMessage()
                : "配置保存成功，相关组件已重新加载";

        return Map.of(
                "code", 200,
                "message", message,
                "data", Map.of(
                        "requiresRestart", result.isRequiresRestart(),
                        "refreshedBeans", result.getRefreshedBeans()
                )
        );
    }

    /**
     * 获取单个配置值
     */
    @GetMapping("/{key}")
    public Map<String, Object> getConfig(@PathVariable String key) {
        String value = configService.getConfig(key);

        return Map.of(
                "code", 200,
                "message", "success",
                "data", value != null ? value : ""
        );
    }

    /**
     * 测试 AI 模型连通性
     */
    @PostMapping("/test/ai")
    public Map<String, Object> testAiConnection(@RequestBody Map<String, Object> params) {
        String agentType = (String) params.get("agentType");
        String provider = (String) params.get("provider");
        String baseUrl = (String) params.get("baseUrl");
        String modelName = (String) params.get("modelName");
        String apiKey = (String) params.get("apiKey");

        log.info("测试 AI 连通性: agent={}, provider={}, model={}", agentType, provider, modelName);

        try {
            long startTime = System.currentTimeMillis();

            // 创建临时模型
            ChatLanguageModel model = createModelForTest(provider, baseUrl, modelName, apiKey);

            // 发送测试请求
            String response = model.generate("Hello");

            long duration = System.currentTimeMillis() - startTime;

            return Map.of(
                    "code", 200,
                    "message", "模型连接成功",
                    "data", Map.of(
                            "success", true,
                            "responseTime", duration + "ms",
                            "testResult", response
                    )
            );

        } catch (Exception e) {
            log.error("AI 连通性测试失败", e);

            return Map.of(
                    "code", 500,
                    "message", "模型连接失败: " + e.getMessage(),
                    "data", Map.of("success", false)
            );
        }
    }

    /**
     * 创建测试模型
     */
    private ChatLanguageModel createModelForTest(String provider, String baseUrl,
                                                 String modelName, String apiKey) {
        return switch (provider.toLowerCase()) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .temperature(0.0)
                    .build();

            case "openai", "deepseek" -> OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(0.0)
                    .build();

            default -> throw new IllegalArgumentException("不支持的 AI 提供商: " + provider);
        };
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test/db")
    public Map<String, Object> testDbConnection(@RequestBody Map<String, Object> params) {
        String host = (String) params.get("host");
        Integer port = (Integer) params.get("port");
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String database = (String) params.get("database");

        log.info("测试数据库连接: host={}, port={}, user={}", host, port, username);

        try {
            long startTime = System.currentTimeMillis();

            // 1. 构建连接 URL
            String url = String.format(
                    "jdbc:mysql://%s:%d%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
                    host,
                    port,
                    database != null && !database.isEmpty() ? "/" + database : ""
            );

            // 2. 尝试连接
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                long latency = System.currentTimeMillis() - startTime;

                // 3. 查询数据库版本
                String version = "";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
                    if (rs.next()) {
                        version = rs.getString(1);
                    }
                }

                return Map.of(
                        "code", 200,
                        "message", "连接成功",
                        "data", Map.of(
                                "success", true,
                                "version", version,
                                "latency", latency + "ms"
                        )
                );
            }

        } catch (Exception e) {
            log.error("数据库连接失败", e);

            return Map.of(
                    "code", 500,
                    "message", "连接失败: " + e.getMessage(),
                    "data", Map.of("success", false)
            );
        }
    }
}
