# 登录认证功能说明

## 功能概述

DB-Doctor V3.1.0 新增了用户登录认证功能，支持：
- 默认账号密码登录
- 在设置中修改密码
- 忘记密码可通过 H2 数据库直接修改

## 默认账号密码

### 开发环境
- 用户名：`admin`
- 密码：`admin123`

### 生产环境
请在 `application-prod.yml` 中配置：
```yaml
db-doctor:
  auth:
    default-username: your_username
    default-password: your_password
```

## API 接口

### 1. 用户登录
**请求：**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "username": "admin",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "loginTime": "2024-01-22T10:30:00"
  }
}
```

### 2. 修改密码
**请求：**
```http
POST /api/auth/change-password
Content-Type: application/json

{
  "oldPassword": "admin123",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

### 3. 检查用户是否存在
**请求：**
```http
GET /api/auth/exists/{username}
```

**响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

## 忘记密码处理

### 方式 1：通过 H2 控制台修改
1. 访问 H2 控制台：`http://localhost:8080/h2-console`
2. 连接信息：
   - JDBC URL: `jdbc:h2:file:./data/db-doctor-internal`
   - 用户名: `sa`
   - 密码: (留空)
3. 执行 SQL 更新密码：
```sql
-- 将密码重置为 newpassword
UPDATE sys_user
SET password = '5e9d110a832c4c07444a7f354c649b5f'  -- MD5 of 'newpassword'
WHERE username = 'admin';
```

### 方式 2：生成 MD5 密码
使用在线 MD5 工具或以下代码：
```java
import java.security.MessageDigest;

public String md5Encrypt(String plainText) {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(plainText.getBytes());
    StringBuilder sb = new StringBuilder();
    for (byte b : digest) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}
```

## 技术实现

### 密码加密
- 使用 MD5 单向加密
- 存储格式：32 位小写十六进制字符串

### 默认用户初始化
- 应用启动时自动检查默认用户是否存在
- 如果不存在，自动创建默认用户
- 如果已存在，跳过初始化

### 数据库表结构
```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NOT NULL
);
```

## 测试

### 运行单元测试
```bash
mvn test -Dtest=AuthenticationServiceTest
```

### 运行集成测试
```bash
mvn test -Dtest=AuthenticationIntegrationTest
```

### 运行所有测试
```bash
mvn test
```

或使用测试脚本：
```bash
# Windows
test-auth.bat

# Linux/Mac
./test-auth.sh
```

## 安全建议

### 生产环境部署前
1. **修改默认密码**：首次登录后立即修改密码
2. **配置 HTTPS**：使用 HTTPS 加密传输
3. **使用 JWT**：当前使用简单 token，建议升级为 JWT
4. **启用会话管理**：添加会话超时和单点登录
5. **密码复杂度**：强制密码复杂度要求

### 后续优化方向
- [ ] 集成 Spring Security
- [ ] 实现 JWT Token 认证
- [ ] 添加验证码功能
- [ ] 实现密码复杂度检查
- [ ] 添加登录失败锁定机制
- [ ] 实现多用户管理

## 常见问题

### Q1: 登录失败提示"用户名或密码错误"？
**A:** 请检查：
1. 用户名和密码是否正确
2. 数据库中用户是否已初始化
3. 密码是否使用 MD5 加密

### Q2: 忘记密码怎么办？
**A:** 使用 H2 控制台直接修改数据库中的密码字段（见上方"忘记密码处理"）

### Q3: 如何修改默认账号密码？
**A:**
1. 使用默认账号登录
2. 调用修改密码接口修改密码
3. 或在配置文件中修改 `db-doctor.auth.default-username` 和 `default-password`

## 版本历史
- **V3.1.0** (2024-01-22): 新增登录认证功能
  - 默认账号密码登录
  - 修改密码功能
  - 忘记密码处理
  - 完整的单元测试和集成测试
