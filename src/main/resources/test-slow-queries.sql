-- ========================================
-- DB-Doctor 慢查询测试语句集合
-- ========================================
-- 使用说明：
-- 1. 确保已执行 test-db-setup.sql 初始化数据库
-- 2. 启动 DB-Doctor
-- 3. 在 MySQL 中执行以下测试语句
-- 4. 观察慢查询是否被捕获和处理
-- 5. 慢查询阈值：0.5 秒
-- ========================================

USE test_db;

-- ========================================
-- 场景 1：全表扫描（最常见）
-- ========================================

-- 测试 1.1：无 WHERE 条件的全表扫描
-- 预期：扫描 10000 行，耗时 > 0.5s
SELECT * FROM users;

-- 测试 1.2：无 WHERE 条件的订单全表扫描
-- 预期：扫描 50000 行，耗时 > 0.5s
SELECT * FROM orders;

-- 测试 1.3：大表 JOIN 全表扫描
-- 预期：扫描多张大表，耗时 > 0.5s
SELECT * FROM orders o
JOIN order_items oi ON o.id = oi.order_id
JOIN users u ON o.user_id = u.id;

-- ========================================
-- 场景 2：模糊查询（LIKE %...%）
-- ========================================

-- 测试 2.1：前缀模糊查询（可以使用索引）
-- 预期：可能较快，但如果索引不佳仍可能慢
SELECT * FROM users WHERE username LIKE 'user_1%';

-- 测试 2.2：中间模糊查询（无法使用索引）
-- 预期：全表扫描，耗时 > 0.5s
SELECT * FROM users WHERE username LIKE '%1234%';

-- 测试 2.3：后缀模糊查询（无法使用索引）
-- 预期：全表扫描，耗时 > 0.5s
SELECT * FROM users WHERE email LIKE '%@example.com';

-- 测试 2.4：多字段模糊查询
-- 预期：多个 OR 条件，耗时 > 0.5s
SELECT * FROM products
WHERE product_name LIKE '%手机%'
   OR description LIKE '%手机%'
   OR brand LIKE '%手机%';

-- ========================================
-- 场景 3：深度分页
-- ========================================

-- 测试 3.1：深度分页（偏移量大）
-- 预期：需要扫描大量数据，耗时 > 0.5s
SELECT * FROM orders ORDER BY id LIMIT 10000, 20;

-- 测试 3.2：超深度分页
-- 预期：扫描 40000 行后返回 20 条，耗时 > 0.5s
SELECT * FROM orders ORDER BY id LIMIT 40000, 20;

-- 测试 3.3：带排序的深度分页
-- 预期：排序 + 深度分页，耗时 > 0.5s
SELECT * FROM user_behavior_logs
ORDER BY created_at DESC
LIMIT 80000, 20;

-- ========================================
-- 场景 4：复杂 JOIN 查询
-- ========================================

-- 测试 4.1：三表 JOIN
-- 预期：连接多张表，耗时 > 0.5s
SELECT
    u.username,
    u.email,
    o.order_no,
    o.total_amount,
    p.product_name,
    oi.quantity
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- 测试 4.2：多表 JOIN + 聚合
-- 预期：复杂 JOIN + GROUP BY，耗时 > 0.5s
SELECT
    u.username,
    COUNT(o.id) AS order_count,
    SUM(o.actual_amount) AS total_spent,
    AVG(o.actual_amount) AS avg_order_amount
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.username
HAVING COUNT(o.id) > 3
ORDER BY total_spent DESC;

-- 测试 4.3：自 JOIN + 分组统计
-- 预期：复杂的自连接查询，耗时 > 0.5s
SELECT
    u1.username,
    COUNT(*) AS mutual_connections
FROM users u1
JOIN user_behavior_logs ubl1 ON u1.id = ubl1.user_id
JOIN user_behavior_logs ubl2 ON ubl1.resource_type = ubl2.resource_type
                          AND ubl1.resource_id = ubl2.resource_id
JOIN users u2 ON ubl2.user_id = u2.id
WHERE u1.id < u2.id
GROUP BY u1.id, u1.username
HAVING COUNT(*) > 5;

-- ========================================
-- 场景 5：子查询
-- ========================================

-- 测试 5.1：IN 子查询
-- 预期：子查询返回大量数据，耗时 > 0.5s
SELECT * FROM users
WHERE id IN (
    SELECT user_id FROM orders
    WHERE actual_amount > 1000
);

-- 测试 5.2：NOT IN 子查询
-- 预期：扫描全表排除子查询结果，耗时 > 0.5s
SELECT * FROM users
WHERE id NOT IN (
    SELECT DISTINCT user_id FROM orders
);

-- 测试 5.3：EXISTS 子查询
-- 预期：相关子查询，每行都执行一次，耗时 > 0.5s
SELECT * FROM users u
WHERE EXISTS (
    SELECT 1 FROM orders o
    WHERE o.user_id = u.id
    AND o.actual_amount > 2000
);

-- 测试 5.4：多层嵌套子查询
-- 预期：复杂的多层嵌套，耗时 > 0.5s
SELECT * FROM products
WHERE category_id IN (
    SELECT id FROM categories
    WHERE parent_id IN (
        SELECT id FROM categories
        WHERE parent_id = 0
    )
);

