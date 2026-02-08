<template>
  <div class="app-container">
    <!-- 登录页面：全屏显示，无侧边栏 -->
    <div v-if="isLoginPage" class="login-page-wrapper">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <!-- 主应用：侧边栏 + 主内容区 -->
    <template v-else>
      <!-- 浅色侧边栏 - Notion 风格 -->
      <aside class="sidebar">
        <!-- 侧边栏标题 -->
        <div class="sidebar-title">
          <img src="/logo.png" alt="DB-Doctor" class="title-logo" />
          <span class="title-text">DB-Doctor</span>
        </div>

        <!-- 导航菜单 -->
        <nav class="sidebar-nav">
          <template v-for="(item, index) in menuItems" :key="index">
            <!-- 分组标题 -->
            <div v-if="item.type === 'group'" class="nav-divider">
              {{ item.name }}
            </div>

            <!-- 菜单项 -->
            <router-link
              v-else
              :to="item.path"
              class="nav-item"
              :class="{ active: isActive(item.path) }"
            >
              <el-icon class="nav-icon">
                <component :is="item.icon" />
              </el-icon>
              <span class="nav-text">{{ item.name }}</span>
              <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
            </router-link>
          </template>
        </nav>

        <!-- 底部信息 -->
        <div class="sidebar-footer">
          <!-- 主题切换按钮 -->
          <button class="theme-toggle" @click="toggleTheme" :title="theme === 'light' ? '切换到暗色主题' : '切换到浅色主题'">
            <el-icon class="theme-icon">
              <Sunny v-if="theme === 'light'" />
              <Moon v-else />
            </el-icon>
            <span class="theme-text">{{ theme === 'light' ? '浅色' : '暗色' }}</span>
          </button>

          <div class="version-info">
            <span class="version-label">v3.1.0</span>
          </div>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="main-content">
        <!-- 顶部用户菜单 -->
        <header v-if="showUserMenu" class="top-header">
          <div class="header-content">
            <div class="header-left">
              <h1 class="page-title">{{ currentPageTitle }}</h1>
            </div>
            <div class="header-right">
              <!-- 用户下拉菜单 -->
              <el-dropdown trigger="click" @command="handleUserMenuCommand">
                <div class="user-menu-trigger">
                  <div class="user-avatar-circle" :style="{ backgroundColor: getUserAvatarColor(currentUser?.username) }">
                    {{ getUserAvatarText(currentUser?.username) }}
                  </div>
                  <span class="username-top">{{ currentUser?.username }}</span>
                  <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item disabled>
                      <div class="dropdown-user-info">
                        <div class="dropdown-user-avatar" :style="{ backgroundColor: getUserAvatarColor(currentUser?.username) }">
                          {{ getUserAvatarText(currentUser?.username) }}
                        </div>
                        <div class="dropdown-user-details">
                          <div class="dropdown-user-name">{{ currentUser?.username }}</div>
                          <div class="dropdown-user-status">已登录</div>
                        </div>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item divided command="changePassword">
                      <el-icon><Lock /></el-icon>
                      修改密码和用户名
                    </el-dropdown-item>
                    <el-dropdown-item command="logout" style="color: #f56c6c">
                      <el-icon><SwitchButton /></el-icon>
                      退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </header>

        <!-- 路由内容 -->
        <div class="page-content" :class="{ 'with-header': showUserMenu }">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>

      <!-- 首次启动引导 -->
      <StartupGuide />

      <!-- 修改密码对话框 -->
      <ChangePasswordDialog
        v-model="showChangePasswordDialog"
        @success="handlePasswordChanged"
      />

      <!-- 首次登录提示对话框 -->
      <FirstLoginPrompt
        v-model="showFirstLoginPrompt"
        @confirm="handleFirstLoginConfirm"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Odometer,
  Briefcase,
  Connection,
  Monitor,
  DataAnalysis,
  Setting,
  TrendCharts,
  Sunny,
  Moon,
  User,
  SwitchButton,
  Lock,
  ArrowDown,
  Bell
} from '@element-plus/icons-vue'
import StartupGuide from '@/components/StartupGuide.vue'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import FirstLoginPrompt from '@/components/FirstLoginPrompt.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const searchText = ref('')

// 对话框显示状态
const showChangePasswordDialog = ref(false)
const showFirstLoginPrompt = ref(false)

// 主题管理
const theme = ref<'light' | 'dark'>('light')

