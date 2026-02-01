<template>
  <el-container class="app-container">
    <!-- 侧边栏 -->
    <el-aside width="200px">
      <div class="logo">
        <img src="@/assets/logo.png" alt="DB-Doctor Logo" class="logo-img" />
        <h2>DB-Doctor</h2>
        <p>v2.3.2</p>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/ai-monitor">
          <el-icon><Monitor /></el-icon>
          <span>AI 监控</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><DataAnalysis /></el-icon>
          <span>慢查询报表</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>设置中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- 系统监控浮窗 -->
    <SystemMonitor />
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SystemMonitor from '@/components/SystemMonitor.vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.app-container {
  height: 100vh;
}

.app-container .el-aside {
  background-color: #409eff;
  color: #fff;
}

.app-container .logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.app-container .logo-img {
  width: 64px;
  height: 64px;
  margin-bottom: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  padding: 8px;
}

.app-container .logo h2 {
  margin: 0;
  font-size: 24px;
}

.app-container .logo p {
  margin: 5px 0 0;
  font-size: 12px;
  opacity: 0.7;
}

.app-container .sidebar-menu {
  border: none;
  background-color: transparent;
}

.app-container .sidebar-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.8);
}

.app-container .sidebar-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.app-container .sidebar-menu .el-menu-item.is-active {
  background-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.app-container .el-main {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>
