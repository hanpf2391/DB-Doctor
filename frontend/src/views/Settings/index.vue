<template>
  <div class="settings-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>设置中心</h3>
        </div>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="目标数据库" name="target-db">
          <div class="tab-content">
            <p>目标数据库配置页面 - 开发中...</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="AI 大脑配置" name="ai">
          <div class="tab-content">
            <p>AI 配置页面 - 开发中...</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="监控与通知" name="monitor-notify">
          <div class="tab-content">
            <p>监控与通知配置页面 - 开发中...</p>
          </div>
        </el-tab-pane>

        <el-tab-pane label="系统维护" name="maintenance">
          <div class="tab-content">
            <p>系统维护页面 - 开发中...</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const activeTab = ref('target-db')
</script>

<style scoped>
.settings-container {
  max-width: 900px;
  margin: 0 auto;
}

.card-header h3 {
  margin: 0;
}

.tab-content {
  padding: 20px 0;
  min-height: 200px;
}
</style>
