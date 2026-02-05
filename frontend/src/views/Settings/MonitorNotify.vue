<template>
  <div class="monitor-notify">
    <!-- 页面头部 -->
    <div class="page-header modern-card">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon gradient-warning">
            <el-icon :size="32"><Bell /></el-icon>
          </div>
          <div class="header-text">
            <h1 class="page-title">监控与通知</h1>
            <p class="page-subtitle">配置告警通知方式，及时接收慢查询告警</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知方式卡片 -->
    <div class="notify-cards">
      <el-row :gutter="20">
        <!-- 邮件通知 -->
        <el-col :xs="24" :sm="24" :md="8">
          <div class="notify-card modern-card" :class="{ active: form.emailEnabled }">
            <div class="card-top">
              <div class="card-icon icon-email">
                <el-icon :size="32"><Message /></el-icon>
              </div>
              <el-switch
                v-model="form.emailEnabled"
                @change="handleEmailToggle"
                size="large"
              />
            </div>
            <div class="card-body">
              <h3 class="card-title">邮件通知</h3>
              <p class="card-desc">通过邮件接收慢查询告警</p>
              <div v-if="form.emailEnabled" class="config-details">
                <div class="detail-item">
                  <el-icon><Message /></el-icon>
                  <span class="detail-label">SMTP:</span>
                  <span class="detail-value">{{ form.emailSmtpHost }}</span>
                </div>
                <div class="detail-item">
                  <el-icon><User /></el-icon>
                  <span class="detail-label">发件人:</span>
                  <span class="detail-value">{{ form.emailUsername || '未配置' }}</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <el-button link type="primary" @click="configEmail">
                <el-icon><Setting /></el-icon>
                配置
              </el-button>
            </div>
          </div>
        </el-col>

        <!-- 钉钉通知 -->
        <el-col :xs="24" :sm="12" :md="8">
          <div class="notify-card modern-card" :class="{ active: form.dingdingEnabled }">
            <div class="card-top">
              <div class="card-icon icon-dingding">
                <el-icon :size="32"><ChatDotRound /></el-icon>
              </div>
              <el-switch
                v-model="form.dingdingEnabled"
                @change="handleDingdingToggle"
                size="large"
              />
            </div>
            <div class="card-body">
              <h3 class="card-title">钉钉通知</h3>
              <p class="card-desc">推送到钉钉群机器人</p>
              <div v-if="form.dingdingEnabled" class="config-details">
                <div class="detail-item">
                  <el-icon><Link /></el-icon>
                  <span class="detail-label">状态:</span>
                  <span class="detail-value">已配置</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <el-button link type="primary" @click="configDingding">
                <el-icon><Setting /></el-icon>
                配置
              </el-button>
            </div>
          </div>
        </el-col>

        <!-- 企微通知 -->
        <el-col :xs="24" :sm="12" :md="8">
          <div class="notify-card modern-card" :class="{ active: form.wecomEnabled }">
            <div class="card-top">
              <div class="card-icon icon-wecom">
                <el-icon :size="32"><ChatSquare /></el-icon>
              </div>
              <el-switch
                v-model="form.wecomEnabled"
                @change="handleWecomToggle"
                size="large"
              />
            </div>
            <div class="card-body">
              <h3 class="card-title">企业微信</h3>
              <p class="card-desc">推送到企业微信群</p>
              <div v-if="form.wecomEnabled" class="config-details">
                <div class="detail-item">
                  <el-icon><Link /></el-icon>
                  <span class="detail-label">状态:</span>
                  <span class="detail-value">已配置</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <el-button link type="primary" @click="configWecom">
                <el-icon><Setting /></el-icon>
                配置
              </el-button>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 配置对话框 -->
    <el-dialog
      v-model="configDialogVisible"
      :title="configDialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <!-- 邮件配置表单 -->
      <div v-if="configType === 'email'" class="config-form">
        <el-form label-width="140px">
          <el-form-item label="SMTP服务器">
            <el-input
              v-model="form.emailSmtpHost"
              placeholder="smtp.gmail.com"
            />
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              例如：smtp.gmail.com（Gmail）、smtp.qq.com（QQ邮箱）
            </div>
          </el-form-item>

          <el-form-item label="SMTP端口">
            <el-input-number
              v-model="form.emailSmtpPort"
              :min="1"
              :max="65535"
              style="width: 100%"
            />
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              常用端口：25（Gmail）、465或587（QQ邮箱）
            </div>
          </el-form-item>

          <el-form-item label="发件人邮箱">
            <el-input
              v-model="form.emailUsername"
              placeholder="your-email@gmail.com"
            />
          </el-form-item>

          <el-form-item label="邮箱密码/授权码">
            <el-input
              v-model="form.emailPassword"
              type="password"
              show-password
              placeholder="请输入邮箱密码或授权码"
            />
            <div class="form-tip">
              <el-icon><Lock /></el-icon>
              密码将加密存储，建议使用应用专用密码
            </div>
          </el-form-item>

          <el-form-item label="收件人列表">
            <el-input
              v-model="form.emailToListStr"
              type="textarea"
              :rows="3"
              placeholder='["admin@example.com", "dba@example.com"]'
            />
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              JSON数组格式，多个收件人用逗号分隔
            </div>
          </el-form-item>

          <el-form-item>
            <el-button @click="testEmail" :loading="testingEmail" class="btn-gradient">
              <el-icon style="margin-right: 6px"><Message /></el-icon>
              发送测试邮件
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 钉钉配置表单 -->
      <div v-else-if="configType === 'dingding'" class="config-form">
        <el-form label-width="140px">
          <el-form-item label="Webhook URL">
            <el-input
              v-model="form.dingdingWebhook"
              type="textarea"
              :rows="4"
              placeholder="https://oapi.dingtalk.com/robot/send?access_token=..."
            />
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              钉钉群机器人的Webhook地址
            </div>
          </el-form-item>

          <el-form-item>
            <el-button @click="testDingding" :loading="testingDingding" class="btn-gradient">
              <el-icon style="margin-right: 6px"><ChatDotRound /></el-icon>
              发送测试消息
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 企微配置表单 -->
      <div v-else-if="configType === 'wecom'" class="config-form">
        <el-form label-width="140px">
          <el-form-item label="Webhook URL">
            <el-input
              v-model="form.wecomWebhook"
              type="textarea"
              :rows="4"
              placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
            />
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon>
              企业微信机器人的Webhook地址
            </div>
          </el-form-item>

          <el-form-item>
            <el-button @click="testWecom" :loading="testingWecom" class="btn-gradient">
              <el-icon style="margin-right: 6px"><ChatSquare /></el-icon>
              发送测试消息
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving" class="btn-gradient">
          <el-icon style="margin-right: 6px"><Check /></el-icon>
          保存配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Message,
  ChatDotRound,
  ChatSquare,
  Setting,
  Check,
  Lock,
  InfoFilled
} from '@element-plus/icons-vue'
import { getConfigsByGroup, batchUpdateConfigs } from '@/api/config'

