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
          <TargetDb />
        </el-tab-pane>

        <el-tab-pane label="数据库实例管理" name="database-instances">
          <DatabaseInstances />
        </el-tab-pane>

        <el-tab-pane label="AI 大脑配置" name="ai">
          <AiConfig />
        </el-tab-pane>

        <el-tab-pane label="AI 服务实例管理" name="ai-service-instances">
          <AiServiceInstances />
        </el-tab-pane>

        <el-tab-pane label="监控与通知" name="monitor-notify">
          <MonitorNotify />
        </el-tab-pane>

        <el-tab-pane label="系统维护" name="maintenance">
          <Maintenance />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TargetDb from './TargetDb.vue'
import DatabaseInstances from './DatabaseInstances.vue'
import AiConfig from './AiConfig.vue'
import AiServiceInstances from './AiServiceInstances.vue'
import MonitorNotify from './MonitorNotify.vue'
import Maintenance from './Maintenance.vue'

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
