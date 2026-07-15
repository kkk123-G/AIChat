export const MENU_CODES = {
  adminDashboard: 'admin-dashboard',
  userManagement: 'user-management',
  rechargeRecords: 'recharge-records',
  aiChat: 'ai-chat',
  balanceChanges: 'balance-changes',
  userProfile: 'user-profile',
} as const

export type MenuCode = (typeof MENU_CODES)[keyof typeof MENU_CODES]

export function defaultConsoleRouteName(menuCodes: readonly MenuCode[]) {
  return menuCodes.includes(MENU_CODES.adminDashboard) ? 'admin-dashboard' : 'ai-chat'
}
