<template>
  <div class="target-db-config">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      label-position="left"
    >
      <!-- 数据库类型 -->
      <el-form-item label="数据库类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择数据库类型">
          <el-option label="MySQL" value="mysql" />
          <el-option label="PostgreSQL" value="postgresql" :disabled="true" />
          <el-option label="Oracle" value="oracle" :disabled="true" />
        </el-select>
        <span class="form-tip">目前仅支持 MySQL，其他数据库正在开发中</span>
      </el-form-item>

      <!-- 主机地址 -->
      <el-form-item label="主机地址" prop="host">
        <el-input
          v-model="form.host"
          placeholder="例如: localhost 或 192.168.1.100"
          clearable
        />
      </el-form-item>

      <!-- 端口号 -->
      <el-form-item label="端口号" prop="port">
        <el-input-number
          v-model="form.port"
          :min="1"
          :max="65535"
          placeholder="3306"
        />
        <span class="form-tip">MySQL 默认端口: 3306</span>
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
      </el-form-item>

      <!-- 测试数据库 -->
      <el-form-item label="测试数据库" prop="testDatabase">
        <el-input
          v-model="form.testDatabase"
          placeholder="用于测试连接的数据库名"
          clearable
        />
        <span class="form-tip">可选，用于验证连接是否成功</span>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button @click="testConnection" :loading="testing">
          <el-icon><Connection /></el-icon>
          测试连接
        </el-button>
        <el-button type="primary" @click="saveConfig" :loading="saving">
          <el-icon><Check /></el-icon>
          保存并应用
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 测试结果对话框 -->
    <el-dialog v-model="showTestResult" title="测试结果" width="500px">
      <el-alert
        :type="testResult.success ? 'success' : 'error'"
        :title="testResult.success ? '连接成功' : '连接失败'"
        :description="testResult.message"
        :closable="false"
        show-icon
      />

      <div v-if="testResult.success" style="margin-top: 20px">
        <p><strong>数据库版本:</strong> {{ testResult.version }}</p>
        <p><strong>延迟:</strong> {{ testResult.latency }}</p>
      </div>

      <template #footer>
        <el-button type="primary" @click="showTestResult = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useConfigStore } from '@/stores/config'
import { testDbConnection } from '@/api/config'

const configStore = useConfigStore()
const formRef = ref<FormInstance>()
const testing = ref(false)
const saving = ref(false)
const showTestResult = ref(false)

// 表单数据
const form = reactive({
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  username: 'root',
  password: '',
  testDatabase: 'test'
})

// 测试结果
const testResult = reactive({
  success: false,
  message: '',
  version: '',
  latency: ''
})

// 表单验证规则
const rules: FormRules = {
  type: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [
    { required: true, message: '请输入主机地址', trigger: 'blur' },
    { pattern: /^[\w.-]+$/, message: '请输入有效的主机地址', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '请输入端口号', trigger: 'blur' },
    { type: 'number', min: 1, max: 65535, message: '端口号范围: 1-65535', trigger: 'blur' }
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

/**
 * 加载配置
 */
async function loadConfig() {
  try {
    await configStore.loadConfig()
    const dbConfig = configStore.config.targetDb

    if (dbConfig) {
      Object.assign(form, {
        type: dbConfig.type || 'mysql',
        host: dbConfig.host || 'localhost',
        port: dbConfig.port || 3306,
        username: dbConfig.username || 'root',
        password: '', // 密码不回显
        testDatabase: 'test'
      })
    }
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

/**
 * 测试连接
 */
async function testConnection() {
  try {
    await formRef.value?.validate()

    testing.value = true

    const result = await testDbConnection({
      host: form.host,
      port: form.port,
      username: form.username,
      password: form.password,
      database: form.testDatabase
    })

    testResult.success = result.success
    testResult.message = result.success ? '成功连接到数据库' : result.message || '连接失败'
    testResult.version = result.version || ''
    testResult.latency = result.latency || ''

    showTestResult.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '测试连接失败')
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

    saving.value = true

    const configs = {
      'target.db.type': form.type,
      'target.db.host': form.host,
      'target.db.port': form.port.toString(),
      'target.db.username': form.username,
      'target.db.password': form.password
    }

    const result = await configStore.saveConfigs('DB', configs)

    if (result.requiresRestart) {
      ElMessage.warning({
        message: '配置已保存，但需要重启服务才能生效',
        duration: 5000
      })
    } else {
      ElMessage.success('配置保存成功，数据源已重新加载')
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
  max-width: 600px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  display: block;
}
</style>
