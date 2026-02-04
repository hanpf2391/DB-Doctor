<template>
  <div class="target-db-config">
    <!-- 数据库连接状态 -->
    <div class="db-status-bar">
      <div class="status-item">
        <span class="status-label">数据库状态：</span>
        <span
          class="status-badge"
          :class="dbStatus.connected ? 'connected' : 'disconnected'"
        >
          <span class="status-dot"></span>
          {{ dbStatus.connected ? '可用' : '不可用' }}
        </span>
      </div>

      <!-- 配置说明 -->
      <el-tooltip effect="dark" placement="bottom">
        <template #content>
          <div class="tooltip-content">
            <p><strong>配置说明：</strong></p>
            <p>配置目标数据库连接信息，DB-Doctor 将连接到该数据库并监听慢查询日志。</p>
            <p><b>✨ 支持热部署：</b>配置保存后立即生效，无需重启服务！</p>
            <p><b>💡 新功能：</b>可以从预配置的数据库实例中选择，或手动输入连接信息。</p>
          </div>
        </template>
        <el-icon class="help-icon"><QuestionFilled /></el-icon>
      </el-tooltip>
    </div>

    <!-- 错误提示 -->
    <div v-if="!dbStatus.connected && dbStatus.lastError" class="error-message">
      {{ dbStatus.lastError }}
    </div>

    <!-- 实例选择区域 -->
    <div class="instance-selector">
      <div class="selector-header">
        <span class="selector-title">从预配置实例中选择</span>
        <el-link type="primary" @click="goToInstanceManagement">
          <el-icon><Plus /></el-icon>
          管理数据库实例
        </el-link>
      </div>
      <el-select
        v-model="selectedInstanceId"
        placeholder="选择已保存的数据库实例（可选）"
        filterable
        clearable
        @change="handleInstanceChange"
        style="width: 100%"
      >
        <el-option
          v-for="instance in availableInstances"
          :key="instance.id"
          :label="getInstanceLabel(instance)"
          :value="instance.id"
        >
          <div class="instance-option">
            <span class="instance-name">{{ instance.instanceName }}</span>
            <el-tag v-if="instance.isDefault" size="small" type="warning">默认</el-tag>
            <el-tag v-if="instance.environment" size="small" :type="getEnvironmentTagType(instance.environment)">
              {{ getEnvironmentLabel(instance.environment) }}
            </el-tag>
          </div>
        </el-option>
      </el-select>
    </div>

    <el-divider content-position="left">或手动输入连接信息</el-divider>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="130px"
      label-position="left"
    >
      <!-- 数据库连接 URL -->
      <el-form-item label="数据库连接 URL" prop="url">
        <el-input
          v-model="form.url"
          type="textarea"
          :rows="3"
          placeholder="jdbc:mysql://localhost:3306/information_schema?useSSL=false&serverTimezone=Asia/Shanghai"
          clearable
        />
        <div class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          需要连接到 information_schema 数据库以获取元数据
        </div>
      </el-form-item>

      <!-- 用户名 -->
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="例如: root"
          clearable
        />
      </el-form-item>

      <!-- 密码 -->
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入数据库密码"
          show-password
          clearable
        />
        <div class="form-tip">
          <el-icon><Lock /></el-icon>
          密码将加密存储，安全性有保障
        </div>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <div class="action-buttons">
          <el-button
            type="primary"
            :loading="testing"
            @click="testConnection"
            size="large"
          >
            <el-icon style="margin-right: 5px;"><Connection /></el-icon>
            测试连接
          </el-button>

          <el-button
            type="success"
            :loading="saving"
            @click="saveConfig"
            :disabled="!canSave"
            size="large"
          >
            <el-icon style="margin-right: 5px;"><Check /></el-icon>
            保存配置（热部署）
          </el-button>

          <el-button @click="resetForm">
            重置
          </el-button>
        </div>
      </el-form-item>

      <!-- 测试状态提示 -->
      <div v-if="testStatus" class="test-status" :class="testStatus">
        <el-icon v-if="testStatus === 'success'"><CircleCheck /></el-icon>
        <el-icon v-else-if="testStatus === 'error'"><CircleClose /></el-icon>
        <span v-if="testStatus === 'success'">
          连接成功，已加载 {{ availableDatabases.length }} 个数据库
        </span>
        <span v-else-if="testStatus === 'error'">
          {{ testErrorMessage }}
        </span>
      </div>

      <!-- 监听的数据库选择器 -->
      <el-form-item label="监听的数据库">
        <el-select
          v-model="form.selectedDatabases"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请先点击上方「测试连接」按钮..."
          :disabled="availableDatabases.length === 0"
          style="width: 100%"
        >
          <template #header>
            <div style="padding: 8px; border-bottom: 1px solid #e4e7ed;">
              <el-checkbox
                v-model="checkAll"
                :indeterminate="isIndeterminate"
                @change="handleCheckAll"
              >
                全选所有数据库 ({{ availableDatabases.length }} 个)
              </el-checkbox>
            </div>
          </template>

          <el-option
            v-for="db in availableDatabases"
            :key="db"
            :label="db"
            :value="db"
          />
        </el-select>
        <div class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          DB-Doctor 将仅分析选中的数据库，未选中的将被忽略
        </div>
      </el-form-item>
    </el-form>

    <!-- 测试连接结果对话框 -->
    <el-dialog
      v-model="showTestResult"
      title="连接测试结果"
      width="650px"
      :close-on-click-modal="false"
    >
      <div v-if="testResultData">
        <el-result
          :icon="testResultData.overallPassed ? 'success' : 'warning'"
          :title="testResultData.overallPassed ? '连接成功' : '连接测试完成'"
          :sub-title="testResultData.summary || '测试完成'"
        >
          <template #extra>
            <div class="test-result-content">
              <!-- 基本信息 -->
              <el-descriptions :column="1" border style="margin-bottom: 20px;">
                <el-descriptions-item label="连接状态">
                  <el-tag :type="testResultData.connectionSuccess ? 'success' : 'danger'">
                    {{ testResultData.connectionSuccess ? '成功' : '失败' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="已加载数据库">
                  {{ availableDatabases.length }} 个
                </el-descriptions-item>
              </el-descriptions>

              <!-- 环境检查项 -->
              <h4 style="margin: 20px 0 10px;">环境检查详情</h4>
              <el-table :data="testResultData.items" border style="width: 100%">
                <el-table-column prop="name" label="检查项" width="140" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.passed ? 'success' : 'danger'">
                      {{ row.passed ? '✓ 通过' : '✗ 失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="currentValue" label="当前值" width="120" />
                <el-table-column prop="errorMessage" label="说明" />
                <el-table-column label="修复" width="120" v-if="!testResultData.overallPassed">
                  <template #default="{ row }">
                    <el-tag v-if="!row.passed && row.fixCommand" type="warning">
                      <el-text style="font-family: monospace; font-size: 12px;">
                        {{ row.fixCommand }}
                      </el-text>
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-result>
      </div>

      <template #footer>
        <el-button type="primary" @click="showTestResult = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import {
  Connection,
  Check,
  CircleCheck,
  CircleClose,
  InfoFilled,
  Lock,
  QuestionFilled,
  Plus
} from '@element-plus/icons-vue'
import { getDatabaseConfig, batchUpdateConfigs } from '@/api/config'
import { getAllDatabaseInstances, type DatabaseInstance } from '@/api/database-instances'

const router = useRouter()
const formRef = ref<FormInstance>()
const testing = ref(false)
const saving = ref(false)
const showTestResult = ref(false)
const testStatus = ref<'success' | 'error' | ''>('')
const testErrorMessage = ref('')

// 数据库连接状态
const dbStatus = ref({
  connected: false,
  lastError: null as string | null
})

// 可用的数据库实例列表
const availableInstances = ref<DatabaseInstance[]>([])
const selectedInstanceId = ref<number>()

// 可用的数据库列表（连接成功后获取）
const availableDatabases = ref<string[]>([])

// 全选逻辑
const isIndeterminate = computed(() => {
  return form.selectedDatabases.length > 0 && form.selectedDatabases.length < availableDatabases.value.length
})

const checkAll = computed({
  get: () => {
    return availableDatabases.value.length > 0 && form.selectedDatabases.length === availableDatabases.value.length
  },
  set: (val: boolean) => {
    form.selectedDatabases = val ? [...availableDatabases.value] : []
  }
})

function handleCheckAll(checked: boolean) {
  form.selectedDatabases = checked ? [...availableDatabases.value] : []
}

// 是否可以保存（连接成功且选择了数据库）
const canSave = computed(() => {
  return testStatus.value === 'success' && form.selectedDatabases.length > 0
})

// 表单数据
const form = reactive({
  url: '',
  username: '',
  password: '',
  selectedDatabases: [] as string[]
})

// 测试结果数据
const testResultData = ref<any>(null)

// 表单验证规则
const rules: FormRules = {
  url: [
    { required: true, message: '请输入数据库连接 URL', trigger: 'blur' },
    {
      pattern: /^jdbc:mysql:\/\//,
      message: '请输入有效的 MySQL JDBC URL',
      trigger: 'blur'
    }
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 加载数据库实例列表
 */
async function loadDatabaseInstances() {
  try {
    availableInstances.value = await getAllDatabaseInstances()
  } catch (error: any) {
    console.error('加载数据库实例列表失败:', error)
  }
}

/**
 * 实例选择变化
 */
function handleInstanceChange(instanceId: number | undefined) {
  if (!instanceId) {
    // 清空选择
    form.url = ''
    form.username = ''
    form.password = ''
    return
  }

  const instance = availableInstances.value.find(i => i.id === instanceId)
  if (instance) {
    form.url = instance.url
    form.username = instance.username
    // 密码不回显，用户需要输入
    form.password = ''
    ElMessage.info(`已加载实例 "${instance.instanceName}"，请输入密码后测试连接`)
  }
}

/**
 * 获取实例标签文本
 */
function getInstanceLabel(instance: DatabaseInstance) {
  const parts = [instance.instanceName]
  if (instance.environment) {
    parts.push(`(${getEnvironmentLabel(instance.environment)})`)
  }
  return parts.join(' ')
}

/**
 * 获取环境标签类型
 */
function getEnvironmentTagType(env: string) {
  const map: Record<string, string> = {
    production: 'danger',
    staging: 'warning',
    development: 'success',
    testing: 'info'
  }
  return map[env] || ''
}

/**
 * 获取环境标签文本
 */
function getEnvironmentLabel(env: string) {
  const map: Record<string, string> = {
    production: '生产',
    staging: '预发布',
    development: '开发',
    testing: '测试'
  }
  return map[env] || env
}

/**
 * 跳转到实例管理页面
 */
function goToInstanceManagement() {
  router.push('/settings/database-instances')
}

/**
 * 加载配置
 */
async function loadConfig() {
  try {
    const result = await getDatabaseConfig()

    if (result.url) {
      form.url = result.url
    }
    if (result.username) {
      form.username = result.username
    }
    // 密码不回显（为了安全）

    // 加载已选择的数据库
    if (result.monitored_dbs) {
      try {
        const dbs = JSON.parse(result.monitored_dbs)
        if (Array.isArray(dbs)) {
          form.selectedDatabases = dbs
        }
      } catch (e) {
        console.error('解析数据库列表失败:', e)
      }
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

/**
 * 测试连接（包含完整环境检查）
 */
async function testConnection() {
  try {
    await formRef.value?.validate()

    testing.value = true
    testStatus.value = ''
    testErrorMessage.value = ''
    testResultData.value = null

    // 调用测试连接 API（包含环境检查）
    const response = await fetch('/api/environment/test-connection', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        url: form.url,
        username: form.username,
        password: form.password
      })
    })

    const result = await response.json()

    // 保存完整的后端返回数据
    testResultData.value = result.data

    // 更新连接状态
    dbStatus.value.connected = result.code === 200 && result.data?.connectionSuccess
    dbStatus.value.lastError = result.data?.connectionError || null

    // 判断测试结果
    const isSuccess = result.code === 200 && result.data?.overallPassed

    if (isSuccess) {
      testStatus.value = 'success'
      ElMessage.success('连接成功！环境检查通过，已加载数据库列表')

      // 连接成功后获取数据库列表
      availableDatabases.value = result.data.availableDatabases || []
      // 清空之前的选择
      form.selectedDatabases.splice(0, form.selectedDatabases.length)
    } else {
      testStatus.value = 'error'
      testErrorMessage.value = result.data?.summary || '连接或环境检查失败'
    }

    // 显示测试结果对话框
    showTestResult.value = true

  } catch (error: any) {
    ElMessage.error(error.message || '测试连接失败')
    testStatus.value = 'error'
    dbStatus.value.connected = false
    availableDatabases.value = []
  } finally {
    testing.value = false
  }
}

/**
 * 保存配置
 */
async function saveConfig() {
  try {
    await formRef.value?.validate()

    // 验证是否选择了数据库
    if (form.selectedDatabases.length === 0) {
      ElMessage.warning('请至少选择一个需要监听的数据库')
      return
    }

    saving.value = true

    const configs: Record<string, string> = {
      'database.url': form.url,
      'database.username': form.username,
      'database.password': form.password,
      'database.monitored_dbs': JSON.stringify(form.selectedDatabases)
    }

    const result = await batchUpdateConfigs({
      configs,
      updatedBy: 'admin'
    })

    // 检查保存结果
    if (result.updatedCount && result.updatedCount > 0) {
      // 根据热部署结果显示不同的消息
      if (result.hotReload) {
        ElMessage.success({
          message: result.hotReloadMessage || `✅ 配置保存成功！数据源已热更新，无需重启！`,
          duration: 5000
        })

        // 更新连接状态
        dbStatus.value.connected = true
        dbStatus.value.lastError = null
      } else {
        ElMessage.success({
          message: `✅ 配置保存成功！已更新 ${result.updatedCount} 项配置。请重启服务以使配置生效。`,
          duration: 5000
        })
      }
    } else {
      ElMessage.warning('配置未发生变化')
    }
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
  formRef.value?.resetFields()
  testStatus.value = ''
  testErrorMessage.value = ''
  testResultData.value = null
  selectedInstanceId.value = undefined
}

/**
 * 获取数据库连接状态
 */
async function fetchDbStatus() {
  try {
    const res = await fetch('/api/system/datasource-status')
    const data = await res.json()
    if (data.code === 200) {
      dbStatus.value.connected = data.data?.connected || false
      dbStatus.value.lastError = data.data?.lastError || null
    }
  } catch (error) {
    console.error('获取数据库状态失败', error)
  }
}

onMounted(() => {
  loadConfig()
  loadDatabaseInstances()
  fetchDbStatus()
})
</script>

<style scoped>
.target-db-config {
  max-width: 800px;
}

/* 状态栏 */
.db-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.connected {
  background: #f0f9ff;
  color: #67c23a;
}

.status-badge.disconnected {
  background: #fef0f0;
  color: #f56c6c;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-badge.connected .status-dot {
  background: #67c23a;
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
}

.status-badge.disconnected .status-dot {
  background: #f56c6c;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2);
}

.help-icon {
  font-size: 18px;
  color: #909399;
  cursor: help;
  transition: color 0.3s;
}

.help-icon:hover {
  color: #409eff;
}

.tooltip-content {
  max-width: 300px;
  line-height: 1.6;
}

.tooltip-content p {
  margin: 4px 0;
}

.error-message {
  padding: 10px 14px;
  background: #fef0f0;
  border-left: 3px solid #f56c6c;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
  margin-bottom: 20px;
}

/* 实例选择器 */
.instance-selector {
  margin-bottom: 15px;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.selector-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.instance-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.instance-name {
  flex: 1;
}

/* 表单提示 */
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1.5;
}

.form-tip .el-icon {
  font-size: 14px;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 测试状态提示 */
.test-status {
  margin-top: 15px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.test-status.success {
  background: #f0f9ff;
  color: #67c23a;
  border: 1px solid #b3e19d;
}

.test-status.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
}

/* 测试结果内容 */
.test-result-content {
  width: 100%;
}

.test-result-content h4 {
  margin: 20px 0 10px;
  color: #303133;
}

:deep(.el-result__title) {
  font-size: 18px;
}

:deep(.el-result__subtitle) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-alert p) {
  margin: 4px 0;
}

:deep(.el-table) {
  font-size: 13px;
}
</style>
