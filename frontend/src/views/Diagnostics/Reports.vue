<template>
  <div class="reports-page">
    <PageHeader
      title="历史报告库"
      subtitle="查看所有慢查询诊断历史报告，追溯问题根源"
      :icon="Document"
      :breadcrumb="[
        { title: '慢查询报表', path: '/reports' },
        { title: '历史报告库', path: '/diagnostics/reports' }
      ]"
    >
      <template #extra>
        <el-button type="primary" class="btn-gradient">
          <el-icon><Bottom /></el-icon>
          导出报告
        </el-button>
      </template>
    </PageHeader>

    <PageCard>
      <el-empty description="暂无历史报告" />
    </PageCard>
  </div>
</template>

<script setup lang="ts">
import { Document, Bottom } from '@element-plus/icons-vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PageCard from '@/components/base/PageCard.vue'
</script>

<style scoped>
.reports-page {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}
</style>
