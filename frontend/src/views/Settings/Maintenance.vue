<template>
  <div class="maintenance-page">
    <el-alert
      title="危险操作"
      type="warning"
      description="以下操作不可逆，请谨慎操作"
      :closable="false"
      show-icon
      style="margin-bottom: 20px"
    />

    <el-card shadow="hover">
      <template #header>
        <h3>数据清理</h3>
      </template>

      <el-form label-width="150px">
        <el-form-item label="清理历史数据">
          <el-input-number
            v-model="cleanupDays"
            :min="1"
            :max="365"
            placeholder="天数"
          />
          <span class="form-tip">清理指定天数之前的历史数据</span>
          <el-button
            type="danger"
            @click="cleanupHistory"
            :loading="cleaning"
            style="margin-left: 10px"
          >
            清理历史数据
          </el-button>
        </el-form-item>

        <el-form-item label="重置系统">
          <span class="form-tip">清空所有指纹、样本和分析报告（危险操作）</span>
          <el-button
            type="danger"
            plain
            @click="confirmReset"
            :loading="resetting"
            style="margin-left: 10px"
          >
            清空所有数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <h3>关于 DB-Doctor</h3>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前版本">
          {{ systemInfo.version }}
        </el-descriptions-item>
        <el-descriptions-item label="构建时间">
          {{ systemInfo.buildTime }}
        </el-descriptions-item>
        <el-descriptions-item label="Git Commit" :span="2">
          <code>{{ systemInfo.gitCommit }}</code>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cleanupHistory as cleanupHistoryApi, resetSystem as resetSystemApi, getSystemInfo } from '@/api/config'

const cleanupDays = ref(7)
const cleaning = ref(false)
const resetting = ref(false)

const systemInfo = reactive({
  version: 'v2.2.0',
  buildTime: '2026-01-31 12:00:00',
  gitCommit: '0faa4a7'
})

/**
 * 清理历史数据
 */
async function cleanupHistory() {
  try {
    await ElMessageBox.confirm(
      `确定要清理 ${cleanupDays.value} 天前的历史数据吗？此操作不可逆。`,
      '确认清理',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )

    cleaning.value = true
    const result = await cleanupHistoryApi(cleanupDays.value)
    ElMessage.success(result.message || `已清理 ${result.deletedTemplates} 条模板, ${result.deletedSamples} 条样本`)
  } catch (error) {
    // 用户取消
  } finally {
    cleaning.value = false
  }
}

/**
 * 重置系统（需输入 "RESET" 确认）
 */
async function confirmReset() {
  try {
    await ElMessageBox.prompt(
      '此操作将清空所有数据，请输入 "RESET" 确认',
      '危险操作确认',
      {
        type: 'error',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^RESET$/,
        inputErrorMessage: '输入错误，请输入 "RESET"'
      }
    )

    resetting.value = true
    await resetSystemApi()
    ElMessage.success('系统已重置，所有数据已清空')
  } catch (error) {
    // 用户取消或输入错误
  } finally {
    resetting.value = false
  }
}

/**
 * 加载系统信息
 */
async function loadSystemInfo() {
  try {
    const result = await getSystemInfo()
    Object.assign(systemInfo, result)
  } catch (error) {
    console.error('加载系统信息失败', error)
  }
}

onMounted(() => {
  loadSystemInfo()
})
</script>

<style scoped>
.maintenance-page {
  max-width: 700px;
}

.form-tip {
  margin-right: 10px;
  font-size: 12px;
  color: #909399;
}

code {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
}
</style>
