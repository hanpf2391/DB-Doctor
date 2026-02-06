<template>
  <div class="settings-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="page-title">设置中心</h1>
        </div>
        <div class="header-right">
          <div class="status-badge">
            <span class="status-dot"></span>
            <span>系统运行正常</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 顶部大项切换 -->
    <div class="top-tabs">
      <div class="tabs-container">
        <div
          v-for="item in menuItems"
          :key="item.key"
          :class="['tab-item', { active: activeTab === item.key }]"
          @click="handleMenuClick(item.key)"
        >
          <el-icon class="tab-icon">
            <component :is="item.icon" />
          </el-icon>
          <span class="tab-text">{{ item.title }}</span>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 基础配置 -->
      <BasicConfig v-if="activeTab === 'basic'" />

      <!-- 监控与通知 -->
      <div v-else-if="activeTab === 'monitor'" class="config-section">
        <!-- 监控配置卡片 -->
        <div class="config-card">
          <div class="card-header">
            <div class="header-left">
              <div class="header-icon icon-warning">
                <el-icon :size="20"><Bell /></el-icon>
              </div>
              <div class="header-text">
                <h3 class="card-title">监控配置</h3>
                <p class="card-desc">配置慢查询监控告警规则</p>
              </div>
            </div>
            <el-tag size="small" type="info" effect="light">开发中</el-tag>
          </div>
          <div class="card-body">
            <el-empty description="监控配置功能开发中..." />
          </div>
        </div>

        <!-- 通知方式卡片 -->
        <div class="config-card">
          <div class="card-header">
            <div class="header-left">
              <div class="header-icon icon-info">
                <el-icon :size="20"><Message /></el-icon>
              </div>
              <div class="header-text">
                <h3 class="card-title">通知方式</h3>
                <p class="card-desc">配置告警通知渠道（邮件、钉钉、企业微信）</p>
              </div>
            </div>
            <el-tag size="small" type="info" effect="light">开发中</el-tag>
          </div>
          <div class="card-body">
            <el-empty description="通知配置功能开发中..." />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  Setting,
  Coin,
  MagicStick,
  Bell,
  Message,
  CircleCheck,
  ArrowRight
} from '@element-plus/icons-vue'
import BasicConfig from './BasicConfig.vue'

const activeTab = ref('basic')

interface MenuItem {
  key: string
  title: string
  description: string
  icon: any
  type: string
}

const menuItems: MenuItem[] = [
  {
    key: 'basic',
    title: '基础配置',
    description: '选择使用的数据库实例和 AI 服务实例',
    icon: Coin,
    type: 'primary'
  },
  {
    key: 'monitor',
    title: '监控与通知',
    description: '配置监控告警和通知方式',
    icon: Bell,
    type: 'warning'
  }
]

function handleMenuClick(key: string) {
  activeTab.value = key
}
</script>

<style scoped>
/* ============================================
   设置中心 - 参考设计风格
============================================ */

.settings-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

/* === 页面头部 === */
.page-header {
  margin-bottom: 32px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left .page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 状态徽章 - 绿色圆点设计 */
.status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  border-radius: var(--radius-full);
  font-size: 14px;
  color: var(--color-success);
  font-weight: 500;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 8px var(--color-success);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* === 顶部大项切换 === */
.top-tabs {
  margin-bottom: 32px;
}

.tabs-container {
  display: flex;
  gap: 8px;
  padding: 4px;
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  width: fit-content;
}

.tab-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 32px;
  border-radius: 12px;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: 14px;
  background: transparent;
  border: 1px solid transparent;
}

.tab-item:hover {
  color: var(--color-text-primary);
}

.tab-item.active {
  background: var(--color-bg-page);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab-icon {
  font-size: 18px;
}

.tab-text {
  font-size: 14px;
}

/* === 内容区域 === */
.content-area {
  min-height: 400px;
}

/* === 配置区域 === */
.config-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* === 配置卡片 === */
.config-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all var(--transition-base);
}

.config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid var(--color-border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: white;
}

.icon-warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.icon-info {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.card-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}

.card-body {
  padding: 32px 24px;
}

/* === 暗色主题适配 === */
[data-theme="dark"] .status-badge {
  background: rgba(16, 185, 129, 0.15);
  border-color: rgba(16, 185, 129, 0.3);
  color: #34d399;
}

[data-theme="dark"] .status-dot {
  background: #34d399;
  box-shadow: 0 0 8px #34d399;
}

[data-theme="dark"] .tabs-container {
  background: #1a1a1a;
  border-color: #262626;
}

[data-theme="dark"] .tab-item {
  color: #a3a3a3;
}

[data-theme="dark"] .tab-item:hover {
  color: #d4d4d4;
}

[data-theme="dark"] .tab-item.active {
  background: #2a2a2a;
  color: #818cf8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

[data-theme="dark"] .config-card {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] .config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

[data-theme="dark"] .card-header {
  border-bottom-color: #262626;
}

[data-theme="dark"] .icon-warning {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
}

[data-theme="dark"] .icon-info {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}
</style>
