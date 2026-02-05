-- 创建数据库
CREATE DATABASE enterprise_core_hr;
USE enterprise_core_hr;

-- 部门表 (department_id 非主键，无索引)
CREATE TABLE departments (
                             department_id VARCHAR(50) NOT NULL,
                             department_name VARCHAR(255) NOT NULL UNIQUE,
                             location VARCHAR(255),
                             budget DECIMAL(15, 2)
);

-- 员工表 (employee_id 非主键，无索引，manager_id, department_id 无外键，email长文本无索引)
CREATE TABLE employees (
                           employee_id VARCHAR(50) NOT NULL,
                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(100) NOT NULL,
                           email VARCHAR(255) NOT NULL UNIQUE,
                           phone_number VARCHAR(50),
                           hire_date DATE NOT NULL,
                           job_title VARCHAR(255),
                           salary DECIMAL(10, 2),
                           department_id VARCHAR(50) NOT NULL, -- 故意不设外键
                           manager_id VARCHAR(50), -- 故意不设外键
                           status VARCHAR(50) -- 'ACTIVE', 'INACTIVE', 'TERMINATED'
);

-- 职位历史表 (employee_id, department_id 无外键，无索引)
CREATE TABLE job_history (
                             history_id INT AUTO_INCREMENT PRIMARY KEY,
                             employee_id VARCHAR(50) NOT NULL, -- 故意不设外键
                             start_date DATE NOT NULL,
                             end_date DATE,
                             job_title VARCHAR(255),
                             department_id VARCHAR(50) -- 故意不设外键
);

-- 插入初始部门数据
INSERT INTO departments (department_id, department_name, location, budget) VALUES
                                                                               ('HR_001', 'Human Resources', 'Building A, Floor 3', 500000.00),
                                                                               ('ENG_002', 'Engineering', 'Building B, Floor 1', 5000000.00),
                                                                               ('SAL_003', 'Sales', 'Building C, Floor 2', 3000000.00),
                                                                               ('MKT_004', 'Marketing', 'Building C, Floor 1', 1500000.00),
                                                                               ('FIN_005', 'Finance', 'Building A, Floor 2', 800000.00);

