<template>
  <div class="monitor-notify-config">
    <el-alert
      title="通知配置说明"
      type="info"
      :closable="false"
      style="margin-bottom: 20px"
    >
      <p>配置邮件、钉钉、企业微信等通知方式，接收慢查询告警。</p>
      <p>配置保存后立即生效（热加载），无需重启服务。</p>
    </el-alert>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 邮件通知 -->
      <el-tab-pane label="邮件通知" name="email">
        <el-form label-width="150px">
          <el-form-item label="启用邮件通知">
            <el-switch
              v-model="form.emailEnabled"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <template v-if="form.emailEnabled">
            <el-form-item label="SMTP 服务器">
              <el-input
                v-model="form.emailSmtpHost"
                placeholder="smtp.gmail.com"
                clearable
              />
            </el-form-item>

            <el-form-item label="SMTP 端口">
              <el-input-number
                v-model="form.emailSmtpPort"
                :min="1"
                :max="65535"
              />
              <span class="form-tip">常用端口: 25, 465, 587</span>
            </el-form-item>

            <el-form-item label="发件人邮箱">
              <el-input
                v-model="form.emailUsername"
                placeholder="your-email@gmail.com"
                clearable
              />
            </el-form-item>

            <el-form-item label="邮箱密码/授权码">
              <el-input
                v-model="form.emailPassword"
                type="password"
                show-password
                placeholder="请输入邮箱密码或授权码"
              />
              <span class="form-tip">将加密存储</span>
            </el-form-item>

            <el-form-item label="收件人列表">
              <el-input
                v-model="form.emailToListStr"
                type="textarea"
                :rows="3"
                placeholder='["admin@example.com", "dba@example.com"]'
              />
              <span class="form-tip">JSON 数组格式</span>
            </el-form-item>
          </template>
        </el-form>
      </el-tab-pane>

      <!-- 钉钉通知 -->
      <el-tab-pane label="钉钉通知" name="dingding">
        <el-form label-width="150px">
          <el-form-item label="启用钉钉通知">
            <el-switch
              v-model="form.dingdingEnabled"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <template v-if="form.dingdingEnabled">
            <el-form-item label="Webhook URL">
              <el-input
                v-model="form.dingdingWebhook"
                type="textarea"
                :rows="3"
                placeholder="https://oapi.dingtalk.com/robot/send?access_token=..."
              />
              <span class="form-tip">钉钉群机器人的 Webhook 地址</span>
            </el-form-item>
          </template>
        </el-form>
      </el-tab-pane>

      <!-- 企微通知 -->
      <el-tab-pane label="企微通知" name="wecom">
        <el-form label-width="150px">
          <el-form-item label="启用企微通知">
            <el-switch
              v-model="form.wecomEnabled"
              active-text="启用"
              inactive-text="禁用"
            />
          </el-form-item>

          <template v-if="form.wecomEnabled">
            <el-form-item label="Webhook URL">
              <el-input
                v-model="form.wecomWebhook"
                type="textarea"
                :rows="3"
                placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..."
              />
              <span class="form-tip">企业微信机器人的 Webhook 地址</span>
            </el-form-item>
          </template>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- 操作按钮 -->
    <div class="actions">
      <el-button type="primary" @click="saveConfig" :loading="saving">
        <el-icon><Check /></el-icon>
        保存配置（热加载）
      </el-button>
      <el-button @click="resetForm">重置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { getConfigsByGroup, batchUpdateConfigs } from '@/api/config'

const activeTab = ref('email')
const saving = ref(false)

// 表单数据
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

/**
 * 加载配置
 */
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

/**
 * 保存配置
 */
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

    await batchUpdateConfigs({
      configs,
      updatedBy: 'admin'
    })

    ElMessage.success({
      message: '✅ 通知配置保存成功，已自动热加载到系统！',
      duration: 3000
    })
  } catch (error: any) {
    ElMessage.error(error.message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

/**
 * 重置表单
 */
function resetForm() {
  loadConfig()
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.monitor-notify-config {
  max-width: 800px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  display: block;
  line-height: 1.5;
}

:deep(.el-alert p) {
  margin: 5px 0;
}

.actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #dcdfe6;
  display: flex;
  gap: 10px;
}
</style>
