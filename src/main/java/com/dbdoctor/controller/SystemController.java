package com.dbdoctor.controller;

import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 系统信息控制器
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SlowQueryTemplateRepository templateRepository;

    @Value("${db-doctor.version:2.2.0}")
    private String version;

    @Value("${db-doctor.build-time:未知}")
    private String buildTime;

    @Value("${git.commit.id.abbrev:unknown}")
    private String gitCommit;

    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> getSystemInfo() {
        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "version", "v" + version,
                        "buildTime", buildTime,
                        "gitCommit", gitCommit
                )
        );
    }

    /**
     * 获取今日概览统计
     *
     * @return 今日统计数据
     */
    @GetMapping("/overview")
    public Map<String, Object> getTodayOverview() {
        log.info("查询今日概览统计");

        // 今日分析总数（所有模板）
        long totalTemplates = templateRepository.count();

        // 高危 SQL 数（严重级别）
        long highRiskCount = templateRepository.countBySeverityLevel(
                com.dbdoctor.common.enums.SeverityLevel.CRITICAL
        );

        // 待分析任务数（状态为 PENDING）
        long pendingTasks = templateRepository.countByStatus(
                com.dbdoctor.entity.SlowQueryTemplate.AnalysisStatus.PENDING
        );

        // 平均耗时
        double avgQueryTime = 0.0;
        // TODO: 计算平均耗时

        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "todayTotal", totalTemplates,
                        "highRiskCount", highRiskCount,
                        "avgQueryTime", avgQueryTime,
                        "pendingTasks", pendingTasks,
                        "date", LocalDate.now().toString()
                )
        );
    }

    /**
     * 获取队列状态
     *
     * @return 队列状态信息
     */
    @GetMapping("/queue-status")
    public Map<String, Object> getQueueStatus() {
        log.info("查询队列状态");

        // 待分析任务数
        long pendingTasks = templateRepository.countByStatus(
                com.dbdoctor.entity.SlowQueryTemplate.AnalysisStatus.PENDING
        );

        // 正在分析任务数（这里用 SUCCESS 表示正在处理或已完成的）
        long processingTasks = templateRepository.countByStatus(
                com.dbdoctor.entity.SlowQueryTemplate.AnalysisStatus.SUCCESS
        );

        // AI 服务状态（简单实现：假设在线）
        boolean aiOnline = true; // TODO: 实际检测 AI 服务状态

        return Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "pendingTasks", pendingTasks,
                        "processingTasks", processingTasks,
                        "aiServiceStatus", aiOnline ? "online" : "offline"
                )
        );
    }
}
