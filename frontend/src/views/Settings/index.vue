<template>
  <div class="settings-container">
    <!-- 页面头部 - 扁平化设计 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon :size="28">
              <Setting />
            </el-icon>
          </div>
          <div class="header-text">
            <h1 class="page-title">设置中心</h1>
            <p class="page-subtitle">配置您的 DB-Doctor 系统</p>
          </div>
        </div>
        <div class="header-right">
          <div class="status-badge">
            <el-icon><CircleCheck /></el-icon>
            <span>系统运行正常</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 扁平化导航菜单 -->
    <div class="navigation-menu">
      <div class="nav-grid">
        <div
          v-for="item in menuItems"
          :key="item.key"
          :class="['nav-item', { active: activeTab === item.key }]"
          @click="handleMenuClick(item.key)"
        >
          <div class="nav-icon" :class="`icon-${item.type}`">
            <el-icon :size="24">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <div class="nav-content">
            <div class="nav-title">{{ item.title }}</div>
            <div class="nav-desc">{{ item.description }}</div>
          </div>
          <div class="nav-arrow">
            <el-icon>
              <ArrowRight />
            </el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 基础配置 -->
      <BasicConfig v-if="activeTab === 'basic'" />

      <!-- 监控与通知 -->
      <div v-else-if="activeTab === 'monitor'">
        <div class="config-section">
          <div class="section-card">
            <div class="section-header">
              <div class="header-icon icon-warning">
                <el-icon :size="24"><Bell /></el-icon>
              </div>
              <div class="header-text">
                <h3 class="section-title">监控配置</h3>
                <p class="section-desc">配置慢查询监控告警规则</p>
              </div>
            </div>
            <div class="section-content">
              <el-empty description="监控配置功能开发中..." />
            </div>
          </div>

          <div class="section-card">
            <div class="section-header">
              <div class="header-icon icon-info">
                <el-icon :size="24"><Message /></el-icon>
              </div>
              <div class="header-text">
                <h3 class="section-title">通知方式</h3>
                <p class="section-desc">配置告警通知渠道（邮件、钉钉、企业微信）</p>
              </div>
            </div>
            <div class="section-content">
              <el-empty description="通知配置功能开发中..." />
            </div>
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
.settings-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--spacing-xl);
}

/* 页面头部 - 扁平化白色卡片 */
.page-header {
  margin-bottom: var(--spacing-lg);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-icon {
  width: 56px;
  height: 56px;
  background: var(--color-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-text {
  color: var(--color-text-primary);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--color-text-primary);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.status-badge .el-icon {
  color: var(--color-primary);
  font-size: 18px;
}

/* 导航菜单 - 扁平化卡片 */
.navigation-menu {
  margin-bottom: var(--spacing-lg);
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--spacing-md);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  border: 1px solid var(--color-border);
  background: var(--color-bg-page);
}

.nav-item:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-hover);
}

.nav-item.active {
  border-color: var(--color-primary);
  background: var(--color-bg-active);
}

.nav-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-base);
  color: white;
}

.icon-primary {
  background: var(--color-primary);
}

.icon-success {
  background: #10b981;
}

.icon-warning {
  background: #f59e0b;
}

.nav-content {
  flex: 1;
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.nav-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.4;
}

.nav-arrow {
  color: var(--color-text-muted);
  transition: all var(--transition-base);
}

.nav-item:hover .nav-arrow {
  color: var(--color-primary);
}

/* 内容区域 */
.content-area {
  min-height: 400px;
}

.instances-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 实例类型切换 - 扁平化标签 */
.instance-type-tabs {
  padding: var(--spacing-sm);
  display: flex;
  gap: var(--spacing-sm);
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.type-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--color-text-secondary);
  font-weight: 500;
  font-size: 14px;
  border: 1px solid transparent;
}

.type-tab:hover {
  background: var(--color-bg-page);
  color: var(--color-primary);
  border-color: var(--color-border);
}

.type-tab.active {
  background: var(--color-bg-page);
  color: var(--color-primary);
  border-color: var(--color-border);
  font-weight: 600;
}

[data-theme="dark"] .instance-type-tabs {
  background: #1a1a1a;
  border-color: #262626;
}

[data-theme="dark"] .type-tab {
  color: #a3a3a3;
  border-color: transparent;
}

[data-theme="dark"] .type-tab:hover {
  background: #2a2a2a;
  color: #818cf8;
  border-color: #262626;
}

[data-theme="dark"] .type-tab.active {
  background: #2a2a2a;
  color: #818cf8;
  border-color: #262626;
}

/* 配置区域 */
.config-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.section-card {
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
}

.section-header {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border-light);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-primary {
  background: var(--color-primary);
  color: white;
}

.icon-success {
  background: #10b981;
  color: white;
}

.icon-warning {
  background: #f59e0b;
  color: white;
}

.icon-info {
  background: #3b82f6;
  color: white;
}

.header-text {
  flex: 1;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--color-text-primary);
}

.section-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.section-content {
  padding: var(--spacing-md) 0;
}
</style>
