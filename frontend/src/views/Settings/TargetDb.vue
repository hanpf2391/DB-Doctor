<template>
  <div class="target-db-config">
    <el-alert
      title="配置说明"
      type="info"
      :closable="false"
      style="margin-bottom: 20px"
    >
      <p>配置目标数据库连接信息，DB-Doctor 将连接到该数据库并监听慢查询日志。</p>
      <p>✨ <b>支持热部署</b>：配置保存后立即生效，无需重启服务！</p>
    </el-alert>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="140px"
      label-position="left"
    >
      <!-- 数据库连接 URL -->
      <el-form-item label="数据库连接 URL" prop="url">
        <el-input
          v-model="form.url"
          type="textarea"
          :rows="3"
          placeholder="jdbc:mysql://localhost:3306/information_schema?useSSL=false&serverTimezone=Asia/Shanghai"
        />
        <span class="form-tip">
          需要连接到 information_schema 数据库以获取元数据
        </span>
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
        <span class="form-tip">
          密码将加密存储，安全性有保障
        </span>
      </el-form-item>

      <!-- 操作栏 -->
      <el-form-item>
        <div class="connection-actions">
          <el-button
            type="primary"
            plain
            :loading="testing"
            @click="testConnection"
          >
            <el-icon style="margin-right: 5px;"><Connection /></el-icon>
            测试连接并加载数据库
          </el-button>

          <span v-if="testStatus === 'success'" class="status-text success">
            <el-icon><CircleCheck /></el-icon> 连接成功，已加载 {{ availableDatabases.length }} 个数据库
          </span>
          <span v-if="testStatus === 'fail'" class="status-text error">
            <el-icon><CircleCheck /></el-icon> 连接失败，请检查配置
          </span>
        </div>
      </el-form-item>

      <!-- 监听的数据库选择器 -->
      <el-form-item label="监听的数据库">
        <el-select
          v-model="form.selectedDatabases"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请先点击上方「测试连接并加载数据库」按钮..."
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

      <!-- 连接池配置（可选） -->
      <el-divider content-position="left">连接池配置（可选）</el-divider>

      <el-form-item label="最大连接数">
        <el-input-number
          v-model="form.poolMaxSize"
          :min="1"
          :max="100"
          placeholder="10"
        />
        <span class="form-tip">默认: 10</span>
      </el-form-item>

      <el-form-item label="最小空闲连接">
        <el-input-number
          v-model="form.poolMinIdle"
          :min="0"
          :max="50"
          placeholder="2"
        />
        <span class="form-tip">默认: 2</span>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button @click="testConnection" :loading="testing">
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
        <el-button type="success" @click="checkEnvironment" :loading="checking" :disabled="!connectionSuccess">
          <el-icon><CircleCheck /></el-icon>
          检查环境配置
        </el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          <el-icon><Check /></el-icon>
          保存配置（支持热部署）
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 测试结果对话框 -->
    <el-dialog v-model="showTestResult" title="连接测试结果" width="500px">
      <el-alert
        :type="testResult.success ? 'success' : 'error'"
        :title="testResult.success ? '✅ 连接成功' : '❌ 连接失败'"
        :description="testResult.message"
        :closable="false"
        show-icon
      />

      <div v-if="testResult.success" style="margin-top: 20px">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="数据库版本">
            {{ testResult.dbVersion }}
          </el-descriptions-item>
          <el-descriptions-item label="连接用户">
            {{ testResult.username }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          title="下一步"
          :closable="false"
          style="margin-top: 20px"
        >
          <p>连接成功！请点击「检查环境配置」按钮，检查 MySQL 慢查询配置是否正确</p>
        </el-alert>
      </div>

      <div v-if="!testResult.success" style="margin-top: 20px">
        <el-alert
          v-if="testResult.sqlState"
          title="错误详情"
          type="warning"
          :closable="false"
        >
          <p>SQL State: {{ testResult.sqlState }}</p>
          <p>错误码: {{ testResult.errorCode }}</p>
        </el-alert>
      </div>

      <template #footer>
        <el-button type="primary" @click="showTestResult = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 环境检查结果对话框 -->
    <el-dialog v-model="showEnvCheckResult" title="环境配置检查结果" width="700px">
      <el-alert
        :type="envCheckResult.success ? 'success' : 'warning'"
        :title="envCheckResult.success ? '✅ 环境配置正确' : '⚠️ 环境配置需要优化'"
        :closable="false"
        show-icon
      />

      <!-- 诊断信息 -->
      <div style="margin-top: 20px">
        <h4>检查报告</h4>
        <pre style="background: #f5f7fa; padding: 15px; border-radius: 4px; white-space: pre-wrap; font-size: 13px;">{{ envCheckResult.diagnosticInfo }}</pre>

        <!-- 如果检查失败，显示修复建议 -->
        <div v-if="!envCheckResult.success" style="margin-top: 20px">
          <h4>📋 修复建议</h4>
          <el-alert type="info" :closable="false">
            <p>请在目标 MySQL 数据库中执行以下 SQL 语句：</p>
            <pre style="background: #fff; padding: 10px; border-radius: 4px; margin-top: 10px;">-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';

-- 设置日志输出方式为表（便于程序读取）
SET GLOBAL log_output = 'TABLE';

-- 设置慢查询阈值（单位：秒）
SET GLOBAL long_query_time = 2;

-- 刷新权限
FLUSH PRIVILEGES;</pre>
          </el-alert>

          <p style="margin-top: 10px; color: #909399; font-size: 12px;">
            ⚠️ 注意：修改配置后，慢查询日志会记录执行时间超过 {{ longQueryTime }} 秒的 SQL 语句
          </p>
        </div>

        <!-- 如果检查成功，显示启用监控的提示 -->
        <div v-if="envCheckResult.success" style="margin-top: 20px">
          <el-result icon="success" title="环境配置正确" sub-title="DB-Doctor 已准备就绪！">
            <template #extra>
              <el-button type="primary" @click="showEnvCheckResult = false">
                开始监控
              </el-button>
            </template>
          </el-result>
        </div>
      </div>

      <template #footer>
        <el-button @click="showEnvCheckResult = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { Connection, Check, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import { testDatabaseConnection, getDatabaseConfig, updateConfig, batchUpdateConfigs } from '@/api/config'

const formRef = ref<FormInstance>()
const testing = ref(false)
const checking = ref(false)
const saving = ref(false)
const showTestResult = ref(false)
const showEnvCheckResult = ref(false)
const connectionSuccess = ref(false)
const testStatus = ref<'success' | 'fail' | ''>('')

// 可用的数据库列表（连接成功后获取）
const availableDatabases = ref<string[]>([])

// 全选逻辑
const isIndeterminate = computed(() => {
  return form.selectedDatabases.length > 0 && form.selectedDatabases.length < availableDatabases.value.length
})

const checkAll = computed({
  get: () => {
    return availableDatabases.value.length > 0 && form.selectedDatabases.length === availableDatabases.value
  },
  set: (val: boolean) => {
    form.selectedDatabases = val ? [...availableDatabases.value] : []
  }
})

function handleCheckAll(checked: boolean) {
  form.selectedDatabases = checked ? [...availableDatabases.value] : []
}

// 表单数据
const form = reactive({
  url: 'jdbc:mysql://localhost:3306/information_schema?useSSL=false&serverTimezone=Asia/Shanghai',
  username: 'root',
  password: '',
  selectedDatabases: [] as string[],
  poolMaxSize: 10,
  poolMinIdle: 2
})

// 测试结果
const testResult = reactive({
  success: false,
  message: '',
  dbVersion: '',
  username: '',
  sqlState: '',
  errorCode: null
})

// 环境检查结果
const envCheckResult = reactive({
  success: false,
  message: '',
  diagnosticInfo: ''
})

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
    // 密码不回显
    if (result.poolMaxSize) {
      form.poolMaxSize = parseInt(result.poolMaxSize)
    }
    if (result.poolMinIdle) {
      form.poolMinIdle = parseInt(result.poolMinIdle)
    }

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
    // 首次加载失败是正常的，不显示错误
  }
}