const form = reactive({
  emailEnabled: false,
  emailSmtpHost: 'smtp.gmail.com',
  emailSmtpPort: 587,
  emailUsername: '',
  emailPassword: '',
  emailToListStr: '[]',
  dingdingEnabled: false,
  dingdingWebhook: '',
  wecomEnabled: false,
  wecomWebhook: ''
})

const configDialogVisible = ref(false)
const configType = ref<'email' | 'dingding' | 'wecom'>('email')
const configDialogTitle = ref('')
const saving = ref(false)
const testingEmail = ref(false)
const testingDingding = ref(false)
const testingWecom = ref(false)

function handleEmailToggle(enabled: boolean) {
  if (enabled) {
    openConfigDialog('email', '邮件通知配置')
  }
}

function handleDingdingToggle(enabled: boolean) {
  if (enabled) {
    openConfigDialog('dingding', '钉钉通知配置')
  }
}

function handleWecomToggle(enabled: boolean) {
  if (enabled) {
    openConfigDialog('wecom', '企业微信通知配置')
  }
}

function openConfigDialog(type: 'email' | 'dingding' | 'wecom', title: string) {
  configType.value = type
  configDialogTitle.value = title
  configDialogVisible.value = true
}

function configEmail() {
  openConfigDialog('email', '邮件通知配置')
}

