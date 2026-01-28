package com.dbdoctor.repository;

import com.dbdoctor.dto.QueryStatisticsDTO;
import com.dbdoctor.model.SlowQuerySample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlowQuerySampleRepository 集成测试
 *
 * @author DB-Doctor
 * @version 2.1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class SlowQuerySampleRepositoryTest {

    @Autowired
    private SlowQuerySampleRepository sampleRepo;

    private String testFingerprint = "test_fingerprint_123";

    @BeforeEach
    void setUp() {
        // 清空测试数据
        sampleRepo.deleteAll();
    }

    @Test
    void save_保存样本成功() {
        // Given
        SlowQuerySample sample = SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SELECT * FROM users WHERE id = 123")
                .queryTime(3.5)
                .lockTime(0.1)
                .rowsSent(100L)
                .rowsExamined(50000L)
                .capturedAt(LocalDateTime.now())
                .build();

        // When
        SlowQuerySample saved = sampleRepo.save(sample);

        // Then
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(testFingerprint, saved.getSqlFingerprint());
    }

    @Test
    void findAllBySqlFingerprintOrderByCapturedAtDesc_按时间倒序查询() {
        // Given
        LocalDateTime time1 = LocalDateTime.now().minusHours(2);
        LocalDateTime time2 = LocalDateTime.now().minusHours(1);
        LocalDateTime time3 = LocalDateTime.now();

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL1")
                .capturedAt(time1)
                .build());

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL2")
                .capturedAt(time2)
                .build());

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL3")
                .capturedAt(time3)
                .build());

        // When
        List<SlowQuerySample> samples = sampleRepo.findAllBySqlFingerprintOrderByCapturedAtDesc(testFingerprint);

        // Then
        assertEquals(3, samples.size());
        assertEquals("SQL3", samples.get(0).getOriginalSql()); // 最新的在前
        assertEquals("SQL2", samples.get(1).getOriginalSql());
        assertEquals("SQL1", samples.get(2).getOriginalSql());
    }

    @Test
    void countBySqlFingerprint_统计出现次数() {
        // Given
        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL1")
                .build());

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL2")
                .build());

        // When
        long count = sampleRepo.countBySqlFingerprint(testFingerprint);

        // Then
        assertEquals(2L, count);
    }

    @Test
    void calculateStatistics_计算统计信息() {
        // Given
        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL1")
                .queryTime(2.0)
                .lockTime(0.1)
                .rowsSent(100L)
                .rowsExamined(1000L)
                .capturedAt(LocalDateTime.now().minusHours(2))
                .build());

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("SQL2")
                .queryTime(4.0)
                .lockTime(0.2)
                .rowsSent(200L)
                .rowsExamined(2000L)
                .capturedAt(LocalDateTime.now())
                .build());

        // When
        QueryStatisticsDTO stats = sampleRepo.calculateStatistics(testFingerprint);

        // Then
        assertNotNull(stats);
        assertEquals(2L, stats.getOccurrenceCount());
        assertEquals(3.0, stats.getAvgQueryTime(), 0.01); // (2+4)/2 = 3
        assertEquals(4.0, stats.getMaxQueryTime());
        assertEquals(0.15, stats.getAvgLockTime(), 0.01); // (0.1+0.2)/2 = 0.15
        assertEquals(0.2, stats.getMaxLockTime());
        assertEquals(150.0, stats.getAvgRowsSent(), 0.01); // (100+200)/2 = 150
        assertEquals(200L, stats.getMaxRowsSent());
        assertEquals(2000L, stats.getMaxRowsExamined());
    }

    @Test
    void countByCapturedAtAfter_统计指定时间之后的数量() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("OLD")
                .capturedAt(LocalDateTime.now().minusHours(2))
                .build());

        sampleRepo.save(SlowQuerySample.builder()
                .sqlFingerprint(testFingerprint)
                .originalSql("NEW")
                .capturedAt(LocalDateTime.now())
                .build());

        // When
        long count = sampleRepo.countByCapturedAtAfter(cutoff);

        // Then
        assertEquals(1L, count); // 只有NEW在cutoff之后
    }
}
