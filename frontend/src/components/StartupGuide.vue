<template>
  <div class="startup-guide-overlay" v-if="false">
    <el-card class="guide-card">
      <template #header>
        <div class="card-header">
          <el-icon size="24" color="#409EFF"><Setting /></el-icon>
          <h3>欢迎使用 DB-Doctor</h3>
          <el-button v-if="currentStep === 0" type="text" @click="skipGuide" style="margin-left: auto;">
            跳过引导，进入系统
          </el-button>
        </div>
      </template>

      <div class="guide-content">
        <!-- 步骤指示器 -->
        <el-steps :active="currentStep" finish-status="success" align-center>
          <el-step title="数据库配置" description="配置目标数据库" />
          <el-step title="AI配置" description="配置AI分析（可选）" />
          <el-step title="完成" description="开始使用" />
        </el-steps>

        <!-- 步骤内容 -->
        <div class="step-content">
          <!-- 步骤1: 数据库配置 -->
          <div v-if="currentStep === 0" class="config-step">
            <h4>配置目标数据库</h4>
            <p class="step-desc">
              请先配置要监控的MySQL数据库连接信息。DB-Doctor 将连接到该数据库并监听慢查询日志。
            </p>

            <el-form :model="dbConfig" :rules="dbRules" ref="dbFormRef" label-width="140px">
              <el-form-item label="数据库地址" prop="url">
                <el-input
                  v-model="dbConfig.url"
                  placeholder="jdbc:mysql://localhost:3306/information_schema?useSSL=false&serverTimezone=Asia/Shanghai"
                />
              </el-form-item>

              <el-form-item label="用户名" prop="username">
                <el-input v-model="dbConfig.username" placeholder="root" />
              </el-form-item>

              <el-form-item label="密码" prop="password">
                <el-input v-model="dbConfig.password" type="password" show-password />
              </el-form-item>

              <el-form-item label="监听的数据库">
                <div v-if="!testResult || !testResult.success || availableDatabases.length === 0" style="color: #909399; font-size: 13px;">
                  <el-icon><InfoFilled /></el-icon>
                  请先点击「测试连接」按钮，连接成功后可选择数据库
                </div>

                <div v-else>
                  <!-- 搜索框 -->
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索数据库名称..."
                    prefix-icon="Search"
                    style="margin-bottom: 10px;"
                    clearable
                  />

                  <!-- 数据库选择列表 -->
                  <div
                    class="database-selector"
                    style="border: 1px solid #dcdfe6; border-radius: 4px; max-height: 200px; overflow-y: auto; background: white;"
                  >
                    <div
                      v-for="db in filteredDatabases"
                      :key="db"
                      @click="toggleDatabase(db)"
                      class="database-item"
                      :class="{ 'selected': selectedDatabases.includes(db) }"
                      style="padding: 8px 12px; cursor: pointer; display: flex; align-items: center; border-bottom: 1px solid #f5f7fa;"
                    >
                      <el-checkbox
                        :model-value="selectedDatabases.includes(db)"
                        @change="toggleDatabase(db)"
                        style="margin-right: 8px;"
                      />
                      <span style="flex: 1;">{{ db }}</span>
                    </div>
                  </div>

                  <!-- 提示信息 -->
                  <div style="margin-top: 8px; font-size: 13px; color: #606266;">
                    已选择 <span style="color: #409EFF; font-weight: bold;">{{ selectedDatabases.length }}</span> 个数据库
                  </div>
                </div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="testDbConnection" :loading="testing">
                  <el-icon><Connection /></el-icon>
                  测试连接
                </el-button>
                <span v-if="testResult" class="test-result" :class="{ success: testResult.success, error: !testResult.success }">
                  {{ testResult.message }}
                </span>
              </el-form-item>
            </el-form>
          </div>

          <!-- 步骤2: AI配置（可选） -->
          <div v-if="currentStep === 1" class="config-step">
            <h4>配置AI分析功能（可选）</h4>
            <p class="step-desc">
              AI功能可以自动分析慢查询并给出优化建议。如果暂时不需要，可以跳过此步骤，稍后在设置中配置。
            </p>

            <el-form label-width="140px">
              <el-form-item label="启用AI分析">
                <el-switch v-model="aiConfig.enabled" />
                <span class="form-tip">启用后将自动分析慢查询并给出优化建议</span>
              </el-form-item>

              <template v-if="aiConfig.enabled">
                <!-- AI 服务提供商 -->
                <el-form-item label="AI 服务提供商">
                  <el-radio-group v-model="aiConfig.provider">
                    <el-radio label="ollama">Ollama (本地)</el-radio>
                    <el-radio label="openai">OpenAI</el-radio>
                  </el-radio-group>
                  <span class="form-tip">选择 AI 模型提供商</span>
                </el-form-item>

                <!-- 通用配置 -->
                <el-form-item label="Base URL">
                  <el-input
                    v-model="aiConfig.baseUrl"
                    :placeholder="aiConfig.provider === 'ollama' ? 'http://localhost:11434' : 'https://api.openai.com/v1'"
                  />
                  <span class="form-tip">
                    {{ aiConfig.provider === 'ollama' ? 'Ollama 服务地址' : 'OpenAI API 地址' }}
                  </span>
                </el-form-item>

                <el-form-item label="API Key" v-if="aiConfig.provider === 'openai'">
                  <el-input v-model="aiConfig.apiKey" type="password" show-password placeholder="sk-..." />
                  <span class="form-tip">OpenAI API 密钥（Ollama 可忽略）</span>
                </el-form-item>

                <!-- 分割线 -->
                <el-divider content-position="left">3 个 AI Agent 配置</el-divider>
                <el-alert
                  title="提示"
                  type="info"
                  :closable="false"
                  style="margin-bottom: 20px"
                >
                  DB-Doctor 使用 3 个 AI Agent 协作：主治医生、推理专家、编码专家。您可以为每个 Agent 配置不同的模型。
                </el-alert>

                <!-- 主治医生配置 -->
                <el-card shadow="never" style="margin-bottom: 15px;">
                  <template #header>
                    <div style="display: flex; align-items: center;">
                      <el-icon style="margin-right: 8px;"><User /></el-icon>
                      <span style="font-weight: bold;">主治医生</span>
                      <el-tag size="small" type="primary" style="margin-left: 10px;">慢查询诊断</el-tag>
                    </div>
                  </template>
                  <el-form-item label="模型名称">
                    <el-input
                      v-model="aiConfig.diagnosis.modelName"
                      :placeholder="aiConfig.provider === 'ollama' ? 'qwen2.5:7b' : 'gpt-4'"
                    />
                    <span class="form-tip">负责慢查询的初步诊断和分析</span>
                  </el-form-item>
                  <el-form-item label="温度参数">
                    <el-slider
                      v-model="aiConfig.diagnosis.temperature"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      show-stops
                      style="width: 100%"
                    />
                    <span class="form-tip">推荐 0.1（低温度，输出更确定）</span>
                  </el-form-item>
                </el-card>

                <!-- 推理专家配置 -->
                <el-card shadow="never" style="margin-bottom: 15px;">
                  <template #header>
                    <div style="display: flex; align-items: center;">
                      <el-icon style="margin-right: 8px;"><ChatDotRound /></el-icon>
                      <span style="font-weight: bold;">推理专家</span>
                      <el-tag size="small" type="success" style="margin-left: 10px;">深度推理</el-tag>
                    </div>
                  </template>
                  <el-form-item label="模型名称">
                    <el-input
                      v-model="aiConfig.reasoning.modelName"
                      :placeholder="aiConfig.provider === 'ollama' ? 'deepseek-r1:7b' : 'gpt-4'"
                    />
                    <span class="form-tip">负责复杂的逻辑推理和深度分析</span>
                  </el-form-item>
                  <el-form-item label="温度参数">
                    <el-slider
                      v-model="aiConfig.reasoning.temperature"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      show-stops
                      style="width: 100%"
                    />
                    <span class="form-tip">推荐 0.3（中等温度，平衡创造性和准确性）</span>
                  </el-form-item>
                </el-card>

                <!-- 编码专家配置 -->
                <el-card shadow="never">
                  <template #header>
                    <div style="display: flex; align-items: center;">
                      <el-icon style="margin-right: 8px;"><EditPen /></el-icon>
                      <span style="font-weight: bold;">编码专家</span>
                      <el-tag size="small" type="warning" style="margin-left: 10px;">SQL 优化</el-tag>
                    </div>
                  </template>
                  <el-form-item label="模型名称">
                    <el-input
                      v-model="aiConfig.coding.modelName"
                      :placeholder="aiConfig.provider === 'ollama' ? 'deepseek-coder:6.7b' : 'gpt-4'"
                    />
                    <span class="form-tip">负责生成优化的 SQL 语句和索引建议</span>
                  </el-form-item>
                  <el-form-item label="温度参数">
                    <el-slider
                      v-model="aiConfig.coding.temperature"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      show-stops
                      style="width: 100%"
                    />
                    <span class="form-tip">推荐 0.2（低温度，确保生成的 SQL 准确）</span>
                  </el-form-item>
                </el-card>

                <!-- 超时时间 -->
                <el-form-item label="超时时间（秒）">
                  <el-input-number
                    v-model="aiConfig.timeoutSeconds"
                    :min="10"
                    :max="300"
                  />
                  <span class="form-tip">API 调用超时时间，默认 60 秒</span>
                </el-form-item>
              </template>
            </el-form>
          </div>

          <!-- 步骤3: 完成 -->
          <div v-if="currentStep === 2" class="config-step">
            <div class="success-content">
              <el-result icon="success" title="配置完成" sub-title="DB-Doctor 已准备就绪！">
                <template #extra>
                  <el-button type="primary" size="large" @click="goToDashboard">
                    进入系统
                  </el-button>
                </template>
              </el-result>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="step-actions">
          <el-button v-if="currentStep > 0 && currentStep < 2" @click="prevStep">上一步</el-button>
          <el-button v-if="currentStep < 1" type="primary" @click="nextStep" :disabled="!canNextStep">
            下一步
          </el-button>
          <el-button v-if="currentStep === 1" type="primary" @click="finishConfig" :loading="saving">
            {{ aiConfig.enabled ? '保存配置' : '跳过并完成' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Connection, InfoFilled, User, ChatDotRound, EditPen } from '@element-plus/icons-vue'
import { getInitializationStatus, testDatabaseConnection, updateConfig, batchUpdateConfigs } from '@/api/config'
import { useRouter } from 'vue-router'

const router = useRouter()

// 状态
const initialized = ref(true) // 默认跳过引导页，直接进入系统
const currentStep = ref(0)
const testing = ref(false)
const saving = ref(false)
const dbFormRef = ref()

// 搜索关键词
const searchKeyword = ref('')

// 可用的数据库列表
const availableDatabases = ref<string[]>([])
const selectedDatabases = ref<string[]>([])

// 过滤后的数据库列表
const filteredDatabases = computed(() => {
  if (!searchKeyword.value) {
    return availableDatabases.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return availableDatabases.value.filter(db => db.toLowerCase().includes(keyword))
})

// 切换数据库选择状态
function toggleDatabase(db: string) {
  const index = selectedDatabases.value.indexOf(db)
  if (index > -1) {
    selectedDatabases.value.splice(index, 1)
  } else {
    selectedDatabases.value.push(db)
  }
}

// 测试结果
const testResult = ref<{ success: boolean; message: string } | null>(null)

// 数据库配置
const dbConfig = reactive({
  url: 'jdbc:mysql://localhost:3306/information_schema?useSSL=false&serverTimezone=Asia/Shanghai',
  username: 'root',
  password: ''
})

// AI配置
const aiConfig = reactive({
  enabled: false,
  provider: 'ollama', // ollama 或 openai
  apiKey: '',
  baseUrl: 'http://localhost:11434',
  timeoutSeconds: 60,
  // 3 个 AI Agent 配置
  diagnosis: {
    modelName: 'qwen2.5:7b',
    temperature: 0.1
  },
  reasoning: {
    modelName: 'deepseek-r1:7b',
    temperature: 0.3
  },
  coding: {
    modelName: 'deepseek-coder:6.7b',
    temperature: 0.2
  }
})

// 数据库配置校验规则
const dbRules = {
  url: [{ required: true, message: '请输入数据库地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 是否可以进入下一步
const canNextStep = computed(() => {
  if (currentStep.value === 0) {
    return testResult.value?.success === true
  }
  return true
})

/**
 * 检查初始化状态
 */
async function checkInitialization() {
  try {
    const result = await getInitializationStatus()
    // 如果已配置，加载已有配置并允许用户重新配置
    if (result.fullyConfigured || result.canStart) {
      // 加载已有配置
      await loadExistingConfigs()
      // 仍然显示引导页（用户可以选择跳过或重新配置）
      initialized.value = false
    } else {
      initialized.value = false
    }
  } catch (error) {
    console.error('检查初始化状态失败:', error)
    // 如果接口调用失败，默认显示引导页
    initialized.value = false
  }
}

/**
 * 加载已有配置
 */
async function loadExistingConfigs() {
  try {
    // 获取数据库配置
    const response = await fetch('/api/system/config/group/database')
    const result = await response.json()
    if (result.success && result.data) {
      const configs = result.data
      configs.forEach((config: any) => {
        if (config.configKey === 'database.url') {
          dbConfig.url = config.configValue || dbConfig.url
        } else if (config.configKey === 'database.username') {
          dbConfig.username = config.configValue || dbConfig.username
        } else if (config.configKey === 'database.monitored_dbs') {
          try {
            const dbs = JSON.parse(config.configValue || '[]')
            if (Array.isArray(dbs)) {
              selectedDatabases.value = dbs
            }
          } catch (e) {
            console.error('解析数据库列表失败:', e)
          }
        }
      })
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

/**
 * 测试数据库连接
 */
async function testDbConnection() {
  // 表单验证
  const valid = await dbFormRef.value?.validate().catch(() => false)
  if (!valid) return

  testing.value = true
  testResult.value = null

  try {
    const result = await testDatabaseConnection({
      url: dbConfig.url,
      username: dbConfig.username,
      password: dbConfig.password
    })

    testResult.value = {
      success: result.success,
      message: result.message || (result.success ? '连接成功' : '连接失败')
    }

    if (result.success) {
      ElMessage.success('数据库连接测试成功')

      // 获取数据库列表
      if (result.databases && Array.isArray(result.databases)) {
        availableDatabases.value = result.databases
        selectedDatabases.value = [] // 清空之前的选择
      }
    } else {
      ElMessage.error(result.message || '数据库连接测试失败')
    }
  } catch (error: any) {
    testResult.value = {
      success: false,
      message: error.message || '连接测试失败'
    }
    ElMessage.error(error.message || '数据库连接测试失败')
  } finally {
    testing.value = false
  }
}

/**
 * 下一步
 */
function nextStep() {
  if (currentStep.value === 0 && !canNextStep.value) {
    ElMessage.warning('请先测试数据库连接成功后再继续')
    return
  }
  currentStep.value++
}

/**
 * 上一步
 */
function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

/**
 * 完成配置
 */
async function finishConfig() {
  saving.value = true

  try {
    const configs: Record<string, string> = {}

    // 保存数据库配置
    configs['database.url'] = dbConfig.url
    configs['database.username'] = dbConfig.username
    configs['database.password'] = dbConfig.password
    configs['database.monitored_dbs'] = JSON.stringify(selectedDatabases.value)

    // 如果启用AI，保存AI配置
    if (aiConfig.enabled) {
      configs['ai.enabled'] = 'true'
      configs['ai.provider'] = aiConfig.provider
      configs['ai.api_key'] = aiConfig.apiKey
      configs['ai.base_url'] = aiConfig.baseUrl

      // 保存 3 个 AI Agent 配置
      // 主治医生
      configs['ai.diagnosis.model_name'] = aiConfig.diagnosis.modelName
      configs['ai.diagnosis.temperature'] = aiConfig.diagnosis.temperature.toString()

      // 推理专家
      configs['ai.reasoning.model_name'] = aiConfig.reasoning.modelName
      configs['ai.reasoning.temperature'] = aiConfig.reasoning.temperature.toString()

      // 编码专家
      configs['ai.coding.model_name'] = aiConfig.coding.modelName
      configs['ai.coding.temperature'] = aiConfig.coding.temperature.toString()

      // 超时时间
      configs['ai.timeout_seconds'] = aiConfig.timeoutSeconds.toString()
    }

    await batchUpdateConfigs({ configs, updatedBy: 'admin' })

    ElMessage.success('配置保存成功')

    // 进入完成步骤
    currentStep.value = 2
  } catch (error: any) {
    ElMessage.error(error.message || '配置保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 进入系统
 */
function goToDashboard() {
  initialized.value = true
  router.push('/dashboard')
}

/**
 * 跳过引导
 */
function skipGuide() {
  initialized.value = true
}

onMounted(() => {
  checkInitialization()
})
</script>

<style scoped>
.startup-guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20px;
}

.guide-card {
  width: 100%;
  max-width: 800px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.guide-content {
  padding: 20px 0;
}

.step-content {
  margin: 40px 0;
  min-height: 300px;
}

.config-step h4 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #303133;
}

.step-desc {
  color: #606266;
  margin-bottom: 24px;
  line-height: 1.6;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.test-result {
  margin-left: 12px;
  font-size: 14px;
}

.test-result.success {
  color: #67c23a;
}

.test-result.error {
  color: #f56c6c;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.success-content {
  padding: 20px 0;
}

/* 数据库选择器样式 */
.database-selector {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
  background: white;
}

.database-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f5f7fa;
  transition: background-color 0.2s;
}

.database-item:hover {
  background-color: #f5f7fa;
}

.database-item.selected {
  background-color: #ecf5ff;
}

.database-item:last-child {
  border-bottom: none;
}
</style>
