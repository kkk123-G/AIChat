<template>
  <div class="app-wrapper" :class="{ 'is-mobile': isMobile }">
    <aside v-if="!isMobile" class="sidebar-container" :class="{ 'is-collapse': isCollapse }">
      <div class="logo-wrapper">
        <img class="logo-image" :src="logoUrl" alt="探索AI" />
        <span class="logo-text">探索AI</span>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <el-menu :default-active="activeMenu" :collapse="menuCollapsed" :collapse-transition="false"
          class="el-menu-vertical" @select="handleMenuSelect">
          <el-menu-item v-for="item in accessibleMainMenuItems" :key="item.index" :index="item.index"
            :disabled="item.disabled">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <template #title><span class="menu-text">{{ item.title }}</span></template>
          </el-menu-item>

          <div v-if="isAdmin && accessibleAccountMenuItems.length" class="menu-group-title">
            <span>我的账户</span>
          </div>

          <el-menu-item v-for="item in accessibleAccountMenuItems" :key="item.index" :index="item.index"
            :disabled="item.disabled">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <template #title><span class="menu-text">{{ item.title }}</span></template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-footer" :class="{ 'is-footer-collapse': isCollapse }">
        <div class="footer-item dark-mode-toggle" @click="toggleDarkMode">
          <el-icon>
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
          <span class="footer-text" v-show="!isCollapse">{{ isDark ? '浅色模式' : '深色模式' }}</span>
        </div>
        <div class="footer-item collapse-toggle" @click="toggleSidebar">
          <el-icon>
            <DArrowLeft v-if="!isCollapse" />
            <DArrowRight v-else />
          </el-icon>
          <span class="footer-text" v-show="!isCollapse">收起侧边栏</span>
        </div>
      </div>
    </aside>

    <el-drawer v-else v-model="drawerVisible" direction="ltr" size="260px" :with-header="false" class="mobile-drawer">
      <div class="logo-wrapper">
        <img class="logo-image" :src="logoUrl" alt="探索AI" />
        <span class="logo-text">探索AI</span>
      </div>
      <el-scrollbar class="menu-scrollbar">
        <el-menu :default-active="activeMenu" class="el-menu-vertical" @select="handleMenuSelect">
          <el-menu-item v-for="item in accessibleMainMenuItems" :key="item.index" :index="item.index"
            :disabled="item.disabled" @click="drawerVisible = false">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span class="menu-text">{{ item.title }}</span>
          </el-menu-item>

          <div v-if="isAdmin && accessibleAccountMenuItems.length" class="menu-group-title">
            <span>我的账户</span>
          </div>

          <el-menu-item v-for="item in accessibleAccountMenuItems" :key="item.index" :index="item.index"
            :disabled="item.disabled" @click="drawerVisible = false">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span class="menu-text">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-footer">
        <div class="footer-item dark-mode-toggle" @click="toggleDarkMode">
          <el-icon>
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
          <span class="footer-text">{{ isDark ? '浅色模式' : '深色模式' }}</span>
        </div>
      </div>
    </el-drawer>

    <div class="main-container">
      <header class="navbar">
        <div class="navbar-left">
          <div v-if="isMobile" class="collapse-btn" @click="drawerVisible = !drawerVisible">
            <el-icon size="20">
              <Expand />
            </el-icon>
          </div>
          <span class="breadcrumb-title">{{ currentMenuTitle }}</span>
        </div>

        <div class="navbar-right">
          <el-button class="check-in-button" :class="{ 'is-completed': checkedInToday }" :loading="checkInLoading"
            :disabled="checkedInToday || checkInLoading" @click="handleCheckIn">
            {{ checkedInToday ? '今日已签到' : '签到' }}
          </el-button>
          <div class="balance-box">
            <el-icon class="money-icon">
              <Money />
            </el-icon>
            <span class="balance-amount">￥{{ balanceText }}</span>
          </div>

          <el-dropdown trigger="click" class="custom-user-dropdown" popper-class="custom-user-dropdown-popper">
            <div class="user-avatar-wrapper">
              <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
              <div v-if="currentUser && !isMobile" class="user-info">
                <span class="username">{{ currentUser.username }}</span>
                <span class="role">{{ isAdmin ? 'Admin' : 'User' }}</span>
              </div>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">
                  <el-icon>
                    <User />
                  </el-icon>
                  <span>个人资料</span>
                </el-dropdown-item>

                <el-dropdown-item @click="openGitHub">
                  <el-icon>
                    <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
                      <path fill="currentColor"
                        d="M511.6 76.3C264.3 76.2 64 276.4 64 523.5 64 718.9 189.3 885 363.8 946c23.5 5.9 19.9-10.8 19.9-22.2v-77.5c-135.7 15.9-141.2-73.9-150.3-88.9C215 726 171.5 718 184.5 703c30.9-15.9 62.4 4 98.9 57.9 26.4 39.1 77.9 32.5 104 26 5.7-23.5 17.9-44.5 34.7-60.8-140.6-25.2-199.2-111-199.2-213 0-49.5 16.3-95 48.3-131.7-20.4-60.5 1.9-112.3 4.9-120 58.1-5.2 118.5 41.6 123.2 45.3 33-8.9 70.7-13.6 112.9-13.6 42.4 0 80.2 4.9 113.5 13.9 11.3-8.6 67.3-48.8 121.3-43.9 2.9 7.7 24.7 58.3 5.5 118 32.4 36.8 48.9 82.7 48.9 132.3 0 102.2-59 188.1-200 212.9 23.5 23.2 38.1 55.4 38.1 91v112.5c0.8 9 0 27.9 22.7 22.7C826.9 883.1 954 717.7 954 523.9 954 277.3 754.6 76.3 511.6 76.3z">
                      </path>
                    </svg>
                  </el-icon>
                  <span>GitHub</span>
                </el-dropdown-item>

                <el-dropdown-item divided class="service-item" @click="keepServiceMenuOpen">
                  <el-icon>
                    <Service />
                  </el-icon>
                  <span>联系客服: 2938374296</span>
                </el-dropdown-item>

                <el-dropdown-item divided :disabled="logoutLoading" @click="handleLogout" class="logout-item">
                  <el-icon>
                    <SwitchButton />
                  </el-icon>
                  <span>{{ logoutLoading ? '正在退出...' : '退出登录' }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-main" :class="{ 'is-chat-page': activeMenu === 'ai-chat' }">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, type Component } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  User,
  Avatar,
  CreditCard,
  TrendCharts,
  ChatDotRound,
  Money,
  Expand,
  ArrowDown,
  Moon,
  Sunny,
  DArrowLeft,
  DArrowRight,
  Service,
  SwitchButton
} from '@element-plus/icons-vue'
import logoUrl from '@/assets/logo.png'
import { authApi, userAccountApi, type CurrentUser } from '@/utils/api'
import { MENU_CODES, type MenuCode } from '@/utils/authorization'
import { clearAccessToken } from '@/utils/auth'
import { useAccountStore } from '@/stores/account'
import { useThemeStore } from '@/stores/theme'