/**
 * 测试连接
 */
async function testConnection() {
  try {
    await formRef.value?.validate()

    testing.value = true
    testStatus.value = ''

    const result = await testDatabaseConnection({
      url: form.url,
      username: form.username,
      password: form.password
    })

    testResult.success = result.success
    testResult.message = result.message || (result.success ? '连接成功' : '连接失败')
    testResult.dbVersion = result.dbVersion || ''
    testResult.username = result.username || ''
    testResult.sqlState = result.sqlState || ''
    testResult.errorCode = result.errorCode || null

    // 更新连接状态
    connectionSuccess.value = result.success

    // 连接成功后获取数据库列表
    if (result.success && result.databases && Array.isArray(result.databases)) {
      availableDatabases.value = result.databases
      testStatus.value = 'success'

      ElMessage.success(`连接成功，已加载 ${result.databases.length} 个数据库`)

      // 清空之前的选择
      form.selectedDatabases.splice(0, form.selectedDatabases.length)
    } else {
      testStatus.value = 'fail'
    }
  } catch (error: any) {
    ElMessage.error(error.message || '测试连接失败')
    connectionSuccess.value = false
    availableDatabases.value = []
  } finally {
    testing.value = false
  }
}

/**
 * 检查环境配置
 */
async function checkEnvironment() {
  if (!connectionSuccess.value) {
    ElMessage.warning('请先测试数据库连接成功后再检查环境配置')
    return
  }

  checking.value = true
  try {
    const response = await fetch('/api/environment/check', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()

    envCheckResult.success = result.success || result.data?.success || false
    envCheckResult.message = result.message || '检查完成'
    envCheckResult.diagnosticInfo = result.data?.diagnosticInfo || ''

    showEnvCheckResult.value = true

    // 对话框已显示详细信息，不需要额外弹窗
    // if (envCheckResult.success) {
    //   ElMessage.success('✅ 环境配置正确！')
    // } else {
    //   ElMessage.warning('环境配置需要优化，请查看检查报告')
    // }
  } catch (error: any) {
    ElMessage.error(error.message || '环境检查失败')
  } finally {
    checking.value = false
  }
}

/**
 * 保存配置
 */
async function saveConfig() {
  try {
    await formRef.value?.validate()

    // 验证是否选择了数据库
    if (connectionSuccess.value && form.selectedDatabases.length === 0) {
      ElMessage.warning('请至少选择一个需要监听的数据库')
      return
    }

    saving.value = true

    const configs: Record<string, string> = {
      'database.url': form.url,
      'database.username': form.username,
      'database.password': form.password,
      'database.monitored_dbs': JSON.stringify(form.selectedDatabases)
      // 注意：连接池配置暂不支持数据库存储，使用 application.yml 中的默认值
      // 'database.pool.max_size': form.poolMaxSize.toString(),
      // 'database.pool.min_idle': form.poolMinIdle.toString()
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
          message: result.hotReloadMessage || `✅ 配置保存成功！已更新 ${result.updatedCount} 项配置。数据源已热更新，无需重启！`,
          duration: 5000
        })
      } else {
        ElMessage.success({
          message: `✅ 配置保存成功！已更新 ${result.updatedCount} 项配置。${result.hotReloadMessage || '请重启服务以使配置生效。'}`,
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
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.target-db-config {
  max-width: 800px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1.5;
}

:deep(.el-alert p) {
  margin: 5px 0;
}

:deep(.el-divider) {
  margin: 30px 0 20px;
}

/* 连接操作栏样式 */
.connection-actions {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 5px;
}

.status-text {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}

.status-text.success {
  color: #67c23a;
}

.status-text.error {
  color: #f56c6c;
}
</style>
