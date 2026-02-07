package com.dbdoctor.service;

import com.dbdoctor.entity.AlertRule;
import com.dbdoctor.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警规则服务
 *
 * <p>提供告警规则的增删改查功能</p>
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;

    /**
     * 查询所有启用的告警规则
     *
     * @return 启用的告警规则列表
     */
    public List<AlertRule> findAllEnabled() {
        log.info("[告警规则服务] 查询所有启用的告警规则");
        return alertRuleRepository.findByEnabledTrueOrderByCreatedAtDesc();
    }

    /**
     * 查询所有告警规则（分页）
     *
     * @param pageable 分页参数
     * @return 告警规则分页结果
     */
    public Page<AlertRule> findAll(Pageable pageable) {
        log.info("[告警规则服务] 查询所有告警规则: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return alertRuleRepository.findAll(pageable);
    }

    /**
     * 根据规则名称查询
     *
     * @param name 规则名称
     * @return 告警规则
     */
    public Optional<AlertRule> findByName(String name) {
        log.info("[告警规则服务] 根据规则名称查询: name={}", name);
        return alertRuleRepository.findByName(name);
    }

    /**
     * 根据ID查询
     *
     * @param id 规则ID
     * @return 告警规则
     */
    public Optional<AlertRule> findById(Long id) {
        log.info("[告警规则服务] 根据ID查询: id={}", id);
        return alertRuleRepository.findById(id);
    }

    /**
     * 创建告警规则
     *
     * @param rule 告警规则
     * @param createdBy 创建人
     * @return 创建的告警规则
     */
    @Transactional
    public AlertRule create(AlertRule rule, String createdBy) {
        log.info("[告警规则服务] 创建告警规则: name={}, displayName={}",
                rule.getName(), rule.getDisplayName());

        // 检查规则名称是否已存在
        if (alertRuleRepository.findByName(rule.getName()).isPresent()) {
            throw new IllegalArgumentException("规则名称已存在: " + rule.getName());
        }

        rule.setCreatedBy(createdBy);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());

        return alertRuleRepository.save(rule);
    }

    /**
     * 更新告警规则
     *
     * @param id 规则ID
     * @param rule 更新的告警规则
     * @param updatedBy 更新人
     * @return 更新后的告警规则
     */
    @Transactional
    public AlertRule update(Long id, AlertRule rule, String updatedBy) {
        log.info("[告警规则服务] 更新告警规则: id={}, displayName={}",
                id, rule.getDisplayName());

        AlertRule existingRule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));

        // 如果修改了规则名称，检查新名称是否已被占用
        if (!existingRule.getName().equals(rule.getName())) {
            if (alertRuleRepository.findByName(rule.getName()).isPresent()) {
                throw new IllegalArgumentException("规则名称已存在: " + rule.getName());
            }
        }

        // 更新字段
        existingRule.setName(rule.getName());
        existingRule.setDisplayName(rule.getDisplayName());
        existingRule.setType(rule.getType());
        existingRule.setMetricName(rule.getMetricName());
        existingRule.setConditionOperator(rule.getConditionOperator());
        existingRule.setThresholdValue(rule.getThresholdValue());
        existingRule.setSeverity(rule.getSeverity());
        existingRule.setEnabled(rule.getEnabled());
        existingRule.setCoolDownMinutes(rule.getCoolDownMinutes());
        existingRule.setDescription(rule.getDescription());
        existingRule.setUpdatedBy(updatedBy);
        existingRule.setUpdatedAt(LocalDateTime.now());

        return alertRuleRepository.save(existingRule);
    }

    /**
     * 删除告警规则
     *
     * @param id 规则ID
     */
    @Transactional
    public void delete(Long id) {
        log.info("[告警规则服务] 删除告警规则: id={}", id);

        if (!alertRuleRepository.existsById(id)) {
            throw new IllegalArgumentException("告警规则不存在: " + id);
        }

        alertRuleRepository.deleteById(id);
    }

    /**
     * 启用/禁用告警规则
     *
     * @param id 规则ID
     * @param enabled 是否启用
     * @param updatedBy 更新人
     * @return 更新后的告警规则
     */
    @Transactional
    public AlertRule toggleEnabled(Long id, boolean enabled, String updatedBy) {
        log.info("[告警规则服务] 切换告警规则状态: id={}, enabled={}", id, enabled);

        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));

        rule.setEnabled(enabled);
        rule.setUpdatedBy(updatedBy);
        rule.setUpdatedAt(LocalDateTime.now());

        return alertRuleRepository.save(rule);
    }

    /**
     * 根据指标名称查询告警规则
     *
     * @param metricName 指标名称
     * @return 告警规则列表
     */
    public List<AlertRule> findByMetricName(String metricName) {
        log.info("[告警规则服务] 根据指标名称查询: metricName={}", metricName);
        return alertRuleRepository.findByMetricNameAndEnabledTrue(metricName);
    }
}
