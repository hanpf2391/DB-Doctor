<template>
  <span class="severity-badge" :class="`severity-${severity}`">
    <el-icon class="badge-icon">
      <component :is="iconComponent" />
    </el-icon>
    <span class="badge-text">{{ config.label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  WarningFilled,
  Warning,
  CircleCheckFilled
} from '@element-plus/icons-vue'

interface Props {
  severity: 'critical' | 'warning' | 'optimized'
}

const props = defineProps<Props>()

const severityConfig = {
  critical: {
    label: '严重',
    icon: WarningFilled,
    color: '#EF4444',
    bgColor: '#FEE2E2'
  },
  warning: {
    label: '警告',
    icon: Warning,
    color: '#F59E0B',
    bgColor: '#FEF3C7'
  },
  optimized: {
    label: '已优化',
    icon: CircleCheckFilled,
    color: '#10B981',
    bgColor: '#D1FAE5'
  }
}

const config = computed(() => severityConfig[props.severity])
const iconComponent = computed(() => config.value.icon)
</script>

<style scoped>
.severity-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
}

.severity-critical {
  background: #FEE2E2;
  color: #991B1B;
  border-color: #FECACA;
}

.severity-warning {
  background: #FEF3C7;
  color: #92400E;
  border-color: #FDE68A;
}

.severity-optimized {
  background: #D1FAE5;
  color: #065F46;
  border-color: #A7F3D0;
}

.badge-icon {
  font-size: 16px;
}
</style>
