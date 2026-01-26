-- ========================================
-- DB-Doctor 性能测试数据库初始化脚本
-- ========================================
-- 功能：
-- 1. 创建 test_db 数据库
-- 2. 创建 7 张表，包含各种字段类型
-- 3. 插入万级测试数据
-- 4. 配置慢查询阈值和慢查询日志
-- 5. 提供各种慢查询测试语句
-- ========================================
-- 使用方法：
--   mysql -u root -p < test-db-setup.sql
--   或在 MySQL 客户端中直接执行
-- ========================================

-- ========================================
-- 第1部分：数据库创建和配置
-- ========================================

-- 删除旧数据库（如果存在）
DROP DATABASE IF EXISTS test_db;

-- 创建新数据库
CREATE DATABASE test_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE test_db;

-- ========================================
-- 慢查询配置
-- ========================================
-- 注意：如果已启用 DB-Doctor 的慢查询自动配置功能，
--       以下语句可以注释掉，避免重复配置
-- ========================================

-- 启用慢查询日志
SET GLOBAL slow_query_log = 1;

-- 设置慢查询时间阈值（秒）
-- 建议根据实际测试需求调整，测试时可以设置为 0.5 秒
SET GLOBAL long_query_time = 0.5;

-- 可选：将慢查询日志输出到表（mysql.slow_log）
-- 如果想从表中查询慢查询，可以使用 TABLE 模式
-- SET GLOBAL log_output = 'TABLE';

-- ========================================
-- 第2部分：创建测试表（7张表，覆盖各种字段类型）
-- ========================================

-- 表1：用户表（users）
-- 字段类型：INT, VARCHAR, DATE, ENUM, BOOLEAN, JSON
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    nickname VARCHAR(50) COMMENT '昵称',
    phone VARCHAR(20) COMMENT '手机号',
    gender ENUM('male', 'female', 'other') DEFAULT 'other' COMMENT '性别',
    age INT COMMENT '年龄',
    birthday DATE COMMENT '生日',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    status ENUM('active', 'inactive', 'banned') DEFAULT 'active' COMMENT '账号状态',
    is_vip BOOLEAN DEFAULT FALSE COMMENT '是否VIP',
    vip_level TINYINT DEFAULT 0 COMMENT 'VIP等级',
    balance DECIMAL(10, 2) DEFAULT 0.00 COMMENT '账户余额',
    points INT DEFAULT 0 COMMENT '积分',
    register_source VARCHAR(50) COMMENT '注册来源',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    remark TEXT COMMENT '备注',
    preferences JSON COMMENT '用户偏好设置',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 表2：商品表（products）
-- 字段类型：INT, DECIMAL, VARCHAR, TEXT, ENUM
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_code VARCHAR(50) UNIQUE NOT NULL COMMENT '商品编码',
    category_id INT NOT NULL COMMENT '分类ID',
    brand VARCHAR(100) COMMENT '品牌',
    price DECIMAL(10, 2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10, 2) COMMENT '原价',
    cost_price DECIMAL(10, 2) COMMENT '成本价',
    stock INT DEFAULT 0 COMMENT '库存',
    sales INT DEFAULT 0 COMMENT '销量',
    weight DECIMAL(10, 2) COMMENT '重量（克）',
    description TEXT COMMENT '商品描述',
    specifications JSON COMMENT '规格参数',
    images JSON COMMENT '商品图片（JSON数组）',
    tags VARCHAR(500) COMMENT '标签（逗号分隔）',
    status ENUM('on_sale', 'out_of_stock', 'discontinued') DEFAULT 'on_sale' COMMENT '商品状态',
    is_hot BOOLEAN DEFAULT FALSE COMMENT '是否热门',
    is_new BOOLEAN DEFAULT FALSE COMMENT '是否新品',
    is_recommend BOOLEAN DEFAULT FALSE COMMENT '是否推荐',
    supplier_id INT COMMENT '供应商ID',
    shelf_time DATETIME COMMENT '上架时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product_name (product_name),
    INDEX idx_category_id (category_id),
    INDEX idx_price (price),
    INDEX idx_status (status),
    INDEX idx_sales (sales)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 表3：订单表（orders）
