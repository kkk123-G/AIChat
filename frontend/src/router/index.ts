import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { authApi, type CurrentUser } from '@/utils/api'
import { defaultDashboardRouteName, MENU_CODES, type MenuCode } from '@/utils/authorization'
import { clearAccessToken, hasAccessToken } from '@/utils/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => (hasAccessToken() ? '/console' : '/login'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresGuest: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresGuest: true, title: '注册' },
  },
  {
    path: '/console',
    name: 'console',
    component: () => import('@/views/console/ConsoleLayout.vue'),
    meta: { requiresAuth: true, title: '管理控制台' },
    children: [
      {
        path: '',
        redirect: { name: 'admin-dashboard' },
      },
      {
        path: 'dashboard',
        name: 'user-dashboard',
        component: () => import('@/views/chat/pages/UserDashboardView.vue'),
        meta: { menuCode: MENU_CODES.userDashboard, title: '仪表盘' },
      },
      {
        path: 'ai-chat',
        name: 'ai-chat',
        component: () => import('@/views/chat/pages/AiChatView.vue'),
        meta: { menuCode: MENU_CODES.aiChat, title: 'ai聊天' },
      },
      {
        path: 'profile',
        name: 'user-profile',
        component: () => import('@/views/user/UserProfileView.vue'),
        meta: { menuCode: MENU_CODES.userProfile, title: '个人资料' },
      },
      {
        path: 'admin/dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/chat/pages/AdminDashboardView.vue'),
        meta: { menuCode: MENU_CODES.adminDashboard, title: '仪表盘' },
      },
      {
        path: 'admin/users',
        name: 'user-management',
        component: () => import('@/views/chat/pages/UserManagementView.vue'),
        meta: { menuCode: MENU_CODES.userManagement, title: '用户管理' },
      },
      {
        path: 'admin/usage-records',
        name: 'usage-records',
        component: () => import('@/views/chat/pages/UsageRecordsView.vue'),
        meta: { menuCode: MENU_CODES.usageRecords, title: '使用记录' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  const isAuthenticated = hasAccessToken()

  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresGuest && isAuthenticated) {
    const currentUser = await loadCurrentUser()
    return currentUser ? defaultDashboardRoute(currentUser) : { name: 'login' }
  }

  const menuCode = to.meta.menuCode as MenuCode | undefined
  if (!menuCode) return

  const currentUser = await loadCurrentUser()
  if (!currentUser) return { name: 'login', query: { redirect: to.fullPath } }
  if (!currentUser.menuCodes.includes(menuCode)) {
    return defaultDashboardRoute(currentUser)
  }
})

async function loadCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await authApi.currentUser()
  } catch {
    clearAccessToken()
    return null
  }
}

function defaultDashboardRoute(currentUser: CurrentUser) {
  return { name: defaultDashboardRouteName(currentUser.menuCodes) }
}

router.afterEach((to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} | AI Chat` : 'AI Chat'
})

export default router
