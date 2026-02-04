<template>
  <div class="ai-service-instances-page">
    <!-- 页面标题和操作 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">AI 服务实例管理</h2>
        <p class="page-desc">预先配置多个 AI 服务实例，在不同场景下选择使用</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        添加实例
      </el-button>
    </div>

    <!-- 实例列表 -->
    <el-table
      v-loading="loading"
      :data="instances"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="instanceName" label="实例名称" width="160" />
      <el-table-column prop="provider" label="提供商" width="110">
        <template #default="{ row }">
          <el-tag :type="getProviderTagType(row.provider)" size="small">
            {{ getProviderLabel(row.provider) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelName" label="模型" width="140" show-overflow-tooltip />
      <el-table-column prop="baseUrl" label="API 地址" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.isEnabled"
            :loading="row._toggling"
            @change="(val) => handleToggleEnabled(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="验证状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isValid" type="success" size="small">✓ 已验证</el-tag>
          <el-tag v-else type="info" size="small">未验证</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="warning" size="small">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.isDefault" link type="primary" @click="handleSetDefault(row)">
            <el-icon><Star /></el-icon>
            设为默认
          </el-button>
          <el-button link type="primary" @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="650px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="实例名称" prop="instanceName">
          <el-input
            v-model="form.instanceName"
            placeholder="例如：OpenAI-GPT4、Ollama-Qwen"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="AI 提供商" prop="provider">
          <el-select v-model="form.provider" placeholder="选择 AI 提供商">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Ollama" value="ollama" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="Anthropic" value="anthropic" />
            <el-option label="Azure" value="azure" />
          </el-select>
        </el-form-item>

        <el-form-item label="API 基础 URL" prop="baseUrl">
          <el-input
            v-model="form.baseUrl"
            placeholder="例如：https://api.openai.com/v1"
          />
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            可选，留空则使用提供商默认地址
          </div>
        </el-form-item>

        <el-form-item label="API 密钥" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密钥' : '请输入 API 密钥'"
          />
          <div class="form-tip">
            <el-icon><Lock /></el-icon>
            密钥将加密存储，安全性有保障
          </div>
        </el-form-item>

        <el-form-item label="模型名称" prop="modelName">
          <el-input
            v-model="form.modelName"
            placeholder="例如：gpt-4、qwen、deepseek-coder"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="温度参数" prop="temperature">
              <el-input-number
                v-model="form.temperature"
                :min="0"
                :max="2"
                :step="0.1"
                :precision="2"
                placeholder="0.0-2.0"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大 Tokens" prop="maxTokens">
              <el-input-number
                v-model="form.maxTokens"
                :min="1"
                :max="100000"
                :step="100"
                placeholder="最大 tokens"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="超时时间(秒)" prop="timeoutSeconds">
              <el-input-number
                v-model="form.timeoutSeconds"
                :min="10"
                :max="300"
                :step="10"
                placeholder="超时时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="实例描述（可选）"
          />
        </el-form-item>

        <el-form-item label="能力标签" prop="capabilityTags">
          <el-input
            v-model="form.capabilityTags"
            placeholder='例如：["推理", "编码", "分析"] (JSON格式)'
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import {
  Plus,
  Edit,
  Delete,
  Star,
  InfoFilled,
  Lock
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
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const isEdit = ref(false)
const editingId = ref<number>()

const dialogTitle = ref('')

// 表单数据
const form = reactive<AiServiceInstance>({
  instanceName: '',
  provider: 'openai',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  temperature: 0.7,
  maxTokens: 4096,
  timeoutSeconds: 60,
  description: '',
  capabilityTags: ''
})

// 表单验证规则
const rules: FormRules = {
  instanceName: [
    { required: true, message: '请输入实例名称', trigger: 'blur' }
  ],
  provider: [
    { required: true, message: '请选择 AI 提供商', trigger: 'change' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ]
}

/**
 * 加载实例列表
 */
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

/**
 * 打开创建对话框
 */
function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '添加 AI 服务实例'
  Object.assign(form, {
    instanceName: '',
    provider: 'openai',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    temperature: 0.7,
    maxTokens: 4096,
    timeoutSeconds: 60,
    description: '',
    capabilityTags: ''
  })
  dialogVisible.value = true
}

/**
 * 打开编辑对话框
 */
function handleEdit(row: AiServiceInstance) {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑 AI 服务实例'
  Object.assign(form, {
    instanceName: row.instanceName,
    provider: row.provider,
    baseUrl: row.baseUrl || '',
    apiKey: '', // 编辑时不回显密钥
    modelName: row.modelName,
    temperature: row.temperature || 0.7,
    maxTokens: row.maxTokens || 4096,
    timeoutSeconds: row.timeoutSeconds || 60,
    description: row.description || '',
    capabilityTags: row.capabilityTags || ''
  })
  dialogVisible.value = true
}

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()

    saving.value = true

    if (isEdit.value && editingId.value) {
      await updateAiServiceInstance(editingId.value, form)
      ElMessage.success('实例更新成功')
    } else {
      await createAiServiceInstance(form)
      ElMessage.success('实例创建成功')
    }

    dialogVisible.value = false
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    saving.value = false
  }
}

/**
 * 对话框关闭
 */
function handleDialogClosed() {
  formRef.value?.resetFields()
}

/**
 * 设置默认实例
 */
async function handleSetDefault(row: AiServiceInstance) {
  if (!row.id) return

  try {
    await setDefaultAiServiceInstance(row.id)
    ElMessage.success('已设置为默认实例')
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '设置失败')
  }
}

/**
 * 切换启用状态
 */
async function handleToggleEnabled(row: AiServiceInstance, enabled: boolean) {
  if (!row.id) return

  row._toggling = true
  try {
    await setAiServiceInstanceEnabled(row.id, enabled)
    ElMessage.success(enabled ? '已启用实例' : '已禁用实例')
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
    // 恢复原状态
    row.isEnabled = !enabled
  } finally {
    row._toggling = false
  }
}

/**
 * 删除实例
 */
async function handleDelete(row: AiServiceInstance) {
  if (!row.id) return

  try {
    await ElMessageBox.confirm(
      `确定要删除实例 "${row.instanceName}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )

    await deleteAiServiceInstance(row.id)
    ElMessage.success('实例已删除')
    await loadInstances()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

/**
 * 获取提供商标签类型
 */
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

/**
 * 获取提供商标签文本
 */
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
.ai-service-instances-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

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
</style>