-- 字段类型：BIGINT, VARCHAR, DECIMAL, ENUM, DATETIME
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(50) UNIQUE NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    discount_amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠金额',
    actual_amount DECIMAL(10, 2) NOT NULL COMMENT '实际金额',
    payment_method ENUM('alipay', 'wechat', 'credit_card', 'bank_transfer', 'cash') COMMENT '支付方式',
    payment_status ENUM('unpaid', 'paid', 'refunded', 'partial_refunded') DEFAULT 'unpaid' COMMENT '支付状态',
    payment_time DATETIME COMMENT '支付时间',
    order_status ENUM('pending', 'confirmed', 'shipped', 'delivered', 'completed', 'cancelled') DEFAULT 'pending' COMMENT '订单状态',
    consignee VARCHAR(50) COMMENT '收货人',
    phone VARCHAR(20) COMMENT '收货电话',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    district VARCHAR(50) COMMENT '区/县',
    address VARCHAR(500) COMMENT '详细地址',
    postal_code VARCHAR(20) COMMENT '邮编',
    shipping_fee DECIMAL(10, 2) DEFAULT 0.00 COMMENT '运费',
    tracking_number VARCHAR(100) COMMENT '物流单号',
    shipping_company VARCHAR(100) COMMENT '物流公司',
    shipped_time DATETIME COMMENT '发货时间',
    delivered_time DATETIME COMMENT '送达时间',
    note TEXT COMMENT '订单备注',
    user_note VARCHAR(500) COMMENT '用户留言',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_payment_status (payment_status),
    INDEX idx_order_status (order_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 表4：订单详情表（order_items）
-- 字段类型：BIGINT, INT, DECIMAL
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    product_code VARCHAR(50) COMMENT '商品编码',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '单价',
    discount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '优惠',
    subtotal DECIMAL(10, 2) NOT NULL COMMENT '小计',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单详情表';

-- 表5：分类表（categories）
-- 字段类型：INT, VARCHAR, BOOLEAN
CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    parent_id INT DEFAULT 0 COMMENT '父分类ID',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_path VARCHAR(500) COMMENT '分类路径',
    description VARCHAR(500) COMMENT '分类描述',
    icon_url VARCHAR(500) COMMENT '图标URL',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 表6：用户行为日志表（user_behavior_logs）
-- 字段类型：BIGINT, VARCHAR, TEXT, JSON, DATETIME
CREATE TABLE user_behavior_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    action_type VARCHAR(50) NOT NULL COMMENT '行为类型（login, view, click, purchase, search等）',
    resource_type VARCHAR(50) COMMENT '资源类型',
    resource_id BIGINT COMMENT '资源ID',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    device_type VARCHAR(50) COMMENT '设备类型',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',
    location VARCHAR(200) COMMENT '地理位置',
    referrer VARCHAR(500) COMMENT '来源页面',
    session_id VARCHAR(100) COMMENT '会话ID',
    duration INT COMMENT '停留时长（秒）',
    metadata JSON COMMENT '元数据（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_action_type (action_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为日志表';

-- 表7：支付记录表（payments）
-- 字段类型：BIGINT, VARCHAR, DECIMAL, DATETIME
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付ID',
    payment_no VARCHAR(50) UNIQUE NOT NULL COMMENT '支付流水号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    payment_method ENUM('alipay', 'wechat', 'credit_card', 'bank_transfer', 'cash') NOT NULL COMMENT '支付方式',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    status ENUM('pending', 'success', 'failed', 'cancelled') DEFAULT 'pending' COMMENT '支付状态',
    transaction_id VARCHAR(100) COMMENT '第三方交易ID',
    paid_at DATETIME COMMENT '支付时间',
    failed_reason VARCHAR(500) COMMENT '失败原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_payment_no (payment_no),
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ========================================
-- 第3部分：插入测试数据（万级）
-- ========================================

-- 插入分类数据（50个分类）
INSERT INTO categories (parent_id, category_name, description, sort_order) VALUES
(0, '电子产品', '各类电子产品', 1),
(0, '服装鞋帽', '服装鞋帽类商品', 2),
(0, '家居用品', '家居生活用品', 3),
(0, '食品饮料', '食品饮料类', 4),
(0, '图书文具', '图书和办公用品', 5);

-- 插入子分类
INSERT INTO categories (parent_id, category_name, description, sort_order)
SELECT
    id,
    CONCAT(category_name, ' - 子类', n),
    CONCAT(category_name, '的第', n, '个子分类'),
    n * 10
FROM categories, (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) nums
WHERE parent_id = 0;

-- 插入用户数据（10000条）
INSERT INTO users (username, email, password, nickname, phone, gender, age, birthday, status, is_vip, vip_level, balance, points, register_source, preferences)
SELECT
    CONCAT('user_', LPAD(n, 5, '0')),
    CONCAT('user_', LPAD(n, 5, '0'), '@example.com'),
    '$2a$10$encrypted_password_placeholder',
    CONCAT('用户', n),
    CONCAT('138', LPAD(n, 8, '0')),
    ELT(1 + (n % 3), 'male', 'female', 'other'),
    18 + (n % 50),
    DATE_SUB(CURDATE(), INTERVAL (18 + n % 50) YEAR),
    ELT(1 + (n % 3), 'active', 'inactive', 'banned'),
    n % 10 = 0,
    (n % 5),
    ROUND(RAND() * 10000, 2),
    FLOOR(RAND() * 10000),
    ELT(1 + (n % 5), 'web', 'ios', 'android', 'wechat', 'app'),
    JSON_OBJECT('theme', ELT(1 + (n % 3), 'light', 'dark', 'auto'), 'language', 'zh-CN')
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + 1 AS n
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d
    WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 < 10000
) nums;

