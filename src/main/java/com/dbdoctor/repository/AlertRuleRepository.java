package com.dbdoctor.repository;

import com.dbdoctor.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 告警规则 Repository
 *
 * @author DB-Doctor
 * @version 3.2.0
 * @since 3.2.0
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * 根据规则名称查询
     *
     * @param name 规则名称
     * @return 规则对象（可能为空）
     */
    Optional<AlertRule> findByName(String name);

    /**
     * 根据启用状态查询规则列表
     *
     * @param enabled 是否启用
     * @return 规则列表
     */
    List<AlertRule> findByEnabled(Boolean enabled);

    /**
     * 根据规则类型查询
     *
     * @param type 规则类型
     * @return 规则列表
     */
    List<AlertRule> findByType(String type);

    /**
     * 根据严重程度查询
     *
     * @param severity 严重程度
     * @return 规则列表
     */
    List<AlertRule> findBySeverity(String severity);

    /**
     * 检查规则名称是否存在
     *
     * @param name 规则名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