// 从 localStorage 读取主题设置
onMounted(() => {
  const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null
  if (savedTheme) {
    theme.value = savedTheme
  } else {
    // 根据系统偏好设置默认主题
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    theme.value = prefersDark ? 'dark' : 'light'
  }
  applyTheme()

  // 检查是否使用默认密码登录（首次登录提示）
  checkFirstLogin()
})

// 监听主题变化并应用
watch(theme, (newTheme) => {
  localStorage.setItem('theme', newTheme)
  applyTheme()
}, { immediate: true })

// 应用主题到 DOM（使用 View Transitions API 优化）
function applyTheme() {
  const html = document.documentElement
  const themeValue = theme.value === 'dark' ? 'dark' : ''

  // 检查浏览器是否支持 View Transitions API
  if (document.startViewTransition) {
    // 使用 View Transitions API 实现丝滑切换
    document.startViewTransition(() => {
      if (themeValue) {
        html.setAttribute('data-theme', themeValue)
      } else {
        html.removeAttribute('data-theme')
      }
    })
  } else {
    // 降级方案：使用 CSS 过渡
    html.classList.add('theme-transitioning')

    if (themeValue) {
      html.setAttribute('data-theme', themeValue)
    } else {
      html.removeAttribute('data-theme')
    }

    // 移除过渡类（让过渡动画完成）
    setTimeout(() => {
      html.classList.remove('theme-transitioning')
    }, 300)
  }
}

// 切换主题
function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
}

// 判断是否为登录页
const isLoginPage = computed(() => route.path === '/login')

// 是否显示顶部用户菜单（除了登录页）
const showUserMenu = computed(() => !isLoginPage.value && authStore.isAuthenticated)

// 当前登录用户
const currentUser = computed(() => authStore.currentUser)

// 当前页面标题
const currentPageTitle = computed(() => route.meta.title as string || 'DB-Doctor')

// 检查是否首次登录（使用默认密码）
function checkFirstLogin() {
  if (authStore.isAuthenticated && authStore.username === 'dbdoctor') {
    // 检查是否显示过提示
    const hasShownPrompt = sessionStorage.getItem('first_login_prompt_shown')
    if (!hasShownPrompt) {
      showFirstLoginPrompt.value = true
    }
  }
}

// 首次登录提示确认
function handleFirstLoginConfirm() {
  sessionStorage.setItem('first_login_prompt_shown', 'true')
  showFirstLoginPrompt.value = false
  showChangePasswordDialog.value = true
}

// 密码修改成功回调
function handlePasswordChanged(newUsername?: string) {
  // 如果修改了用户名，额外提示
  if (newUsername && newUsername !== authStore.username) {
    console.log(`用户名已修改: ${authStore.username} -> ${newUsername}`)
  }

  // 登出并跳转到登录页
  handleLogout()
}

// 处理登出
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}

