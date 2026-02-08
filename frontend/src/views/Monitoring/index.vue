<template>
  <div class="monitoring-container">
    <!-- 监控中心页面容器 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
// 监控中心主容器页面
</script>

<style scoped>
.monitoring-container {
  width: 100%;
  height: 100%;
  min-height: 100vh;
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
