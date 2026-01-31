<template>
  <el-tooltip
    :content="fullSql"
    placement="top"
    effect="light"
    :hide-after="0"
  >
    <div class="sql-truncated">
      <code v-html="highlightedSql"></code>
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import hljs from 'highlight.js'

const props = defineProps<{
  sql: string
  maxLength?: number
}>()

const fullSql = computed(() => props.sql.trim())

const truncatedSql = computed(() => {
  const maxLen = props.maxLength || 80
  if (fullSql.value.length <= maxLen) return fullSql.value
  return fullSql.value.substring(0, maxLen) + '...'
})

const highlightedSql = computed(() => {
  // 简单的 SQL 关键字高亮
  let sql = truncatedSql.value
  const keywords = ['SELECT', 'FROM', 'WHERE', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN',
                    'ORDER BY', 'GROUP BY', 'HAVING', 'LIMIT', 'OFFSET', 'AND', 'OR', 'NOT',
                    'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'DROP', 'ALTER', 'INDEX']

  keywords.forEach(keyword => {
    const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
    sql = sql.replace(regex, `<span class="sql-keyword">${keyword}</span>`)
  })

  return sql
})
</script>

<style scoped>
.sql-truncated {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.sql-truncated code {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #333;
}

:deep(.sql-keyword) {
  color: #409eff;
  font-weight: bold;
}
</style>
