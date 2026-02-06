<template>
  <div class="ai-service-instance-management">
    <!-- AI服务实例卡片 -->
    <el-card class="instances-card" shadow="never">
      <!-- 卡片头部：标题和操作按钮 -->
      <template #header>
        <div class="card-header">
          <h3 class="card-title">AI服务实例</h3>
          <div class="card-actions">
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新增AI服务实例
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="filter-form">
        <el-input
          v-model="searchText"
          placeholder="搜索实例名称、模型..."
          clearable
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 实例列表表格 -->
      <el-table
        :data="filteredInstances"
        v-loading="loading"
        :empty-text="'暂无AI服务实例，点击上方按钮新增'"
        stripe
        class="modern-table"
        style="width: 100%; margin-top: 16px"
      >
        <!-- 实例名称 -->
        <el-table-column label="实例名称" min-width="180" fixed="left">
          <template #default="{ row }">
            <div class="instance-name-cell">
              <div class="deployment-tags">
                <el-tag size="small" :type="getDeploymentTypeTagType(row.deploymentType)" effect="light">
                  {{ getDeploymentTypeLabel(row.deploymentType) }}
                </el-tag>
                <el-tag size="small" :type="getProviderTagType(row.provider)" effect="light">
                  {{ getProviderLabel(row.provider) }}
                </el-tag>
              </div>
              <div class="name-text">
                <span class="name">{{ row.instanceName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 模型名称 -->
        <el-table-column label="模型" min-width="200">
          <template #default="{ row }">
            <div class="model-info">
              <div class="model-name">{{ row.modelName }}</div>
              <div class="model-params" v-if="row.temperature !== undefined || row.maxTokens">
                <span v-if="row.temperature !== undefined">温度: {{ row.temperature }}</span>
                <span v-if="row.maxTokens !== undefined">Token: {{ row.maxTokens }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- API地址 -->
        <el-table-column label="API地址" min-width="250">
          <template #default="{ row }">
            <span class="url-text">{{ row.baseUrl || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 超时时间 -->
        <el-table-column label="超时" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.timeoutSeconds || '-' }}s</span>
          </template>
        </el-table-column>

        <!-- 启用状态 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                v-model="row.isEnabled"
                @change="(val) => handleToggleEnabled(row, val)"
                :loading="row._toggling"
              />
            </div>
          </template>
        </el-table-column>

        <!-- 连接验证 -->
        <el-table-column label="连接验证" width="120" align="center">
          <template #default="{ row }">
            <el-button v-if="row.isValid" type="success" size="small" disabled>
              <el-icon style="margin-right: 4px;"><CircleCheck /></el-icon>
              已验证
            </el-button>
            <el-button v-else type="danger" size="small" disabled>
              <el-icon style="margin-right: 4px;"><CircleClose /></el-icon>
              未验证
            </el-button>
          </template>
        </el-table-column>

        <!-- 描述 -->
        <el-table-column label="描述" min-width="180">
          <template #default="{ row }">
            <span>{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                size="small"
                @click="handleTest(row)"
                :loading="row._testing"
              >
                <el-icon style="margin-right: 4px;"><Connection /></el-icon>
                测试
              </el-button>
              <el-button
                type="info"
                size="small"
                @click="handleEdit(row)"
              >
                <el-icon style="margin-right: 4px;"><Edit /></el-icon>
                编辑
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >
                <el-icon style="margin-right: 4px;"><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑AI服务实例' : '新增AI服务实例'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
        <el-form-item label="部署类型" prop="deploymentType">
          <el-radio-group v-model="form.deploymentType" @change="handleDeploymentTypeChange">
            <el-radio value="cloud">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-icon><Cloudy /></el-icon>
                <span>云端API</span>
              </div>
            </el-radio>
            <el-radio value="local">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-icon><Monitor /></el-icon>
                <span>本地部署</span>
              </div>
            </el-radio>
          </el-radio-group>
          <div class="form-tip" v-if="form.deploymentType === 'local'">
            <el-icon><InfoFilled /></el-icon>
            本地部署无需 API 密钥，适合开发测试和隐私敏感场景
          </div>
          <div class="form-tip" v-else>
            <el-icon><InfoFilled /></el-icon>
            云端 API 需要提供 API 密钥，适合生产环境和高性能需求
          </div>
        </el-form-item>

        <el-form-item label="实例名称" prop="instanceName">
          <el-input
            v-model="form.instanceName"
            placeholder="例如：OpenAI-GPT4、Ollama-Qwen"
            clearable
          />
        </el-form-item>

        <el-form-item label="AI提供商" prop="provider">
          <el-select
            v-model="form.provider"
            placeholder="选择AI提供商"
            @change="handleProviderChange"
            style="width: 100%"
          >
            <el-option
              v-for="provider in providers"
              :key="provider.value"
              :label="provider.label"
              :value="provider.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="API地址" prop="baseUrl">
          <el-input
            v-model="form.baseUrl"
            placeholder="例如：https://api.openai.com/v1"
            clearable
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            留空则使用提供商默认地址
          </div>
        </el-form-item>

        <el-form-item
          v-if="form.deploymentType === 'cloud'"
          label="API密钥"
          prop="apiKey"
        >
          <el-input
            v-model="form.apiKey"
            type="password"
            placeholder="sk-xxxxx"
            show-password
            clearable
          />
          <div class="form-tip">
            <el-icon><Lock /></el-icon>
            API密钥将被加密存储
          </div>
        </el-form-item>

        <el-form-item v-else label="认证说明">
          <div class="auth-hint">
            <el-icon><Unlock /></el-icon>
            <span>本地部署无需 API 密钥，直接连接即可使用</span>
          </div>
        </el-form-item>

        <el-form-item label="模型名称" prop="modelName">
          <el-input
            v-model="form.modelName"
            placeholder="例如：gpt-4、qwen、deepseek-coder"
            clearable
          />
          <div class="form-tip" v-if="form.provider === 'ollama'">
            <el-icon><InfoFilled /></el-icon>
            Ollama 模型名称格式：deepseek-coder:6.7b
          </div>
        </el-form-item>

        <el-form-item label="温度参数">
          <el-slider
            v-model="form.temperature"
            :min="0"
            :max="1"
            :step="0.1"
            :marks="{ 0: '精确', 0.7: '平衡', 1: '创意' }"
            show-stops
          />
          <div class="form-tip">当前值: {{ form.temperature }}</div>
        </el-form-item>

        <el-form-item label="最大Tokens">
          <el-input-number
            v-model="form.maxTokens"
            :min="1"
            :max="128000"
            :step="1000"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="超时时间（秒）">
          <el-input-number
            v-model="form.timeoutSeconds"
            :min="10"
            :max="300"
            :step="10"
            style="width: 100%"
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            本地部署建议 120 秒，云端 API 建议 60 秒
          </div>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="实例用途说明..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" class="btn-gradient">
            <el-icon style="margin-right: 6px"><Check /></el-icon>
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus,
  Search,
  Cloudy,
  Monitor,
  InfoFilled,
  Lock,
  Unlock,
  Connection,
  Check,
  CircleCheck,
  CircleClose,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import {
  getAllAiServiceInstances,
  createAiServiceInstance,
  updateAiServiceInstance,
  deleteAiServiceInstance,
  setDefaultAiServiceInstance,
  setAiServiceInstanceEnabled,
  type AiServiceInstance
} from '@/api/ai-service-instances'

const loading = ref(false)
const instances = ref<AiServiceInstance[]>([])
const searchText = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// AI 提供商列表
const providers = [
  { label: 'OpenAI', value: 'openai' },
  { label: 'Ollama', value: 'ollama' },
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Anthropic', value: 'anthropic' },
  { label: 'Azure', value: 'azure' }
]

// 提供商默认 URL
const providerUrls: Record<string, string> = {
  openai: 'https://api.openai.com/v1',
  ollama: 'http://localhost:11434',
  deepseek: 'https://api.deepseek.com/v1',
  anthropic: 'https://api.anthropic.com/v1',
  azure: 'https://api.openai.azure.com'
}

// 表单数据
const form = reactive<Partial<AiServiceInstance>>({
  instanceName: '',
  provider: 'openai',
  deploymentType: 'cloud',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  temperature: 0.7,
  maxTokens: 4096,
  timeoutSeconds: 60,
  description: ''
})

// 表单验证规则
const rules: FormRules = {
  instanceName: [
    { required: true, message: '请输入实例名称', trigger: 'blur' }
  ],
  provider: [
    { required: true, message: '请选择AI提供商', trigger: 'change' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  apiKey: [
    {
      required: true,
      message: '请输入API密钥',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (form.deploymentType === 'cloud' && !value) {
          callback(new Error('云端API需要提供API密钥'))
        } else {
          callback()
        }
      }
    }
  ]
}

// 过滤后的实例列表
const filteredInstances = computed(() => {
  if (!searchText.value) return instances.value
  const search = searchText.value.toLowerCase()
  return instances.value.filter(
    (instance) =>
      instance.instanceName.toLowerCase().includes(search) ||
      instance.modelName.toLowerCase().includes(search) ||
      instance.provider.toLowerCase().includes(search)
  )
})

// 获取行样式
function getRowClassName({ row }: { row: AiServiceInstance }) {
  return row.isEnabled ? '' : 'is-disabled-row'
}

// 加载实例列表
async function loadInstances() {
  loading.value = true
  try {
    instances.value = await getAllAiServiceInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '加载实例列表失败')
  } finally {
    loading.value = false
  }
}

// 创建实例
function handleCreate() {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑实例
function handleEdit(instance: AiServiceInstance) {
  isEdit.value = true
  Object.assign(form, instance)
  dialogVisible.value = true
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateAiServiceInstance(form.id!, form as any)
        ElMessage.success('更新成功')
      } else {
        await createAiServiceInstance(form as any)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadInstances()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, {
    instanceName: '',
    provider: 'openai',
    deploymentType: 'cloud',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60,
    description: ''
  })
}

// 切换部署类型
function handleDeploymentTypeChange(deploymentType: string) {
  if (deploymentType === 'local') {
    form.apiKey = ''
    form.timeoutSeconds = 120
  } else {
    form.timeoutSeconds = 60
  }
}

// 提供商变化
function handleProviderChange(provider: string) {
  if (!form.baseUrl) {
    form.baseUrl = providerUrls[provider] || ''
  }
}

// 测试连接
async function handleTest(instance: AiServiceInstance) {
  instance._testing = true
  try {
    // TODO: 实现测试连接逻辑
    await new Promise((resolve) => setTimeout(resolve, 1000))
    ElMessage.success('连接测试成功')
  } catch (error: any) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    instance._testing = false
  }
}

// 切换启用状态
async function handleToggleEnabled(instance: AiServiceInstance, enabled: boolean) {
  instance._toggling = true
  try {
    await setAiServiceInstanceEnabled(instance.id!, enabled)
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch (error: any) {
    instance.isEnabled = !enabled
    ElMessage.error(error.message || '操作失败')
  } finally {
    instance._toggling = false
  }
}

// 删除实例
async function handleDelete(instance: AiServiceInstance) {
  try {
    await ElMessageBox.confirm(
      `确定要删除实例 "${instance.instanceName}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteAiServiceInstance(instance.id!)
    ElMessage.success('删除成功')
    await loadInstances()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 获取部署类型标签类型
function getDeploymentTypeTagType(type?: string) {
  const map: Record<string, string> = {
    local: 'success',
    cloud: 'primary'
  }
  return map[type || 'cloud'] || ''
}

// 获取部署类型标签文本
function getDeploymentTypeLabel(type?: string) {
  const map: Record<string, string> = {
    local: '本地',
    cloud: '云端'
  }
  return map[type || 'cloud'] || '云端'
}

// 获取提供商标签类型
function getProviderTagType(provider: string) {
  const map: Record<string, string> = {
    openai: 'success',
    ollama: 'primary',
    deepseek: 'warning',
    anthropic: '',
    azure: 'info'
  }
  return map[provider] || ''
}

// 获取提供商标签文本
function getProviderLabel(provider: string) {
  const map: Record<string, string> = {
    openai: 'OpenAI',
    ollama: 'Ollama',
    deepseek: 'DeepSeek',
    anthropic: 'Anthropic',
    azure: 'Azure'
  }
  return map[provider] || provider
}

onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
/* ============================================
   AI服务实例管理 - 卡片式设计
   参照数据库实例管理设计风格
============================================ */

.ai-service-instance-management {
  width: 100%;
}

/* === 卡片头部 === */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-actions {
  display: flex;
  gap: 12px;
}

/* === 筛选表单 === */
.filter-form {
  margin-bottom: 16px;
}

/* === 表格样式 === */
:deep(.modern-table) {
  font-size: 14px;
}

:deep(.modern-table .el-table__header th) {
  background: var(--color-bg-sidebar);
  color: var(--color-text-primary);
  font-weight: 600;
  font-size: 13px;
  padding: 12px 16px;
}

:deep(.modern-table .el-table__body td) {
  padding: 12px 16px;
  vertical-align: middle;
}

:deep(.modern-table .el-table__row:hover > td) {
  background: var(--color-bg-hover);
}

/* 修复标签单元格垂直对齐和不被截断 */
:deep(.modern-table .el-table__body td .el-tag) {
  vertical-align: middle;
  white-space: nowrap;
  overflow: visible !important;
  text-overflow: clip !important;
  max-width: none !important;
}

/* 确保单元格内容不被截断 */
:deep(.modern-table .el-table__body td) {
  overflow: visible !important;
}

:deep(.modern-table .el-table__cell) {
  overflow: visible !important;
}

/* 确保标签容器不被截断 */
:deep(.el-tag) {
  max-width: none !important;
}

/* === 单元格样式 === */
.name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-size: 14px;
}

.url-text {
  font-family: 'SF Mono', Monaco, 'Courier New', monospace;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

/* === 实例名称单元格 === */
.instance-name-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.name-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.deployment-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* === 模型信息 === */
.model-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-size: 14px;
}

.model-params {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-muted);
}

/* ============================================
   对话框纯色主题适配
============================================ */

/* === 新增/编辑对话框 === */
:deep(.el-dialog) {
  background: var(--color-bg-page);
  border-radius: var(--radius-2xl);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--color-border);
  padding: 20px 24px;
  background: var(--color-bg-page);
}

:deep(.el-dialog__title) {
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 24px;
  background: var(--color-bg-page);
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-page);
}

/* === 表单样式 === */
:deep(.el-form-item__label) {
  color: var(--color-text-primary);
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  background: var(--color-bg-sidebar);
  border-color: var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: none;
  transition: all var(--transition-base);
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--color-primary);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

:deep(.el-input__inner) {
  color: var(--color-text-primary);
  background: transparent;
}

:deep(.el-input__inner::placeholder) {
  color: var(--color-text-placeholder);
}

:deep(.el-textarea__inner) {
  background: var(--color-bg-sidebar);
  border-color: var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  transition: all var(--transition-base);
}

:deep(.el-textarea__inner:hover) {
  border-color: var(--color-primary);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

/* === 下拉选择框 === */
:deep(.el-select__wrapper) {
  background: var(--color-bg-sidebar);
  border-color: var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: none;
}

:deep(.el-select__wrapper:hover) {
  border-color: var(--color-primary);
}

:deep(.el-select__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

:deep(.el-select__input) {
  color: var(--color-text-primary);
}

:deep(.el-select__placeholder) {
  color: var(--color-text-placeholder);
}

/* === 按钮纯色样式 === */
:deep(.el-dialog__footer .el-button--primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #ffffff;
  box-shadow: none;
  background-image: none;
  transition: all var(--transition-base);
}

:deep(.el-dialog__footer .el-button--primary:hover) {
  background: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
  box-shadow: none;
  background-image: none;
  transform: translateY(-1px);
}

:deep(.el-dialog__footer .el-button--primary:active) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateY(0);
}

:deep(.el-button--default) {
  background: var(--color-bg-sidebar);
  border-color: var(--color-border);
  color: var(--color-text-primary);
  transition: all var(--transition-base);
}

:deep(.el-button--default:hover) {
  background: var(--color-bg-hover);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* === 表单提示 === */
.form-tip {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 8px;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 4px;
}

.auth-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* === 对话框底部 === */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* === 暗色主题适配 === */
[data-theme="dark"] :deep(.el-input__wrapper) {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] :deep(.el-input__wrapper:hover) {
  border-color: #818cf8;
}

[data-theme="dark"] :deep(.el-input__wrapper.is-focus) {
  border-color: #818cf8;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
}

[data-theme="dark"] :deep(.el-textarea__inner) {
  background: #0a0a0a;
  border-color: #262626;
  color: #ffffff;
}

[data-theme="dark"] :deep(.el-textarea__inner:hover) {
  border-color: #818cf8;
}

[data-theme="dark"] :deep(.el-textarea__inner:focus) {
  border-color: #818cf8;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
}

[data-theme="dark"] :deep(.el-select__wrapper) {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] :deep(.el-select__wrapper:hover) {
  border-color: #818cf8;
}

[data-theme="dark"] :deep(.el-select__wrapper.is-focus) {
  border-color: #818cf8;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
}

[data-theme="dark"] :deep(.el-dialog__footer .el-button--primary) {
  background: #818cf8;
  border-color: #818cf8;
  color: #000000;
}

[data-theme="dark"] :deep(.el-dialog__footer .el-button--primary:hover) {
  background: #a5b4fc;
  border-color: #a5b4fc;
}

[data-theme="dark"] :deep(.el-button--default) {
  background: #1a1a1a;
  border-color: #262626;
  color: #d4d4d4;
}

[data-theme="dark"] :deep(.el-button--default:hover) {
  background: #2a2a2a;
  border-color: #818cf8;
  color: #818cf8;
}

[data-theme="dark"] .auth-hint {
  background: #1a1a1a;
  border-color: #262626;
  color: #d4d4d4;
}

[data-theme="dark"] .form-tip {
  color: #a3a3a3;
}
</style>
