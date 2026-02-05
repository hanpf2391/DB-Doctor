<template>
  <el-dialog
    v-model="dialogVisible"
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
    >
      <el-form-item label="实例名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="例如：生产环境主库"
          clearable
        >
          <template #prefix>
            <el-icon><Coin /></el-icon>
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
              <el-icon color="#EF4444"><SuccessFilled /></el-icon>
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
        <!-- 测试结果面板 -->
        <div class="test-result-panel">
          <!-- 测试成功 -->
          <transition name="fade">
            <div
              v-if="testState === 'success'"
              class="test-success-container"
            >
              <div class="result-item result-success">
                <el-icon :size="24"><CircleCheckFilled /></el-icon>
                <div class="result-text">
                  <div class="result-title">连接测试完成</div>
                  <div class="result-detail">延迟 {{ testLatency }}ms</div>
                </div>
              </div>

              <!-- 环境检查结果 -->
              <div v-if="testResult && testResult.environmentChecks" class="environment-checks">
                <!-- 检查摘要 -->
                <div class="check-summary" :class="hasFailedChecks ? 'has-failed' : 'all-passed'">
                  <div class="summary-icon">
                    <el-icon :size="20">
                      <SuccessFilled v-if="!hasFailedChecks" />
                      <WarningFilled v-else />
                    </el-icon>
                  </div>
                  <div class="summary-content">
                    <div class="summary-title">
                      环境检查{{ hasFailedChecks ? '未通过' : '全部通过' }}
                    </div>
                    <div class="summary-desc">
                      {{ hasFailedChecks ? '部分配置需要调整才能正常工作' : '您的数据库配置符合要求' }}
                    </div>
                  </div>
                  <div class="summary-stats">
                    <span class="stat-item">
                      <span class="stat-label">通过:</span>
                      <span class="stat-value stat-pass">{{ passedCount }}</span>
                    </span>
                    <span class="stat-item">
                      <span class="stat-label">失败:</span>
                      <span class="stat-value stat-fail">{{ failedCount }}</span>
                    </span>
                  </div>
                </div>

                <!-- 检查项列表 -->
                <div class="check-items-list">
                  <div
                    v-for="check in testResult.environmentChecks"
                    :key="check.name"
                    class="check-item"
                    :class="`check-${check.status}`"
                  >
                    <div class="check-item-header">
                      <div class="check-name">
                        <span class="check-display-name">{{ check.displayName }}</span>
                        <code class="check-var-name">{{ check.name }}</code>
                      </div>
                      <el-tag
                        :type="check.status === 'pass' ? 'success' : check.status === 'fail' ? 'danger' : 'warning'"
                        size="small"
                        effect="plain"
                      >
                        {{ check.status === 'pass' ? '✓ 通过' : check.status === 'fail' ? '✗ 未通过' : '⚠ 警告' }}
                      </el-tag>
                    </div>

                    <div class="check-current">
                      <span class="check-label">当前值:</span>
                      <code class="check-value">{{ check.currentValue }}</code>
                    </div>

                    <div class="check-description">
                      <el-icon :size="14"><InfoFilled /></el-icon>
                      <span>{{ check.description }}</span>
                    </div>

                    <!-- 修复建议 -->
                    <div v-if="check.fixSql" class="check-fix">
                      <div class="fix-header" @click="toggleFix(check.name)">
                        <el-icon :size="14">
                          <Tools />
                        </el-icon>
                        <span class="fix-title">修复方案</span>
                        <el-icon class="toggle-icon" :size="14">
                          <ArrowDown v-if="!expandedFixes[check.name]" />
                          <ArrowUp v-else />
                        </el-icon>
                      </div>
                      <div v-show="expandedFixes[check.name]" class="fix-content">
                        <div class="fix-desc">{{ check.fixDescription || '执行以下 SQL 语句来修复此问题' }}</div>
                        <div class="fix-sql-box">
                          <pre class="fix-sql">{{ check.fixSql }}</pre>
                          <el-button
                            size="small"
                            type="primary"
                            link
                            @click="copySql(check.fixSql)"
                          >
                            <el-icon :size="14"><DocumentCopy /></el-icon>
                            复制 SQL
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 数据库信息 -->
              <div v-if="testResult" class="database-info">
                <div class="info-item">
                  <span class="info-label">连接状态:</span>
                  <el-tag type="success" size="small">成功</el-tag>
                </div>
                <div v-if="testResult.databaseCount" class="info-item">
                  <span class="info-label">可用数据库:</span>
                  <span class="info-value">{{ testResult.databaseCount }} 个</span>
                </div>
                <div v-if="testResult.version" class="info-item">
                  <span class="info-label">MySQL 版本:</span>
                  <span class="info-value">{{ testResult.version }}</span>
                </div>
              </div>
            </div>
          </transition>

          <!-- 测试失败但有环境检查结果 -->
          <transition name="fade">
            <div
              v-if="testState === 'fail' && testResult && testResult.environmentChecks"
              class="test-fail-container"
            >
              <div class="result-item result-fail">
                <el-icon :size="24"><WarningFilled /></el-icon>
                <div class="result-text">
                  <div class="result-title">环境检查未通过</div>
                  <div class="result-detail">{{ testError }}</div>
                </div>
              </div>

              <!-- 环境检查结果 -->
              <div class="environment-checks">
                <!-- 检查摘要 -->
                <div class="check-summary has-failed">
                  <div class="summary-icon">
                    <el-icon :size="20"><WarningFilled /></el-icon>
                  </div>
                  <div class="summary-content">
                    <div class="summary-title">环境检查未通过</div>
                    <div class="summary-desc">部分配置需要调整才能正常工作</div>
                  </div>
                  <div class="summary-stats">
                    <span class="stat-item">
                      <span class="stat-label">通过:</span>
                      <span class="stat-value stat-pass">{{ passedCount }}</span>
                    </span>
                    <span class="stat-item">
                      <span class="stat-label">失败:</span>
                      <span class="stat-value stat-fail">{{ failedCount }}</span>
                    </span>
                  </div>
                </div>

                <!-- 检查项列表 -->
                <div class="check-items-list">
                  <div
                    v-for="check in testResult.environmentChecks"
                    :key="check.name"
                    class="check-item"
                    :class="`check-${check.status}`"
                  >
                    <div class="check-item-header">
                      <div class="check-name">
                        <span class="check-display-name">{{ check.displayName }}</span>
                        <code class="check-var-name">{{ check.name }}</code>
                      </div>
                      <el-tag
                        :type="check.status === 'pass' ? 'success' : check.status === 'fail' ? 'danger' : 'warning'"
                        size="small"
                        effect="plain"
                      >
                        {{ check.status === 'pass' ? '✓ 通过' : check.status === 'fail' ? '✗ 未通过' : '⚠ 警告' }}
                      </el-tag>
                    </div>

                    <div class="check-current">
                      <span class="check-label">当前值:</span>
                      <code class="check-value">{{ check.currentValue }}</code>
                    </div>

                    <div class="check-description">
                      <el-icon :size="14"><InfoFilled /></el-icon>
                      <span>{{ check.description }}</span>
                    </div>

                    <!-- 修复建议 -->
                    <div v-if="check.fixSql" class="check-fix">
                      <div class="fix-header" @click="toggleFix(check.name)">
                        <el-icon :size="14"><Tools /></el-icon>
                        <span class="fix-title">修复方案</span>
                        <el-icon class="toggle-icon" :size="14">
                          <ArrowDown v-if="!expandedFixes[check.name]" />
                          <ArrowUp v-else />
                        </el-icon>
                      </div>
                      <div v-show="expandedFixes[check.name]" class="fix-content">
                        <div class="fix-desc">{{ check.fixDescription || '执行以下 SQL 语句来修复此问题' }}</div>
                        <div class="fix-sql-box">
                          <pre class="fix-sql">{{ check.fixSql }}</pre>
                          <el-button
                            size="small"
                            type="primary"
                            link
                            @click="copySql(check.fixSql)"
                          >
                            <el-icon :size="14"><DocumentCopy /></el-icon>
                            复制 SQL
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 数据库信息 -->
              <div v-if="testResult" class="database-info">
                <div class="info-item">
                  <span class="info-label">连接状态:</span>
                  <el-tag type="warning" size="small">成功</el-tag>
                </div>
                <div v-if="testResult.databaseCount" class="info-item">
                  <span class="info-label">可用数据库:</span>
                  <span class="info-value">{{ testResult.databaseCount }} 个</span>
                </div>
                <div v-if="testResult.version" class="info-item">
                  <span class="info-label">MySQL 版本:</span>
                  <span class="info-value">{{ testResult.version }}</span>
                </div>
              </div>
            </div>
          </transition>

          <!-- 测试失败（没有环境检查结果） -->
          <transition name="fade">
            <div
              v-if="testState === 'fail' && (!testResult || !testResult.environmentChecks)"
              class="result-item result-fail"
            >
              <el-icon :size="24"><CircleCloseFilled /></el-icon>
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
              <el-icon :size="24" class="rotating"><Loading /></el-icon>
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
            @click="onTestClick"
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
import { ref, watch, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  Coin,
  Connection,
  User,
  Lock,
  Files,
  CircleCheckFilled,
  CircleCloseFilled,
  Loading,
  Check,
  SuccessFilled,
  WarningFilled,
  Warning,
  InfoFilled,
  Tools,
  ArrowDown,
  ArrowUp,
  DocumentCopy
} from '@element-plus/icons-vue'
import type { DatabaseInstance, DatabaseInstanceForm } from '@/types/instances'
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

