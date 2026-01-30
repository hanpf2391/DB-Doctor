package com.dbdoctor.service;

import com.dbdoctor.agent.CodingAgent;
import com.dbdoctor.agent.DBAgent;
import com.dbdoctor.agent.ReasoningAgent;
import com.dbdoctor.agent.SqlDiagnosticsTools;
import com.dbdoctor.common.util.PromptUtil;
import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.model.AnalysisContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 多 Agent 协调器
 *
 * 核心职责：
 * - 协调 3 个 Agent 的协作流程
 * - 实现单 Agent 模式和多 Agent 模式的切换
 * - 整合各 Agent 的输出，生成最终报告
 *
 * 协作流程（ReAct 模式）：
 * 1. DiagnosisAgent（主治医生）：初步诊断，收集证据
 * 2. 判断是否需要升级：
 *    - 如果是简单问题 → 直接生成报告
 *    - 如果是复杂问题 → 调用 ReasoningAgent
 * 3. ReasoningAgent（推理专家）：深度推理，找到根因
 * 4. CodingAgent（编码专家）：生成优化代码
 * 5. 整合所有输出，生成最终报告
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiAgentCoordinator {

    private final DBAgent diagnosisAgent;          // 主治医生
    private final ReasoningAgent reasoningAgent;    // 推理专家
    private final CodingAgent codingAgent;          // 编码专家
    private final SqlDiagnosticsTools tools;        // 诊断工具箱
    private final ObjectMapper objectMapper;
    private final PromptUtil promptUtil;            // 提示词工具

    /**
     * 分析慢查询（多 Agent 协作）
     *
     * @param context 分析上下文（数据快照）
     * @return 完整的诊断报告
     */
    public String analyze(AnalysisContext context) {
        log.info("开始多 Agent 协作分析: fingerprint={}", context.getSqlFingerprint());
        long startTime = System.currentTimeMillis();

        try {
            // === 第一步：主治医生初步诊断 ===
            log.info("🔍 步骤 1：主治医生初步诊断");
            String diagnosisReport = performDiagnosis(context);
            log.debug("主治医生诊断完成，报告长度: {} 字符", diagnosisReport.length());

            // === 第二步：判断是否需要升级到推理专家 ===
            boolean needsExpert = shouldUpgradeToExpert(context);
            log.info("🤔 是否需要推理专家: {}", needsExpert ? "是" : "否");

            String reasoningReport = null;
            String optimizationCode = null;

            if (needsExpert) {
                // === 第三步：推理专家深度推理 ===
                log.info("🧠 步骤 2：推理专家深度推理");
                reasoningReport = performDeepReasoning(context, diagnosisReport);
                log.debug("推理专家分析完成，报告长度: {} 字符", reasoningReport != null ? reasoningReport.length() : 0);

                // === 第四步：编码专家生成优化代码 ===
                log.info("💻 步骤 3：编码专家生成优化方案");
                optimizationCode = generateOptimizationCode(context, reasoningReport);
                log.debug("编码专家优化方案生成完成，长度: {} 字符", optimizationCode != null ? optimizationCode.length() : 0);
            }

            // === 第五步：整合所有输出 ===
            log.info("📝 步骤 4：整合分析报告");
            String finalReport = buildFinalReport(context, diagnosisReport, reasoningReport, optimizationCode);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 多 Agent 协作分析完成: fingerprint={}, 耗时={}ms", context.getSqlFingerprint(), duration);

            return finalReport;

        } catch (Exception e) {
            log.error("❌ 多 Agent 协作分析失败: fingerprint={}", context.getSqlFingerprint(), e);
            // 不再生成错误报告，直接抛出异常
            throw new RuntimeException("AI分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 步骤 1：主治医生初步诊断
     *
     * @param context 分析上下文
     * @return 诊断报告
     */
    private String performDiagnosis(AnalysisContext context) {
        log.info("调用主治医生 Agent 进行初步诊断");

        // 手动格式化提示词(解决 LangChain4j 占位符替换问题)
        String formattedPrompt = promptUtil.formatUserPrompt(
                context.getDbName(),
                formatTime(context.getTemplateStats().getFirstSeenTime()),
                context.getTemplateStats().getAvgQueryTime(),
                context.getTemplateStats().getAvgLockTime(),
                context.getTemplateStats().getMaxRowsExamined(),
                context.getTemplateStats().getMaxRowsSent(),
                context.getSampleSql()
        );

        log.info("📝 [提示词] 格式化完成,长度={}", formattedPrompt.length());

        try {
            return diagnosisAgent.analyzeSlowLog(formattedPrompt);
        } catch (Exception e) {
            log.error("主治医生诊断失败", e);
            throw new RuntimeException("主治医生诊断失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断是否需要升级到推理专家
     *
     * 升级条件：
     * 1. 高频 SQL（24 小时内出现 > 100 次）
     * 2. 严重慢查询（平均耗时 > 3 秒）
     * 3. 存在锁等待问题
     * 4. 疑似全表扫描
     *
     * @param context 分析上下文
     * @return true=需要升级，false=不需要
     */
    private boolean shouldUpgradeToExpert(AnalysisContext context) {
        AnalysisContext.TemplateStatisticsSnapshot stats = context.getTemplateStats();

        // 条件 1：高频 SQL
        if (stats.isHighFrequency()) {
            log.info("升级原因：高频 SQL（出现次数={}）", stats.getOccurrenceCount());
            return true;
        }

        // 条件 2：严重慢查询
        if (stats.isSevere()) {
            log.info("升级原因：严重慢查询（平均耗时={}s）", stats.getAvgQueryTime());
            return true;
        }

        // 条件 3：锁等待问题
        if (stats.hasLockIssue()) {
            log.info("升级原因：存在锁等待问题（平均锁等待={}s）", stats.getAvgLockTime());
            return true;
        }

        // 条件 4：疑似全表扫描
        if (stats.hasFullTableScan()) {
            log.info("升级原因：疑似全表扫描（扫描/返回={}）",
                stats.getAvgRowsExamined() / stats.getAvgRowsSent());
            return true;
        }

        return false;
    }

    /**
     * 步骤 3：推理专家深度推理
     *
     * @param context          分析上下文
     * @param diagnosisReport  主治医生的诊断报告
     * @return 深度推理报告
     */
    private String performDeepReasoning(AnalysisContext context, String diagnosisReport) {
        log.info("调用推理专家 Agent 进行深度推理");

        try {
            // 准备统计信息和执行计划的 JSON 格式
            String statisticsJson = toJson(context.getTemplateStats());
            String executionPlanJson = toJson(tools.getExecutionPlan(
                context.getDbName(),
                context.getSampleSql()
            ));

            return reasoningAgent.performDeepReasoning(
                diagnosisReport,
                statisticsJson,
                executionPlanJson
            );
        } catch (Exception e) {
            log.error("推理专家分析失败", e);
            return "## 推理专家分析失败\n\n错误信息：" + e.getMessage();
        }
    }

    /**
     * 步骤 4：编码专家生成优化代码
     *
     * @param context         分析上下文
     * @param reasoningReport 推理专家的分析报告
     * @return 优化代码
     */
    private String generateOptimizationCode(AnalysisContext context, String reasoningReport) {
        log.info("调用编码专家 Agent 生成优化方案");

        try {
            String executionPlanJson = toJson(tools.getExecutionPlan(
                context.getDbName(),
                context.getSampleSql()
            ));

            // 提取推理专家的核心建议作为问题描述
            String problemDesc = extractProblemDescription(reasoningReport);

            return codingAgent.generateOptimizationCode(
                context.getSampleSql(),
                problemDesc,
                executionPlanJson
            );
        } catch (Exception e) {
            log.error("编码专家生成优化方案失败", e);
            return "## 编码专家生成优化方案失败\n\n错误信息：" + e.getMessage();
        }
    }

    /**
     * 步骤 5：整合最终报告
     *
     * @param context          分析上下文
     * @param diagnosisReport  主治医生报告
     * @param reasoningReport  推理专家报告（可能为 null）
     * @param optimizationCode 优化代码（可能为 null）
     * @return 最终报告
     */
    private String buildFinalReport(
            AnalysisContext context,
            String diagnosisReport,
            String reasoningReport,
            String optimizationCode) {

        StringBuilder report = new StringBuilder();

        // === 报告头部 ===
        report.append("# 慢查询智能诊断报告\n\n");
        report.append("---\n\n");

        // === 基本信息 ===
        report.append("## 📊 基本信息\n\n");
        report.append("**数据库**: ").append(context.getDbName()).append("\n\n");
        report.append("**时间范围**: ").append(context.getTimeRangeDescription()).append("\n\n");
        report.append("**出现次数**: ").append(context.getTemplateStats().getOccurrenceCount()).append(" 次\n\n");
        report.append("**平均耗时**: ").append(String.format("%.3f", context.getTemplateStats().getAvgQueryTime())).append(" 秒\n\n");
        report.append("**平均锁等待**: ").append(String.format("%.3f", context.getTemplateStats().getAvgLockTime())).append(" 秒\n\n");

        // === 主治医生诊断 ===
        report.append("## 🔍 主治医生诊断\n\n");
        report.append(diagnosisReport).append("\n\n");

        // === 推理专家分析（如果存在）===
        if (reasoningReport != null) {
            report.append("---\n\n");
            report.append("## 🧠 推理专家深度分析\n\n");
            report.append(reasoningReport).append("\n\n");
        }

        // === 优化方案（如果存在）===
        if (optimizationCode != null) {
            report.append("---\n\n");
            report.append("## 💻 优化方案\n\n");
            report.append(optimizationCode).append("\n\n");
        }

        // === 报告尾部 ===
        report.append("---\n\n");
        report.append("**生成时间**: ").append(java.time.LocalDateTime.now()).append("\n\n");
        report.append("**DB-Doctor 版本**: v1.0.0\n\n");

        return report.toString();
    }

    /**
     * 构建错误报告
     *
     * @param context 分析上下文
     * @param e       异常
     * @return 错误报告
     */
    private String buildErrorReport(AnalysisContext context, Exception e) {
        StringBuilder report = new StringBuilder();

        report.append("# 慢查询智能诊断报告（分析失败）\n\n");
        report.append("---\n\n");
        report.append("## ❌ 错误信息\n\n");
        report.append("**错误类型**: ").append(e.getClass().getSimpleName()).append("\n\n");
        report.append("**错误描述**: ").append(e.getMessage()).append("\n\n");

        // 堆栈跟踪（DEBUG 模式）
        if (log.isDebugEnabled()) {
            report.append("**堆栈跟踪**:\n\n```\n");
            for (StackTraceElement element : e.getStackTrace()) {
                report.append(element.toString()).append("\n");
            }
            report.append("```\n\n");
        }

        // 基础信息
        report.append("---\n\n");
        report.append("## 📊 基本信息\n\n");
        report.append("**数据库**: ").append(context.getDbName()).append("\n\n");
        report.append("**出现次数**: ").append(context.getTemplateStats().getOccurrenceCount()).append(" 次\n\n");
        report.append("**平均耗时**: ").append(String.format("%.3f", context.getTemplateStats().getAvgQueryTime())).append(" 秒\n\n");

        return report.toString();
    }

    // === 辅助方法 ===

    /**
     * 将对象转换为 JSON 字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }

    /**
     * 格式化时间
     */
    private String formatTime(java.time.LocalDateTime time) {
        if (time == null) {
            return "未知";
        }
        return time.toString().replace('T', ' ').substring(0, 19);
    }

    /**
     * 从推理报告中提取问题描述
     */
    private String extractProblemDescription(String reasoningReport) {
        // 简单提取：取前 500 个字符作为问题描述
        if (reasoningReport == null) {
            return "无问题描述";
        }
        return reasoningReport.length() > 500
            ? reasoningReport.substring(0, 500) + "..."
            : reasoningReport;
    }
}
