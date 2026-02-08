package com.dbdoctor.service;

import com.dbdoctor.dto.NotificationScheduleLogDTO;
import com.dbdoctor.entity.NotificationScheduleLog;
import com.dbdoctor.repository.NotificationScheduleLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationScheduleLogService 单元测试
 *
 * TDD 流程：
 * - RED: 先编写测试，预期失败
 * - GREEN: 实现 Service，使测试通过
 * - REFACTOR: 重构代码，保持测试通过
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationScheduleLogServiceTest {

    @Mock
    private NotificationScheduleLogRepository repository;

    @InjectMocks
    private NotificationScheduleLogService service;

    private NotificationScheduleLog log1;
    private NotificationScheduleLog log2;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        log1 = NotificationScheduleLog.builder()
            .id(1L)
            .executionId(UUID.randomUUID().toString())
            .triggerTime(now)
            .status("SUCCESS")
            .windowStart(now.minusHours(1))
            .windowEnd(now)
            .waitingCount(10)
            .sentCount(10)
            .durationMs(1234L)
            .createdAt(now)
            .build();

        log2 = NotificationScheduleLog.builder()
            .id(2L)
            .executionId(UUID.randomUUID().toString())
            .triggerTime(now.minusHours(2))
            .status("FAILED")
            .windowStart(now.minusHours(3))
            .windowEnd(now.minusHours(2))
            .waitingCount(5)
            .sentCount(0)
            .failedChannels("[\"EMAIL\"]")
            .durationMs(567L)
            .createdAt(now.minusHours(2))
            .build();
    }

    @Test
    void testSave() {
        // Given: 待保存的日志
        NotificationScheduleLog logToSave = log1;
        logToSave.setId(null); // 新记录，ID 为 null

        when(repository.save(any(NotificationScheduleLog.class))).thenReturn(log1);

        // When: 保存日志
        NotificationScheduleLog result = service.save(logToSave);

        // Then: 验证保存成功
        assertNotNull(result);
        assertEquals(log1.getId(), result.getId());
        assertEquals(log1.getExecutionId(), result.getExecutionId());

        verify(repository, times(1)).save(logToSave);
    }

    @Test
    void testFindLogsWithNoFilters() {
        // Given: 无筛选条件
        Integer page = 0;
        Integer size = 20;
        String status = null;
        LocalDate startDate = null;
        LocalDate endDate = null;

        Page<NotificationScheduleLog> mockPage = new PageImpl<>(List.of(log1, log2));
        when(repository.findAllOrderByTriggerTimeDesc(any())).thenReturn(mockPage);

        // When: 分页查询
        Page<NotificationScheduleLogDTO> result = service.findLogs(page, size, status, startDate, endDate);

        // Then: 验证返回结果
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        // 验证 DTO 转换
        NotificationScheduleLogDTO dto1 = result.getContent().get(0);
        assertEquals(log1.getId(), dto1.getId());
        assertEquals(log1.getStatus(), dto1.getStatus());

        verify(repository, times(1)).findAllOrderByTriggerTimeDesc(any());
    }

    @Test
    void testFindLogsWithStatusFilter() {
        // Given: 按状态筛选
        Integer page = 0;
        Integer size = 20;
        String status = "SUCCESS";
        LocalDate startDate = null;
        LocalDate endDate = null;

        Page<NotificationScheduleLog> mockPage = new PageImpl<>(List.of(log1));
        when(repository.findByStatus(anyString(), any())).thenReturn(mockPage);

        // When: 分页查询
        Page<NotificationScheduleLogDTO> result = service.findLogs(page, size, status, startDate, endDate);

        // Then: 验证返回结果
        assertEquals(1, result.getTotalElements());
        assertEquals("SUCCESS", result.getContent().get(0).getStatus());

        verify(repository, times(1)).findByStatus(eq("SUCCESS"), any());
    }

    @Test
    void testFindLogsWithDateRangeFilter() {
        // Given: 按日期范围筛选
        Integer page = 0;
        Integer size = 20;
        String status = null;
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        Page<NotificationScheduleLog> mockPage = new PageImpl<>(List.of(log1, log2));
        when(repository.findByTriggerTimeBetween(any(), any(), any())).thenReturn(mockPage);

        // When: 分页查询
        Page<NotificationScheduleLogDTO> result = service.findLogs(page, size, status, startDate, endDate);

        // Then: 验证返回结果
        assertEquals(2, result.getTotalElements());

        verify(repository, times(1)).findByTriggerTimeBetween(any(), any(), any());
    }

    @Test
    void testFindLogsWithStatusAndDateRangeFilter() {
        // Given: 按状态和日期范围筛选
        Integer page = 0;
        Integer size = 20;
        String status = "FAILED";
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        Page<NotificationScheduleLog> mockPage = new PageImpl<>(List.of(log2));
        when(repository.findByStatusAndTriggerTimeBetween(any(), any(), any(), any()))
            .thenReturn(mockPage);

        // When: 分页查询
        Page<NotificationScheduleLogDTO> result = service.findLogs(page, size, status, startDate, endDate);

        // Then: 验证返回结果
        assertEquals(1, result.getTotalElements());
        assertEquals("FAILED", result.getContent().get(0).getStatus());

        verify(repository, times(1)).findByStatusAndTriggerTimeBetween(
            eq("FAILED"), any(), any(), any()
        );
    }

    @Test
    void testFindById() {
        // Given: 日志 ID
        Long id = 1L;

        when(repository.findById(id)).thenReturn(Optional.of(log1));

        // When: 根据ID查询
        NotificationScheduleLogDTO result = service.findById(id);

        // Then: 验证返回结果
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(log1.getExecutionId(), result.getExecutionId());

        verify(repository, times(1)).findById(id);
    }

    @Test
    void testFindByIdNotFound() {
        // Given: 不存在的日志 ID
        Long id = 999L;

        when(repository.findById(id)).thenReturn(Optional.empty());

        // When: 根据ID查询
        NotificationScheduleLogDTO result = service.findById(id);

        // Then: 返回 null
        assertNull(result);

        verify(repository, times(1)).findById(id);
    }
}
