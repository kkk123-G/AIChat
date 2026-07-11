import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { hasAccessToken } from '@/utils/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => (hasAccessToken() ? '/chat' : '/login'),
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
    path: '/chat',
    name: 'chat',
    component: () => import('@/views/chat/ChatView.vue'),
    meta: { requiresAuth: true, title: 'AI 聊天' },
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

router.beforeEach((to) => {
  const isAuthenticated = hasAccessToken()

  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresGuest && isAuthenticated) {
    return { name: 'chat' }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${String(to.meta.title)} | AI Chat` : 'AI Chat'
})

export default router