interface MenuItem {
  code: MenuCode
  index: string
  title: string
  icon: Component
  disabled?: boolean
}

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const menuCollapsed = ref(false)
const drawerVisible = ref(false)
const currentUser = ref<CurrentUser | null>(null)
const accountStore = useAccountStore()
const themeStore = useThemeStore()
const { balance } = storeToRefs(accountStore)
const { isDark } = storeToRefs(themeStore)
const logoutLoading = ref(false)
const checkedInToday = ref(false)
const checkInLoading = ref(false)
const screenWidth = ref(window.innerWidth)

const mainMenuItems: MenuItem[] = [
  { code: MENU_CODES.adminDashboard, index: 'admin-dashboard', title: '仪表盘', icon: Odometer },
  { code: MENU_CODES.userManagement, index: 'user-management', title: '用户管理', icon: User },
  { code: MENU_CODES.rechargeRefundRecords, index: 'recharge-refund-records', title: '充值退款', icon: CreditCard },
]

const accountMenuItems: MenuItem[] = [
  { code: MENU_CODES.aiChat, index: 'ai-chat', title: 'ai聊天', icon: ChatDotRound },
  { code: MENU_CODES.balanceChanges, index: 'balance-changes', title: '金额变动', icon: TrendCharts },
  { code: MENU_CODES.userProfile, index: 'user-profile', title: '个人资料', icon: Avatar },
]

