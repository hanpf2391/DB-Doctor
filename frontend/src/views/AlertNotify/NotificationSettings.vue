<template>
  <div class="notification-config">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>通知配置</h1>
      <el-button type="primary" @click="saveConfig" :loading="saving">
        <el-icon><Check /></el-icon> 保存配置
      </el-button>
    </div>

    <!-- 配置卡片 -->
    <div class="config-cards">
      <!-- 邮件通知 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Message /></el-icon>
            <span>邮件通知</span>
            <el-switch v-model="configs.EMAIL.enabled" />
          </div>
        </template>
        <el-form :model="configs.EMAIL" label-width="100px">
          <el-form-item label="SMTP 服务器">
            <el-input v-model="configs.EMAIL.config.host" placeholder="smtp.example.com" />
          </el-form-item>
          <el-form-item label="端口">
            <el-input-number v-model="configs.EMAIL.config.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="configs.EMAIL.config.username" placeholder="your-email@example.com" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="configs.EMAIL.config.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="发件人">
            <el-input v-model="configs.EMAIL.config.from" placeholder="DB-Doctor <noreply@dbdoctor.com>" />
          </el-form-item>
          <el-form-item label="严重收件人">
            <el-input v-model="configs.EMAIL.config.recipients_CRITICAL" placeholder="admin@example.com,ops@example.com" />
          </el-form-item>
          <el-form-item label="警告收件人">
            <el-input v-model="configs.EMAIL.config.recipients_WARNING" placeholder="ops@example.com" />
          </el-form-item>
          <el-form-item>
            <el-button @click="sendTestNotification('EMAIL')" :loading="testing.EMAIL">
              <el-icon><Promotion /></el-icon> 发送测试
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 钉钉通知 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><ChatDotRound /></el-icon>
            <span>钉钉通知</span>
            <el-switch v-model="configs.DINGTALK.enabled" />
          </div>
        </template>
        <el-form :model="configs.DINGTALK" label-width="100px">
          <el-form-item label="Webhook URL">
            <el-input v-model="configs.DINGTALK.config.webhook" placeholder="https://oapi.dingtalk.com/robot/send?access_token=xxx" />
          </el-form-item>
          <el-form-item label="加签密钥">
            <el-input v-model="configs.DINGTALK.config.secret" type="password" show-password placeholder="SECxxxxxxxxxxxx" />
          </el-form-item>
          <el-form-item>
            <el-button @click="sendTestNotification('DINGTALK')" :loading="testing.DINGTALK">
              <el-icon><Promotion /></el-icon> 发送测试
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 飞书通知 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><ChatLineSquare /></el-icon>
            <span>飞书通知</span>
            <el-switch v-model="configs.FEISHU.enabled" />
          </div>
        </template>
        <el-form :model="configs.FEISHU" label-width="100px">
          <el-form-item label="Webhook URL">
            <el-input v-model="configs.FEISHU.config.webhook" placeholder="https://open.feishu.cn/open-apis/bot/v2/hook/xxx" />
          </el-form-item>
          <el-form-item>
            <el-button @click="sendTestNotification('FEISHU')" :loading="testing.FEISHU">
              <el-icon><Promotion /></el-icon> 发送测试
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 企业微信通知 -->
      <el-card class="config-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><ChatLineRound /></el-icon>
            <span>企业微信通知</span>
            <el-switch v-model="configs.WECOM.enabled" />
          </div>
        </template>
        <el-form :model="configs.WECOM" label-width="100px">
          <el-form-item label="Webhook URL">
            <el-input v-model="configs.WECOM.config.webhook" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx" />
          </el-form-item>
          <el-form-item>
            <el-button @click="sendTestNotification('WECOM')" :loading="testing.WECOM">
              <el-icon><Promotion /></el-icon> 发送测试
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 定时批量通知配置 -->
      <el-card class="config-card schedule-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon><Clock /></el-icon>
            <span>定时批量通知配置</span>
          </div>
        </template>
        <el-form :model="scheduleConfig" label-width="120px">
          <el-form-item label="执行频率">
            <el-select
              v-model="scheduleConfig.batchCron"
              placeholder="选择执行频率"
              style="width: 100%"
            >
              <el-option
                v-for="option in cronOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              >
                <div style="display: flex; flex-direction: column; gap: 4px;">
                  <span>{{ option.label }}</span>
                  <span style="font-size: 12px; color: var(--el-text-color-secondary);">{{ option.description }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="执行说明">
            <el-text type="info" size="small">
              {{ scheduleConfig.cronDescription || '请在上方选择执行频率' }}
            </el-text>
          </el-form-item>

          <el-form-item label="启用渠道">
            <el-checkbox-group v-model="scheduleConfig.enabledChannels">
              <el-checkbox
                v-for="channel in channelOptions"
                :key="channel.value"
                :value="channel.value"
                :disabled="!configs[channel.value]?.enabled"
              >
                {{ channel.label }}
                <el-tooltip v-if="!configs[channel.value]?.enabled" content="该渠道未启用" placement="top">
                  <el-icon style="margin-left: 4px; color: var(--el-color-warning);"><Warning /></el-icon>
                </el-tooltip>
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="下次执行时间">
            <el-text v-if="scheduleConfig.nextExecutionTime">
              {{ new Date(scheduleConfig.nextExecutionTime).toLocaleString('zh-CN') }}
            </el-text>
            <el-text v-else type="info">暂无</el-text>
          </el-form-item>

          <el-form-item label="上次执行时间">
            <el-text v-if="scheduleConfig.lastExecutionTime">
              {{ new Date(scheduleConfig.lastExecutionTime).toLocaleString('zh-CN') }}
            </el-text>
            <el-text v-else type="info">暂无执行记录</el-text>
          </el-form-item>

          <el-form-item>
            <div style="display: flex; gap: 12px;">
              <el-button
                type="primary"
                @click="updateScheduleConfig"
                :loading="scheduleSaving"
              >
                <el-icon><Check /></el-icon> 保存配置
              </el-button>
              <el-button
                @click="triggerSchedule"
                :loading="scheduleTriggering"
                :disabled="scheduleConfig.enabledChannels.length === 0"
              >
                <el-icon><Refresh /></el-icon> 立即触发
              </el-button>
              <el-button @click="() => ElMessage.info('日志查看功能待实现')">
                <el-icon><View /></el-icon> 查看日志
              </el-button>
            </div>
          </el-form-item>

          <el-alert
            title="提示"
            type="success"
            :closable="false"
            show-icon
          >
            <template #default>
              <div style="font-size: 12px;">
                • 配置修改后自动热加载，无需重启应用
                <br>
                • 定时任务会批量发送所有等待中的通知，请谨慎设置执行频率
                <br>
                • 建议在非高峰时段执行定时批量通知
              </div>
            </template>
          </el-alert>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, Message, ChatDotRound, ChatLineSquare, ChatLineRound, Promotion, Clock, Refresh, Warning, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationConfigApi } from '@/api/monitoring'
