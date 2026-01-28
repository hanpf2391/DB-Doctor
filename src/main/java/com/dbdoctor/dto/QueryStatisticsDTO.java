package com.dbdoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 慢查询统计信息DTO
 * 用于返回实时计算的统计结果
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryStatisticsDTO {

    /**
     * 出现次数
     */
    private Long occurrenceCount;

    /**
     * 平均查询耗时（秒）
     */
    private Double avgQueryTime;

    /**
     * 最大查询耗时（秒）
     */
    private Double maxQueryTime;

    /**
     * 平均锁等待时间（秒）
     */
    private Double avgLockTime;

    /**
     * 最大锁等待时间（秒）
     */
    private Double maxLockTime;

    /**
     * 平均返回行数
     */
    private Double avgRowsSent;

    /**
     * 最大返回行数
     */
    private Long maxRowsSent;

    /**
     * 最大扫描行数
     */
    private Long maxRowsExamined;

    /**
     * 首次发现时间
     */
    private java.time.LocalDateTime firstSeenTime;

    /**
     * 最近发现时间
     */
    private java.time.LocalDateTime lastSeenTime;
}
