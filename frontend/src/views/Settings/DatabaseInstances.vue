<template>
  <div class="database-instances-page">
    <!-- 页面标题和操作 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">数据库实例管理</h2>
        <p class="page-desc">预先配置多个数据库连接实例，在使用时直接选择</p>
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
      <el-table-column prop="instanceType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.instanceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="environment" label="环境" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.environment" :type="getEnvironmentTagType(row.environment)" size="small">
            {{ getEnvironmentLabel(row.environment) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="连接地址" min-width="200" show-overflow-tooltip />
      <el-table-column prop="username" label="用户名" width="120" />
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
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleValidate(row)" :loading="row._validating">
            <el-icon><Connection /></el-icon>
            验证
          </el-button>
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
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item label="实例名称" prop="instanceName">
          <el-input
            v-model="form.instanceName"
            placeholder="例如：生产数据库、测试数据库"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="数据库类型" prop="instanceType">
          <el-select v-model="form.instanceType" placeholder="选择数据库类型">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
          </el-select>
        </el-form-item>

        <el-form-item label="环境标识" prop="environment">
          <el-select v-model="form.environment" placeholder="选择环境（可选）" clearable>
            <el-option label="生产环境" value="production" />
            <el-option label="预发布环境" value="staging" />
            <el-option label="开发环境" value="development" />
            <el-option label="测试环境" value="testing" />
          </el-select>
        </el-form-item>

        <el-form-item label="连接 URL" prop="url">
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

        <el-form-item label="标签" prop="tags">
          <el-input
            v-model="form.tags"
            placeholder='例如：["主库", "核心业务"] (JSON格式)'
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

    <!-- 验证结果对话框 -->
    <el-dialog
      v-model="validateDialogVisible"
      title="连接验证结果"
      width="650px"
    >
      <div v-if="validateResult">
        <el-result
          :icon="validateResult.overallPassed ? 'success' : 'warning'"
          :title="validateResult.overallPassed ? '连接成功' : '验证完成'"
          :sub-title="validateResult.summary || '验证完成'"
        >
          <template #extra>
            <div class="validate-result-content">
              <el-descriptions :column="1" border style="margin-bottom: 20px;">
                <el-descriptions-item label="连接状态">
                  <el-tag :type="validateResult.connectionSuccess ? 'success' : 'danger'">
                    {{ validateResult.connectionSuccess ? '成功' : '失败' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item v-if="validateResult.availableDatabases" label="可用数据库">
                  {{ validateResult.availableDatabases.length }} 个
                </el-descriptions-item>
              </el-descriptions>

              <h4 v-if="validateResult.items" style="margin: 20px 0 10px;">环境检查详情</h4>
              <el-table v-if="validateResult.items" :data="validateResult.items" border>
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
        <el-button type="primary" @click="validateDialogVisible = false">关闭</el-button>
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
  Connection,
  Star
} from '@element-plus/icons-vue'
import {
  getAllDatabaseInstances,
  createDatabaseInstance,
  updateDatabaseInstance,
  deleteDatabaseInstance,
  validateDatabaseConnection,
  setDefaultDatabaseInstance,
  setDatabaseInstanceEnabled,
  type DatabaseInstance,
  type EnvCheckReport
} from '@/api/database-instances'

const loading = ref(false)
const instances = ref<DatabaseInstance[]>([])
const dialogVisible = ref(false)
const validateDialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const validateResult = ref<EnvCheckReport | null>(null)

const isEdit = ref(false)
const editingId = ref<number>()

const dialogTitle = ref('')

// 表单数据
const form = reactive<DatabaseInstance>({
  instanceName: '',
  instanceType: 'mysql',
  environment: '',
  url: '',
  username: '',
  password: '',
  description: '',
  tags: ''
})

// 表单验证规则
const rules: FormRules = {
  instanceName: [
    { required: true, message: '请输入实例名称', trigger: 'blur' }
  ],
  instanceType: [
    { required: true, message: '请选择数据库类型', trigger: 'change' }
  ],
  url: [
    { required: true, message: '请输入连接 URL', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur' }
  ]
}

/**
 * 加载实例列表
 */
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

/**
 * 打开创建对话框
 */
function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '添加数据库实例'
  Object.assign(form, {
    instanceName: '',
    instanceType: 'mysql',
    environment: '',
    url: '',
    username: '',
    password: '',
    description: '',
    tags: ''
  })
  dialogVisible.value = true
}

/**
 * 打开编辑对话框
 */
function handleEdit(row: DatabaseInstance) {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑数据库实例'
  Object.assign(form, {
    instanceName: row.instanceName,
    instanceType: row.instanceType,
    environment: row.environment || '',
    url: row.url,
    username: row.username,
    password: '', // 编辑时不回显密码
    description: row.description || '',
    tags: row.tags || ''
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
      await updateDatabaseInstance(editingId.value, form)
      ElMessage.success('实例更新成功')
    } else {
      await createDatabaseInstance(form)
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
  validateResult.value = null
}

/**
 * 验证连接
 */
async function handleValidate(row: DatabaseInstance) {
  if (!row.id) return

  row._validating = true
  try {
    validateResult.value = await validateDatabaseConnection(row.id)
    validateDialogVisible.value = true

    // 刷新列表以更新验证状态
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '验证连接失败')
  } finally {
    row._validating = false
  }
}

/**
 * 设置默认实例
 */
async function handleSetDefault(row: DatabaseInstance) {
  if (!row.id) return

  try {
    await setDefaultDatabaseInstance(row.id)
    ElMessage.success('已设置为默认实例')
    await loadInstances()
  } catch (error: any) {
    ElMessage.error(error.message || '设置失败')
  }
}

/**
 * 切换启用状态
 */
async function handleToggleEnabled(row: DatabaseInstance, enabled: boolean) {
  if (!row.id) return

  row._toggling = true
  try {
    await setDatabaseInstanceEnabled(row.id, enabled)
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

onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
.database-instances-page {
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

.validate-result-content {
  width: 100%;
}

.validate-result-content h4 {
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
</style>