// 使用组合式函数
const {
  form,
  testState,
  testLatency,
  testError,
  testResult,
  canSave,
  isEdit,
  handleTest,
  reset
} = useDatabaseForm(props.instance || undefined)

// 修复方案展开状态
const expandedFixes = reactive<Record<string, boolean>>({})

// 对话框显示状态
const dialogVisible = ref(props.visible)

// 监听 props.visible 变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
})

// 监听 dialogVisible 变化
watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

// 计算是否有失败的检查项
const hasFailedChecks = computed(() => {
  if (!testResult.value?.environmentChecks) return false
  return testResult.value.environmentChecks.some(check => check.status === 'fail')
})

// 计算通过和失败的数量
const passedCount = computed(() => {
  if (!testResult.value?.environmentChecks) return 0
  return testResult.value.environmentChecks.filter(check => check.status === 'pass').length
})

const failedCount = computed(() => {
  if (!testResult.value?.environmentChecks) return 0
  return testResult.value.environmentChecks.filter(check => check.status === 'fail').length
})

// 切换修复方案展开/折叠
function toggleFix(checkName: string) {
  expandedFixes[checkName] = !expandedFixes[checkName]
}

// 复制 SQL
async function copySql(sql: string) {
  try {
    await navigator.clipboard.writeText(sql)
    ElMessage.success('SQL 已复制到剪贴板')
  } catch {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = sql
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('SQL 已复制到剪贴板')
  }
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

// 测试连接（带表单验证）
async function onTestClick() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 调用 composable 中的测试函数
  await handleTest()
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
    // TODO: 调用保存 API
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟 API 调用

    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
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

  dialogVisible.value = false
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
  background: transparent;
  border: 1px solid #dcfce7;
  color: #166534;
}