-- 存储过程：插入大量员工数据 (50万)
DELIMITER //
CREATE PROCEDURE InsertLargeEmployees()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE dep_id VARCHAR(50);
    DECLARE mgr_id VARCHAR(50);
    DECLARE hire_dt DATE;
    DECLARE dep_ids_array TEXT;
    SET dep_ids_array = 'HR_001,ENG_002,SAL_003,MKT_004,FIN_005';

    -- 插入至少一个经理作为基准
    INSERT INTO employees (employee_id, first_name, last_name, email, phone_number, hire_date, job_title, salary, department_id, manager_id, status) VALUES
        ('EMP_000001', 'John', 'Doe', 'john.doe@example.com', '555-0001', '2010-01-01', 'CEO', 250000.00, 'FIN_005', NULL, 'ACTIVE');

    WHILE i <= 500000 DO
            SET dep_id = ELT(FLOOR(1 + (RAND() * 5)), 'HR_001', 'ENG_002', 'SAL_003', 'MKT_004', 'FIN_005');
            SET hire_dt = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365 * 15) DAY); -- 随机15年内入职
            SET mgr_id = CASE WHEN RAND() < 0.2 THEN CONCAT('EMP_', LPAD(FLOOR(1 + (RAND() * (i-1))), 6, '0')) ELSE NULL END; -- 20%的员工有经理
            IF mgr_id IS NULL THEN SET mgr_id = 'EMP_000001'; END IF; -- 确保大部分员工有个CEO做经理

            INSERT INTO employees (employee_id, first_name, last_name, email, phone_number, hire_date, job_title, salary, department_id, manager_id, status) VALUES
                (CONCAT('EMP_', LPAD(i + 1, 6, '0')),
                 CONCAT('FirstName', i),
                 CONCAT('LastName', i),
                 CONCAT('employee', i, '@company.com'),
                 CONCAT('555-', LPAD(i + 1000, 4, '0')),
                 hire_dt,
                 CONCAT('JobTitle', (i % 20) + 1),
                 ROUND(RAND() * 100000 + 30000, 2),
                 dep_id,
                 mgr_id,
                 CASE WHEN RAND() < 0.95 THEN 'ACTIVE' ELSE 'INACTIVE' END
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeEmployees();
DROP PROCEDURE InsertLargeEmployees;

-- 存储过程：插入大量职位历史数据 (100万)
DELIMITER //
CREATE PROCEDURE InsertLargeJobHistory()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE emp_id VARCHAR(50);
    DECLARE dep_id VARCHAR(50);
    DECLARE start_dt DATE;
    WHILE i <= 1000000 DO
            SET emp_id = CONCAT('EMP_', LPAD(FLOOR(1 + (RAND() * 500000)), 6, '0'));
            SET dep_id = ELT(FLOOR(1 + (RAND() * 5)), 'HR_001', 'ENG_002', 'SAL_003', 'MKT_004', 'FIN_005');
            SET start_dt = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365 * 10) DAY); -- 随机10年内
            INSERT INTO job_history (employee_id, start_date, end_date, job_title, department_id) VALUES
                (emp_id,
                 start_dt,
                 CASE WHEN RAND() < 0.7 THEN NULL ELSE DATE_ADD(start_dt, INTERVAL FLOOR(RAND() * 1000) DAY) END,
                 CONCAT('OldJobTitle', (i % 30) + 1),
                 dep_id
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeJobHistory();
DROP PROCEDURE InsertLargeJobHistory;


-- 创建数据库
CREATE DATABASE enterprise_crm_system;
USE enterprise_crm_system;

-- 客户表 (customer_id 非主键，无索引，行业字段无索引)
CREATE TABLE customers (
                           customer_id VARCHAR(50) NOT NULL,
                           customer_name VARCHAR(255) NOT NULL,
                           industry VARCHAR(100),
                           city VARCHAR(100),
                           country VARCHAR(100),
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           last_updated DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 销售机会表 (opportunity_id 非主键，无索引，customer_id, employee_id 无外键)
CREATE TABLE sales_opportunities (
                                     opportunity_id VARCHAR(50) NOT NULL,
                                     customer_id VARCHAR(50) NOT NULL, -- 故意不设外键
                                     opportunity_name VARCHAR(255) NOT NULL,
                                     stage VARCHAR(100), -- 'NEW', 'QUALIFIED', 'PROPOSAL', 'CLOSED_WON', 'CLOSED_LOST'
                                     amount DECIMAL(15, 2),
                                     close_date DATE,
                                     assigned_employee_id VARCHAR(50), -- 故意不设外键 (指向HR库的employee_id)
                                     created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 客户活动表 (activity_id 非主键，无索引，customer_id, opportunity_id, employee_id 无外键，activity_details长文本)
CREATE TABLE customer_activities (
                                     activity_id VARCHAR(50) NOT NULL,
                                     customer_id VARCHAR(50) NOT NULL, -- 故意不设外键
                                     opportunity_id VARCHAR(50), -- 故意不设外键
                                     activity_type VARCHAR(100), -- 'CALL', 'EMAIL', 'MEETING', 'TASK'
                                     activity_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     assigned_employee_id VARCHAR(50), -- 故意不设外键 (指向HR库的employee_id)
                                     activity_details TEXT -- 存储详细活动内容
);

-- 插入初始客户数据 (10万)
DELIMITER //
CREATE PROCEDURE InsertLargeCustomers()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE ind VARCHAR(100);
    DECLARE cty VARCHAR(100);
    WHILE i <= 100000 DO
            SET ind = ELT(FLOOR(1 + (RAND() * 5)), 'Tech', 'Retail', 'Finance', 'Manufacturing', 'Healthcare');
            SET cty = ELT(FLOOR(1 + (RAND() * 5)), 'New York', 'London', 'Tokyo', 'Shanghai', 'Sydney');
            INSERT INTO customers (customer_id, customer_name, industry, city, country) VALUES
                (CONCAT('CUST_', LPAD(i, 6, '0')),
                 CONCAT('CustomerCorp', i),
                 ind,
                 cty,
                 ELT(FLOOR(1 + (RAND() * 3)), 'USA', 'UK', 'China')
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeCustomers();
DROP PROCEDURE InsertLargeCustomers;

-- 插入销售机会数据 (20万)
DELIMITER //
CREATE PROCEDURE InsertLargeOpportunities()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE cust_id VARCHAR(50);
    DECLARE emp_id VARCHAR(50);
    DECLARE close_dt DATE;
    WHILE i <= 200000 DO
            SET cust_id = CONCAT('CUST_', LPAD(FLOOR(1 + (RAND() * 100000)), 6, '0'));
            SET emp_id = CONCAT('EMP_', LPAD(FLOOR(1 + (RAND() * 500000)), 6, '0')); -- 引用HR库的员工
            SET close_dt = DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 365) DAY);
            INSERT INTO sales_opportunities (opportunity_id, customer_id, opportunity_name, stage, amount, close_date, assigned_employee_id) VALUES
                (CONCAT('OPP_', LPAD(i, 6, '0')),
                 cust_id,
                 CONCAT('Opportunity for ', cust_id, ' - Project ', i),
                 ELT(FLOOR(1 + (RAND() * 5)), 'NEW', 'QUALIFIED', 'PROPOSAL', 'CLOSED_WON', 'CLOSED_LOST'),
                 ROUND(RAND() * 500000 + 1000, 2),
                 close_dt,
                 emp_id
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeOpportunities();
DROP PROCEDURE InsertLargeOpportunities;

-- 插入客户活动数据 (50万)
DELIMITER //
CREATE PROCEDURE InsertLargeCustomerActivities()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE cust_id VARCHAR(50);
    DECLARE opp_id VARCHAR(50);
    DECLARE emp_id VARCHAR(50);
    WHILE i <= 500000 DO
            SET cust_id = CONCAT('CUST_', LPAD(FLOOR(1 + (RAND() * 100000)), 6, '0'));
            SET opp_id = CONCAT('OPP_', LPAD(FLOOR(1 + (RAND() * 200000)), 6, '0'));
            SET emp_id = CONCAT('EMP_', LPAD(FLOOR(1 + (RAND() * 500000)), 6, '0')); -- 引用HR库的员工
            INSERT INTO customer_activities (activity_id, customer_id, opportunity_id, activity_type, assigned_employee_id, activity_details) VALUES
                (CONCAT('ACT_', LPAD(i, 6, '0')),
                 cust_id,
                 CASE WHEN RAND() < 0.8 THEN opp_id ELSE NULL END, -- 20%的活动没有关联机会
                 ELT(FLOOR(1 + (RAND() * 4)), 'CALL', 'EMAIL', 'MEETING', 'TASK'),
                 emp_id,
                 CONCAT('Details for activity ', i, '. This is a very verbose description of the customer interaction to simulate large text data storage and retrieval. It might contain notes about discussions, next steps, and customer feedback. ', REPEAT('More text here to ensure the TEXT column is well-filled.', 5))
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeCustomerActivities();
DROP PROCEDURE InsertLargeCustomerActivities;


-- 创建数据库
CREATE DATABASE enterprise_project_mgmt;
USE enterprise_project_mgmt;

-- 项目表 (project_id 非主键，无索引，owner_employee_id 无外键)
CREATE TABLE projects (
                          project_id VARCHAR(50) NOT NULL,
                          project_name VARCHAR(255) NOT NULL,
                          description TEXT,
                          start_date DATE,
                          end_date DATE,
                          status VARCHAR(50), -- 'PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELED'
                          owner_employee_id VARCHAR(50), -- 故意不设外键 (指向HR库的employee_id)
                          budget DECIMAL(15, 2)
);

-- 任务表 (task_id 非主键，无索引，project_id, assigned_employee_id, parent_task_id 无外键)
CREATE TABLE tasks (
                       task_id VARCHAR(50) NOT NULL,
                       project_id VARCHAR(50) NOT NULL, -- 故意不设外键
                       task_name VARCHAR(255) NOT NULL,
                       description TEXT,
                       due_date DATE,
                       priority VARCHAR(50), -- 'HIGH', 'MEDIUM', 'LOW'
                       status VARCHAR(50), -- 'OPEN', 'IN_PROGRESS', 'CLOSED'
                       assigned_employee_id VARCHAR(50), -- 故意不设外键 (指向HR库的employee_id)
                       parent_task_id VARCHAR(50) -- 故意不设外键 (自引用，无索引)
);

-- 插入项目数据 (5万)
DELIMITER //
CREATE PROCEDURE InsertLargeProjects()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE emp_id VARCHAR(50);
    DECLARE start_dt DATE;
    WHILE i <= 50000 DO
            SET emp_id = CONCAT('EMP_', LPAD(FLOOR(1 + (RAND() * 500000)), 6, '0')); -- 引用HR库的员工
            SET start_dt = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365 * 5) DAY);
            INSERT INTO projects (project_id, project_name, description, start_date, end_date, status, owner_employee_id, budget) VALUES
                (CONCAT('PROJ_', LPAD(i, 5, '0')),
                 CONCAT('Enterprise Project ', i, ' - Initiative Alpha'),
                 CONCAT('A comprehensive project description for project ', i, ' outlining objectives, scope, and deliverables. This text is long to test TEXT column performance.'),
                 start_dt,
                 DATE_ADD(start_dt, INTERVAL FLOOR(RAND() * 730) DAY), -- 最多2年
                 ELT(FLOOR(1 + (RAND() * 4)), 'PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELED'),
                 emp_id,
                 ROUND(RAND() * 1000000 + 10000, 2)
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeProjects();
DROP PROCEDURE InsertLargeProjects;

-- 插入任务数据 (20万)
DELIMITER //
CREATE PROCEDURE InsertLargeTasks()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE proj_id VARCHAR(50);
    DECLARE emp_id VARCHAR(50);
    DECLARE due_dt DATE;
    DECLARE parent_t_id VARCHAR(50);
    WHILE i <= 200000 DO
            SET proj_id = CONCAT('PROJ_', LPAD(FLOOR(1 + (RAND() * 50000)), 5, '0'));
            SET emp_id = CONCAT('EMP_', LPAD(FLOOR(1 + (RAND() * 500000)), 6, '0')); -- 引用HR库的员工
            SET due_dt = DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 180) DAY);
            SET parent_t_id = CASE WHEN RAND() < 0.2 THEN CONCAT('TASK_', LPAD(FLOOR(1 + (RAND() * (i-1))), 6, '0')) ELSE NULL END;

            INSERT INTO tasks (task_id, project_id, task_name, description, due_date, priority, status, assigned_employee_id, parent_task_id) VALUES
                (CONCAT('TASK_', LPAD(i, 6, '0')),
                 proj_id,
                 CONCAT('Task ', i, ' for Project ', proj_id),
                 CONCAT('Details for task ', i, '. This description can be quite long, containing instructions, sub-tasks, and relevant links. This will further stress the TEXT column lookups.'),
                 due_dt,
                 ELT(FLOOR(1 + (RAND() * 3)), 'HIGH', 'MEDIUM', 'LOW'),
                 ELT(FLOOR(1 + (RAND() * 3)), 'OPEN', 'IN_PROGRESS', 'CLOSED'),
                 emp_id,
                 parent_t_id
                );
            SET i = i + 1;
        END WHILE;
END //
DELIMITER ;
CALL InsertLargeTasks();
DROP PROCEDURE InsertLargeTasks;

-- 创建数据库
CREATE DATABASE enterprise_reporting_db;
USE enterprise_reporting_db;

-- 报告日志表 (模拟一个用于存储报告生成记录的表，实际业务数据通过跨库查询获取)
CREATE TABLE report_generation_log (
                                       log_id INT AUTO_INCREMENT PRIMARY KEY,
                                       report_name VARCHAR(255) NOT NULL,
                                       start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       end_time DATETIME,
                                       status VARCHAR(50), -- 'SUCCESS', 'FAILED', 'RUNNING'
                                       generated_by_employee_id VARCHAR(50) -- 故意不设外键 (指向HR库的employee_id)
);

-- 插入一些报告日志
INSERT INTO report_generation_log (report_name, start_time, end_time, status, generated_by_employee_id) VALUES
                                                                                                            ('Monthly Sales Performance', NOW(), NOW() + INTERVAL 30 SECOND, 'SUCCESS', 'EMP_000001'),
                                                                                                            ('Employee Turnover Rate', NOW(), NOW() + INTERVAL 1 MINUTE, 'SUCCESS', 'EMP_000002'),
                                                                                                            ('Project Progress Report', NOW(), NULL, 'RUNNING', 'EMP_000003');