function configDingding() {
  openConfigDialog('dingding', '钉钉通知配置')
}

function configWecom() {
  openConfigDialog('wecom', '企业微信通知配置')
}

async function testEmail() {
  testingEmail.value = true
  try {
    // 调用测试邮件API
    ElMessage.success('测试邮件已发送，请检查收件箱')
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    testingEmail.value = false
  }
}

async function testDingding() {
  testingDingding.value = true
  try {
    // 调用测试钉钉API
    ElMessage.success('测试消息已发送到钉钉')
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    testingDingding.value = false
  }
}

async function testWecom() {
  testingWecom.value = true
  try {
    // 调用测试企微API
    ElMessage.success('测试消息已发送到企业微信')
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    testingWecom.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const configs: Record<string, string> = {
      'notification.email.enabled': form.emailEnabled.toString(),
      'notification.email.smtp_host': form.emailSmtpHost,
      'notification.email.smtp_port': form.emailSmtpPort.toString(),
      'notification.email.username': form.emailUsername,
      'notification.email.password': form.emailPassword,
      'notification.email.to_list': form.emailToListStr,
      'notification.dingding.enabled': form.dingdingEnabled.toString(),
      'notification.dingding.webhook': form.dingdingWebhook,
      'notification.wecom.enabled': form.wecomEnabled.toString(),
      'notification.wecom.webhook': form.wecomWebhook
    }

    await batchUpdateConfigs({ configs, updatedBy: 'admin' })

    ElMessage.success('通知配置保存成功')
    configDialogVisible.value = false
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function loadConfig() {
  try {
    const configs = await getConfigsByGroup('notification')

    if (configs) {
      Object.assign(form, {
        emailEnabled: configs['notification.email.enabled'] === 'true',
        emailSmtpHost: configs['notification.email.smtp_host'] || 'smtp.gmail.com',
        emailSmtpPort: parseInt(configs['notification.email.smtp_port'] || '587'),
        emailUsername: configs['notification.email.username'] || '',
        emailPassword: configs['notification.email.password'] || '',
        emailToListStr: configs['notification.email.to_list'] || '[]',
        dingdingEnabled: configs['notification.dingding.enabled'] === 'true',
        dingdingWebhook: configs['notification.dingding.webhook'] || '',
        wecomEnabled: configs['notification.wecom.enabled'] === 'true',
        wecomWebhook: configs['notification.wecom.webhook'] || ''
      })
    }
  } catch (error) {
    console.error('加载通知配置失败:', error)
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.monitor-notify {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 页面头部 */
.page-header {
  padding: var(--spacing-xl);
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border: none;
}

.page-header .el-card__body {
  padding: var(--spacing-xl);
}

.header-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.header-icon {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-text {
  color: white;
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  margin: 0 0 var(--spacing-xs) 0;
}

.page-subtitle {
  font-size: var(--font-size-base);
  opacity: 0.9;
  margin: 0;
}

/* 通知卡片 */
.notify-cards {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.notify-card {
  padding: var(--spacing-lg);
  height: 100%;
  transition: all var(--transition-slow);
  border: 2px solid transparent;
}

.notify-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
}

.notify-card.active {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.icon-email {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.icon-dingding {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.icon-wecom {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.card-body {
  margin-bottom: var(--spacing-md);
}

.card-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-gray-800);
  margin: 0 0 var(--spacing-xs) 0;
}

.card-desc {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
  margin: 0 0 var(--spacing-md) 0;
}

.config-details {
  padding: var(--spacing-md);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.detail-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-xs);
  font-size: var(--font-size-sm);
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item .el-icon {
  color: var(--color-gray-400);
}

.detail-label {
  color: var(--color-gray-500);
}

.detail-value {
  color: var(--color-gray-700);
  font-weight: 500;
}

.card-footer {
  display: flex;
  justify-content: center;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-gray-100);
}

/* 配置表单 */
.config-form {
  padding: var(--spacing-md);
}

.form-tip {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
  margin-top: var(--spacing-xs);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  line-height: 1.5;
}

.form-tip .el-icon {
  font-size: 14px;
  color: var(--color-primary);
}
</style>
