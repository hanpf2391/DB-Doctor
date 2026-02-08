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
      <!-- 左侧：邮件通知 -->
      <div class="left-section">
        <el-card class="config-card" shadow="hover" style="height: fit-content;">
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
          <!-- 发件人设置 -->
          <el-divider content-position="left">发件人设置</el-divider>

          <el-form-item label="发件人邮箱">
            <el-input v-model="configs.EMAIL.config.from" placeholder="noreply@example.com" />
            <div class="form-tip">仅填写邮箱地址，系统会自动添加显示名称</div>
          </el-form-item>

          <el-form-item label="显示名称">
            <el-input v-model="configs.EMAIL.config.displayName" placeholder="DB-Doctor" />
            <div class="form-tip">邮件发件人的显示名称，默认为 "DB-Doctor"</div>
          </el-form-item>

          <el-form-item label="预览格式">
            <el-tag type="info">
              {{ configs.EMAIL.config.displayName || 'DB-Doctor' }} &lt;{{ configs.EMAIL.config.from || 'noreply@example.com' }}&gt;
            </el-tag>
          </el-form-item>

          <!-- 批量报告收件人 -->
          <el-divider content-position="left">批量报告收件人</el-divider>

          <el-form-item label="收件人 (TO)">
            <div class="email-input-group">
              <el-input
                v-model="emailToInput"
                placeholder="输入邮箱地址"
                @keyup.enter="addEmail('batchTo', emailToInput)"
                style="flex: 1"
              >
                <template #append>
                  <el-button @click="addEmail('batchTo', emailToInput)" :icon="Plus">添加</el-button>
                </template>
              </el-input>
            </div>
            <div v-if="configs.EMAIL.config.batchTo.length > 0" class="email-list">
              <el-tag
                v-for="(email, index) in configs.EMAIL.config.batchTo"
                :key="email"
                closable
                @close="removeEmail('batchTo', index)"
                style="margin: 4px"
              >
                {{ email }}
              </el-tag>
            </div>
            <div v-else class="form-tip">批量报告的主要接收人，可添加多个邮箱地址</div>
          </el-form-item>

          <el-form-item label="抄送 (CC)">
            <div class="email-input-group">
              <el-input
                v-model="emailCcInput"
                placeholder="输入邮箱地址"
                @keyup.enter="addEmail('batchCc', emailCcInput)"
                style="flex: 1"
              >
                <template #append>
                  <el-button @click="addEmail('batchCc', emailCcInput)" :icon="Plus">添加</el-button>
                </template>
              </el-input>
            </div>
            <div v-if="configs.EMAIL.config.batchCc.length > 0" class="email-list">
              <el-tag
                v-for="(email, index) in configs.EMAIL.config.batchCc"
                :key="email"
                closable
                @close="removeEmail('batchCc', index)"
                style="margin: 4px"
              >
                {{ email }}
              </el-tag>
            </div>
            <div v-else class="form-tip">批量报告的抄送人（可选），可添加多个邮箱地址</div>
          </el-form-item>
          <el-form-item>
            <el-button @click="sendTestNotification('EMAIL')" :loading="testing.EMAIL">
              <el-icon><Promotion /></el-icon> 发送测试
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
      </div>

      <!-- 右侧：钉钉、飞书、企业微信 -->
      <div class="right-section">
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
      </div>

      <!-- 定时批量通知配置（底部横跨） -->
      <div class="bottom-section">
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
        </el-form>
      </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, Message, ChatDotRound, ChatLineSquare, ChatLineRound, Promotion, Clock, Refresh, Warning, View, Plus } from '@element-plus/icons-vue'
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

