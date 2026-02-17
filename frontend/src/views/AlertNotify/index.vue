<template>
  <div class="alert-notify-container">
    <!-- 标签页导航 -->
    <div class="tabs-wrapper">
      <el-tabs v-model="activeTab" class="alert-notify-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="告警历史" name="alert-history" />
        <el-tab-pane label="通知设置" name="notification-settings" />
        <el-tab-pane label="通知历史" name="notification-history" />
      </el-tabs>
    </div>

    <!-- 页面内容 -->
    <div class="content-wrapper">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const activeTab = ref('alert-history')

// 监听路由变化，更新标签页
watch(() => route.path, (newPath) => {
  if (newPath.includes('/notification-history')) {
    activeTab.value = 'notification-history'
  } else if (newPath.includes('/notification')) {
    activeTab.value = 'notification-settings'
  } else {
    activeTab.value = 'alert-history'
  }
}, { immediate: true })

// 标签页切换
const handleTabChange = (tabName: string) => {
  if (tabName === 'alert-history') {
    router.push('/alert-notify/history')
  } else if (tabName === 'notification-settings') {
    router.push('/alert-notify/notification')
  } else if (tabName === 'notification-history') {
    router.push('/alert-notify/notification-history')
  }
}
</script>

<style scoped>
.alert-notify-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f7f8fa;
}

.tabs-wrapper {
  flex-shrink: 0;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.alert-notify-tabs {
  padding: 0 32px;
}

.alert-notify-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.alert-notify-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.alert-notify-tabs :deep(.el-tabs__item) {
  height: 48px;
  line-height: 48px;
  font-size: 15px;
  font-weight: 500;
  color: #6b7280;
  padding: 0 20px;
}

.alert-notify-tabs :deep(.el-tabs__item.is-active) {
  color: #3b82f6;
  font-weight: 600;
}

.alert-notify-tabs :deep(.el-tabs__active-bar) {
  background-color: #3b82f6;
  height: 3px;
}

.content-wrapper {
  flex: 1;
  overflow: auto;
}

/* 页面过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
