-- ========================================
-- DB-Doctor 手动测试 SQL 脚本
-- ========================================
-- 使用说明：
-- 1. 启动 DB-Doctor：java -jar target/db-doctor-1.0.0.jar
-- 2. 在 MySQL 中执行本脚本的各个测试场景
-- 3. 观察 DB-Doctor 的日志输出
-- ========================================

-- 准备工作：设置慢查询阈值
SET GLOBAL long_query_time = 1;
SET GLOBAL slow_query_log = 1;

-- ========================================
-- 场景 1：单条慢查询（基础测试）
-- ========================================
-- 目标：验证系统能否正常捕获和处理慢查询
USE test_db;

SELECT * FROM users;
-- 等待 10 秒，观察 DB-Doctor 日志

-- ========================================
-- 场景 2：重复 SQL（测试去重功能）
-- ========================================
-- 第1次执行（新 SQL）
SELECT * FROM orders WHERE user_id = 1;
-- 等待 40 秒，观察是否触发通知

-- 第2次执行（相同 SQL）
SELECT * FROM orders WHERE user_id = 1;
-- 等待 40 秒，观察是否不触发通知

-- 第3次执行（性能恶化 - 使用更慢的查询）
SELECT * FROM orders WHERE user_id = 1 FORCE INDEX (PRIMARY);
-- 等待 40 秒，观察是否触发性能恶化通知

-- ========================================
-- 场景 3：并发处理（测试线程池）
-- ========================================
-- 快速连续执行不同的慢查询
SELECT * FROM users WHERE username LIKE '%test%';

SELECT SLEEP(2);

SELECT * FROM orders WHERE amount > 1000;

SELECT SLEEP(2);

SELECT * FROM products WHERE category_id = 5;

SELECT SLEEP(2);

SELECT * FROM order_items WHERE price > 500;
-- 等待 60 秒，观察并发处理日志

-- ========================================
-- 场景 4：全表扫描（测试性能诊断）
-- ========================================
-- 无索引的模糊查询（全表扫描）
SELECT * FROM users WHERE username LIKE '%zhang%';
-- 等待 40 秒，观察报告中的全表扫描提示

-- ========================================
-- 场景 5：复杂查询（测试报告质量）
-- ========================================
-- 复杂 JOIN 查询
SELECT
    u.username,
    u.email,
    COUNT(o.id) AS order_count,
    SUM(o.amount) AS total_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.city = '北京'
GROUP BY u.id
HAVING COUNT(o.id) > 5
ORDER BY total_amount DESC;
-- 等待 40 秒，观察报告质量

-- ========================================
-- 场景 6：锁等待测试（需要两个终端）
-- ========================================
-- 终端 1：开启事务并加锁
/*
BEGIN;
SELECT * FROM users WHERE id = 1 FOR UPDATE;
-- 保持锁，不提交
*/

-- 终端 2：执行被阻塞的查询
/*
SELECT * FROM users WHERE id = 1 FOR UPDATE;
-- 这个查询会被阻塞，产生锁等待
*/

-- ========================================
-- 场景 7：批量慢查询（测试队列削峰填谷）
-- ========================================
-- 连续执行 10 条慢查询
DROP PROCEDURE IF EXISTS test_batch_slow_queries;

DELIMITER //
CREATE PROCEDURE test_batch_slow_queries()
BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < 10 DO
        SELECT * FROM users WHERE id = (1 + i);
        SET i = i + 1;
        SELECT SLEEP(1);
    END WHILE;
END //
DELIMITER ;

-- 执行批量测试
CALL test_batch_slow_queries();
-- 等待 5 分钟，观察队列处理情况

-- ========================================
-- 场景 8：高频慢查询（测试高频阈值）
-- ========================================
-- 连续执行 3 次相同查询（每次间隔 1 分钟）
SELECT * FROM users WHERE email LIKE '%admin%';
-- 等待 60 秒

SELECT * FROM users WHERE email LIKE '%admin%';
-- 等待 60 秒

SELECT * FROM users WHERE email LIKE '%admin%';
-- 等待 60 秒

-- ========================================
-- 场景 9：子查询（测试复杂SQL分析）
-- ========================================
SELECT * FROM users
WHERE id IN (
    SELECT user_id FROM orders WHERE amount > 1000
);
-- 等待 40 秒，观察子查询分析

-- ========================================
-- 场景 10：聚合查询（测试GROUP BY分析）
-- ========================================
SELECT
    city,
    COUNT(*) as user_count,
    AVG(age) as avg_age
FROM users
GROUP BY city
HAVING COUNT(*) > 5;
-- 等待 40 秒，观察聚合查询分析

-- ========================================
-- 清理工作
-- ========================================
DROP PROCEDURE IF EXISTS test_batch_slow_queries;

-- 查看慢查询日志数量
SELECT COUNT(*) as slow_query_count FROM mysql.slow_log;

-- ========================================
-- 测试完成
-- ========================================
-- 现在可以查看 H2 数据库中的测试结果
-- 连接：http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:file:./data/db-doctor-internal
-- 用户名: sa
-- 密码: (空)
-- ========================================
