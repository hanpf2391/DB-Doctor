<template>
  <div class="monitor-notify-config">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 监控策略 -->
      <el-tab-pane label="监控策略" name="monitor">
        <el-form :model="monitorConfig" label-width="150px">
          <el-form-item label="慢查询阈值（秒）">
            <el-input-number
              v-model="monitorConfig.slowLogThreshold"
              :min="0.1"
              :max="60"
              :step="0.1"
              :precision="1"
            />
            <span class="form-tip">查询耗时超过此值将被记录为慢查询</span>
          </el-form-item>

          <el-form-item label="采集批次大小">
            <el-input-number
              v-model="monitorConfig.batchSize"
              :min="10"
              :max="1000"
              :step="10"
            />
            <span class="form-tip">每次从数据库读取的最大记录数</span>
          </el-form-item>

          <el-form-item label="自适应轮询">
            <el-switch
              v-model="monitorConfig.adaptivePolling"
              active-text="启用"
              inactive-text="禁用"
            />
            <span class="form-tip">根据慢查询负载自动调整轮询间隔</span>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 通知策略 -->
      <el-tab-pane label="通知策略" name="notify">
        <el-form :model="notifyConfig" label-width="150px">
          <el-form-item label="通知模式">
            <el-radio-group v-model="notifyConfig.mode">
              <el-radio label="batch">定期汇总</el-radio>
              <el-radio label="realtime">实时告警</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="汇总间隔" v-if="notifyConfig.mode === 'batch'">
            <el-select v-model="notifyConfig.batchInterval">
              <el-option label="每 30 分钟" value="0,30 * * * * *" />
              <el-option label="每 1 小时" value="0 0 * * * * *" />
              <el-option label="每 2 小时" value="0 0 */2 * * * *" />
              <el-option label="每天 9 点" value="0 0 9 * * *" />
            </el-select>
          </el-form-item>

          <el-form-item label="严重程度阈值">
            <el-slider
              v-model="notifyConfig.severityThreshold"
              :min="1"
              :max="10"
              :step="0.5"
              show-input
            />
          </el-form-item>

          <el-form-item label="接收邮箱">
            <el-input
              v-model="notifyConfig.emails"
              type="textarea"
              :rows="3"
              placeholder="dba@example.com,dev-team@example.com"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- SMTP 配置 -->
      <el-tab-pane label="SMTP 配置" name="smtp">
        <el-form :model="smtpConfig" label-width="150px">
          <el-form-item label="SMTP 服务器">
            <el-input v-model="smtpConfig.host" placeholder="smtp.qq.com" />
          </el-form-item>

          <el-form-item label="端口">
            <el-input-number
              v-model="smtpConfig.port"
              :min="1"
              :max="65535"
            />
          </el-form-item>

          <el-form-item label="用户名">
            <el-input v-model="smtpConfig.username" placeholder="noreply@qq.com" />
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="smtpConfig.password"
              type="password"
              show-password
              placeholder="请输入 SMTP 密码或授权码"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- 操作按钮 -->
    <div class="actions">
      <el-button @click="sendTestEmail" :loading="sending">
        <el-icon><Message /></el-icon>
        发送测试邮件
      </el-button>
      <el-button type="primary" @click="saveConfig" :loading="saving">
        <el-icon><Check /></el-icon>
        保存并应用
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfigStore } from '@/stores/config'
import { sendTestEmail as sendTestEmailApi } from '@/api/config'

const configStore = useConfigStore()
const activeTab = ref('monitor')
const sending = ref(false)
const saving = ref(false)

const monitorConfig = reactive({
  slowLogThreshold: 2.0,
  batchSize: 100,
  adaptivePolling: true
})

const notifyConfig = reactive({
  mode: 'batch',
  batchInterval: '0 0 * * * *',
  severityThreshold: 3.0,
  emails: 'dba@example.com'
})

const smtpConfig = reactive({
  host: 'smtp.qq.com',
  port: 587,
  username: 'noreply@qq.com',
  password: ''
})

async function loadConfig() {
  try {
    await configStore.loadConfig()
    const monitor = configStore.config.monitor
    const notify = configStore.config.notify

    if (monitor) Object.assign(monitorConfig, monitor)
    if (notify) {
      Object.assign(notifyConfig, notify.mode || {}, notify.smtp || {})
    }
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

async function sendTestEmail() {
  sending.value = true
  try {
    // 解析邮箱列表
    const emails = notifyConfig.emails.split(',').map(e => e.trim()).filter(e => e)

    if (emails.length === 0) {
      ElMessage.warning('请先配置接收邮箱')
      return
    }

    await sendTestEmailApi({
      to: emails,
      subject: 'DB-Doctor 测试邮件'
    })

    ElMessage.success('测试邮件已发送，请检查收件箱')
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const configs: Record<string, string> = {
      'monitor.slow-log-threshold': monitorConfig.slowLogThreshold.toString(),
      'monitor.batch-size': monitorConfig.batchSize.toString(),
      'monitor.adaptive-polling': monitorConfig.adaptivePolling.toString(),
      'notify.mode': notifyConfig.mode,
      'notify.batch-interval-cron': notifyConfig.batchInterval,
      'notify.severity-threshold': notifyConfig.severityThreshold.toString(),
      'notify.emails': notifyConfig.emails,
      'notify.smtp.host': smtpConfig.host,
      'notify.smtp.port': smtpConfig.port.toString(),
      'notify.smtp.username': smtpConfig.username,
      'notify.smtp.password': smtpConfig.password
    }

    await configStore.saveConfigs('NOTIFY', configs)
    ElMessage.success('配置保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.monitor-notify-config {
  max-width: 700px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #dcdfe6;
}
</style>
