# 登录页面 Logo 使用指南

## 方式 1：使用图片文件（推荐）

### 步骤：

1. **准备 Logo 图片**
   - 推荐尺寸：128x128 或 256x256
   - 格式：PNG（支持透明背景）、SVG、JPG
   - 文件名：`logo.png` 或 `logo.svg`

2. **放置图片**

   **选项 A：放到 public 目录**（推荐）
   ```
   public/
   └── logo.png  ← 放在这里
   ```

   **选项 B：放到 assets 目录**
   ```
   src/
   └── assets/
       └── images/
           └── logo.png  ← 放在这里
   ```

3. **修改登录页面代码**

   ```vue
   <!-- 如果在 public 目录 -->
   <template>
     <div class="login-container">
       <div class="login-card">
         <div class="login-header">
           <!-- 使用图片 -->
           <img src="/logo.png" alt="DB-Doctor" class="project-logo" />
           <h1 class="login-title">DB-Doctor</h1>
           <p class="login-subtitle">MySQL 慢查询智能诊疗系统</p>
         </div>
         ...
       </div>
     </div>
   </template>

   <style scoped>
   .project-logo {
     width: 80px;
     height: 80px;
     margin-bottom: 20px;
     object-fit: contain;
   }
   </style>
   ```

   ```vue
   <!-- 如果在 src/assets 目录 -->
   <template>
     <div class="login-header">
       <!-- 使用 import 导入图片 -->
       <img :src="logoUrl" alt="DB-Doctor" class="project-logo" />
       ...
     </div>
   </template>

   <script setup lang="ts">
   import { ref, reactive, onMounted } from 'vue'
   import { useRouter } from 'vue-router'
   import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
   import { User, Lock } from '@element-plus/icons-vue'
   import { useAuthStore } from '@/stores/auth'

   // 导入图片
   import logoUrl from '@/assets/images/logo.png'
   </script>

   <style scoped>
   .project-logo {
     width: 80px;
     height: 80px;
     margin-bottom: 20px;
     object-fit: contain;
   }
   </style>
   ```

---

## 方式 2：使用 Element Plus 图标（当前实现）

### 当前使用的图标：
- **Document** - 文档图标（蓝色）
- 其他可选图标见下方列表

### 更换其他图标：

```vue
<template>
  <div class="login-header">
    <!-- 更换不同的图标 -->
    <div class="project-icon">
      <el-icon :size="64"><Coin /></el-icon>
    </div>
    ...
  </div>
</template>

<script setup lang="ts">
import { User, Lock, Coin } from '@element-plus/icons-vue'  // 导入新图标
</script>
```

### 可用的图标选项：

#### 数据库相关
```vue
<el-icon :size="64"><Coin /></el-icon>          <!-- 数据库/硬币 -->
<el-icon :size="64"><Files /></el-icon>         <!-- 文件/多文档 -->
<el-icon :size="64"><FolderOpened /></el-icon> <!-- 文件夹 -->
<el-icon :size="64"><Document /></el-icon>      <!-- 单个文档 -->
<el-icon :size="64"><Notebook /></el-icon>      <!-- 笔记本 -->
```

#### 服务器相关
```vue
<el-icon :size="64"><Monitor /></el-icon>        <!-- 显示器/服务器 -->
<el-icon :size="64"><Odometer /></el-icon>       <!-- 仪表盘 -->
<el-icon :size="64"><DataLine /></el-icon>      <!-- 数据线 -->
<el-icon :size="64"><TrendCharts /></el-icon>    <!-- 图表/分析 -->
```

#### 医疗相关
```vue
<el-icon :size="64"><FirstAidKit /></el-icon>    <!-- 急救箱 -->
<el-icon :size="64"><Briefcase /></el-icon>     <!-- 工具箱 -->
```

#### 搜索相关
```vue
<el-icon :size="64"><Search /></el-icon>        <!-- 搜索/放大镜 -->
<el-icon :size="64"><ZoomIn /></el-icon>        <!-- 放大镜 -->
```

---

## 方式 3：使用自定义 SVG 代码

```vue
<template>
  <div class="login-header">
    <!-- 自定义 SVG Logo -->
    <div class="project-icon" v-html="logoSvg"></div>
    ...
  </div>
</template>

<script setup lang="ts">
// 定义 SVG 代码
const logoSvg = `
  <svg viewBox="0 0 128 128" fill="none" xmlns="http://www.w3.org/2000/svg">
    <!-- 背景圆 -->
    <circle cx="64" cy="64" r="60" fill="url(#gradient)"/>

    <!-- 图标内容 -->
    <path d="M40 64 L64 40 L88 64" stroke="white" stroke-width="4" stroke-linecap="round"/>

    <!-- 渐变定义 -->
    <defs>
      <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:#667eea;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#764ba2;stop-opacity:1" />
      </linearGradient>
    </defs>
  </svg>
`
</script>
```

---

## 方式 4：使用文字图标（最简单）

```vue
<template>
  <div class="login-header">
    <!-- 文字图标 -->
    <div class="project-icon-text">
      DB
    </div>
    <h1 class="login-title">DB-Doctor</h1>
    ...
  </div>
</template>

<style scoped>
.project-icon-text {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  color: white;
  font-size: 32px;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}
</style>
```

---

## 🎯 推荐方案

### 如果你有 Logo 图片：
1. 把图片放到 `public/logo.png`
2. 修改登录页面使用 `<img src="/logo.png">`

### 如果没有 Logo：
- **方案 A**：使用 Element Plus 图标（当前实现）
- **方案 B**：使用文字图标（如 "DB"）
- **方案 C**：设计一个 SVG 图标

---

## 💡 快速替换图标

如果你想换成其他 Element Plus 图标，告诉我你想要什么类型的图标，我帮你替换：

- 数据库图标？
- 医疗图标？
- 分析图标？
- 搜索图标？
- 或者其他？

或者你有一个 Logo 图片文件，发给我，我帮你集成到登录页面！