const activeMenu = computed(() => String(route.name ?? ''))
const isMobile = computed(() => screenWidth.value < 768)
const isAdmin = computed(() => currentUser.value?.role === 1)
const accessibleMainMenuItems = computed(() => filterAccessibleMenus(mainMenuItems))
const accessibleAccountMenuItems = computed(() => filterAccessibleMenus(accountMenuItems))
const avatarText = computed(() => currentUser.value?.username.slice(0, 2).toUpperCase() ?? '')
const balanceText = computed(() => (balance.value === null ? '0.00' : Number(balance.value).toFixed(2)))
const currentMenuTitle = computed(() => {
  const currentMenu = [...accessibleMainMenuItems.value, ...accessibleAccountMenuItems.value].find(
    (item) => item.index === activeMenu.value,
  )

  return currentMenu?.title ?? String(route.meta.title ?? '管理控制台')
})

let collapseTimer: ReturnType<typeof window.setTimeout> | undefined

const toggleDarkMode = () => {
  themeStore.toggleTheme()
}

const openGitHub = () => {
  window.open('https://github.com/kkk123-G/AIChat', '_blank')
}

const keepServiceMenuOpen = (event: MouseEvent) => {
  event.preventDefault()
}

const handleMenuSelect = (index: string) => {
  void router.push({ name: index })
}

const goToProfile = () => {
  void router.push({ name: 'user-profile' })
}

const loadCurrentUser = async () => {
  try {
    currentUser.value = await authApi.currentUser()
  } catch {
    currentUser.value = null
  }
}

const loadBalance = async () => {
  await accountStore.refreshBalance()
}

const loadCheckInStatus = async () => {
  try {
    checkedInToday.value = (await userAccountApi.checkInStatus()).checkedIn
  } catch {
    checkedInToday.value = false
  }
}

const handleCheckIn = async () => {
  if (checkedInToday.value || checkInLoading.value) return

  checkInLoading.value = true
  try {
    await userAccountApi.checkIn()
    checkedInToday.value = true
    await accountStore.refreshBalance()
    ElMessage.success('签到成功')
  } catch {
    await loadCheckInStatus()
  } finally {
    checkInLoading.value = false
  }
}

function filterAccessibleMenus(menuItems: MenuItem[]) {
  const menuCodes = currentUser.value?.menuCodes ?? []
  return menuItems.filter((item) => menuCodes.includes(item.code))
}

