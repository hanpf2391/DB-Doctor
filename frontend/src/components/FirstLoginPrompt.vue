<template>
  <el-dialog
    v-model="dialogVisible"
    title="安全提示"
    width="480px"
    :close-on-click-modal="false"
    :show-close="false"
    class="first-login-dialog"
  >
    <div class="prompt-content">
      <el-icon class="warning-icon" :size="48" color="#e6a23c">
        <Warning />
      </el-icon>

      <h3 class="prompt-title">您正在使用默认密码登录</h3>

      <p class="prompt-description">
        为了您的账户安全，我们强烈建议您立即修改默认密码。
      </p>

      <el-alert
        title="默认密码信息"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      >
        <template #default>
          <div class="password-info">
            <p><strong>用户名:</strong> dbdoctor</p>
            <p><strong>默认密码:</strong> dbdoctor</p>
          </div>
        </template>
      </el-alert>

      <div class="security-tips">
        <h4>密码安全建议:</h4>
        <ul>
          <li>使用包含字母、数字和特殊字符的复杂密码</li>
          <li>密码长度至少 6 个字符</li>
          <li>不要使用与其他网站相同的密码</li>
          <li>定期更换密码</li>
        </ul>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleSkip" :disabled="loading">
          稍后再说
        </el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">
          立即修改密码
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Warning } from '@element-plus/icons-vue'

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
.prompt-content {
  text-align: center;
  padding: 20px 0;
}

.warning-icon {
  margin-bottom: 16px;
}

.prompt-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 12px 0;
}

.prompt-description {
  font-size: 14px;
  color: var(--el-text-color-regular);
  margin: 0 0 20px 0;
  line-height: 1.6;
}

.password-info {
  text-align: left;
  padding: 8px 0;
}

.password-info p {
  margin: 8px 0;
  font-size: 14px;
}

.security-tips {
  margin-top: 20px;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  text-align: left;
}

.security-tips h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.security-tips ul {
  margin: 0;
  padding-left: 20px;
  list-style: disc;
}

.security-tips li {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin: 6px 0;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}

:deep(.first-login-dialog .el-dialog__header) {
  text-align: center;
}

:deep(.first-login-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}
</style>
