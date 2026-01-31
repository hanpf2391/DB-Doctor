<template>
  <el-tag :type="tagType" :class="{ 'is-loading': status === 'DIAGNOSING' }">
    <el-icon v-if="status === 'DIAGNOSING'" class="loading-icon">
      <Loading />
    </el-icon>
    {{ statusText }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps<{
  status: string
}>()

const statusText = computed(() => {
  const statusMap: Record<string, string> = {
    'SUCCESS': '✅ 诊断完成',
    'COMPLETED': '✅ 诊断完成',
    'FAILED': '❌ 诊断失败',
    'DIAGNOSING': '👨⚕️ 正在会诊...',
    'PENDING': '⏳ 排队中',
    'ANALYZING': '🔬 分析中...'
  }
  return statusMap[props.status] || props.status
})

const tagType = computed(() => {
  const typeMap: Record<string, 'success' | 'danger' | 'warning' | 'info'> = {
    'SUCCESS': 'success',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'DIAGNOSING': 'warning',
    'PENDING': 'info',
    'ANALYZING': 'warning'
  }
  return typeMap[props.status] || 'info'
})
</script>

<style scoped>
.is-loading {
  position: relative;
}

.loading-icon {
  margin-right: 4px;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
