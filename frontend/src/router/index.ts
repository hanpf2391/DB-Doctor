import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
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
    }
  ]
})

export default router
