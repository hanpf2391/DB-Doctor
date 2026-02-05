# 前后端登录功能完整文档

## 概述

本文档描述 DB-Doctor V3.1.0 的前后端登录认证功能，采用 TDD（测试驱动开发）方法实现。

## 默认账号密码

- **用户名**: `dbdoctor`
- **密码**: `dbdoctor`

## 功能特性

### 后端功能 ✅
- ✅ 用户登录认证
- ✅ 密码修改
- ✅ 默认用户自动初始化
- ✅ MD5 密码加密
- ✅ 完整的单元测试和集成测试

### 前端功能 ✅
- ✅ 登录页面
- ✅ 路由守卫（未登录跳转）
- ✅ 认证状态管理（Pinia Store）
- ✅ Token 自动添加到请求头
- ✅ 登录状态持久化（localStorage）
- ✅ 401 自动跳转登录
- ✅ 完整的单元测试

## 技术栈

### 后端
- Spring Boot 3.x
- Spring Data JPA
- H2 Database
- JUnit 5
- Mockito

### 前端
- Vue 3 + TypeScript
- Pinia (状态管理)
- Vue Router
- Element Plus
- Vitest (测试框架)
- Axios

## 后端实现

### 1. 实体和 DTO

**User 实体** (`src/main/java/com/dbdoctor/entity/User.java`)
```java
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false)
    private LocalDateTime updatedTime;
}
```

**DTO 类**
- `LoginRequest.java` - 登录请求
- `LoginResponse.java` - 登录响应
- `ChangePasswordRequest.java` - 修改密码请求

### 2. API 接口

**登录接口**
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "dbdoctor",
  "password": "dbdoctor"
}

响应:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "username": "dbdoctor",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "loginTime": "2024-01-22T10:30:00"
  }
}
```

**修改密码接口**
```
POST /api/auth/change-password
Content-Type: application/json

{
  "oldPassword": "dbdoctor",
  "newPassword": "newpass123",
  "confirmPassword": "newpass123"
}

响应:
{
  "code": 200,
  "message": "密码修改成功"
}
```

### 3. 核心服务

**AuthenticationService** (`src/main/java/com/dbdoctor/service/AuthenticationService.java`)
```java
public interface AuthenticationService {
    LoginResponse login(LoginRequest request);
    void changePassword(String username, ChangePasswordRequest request);
    void initializeDefaultUser(String defaultUsername, String defaultPassword);
    boolean userExists(String username);
}
```

### 4. 自动初始化

应用启动时自动创建默认用户（如果不存在）：

**UserInitializationConfig** (`src/main/java/com/dbdoctor/config/UserInitializationConfig.java`)
```java
@Component
public class UserInitializationConfig {
    @EventListener(ApplicationReadyEvent.class)
    public void initializeDefaultUser() {
        authenticationService.initializeDefaultUser(
            defaultUsername,
            defaultPassword
        );
    }
}
```

## 前端实现

### 1. 类型定义

**认证类型** (`frontend/src/api/types.ts`)
```typescript
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  username: string
  token: string
  loginTime: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
