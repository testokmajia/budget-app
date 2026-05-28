import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'checklist',
        name: 'Checklist',
        component: () => import('@/views/checklist/ChecklistView.vue'),
        meta: { title: '清单管理' },
      },
      {
        path: 'reward',
        name: 'Reward',
        component: () => import('@/views/reward/RewardView.vue'),
        meta: { title: '奖惩记录' },
      },
      {
        path: 'issue',
        name: 'Issue',
        component: () => import('@/views/issue/IssueView.vue'),
        meta: { title: '科技问题管理' },
      },
      {
        path: 'pending',
        name: 'Pending',
        component: () => import('@/views/pending/PendingView.vue'),
        meta: { title: '系统问题实施' },
      },
      {
        path: 'weekly',
        name: 'Weekly',
        component: () => import('@/views/weekly/WeeklyView.vue'),
        meta: { title: '工作周报' },
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/AdminView.vue'),
        meta: { title: '系统管理', roles: ['ROLE_ADMIN'] },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public || userStore.token) {
    next()
  } else {
    next('/login')
  }
})

export default router
