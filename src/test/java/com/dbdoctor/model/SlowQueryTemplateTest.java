package com.dbdoctor.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlowQueryTemplate 实体单元测试
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
class SlowQueryTemplateTest {

    @Test
    void shouldReAnalyze_当状态为ERROR时返回true() {
        // Given
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .status(SlowQueryTemplate.AnalysisStatus.ERROR)
                .firstSeenTime(LocalDateTime.now())
                .lastSeenTime(LocalDateTime.now())
                .build();

        // When
        boolean result = template.shouldReAnalyze();

        // Then
        assertTrue(result, "状态为ERROR时应该返回true");
    }

    @Test
    void shouldReAnalyze_当距离首次发现超过7天时返回true() {
        // Given
        LocalDateTime firstSeen = LocalDateTime.now().minusDays(10);
        LocalDateTime lastSeen = LocalDateTime.now().minusDays(1);

        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .status(SlowQueryTemplate.AnalysisStatus.SUCCESS)
                .firstSeenTime(firstSeen)
                .lastSeenTime(lastSeen)
                .build();

        // When
        boolean result = template.shouldReAnalyze();

        // Then
        assertTrue(result, "距离首次发现超过7天时应该返回true");
    }

    @Test
    void shouldReAnalyze_当状态为SUCCESS且未超过7天时返回false() {
        // Given
        LocalDateTime firstSeen = LocalDateTime.now().minusDays(3);
        LocalDateTime lastSeen = LocalDateTime.now().minusDays(1);

        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .status(SlowQueryTemplate.AnalysisStatus.SUCCESS)
                .firstSeenTime(firstSeen)
                .lastSeenTime(lastSeen)
                .build();

        // When
        boolean result = template.shouldReAnalyze();

        // Then
        assertFalse(result, "状态为SUCCESS且未超过7天时应该返回false");
    }

    @Test
    void shouldNotify_首次通知时返回true() {
        // Given
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .lastNotifiedTime(null)
                .build();

        // When
        boolean result = template.shouldNotify(1, 1.5, 3.0);

        // Then
        assertTrue(result, "首次通知时应该返回true");
    }

    @Test
    void shouldNotify_性能显著恶化时返回true() {
        // Given
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .lastNotifiedTime(LocalDateTime.now().minusHours(2))  // 2小时前通知过
                .lastNotifiedAvgTime(2.0)  // 上次通知时耗时2秒
                .build();

        // When
        boolean result = template.shouldNotify(1, 1.5, 3.5);  // 当前耗时3.5秒，恶化75%

        // Then
        assertTrue(result, "性能恶化超过阈值时应该返回true");
    }

    @Test
    void shouldNotify_在冷却期内且性能未恶化时返回false() {
        // Given
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .lastNotifiedTime(LocalDateTime.now().minusHours(2))  // 2小时前通知过
                .lastNotifiedAvgTime(3.0)  // 上次通知时耗时3秒
                .build();

        // When
        boolean result = template.shouldNotify(5, 1.5, 3.2);  // 冷却期5小时，性能恶化未超过阈值

        // Then
        assertFalse(result, "在冷却期内且性能未恶化时应该返回false");
    }

    @Test
    void shouldNotify_超过冷却期时返回true() {
        // Given
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .lastNotifiedTime(LocalDateTime.now().minusHours(10))  // 10小时前通知过
                .lastNotifiedAvgTime(3.0)
                .build();

        // When
        boolean result = template.shouldNotify(5, 1.5, 3.2);  // 冷却期5小时，已超过10小时

        // Then
        assertTrue(result, "超过冷却期时应该返回true");
    }

    @Test
    void updateNotificationInfo_正确更新通知信息() {
        // Given
        SlowQueryTemplate template = SlowQueryTemplate.builder()
                .sqlFingerprint("abc123")
                .lastNotifiedTime(null)
                .lastNotifiedAvgTime(null)
                .build();

        // When
        template.updateNotificationInfo(3.5);

        // Then
        assertNotNull(template.getLastNotifiedTime());
        assertEquals(3.5, template.getLastNotifiedAvgTime());
    }
}
