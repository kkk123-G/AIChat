export const MENU_CODES = {
  adminDashboard: 'admin-dashboard',
  userManagement: 'user-management',
  usageRecords: 'usage-records',
  userDashboard: 'user-dashboard',
  aiChat: 'ai-chat',
  userProfile: 'user-profile',
} as const

export type MenuCode = (typeof MENU_CODES)[keyof typeof MENU_CODES]

export function defaultDashboardRouteName(menuCodes: readonly MenuCode[]) {
  return menuCodes.includes(MENU_CODES.adminDashboard) ? 'admin-dashboard' : 'user-dashboard'
}
