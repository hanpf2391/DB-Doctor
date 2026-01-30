package com.dbdoctor.service;

import com.dbdoctor.entity.SlowQueryTemplate;
import com.dbdoctor.repository.SlowQuerySampleRepository;
import com.dbdoctor.repository.SlowQueryTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统维护服务
 *
 * @author DB-Doctor
 * @version 2.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final SlowQueryTemplateRepository templateRepository;
    private final SlowQuerySampleRepository sampleRepository;

    /**
     * 清理指定天数之前的历史数据
     *
     * @param days 天数
     * @return 删除数量
     */
    @Transactional
    public Map<String, Integer> cleanupHistory(int days) {
        log.info("开始清理历史数据: days={}", days);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        // 查询需要删除的模板记录
        List<SlowQueryTemplate> templates = templateRepository.findByLastSeenTimeBefore(cutoffDate);

        int deletedTemplates = 0;
        int deletedSamples = 0;

        for (SlowQueryTemplate template : templates) {
            // 先删除关联的样本记录
            long sampleCount = sampleRepository.countBySqlFingerprint(template.getSqlFingerprint());
            if (sampleCount > 0) {
                sampleRepository.deleteBySqlFingerprint(template.getSqlFingerprint());
                deletedSamples += sampleCount;
            }

            // 再删除模板记录
            templateRepository.delete(template);
            deletedTemplates++;
        }

        log.info("清理完成: 删除 {} 条模板, {} 条样本", deletedTemplates, deletedSamples);

        return Map.of(
                "deletedTemplates", deletedTemplates,
                "deletedSamples", deletedSamples
        );
    }

    /**
     * 重置系统（清空所有慢查询数据）
     *
     * @return 删除数量
     */
    @Transactional
    public Map<String, Integer> resetSystem() {
        log.warn("执行系统重置操作");

        long totalSamples = sampleRepository.count();
        long totalTemplates = templateRepository.count();

        // 先清空样本表
        sampleRepository.deleteAll();

        // 再清空模板表
        templateRepository.deleteAll();

        log.warn("系统重置完成: 删除 {} 条模板, {} 条样本", totalTemplates, totalSamples);

        return Map.of(
                "deletedTemplates", (int) totalTemplates,
                "deletedSamples", (int) totalSamples
        );
    }
}