-- 插入商品数据（5000条）
INSERT INTO products (product_name, product_code, category_id, brand, price, original_price, cost_price, stock, sales, weight, status, is_hot, is_new, is_recommend)
SELECT
    CONCAT('商品', n, ' - ', ELT(1 + (n % 20), '手机', '电脑', '耳机', '键盘', '鼠标', '显示器', '摄像头', '音箱', '充电器', '数据线', '保护壳', '贴膜', '支架', '清洁套装', '收纳盒', '背包', '腕带', '转换器', '扩展坞', '其他')),
    CONCAT('PROD', LPAD(n, 6, '0')),
    1 + (n % 50),
    ELT(1 + (n % 15), 'Apple', 'Samsung', 'Huawei', 'Xiaomi', 'Sony', 'LG', 'Dell', 'HP', 'Lenovo', 'Asus', 'Acer', 'MSI', 'Razer', 'Logitech', 'Microsoft'),
    ROUND(10 + (RAND() * 10000), 2),
    ROUND(20 + (RAND() * 12000), 2),
    ROUND(5 + (RAND() * 5000), 2),
    FLOOR(RAND() * 10000),
    FLOOR(RAND() * 5000),
    ROUND(RAND() * 5000, 2),
    ELT(1 + (n % 3), 'on_sale', 'out_of_stock', 'discontinued'),
    n % 10 = 0,
    n % 20 = 0,
    n % 15 = 0
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + 1 AS n
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d
    WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 < 5000
) nums;

-- 插入订单数据（50000条）
INSERT INTO orders (order_no, user_id, total_amount, discount_amount, actual_amount, payment_method, payment_status, order_status, consignee, phone, province, city, district, address, shipping_fee)
SELECT
    CONCAT('ORD', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(n, 8, '0')),
    1 + (n % 10000),
    ROUND(50 + (RAND() * 5000), 2),
    ROUND(RAND() * 100, 2),
    ROUND(50 + (RAND() * 5000) - (RAND() * 100), 2),
    ELT(1 + (n % 5), 'alipay', 'wechat', 'credit_card', 'bank_transfer', 'cash'),
    ELT(1 + (n % 4), 'unpaid', 'paid', 'refunded', 'partial_refunded'),
    ELT(1 + (n % 6), 'pending', 'confirmed', 'shipped', 'delivered', 'completed', 'cancelled'),
    CONCAT('收货人', n),
    CONCAT('18', LPAD(n, 9, '0')),
    ELT(1 + (n % 34), '北京市', '上海市', '广州市', '深圳市', '杭州市', '南京市', '苏州市', '成都市', '重庆市', '武汉市', '西安市', '郑州市', '沈阳市', '青岛市', '大连市', '厦门市', '长沙市', '哈尔滨市', '济南市', '昆明市', '天津市', '南宁市', '宁波市', '合肥市', '福州市', '贵阳市', '太原市', '长春市', '南昌市', '石家庄市', '呼和浩特市', '兰州市', '银川市', '西宁市', '乌鲁木齐市'),
    ELT(1 + (n % 20), '朝阳区', '海淀区', '浦东新区', '静安区', '黄浦区', '徐汇区', '长宁区', '普陀区', '虹口区', '杨浦区', '宝山区', '嘉定区', '闵行区', '松江区', '青浦区', '奉贤区', '金山区', '崇明区', '徐汇区'),
    CONCAT('第', (n % 100 + 1), '街道'),
    CONCAT('详细地址', n, '号'),
    ROUND(RAND() * 20, 2)
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + 1 AS n
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) d
    WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 < 50000
) nums;