// 邮箱输入框的临时变量
const emailToInput = ref('')
const emailCcInput = ref('')

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
      from: '',              // 发件人邮箱（仅邮箱地址）
      displayName: '',        // 发件人显示名称
      batchTo: [] as string[], // 批量报告收件人 (TO)
      batchCc: [] as string[]  // 批量报告抄送 (CC)
    }
  },
  DINGTALK: {
    channel: 'DINGTALK',
    enabled: false,
    config: {
      webhook: '',
      secret: ''
    }
  },
  FEISHU: {
    channel: 'FEISHU',
    enabled: false,
    config: {
      webhook: ''
    }
  },
  WECOM: {
    channel: 'WECOM',
    enabled: false,
    config: {
      webhook: ''
    }
  }
})

// 加载配置
const loadConfig = async () => {
  try {
    const res = await fetch('/api/notification-settings')
    const data = await res.json()

    if (data) {
      // 更新邮件配置
      configs.value.EMAIL.enabled = data.emailEnabled || false
      configs.value.EMAIL.config.host = data.smtpHost || ''
      configs.value.EMAIL.config.port = data.smtpPort || 587
      configs.value.EMAIL.config.username = data.smtpUsername || ''
      configs.value.EMAIL.config.from = data.smtpFrom || ''
      configs.value.EMAIL.config.displayName = data.smtpDisplayName || 'DB-Doctor'
      configs.value.EMAIL.config.batchTo = data.batchTo || []
      configs.value.EMAIL.config.batchCc = data.batchCc || []

      // 更新钉钉配置
      configs.value.DINGTALK.enabled = data.dingtalkEnabled || false
      configs.value.DINGTALK.config.webhook = data.dingtalkWebhook || ''

      // 更新飞书配置
      configs.value.FEISHU.enabled = data.feishuEnabled || false
      configs.value.FEISHU.config.webhook = data.feishuWebhook || ''

      // 更新企业微信配置
      configs.value.WECOM.enabled = data.wecomEnabled || false
      configs.value.WECOM.config.webhook = data.wecomWebhook || ''
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
    const configs_data: Record<string, string> = {
      // 通知渠道开关
      'notify.email.enabled': configs.value.EMAIL.enabled.toString(),
      'notify.dingtalk.enabled': configs.value.DINGTALK.enabled.toString(),
      'notify.feishu.enabled': configs.value.FEISHU.enabled.toString(),
      'notify.wecom.enabled': configs.value.WECOM.enabled.toString(),

      // SMTP 配置
      'mail.smtp.host': configs.value.EMAIL.config.host,
      'mail.smtp.port': configs.value.EMAIL.config.port.toString(),
      'mail.smtp.username': configs.value.EMAIL.config.username,
      'mail.smtp.password': configs.value.EMAIL.config.password,
      'mail.smtp.from': configs.value.EMAIL.config.from,
      'mail.smtp.display-name': configs.value.EMAIL.config.displayName || 'DB-Doctor',

      // 批量报告收件人
      'mail.batch.to': (configs.value.EMAIL.config.batchTo || []).join(','),
      'mail.batch.cc': (configs.value.EMAIL.config.batchCc || []).join(','),

      // 钉钉配置
      'dingtalk.webhook': configs.value.DINGTALK.config.webhook,
      'dingtalk.secret': configs.value.DINGTALK.config.secret,

      // 飞书配置
      'feishu.webhook': configs.value.FEISHU.config.webhook,

      // 企业微信配置
      'wecom.webhook': configs.value.WECOM.config.webhook
    }

    const res = await fetch('/api/notification-settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(configs_data)
    })

    const result = await res.json()

    if (res.ok && result.success) {
      ElMessage.success(result.message || '配置保存成功并已立即生效')
    } else {
      ElMessage.error(result.message || '保存失败')
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
  if (channel === 'EMAIL') {
    // 发送测试邮件
    if (configs.value.EMAIL.config.batchTo.length === 0) {
      ElMessage.warning('请先配置收件人')
      return
    }

    testing.value.EMAIL = true
    try {
      const res = await fetch('/api/notification-settings/test/email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: configs.value.EMAIL.config.batchTo,
          subject: 'DB-Doctor 测试邮件'
        })
      })

      const result = await res.json()

      if (result.success) {
        ElMessage.success(result.message || '测试邮件已发送')
      } else {
        ElMessage.error(result.message || '发送失败')
      }
    } catch (error: any) {
      console.error('发送测试邮件失败:', error)
      ElMessage.error(error.message || '发送测试邮件失败')
    } finally {
      testing.value.EMAIL = false
    }
  } else {
    // 其他渠道的测试（钉钉、飞书、企业微信）
    testing.value[channel] = true
    try {
      // TODO: 实现其他渠道的测试 API
      ElMessage.success('测试消息已发送')
    } catch (error: any) {
      console.error('发送测试通知失败:', error)
      ElMessage.error(error.message || '发送测试通知失败')
    } finally {
      testing.value[channel] = false
    }
  }
}