const handleLogout = async () => {
  if (logoutLoading.value) return

  try {
    await ElMessageBox.confirm('退出后需要重新登录才能继续使用。', '确认退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  logoutLoading.value = true
  let logoutFailed = false
  try {
    await authApi.logout()
  } catch {
    logoutFailed = true
  } finally {
    clearAccessToken()
    currentUser.value = null
    accountStore.clearBalance()
    logoutLoading.value = false
  }

  await router.replace({ name: 'login' })
  if (logoutFailed) {
    ElMessage.warning('服务器连接异常，已清除本地登录状态')
    return
  }
  ElMessage.success('已退出登录')
}

const toggleSidebar = () => {
  if (collapseTimer) {
    window.clearTimeout(collapseTimer)
    collapseTimer = undefined
  }

  if (isCollapse.value) {
    menuCollapsed.value = false
    window.requestAnimationFrame(() => {
      isCollapse.value = false
    })
    return
  }

  isCollapse.value = true
  collapseTimer = window.setTimeout(() => {
    menuCollapsed.value = true
    collapseTimer = undefined
  }, 160)
}

const handleResize = () => {
  screenWidth.value = window.innerWidth
  if (!isMobile.value) drawerVisible.value = false
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  void loadCurrentUser()
  void loadBalance()
  void loadCheckInStatus()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (collapseTimer) window.clearTimeout(collapseTimer)
})
</script>

<style scoped lang="scss">
$sidebar-bg: var(--app-surface);
$navbar-bg: var(--app-surface);
$main-bg: var(--app-bg);
$border-color: var(--app-border-muted);
$active-green-bg: var(--app-primary-soft);
$active-green-text: var(--app-primary);

$sidebar-width: 240px;
$sidebar-collapse-width: 64px;
$navbar-height: 60px;

:global(html),
:global(body),
:global(#app) {
  width: 100%;
  height: 100%;
  margin: 0;
  overflow: hidden;
}

.app-wrapper {
  display: flex;
  width: 100%;
  height: 100dvh;
  overflow: hidden;
  background-color: $main-bg;
  box-sizing: border-box;
}

.sidebar-container {
  width: $sidebar-width;
  height: 100%;
  background-color: $sidebar-bg;
  border-right: 1px solid $border-color;
  transition: width 0.24s ease;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;

  &.is-collapse {
    width: $sidebar-collapse-width;

    .logo-wrapper {
      padding-left: 18px;

      .logo-text {
        max-width: 0;
        margin-left: 0;
        opacity: 0;
      }
    }

    .menu-group-title {
      &::after {
        position: absolute;
        top: 50%;
        left: 50%;
        width: 20px;
        border-top: 1px solid #a8b4c4;
        content: '';
        transform: translate(-50%, -50%);
      }

      span {
        opacity: 0;
      }
    }

    :deep(.el-menu-vertical.el-menu--collapse) {
      width: 100%;
      padding: 0;
    }

    :deep(.el-menu-vertical.el-menu--collapse .el-menu-item) {
      padding: 0 !important;
      width: 100%;
      display: flex;
      justify-content: center;
    }

    :deep(.el-menu-vertical.el-menu--collapse .el-menu-item .el-icon) {
      margin-right: 0 !important;
    }

    :deep(.el-menu-vertical.el-menu--collapse .el-menu-item.is-active) {
      position: relative;
      background-color: transparent !important;
      color: $active-green-text !important;
    }

    :deep(.el-menu-vertical.el-menu--collapse .el-menu-item.is-active)::before {
      position: absolute;
      top: 50%;
      left: 50%;
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background-color: $active-green-bg;
      content: '';
      pointer-events: none;
      transform: translate(-50%, -50%);
    }

    :deep(.el-menu-vertical.el-menu--collapse .el-menu-item.is-active .el-icon) {
      position: relative;
      z-index: 1;
      color: $active-green-text !important;
    }
  }
}

.logo-wrapper {
  height: $navbar-height;
  padding-left: 20px;
  display: flex;
  align-items: center;
  overflow: hidden;
  border-bottom: 1px solid $border-color;
  flex-shrink: 0;
  transition: padding-left 0.24s ease;

  .logo-image {
    width: 28px;
    height: 28px;
    flex: 0 0 28px;
    object-fit: contain;
  }

  .logo-text {
    max-width: 100px;
    margin-left: 10px;
    overflow: hidden;
    font-size: 18px;
    font-weight: bold;
    color: var(--app-text);
    white-space: nowrap;
    transition:
      max-width 0.16s ease,
      margin-left 0.16s ease,
      opacity 0.12s ease;
  }
}

.menu-scrollbar {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
}

.menu-group-title {
  font-size: 12px;
  color: var(--app-text-muted);
  font-weight: 600;
  height: 46px;
  padding: 0 20px 10px;
  display: flex;
  align-items: flex-end;
  box-sizing: border-box;
  position: relative;
  text-align: left;
  user-select: none;

  span {
    transition: opacity 0.12s ease;
  }
}

.el-menu-vertical {
  border-right: none;
  padding: 0;
  margin-top: 14px;
  transition:
    width 0.24s ease,
    padding 0.24s ease;

  :deep(.el-menu-item) {
    height: 46px;
    line-height: 46px;
    margin: 6px 0;
    border-radius: 8px;
    color: var(--app-text-regular);
    transition: all 0.2s ease;

    .menu-text {
      display: inline-block;
      font-size: 15px;
      font-weight: 600;
      opacity: 1;
      transform: translateX(0);
      transition:
        opacity 0.16s ease,
        transform 0.16s ease;
    }

    .el-icon {
      color: var(--app-text-regular);
      font-shrink: 0;
      font-size: 19px;
      margin-right: 4px;
    }

    &:hover {
      background-color: var(--app-surface-muted);
      color: var(--app-text);
    }

    &.is-active {
      background-color: $active-green-bg !important;
      color: $active-green-text !important;

      .menu-text {
        font-weight: 700;
      }

      .el-icon {
        color: $active-green-text !important;
      }
    }
  }
}

.sidebar-container.is-collapse {
  :deep(.el-menu-vertical:not(.el-menu--collapse) .menu-text) {
    opacity: 0;
    transform: translateX(-8px);
  }
}

.sidebar-footer {
  border-top: 1px solid $border-color;
  padding: 8px 12px;
  background-color: $sidebar-bg;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;

  .footer-item {
    height: 40px;
    display: flex;
    align-items: center;
    padding: 0 12px;
    color: var(--app-text-regular);
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.2s;
    font-size: 14px;
    font-weight: 500;

    &:hover {
      background-color: var(--app-surface-muted);
      color: var(--app-text);
    }

    .el-icon {
      font-size: 18px;
    }

    .footer-text {
      margin-left: 12px;
      white-space: nowrap;
    }
  }

  &.is-footer-collapse {
    padding: 8px 0;

    .footer-item {
      padding: 0;
      justify-content: center;
    }
  }
}

.main-container {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.navbar {
  height: $navbar-height;
  background-color: $navbar-bg;
  border-bottom: 1px solid $border-color;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;

  .navbar-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      cursor: pointer;
      margin-right: 16px;
      color: var(--app-text-muted);
      outline: none;
      user-select: none;
      -webkit-tap-highlight-color: transparent;

      &:focus,
      &:focus-visible {
        outline: none;
      }
    }

    .breadcrumb-title {
      font-size: 15px;
      font-weight: 500;
    }
  }

  .navbar-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .check-in-button {
      min-width: 64px;
      height: 32px;
      padding: 0 10px;
      border: 1px solid #bbf7d0;
      border-radius: 6px;
      background-color: #f0fdf4;
      color: #16a34a;
      font-weight: 600;

      &:hover:not(:disabled),
      &:focus-visible:not(:disabled) {
        border-color: #86efac;
        background-color: #dcfce7;
        color: #16a34a;
      }

      &.is-completed,
      &:disabled {
        border-color: #e5e7eb;
        background-color: #f3f4f6;
        color: #9ca3af;
      }
    }

    .balance-box {
      display: flex;
      align-items: center;
      background-color: #f0fdf4;
      border: 1px solid #bbf7d0;
      padding: 6px 10px;
      border-radius: 6px;
      color: #16a34a;
      font-weight: 600;
      font-size: 14px;

      .money-icon {
        margin-right: 4px;
      }
    }

    .user-avatar-wrapper {
      display: flex;
      align-items: center;
      cursor: pointer;
      gap: 8px;

      .user-avatar {
        background-color: #e0f2f1;
        color: #00695c;
        font-weight: 600;
      }

      .user-info {
        display: flex;
        flex-direction: column;
        text-align: left;

        .username {
          font-size: 13px;
          font-weight: 500;
          line-height: 1.2;
        }

        .role {
          font-size: 11px;
          color: var(--app-text-muted);
        }
      }
    }
  }
}