-- ========================================
-- 场景 6：聚合查询（GROUP BY + 聚合函数）
-- ========================================

-- 测试 6.1：简单聚合
-- 预期：扫描全表分组，耗时 > 0.5s
SELECT
    order_status,
    COUNT(*) AS count,
    SUM(actual_amount) AS total_amount,
    AVG(actual_amount) AS avg_amount
FROM orders
GROUP BY order_status;

-- 测试 6.2：多字段聚合
-- 预期：多个分组字段，耗时 > 0.5s
SELECT
    payment_method,
    payment_status,
    order_status,
    COUNT(*) AS count,
    SUM(actual_amount) AS total_amount
FROM orders
GROUP BY payment_method, payment_status, order_status;

-- 测试 6.3：带 HAVING 的聚合
-- 预期：分组后过滤，耗时 > 0.5s
SELECT
    user_id,
    COUNT(*) AS order_count,
    SUM(actual_amount) AS total_amount
FROM orders
GROUP BY user_id
HAVING SUM(actual_amount) > 5000
ORDER BY total_amount DESC;

-- 测试 6.4：时间范围聚合
-- 预期：时间函数 + 聚合，耗时 > 0.5s
SELECT
    DATE(created_at) AS order_date,
    COUNT(*) AS order_count,
    SUM(actual_amount) AS total_amount
FROM orders
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 90 DAY)
GROUP BY DATE(created_at)
ORDER BY order_date DESC;

-- ========================================
-- 场景 7：排序查询
-- ========================================

-- 测试 7.1：单字段排序
-- 预期：全表排序，耗时 > 0.5s
SELECT * FROM user_behavior_logs
ORDER BY created_at DESC;

-- 测试 7.2：多字段排序
-- 预期：多字段排序，耗时 > 0.5s
SELECT * FROM orders
ORDER BY payment_status, created_at DESC, actual_amount DESC;

-- 测试 7.3：大结果集排序 + LIMIT
-- 预期：大结果集排序后截取，耗时 > 0.5s
SELECT * FROM user_behavior_logs
ORDER BY created_at DESC
LIMIT 100;

-- ========================================
-- 场景 8：无索引字段查询
-- ========================================

-- 测试 8.1：查询非索引字段
-- 预期：全表扫描，耗时 > 0.5s
SELECT * FROM users WHERE nickname = '用户1234';

-- 测试 8.2：JSON 字段查询
-- 预期：JSON 解析 + 全表扫描，耗时 > 0.5s
SELECT * FROM users
WHERE JSON_EXTRACT(preferences, '$.theme') = 'dark';

-- 测试 8.3：函数包裹的索引字段（无法使用索引）
-- 预期：全表扫描，耗时 > 0.5s
SELECT * FROM users
WHERE YEAR(created_at) = 2024;

-- ========================================
-- 场景 9：DISTINCT 去重查询
-- ========================================

-- 测试 9.1：单字段 DISTINCT
-- 预期：全表扫描去重，耗时 > 0.5s
SELECT DISTINCT user_id FROM orders;

-- 测试 9.2：多字段 DISTINCT
-- 预期：多字段去重，耗时 > 0.5s
SELECT DISTINCT user_id, order_status FROM orders;

-- 测试 9.3：DISTINCT + JOIN
-- 预期：多表连接后去重，耗时 > 0.5s
SELECT DISTINCT
    u.username,
    o.order_status
FROM users u
JOIN orders o ON u.id = o.user_id;

-- ========================================
-- 场景 10：UNION 查询
-- ========================================

-- 测试 10.1：UNION 合并多个查询
-- 预期：执行多个查询 + 去重，耗时 > 0.5s
SELECT username FROM users
WHERE email LIKE '%@qq.com'
UNION
SELECT username FROM users
WHERE email LIKE '%@gmail.com';

-- 测试 10.2：UNION ALL（不去重，更快）
-- 预期：合并查询，耗时 > 0.5s
SELECT username FROM users
WHERE age BETWEEN 20 AND 30
UNION ALL
SELECT username FROM users
WHERE age BETWEEN 31 AND 40;

-- ========================================
-- 场景 11：COUNT(*) 大表统计
-- ========================================

-- 测试 11.1：简单 COUNT
-- 预期：全表扫描统计，耗时 > 0.5s
SELECT COUNT(*) FROM user_behavior_logs;

-- 测试 11.2：COUNT(DISTINCT)
-- 预期：全表扫描去重统计，耗时 > 0.5s
SELECT COUNT(DISTINCT user_id) FROM user_behavior_logs;

-- 测试 11.3：多表 COUNT JOIN
-- 预期：连接后统计，耗时 > 0.5s
SELECT COUNT(DISTINCT o.user_id)
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE oi.product_id IN (1, 2, 3, 4, 5);

-- ========================================
-- 场景 12：批量数据查询
-- ========================================

-- 测试 12.1：IN 大量值
-- 预期：IN 列表很长，耗时 > 0.5s
SELECT * FROM products
WHERE id IN (
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
    11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
    21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
    31, 32, 33, 34, 35, 36, 37, 38, 39, 40
);

