# 登录功能快速开始指南

## 功能特点
✅ 默认账号密码：dbdoctor / dbdoctor
✅ 支持修改密码
✅ 忘记密码可通过 H2 数据库直接修改
✅ 完整的单元测试和集成测试
✅ TDD 开发流程保证代码质量

## 快速开始

### 1. 启动应用
```bash
mvn spring-boot:run
```

### 2. 测试登录
使用 cURL 或 Postman 测试：

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"dbdoctor","password":"dbdoctor"}'
```

### 3. 修改密码
```bash
curl -X POST http://localhost:8080/api/auth/change-password \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"dbdoctor","newPassword":"newpass123","confirmPassword":"newpass123"}'
```

### 4. 验证新密码
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"dbdoctor","password":"newpass123"}'
```

## 运行测试

### Windows
```bash
verify-auth.bat
```

### Linux/Mac
```bash
chmod +x verify-auth.sh
./verify-auth.sh
```

### 或使用 Maven
```bash
mvn test
```

## 忘记密码处理

### 方法 1：通过 H2 控制台
1. 访问：http://localhost:8080/h2-console
2. 连接信息：
   - JDBC URL: `jdbc:h2:file:./data/db-doctor-internal`
   - 用户名: `sa`
   - 密码: (空)
3. 执行 SQL：
```sql
UPDATE sys_user
SET password = '5e9d110a832c4c07444a7f354c649b5f'
WHERE username = 'admin';
```
- 注：`5e9d110a832c4c07444a7f354c649b5f` 是 `newpassword` 的 MD5 值

### 方法 2：在线 MD5 工具
1. 访问：https://md5jiami.51240.com/
2. 输入新密码，获取 MD5 值
3. 使用 H2 控制台更新

## 配置修改

### 修改默认账号密码
编辑 `application.yml`：
```yaml
db-doctor:
  auth:
    default-username: your_username
    default-password: your_password
```

## API 文档

### 登录
- **URL**: `POST /api/auth/login`
- **请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- **响应**:
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

### 修改密码
- **URL**: `POST /api/auth/change-password`
- **请求体**:
```json
{
  "oldPassword": "admin123",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}
```
- **响应**:
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

### 检查用户是否存在
- **URL**: `GET /api/auth/exists/{username}`
- **响应**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true
}
```

## 项目结构
```
src/main/java/com/dbdoctor/
├── controller/
│   └── AuthController.java          # 认证控制器
├── service/
│   ├── AuthenticationService.java    # 认证服务接口
│   └── AuthenticationServiceImpl.java # 认证服务实现
├── repository/
│   └── UserRepository.java           # 用户仓储
├── entity/
│   └── User.java                     # 用户实体
├── dto/
│   ├── LoginRequest.java            # 登录请求 DTO
│   ├── LoginResponse.java           # 登录响应 DTO
│   └── ChangePasswordRequest.java   # 修改密码请求 DTO
└── config/
    └── UserInitializationConfig.java # 用户初始化配置

src/test/java/com/dbdoctor/
├── service/
│   └── AuthenticationServiceTest.java   # 单元测试
└── integration/
    └── AuthenticationIntegrationTest.java # 集成测试
```

## 常见问题

**Q: 登录失败提示"用户名或密码错误"？**
A: 检查以下几点：
1. 用户名和密码是否正确
2. 数据库中用户是否已初始化
3. 密码是否使用 MD5 加密

**Q: 如何重置密码？**
A: 通过 H2 控制台直接修改数据库（见上方"忘记密码处理"）

**Q: 首次登录后必须修改密码吗？**
A: 不是必须的，但建议首次登录后修改默认密码

## 技术栈
- Spring Boot 3.x
- Spring Data JPA
- H2 Database
- Lombok
- JUnit 5
- Mockito

## 后续优化建议
- [ ] 集成 Spring Security
- [ ] 实现 JWT Token 认证
- [ ] 添加验证码功能
- [ ] 实现密码复杂度检查
- [ ] 添加登录失败锁定机制
- [ ] 实现多用户管理
- [ ] 添加用户角色权限

## 详细文档
更多详细信息请查看：[docs/AUTH_FEATURE.md](AUTH_FEATURE.md)