.app-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
  padding: 24px;
  overflow-y: auto;
  box-sizing: border-box;

  > :deep(*) {
    min-width: 0;
  }

  > :deep(.user-manage-container) {
    flex: 1;
    min-height: 0;
  }
}

.is-mobile {
  .app-main.is-chat-page {
    padding: 0;
  }

  .navbar {
    padding: 0 12px;

    .navbar-left,
    .navbar-right {
      height: 32px;
    }

    .navbar-left {
      .collapse-btn,
      .breadcrumb-title {
        display: flex;
        align-items: center;
        height: 100%;
        line-height: 1;
      }

      .collapse-btn {
        margin-right: 8px;
      }

      .breadcrumb-title {
        flex-shrink: 0;
        white-space: nowrap;
      }
    }

    .navbar-right {
      gap: 8px;

      .check-in-button,
      .balance-box,
      .user-avatar-wrapper {
        box-sizing: border-box;
        height: 100%;
      }

      .check-in-button,
      .balance-box,
      .user-avatar-wrapper,
      .custom-user-dropdown {
        display: flex;
        align-items: center;
      }

      .check-in-button {
        min-width: 52px;
        padding: 0 6px;
        justify-content: center;
        line-height: 1;
      }

      .balance-box {
        padding: 6px 5px;

        .money-icon {
          margin-right: 2px;
        }
      }
    }
  }
}