-- 测试 12.2：OR 多个条件
-- 预期：多个 OR 条件，耗时 > 0.5s
SELECT * FROM users
WHERE username = 'user_1'
   OR username = 'user_2'
   OR username = 'user_3'
   OR username = 'user_4'
   OR username = 'user_5';

-- ========================================
-- 场景 13：时间范围查询
-- ========================================

-- 测试 13.1：大时间范围查询
-- 预期：扫描大量历史数据，耗时 > 0.5s
SELECT * FROM orders
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 365 DAY);

-- 测试 13.2：时间范围 + 其他条件
-- 预期：复合条件查询，耗时 > 0.5s
SELECT * FROM user_behavior_logs
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 60 DAY)
  AND action_type = 'purchase'
ORDER BY created_at DESC;

-- ========================================
-- 场景 14：正则表达式查询
-- ========================================

-- 测试 14.1：REGEXP 查询
-- 预期：正则匹配，耗时 > 0.5s
SELECT * FROM users
WHERE username REGEXP 'user_[0-9]{4}';

-- 测试 14.2：NOT REGEXP 查询
-- 预期：正则不匹配，耗时 > 0.5s
SELECT * FROM products
WHERE product_name NOT REGEXP '[0-9]';

-- ========================================
-- 场景 15：跨库关联查询（如果配置了多库）
-- ========================================

-- 测试 15.1：如果有多个数据库，模拟跨库查询
-- 预期：跨库查询，耗时 > 0.5s
-- SELECT * FROM test_db.users u
-- JOIN information_schema.columns c ON u.username = c.column_name;

-- ========================================
-- 场景 16：全文搜索（如果有全文索引）
-- ========================================

-- 测试 16.1：TEXT 字段搜索
-- 预期：全文搜索，耗时 > 0.5s
SELECT * FROM products
WHERE description LIKE '%高性能%'
   OR description LIKE '%优惠%'
   OR description LIKE '%特价%';

-- ========================================
-- 场景 17：CASE WHEN 复杂逻辑
-- ========================================

-- 测试 17.1：复杂 CASE WHEN
-- 预期：复杂逻辑判断，耗时 > 0.5s
SELECT
    username,
    email,
    CASE
        WHEN age < 18 THEN '未成年'
        WHEN age BETWEEN 18 AND 35 THEN '青年'
        WHEN age BETWEEN 36 AND 60 THEN '中年'
        ELSE '老年'
    END AS age_group,
    CASE
        WHEN points < 100 THEN '普通用户'
        WHEN points BETWEEN 100 AND 1000 THEN '铜牌用户'
        WHEN points BETWEEN 1001 AND 5000 THEN '银牌用户'
        ELSE '金牌用户'
    END AS user_level
FROM users
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);

-- ========================================
-- 场景 18：临时表模拟（如果支持）
-- ========================================

-- 测试 18.1：使用子查询作为临时表
-- 预期：子查询物化，耗时 > 0.5s
SELECT * FROM (
    SELECT
        user_id,
        COUNT(*) AS order_count,
        SUM(actual_amount) AS total_amount
    FROM orders
    GROUP BY user_id
    HAVING COUNT(*) > 2
) AS user_stats
JOIN users u ON user_stats.user_id = u.id;

-- ========================================
-- 场景 19：索引失效场景
-- ========================================

-- 测试 19.1：对索引字段使用函数
-- 预期：索引失效，全表扫描，耗时 > 0.5s
SELECT * FROM users
WHERE LOWER(username) = 'user_1000';

-- 测试 19.2：索引字段参与运算
-- 预期：索引失效，全表扫描，耗时 > 0.5s
SELECT * FROM users
WHERE id + 1 = 1001;

-- 测试 19.3：OR 连接不同字段
-- 预期：索引可能失效，耗时 > 0.5s
SELECT * FROM orders
WHERE user_id = 100 OR order_status = 'pending';

-- ========================================
-- 场景 20：数据统计报表查询
-- ========================================

-- 测试 20.1：复杂报表查询
-- 预期：多表连接 + 聚合 + 排序，耗时 > 0.5s
SELECT
    u.username,
    u.email,
    COUNT(DISTINCT o.id) AS total_orders,
    SUM(o.actual_amount) AS total_spent,
    AVG(o.actual_amount) AS avg_order_value,
    MIN(o.actual_amount) AS min_order_value,
    MAX(o.actual_amount) AS max_order_value,
    COUNT(DISTINCT oi.product_id) AS total_products_purchased
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE u.created_at >= DATE_SUB(NOW(), INTERVAL 180 DAY)
  AND o.created_at >= DATE_SUB(NOW(), INTERVAL 90 DAY)
GROUP BY u.id, u.username, u.email
HAVING COUNT(DISTINCT o.id) > 1
ORDER BY total_spent DESC
LIMIT 100;

-- ========================================
-- 测试完成提示
-- ========================================
SELECT '测试场景集合执行完成！' AS message;
SELECT '请查看 DB-Doctor 日志，观察慢查询捕获情况。' AS next_step;
