import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { authApi, type CurrentUser } from '@/utils/api'
import { defaultConsoleRouteName, MENU_CODES, type MenuCode } from '@/utils/authorization'
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
        path: 'ai-chat',
        name: 'ai-chat',
        component: () => import('@/views/chat/AiChatView.vue'),
        meta: { menuCode: MENU_CODES.aiChat, title: 'ai聊天' },
      },
      {
        path: 'balance-changes',
        name: 'balance-changes',
        component: () => import('@/views/user/BalanceChangesView.vue'),
        meta: { menuCode: MENU_CODES.balanceChanges, title: '金额变动' },
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
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { menuCode: MENU_CODES.adminDashboard, title: '仪表盘' },
      },
      {
        path: 'admin/users',
        name: 'user-management',
        component: () => import('@/views/admin/UserManagementView.vue'),
        meta: { menuCode: MENU_CODES.userManagement, title: '用户管理' },
      },
      {
        path: 'admin/recharge-refund-records',
        name: 'recharge-refund-records',
        component: () => import('@/views/admin/RechargeRefundRecordsView.vue'),
        meta: { menuCode: MENU_CODES.rechargeRefundRecords, title: '充值退款' },
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
    return currentUser ? defaultConsoleRoute(currentUser) : { name: 'login' }
  }

  const menuCode = to.meta.menuCode as MenuCode | undefined
  if (!menuCode) return

  const currentUser = await loadCurrentUser()
  if (!currentUser) return { name: 'login', query: { redirect: to.fullPath } }
  if (!currentUser.menuCodes.includes(menuCode)) {
    return defaultConsoleRoute(currentUser)
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

function defaultConsoleRoute(currentUser: CurrentUser) {
  return { name: defaultConsoleRouteName(currentUser.menuCodes) }
}

router.afterEach((to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} | AI Chat` : 'AI Chat'
})

export default router
