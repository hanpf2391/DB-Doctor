package com.dbdoctor.repository;

import com.dbdoctor.model.SlowQueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 慢查询分析历史 Repository
 * 操作 H2 数据库
 *
 * @author DB-Doctor
 * @version 2.0.0
 */
@Repository
public interface SlowQueryHistoryRepository extends JpaRepository<SlowQueryHistory, Long> {

    /**
     * 根据 SQL 指纹查询（用于去重判断）
     *
     * @param sqlFingerprint SQL 指纹
     * @return 查询结果
     */
    Optional<SlowQueryHistory> findBySqlFingerprint(String sqlFingerprint);

    /**
     * 查询最常见的慢查询（按出现次数排序）
     *
     * @param limit 限制数量
     * @return 最常见的慢查询列表
     */
    List<SlowQueryHistory> findTopByOrderByOccurrenceCountDesc();

    /**
     * 查询最近新增的慢查询（按首次发现时间排序）
     *
     * @return 最近的慢查询列表
     */
    List<SlowQueryHistory> findTop10ByOrderByFirstSeenTimeDesc();

    /**
     * 查询指定状态的记录
     *
     * @param status 状态
     * @return 指定状态的记录列表
     */
    List<SlowQueryHistory> findByStatus(SlowQueryHistory.AnalysisStatus status);

    /**
     * 查询指定数据库的慢查询
     *
     * @param dbName 数据库名
     * @return 慢查询列表
     */
    List<SlowQueryHistory> findByDbName(String dbName);

    /**
     * 统计指定状态的记录数量
     *
     * @param status 状态
     * @return 记录数量
     */
    long countByStatus(SlowQueryHistory.AnalysisStatus status);
}
