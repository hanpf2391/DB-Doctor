package com.dbdoctor.service;

import com.dbdoctor.dto.NotificationScheduleLogDTO;
import com.dbdoctor.entity.NotificationScheduleLog;
import com.dbdoctor.repository.NotificationScheduleLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通知调度日志服务
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleLogService {

    private final NotificationScheduleLogRepository repository;

    /**
     * 保存日志
     */
    public NotificationScheduleLog save(NotificationScheduleLog log) {
        return repository.save(log);
    }

    /**
     * 分页查询日志
     */
    public Page<NotificationScheduleLogDTO> findLogs(
            Integer page,
            Integer size,
            String status,
            LocalDate startDate,
            LocalDate endDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "triggerTime"));
        Page<NotificationScheduleLog> logPage;

        if (status != null && !status.isEmpty()) {
            if (startDate != null && endDate != null) {
                logPage = repository.findByStatusAndTriggerTimeBetween(
                    status,
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay(),
                    pageable
                );
            } else {
                logPage = repository.findByStatus(status, pageable);
            }
        } else if (startDate != null && endDate != null) {
            logPage = repository.findByTriggerTimeBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                pageable
            );
        } else {
            logPage = repository.findAllOrderByTriggerTimeDesc(pageable);
        }

        return logPage.map(this::toDTO);
    }

    /**
     * 查询日志详情
     */
    public NotificationScheduleLogDTO findById(Long id) {
        return repository.findById(id)
            .map(this::toDTO)
            .orElse(null);
    }

    /**
     * 转换为 DTO
     */
    private NotificationScheduleLogDTO toDTO(NotificationScheduleLog log) {
        return NotificationScheduleLogDTO.builder()
            .id(log.getId())
            .executionId(log.getExecutionId())
            .triggerTime(log.getTriggerTime())
            .status(log.getStatus())
            .windowStart(log.getWindowStart())
            .windowEnd(log.getWindowEnd())
            .waitingCount(log.getWaitingCount())
            .sentCount(log.getSentCount())
            .failedChannels(log.getFailedChannels())
            .durationMs(log.getDurationMs())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
