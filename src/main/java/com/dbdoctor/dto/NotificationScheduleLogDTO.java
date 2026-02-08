package com.dbdoctor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知调度日志 DTO
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Data
@Builder
public class NotificationScheduleLogDTO {
    private Long id;
    private String executionId;
    private LocalDateTime triggerTime;
    private String status;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private Integer waitingCount;
    private Integer sentCount;
    private String failedChannels;
    private Long durationMs;
    private LocalDateTime createdAt;
}