```

### 2. API 调用

**认证 API** (`frontend/src/api/auth.ts`)
```typescript
export async function login(data: LoginRequest): Promise<LoginResponse> {
  const response = await request<ApiResponse<LoginResponse>>({
    url: '/api/auth/login',
    method: 'POST',
    data
  })
  return response.data
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await request<ApiResponse<void>>({
    url: '/api/auth/change-password',
    method: 'POST',
    data
  })
}
```

### 3. 状态管理

**认证 Store** (`frontend/src/stores/auth.ts`)
```typescript
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('dbdoctor_token'))
  const username = ref<string | null>(localStorage.getItem('dbdoctor_user'))
  const loginTime = ref<string | null>(localStorage.getItem('dbdoctor_login_time'))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  async function login(credentials: LoginRequest) {
    // 登录逻辑
  }

  async function logout() {
    // 登出逻辑
  }

  function restoreSession() {
    // 从 localStorage 恢复登录状态
  }

  return {
    token,
    username,
    isAuthenticated,
    login,
    logout,
    restoreSession
  }
})
```

### 4. 登录页面

**登录组件** (`frontend/src/views/Login.vue`)

功能：
- 用户名密码输入
- 表单验证
- 加载状态显示
- 错误提示
- Enter 键快捷登录

### 5. 路由守卫

**路由配置** (`frontend/src/router/index.ts`)
```typescript
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 恢复登录状态
  if (!authStore.isAuthenticated && !authStore.username) {
    authStore.restoreSession()
  }

  // 需要认证的页面
  if (to.meta.requiresAuth !== false) {
    if (authStore.isAuthenticated) {
      next()
    } else {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }
  } else {
    next()
  }
})
```

### 6. HTTP 拦截器

**请求拦截器** (`frontend/src/api/index.ts`)
```typescript
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('dbdoctor_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

**响应拦截器**
```typescript
request.interceptors.response.use(
  (response) => {
    // 成功响应处理
  },
  (error) => {
    // 401 未授权处理
    if (error.response?.status === 401) {
      // 清除认证信息
      localStorage.removeItem('dbdoctor_token')
      // 跳转登录页
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
```

## 使用指南

### 启动后端

```bash
# 方式 1: Maven
mvn spring-boot:run

# 方式 2: IDE
运行主类: DbDoctorApplication

# 方式 3: JAR
java -jar target/db-doctor-3.1.0.jar
```

### 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 类型检查
npm run type-check

# 运行测试
npm run test
```

### 访问应用

1. 打开浏览器: `http://localhost:5173` (前端开发服务器)
2. 自动跳转到登录页
3. 使用默认账号登录:
   - 用户名: `dbdoctor`
   - 密码: `dbdoctor`
4. 登录成功后跳转到首页

## 测试

### 后端测试

**运行所有测试**
```bash
mvn test
```

**运行认证相关测试**
```bash
# 单元测试
mvn test -Dtest=AuthenticationServiceTest

# 集成测试
mvn test -Dtest=AuthenticationIntegrationTest

# 使用验证脚本
./verify-auth.sh  # Linux/Mac
verify-auth.bat   # Windows
```

### 前端测试

**运行所有测试**
```bash
cd frontend
npm run test
```

**运行特定测试**
```bash
# API 测试
npm run test auth.spec.ts

# Store 测试
npm run test stores/__tests__/auth.spec.ts
```

## 修改默认密码

### 方法 1：通过登录页面修改

1. 使用默认账号登录
2. 访问设置中心（TODO：需要实现修改密码页面）
3. 输入旧密码和新密码
4. 提交修改

### 方法 2：通过 H2 数据库修改

1. 访问 H2 控制台: `http://localhost:8080/h2-console`
2. 连接信息:
   - JDBC URL: `jdbc:h2:file:./data/db-doctor-internal`
   - 用户名: `sa`
   - 密码: (空)
3. 执行 SQL:
```sql
UPDATE sys_user
SET password = '5e9d110a832c4c07444a7f354c649b5f'  -- MD5 of 'newpassword'
WHERE username = 'dbdoctor';
```

### 方法 3：在线 MD5 工具

1. 访问: https://md5jiami.51240.com/
2. 输入新密码，获取 MD5 值
3. 使用 H2 控制台更新

## 配置修改

### 后端配置

**application.yml**
```yaml
db-doctor:
  auth:
    default-username: dbdoctor
    default-password: dbdoctor
```

### 前端配置

前端无需额外配置，默认从后端 API 获取认证信息。

## 项目结构

### 后端结构
```
src/main/java/com/dbdoctor/
├── controller/
│   └── AuthController.java              # 认证控制器
├── service/
│   ├── AuthenticationService.java        # 认证服务接口
│   └── AuthenticationServiceImpl.java    # 认证服务实现
├── repository/
│   └── UserRepository.java               # 用户仓储
├── entity/
│   └── User.java                         # 用户实体
├── dto/
│   ├── LoginRequest.java                # 登录请求 DTO
│   ├── LoginResponse.java               # 登录响应 DTO
│   └── ChangePasswordRequest.java       # 修改密码请求 DTO
└── config/
    └── UserInitializationConfig.java     # 用户初始化配置

src/test/java/com/dbdoctor/
├── service/
│   └── AuthenticationServiceTest.java    # 单元测试
└── integration/
    └── AuthenticationIntegrationTest.java # 集成测试
```

### 前端结构
```
frontend/src/
├── api/
│   ├── auth.ts                          # 认证 API
│   ├── types.ts                         # 类型定义（包含认证类型）
│   └── __tests__/
│       └── auth.spec.ts                 # API 测试
├── stores/
│   ├── auth.ts                          # 认证 Store
│   └── __tests__/
│       └── auth.spec.ts                 # Store 测试
├── views/
│   └── Login.vue                        # 登录页面
├── router/
│   └── index.ts                         # 路由配置（含守卫）
└── main.ts                              # 应用入口（集成认证）
```

## TDD 流程

本项目严格遵循 TDD（测试驱动开发）流程：

### 后端 TDD
1. ✅ **红色阶段**: 编写失败的测试
2. ✅ **绿色阶段**: 实现最小代码使测试通过
3. ✅ **重构阶段**: 优化代码质量

### 前端 TDD
1. ✅ **搭建阶段**: 定义接口和类型
2. ✅ **红色阶段**: 编写失败的测试
3. ✅ **绿色阶段**: 实现最小代码使测试通过

## 安全建议

### 当前实现（基础版）
- ✅ MD5 密码加密
- ✅ Token 认证
- ✅ 路由守卫
- ✅ 自动初始化默认用户

### 生产环境建议（升级版）
- ⚠️ 使用 BCrypt 替代 MD5
- ⚠️ 实现 JWT Token
- ⚠️ 添加 Token 刷新机制
- ⚠️ 实现密码复杂度检查
- ⚠️ 添加登录失败锁定
- ⚠️ 实现 CSRF 防护
- ⚠️ 添加验证码功能
- ⚠️ 启用 HTTPS

## 常见问题

### Q1: 登录后刷新页面，为什么还在登录页？
**A**: 检查以下几点：
1. localStorage 是否正常保存 token
2. `main.ts` 中是否调用了 `restoreSession()`
3. 路由守卫是否正确配置

### Q2: 为什么修改密码后需要重新登录？
**A**: 这是正常行为，修改密码后旧 token 失效，需要重新登录。

### Q3: 如何在生产环境修改默认账号密码？
**A**: 在 `application-prod.yml` 中配置：
```yaml
db-doctor:
  auth:
    default-username: your_username
    default-password: your_password
```

### Q4: 前端测试如何运行？
**A**:
```bash
cd frontend
npm run test       # 运行所有测试
npm run test:ui    # 运行测试 UI
npm run test:coverage # 生成覆盖率报告
```

## 后续优化方向

- [ ] 集成 Spring Security
- [ ] 实现 JWT Token 认证
- [ ] 添加验证码功能
- [ ] 实现密码复杂度检查
- [ ] 添加登录失败锁定机制
- [ ] 实现多用户管理
- [ ] 添加用户角色权限
- [ ] 实现记住我功能
- [ ] 添加登录日志

## 相关文档

- [后端认证功能文档](AUTH_FEATURE.md)
- [快速开始指南](AUTH_QUICKSTART.md)
- [项目开发规范](CLAUDE.md)

## 版本历史

- **V3.1.0** (2024-01-22): 新增前后端登录功能
  - 默认账号密码: dbdoctor/dbdoctor
  - 完整的 TDD 测试
  - 路由守卫和状态管理
  - Token 自动添加到请求头
  - 401 自动跳转登录
