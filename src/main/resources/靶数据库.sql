-- 1. 创建并进入你的测试数据库
CREATE DATABASE IF NOT EXISTS test;
USE test;
SET GLOBAL slow_query_log = 'ON';

SET GLOBAL log_output = 'TABLE';

-- 2. 使用从 MySQL 官方复制的结构，创建一张完全仿真的 slow_log 表
--    (注意：我们保留了 ENGINE=CSV 和 utf8mb3，以确保 100% 模拟真实环境)
CREATE TABLE  IF NOT EXISTS `slow_log` (
                            `start_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            `user_host` mediumtext NOT NULL,
                            `query_time` time(6) NOT NULL,
                            `lock_time` time(6) NOT NULL,
                            `rows_sent` int NOT NULL,
                            `rows_examined` int NOT NULL,
                            `db` varchar(512) NOT NULL,
                            `last_insert_id` int NOT NULL,
                            `insert_id` int NOT NULL,
                            `server_id` int unsigned NOT NULL,
                            `sql_text` mediumblob NOT NULL,
                            `thread_id` bigint unsigned NOT NULL
) ENGINE=CSV DEFAULT CHARSET=utf8mb3 COMMENT='Slow log';

-- 确保你在 test 数据库中
USE test;

-- 一次性插入多条模拟慢查询记录
INSERT INTO `slow_log`
(`start_time`, `user_host`, `query_time`, `lock_time`, `rows_sent`, `rows_examined`, `db`, `last_insert_id`, `insert_id`, `server_id`, `sql_text`, `thread_id`)
VALUES
-- 记录1：一个简单的 sleep，模拟 I/O 等待
(NOW(6), 'root[root] @ localhost [::1]', '00:00:02.006900', '00:00:00.000000', 1, 1, 'workflow', 0, 0, 1, 0x53454C45435420534C454550283229, 18),

-- 记录2：全表扫描一个大表，模拟没有索引
(NOW(6), 'root[root] @ localhost [::1]', '00:00:01.954009', '00:00:00.000003', 6615, 6615, 'workflow', 0, 0, 1, 0x73654C454354202A2066726F6D2020776F726B666C6F775F6A736F6E, 18),

-- 记录3：另一个 sleep，但来自不同的线程
(NOW(6), 'root[root] @ localhost [::1]', '00:00:02.007250', '00:00:00.000000', 1, 1, 'workflow', 0, 0, 1, 0x53454C45435420736C656570283229, 319),

-- 记录4：同样是全表扫描，但有微小的锁等待
(NOW(6), 'root[root] @ localhost [::1]', '00:00:01.690235', '00:00:00.000015', 6615, 6615, 'workflow', 0, 0, 1, 0x53454C454354202A2046524F4D2060776F726B666C6F775F6A736F6E60, 33),

-- 记录5：高耗时、高扫描行数，模拟典型的“坏SQL”
(NOW(6), 'root[root] @ localhost [::1]', '00:00:05.123456', '00:00:00.000005', 100, 500000, 'production_db', 0, 0, 1, 0x53454C454354202A2046524F4D207573657273206A6F696E206F7264657273204F4E2075736572732E6964203D206F72646572732E757365725F6964, 55),

-- 记录6：【新增】包含手机号的查询，用于测试脱敏
(NOW(6), 'admin[admin] @ 192.168.1.5 []', '00:00:03.543210', '00:00:00.000010', 1, 120000, 'crm_db', 0, 0, 1, 0x53454C454354202A2046524F4D20637573746F6D6572732057484552452070686F6E65203D2027313338313233343536373827, 77);



