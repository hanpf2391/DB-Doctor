package com.dbdoctor.repository;

import com.dbdoctor.entity.AlertRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * AlertRule Repository 集成测试
 *
 * <p>TDD 红色阶段：测试实体映射和自定义查询方法</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AlertRule Repository 测试")
class AlertRuleRepositoryTest {

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        alertRuleRepository.deleteAll();

        testRule = AlertRule.builder()
            .name("test-rule")
            .displayName("测试规则")
            .type("THRESHOLD")
            .metricName("testMetric")
            .conditionOperator(">")
            .thresholdValue(10.0)
            .severity("WARNING")
            .enabled(true)
            .coolDownMinutes(30)
            .description("测试用告警规则")
            .build();

        testRule = alertRuleRepository.save(testRule);
    }

    @Test
    @DisplayName("保存告警规则 - 成功")
    void testSaveAlertRule() {
        // Given
        AlertRule newRule = AlertRule.builder()
            .name("new-rule")
            .displayName("新规则")
            .type("ANOMALY")
            .metricName("anomalyMetric")
            .conditionOperator("=")
            .thresholdValue(1.0)
            .severity("CRITICAL")
            .enabled(true)
            .coolDownMinutes(60)
            .description("新告警规则")
            .build();

        // When
        AlertRule savedRule = alertRuleRepository.save(newRule);

        // Then
        assertThat(savedRule).isNotNull();
        assertThat(savedRule.getId()).isNotNull();
        assertThat(savedRule.getName()).isEqualTo("new-rule");
        assertThat(savedRule.getCreatedAt()).isNotNull();
        assertThat(savedRule.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("根据规则名称查询 - 成功")
    void testFindByName() {
        // When
        Optional<AlertRule> foundRule = alertRuleRepository.findByName("test-rule");

        // Then
        assertThat(foundRule).isPresent();
        assertThat(foundRule.get().getDisplayName()).isEqualTo("测试规则");
    }

    @Test
    @DisplayName("根据规则名称查询 - 不存在")
    void testFindByNameNotFound() {
        // When
        Optional<AlertRule> foundRule = alertRuleRepository.findByName("non-existent");

        // Then
        assertThat(foundRule).isEmpty();
    }

    @Test
    @DisplayName("根据启用状态查询 - 成功")
    void testFindByEnabled() {
        // Given
        AlertRule disabledRule = AlertRule.builder()
            .name("disabled-rule")
            .displayName("禁用规则")
            .type("THRESHOLD")
            .metricName("metric1")
            .conditionOperator(">")
            .thresholdValue(5.0)
            .severity("INFO")
            .enabled(false)
            .coolDownMinutes(30)
            .build();
        alertRuleRepository.save(disabledRule);

        // When
        List<AlertRule> enabledRules = alertRuleRepository.findByEnabled(true);
        List<AlertRule> disabledRules = alertRuleRepository.findByEnabled(false);

        // Then
        assertThat(enabledRules).hasSize(1);
        assertThat(enabledRules.get(0).getName()).isEqualTo("test-rule");
        assertThat(disabledRules).hasSize(1);
        assertThat(disabledRules.get(0).getName()).isEqualTo("disabled-rule");
    }

    @Test
    @DisplayName("根据规则类型查询 - 成功")
    void testFindByType() {
        // Given
        AlertRule anomalyRule = AlertRule.builder()
            .name("anomaly-rule")
            .displayName("异常规则")
            .type("ANOMALY")
            .metricName("metric2")
            .conditionOperator("=")
            .thresholdValue(1.0)
            .severity("CRITICAL")
            .enabled(true)
            .coolDownMinutes(30)
            .build();
        alertRuleRepository.save(anomalyRule);

        // When
        List<AlertRule> thresholdRules = alertRuleRepository.findByType("THRESHOLD");
        List<AlertRule> anomalyRules = alertRuleRepository.findByType("ANOMALY");

        // Then
        assertThat(thresholdRules).hasSize(1);
        assertThat(thresholdRules.get(0).getType()).isEqualTo("THRESHOLD");
        assertThat(anomalyRules).hasSize(1);
        assertThat(anomalyRules.get(0).getType()).isEqualTo("ANOMALY");
    }

    @Test
    @DisplayName("根据严重程度查询 - 成功")
    void testFindBySeverity() {
        // Given
        AlertRule criticalRule = AlertRule.builder()
            .name("critical-rule")
            .displayName("严重规则")
            .type("THRESHOLD")
            .metricName("metric3")
            .conditionOperator(">")
            .thresholdValue(100.0)
            .severity("CRITICAL")
            .enabled(true)
            .coolDownMinutes(15)
            .build();
        alertRuleRepository.save(criticalRule);

        // When
        List<AlertRule> warningRules = alertRuleRepository.findBySeverity("WARNING");
        List<AlertRule> criticalRules = alertRuleRepository.findBySeverity("CRITICAL");

        // Then
        assertThat(warningRules).hasSize(1);
        assertThat(warningRules.get(0).getSeverity()).isEqualTo("WARNING");
        assertThat(criticalRules).hasSize(1);
        assertThat(criticalRules.get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("检查规则名称是否存在 - 存在")
    void testExistsByNameTrue() {
        // When
        boolean exists = alertRuleRepository.existsByName("test-rule");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查规则名称是否存在 - 不存在")
    void testExistsByNameFalse() {
        // When
        boolean exists = alertRuleRepository.existsByName("non-existent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("更新告警规则 - 成功")
    void testUpdateAlertRule() {
        // Given
        testRule.setDisplayName("更新后的规则");
        testRule.setThresholdValue(20.0);

        // When
        AlertRule updatedRule = alertRuleRepository.save(testRule);

        // Then
        assertThat(updatedRule.getDisplayName()).isEqualTo("更新后的规则");
        assertThat(updatedRule.getThresholdValue()).isEqualTo(20.0);
        assertThat(updatedRule.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("删除告警规则 - 成功")
    void testDeleteAlertRule() {
        // When
        alertRuleRepository.deleteById(testRule.getId());

        // Then
        Optional<AlertRule> deletedRule = alertRuleRepository.findById(testRule.getId());
        assertThat(deletedRule).isEmpty();
    }

    @Test
    @DisplayName("规则名称唯一性约束 - 违反约束时抛出异常")
    void testUniqueNameConstraint() {
        // Given
        AlertRule duplicateRule = AlertRule.builder()
            .name("test-rule") // 重复的名称
            .displayName("重复规则")
            .type("THRESHOLD")
            .metricName("metric4")
            .conditionOperator("<")
            .thresholdValue(5.0)
            .severity("INFO")
            .enabled(true)
            .coolDownMinutes(30)
            .build();

        // When & Then
        assertThatThrownBy(() -> alertRuleRepository.save(duplicateRule))
            .isInstanceOf(Exception.class); // 实际可能是 DataIntegrityViolationException
    }

    @Test
    @DisplayName("默认值测试 - enabled 默认为 true")
    void testDefaultValueEnabled() {
        // Given
        AlertRule ruleWithoutEnabled = AlertRule.builder()
            .name("default-enabled-rule")
            .displayName("默认启用规则")
            .type("THRESHOLD")
            .metricName("metric5")
            .conditionOperator(">")
            .thresholdValue(10.0)
            .severity("WARNING")
            .build();

        // When
        AlertRule savedRule = alertRuleRepository.save(ruleWithoutEnabled);

        // Then
        assertThat(savedRule.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("默认值测试 - coolDownMinutes 默认为 30")
    void testDefaultValueCoolDownMinutes() {
        // Given
        AlertRule ruleWithoutCoolDown = AlertRule.builder()
            .name("default-cooldown-rule")
            .displayName("默认冷却期规则")
            .type("THRESHOLD")
            .metricName("metric6")
            .conditionOperator(">")
            .thresholdValue(10.0)
            .severity("WARNING")
            .enabled(true)
            .build();

        // When
        AlertRule savedRule = alertRuleRepository.save(ruleWithoutCoolDown);

        // Then
        assertThat(savedRule.getCoolDownMinutes()).isEqualTo(30);
    }
}