:deep(.mobile-drawer) {
  .el-drawer__body {
    padding: 0;
    display: flex;
    flex-direction: column;

    .el-menu-vertical {
      padding: 0 12px;
    }
  }
}

:global(.custom-user-dropdown-popper) {
  translate: -25px 0 !important;
}

:global(.custom-user-dropdown),
:global(.custom-user-dropdown .user-avatar-wrapper),
:global(.custom-user-dropdown .el-tooltip__trigger),
:global(.custom-user-dropdown .el-tooltip__trigger:focus),
:global(.custom-user-dropdown .el-tooltip__trigger:focus-visible),
:global(.custom-user-dropdown .user-avatar-wrapper:focus),
:global(.custom-user-dropdown .user-avatar-wrapper:focus-visible),
:global(.custom-user-dropdown:focus),
:global(.custom-user-dropdown:focus-visible) {
  outline: none !important;
  box-shadow: none !important;
  background: transparent !important;
  -webkit-tap-highlight-color: transparent;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  color: #334155;
  font-size: 14px;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item .el-icon) {
  margin-right: 12px;
  color: #475569;
  font-size: 16px;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item:not(.service-item):not(.logout-item):not(.is-disabled):hover),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item:not(.service-item):not(.logout-item):not(.is-disabled):focus),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item:not(.service-item):not(.logout-item):not(.is-disabled).is-focus) {
  background-color: #f3f4f6 !important;
  color: #334155 !important;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item:not(.service-item):not(.logout-item):not(.is-disabled):hover .el-icon),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item:not(.service-item):not(.logout-item):not(.is-disabled):focus .el-icon),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item:not(.service-item):not(.logout-item):not(.is-disabled).is-focus .el-icon) {
  color: #475569;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item:hover),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item:focus),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item.is-focus),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item.is-disabled) {
  cursor: text;
  pointer-events: auto;
  user-select: text;
  -webkit-user-select: text;
  background-color: transparent !important;
  color: #64748b !important;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item .el-icon),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.service-item:hover .el-icon) {
  color: #94a3b8;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item .el-icon) {
  color: #dc2626 !important;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item:hover),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item:focus),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item.is-focus) {
  background-color: #fef2f2 !important;
  color: #dc2626 !important;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item:hover .el-icon),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item:focus .el-icon),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item.is-focus .el-icon) {
  color: #dc2626 !important;
}

:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item.is-disabled),
:global(.custom-user-dropdown-popper .el-dropdown-menu__item.logout-item.is-disabled .el-icon) {
  color: #fca5a5 !important;
}
</style>