-- 插入订单详情数据（150000条，平均每个订单3个商品）
-- 使用 JOIN 替代子查询，确保所有字段引用同一个产品记录
INSERT INTO order_items (order_id, product_id, product_name, product_code, quantity, unit_price, discount, subtotal)
SELECT
    o.id AS order_id,
    p.id AS product_id,
    p.product_name,
    p.product_code,
    (1 + FLOOR(RAND() * 5)) AS quantity,
    p.price AS unit_price,
    ROUND(RAND() * 50, 2) AS discount,
    ((1 + FLOOR(RAND() * 5)) * p.price - ROUND(RAND() * 50, 2)) AS subtotal
FROM orders o
CROSS JOIN (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3) item_nums
CROSS JOIN (SELECT 1 + FLOOR(RAND() * 5000) AS random_product_id) rp
JOIN products p ON p.id = rp.random_product_id
WHERE o.id <= 50000
LIMIT 150000;

-- 插入用户行为日志（100000条）
INSERT INTO user_behavior_logs (user_id, action_type, resource_type, resource_id, ip_address, device_type, browser, os, duration, metadata)
SELECT
    1 + (n % 10000),
    ELT(1 + (n % 8), 'login', 'view', 'click', 'search', 'add_to_cart', 'purchase', 'favorite', 'share'),
    ELT(1 + (n % 5), 'product', 'category', 'order', 'user', 'article'),
    FLOOR(RAND() * 5000),
    CONCAT('192.168.', 1 + (n % 255), '.', 1 + (n % 255)),
    ELT(1 + (n % 4), 'desktop', 'mobile', 'tablet', 'unknown'),
    ELT(1 + (n % 5), 'Chrome', 'Firefox', 'Safari', 'Edge', 'Opera'),
    ELT(1 + (n % 4), 'Windows', 'macOS', 'Linux', 'Android'),
    FLOOR(RAND() * 3600),
    JSON_OBJECT('page', ELT(1 + (n % 10), 'home', 'product_list', 'product_detail', 'cart', 'order_confirm', 'user_center', 'search_result', 'category', 'brand', 'activity'), 'referrer', ELT(1 + (n % 5), 'google', 'baidu', 'direct', 'wechat', 'app'))
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 + 1 AS n
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) e
    WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 + e.N * 10000 < 100000
) nums;

-- 插入支付记录（50000条）
INSERT INTO payments (payment_no, order_id, user_id, payment_method, amount, status, transaction_id)
SELECT
    CONCAT('PAY', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(n, 8, '0')),
    n,
    (SELECT user_id FROM orders WHERE id = n LIMIT 1),
    (SELECT payment_method FROM orders WHERE id = n LIMIT 1),
    (SELECT actual_amount FROM orders WHERE id = n LIMIT 1),
    ELT(1 + (n % 4), 'pending', 'success', 'failed', 'cancelled'),
    CONCAT('TXN', LPAD(n, 12, '0'))
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + 1 AS n
    FROM
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) c,
        (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d
    WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 < 50000
) nums;

-- ========================================
-- 第4部分：数据统计确认
-- ========================================

SELECT '数据库初始化完成！' AS message;
SELECT '表数据统计：' AS info;

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'user_behavior_logs', COUNT(*) FROM user_behavior_logs
UNION ALL
SELECT 'payments', COUNT(*) FROM payments;

SELECT '慢查询阈值已设置为 0.5 秒' AS config_info;

-- ========================================
-- 完成
-- ========================================
