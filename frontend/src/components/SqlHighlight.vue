<template>
  <div class="sql-highlight">
    <div class="sql-header" v-if="showHeader">
      <span class="sql-title">{{ title }}</span>
      <el-button size="small" @click="handleCopy">
        <el-icon><DocumentCopy /></el-icon>
        复制
      </el-button>
    </div>
    <pre><code :class="`language-sql`" v-html="highlightedCode"></code></pre>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = withDefaults(defineProps<{
  code: string
  title?: string
  showHeader?: boolean
}>(), {
  title: 'SQL',
  showHeader: true
})

const highlightedCode = computed(() => {
  return hljs.highlight(props.code, { language: 'sql' }).value
})

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.code)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.sql-highlight {
  margin: 16px 0;
}

.sql-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #f6f8fa;
  border: 1px solid #ddd;
  border-bottom: none;
  border-radius: 6px 6px 0 0;
}

.sql-title {
  font-weight: bold;
  color: #333;
}

pre {
  margin: 0;
  padding: 16px;
  background-color: #f6f8fa;
  border: 1px solid #ddd;
  border-radius: 0 0 6px 6px;
  overflow-x: auto;
}

code {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
}
</style>