// 处理用户菜单命令
function handleUserMenuCommand(command: string) {
  switch (command) {
    case 'changePassword':
      showChangePasswordDialog.value = true
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 获取用户头像文字（首字母或前两个字）
function getUserAvatarText(username?: string): string {
  if (!username) return '?'
  // 中文用户名取第一个字
  if (/[\u4e00-\u9fa5]/.test(username[0])) {
    return username[0]
  }
  // 英文用户名取第一个字母大写
  return username[0].toUpperCase()
}

// 获取用户头像颜色（固定紫色）
function getUserAvatarColor(username?: string): string {
  return 'rgb(139, 92, 246)'
}

const menuItems = [
  { name: '仪表盘', path: '/', icon: Odometer },
  { name: 'AI 监控', path: '/ai-monitor', icon: Monitor },
  { name: '监控中心', path: '/monitoring', icon: TrendCharts },
  { name: '告警与通知', path: '/alert-notify', icon: Bell },
  { name: '慢查询报表', path: '/reports', icon: DataAnalysis },
  { type: 'group', name: '管理' },
  { name: '实例管理', path: '/instances', icon: Connection },
  { name: '设置中心', path: '/settings', icon: Setting }
]

function isActive(path: string): boolean {
  if (path === '/') {
    return route.path === '/'
  }
  return route.path.startsWith(path)
}
</script>

<style>
/* === 应用容器 === */
.app-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

/* === 浅色侧边栏 - Notion 风格 === */
.sidebar {
  width: 260px;
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 侧边栏标题 - Notion 风格极简设计 */
.sidebar-title {
  padding: 20px 20px 16px;
  border-bottom: 1px solid transparent;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.title-text {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

[data-theme="dark"] .title-text {
  color: #737373;
}

/* Logo 区域 */
.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.logo-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
}

.logo-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
}

[data-theme="dark"] .logo-title {
  color: #ffffff;
}

/* 搜索框 */
.search-wrapper {
  position: relative;
  padding: 16px 20px 8px;
}

.search-icon {
  position: absolute;
  left: 32px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  font-size: 16px;
}

[data-theme="dark"] .search-icon {
  color: #737373;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  outline: none;
  transition: all var(--transition-base);
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
}

/* 暗色主题下的搜索框 */
[data-theme="dark"] .search-input {
  background: #1a1a1a;
  border-color: #262626;
  color: #ffffff;
}

[data-theme="dark"] .search-input:focus {
  border-color: #818cf8;
  box-shadow: 0 0 0 2px rgba(129, 140, 248, 0.2);
}

[data-theme="dark"] .search-input::placeholder {
  color: #737373;
}

.search-input::placeholder {
  color: var(--color-text-placeholder);
}

.search-shortcut {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 2px 6px;
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-family: monospace;
}

[data-theme="dark"] .search-shortcut {
  background: #2a2a2a;
  border-color: #262626;
  color: #a3a3a3;
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-base);
}

.nav-item:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: var(--color-bg-active);
  color: var(--color-primary);
  font-weight: 600;
}

[data-theme="dark"] .nav-item {
  color: #a3a3a3;
}

[data-theme="dark"] .nav-item:hover {
  background: #1a1a1a;
  color: #ffffff;
}

[data-theme="dark"] .nav-item.active {
  background: #2a2a2a;
  color: #818cf8;
}

.nav-icon {
  font-size: 18px;
  color: var(--color-text-muted);
  flex-shrink: 0;
  transition: color var(--transition-base);
}

.nav-item:hover .nav-icon {
  color: var(--color-primary);
}

.nav-item.active .nav-icon {
  color: var(--color-primary);
}

[data-theme="dark"] .nav-icon {
  color: #737373;
}

[data-theme="dark"] .nav-item:hover .nav-icon,
[data-theme="dark"] .nav-item.active .nav-icon {
  color: #818cf8;
}

.nav-text {
  flex: 1;
}

.nav-badge {
  margin-left: auto;
  padding: 2px 8px;
  background: var(--color-bg-hover);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--radius-full);
}

.nav-divider {
  padding: 20px 12px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

[data-theme="dark"] .nav-divider {
  color: #737373;
}

/* 滚动条美化 */
.sidebar-nav::-webkit-scrollbar {
  width: 6px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: var(--color-border-light);
  border-radius: 3px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: var(--color-border);
}

/* 底部信息 */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 主题切换按钮 */
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 16px;
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-base);
}

.theme-toggle:hover {
  background: var(--color-bg-active);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

[data-theme="dark"] .theme-toggle {
  background: #1a1a1a;
  border-color: #262626;
  color: #d4d4d4;
}

[data-theme="dark"] .theme-toggle:hover {
  background: #2a2a2a;
  border-color: #818cf8;
  color: #818cf8;
}

.theme-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.theme-text {
  flex: 1;
}

.version-info {
  text-align: center;
}

.version-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

[data-theme="dark"] .version-label {
  color: #737373;
}

/* === 主内容区 === */
.main-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
  background: var(--color-bg-page);
  display: flex;
  flex-direction: column;
}

/* 登录页面包装器 */
.login-page-wrapper {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* === 顶部用户菜单 === */
.top-header {
  background: var(--color-bg-sidebar);
  border-bottom: 1px solid var(--color-border);
  padding: 0;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 用户菜单触发器 */
.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
}

.user-menu-trigger:hover {
  background: var(--color-bg-hover);
}

.user-avatar-top {
  background: var(--color-primary);
  color: white;
}

.username-top {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.dropdown-icon {
  font-size: 14px;
  color: var(--color-text-muted);
}

/* 页面内容区域 */
.page-content {
  flex: 1;
  overflow: auto;
  padding: 24px;
}

.page-content.with-header {
  padding: 0;
}

/* 用户头像圆圈 */
.user-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 下拉菜单用户信息 */
.dropdown-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.dropdown-user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.dropdown-user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dropdown-user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dropdown-user-status {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 页面过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