.result-fail {
  background: #FEE2E2;
  color: #991B1B;
}

.result-testing {
  background: #DBEAFE;
  color: #1E40AF;
}

.test-fail-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.test-success-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.environment-checks {
  margin-top: var(--spacing-sm);
  padding: 0;
  background: transparent;
  border: none;
}

.check-summary {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
  font-size: var(--font-size-sm);
}

.check-summary.all-passed {
  background: transparent;
  border: 1px solid #86efac;
}

.check-summary.has-failed {
  background: transparent;
  border: 1px solid #fca5a5;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.all-passed .summary-icon {
  background: #f0fdf4;
  color: #16a34a;
}

.has-failed .summary-icon {
  background: #fef2f2;
  color: #dc2626;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-weight: 500;
  font-size: var(--font-size-base);
  margin-bottom: 4px;
  color: var(--color-text-primary);
}

.summary-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.summary-stats {
  display: flex;
  gap: var(--spacing-md);
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
}

.stat-label {
  color: var(--color-text-muted);
}

.stat-value {
  font-weight: 500;
  font-size: var(--font-size-sm);
}

.stat-pass {
  color: #16a34a;
}

.stat-fail {
  color: #dc2626;
}

.check-items-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.check-item {
  padding: var(--spacing-md);
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  transition: all var(--transition-fast);
}

