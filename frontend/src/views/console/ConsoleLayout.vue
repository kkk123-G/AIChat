<template>
  <div class="app-wrapper" :class="{ 'is-mobile': isMobile }">
    <aside v-if="!isMobile" class="sidebar-container" :class="{ 'is-collapse': isCollapse }">
      <div class="logo-wrapper">
        <img class="logo-image" :src="logoUrl" alt="探索AI" />
        <span class="logo-text">探索AI</span>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="activeMenu"
          :collapse="menuCollapsed"
          :collapse-transition="false"
          class="el-menu-vertical"
          @select="handleMenuSelect"
        >
          <el-menu-item
            v-for="item in accessibleMainMenuItems"
            :key="item.index"
            :index="item.index"
            :disabled="item.disabled"
          >
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <template #title
              ><span class="menu-text">{{ item.title }}</span></template
            >
          </el-menu-item>

          <div v-if="isAdmin && accessibleAccountMenuItems.length" class="menu-group-title">
            <span>我的账户</span>
          </div>

          <el-menu-item
            v-for="item in accessibleAccountMenuItems"
            :key="item.index"
            :index="item.index"
            :disabled="item.disabled"
          >
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <template #title
              ><span class="menu-text">{{ item.title }}</span></template
            >
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-footer" :class="{ 'is-footer-collapse': isCollapse }">
        <div class="footer-item dark-mode-toggle" @click="toggleDarkMode">
          <el-icon>
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
          <span class="footer-text" v-show="!isCollapse">深色模式</span>
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

    <el-drawer
      v-else
      v-model="drawerVisible"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="logo-wrapper">
        <img class="logo-image" :src="logoUrl" alt="探索AI" />
        <span class="logo-text">探索AI</span>
      </div>
      <el-scrollbar class="menu-scrollbar">
        <el-menu :default-active="activeMenu" class="el-menu-vertical" @select="handleMenuSelect">
          <el-menu-item
            v-for="item in accessibleMainMenuItems"
            :key="item.index"
            :index="item.index"
            :disabled="item.disabled"
            @click="drawerVisible = false"
          >
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span class="menu-text">{{ item.title }}</span>
          </el-menu-item>

          <div v-if="isAdmin && accessibleAccountMenuItems.length" class="menu-group-title">
            <span>我的账户</span>
          </div>

          <el-menu-item
            v-for="item in accessibleAccountMenuItems"
            :key="item.index"
            :index="item.index"
            :disabled="item.disabled"
            @click="drawerVisible = false"
          >
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
          <span class="footer-text">深色模式</span>
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
          <div class="balance-box">
            <el-icon class="money-icon">
              <Money />
            </el-icon>
            <span class="balance-amount">￥{{ balanceText }}</span>
          </div>
          <el-dropdown trigger="click">
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
                <el-dropdown-item @click="goToProfile">个人资料</el-dropdown-item>
                <el-dropdown-item divided :disabled="logoutLoading" @click="handleLogout">
                  {{ logoutLoading ? '正在退出...' : '退出登录' }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-main">
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
  Document,
  ChatDotRound,
  Money,
  Expand,
  ArrowDown,
  Moon,
  Sunny,
  DArrowLeft,
  DArrowRight,
} from '@element-plus/icons-vue'
import logoUrl from '@/assets/logo.png'
import { authApi, type CurrentUser } from '@/utils/api'
import { MENU_CODES, type MenuCode } from '@/utils/authorization'
import { clearAccessToken } from '@/utils/auth'
import { useAccountStore } from '@/stores/account'

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
const isDark = ref(false)
const currentUser = ref<CurrentUser | null>(null)
const accountStore = useAccountStore()
const { balance } = storeToRefs(accountStore)
const logoutLoading = ref(false)
const screenWidth = ref(window.innerWidth)

const mainMenuItems: MenuItem[] = [
  { code: MENU_CODES.adminDashboard, index: 'admin-dashboard', title: '仪表盘', icon: Odometer },
  { code: MENU_CODES.userManagement, index: 'user-management', title: '用户管理', icon: User },
  { code: MENU_CODES.usageRecords, index: 'usage-records', title: '使用记录', icon: Document },
  { code: MENU_CODES.rechargeRecords, index: 'recharge-records', title: '充值记录', icon: Document },
]

const accountMenuItems: MenuItem[] = [
  { code: MENU_CODES.userDashboard, index: 'user-dashboard', title: '仪表盘', icon: Odometer },
  { code: MENU_CODES.aiChat, index: 'ai-chat', title: 'ai聊天', icon: ChatDotRound },
  { code: MENU_CODES.balanceChanges, index: 'balance-changes', title: '金额变动', icon: Document, disabled: true },
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
  isDark.value = !isDark.value
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
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (collapseTimer) window.clearTimeout(collapseTimer)
})
</script>

<style scoped lang="scss">
$sidebar-bg: #ffffff;
$navbar-bg: #ffffff;
$main-bg: #f8fafc;
$border-color: #f1f5f9;
$active-green-bg: #f0fdfa;
$active-green-text: #0d9488;

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
    color: #000;
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

// 优化后：“我的账户” 分组标题样式
.menu-group-title {
  font-size: 12px; // 稍微调小一点 (原 11px，这里相较于新菜单的 15px 算明显小)
  color: #64748b; // 明确的灰色
  font-weight: 600; // 同样进行一定的加粗
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
  margin-top: 14px; // 核心修改：拉大最上方“仪表盘”绿色激活区与顶部Logo细横线的间距
  transition:
    width 0.24s ease,
    padding 0.24s ease;

  :deep(.el-menu-item) {
    height: 46px; // 稍微调高一点菜单项高度以匹配大字体
    line-height: 46px;
    margin: 6px 0;
    border-radius: 8px;
    color: #334155;
    transition: all 0.2s ease;

    // 菜单字体优化
    .menu-text {
      display: inline-block;
      font-size: 15px; // 字体稍微调大 (原 14px)
      font-weight: 600; // 字体进行一定的加粗
      opacity: 1;
      transform: translateX(0);
      transition:
        opacity 0.16s ease,
        transform 0.16s ease;
    }

    // 图标同步变大
    .el-icon {
      color: #475569;
      font-shrink: 0;
      font-size: 19px; // 图标跟随字体一起变大 (原 17px)
      margin-right: 4px;
    }

    &:hover {
      background-color: #f8fafc;
      color: #0f172a;
    }

    &.is-active {
      background-color: $active-green-bg !important;
      color: $active-green-text !important;

      .menu-text {
        font-weight: 700; // 选中状态下加深字重
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
    color: #475569;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.2s;
    font-size: 14px;
    font-weight: 500;

    &:hover {
      background-color: #f8fafc;
      color: #0f172a;
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
      color: #64748b;
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

    .balance-box {
      display: flex;
      align-items: center;
      background-color: #f0fdf4;
      border: 1px solid #bbf7d0;
      padding: 6px 12px;
      border-radius: 6px;
      color: #16a34a;
      font-weight: 600;
      font-size: 14px;

      .money-icon {
        margin-right: 6px;
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
          color: #94a3b8;
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
  .navbar {
    padding: 0 16px;

    .navbar-right {
      gap: 12px;
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
</style>
