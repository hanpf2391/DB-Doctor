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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Check, Message, ChatDotRound, ChatLineSquare, ChatLineRound, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { notificationConfigApi } from '@/api/monitoring'

// 响应式数据
const saving = ref(false)
const testing = ref({
  EMAIL: false,
  DINGTALK: false,
  FEISHU: false,
  WECOM: false
})

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
    const channels = Object.values(configs.value || {}).map(ch => ({
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

onMounted(() => {
  loadConfig()
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
  }

  .el-form {
    margin-top: 20px;
  }
}
</style>
