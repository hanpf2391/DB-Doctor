<template>
  <div class="alert-settings">
    <el-form :model="form" label-width="180px">
      <!-- 严重程度阈值 -->
      <el-form-item>
        <template #label>
          <span>严重程度阈值（秒）</span>
          <el-tooltip
            content="平均查询耗时低于此值不发送通知"
            placement="top"
          >
            <el-icon class="tooltip-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </template>
        <el-input-number
          v-model="form.severityThreshold"
          :min="1.0"
          :max="10.0"
          :step="0.1"
          :precision="1"
          @change="handleUpdate"
        />
        <span class="range-hint">范围: 1.0 - 10.0</span>
      </el-form-item>

      <!-- 冷却期 -->
      <el-form-item>
        <template #label>
          <span>冷却期（小时）</span>
          <el-tooltip
            content="同一 SQL 两次通知的最小间隔时间"
            placement="top"
          >
            <el-icon class="tooltip-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </template>
        <el-input-number
          v-model="form.coolDownHours"
          :min="1"
          :max="168"
          :step="1"
          @change="handleUpdate"
        />
        <span class="range-hint">范围: 1 - 168（1小时-1周）</span>
      </el-form-item>

      <!-- 性能恶化倍率 -->
      <el-form-item>
        <template #label>
          <span>性能恶化倍率</span>
          <el-tooltip
            content="当前耗时/上次通知耗时 >= 此值时触发二次唤醒通知"
            placement="top"
          >
            <el-icon class="tooltip-icon"><QuestionFilled /></el-icon>
          </el-tooltip>
        </template>
        <el-input-number
          v-model="form.degradationMultiplier"
          :min="1.1"
          :max="10.0"
          :step="0.1"
          :precision="1"
          @change="handleUpdate"
        />
        <span class="range-hint">范围: 1.1 - 10.0</span>
      </el-form-item>
    </el-form>

    <!-- 重置按钮 -->
    <div class="actions">
      <el-button @click="handleReset">重置为默认值</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { alertSettingsApi } from '@/api/alertSettings'

const form = ref({
  severityThreshold: 3.0,
  coolDownHours: 1,
  degradationMultiplier: 1.5
})

// 加载配置
const loadSettings = async () => {
  try {
    const res = await alertSettingsApi.getSettings()
    if (res.code === 'SUCCESS') {
      form.value = res.data
    }
  } catch (error) {
    console.error('加载告警设置失败:', error)
  }
}

// 更新配置
const handleUpdate = async () => {
  try {
    await alertSettingsApi.updateSettings(form.value)
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

// 重置为默认值
const handleReset = async () => {
  try {
    await ElMessageBox.confirm(
      '确认将告警设置重置为默认值？',
      '重置确认',
      {
        type: 'warning',
        confirmButtonText: '重置',
        cancelButtonText: '取消'
      }
    )

    const res = await alertSettingsApi.resetSettings()
    if (res.code === 'SUCCESS') {
      form.value = res.data
      ElMessage.success('已重置为默认值')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重置失败')
    }
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped lang="scss">
.alert-settings {
  .tooltip-icon {
    margin-left: 4px;
    cursor: help;
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }

  .range-hint {
    margin-left: 12px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .actions {
    margin-top: 24px;
    padding-left: 180px;
  }
}
</style>