.check-item:hover {
  border-color: var(--color-primary);
}

.check-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.check-name {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.check-display-name {
  font-weight: 500;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.check-var-name {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: var(--font-size-xs);
  padding: 2px 6px;
  background: var(--color-bg-sidebar);
  border-radius: 4px;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border-light);
}

.check-current {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) 0;
  margin-bottom: var(--spacing-xs);
  font-size: var(--font-size-xs);
}

.check-label {
  color: var(--color-text-muted);
  font-weight: 400;
}

.check-value {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: var(--font-size-xs);
  padding: 2px 8px;
  background: var(--color-bg-sidebar);
  border-radius: 4px;
  color: var(--color-text-primary);
  border: 1px solid var(--color-border-light);
}

.check-description {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) 0;
  margin-bottom: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.check-fix {
  margin-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border-light);
  padding-top: var(--spacing-sm);
}

.fix-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  user-select: none;
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
}

.fix-header:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-primary);
}

.fix-title {
  flex: 1;
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text-primary);
}

.toggle-icon {
  transition: transform var(--transition-fast);
}

.fix-content {
  margin-top: var(--spacing-sm);
}

.fix-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
  padding: 0 var(--spacing-xs);
}

.fix-sql-box {
  position: relative;
  background: #f8fafc;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: var(--spacing-sm);
}

.fix-sql {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-primary);
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  padding-right: 80px;
}

.fix-sql-box .el-button {
  position: absolute;
  top: var(--spacing-sm);
  right: var(--spacing-sm);
}

.database-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-md);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-sm);
}

.info-label {
  color: var(--color-text-muted);
  font-weight: 400;
}

.info-value {
  color: var(--color-text-primary);
  font-weight: 500;
}

/* 暗色主题适配 */
[data-theme="dark"] .result-success {
  background: transparent;
  border-color: #16a34a;
  color: #4ade80;
}

[data-theme="dark"] .result-fail {
  background: transparent;
  border-color: #dc2626;
  color: #f87171;
}

[data-theme="dark"] .check-summary.all-passed {
  background: transparent;
  border-color: #16a34a;
}

[data-theme="dark"] .check-summary.has-failed {
  background: transparent;
  border-color: #dc2626;
}

[data-theme="dark"] .all-passed .summary-icon {
  background: rgba(22, 163, 74, 0.2);
  color: #4ade80;
}

[data-theme="dark"] .has-failed .summary-icon {
  background: rgba(220, 38, 38, 0.2);
  color: #f87171;
}

[data-theme="dark"] .environment-checks {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] .check-item {
  background: #0a0a0a;
  border-color: #262626;
}

[data-theme="dark"] .check-item:hover {
  border-color: #404040;
}

[data-theme="dark"] .check-var-name,
[data-theme="dark"] .check-value {
  background: #1a1a1a;
  color: #d4d4d4;
}

[data-theme="dark"] .fix-header {
  background: #1a1a1a;
  color: #fbbf24;
}

[data-theme="dark"] .fix-header:hover {
  background: #2a2a2a;
}

[data-theme="dark"] .fix-sql-box {
  background: #0a0a0a;
  border: 1px solid #262626;
}

[data-theme="dark"] .fix-sql {
  color: #d4d4d4;
}

[data-theme="dark"] .check-var-name,
[data-theme="dark"] .check-value {
  background: #1a1a1a;
  color: #d4d4d4;
  border-color: #262626;
}

[data-theme="dark"] .database-info {
  background: transparent;
  border-color: #262626;
}

[data-theme="dark"] .check-item {
  background: #0a0a0a;
  border-color: #262626;
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
