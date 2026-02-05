<template>
  <div class="database-instance-management">
    <!-- 操作栏 -->
    <div class="action-bar modern-card">
      <div class="action-left">
        <el-button type="primary" size="large" @click="handleCreate" class="btn-gradient">
          <el-icon style="margin-right: 6px"><Plus /></el-icon>
          新增数据库实例
        </el-button>
      </div>
      <div class="action-right">
        <el-input
          v-model="searchText"
          placeholder="搜索实例名称..."
          prefix-icon="Search"
          style="width: 300px"
          clearable
        />
      </div>
    </div>

    <!-- 实例列表表格 -->
    <div class="instances-table modern-card">
      <el-table
        :data="filteredInstances"
        style="width: 100%"
        v-loading="loading"
        :empty-text="'暂无数据库实例，点击上方按钮新增'"
        stripe
        border
      >
        <!-- 实例名称 -->
        <el-table-column prop="instanceName" label="实例名称" min-width="160">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-weight: 500;">{{ row.instanceName }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 连接地址 -->
        <el-table-column label="连接地址" min-width="200">
          <template #default="{ row }">
            <span style="font-family: 'SF Mono', Monaco, monospace; font-size: 13px; color: var(--color-text-secondary);">
              {{ formatUrl(row.url) }}
            </span>
          </template>
        </el-table-column>

        <!-- 用户名 -->
        <el-table-column prop="username" label="用户名" width="140" />

        <!-- 环境 -->
        <el-table-column label="环境" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="row.environment"
              size="small"
              :type="getEnvironmentTagType(row.environment)"
              effect="plain"
            >
              {{ getEnvironmentLabel(row.environment) }}
            </el-tag>
            <span v-else style="color: var(--color-text-muted);">-</span>
          </template>
        </el-table-column>

        <!-- 验证状态 -->
        <el-table-column label="验证状态" width="140">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 4px;">
              <el-tag
                v-if="row.isValid"
                type="success"
                effect="plain"
                size="small"
              >
                <el-icon style="margin-right: 4px; vertical-align: -2px;"><CircleCheck /></el-icon>
                已验证
              </el-tag>
              <el-tag v-else type="info" effect="plain" size="small">未验证</el-tag>
              <span
                v-if="row.lastValidatedAt"
                style="font-size: 11px; color: var(--color-text-muted);"
              >
                {{ formatTime(row.lastValidatedAt) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 启用状态 -->
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.isEnabled"
              @change="(val) => handleToggleEnabled(row, val)"
              :loading="row._toggling"
              inline-prompt
              active-text="启"
              inactive-text="禁"
            />
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleValidate(row)"
                :loading="row._validating"
              >
                <el-icon style="margin-right: 4px; vertical-align: -2px;"><Connection /></el-icon>
                测试连接
              </el-button>
              <el-button
                type="primary"
                link
                size="small"
                @click="handleEdit(row)"
              >
                <el-icon style="margin-right: 4px; vertical-align: -2px;"><Edit /></el-icon>
                编辑
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click="handleDelete(row)"
              >
                <el-icon style="margin-right: 4px; vertical-align: -2px;"><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

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
          class="btn-gradient"
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
            <el-button type="primary" @click="handleSaveAfterTest" class="btn-gradient">
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
  Check
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
.database-instance-management {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
}

.action-left,
.action-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

/* 表格容器 */
.instances-table {
  padding: var(--spacing-lg);
}

/* 测试结果 */
.test-result-content {
  width: 100%;
}

.test-result-content h4 {
  margin: var(--spacing-lg) 0 var(--spacing-sm);
  color: var(--color-gray-800);
}
</style>
