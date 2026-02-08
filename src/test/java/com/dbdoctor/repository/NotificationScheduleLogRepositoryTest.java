package com.dbdoctor.repository;

import com.dbdoctor.entity.NotificationScheduleLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationScheduleLogRepository 单元测试
 *
 * TDD 流程：
 * - RED: 先编写测试，预期失败
 * - GREEN: 实现 Repository，使测试通过
 * - REFACTOR: 重构代码，保持测试通过
 *
 * @author DB-Doctor
 * @version 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
class NotificationScheduleLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationScheduleLogRepository repository;

    private NotificationScheduleLog log1;
    private NotificationScheduleLog log2;
    private NotificationScheduleLog log3;

    @BeforeEach
    void setUp() {
        // 清空数据
        repository.deleteAll();

        // 创建测试数据
        LocalDateTime now = LocalDateTime.now();

        log1 = NotificationScheduleLog.builder()
            .executionId(UUID.randomUUID().toString())
            .triggerTime(now)
            .status("SUCCESS")
            .windowStart(now.minusHours(1))
            .windowEnd(now)
            .waitingCount(10)
            .sentCount(10)
            .durationMs(1234L)
            .build();

        log2 = NotificationScheduleLog.builder()
            .executionId(UUID.randomUUID().toString())
            .triggerTime(now.minusHours(2))
            .status("FAILED")
            .windowStart(now.minusHours(3))
            .windowEnd(now.minusHours(2))
            .waitingCount(5)
            .sentCount(0)
            .failedChannels("[\"EMAIL\"]")
            .durationMs(567L)
            .build();

        log3 = NotificationScheduleLog.builder()
            .executionId(UUID.randomUUID().toString())
            .triggerTime(now.minusDays(1))
            .status("SUCCESS")
            .windowStart(now.minusDays(1).minusHours(1))
            .windowEnd(now.minusDays(1))
            .waitingCount(8)
            .sentCount(8)
            .durationMs(890L)
            .build();

        // 保存测试数据
        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.persist(log3);
        entityManager.flush();
    }

    @Test
    void testFindByStatus() {
        // Given: 状态为 SUCCESS 的日志有 2 条（log1, log3）
        String status = "SUCCESS";
        Pageable pageable = PageRequest.of(0, 10);

        // When: 根据状态查询
        Page<NotificationScheduleLog> result = repository.findByStatus(status, pageable);

        // Then: 应该返回 2 条记录
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        // 验证返回的都是 SUCCESS 状态
        result.getContent().forEach(log -> assertEquals("SUCCESS", log.getStatus()));
    }

    @Test
    void testFindByTriggerTimeBetween() {
        // Given: 时间范围为最近 2 小时
        LocalDateTime startTime = LocalDateTime.now().minusHours(3);
        LocalDateTime endTime = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        // When: 根据时间范围查询
        Page<NotificationScheduleLog> result = repository.findByTriggerTimeBetween(startTime, endTime, pageable);

        // Then: 应该返回 2 条记录（log1, log2）
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void testFindByStatusAndTriggerTimeBetween() {
        // Given: 状态为 SUCCESS，时间范围为最近 2 小时
        String status = "SUCCESS";
        LocalDateTime startTime = LocalDateTime.now().minusHours(3);
        LocalDateTime endTime = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        // When: 根据状态和时间范围查询
        Page<NotificationScheduleLog> result = repository.findByStatusAndTriggerTimeBetween(
            status, startTime, endTime, pageable
        );

        // Then: 应该返回 1 条记录（log1）
        assertEquals(1, result.getTotalElements());
        assertEquals("SUCCESS", result.getContent().get(0).getStatus());
    }

    @Test
    void testFindAllOrderByTriggerTimeDesc() {
        // Given: 分页参数
        Pageable pageable = PageRequest.of(0, 10);

        // When: 查询所有日志，按触发时间倒序
        Page<NotificationScheduleLog> result = repository.findAllOrderByTriggerTimeDesc(pageable);

        // Then: 应该返回 3 条记录，并按时间倒序排列
        assertEquals(3, result.getTotalElements());

        // 验证顺序：log1（最新） > log2 > log3（最旧）
        assertTrue(result.getContent().get(0).getTriggerTime().isAfter(result.getContent().get(1).getTriggerTime()));
        assertTrue(result.getContent().get(1).getTriggerTime().isAfter(result.getContent().get(2).getTriggerTime()));
    }

    @Test
    void testSave() {
        // Given: 新的日志记录
        NotificationScheduleLog newLog = NotificationScheduleLog.builder()
            .triggerTime(LocalDateTime.now())
            .status("SUCCESS")
            .waitingCount(15)
            .sentCount(15)
            .durationMs(2000L)
            .build();

        // When: 保存日志
        NotificationScheduleLog saved = repository.save(newLog);

        // Then: 应该生成 ID
        assertNotNull(saved.getId());
        assertNotNull(saved.getExecutionId());
        assertNotNull(saved.getCreatedAt());
    }
}
