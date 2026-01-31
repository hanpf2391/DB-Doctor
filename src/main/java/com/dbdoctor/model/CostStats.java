package com.dbdoctor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI 成本统计
 *
 * @author DB-Doctor
 * @version 2.3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostStats {

    /**
     * 总成本（美元）
     */
    private Double totalCost;

    /**
     * 各模型成本分布
     * Key: 模型名称（如 gpt-4o, qwen）
     * Value: 成本（美元）
     */
    private Map<String, Double> costByModel;

    /**
     * 各 Agent 成本分布
     * Key: Agent 名称（DIAGNOSIS, REASONING, CODING）
     * Value: 成本（美元）
     */
    private Map<String, Double> costByAgent;

    /**
     * 输入 Token 总数
     */
    private Long totalInputTokens;

    /**
     * 输出 Token 总数
     */
    private Long totalOutputTokens;

    /**
     * 总 Token 数
     */
    private Long totalTokens;

    /**
     * 调用总次数
     */
    private Long totalCalls;

    /**
     * 平均每次调用成本
     */
    private Double avgCostPerCall;

    /**
     * 统计时间范围描述
     */
    private String timeRange;
}