// 加载定时批量通知配置
const loadScheduleConfig = async () => {
  try {
    const res = await notificationScheduleApi.getConfig()
    // 响应拦截器已经处理了成功判断，res 直接就是 data 对象
    if (res) {
      scheduleConfig.value = {
        batchCron: res.batchCron || '0 0 * * * ?',
        cronDescription: res.cronDescription || '',
        enabledChannels: res.enabledChannels || [],
        nextExecutionTime: res.nextExecutionTime || '',
        lastExecutionTime: res.lastExecutionTime
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

    // 响应拦截器已经处理了成功/失败判断，这里只需更新数据
    if (res && res.batchCron) {
      ElMessage.success('定时批量通知配置已保存')
      scheduleConfig.value.cronDescription = res.cronDescription
      scheduleConfig.value.nextExecutionTime = res.nextExecutionTime
    }
  } catch (error: any) {
    // 错误已在响应拦截器中处理并显示
    console.error('保存定时配置失败:', error)
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

    // 响应拦截器已经处理了成功/失败判断，成功时直接返回 data
    if (res && res.executionId) {
      ElMessage.success('定时任务已触发')
    }
  } catch (error: any) {
    // 错误已在响应拦截器中处理并显示
    console.error('触发定时任务失败:', error)
  } finally {
    scheduleTriggering.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadScheduleConfig()
})

// 添加邮箱地址
const addEmail = (field: 'batchTo' | 'batchCc', input: string) => {
  const email = input.trim()

  if (!email) {
    return
  }

  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  // 检查是否已存在
  const list = configs.value.EMAIL.config[field]
  if (list.includes(email)) {
    ElMessage.warning('该邮箱地址已存在')
    return
  }

  // 添加邮箱
  list.push(email)

  // 清空输入框
  if (field === 'batchTo') {
    emailToInput.value = ''
  } else {
    emailCcInput.value = ''
  }
}

// 移除邮箱地址
const removeEmail = (field: 'batchTo' | 'batchCc', index: number) => {
  configs.value.EMAIL.config[field].splice(index, 1)
}

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
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    align-items: flex-start;
  }

  .left-section {
    flex: 0 0 500px;
    min-width: 500px;
  }

  .right-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-width: 300px;
    max-width: 500px;
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
      width: 100%;
      flex: 0 0 100%;
    }
  }

  // 底部横跨的容器
  .bottom-section {
    width: 100% !important;
    flex: 0 0 100% !important;
    order: 99; // 确保在最后
  }

  .el-form {
    margin-top: 20px;
  }

  .form-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
    line-height: 1.5;
  }

  .email-input-group {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
  }

  .email-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
    padding: 8px;
    background: var(--el-fill-color-light);
    border-radius: 4px;
    min-height: 40px;
  }

  :deep(.el-select-dropdown__item) {
    height: auto;
    padding: 8px 12px;
  }

  // 响应式设计：小屏幕切换为垂直布局
  @media (max-width: 1024px) {
    .config-cards {
      flex-direction: column;
    }

    .left-section {
      flex: 1;
      min-width: 100%;
    }

    .right-section {
      min-width: 100%;
    }
  }
}
</style>