import { notificationScheduleApi } from '@/api/notificationSchedule'

// 响应式数据
const saving = ref(false)
const testing = ref({
  EMAIL: false,
  DINGTALK: false,
  FEISHU: false,
  WECOM: false
})

// 定时批量通知配置
const scheduleConfig = ref({
  batchCron: '0 0 * * * ?',
  cronDescription: '',
  enabledChannels: [] as string[],
  nextExecutionTime: '',
  lastExecutionTime: null as string | null
})
const scheduleSaving = ref(false)
const scheduleTriggering = ref(false)

// Cron 表达式预设选项
const cronOptions = [
  { value: '0 0 * * * ?', label: '每小时', description: '每小时的 0 分 0 秒执行' },
  { value: '0 0 */2 * * ?', label: '每 2 小时', description: '每 2 小时的 0 分 0 秒执行' },
  { value: '0 0 */6 * * ?', label: '每 6 小时', description: '每 6 小时的 0 分 0 秒执行' },
  { value: '0 0 9 * * ?', label: '每天上午 9 点', description: '每天 9:00 执行' },
  { value: '0 0 18 * * ?', label: '每天下午 18 点', description: '每天 18:00 执行' },
  { value: '0 0 9,18 * * ?', label: '每天 9 点和 18 点', description: '每天 9:00 和 18:00 执行' },
  { value: '0 0 9 * * MON', label: '每周一上午 9 点', description: '每周一 9:00 执行' }
]

// 可用的通知渠道
const channelOptions = [
  { value: 'EMAIL', label: '邮件' },
  { value: 'DINGTALK', label: '钉钉' },
  { value: 'WECOM', label: '企业微信' },
  { value: 'FEISHU', label: '飞书' }
]

