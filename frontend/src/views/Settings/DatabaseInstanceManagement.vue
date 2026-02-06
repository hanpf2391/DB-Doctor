<template>
  <div class="database-instance-management">
    <!-- 数据库实例卡片 -->
    <el-card class="instances-card" shadow="never">
      <!-- 卡片头部：标题和操作按钮 -->
      <template #header>
        <div class="card-header">
          <h3 class="card-title">数据库实例</h3>
          <div class="card-actions">
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新增实例
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="filter-form">
        <el-input
          v-model="searchText"
          placeholder="搜索实例名称..."
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
        :empty-text="'暂无数据库实例，点击上方按钮新增'"
        stripe
        class="modern-table"
        style="width: 100%; margin-top: 16px"
      >
        <!-- 实例名称 -->
        <el-table-column label="实例名称" min-width="150" fixed="left">
          <template #default="{ row }">
            <span class="name">{{ row.instanceName }}</span>
          </template>
        </el-table-column>

        <!-- 环境 -->
        <el-table-column label="环境" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.environment" size="small" :type="getEnvironmentTagType(row.environment)" effect="light">
              {{ getEnvironmentLabel(row.environment) }}
            </el-tag>
            <span v-else style="color: var(--color-text-muted);">-</span>
          </template>
        </el-table-column>

        <!-- 类型 -->
        <el-table-column label="类型" width="150" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="success" effect="light">
              {{ row.instanceType?.toUpperCase() || 'MYSQL' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 连接地址 -->
        <el-table-column label="连接地址" min-width="200">
          <template #default="{ row }">
            <span class="url-text">{{ formatUrl(row.url) }}</span>
          </template>
        </el-table-column>

        <!-- 用户名 -->
        <el-table-column label="用户名" width="120">
          <template #default="{ row }">
            <span>{{ row.username }}</span>
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

        <!-- 验证状态 -->
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
                @click="handleValidate(row)"
                :loading="row._validating"
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑数据库实例' : '新增数据库实例'"
      width="600px"
      :close-on-click-modal="false"
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
            placeholder="例如：生产数据库、测试数据库"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="环境标识" prop="environment">
          <el-select v-model="form.environment" placeholder="选择环境（可选）" clearable>
            <el-option label="生产环境" value="production" />
            <el-option label="预发布环境" value="staging" />
            <el-option label="开发环境" value="development" />
            <el-option label="测试环境" value="testing" />
          </el-select>
        </el-form-item>

        <el-form-item label="连接地址" prop="url">
          <el-input
            v-model="form.url"
            type="textarea"
            :rows="3"
            placeholder="jdbc:mysql://localhost:3306/information_schema?useSSL=false&serverTimezone=Asia/Shanghai"
          />
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="数据库用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密码' : '请输入数据库密码'"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="实例描述（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleTestBeforeSave"
          :loading="testing"
        >
          <el-icon style="margin-right: 6px"><Connection /></el-icon>
          测试并保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 验证结果对话框 -->
    <el-dialog
      v-model="showTestResult"
      title="连接测试结果"
      width="650px"
    >
      <div v-if="testResultData" class="test-result">
        <el-result
          :icon="testResultData.overallPassed ? 'success' : 'warning'"
          :title="testResultData.overallPassed ? '连接成功' : '连接测试完成'"
          :sub-title="testResultData.summary || '测试完成'"
        >
          <template #extra>
            <div class="test-result-content">
              <!-- 基本信息 -->
              <el-descriptions :column="1" border>
                <el-descriptions-item label="连接状态">
                  <el-tag :type="testResultData.connectionSuccess ? 'success' : 'danger'">
                    {{ testResultData.connectionSuccess ? '成功' : '失败' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item v-if="testResultData.availableDatabases" label="可用数据库">
                  {{ testResultData.availableDatabases.length }} 个
                </el-descriptions-item>
              </el-descriptions>

              <!-- 环境检查项 -->
              <h4 v-if="testResultData.items">环境检查详情</h4>
              <el-table v-if="testResultData.items" :data="testResultData.items" border>
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
              </el-table>
            </div>
          </template>
        </el-result>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: space-between; width: 100%;">
          <el-button
            v-if="!testResultData?.overallPassed"
            type="danger"
            @click="showTestResult = false"
          >
            关闭
          </el-button>
          <div v-if="testResultData?.overallPassed">
            <el-button @click="showTestResult = false">返回修改</el-button>
            <el-button type="primary" @click="handleSaveAfterTest">
              <el-icon style="margin-right: 6px"><Check /></el-icon>
              确认保存
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import {
  Plus,
  Edit,
  Delete,
  Connection,
  Link,
  User,
  CircleCheck,
  Check,
  CircleClose,
  ArrowDown,
  Search
} from '@element-plus/icons-vue'
import {
  getAllDatabaseInstances,
  createDatabaseInstance,
  updateDatabaseInstance,
  deleteDatabaseInstance,
  validateDatabaseConnection,
  setDefaultDatabaseInstance,
  setDatabaseInstanceEnabled,
  type DatabaseInstance
} from '@/api/database-instances'

const loading = ref(false)
const instances = ref<DatabaseInstance[]>([])
const dialogVisible = ref(false)
const showTestResult = ref(false)
const testing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const testResultData = ref<any>(null)
const searchText = ref('')

const isEdit = ref(false)
const editingId = ref<number>()

// 表单数据
const form = reactive<DatabaseInstance>({
  instanceName: '',
  instanceType: 'mysql',
  url: '',
  username: '',
  password: '',
  description: ''
})

// 表单验证规则
const rules: FormRules = {
  instanceName: [{ required: true, message: '请输入实例名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入连接地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: !isEdit.value, message: '请输入密码', trigger: 'blur' }]
}

// 过滤后的实例列表
const filteredInstances = computed(() => {
  if (!searchText.value) return instances.value
  return instances.value.filter(instance =>
    instance.instanceName.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 加载实例列表
async function loadInstances() {
  loading.value = true
  try {
    instances.value = await getAllDatabaseInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '加载实例列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化URL
function formatUrl(url: string) {
  try {
    const urlObj = new URL(url.replace('jdbc:mysql://', 'http://').split('?')[0])
    return urlObj.host
  } catch {
    return url.length > 40 ? url.substring(0, 40) + '...' : url
  }
}

// 格式化时间
function formatTime(time: string) {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  return date.toLocaleDateString()
}

// 获取环境标签类型
function getEnvironmentTagType(env: string) {
  const map: Record<string, string> = {
    production: 'danger',
    staging: 'warning',
    development: 'success',
    testing: 'info'
  }
  return map[env] || ''
}

// 获取环境标签文本
function getEnvironmentLabel(env: string) {
  const map: Record<string, string> = {
    production: '生产',
    staging: '预发布',
    development: '开发',
    testing: '测试'
  }
  return map[env] || env
}

// 打开创建对话框
function handleCreate() {
  isEdit.value = false
  Object.assign(form, {
    instanceName: '',
    instanceType: 'mysql',
    url: '',
    username: '',
    password: '',
    description: ''
  })
  dialogVisible.value = true
}

// 打开编辑对话框
function handleEdit(row: DatabaseInstance) {
  isEdit.value = true
  editingId.value = row.id
  Object.assign(form, {
    instanceName: row.instanceName,
    instanceType: row.instanceType,
    environment: row.environment,
    url: row.url,
    username: row.username,
    password: '', // 编辑时不回显密码
    description: row.description || ''
  })
  dialogVisible.value = true
}

// 测试连接（保存前必须测试）
async function handleTestBeforeSave() {
  try {
    await formRef.value?.validate()

    testing.value = true

    // 调用测试连接API
    const response = await fetch('/api/environment/test-connection', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url: form.url,
        username: form.username,
        password: form.password
      })
    })

    const result = await response.json()
    testResultData.value = result.data
    showTestResult.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '测试连接失败')
  } finally {
    testing.value = false
  }
}

// 测试成功后保存
async function handleSaveAfterTest() {
  try {
    saving.value = true

    if (isEdit.value && editingId.value) {
      await updateDatabaseInstance(editingId.value, form)
      ElMessage.success('实例更新成功')
    } else {
      await createDatabaseInstance(form)
      ElMessage.success('实例创建成功')
    }

    dialogVisible.value = false
    showTestResult.value = false
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 验证连接（仅测试，不保存）
async function handleValidate(row: DatabaseInstance) {
  if (!row.id) return

  row._validating = true
  try {
    const result = await validateDatabaseConnection(row.id)

    // 检查环境检查是否全部通过
    if (result.overallPassed) {
      ElMessage.success('连接测试成功，实例已更新验证状态')
    } else {
      // 连接成功但环境检查未通过
      const failedItems = result.items?.filter(item => !item.passed) || []
      const failedCount = failedItems.length
      ElMessage.error(`环境检查未通过：${failedCount} 项配置需要修复`)
    }

    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '验证连接失败')
  } finally {
    row._validating = false
  }
}

// 切换启用状态
async function handleToggleEnabled(row: DatabaseInstance, enabled: boolean) {
  if (!row.id) return

  row._toggling = true
  try {
    await setDatabaseInstanceEnabled(row.id, enabled)
    ElMessage.success(enabled ? '已启用实例' : '已禁用实例')
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
    row.isEnabled = !enabled // 恢复原状态
  } finally {
    row._toggling = false
  }
}

// 删除实例
async function handleDelete(row: DatabaseInstance) {
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

    await deleteDatabaseInstance(row.id)
    ElMessage.success('实例已删除')
    await loadInstances()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
/* ============================================
   数据库实例管理 - 卡片式设计
   参照慢查询报表设计风格
============================================ */

.database-instance-management {
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

/* 修复表格列的cell不溢出 */
:deep(.el-table__cell) {
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

/* === 测试结果 === */
.test-result-content {
  width: 100%;
}

.test-result-content h4 {
  margin: 20px 0 12px;
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 600;
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

/* === 验证结果对话框 === */
:deep(.el-result__title) {
  color: var(--color-text-primary);
}

:deep(.el-result__subtitle) {
  color: var(--color-text-secondary);
}

:deep(.el-descriptions) {
  background: var(--color-bg-page);
}

:deep(.el-descriptions__label) {
  color: var(--color-text-secondary);
  background: var(--color-bg-sidebar);
}

:deep(.el-descriptions__content) {
  color: var(--color-text-primary);
  background: var(--color-bg-page);
}

:deep(.el-descriptions.is-bordered .el-descriptions__cell) {
  border-color: var(--color-border);
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
</style>
