package com.dbdoctor.service;

import com.dbdoctor.entity.AiInvocationLog;
import com.dbdoctor.model.CostStats;
import com.dbdoctor.model.ModelPricing;
import com.dbdoctor.repository.AiInvocationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 成本分析服务
 *
 * <p>功能：</p>
 * <ul>
 *   <li>计算单次 AI 调用成本</li>
 *   <li>统计时间范围内的成本分布</li>
 *   <li>按模型、Agent 维度分析成本</li>
 * </ul>
 *
 * @author DB-Doctor
 * @version 2.3.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCostService {

    private final AiInvocationLogRepository repository;

    /**
     * 模型定价配置（从 application.yml 读取）
     *
     * 示例配置：
     * db-doctor:
     *   ai:
     *     cost:
     *       model-pricing:
     *         gpt-4o: { input-price: 5.0, output-price: 15.0, provider: openai }
     *         qwen: { input-price: 0.0, output-price: 0.0, provider: ollama }
     */
    @Value("#{${db-doctor.ai.cost.model-pricing:{}}}")
    private Map<String, ModelPricing> modelPricingMap = new HashMap<>();

    /**
     * 默认定价（配置缺失时使用）
     */
    private static final ModelPricing DEFAULT_PRICING =
            ModelPricing.builder()
                    .inputPrice(0.0)
                    .outputPrice(0.0)
                    .provider("unknown")
                    .build();

    /**
     * 计算单次调用成本
     *
     * @param modelName    模型名称
     * @param inputTokens  输入 Token 数
     * @param outputTokens 输出 Token 数
     * @return 成本（美元）
     */
    public double calculateCost(String modelName, int inputTokens, int outputTokens) {
        ModelPricing pricing = findPricing(modelName);

        // Token 数转换为百万单位
        double inputMillions = inputTokens / 1_000_000.0;
        double outputMillions = outputTokens / 1_000_000.0;

        // 计算成本
        double inputCost = inputMillions * pricing.getInputPrice();
        double outputCost = outputMillions * pricing.getOutputPrice();
        double totalCost = inputCost + outputCost;

        log.debug("[成本分析] model={}, inTokens={}, outTokens={}, cost=${}",
                modelName, inputTokens, outputTokens,
                String.format("%.6f", totalCost));

        return totalCost;
    }

    /**
     * 获取时间范围内的成本统计
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成本统计
     */
    public CostStats getCostStats(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("[成本分析] 查询成本统计: {} ~ {}", startTime, endTime);

        // 查询所有调用日志
        List<AiInvocationLog> logs = repository.findByStartTimeBetween(startTime, endTime);

        if (logs.isEmpty()) {
            log.warn("[成本分析] 未找到调用记录");
            return buildEmptyStats(startTime, endTime);
        }

        // 统计数据
        double totalCost = 0.0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        Map<String, Double> costByModel = new HashMap<>();
        Map<String, Double> costByAgent = new HashMap<>();

        for (AiInvocationLog log : logs) {
            // 计算 单次成本
            double cost = calculateCost(
                    log.getModelName(),
                    log.getInputTokens(),
                    log.getOutputTokens()
            );

            // 累加统计
            totalCost += cost;
            totalInputTokens += log.getInputTokens();
            totalOutputTokens += log.getOutputTokens();

            // 按模型统计
            costByModel.merge(log.getModelName(), cost, Double::sum);

            // 按 Agent 统计
            costByAgent.merge(log.getAgentName(), cost, Double::sum);
        }

        // 构建统计结果
        String timeRange = formatTimeRange(startTime, endTime);

        return CostStats.builder()
                .totalCost(totalCost)
                .costByModel(sortByValueDescending(costByModel))
                .costByAgent(sortByValueDescending(costByAgent))
                .totalInputTokens(totalInputTokens)
                .totalOutputTokens(totalOutputTokens)
                .totalTokens(totalInputTokens + totalOutputTokens)
                .totalCalls((long) logs.size())
                .avgCostPerCall(totalCost / logs.size())
                .timeRange(timeRange)
                .build();
    }

    /**
     * 查找模型定价配置
     *
     * @param modelName 模型名称
     * @return 定价配置
     */
    private ModelPricing findPricing(String modelName) {
        if (modelName == null || modelPricingMap.isEmpty()) {
            return DEFAULT_PRICING;
        }

        // 精确匹配
        if (modelPricingMap.containsKey(modelName)) {
            return modelPricingMap.get(modelName);
        }

        // 模糊匹配（如 "gpt-4o-mini" 匹配 "gpt-4o"）
        for (Map.Entry<String, ModelPricing> entry : modelPricingMap.entrySet()) {
            if (modelName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        log.warn("[成本分析] 未找到模型 {} 的定价配置，使用默认值", modelName);
        return DEFAULT_PRICING;
    }

    /**
     * 按值降序排序 Map
     */
    private Map<String, Double> sortByValueDescending(Map<String, Double> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }

    /**
     * 构建空统计结果
     */
    private CostStats buildEmptyStats(LocalDateTime startTime, LocalDateTime endTime) {
        return CostStats.builder()
                .totalCost(0.0)
                .costByModel(new HashMap<>())
                .costByAgent(new HashMap<>())
                .totalInputTokens(0L)
                .totalOutputTokens(0L)
                .totalTokens(0L)
                .totalCalls(0L)
                .avgCostPerCall(0.0)
                .timeRange(formatTimeRange(startTime, endTime))
                .build();
    }

    /**
     * 格式化时间范围
     */
    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return start.format(formatter) + " ~ " + end.format(formatter);
    }
}