const configs = ref<any>({
  EMAIL: {
    channel: 'EMAIL',
    enabled: false,
    config: {
      host: '',
      port: 587,
      username: '',
      password: '',
      from: '',
      recipients_CRITICAL: '',
      recipients_WARNING: '',
      recipients_INFO: ''
    },
    severityLevels: 'CRITICAL,WARNING,INFO'
  },
  DINGTALK: {
    channel: 'DINGTALK',
    enabled: false,
    config: {
      webhook: '',
      secret: ''
    },
    severityLevels: 'CRITICAL,WARNING'
  },
  FEISHU: {
    channel: 'FEISHU',
    enabled: false,
    config: {
      webhook: ''
    },
    severityLevels: 'CRITICAL,WARNING'
  },
  WECOM: {
    channel: 'WECOM',
    enabled: false,
    config: {
      webhook: ''
    },
    severityLevels: 'CRITICAL,WARNING'
  }
})

// 加载配置
const loadConfig = async () => {
  try {
    const res = await notificationConfigApi.getConfig()
    if (res.code === 'SUCCESS' && res.data.channels) {
      res.data.channels.forEach((ch: any) => {
        if (configs.value[ch.channel]) {
          configs.value[ch.channel] = ch
        }
      })
    }
  } catch (error: any) {
    console.error('加载配置失败:', error)
    ElMessage.error(error.message || '加载配置失败')
  }
}

// 保存配置
const saveConfig = async () => {
  saving.value = true
  try {
    const channels = Object.values(configs.value).map(ch => ({
      channel: ch.channel,
      enabled: ch.enabled,
      config: JSON.stringify(ch.config),
      severityLevels: ch.severityLevels
    }))

    const res = await notificationConfigApi.updateConfig({ channels })

    if (res.code === 'SUCCESS') {
      ElMessage.success('保存成功')
    }
  } catch (error: any) {
    console.error('保存配置失败:', error)
    ElMessage.error(error.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

// 发送测试通知
const sendTestNotification = async (channel: string) => {
  testing.value[channel] = true
  try {
    const res = await notificationConfigApi.sendTestNotification({ channel })

    if (res.code === 'SUCCESS') {
      ElMessage.success('测试通知发送成功')
    } else {
      ElMessage.error(res.message || '测试通知发送失败')
    }
  } catch (error: any) {
    console.error('发送测试通知失败:', error)
    ElMessage.error(error.message || '发送测试通知失败')
  } finally {
    testing.value[channel] = false
  }
}

// 加载定时批量通知配置
const loadScheduleConfig = async () => {
  try {
    const res = await notificationScheduleApi.getConfig()
    if (res.code === 'SUCCESS' && res.data) {
      scheduleConfig.value = {
        batchCron: res.data.batchCron || '0 0 * * * ?',
        cronDescription: res.data.cronDescription || '',
        enabledChannels: res.data.enabledChannels || [],
        nextExecutionTime: res.data.nextExecutionTime || '',
        lastExecutionTime: res.data.lastExecutionTime
      }
    }
  } catch (error: any) {
    console.error('加载定时配置失败:', error)
  }
}

// 更新定时批量通知配置
const updateScheduleConfig = async () => {
  scheduleSaving.value = true
  try {
    const res = await notificationScheduleApi.updateConfig({
      batchCron: scheduleConfig.value.batchCron,
      enabledChannels: scheduleConfig.value.enabledChannels
    })

    if (res.code === 'SUCCESS') {
      ElMessage.success('定时批量通知配置已保存')
      scheduleConfig.value.cronDescription = res.data.cronDescription
      scheduleConfig.value.nextExecutionTime = res.data.nextExecutionTime
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error: any) {
    console.error('保存定时配置失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    scheduleSaving.value = false
  }
}

// 手动触发定时任务
const triggerSchedule = async () => {
  scheduleTriggering.value = true
  try {
    const res = await notificationScheduleApi.triggerNow({
      reason: '手动触发'
    })

    if (res.code === 'SUCCESS') {
      ElMessage.success('定时任务已触发')
    } else {
      ElMessage.error(res.message || '触发失败')
    }
  } catch (error: any) {
    console.error('触发定时任务失败:', error)
    ElMessage.error(error.message || '触发失败')
  } finally {
    scheduleTriggering.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadScheduleConfig()
})
</script>

<style scoped lang="scss">
.notification-config {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .config-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 20px;
  }

  .config-card {
    .card-header {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 600;

      .el-icon {
        font-size: 20px;
      }
    }

    &.schedule-card {
      grid-column: 1 / -1;
    }
  }

  .el-form {
    margin-top: 20px;
  }

  :deep(.el-select-dropdown__item) {
    height: auto;
    padding: 8px 12px;
  }
}
</style>
