<template>
  <el-dialog
    v-model="dialogVisible"
    title=""
    width="420px"
    :close-on-click-modal="false"
    :show-close="false"
    class="first-login-prompt"
    :class="{ 'is-dark': isDark }"
  >
    <div class="prompt-wrapper">
      <h3 class="prompt-title">安全提示</h3>
      <p class="prompt-description">
        您正在使用默认密码登录。为了账户安全，建议您立即修改密码。
      </p>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <button class="btn-secondary" @click="handleSkip" :disabled="loading">
          稍后再说
        </button>
        <button class="btn-primary" :disabled="loading" @click="handleConfirm">
          {{ loading ? '处理中...' : '立即修改' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'confirm'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const dialogVisible = ref(props.modelValue)
const isDark = ref(false)

// 检测暗色主题
function checkTheme() {
  const hasDataTheme = document.documentElement.getAttribute('data-theme') === 'dark'
  const hasClass = document.documentElement.classList.contains('dark')
  const hasBodyClass = document.body.classList.contains('dark')
  isDark.value = hasDataTheme || hasClass || hasBodyClass
}

let themeObserver: MutationObserver | null = null

onMounted(() => {
  checkTheme()
  themeObserver = new MutationObserver(checkTheme)
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme', 'class']
  })
  themeObserver.observe(document.body, {
    attributes: true,
    attributeFilter: ['class']
  })
})

onUnmounted(() => {
  if (themeObserver) {
    themeObserver.disconnect()
  }
})

watch(
  () => props.modelValue,
  (val) => {
    dialogVisible.value = val
  }
)

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// 跳过修改密码
function handleSkip() {
  dialogVisible.value = false
}

// 确认修改密码
function handleConfirm() {
  loading.value = true
  dialogVisible.value = false
  emit('confirm')
  loading.value = false
}
</script>

<style scoped>
/* ============================================
   首次登录提示 - 极简设计
============================================ */

.prompt-wrapper {
  padding: 20px 8px;
}

/* 标题和描述 */
.prompt-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  text-align: center;
  margin: 0 0 12px 0;
  letter-spacing: -0.3px;
}

.prompt-description {
  font-size: 14px;
  color: #6b7280;
  text-align: center;
  margin: 0;
  line-height: 1.6;
}

/* 底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.btn-secondary,
.btn-primary {
  padding: 10px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
}

.btn-secondary {
  background: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #111827;
  color: white;
  border: 1px solid transparent;
}

.btn-primary:hover:not(:disabled) {
  background: #000000;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ============================================
   暗色主题
============================================ */

.is-dark .prompt-title {
  color: #f3f4f6;
}

.is-dark .prompt-description {
  color: #9ca3af;
}

.is-dark .btn-secondary {
  background: transparent;
  color: #e5e7eb;
  border-color: rgba(255, 255, 255, 0.2);
}

.is-dark .btn-secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.is-dark .btn-primary {
  background: #111827;
  color: white;
}

.is-dark .btn-primary:hover:not(:disabled) {
  background: #000000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
}

/* Dialog 样式覆盖 - 强制纯白 */
:deep(.first-login-prompt.el-dialog) {
  border-radius: 12px !important;
  background: #ffffff !important;
  border: none !important;
}

:deep(.first-login-prompt .el-dialog__header) {
  padding: 0 !important;
  margin-bottom: 0 !important;
  display: none !important;
  background: #ffffff !important;
}

:deep(.first-login-prompt .el-dialog__title) {
  color: #000000 !important;
  background: #ffffff !important;
}

:deep(.first-login-prompt .el-dialog__headerbtn) {
  display: none !important;
}

:deep(.first-login-prompt .el-dialog__body) {
  padding: 32px 32px 24px !important;
  background: #ffffff !important;
}

:deep(.first-login-prompt .el-dialog__footer) {
  padding: 0 32px 32px !important;
  background: #ffffff !important;
  border-top: none !important;
}

/* 暗色主题 Dialog - 强制纯黑 */
:deep(.is-dark.first-login-prompt.el-dialog) {
  background: #000000 !important;
  border: 1px solid #1f2937 !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;
}

:deep(.is-dark.first-login-prompt .el-dialog__header) {
  background: #000000 !important;
}

:deep(.is-dark.first-login-prompt .el-dialog__title) {
  color: #ffffff !important;
  background: #000000 !important;
}

:deep(.is-dark.first-login-prompt .el-dialog__body) {
  background: #000000 !important;
}

:deep(.is-dark.first-login-prompt .el-dialog__footer) {
  background: #000000 !important;
}
</style>

<style>
/* 全局样式 - 覆盖 Element Plus 默认样式 */

/* 浅色主题 */
.first-login-prompt.el-dialog {
  background: #ffffff !important;
  border: none !important;
}

.first-login-prompt .el-dialog__header {
  background: #ffffff !important;
  border: none !important;
}

.first-login-prompt .el-dialog__title {
  color: #000000 !important;
  background: #ffffff !important;
}

.first-login-prompt .el-dialog__body {
  background: #ffffff !important;
}

.first-login-prompt .el-dialog__footer {
  background: #ffffff !important;
  border-top: none !important;
}

/* 遮罩层 */
.el-overlay.first-login-prompt {
  background-color: rgba(0, 0, 0, 0.5) !important;
}

/* 暗色主题 */
.is-dark.first-login-prompt.el-dialog {
  background: #000000 !important;
  border: 1px solid #1f2937 !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;
}

.is-dark.first-login-prompt .el-dialog__header {
  background: #000000 !important;
  border: none !important;
}

.is-dark.first-login-prompt .el-dialog__title {
  color: #ffffff !important;
  background: #000000 !important;
}

.is-dark.first-login-prompt .el-dialog__body {
  background: #000000 !important;
}

.is-dark.first-login-prompt .el-dialog__footer {
  background: #000000 !important;
  border-top: none !important;
}
</style>
