<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <!-- 项目 Logo 图片 -->
        <img src="/logo.png" alt="DB-Doctor" class="project-logo" />

        <h1 class="login-title">DB-Doctor</h1>
        <p class="login-subtitle">MySQL 慢查询智能诊疗系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
            :disabled="loading"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            :disabled="loading"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>

        <div v-if="errorMessage" class="error-message">
          <el-alert :title="errorMessage" type="error" :closable="false" show-icon />
        </div>

        <div class="login-tips">
          <p>默认账号：dbdoctor</p>
          <p>默认密码：dbdoctor</p>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

// 路由
const router = useRouter()

// Store
const authStore = useAuthStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 1, max: 50, message: '密码长度为 1-50 个字符', trigger: 'blur' }
  ]
}

// 加载状态
const loading = ref(false)
const errorMessage = ref('')

// 检查是否已登录
onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/')
    return
  }

  // 检查是否有刚修改的用户名，自动填充
  const newUsername = sessionStorage.getItem('new_username_after_change')
  if (newUsername) {
    loginForm.username = newUsername
    // 清除标记，避免下次还填充
    sessionStorage.removeItem('new_username_after_change')
  }
})

/**
 * 处理登录
 */
async function handleLogin() {
  if (!loginFormRef.value) return

  try {
    // 清除错误信息
    errorMessage.value = ''
    authStore.clearError()

    // 验证表单
    await loginFormRef.value.validate()

    // 开始登录
    loading.value = true

    await authStore.login({
      username: loginForm.username,
      password: loginForm.password
    })

    // 登录成功
    ElMessage.success('登录成功')

    // 跳转到首页
    router.push('/')
  } catch (error: any) {
    console.error('登录失败:', error)

    // 显示错误信息
    if (error.message) {
      errorMessage.value = error.message
    } else if (error.errors) {
      // 表单验证错误
      errorMessage.value = '请检查输入'
    } else {
      errorMessage.value = '登录失败，请重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #ffffff;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

/* 项目 Logo */
.project-logo {
  display: block;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  object-fit: contain;
  border-radius: 20px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.project-logo:hover {
  transform: translateY(-4px) scale(1.05);
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}

.error-message {
  margin-bottom: 15px;
}

.login-tips {
  margin-top: 30px;
  padding: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  text-align: center;
}

.login-tips p {
  font-size: 13px;
  color: #6b7280;
  margin: 4px 0;
  line-height: 1.5;
}
</style>
