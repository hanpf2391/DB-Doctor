package com.dbdoctor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 模型定价配置
 *
 * @author DB-Doctor
 * @version 2.3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelPricing {

    /**
     * 输入 Token 单价（美元/百万 Token）
     */
    private Double inputPrice;

    /**
     * 输出 Token 单价（美元/百万 Token）
     */
    private Double outputPrice;

    /**
     * 模型供应商（openai, ollama, deepseek 等）
     */
    private String provider;
}
