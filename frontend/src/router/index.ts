import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard/Dashboard.vue')
    },
    {
      path: '/reports',
      name: 'Home',
      component: () => import('@/views/Home.vue')
    },
    {
      path: '/settings',
      name: 'Settings',
      redirect: '/settings/target-db',
      component: () => import('@/views/Settings/index.vue'),
      children: [
        {
          path: 'target-db',
          name: 'TargetDb',
          component: () => import('@/views/Settings/TargetDb.vue')
        },
        {
          path: 'ai',
          name: 'AiConfig',
          component: () => import('@/views/Settings/AiConfig.vue')
        },
        {
          path: 'monitor-notify',
          name: 'MonitorNotify',
          component: () => import('@/views/Settings/MonitorNotify.vue')
        },
        {
          path: 'maintenance',
          name: 'Maintenance',
          component: () => import('@/views/Settings/Maintenance.vue')
        }
      ]
    },
    {
      path: '/ai-monitor',
      name: 'AiMonitor',
      redirect: '/ai-monitor/index',
      component: () => import('@/views/AiMonitor/index.vue')
    },
    {
      path: '/ai-monitor/index',
      name: 'AiMonitorIndex',
      component: () => import('@/views/AiMonitor/index.vue')
    },
    {
      path: '/ai-monitor/invocation-log',
      name: 'AiMonitorInvocationLog',
      component: () => import('@/views/AiMonitor/InvocationLog.vue')
    }
  ]
})

export default router
