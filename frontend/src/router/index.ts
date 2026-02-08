import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 登录页面
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', requiresAuth: false }
    },

    // 仪表盘
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard/Dashboard.vue'),
      meta: { title: '仪表盘', icon: 'Odometer', requiresAuth: false }
    },

    // 慢查询报表
    {
      path: '/reports',
      name: 'Reports',
      component: () => import('@/views/Home.vue'),
      meta: { title: '慢查询报表', icon: 'DataAnalysis', requiresAuth: false }
    },

    // AI 监控
    {
      path: '/ai-monitor',
      name: 'AiMonitor',
      redirect: '/ai-monitor/index',
      component: () => import('@/views/AiMonitor/index.vue'),
      meta: { title: 'AI 监控', icon: 'Monitor', requiresAuth: false },
      children: [
        {
          path: 'index',
          name: 'AiMonitorIndex',
          component: () => import('@/views/AiMonitor/index.vue'),
          meta: { title: '监控总览' }
        },
        {
          path: 'invocation-log',
          name: 'AiMonitorInvocationLog',
          component: () => import('@/views/AiMonitor/InvocationLog.vue'),
          meta: { title: '调用日志' }
        },
        {
          path: 'analysis-traces',
          name: 'AnalysisTraces',
          component: () => import('@/views/AiMonitor/AnalysisTraceList.vue'),
          meta: { title: '链路追踪' }
        },
        {
          path: 'cost-analysis',
          name: 'CostAnalysis',
          component: () => import('@/views/AiMonitor/CostAnalysis.vue'),
          meta: { title: '成本分析' }
        }
      ]
    },

    // 实例管理
    {
      path: '/instances',
      name: 'Instances',
      component: () => import('@/views/Settings/InstanceManagement.vue'),
      meta: { title: '实例管理', icon: 'Connection', requiresAuth: true }
    },

    // 告警与通知
    {
      path: '/alert-notify',
      name: 'AlertNotify',
      redirect: '/alert-notify/history',
      component: () => import('@/views/AlertNotify/index.vue'),
      meta: { title: '告警与通知', icon: 'Bell', requiresAuth: false },
      children: [
        {
          path: 'history',
          name: 'AlertHistory',
          component: () => import('@/views/AlertNotify/History.vue'),
          meta: { title: '告警历史' }
        },
        {
          path: 'notification',
          name: 'NotificationSettings',
          component: () => import('@/views/AlertNotify/NotificationSettings.vue'),
          meta: { title: '通知设置' }
        }
      ]
    },

    // 监控中心
    {
      path: '/monitoring',
      name: 'Monitoring',
      redirect: '/monitoring/system-health',
      component: () => import('@/views/Monitoring/index.vue'),
      meta: { title: '监控中心', icon: 'TrendCharts', requiresAuth: false },
      children: [
        {
          path: 'system-health',
          name: 'SystemHealth',
          component: () => import('@/views/Monitoring/SystemHealth.vue'),
          meta: { title: '系统健康' }
        },
        {
          path: 'notification-config',
          name: 'NotificationConfig',
          component: () => import('@/views/Monitoring/NotificationConfig.vue'),
          meta: { title: '通知配置' }
        }
      ]
    },

    // 设置中心
    {
      path: '/settings',
      name: 'Settings',
      component: () => import('@/views/Settings/index.vue'),
      meta: { title: '设置中心', icon: 'Setting', requiresAuth: true }
    }
  ]
})

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 恢复登录状态（首次访问时）
  if (!authStore.isAuthenticated && !authStore.username) {
    authStore.restoreSession()
  }

  // 需要认证的页面
  if (to.meta.requiresAuth !== false) {
    if (authStore.isAuthenticated) {
      // 已登录，放行
      next()
    } else {
      // 未登录，跳转到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath } // 保存原始路径，登录后跳转
      })
    }
  } else {
    // 不需要认证的页面（如登录页）
    if (to.path === '/login' && authStore.isAuthenticated) {
      // 已登录用户访问登录页，跳转到首页
      next('/')
    } else {
      next()
    }
  }
})

export default router
