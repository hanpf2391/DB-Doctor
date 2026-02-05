# 实例管理模块详细设计文档

> **模块名称**: Instances Management
> **优先级**: P0 (最高)
> **开发周期**: Phase 2 (第3-4周)
> **负责人**: 待分配

---

## 📋 目录

1. [功能概述](#功能概述)
2. [数据库实例管理](#数据库实例管理)
3. [AI服务实例管理](#ai服务实例管理)
4. [API设计](#api设计)
5. [数据模型](#数据模型)
6. [测试用例](#测试用例)

---

## 🎯 功能概述

### 核心需求

将数据库配置和AI配置视为**"资源实例"**统一管理，采用**"先测试后保存"**的交互逻辑，确保所有配置都是可用的。

### 关键特性

1. **先测试后保存**: 只有测试连接通过后，才能保存实例
2. **可视化卡片**: 使用卡片而非表格展示，更直观
3. **实时状态**: 显示实例的在线/离线状态
4. **快速操作**: 支持启用/禁用、编辑、删除等操作

---

## 🗄️ 数据库实例管理

### 1. 页面布局

```
┌───────────────────────────────────────────────────────────────┐
│  面包屑: 首页 > 实例管理 > 数据库实例                         │
├───────────────────────────────────────────────────────────────┤
│  页面标题                                                      │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  🔌 数据库实例管理                                             │
│  统一管理 MySQL 数据库连接，测试通过后即可用于监控             │
├───────────────────────────────────────────────────────────────┤
│  [+ 新增数据库实例]         [🔍 搜索实例名称、地址...]         │
├───────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │ 生产环境主库  │  │ 测试环境库   │  │ 开发环境库   │        │
│  │ ━━━━━━━━━━  │  │ ━━━━━━━━━━  │  │ ━━━━━━━━━━  │        │
│  │ 🟢 在线     │  │ 🟢 在线     │  │ 🔴 离线     │        │
│  │              │  │              │  │              │        │
│  │ 地址:        │  │ 地址:        │  │ 地址:        │        │
│  │ 192.168.1.100│  │ 192.168.1.101│  │ 192.168.1.102│        │
│  │ 用户: root   │  │ 用户: admin  │  │ 用户: dev    │        │
│  │ 环境: 生产   │  │ 环境: 测试   │  │ 环境: 开发   │        │
│  │ 最后验证:    │  │ 最后验证:    │  │ 最后验证:    │        │
│  │ 2分钟前      │  │ 1小时前      │  │ 3天前        │        │
│  │              │  │              │  │              │        │
│  │ [编辑] [删除]│  │ [编辑] [删除]│  │ [编辑] [删除]│        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└───────────────────────────────────────────────────────────────┘
```

### 2. 组件结构

```
DatabaseInstances (页面)
├─ PageHeader (页面头部)
├─ ActionBar (操作栏)
│  ├─ PrimaryButton (新增按钮)
│  └─ SearchInput (搜索框)
├─ InstanceGrid (实例网格)
│  └─ DatabaseInstanceCard (实例卡片) × N
│     ├─ CardHeader (卡片头部)
│     │  ├─ InstanceIcon (实例图标)
│     │  ├─ InstanceName (实例名称)
│     │  ├─ StatusBadge (状态徽章)
│     │  └─ ToggleSwitch (启用/禁用开关)
│     ├─ CardBody (卡片内容)
│     │  ├─ InfoRow (信息行) × N
│     │  └─ LastValidated (最后验证时间)
│     └─ CardFooter (卡片底部)
│        ├─ EditButton (编辑按钮)
│        └─ DeleteButton (删除按钮)
└─ DatabaseInstanceDialog (新增/编辑对话框)
   ├─ InstanceForm (表单)
   │  ├─ NameInput (名称输入)
   │  ├─ HostInput (主机输入)
   │  ├─ PortInput (端口输入)
   │  ├─ UsernameInput (用户名输入)
   │  ├─ PasswordInput (密码输入)
   │  └─ EnvironmentSelect (环境选择)
   ├─ TestResult (测试结果显示)
   └─ ActionButtons (操作按钮)
      ├─ CancelButton (取消按钮)
      ├─ TestButton (测试按钮)
      └─ SaveButton (保存按钮 - 默认禁用)
```

### 3. 核心交互：先测试后保存

#### 3.1 交互流程图

```
┌─────────────┐
│  点击"新增"  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────┐
│  打开表单对话框          │
│  "保存"按钮置灰          │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│  用户填写表单            │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐
│  点击"测试连接"          │
└──────┬──────────────────┘
       │
       ▼
┌─────────────────────────┐       ┌──────────────────┐
│  测试成功                │  YES  │ 显示绿色✓        │
│  (连接通过)              │──────►│ 延迟: 1.2s       │
└─────────────────────────┘       │ "保存"按钮高亮   │
                                 └──────────────────┘
                                        │
                                        ▼
                                 ┌──────────────────┐
                                 │  点击"保存"       │
                                 └──────────────────┘
                                        │
                                        ▼
                                 ┌──────────────────┐
                                 │  保存到数据库     │
                                 │  刷新列表         │
                                 └──────────────────┘

┌─────────────────────────┐       ┌──────────────────┐
│  测试失败                │  NO   │ 显示红色✗        │
│  (连接拒绝/超时)         │──────►│ 错误信息         │
└─────────────────────────┘       │ "保存"按钮保持   │
                                 └──────────────────┘
```

#### 3.2 状态机

```typescript
type TestState = 'none' | 'testing' | 'success' | 'fail'

interface TestStateMachine {
  // 初始状态
  none: {
    onTest: () => 'testing'
  }

  // 测试中
  testing: {
    onSuccess: () => 'success'
    onFail: () => 'fail'
  }

  // 测试成功
  success: {
    onFormChange: () => 'none'  // 表单变化时重置
    canSave: true
  }

  // 测试失败
  fail: {
    onTest: () => 'testing'     // 可以重试
    onFormChange: () => 'none'  // 表单变化时重置
    canSave: false
  }
}
```

#### 3.3 代码实现

```typescript
// src/composables/useDatabaseForm.ts
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { testConnection, saveInstance } from '@/api/instances'

export function useDatabaseForm(initialData?: DatabaseInstance) {
  // 表单数据
  const form = reactive<DatabaseInstanceForm>({
    id: initialData?.id || '',
    name: initialData?.name || '',
    host: initialData?.host || 'localhost',
    port: initialData?.port || 3306,
    username: initialData?.username || '',
    password: initialData?.password || '',
    environment: initialData?.environment || 'development'
  })

  // 测试状态
  const testState = ref<TestState>('none')
  const testLatency = ref(0)
  const testError = ref('')

  // 是否可以保存
  const canSave = computed(() => testState.value === 'success')

  // 监听表单变化，重置测试状态
  watch(
    () => [form.name, form.host, form.port, form.username, form.password],
    () => {
      if (testState.value === 'success' || testState.value === 'fail') {
        testState.value = 'none'
        testError.value = ''
      }
    },
    { deep: true }
  )

  // 测试连接
  async function handleTest(): Promise<boolean> {
    // 验证表单
    const valid = await validateForm(form)
    if (!valid) return false

    testState.value = 'testing'
    const startTime = Date.now()

    try {
      await testConnection({
        host: form.host,
        port: form.port,
        username: form.username,
        password: form.password
      })

      testLatency.value = Date.now() - startTime
      testState.value = 'success'
      ElMessage.success(`连接成功！延迟 ${testLatency.value}ms`)
      return true
    } catch (error: any) {
      testState.value = 'fail'
      testError.value = error.message || '连接失败'
      ElMessage.error(testError.value)
      return false
    }
  }

  // 保存实例
  async function handleSave(): Promise<void> {
    if (!canSave.value) {
      ElMessage.warning('请先测试连接通过后再保存')
      return
    }

    try {
      await saveInstance(form)
      ElMessage.success('保存成功')
      return true
    } catch (error: any) {
      ElMessage.error('保存失败：' + error.message)
      return false
    }
  }

  return {
    form,
    testState,
    testLatency,
    testError,
    canSave,
    handleTest,
    handleSave
  }
}
```

### 4. 组件详细设计

#### 4.1 DatabaseInstanceCard

```vue
<template>
  <div
    class="database-instance-card modern-card"
    :class="{ 'is-disabled': !instance.isEnabled }"
  >
    <!-- 卡片头部 - 渐变背景 -->
    <div class="card-header gradient-primary">
      <div class="header-left">
        <el-icon class="instance-icon" :size="32">
          <Database />
        </el-icon>
        <div class="instance-info">
          <h3 class="instance-name">{{ instance.name }}</h3>
          <div class="instance-meta">
            <el-tag
              :type="instance.isEnabled ? 'success' : 'info'"
              size="small"
              effect="dark"
            >
              {{ instance.isEnabled ? '在线' : '离线' }}
            </el-tag>
            <el-tag
              v-if="instance.isDefault"
              type="warning"
              size="small"
              effect="dark"
            >
              默认
            </el-tag>
            <el-tag
              :type="getEnvironmentTagType(instance.environment)"
              size="small"
            >
              {{ getEnvironmentLabel(instance.environment) }}
            </el-tag>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-switch
          v-model="instance.isEnabled"
          :loading="instance._toggling"
          @change="handleToggle"
        />
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="card-body">
      <div class="info-row">
        <el-icon class="row-icon"><Link /></el-icon>
        <span class="row-label">地址:</span>
        <span class="row-value">{{ instance.host }}:{{ instance.port }}</span>
      </div>
      <div class="info-row">
        <el-icon class="row-icon"><User /></el-icon>
        <span class="row-label">用户:</span>
        <span class="row-value">{{ instance.username }}</span>
      </div>
      <div class="info-row" v-if="instance.database">
        <el-icon class="row-icon"><Files /></el-icon>
        <span class="row-label">数据库:</span>
        <span class="row-value">{{ instance.database }}</span>
      </div>

      <!-- 验证状态 -->
      <div class="validation-status">
        <el-icon
          v-if="instance.lastValidatedAt"
          :color="isValidated ? '#10B981' : '#9CA3AF'"
          :size="16"
        >
          <CircleCheck v-if="isValidated" />
          <Clock v-else />
        </el-icon>
        <span class="validate-text">
          {{ validationText }}
        </span>
      </div>
    </div>

    <!-- 卡片底部 -->
    <div class="card-footer">
      <el-button
        link
        type="primary"
        @click="handleEdit"
        :icon="Edit"
      >
        编辑
      </el-button>
      <el-button
        link
        type="danger"
        @click="handleDelete"
        :icon="Delete"
      >
        删除
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Database,
  Link,
  User,
  Files,
  CircleCheck,
  Clock,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import { formatDistanceToNow } from '@/utils/date'

interface DatabaseInstance {
  id: string
  name: string
  host: string
  port: number
  username: string
  password: string
  database?: string
  environment: 'production' | 'testing' | 'development'
  isEnabled: boolean
  isDefault: boolean
  lastValidatedAt?: Date
  _toggling?: boolean
}

const props = defineProps<{
  instance: DatabaseInstance
}>()

const emit = defineEmits<{
  edit: [instance: DatabaseInstance]
  delete: [instance: DatabaseInstance]
  toggle: [instance: DatabaseInstance, value: boolean]
}>()

// 计算验证文本
const validationText = computed(() => {
  if (!props.instance.lastValidatedAt) {
    return '未验证'
  }
  return `最后验证: ${formatDistanceToNow(props.instance.lastValidatedAt)}`
})

// 计算是否已验证（24小时内）
const isValidated = computed(() => {
  if (!props.instance.lastValidatedAt) return false
  const hoursSinceValidation =
    (Date.now() - new Date(props.instance.lastValidatedAt).getTime()) /
    (1000 * 60 * 60)
  return hoursSinceValidation < 24
})

// 环境标签类型
function getEnvironmentTagType(env: string) {
  const map = {
    production: 'danger',
    testing: 'warning',
    development: 'success'
  }
  return map[env] || 'info'
}

// 环境标签文本
function getEnvironmentLabel(env: string) {
  const map = {
    production: '生产',
    testing: '测试',
    development: '开发'
  }
  return map[env] || env
}

// 处理编辑
function handleEdit() {
  emit('edit', props.instance)
}

// 处理删除
function handleDelete() {
  emit('delete', props.instance)
}

// 处理启用/禁用
function handleToggle(value: boolean) {
  emit('toggle', props.instance, value)
}
</script>

<style scoped>
.database-instance-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.database-instance-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.database-instance-card.is-disabled {
  opacity: 0.6;
}

.card-header {
  padding: var(--spacing-lg);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-left {
  display: flex;
  gap: var(--spacing-md);
}

.instance-icon {
  flex-shrink: 0;
}

.instance-info {
  flex: 1;
}

.instance-name {
  font-size: var(--font-size-lg);
  font-weight: 600;
  margin: 0 0 var(--spacing-xs) 0;
}

.instance-meta {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.card-body {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.info-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--font-size-sm);
}

.row-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.row-label {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.row-value {
  color: var(--color-text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.validation-status {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
}

.validate-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.card-footer {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: space-around;
}
</style>
```

#### 4.2 DatabaseInstanceDialog

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑数据库实例' : '新增数据库实例'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      label-width="100px"
    >
      <el-form-item label="实例名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="例如：生产环境主库"
          clearable
        >
          <template #prefix>
            <el-icon><Database /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="16">
          <el-form-item label="主机地址" prop="host">
            <el-input
              v-model="form.host"
              placeholder="localhost 或 IP 地址"
              clearable
            >
              <template #prefix>
                <el-icon><Connection /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="端口" prop="port">
            <el-input-number
              v-model="form.port"
              :min="1"
              :max="65535"
              class="w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="MySQL 用户名"
          clearable
        >
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="MySQL 密码"
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="默认数据库">
        <el-input
          v-model="form.database"
          placeholder="information_schema"
          clearable
        >
          <template #prefix>
            <el-icon><Files /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="环境类型" prop="environment">
        <el-select
          v-model="form.environment"
          placeholder="选择环境"
          class="w-full"
        >
          <el-option
            label="生产环境"
            value="production"
          >
            <div class="env-option">
              <el-icon color="#EF4444"><CircleFilled /></el-icon>
              <span>生产环境</span>
            </div>
          </el-option>
          <el-option
            label="测试环境"
            value="testing"
          >
            <div class="env-option">
              <el-icon color="#F59E0B"><WarningFilled /></el-icon>
              <span>测试环境</span>
            </div>
          </el-option>
          <el-option
            label="开发环境"
            value="development"
          >
            <div class="env-option">
              <el-icon color="#10B981"><CircleCheckFilled /></el-icon>
              <span>开发环境</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-checkbox v-model="form.isDefault">
          设为默认实例
        </el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer-content">
        <!-- 测试结果提示 -->
        <div class="test-result-panel">
          <!-- 测试成功 -->
          <transition name="fade">
            <div
              v-if="testState === 'success'"
              class="result-item result-success"
            >
              <el-icon :size="20"><CircleCheckFilled /></el-icon>
              <div class="result-text">
                <div class="result-title">连接成功</div>
                <div class="result-detail">延迟 {{ testLatency }}ms</div>
              </div>
            </div>
          </transition>

          <!-- 测试失败 -->
          <transition name="fade">
            <div
              v-if="testState === 'fail'"
              class="result-item result-fail"
            >
              <el-icon :size="20"><CircleCloseFilled /></el-icon>
              <div class="result-text">
                <div class="result-title">连接失败</div>
                <div class="result-detail">{{ testError }}</div>
              </div>
            </div>
          </transition>

          <!-- 测试中 -->
          <transition name="fade">
            <div
              v-if="testState === 'testing'"
              class="result-item result-testing"
            >
              <el-icon :size="20" class="rotating"><Loading /></el-icon>
              <div class="result-text">
                <div class="result-title">正在测试连接...</div>
              </div>
            </div>
          </transition>
        </div>

        <!-- 按钮组 -->
        <div class="dialog-actions">
          <el-button @click="handleClose">取消</el-button>
          <el-button
            type="info"
            plain
            :loading="testState === 'testing'"
            :disabled="testState === 'testing'"
            @click="handleTest"
          >
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <el-button
            type="primary"
            :disabled="!canSave"
            :loading="saving"
            @click="handleSave"
          >
            <el-icon><Check /></el-icon>
            保存实例
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  Database,
  Connection,
  User,
  Lock,
  Files,
  CircleCheckFilled,
  CircleCloseFilled,
  Loading,
  Check,
  CircleFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { testDatabaseConnection, saveDatabaseInstance } from '@/api/instances'
import { useDatabaseForm } from '@/composables/useDatabaseForm'

interface Props {
  visible: boolean
  instance?: DatabaseInstance | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const isEdit = computed(() => !!props.instance?.id)

// 初始化表单
const {
  form,
  testState,
  testLatency,
  testError,
  canSave,
  handleTest,
  handleSave: saveInstance
} = useDatabaseForm(props.instance || undefined)

// 测试连接
async function handleTest() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const success = await handleTest()
  if (!success) {
    // 测试失败，不允许保存
  }
}

// 保存实例
async function handleSave() {
  if (!canSave.value) {
    ElMessage.warning('请先测试连接通过后再保存')
    return
  }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await saveInstance()
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    emit('update:visible', false)
    emit('saved')
  } catch (error: any) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

// 关闭对话框
async function handleClose() {
  // 如果有未保存的修改，提示用户
  if (canSave.value) {
    try {
      await ElMessageBox.confirm(
        '连接测试已通过，确定要放弃保存吗？',
        '提示',
        {
          type: 'warning',
          confirmButtonText: '确定放弃',
          cancelButtonText: '继续编辑'
        }
      )
    } catch {
      return
    }
  }

  emit('update:visible', false)
}

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入实例名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  host: [
    { required: true, message: '请输入主机地址', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '请输入端口', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  environment: [
    { required: true, message: '请选择环境类型', trigger: 'change' }
  ]
}
</script>

<style scoped>
.dialog-footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-lg);
}

.test-result-panel {
  flex: 1;
  min-width: 200px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-sm);
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-success {
  background: #D1FAE5;
  color: #065F46;
}

.result-fail {
  background: #FEE2E2;
  color: #991B1B;
}

.result-testing {
  background: #DBEAFE;
  color: #1E40AF;
}

.result-text {
  flex: 1;
}

.result-title {
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.result-detail {
  font-size: var(--font-size-xs);
  opacity: 0.8;
}

.dialog-actions {
  display: flex;
  gap: var(--spacing-sm);
  flex-shrink: 0;
}

.env-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
```

---

## 🤖 AI服务实例管理

### 1. 页面布局

```
┌───────────────────────────────────────────────────────────────┐
│  面包屑: 首页 > 实例管理 > AI 服务实例                        │
├───────────────────────────────────────────────────────────────┤
│  页面标题                                                      │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  🤖 AI 服务实例管理                                           │
│  为三个 AI Agent 配置算力服务，支持 OpenAI、Ollama、DeepSeek   │
├───────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  🩺 主治医生 (Diagnosis Agent)                         │  │
│  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │  │
│  │  提供商: OpenAI                    [🟢 已配置]          │  │
│  │  模型: gpt-4-turbo                                      │  │
│  │  Base URL: https://api.openai.com/v1                   │  │
│  │  最后验证: 5分钟前 (延迟 1.2s)                          │  │
│  │                                   [配置] [测试] [编辑]  │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  🧠 推理专家 (Reasoning Agent)                         │  │
│  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │  │
│  │  提供商: DeepSeek                    [🟢 已配置]        │  │
│  │  模型: deepseek-chat                                   │  │
│  │  Base URL: https://api.deepseek.com                    │  │
│  │  最后验证: 1小时前 (延迟 0.8s)                          │  │
│  │                                   [配置] [测试] [编辑]  │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  💻 编码专家 (Coding Agent)                            │  │
│  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │  │
│  │  ⚪ 未配置                                              │  │
│  │                                   [配置]               │  │
│  └─────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

### 2. 组件结构

```
AiServiceInstances (页面)
├─ PageHeader
└─ AgentGrid
   └─ AgentCard × 3
      ├─ AgentHeader
      │  ├─ AgentIcon (🩺/🧠/💻)
      │  ├─ AgentName
      │  ├─ AgentDescription
      │  └─ StatusBadge
      ├─ AgentBody
      │  ├─ ProviderInfo (提供商信息)
      │  ├─ ModelInfo (模型信息)
      │  └─ ValidationInfo (验证信息)
      └─ AgentFooter
         └─ ActionButtons (配置/测试/编辑)
```

### 3. 核心交互

#### 3.1 配置流程

```
点击"配置"
   │
   ▼
打开配置抽屉（Drawer）
   │
   ├─ 选择提供商（OpenAI/Ollama/DeepSeek/Anthropic/Azure）
   │
   ├─ 填写配置信息
   │  ├─ Base URL
   │  ├─ API Key
   │  ├─ Model Name
   │  └─ Temperature
   │
   ├─ 点击"测试连接"
   │  │
   │  ▼
   │  后端发送测试请求到 AI 服务
   │  │
   │  ▼
   │  显示结果
   │  ├─ ✅ 成功: 显示延迟 → "保存"按钮高亮
   │  └─ ❌ 失败: 显示错误 → "保存"按钮禁用
   │
   └─ 点击"保存"
      │
      ▼
   保存到数据库，刷新卡片
```

#### 3.2 代码实现

```typescript
// src/composables/useAiServiceForm.ts
export function useAiServiceForm(agentType: AgentType) {
  const form = reactive<AiServiceForm>({
    provider: 'openai',
    baseUrl: '',
    apiKey: '',
    model: '',
    temperature: 0.7,
    maxTokens: 2000
  })

  const testState = ref<TestState>('none')
  const testLatency = ref(0)
  const testResponse = ref('')

  // 根据提供商设置默认值
  function setDefaultsByProvider(provider: string) {
    const defaults = {
      openai: {
        baseUrl: 'https://api.openai.com/v1',
        model: 'gpt-4-turbo'
      },
      ollama: {
        baseUrl: 'http://localhost:11434',
        model: 'llama2'
      },
      deepseek: {
        baseUrl: 'https://api.deepseek.com',
        model: 'deepseek-chat'
      }
    }

    if (defaults[provider]) {
      form.baseUrl = defaults[provider].baseUrl
      form.model = defaults[provider].model
    }
  }

  // 测试连接
  async function handleTest(): Promise<boolean> {
    testState.value = 'testing'
    const startTime = Date.now()

    try {
      const response = await testAiService({
        provider: form.provider,
        baseUrl: form.baseUrl,
        apiKey: form.apiKey,
        model: form.model
      })

      testLatency.value = Date.now() - startTime
      testResponse.value = response.message || '连接成功'
      testState.value = 'success'
      ElMessage.success(`连接成功！延迟 ${testLatency.value}ms`)
      return true
    } catch (error: any) {
      testState.value = 'fail'
      testResponse.value = error.message || '连接失败'
      ElMessage.error(testResponse.value)
      return false
    }
  }

  return {
    form,
    testState,
    testLatency,
    testResponse,
    setDefaultsByProvider,
    handleTest
  }
}
```

---

## 🔌 API设计

### 1. 数据库实例 API

#### 1.1 获取实例列表

```typescript
GET /api/instances/database

Response:
{
  "success": true,
  "data": [
    {
      "id": "1",
      "name": "生产环境主库",
      "host": "192.168.1.100",
      "port": 3306,
      "username": "root",
      "database": "information_schema",
      "environment": "production",
      "isEnabled": true,
      "isDefault": true,
      "lastValidatedAt": "2024-02-04T10:30:00Z",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ]
}
```

#### 1.2 测试连接

```typescript
POST /api/instances/database/test

Request:
{
  "host": "192.168.1.100",
  "port": 3306,
  "username": "root",
  "password": "encrypted_password",
  "database": "information_schema"
}

Response:
{
  "success": true,
  "data": {
    "connected": true,
    "latency": 120,
    "version": "8.0.32",
    "database": "information_schema"
  }
}

Error Response:
{
  "success": false,
  "error": "Connection refused: connect"
}
```

#### 1.3 保存实例

```typescript
POST /api/instances/database
PUT /api/instances/database/:id

Request:
{
  "name": "生产环境主库",
  "host": "192.168.1.100",
  "port": 3306,
  "username": "root",
  "password": "encrypted_password",
  "database": "information_schema",
  "environment": "production",
  "isDefault": true
}

Response:
{
  "success": true,
  "data": {
    "id": "1",
    "name": "生产环境主库",
    ...
  }
}
```

### 2. AI 服务实例 API

#### 2.1 获取 AI 服务配置

```typescript
GET /api/instances/ai-service

Response:
{
  "success": true,
  "data": {
    "diagnosis": {
      "agentType": "diagnosis",
      "provider": "openai",
      "baseUrl": "https://api.openai.com/v1",
      "model": "gpt-4-turbo",
      "temperature": 0.7,
      "isEnabled": true,
      "lastValidatedAt": "2024-02-04T10:30:00Z"
    },
    "reasoning": { ... },
    "coding": { ... }
  }
}
```

#### 2.2 测试 AI 服务

```typescript
POST /api/instances/ai-service/test

Request:
{
  "agentType": "diagnosis",
  "provider": "openai",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-xxx",
  "model": "gpt-4-turbo"
}

Response:
{
  "success": true,
  "data": {
    "connected": true,
    "latency": 1200,
    "response": "Hello! I'm ready to help."
  }
}
```

---

## 📊 数据模型

### DatabaseInstance

```typescript
interface DatabaseInstance {
  // 基本信息
  id: string
  name: string
  host: string
  port: number
  username: string
  password: string  // 加密存储
  database?: string

  // 配置
  environment: 'production' | 'testing' | 'development'
  isEnabled: boolean
  isDefault: boolean

  // 验证信息
  lastValidatedAt?: Date

  // 元数据
  createdAt: Date
  updatedAt: Date
}
```

### AiServiceInstance

```typescript
interface AiServiceInstance {
  // 基本信息
  id: string
  agentType: 'diagnosis' | 'reasoning' | 'coding'
  provider: 'openai' | 'ollama' | 'deepseek' | 'anthropic' | 'azure'

  // 配置
  baseUrl: string
  apiKey: string  // 加密存储
  model: string
  temperature?: number
  maxTokens?: number

  // 状态
  isEnabled: boolean

  // 验证信息
  lastValidatedAt?: Date

  // 元数据
  createdAt: Date
  updatedAt: Date
}
```

---

## 🧪 测试用例

### 单元测试

```typescript
// DatabaseInstanceCard.spec.ts
describe('DatabaseInstanceCard', () => {
  it('should display instance information correctly', () => {
    // ...
  })

  it('should show online status when enabled', () => {
    // ...
  })

  it('should emit edit event when edit button clicked', () => {
    // ...
  })

  it('should emit delete event when delete button clicked', () => {
    // ...
  })

  it('should emit toggle event when switch changed', () => {
    // ...
  })
})

// DatabaseInstanceDialog.spec.ts
describe('DatabaseInstanceDialog', () => {
  it('should disable save button initially', () => {
    // ...
  })

  it('should keep save button disabled after test fails', () => {
    // ...
  })

  it('should enable save button after test succeeds', () => {
    // ...
  })

  it('should reset test state when form changes', () => {
    // ...
  })

  it('should validate form before test', () => {
    // ...
  })

  it('should save instance after successful test', () => {
    // ...
  })
})
```

### 集成测试

```typescript
// DatabaseInstances.spec.ts
describe('DatabaseInstances Page Integration', () => {
  it('should create new instance after test passes', async () => {
    // 1. 打开页面
    // 2. 点击"新增"
    // 3. 填写表单
    // 4. 点击"测试连接"
    // 5. 验证测试成功
    // 6. 验证"保存"按钮可用
    // 7. 点击"保存"
    // 8. 验证实例出现在列表中
  })

  it('should not save instance if test fails', async () => {
    // ...
  })

  it('should edit existing instance', async () => {
    // ...
  })

  it('should delete instance after confirmation', async () => {
    // ...
  })
})
```

---

## ✅ 验收标准

### 功能验收

- [ ] 能够新增数据库实例
- [ ] 只有测试连接通过后才能保存实例
- [ ] 能够编辑和删除实例
- [ ] 能够启用/禁用实例
- [ ] 搜索功能正常
- [ ] 能够配置三个 AI Agent
- [ ] 所有表单验证正确

### UI/UX 验收

- [ ] 卡片悬停有抬升效果
- [ ] 测试结果有动画显示
- [ ] 按钮状态正确（禁用/启用）
- [ ] 响应式布局适配
- [ ] 加载状态正确显示

### 性能验收

- [ ] 页面加载时间 < 1s
- [ ] 测试连接响应时间 < 3s
- [ ] 列表渲染流畅（100+ 实例）

### 测试验收

- [ ] 单元测试覆盖率 ≥ 80%
- [ ] 所有集成测试通过
- [ ] 无控制台错误

---

**文档版本**: 1.0.0
**最后更新**: 2026-02-04
