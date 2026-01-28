package com.dbdoctor.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlowQuerySample 实体单元测试
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
class SlowQuerySampleTest {

    @Test
    void builder_正确构建样本对象() {
        // Given
        String fingerprint = "abc123";
        String originalSql = "SELECT * FROM users WHERE id = 123";
        Double queryTime = 3.5;
        Double lockTime = 0.1;
        Long rowsSent = 100L;
        Long rowsExamined = 50000L;
        LocalDateTime capturedAt = LocalDateTime.now();

        // When
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint(fingerprint)
                .originalSql(originalSql)
                .queryTime(queryTime)
                .lockTime(lockTime)
                .rowsSent(rowsSent)
                .rowsExamined(rowsExamined)
                .capturedAt(capturedAt)
                .build();

        // Then
        assertNotNull(sample);
        assertEquals(fingerprint, sample.getSqlFingerprint());
        assertEquals(originalSql, sample.getOriginalSql());
        assertEquals(queryTime, sample.getQueryTime());
        assertEquals(lockTime, sample.getLockTime());
        assertEquals(rowsSent, sample.getRowsSent());
        assertEquals(rowsExamined, sample.getRowsExamined());
        assertEquals(capturedAt, sample.getCapturedAt());
    }

    @Test
    void toString_验证Lombok注解生效() {
        // Given
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint("abc123")
                .originalSql("SELECT * FROM users")
                .queryTime(3.5)
                .build();

        // When
        String str = sample.toString();

        // Then
        assertNotNull(str);
        assertTrue(str.contains("abc123"));
        assertTrue(str.contains("SELECT * FROM users"));
    }
}
