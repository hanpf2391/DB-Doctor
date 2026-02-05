<template>
  <el-dialog
    v-model="dialogVisible"
    title="修改密码和用户名"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="新用户名" prop="newUsername">
        <el-input
          v-model="form.newUsername"
          placeholder="留空则不修改用户名"
          :disabled="loading"
          clearable
        />
        <div class="form-tip">留空表示不修改用户名</div>
      </el-form-item>

      <el-form-item label="旧密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入旧密码"
          show-password
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码（6-20位）"
          show-password
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
          :disabled="loading"
          @keyup.enter="handleSubmit"
        />
      </el-form-item>

      <el-alert
        title="密码要求"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #default>
          <ul style="margin: 8px 0 0 0; padding-left: 20px">
            <li>密码长度为 6-20 个字符</li>
            <li>建议包含字母、数字和特殊字符</li>
            <li>用户名可以留空，表示不修改用户名</li>
          </ul>
        </template>
      </el-alert>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" :disabled="loading">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          确定修改
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, type FormInstance, type FormRules } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

// 对话框显示状态
const dialogVisible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (val) => {
    dialogVisible.value = val
    if (val) {
      // 对话框打开时重置表单
      resetForm()
    }
  }
)

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// 表单数据
const form = reactive({
  newUsername: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const rules: FormRules = {
  newUsername: [
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 1, max: 50, message: '密码长度为 1-50 个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 重置表单
function resetForm() {
  form.newUsername = ''
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

// 关闭对话框
function handleClose() {
  dialogVisible.value = false
  resetForm()
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return

  try {
    // 验证表单
    await formRef.value.validate()

    // 开始修改密码
    loading.value = true

    await authStore.changePassword({
      newUsername: form.newUsername.trim() || undefined, // 如果为空则不传
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })

    // 根据是否修改用户名显示不同的提示
    const newUsername = form.newUsername.trim()
    if (newUsername) {
      ElMessage.success(`用户名已修改为 "${newUsername}"，请使用新用户名重新登录`)
      // 将新用户名保存到 sessionStorage，登录页面可以自动填充
      sessionStorage.setItem('new_username_after_change', newUsername)
    } else {
      ElMessage.success('密码修改成功，请重新登录')
    }

    // 关闭对话框
    dialogVisible.value = false

    // 触发成功事件，传递新用户名
    emit('success', newUsername)
  } catch (error: any) {
    console.error('修改密码失败:', error)
    // 错误信息已经在 store 中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

:deep(.el-alert__content) {
  padding: 0;
}
</style>
