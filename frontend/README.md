# DB-Doctor Frontend

DB-Doctor 前端项目 - 基于 Vue 3 + Element Plus

## 技术栈

- Vue 3 (Composition API + `<script setup>`)
- TypeScript
- Vite 5.x
- Element Plus
- Pinia (状态管理)
- Vue Router 4
- Axios

## 开发环境运行

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:3000

## 生产构建

```bash
npm run build
```

构建产物在 `dist/` 目录

## 与 Spring Boot 集成

### 方式 1: 手动复制

```bash
# 1. 构建前端
npm run build

# 2. 复制到 Spring Boot static 目录
cp -r dist/* ../src/main/resources/static/
```

### 方式 2: Maven 插件（推荐）

在 `pom.xml` 中添加资源复制插件（见后端配置）

## 项目结构

```
frontend/
├── src/
│   ├── api/                 # API 封装
│   ├── assets/styles/      # 全局样式
│   ├── components/         # 公共组件
│   ├── router/             # 路由配置
│   ├── stores/             # Pinia 状态管理
│   ├── views/               # 页面组件
│   │   ├── Home.vue        # 首页
│   │   └── Settings/       # 设置中心
│   ├── App.vue             # 根组件
│   └── main.ts             # 入口文件
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## API 接口

所有 API 请求都会通过 Vite Proxy 代理到 `http://localhost:8080/api`

- `/api/config/all` - 获取所有配置
- `/api/config/save` - 保存配置
- `/api/config/test/db` - 测试数据库连接
- `/api/config/test/ai` - 测试 AI 连通性

## 开发注意事项

1. **路径别名**: 使用 `@/` 代替相对路径
2. **API 代理**: 开发环境自动代理到后端 8080 端口
3. **类型安全**: 所有 API 都有 TypeScript 类型定义
4. **组件使用**: 优先使用 Element Plus 组件

---

**生成时间**: 2026-01-31
**版本**: v2.2.0
