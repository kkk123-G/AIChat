import request from '@/utils/request'
import type { MenuCode } from '@/utils/authorization'
import { clearAccessToken, getAccessToken, setAccessToken } from '@/utils/auth'
import { localizeErrorMessage, RequestError, type ApiResponse } from '@/utils/response-data'

export interface LoginPayload {
  username: string
  password: string
  deviceId?: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
}

export interface TokenData {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface CurrentUser {
  id: number
  username: string
  role: number
  menuCodes: MenuCode[]
}

export interface AdminUser {
  id: string
  username: string
  role: 'ADMIN' | 'USER'
  balance: number
  status: 'ENABLED' | 'DISABLED'
  lastActiveAt: string | null
  lastUsedAt: string | null
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface AdminUserListParams {
  page: number
  size: 10 | 20 | 50
  keyword?: string
}

export interface AdminRechargePayload {
  amount: string
  remark?: string
}

export interface AdminRechargeResult {
  userId: number
  balance: number
}

export interface AdminRefundPayload {
  amount: string
  remark?: string
}

export interface AdminBalanceAdjustmentResult {
  userId: number
  balance: number
}

export interface AdminUserStatusPayload {
  enabled: boolean
}

export interface DashboardTrend {
  labels: string[]
  values: number[]
}

export interface DashboardTopUser {
  username: string
  values: number[]
}

export interface AdminDashboard {
  userCount: number
  newUsersToday: number
  todayRequests: number
  totalRequests: number
  todaySuccesses: number
  totalSuccesses: number
  todayFailures: number
  totalFailures: number
  todaySuccessRate: number
  totalSuccessRate: number
  todayFailureRate: number
  totalFailureRate: number
  requestTrend: DashboardTrend
  topUsers: DashboardTopUser[]
}

export interface RechargeRefundRecord {
  id: string
  operationNo: string
  username: string
  amount: number
  operationType: 'RECHARGE' | 'REFUND'
  balanceBefore: number
  balanceAfter: number
  remark: string | null
  operatorUsername: string
  createdAt: string
}

export interface RechargeRefundRecordListParams {
  page: number
  size: 10 | 20 | 50
}

export interface UserBalance {
  balance: number
}

export interface UserProfile {
  username: string
  role: 'ADMIN' | 'USER'
  status: 'ENABLED' | 'DISABLED'
  totalCalls: number
  balance: number
  createdAt: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export interface DailyCheckInStatus {
  checkedIn: boolean
}

export interface BalanceChangeRecord {
  id: string
  amount: number
  createdAt: string
  type: string
  remark: string | null
}

export interface BalanceChangeListParams {
  page: number
  size: 10 | 20 | 50
}

export interface Conversation {
  id: string
  title: string
  lastMessageAt: string | null
  createdAt: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface ChatStreamEvent {
  type: 'delta' | 'done' | 'error'
  delta: string | null
  message: ChatMessage | null
  error: string | null
}

export const authApi = {
  register: (payload: RegisterPayload) => request.post<void>('/auth/register', payload, { skipAuthRefresh: true }),
  login: (payload: LoginPayload) => request.post<TokenData>('/auth/login', payload, { skipAuthRefresh: true }),
  refresh: () => request.post<TokenData>('/auth/refresh', undefined, { skipAuthRefresh: true, silent: true }),
  logout: () => request.post<void>('/auth/logout', undefined, { skipAuthRefresh: true, silent: true }),
  currentUser: () => request.get<CurrentUser>('/auth/me'),
}

export const adminUserApi = {
  list: (params: AdminUserListParams) => request.get<PageResult<AdminUser>>('/admin/users', { params }),
  recharge: (userId: string, payload: AdminRechargePayload) =>
    request.post<AdminRechargeResult>(`/admin/users/${userId}/recharges`, payload),
  refund: (userId: string, payload: AdminRefundPayload) =>
    request.post<AdminBalanceAdjustmentResult>(`/admin/users/${userId}/refunds`, payload),
  updateStatus: (userId: string, payload: AdminUserStatusPayload) =>
    request.put<void>(`/admin/users/${userId}/status`, payload),
}

export const adminDashboardApi = {
  overview: () => request.get<AdminDashboard>('/admin/dashboard'),
}

export const adminRechargeRefundRecordApi = {
  list: (params: RechargeRefundRecordListParams) =>
    request.get<PageResult<RechargeRefundRecord>>('/admin/recharge-refund-records', { params }),
}

export const userAccountApi = {
  balance: () => request.get<UserBalance>('/user/balance'),
  profile: () => request.get<UserProfile>('/user/profile'),
  changePassword: (payload: ChangePasswordPayload) => request.put<void>('/user/password', payload),
  checkInStatus: () => request.get<DailyCheckInStatus>('/user/check-in/status'),
  checkIn: () => request.post<void>('/user/check-in'),
  balanceChanges: (params: BalanceChangeListParams) =>
    request.get<PageResult<BalanceChangeRecord>>('/user/balance-changes', { params }),
}

export const chatApi = {
  createConversation: () => request.post<Conversation>('/chat/conversations'),
  listConversations: () => request.get<Conversation[]>('/chat/conversations'),
  searchConversations: (keyword: string) =>
    request.get<Conversation[]>('/chat/conversations/search', { params: { keyword } }),
  listMessages: (conversationId: string) => request.get<ChatMessage[]>(`/chat/conversations/${conversationId}/messages`),
  deleteConversation: (conversationId: string) => request.delete<void>(`/chat/conversations/${conversationId}`),
  streamMessage: (
    conversationId: string,
    content: string,
    signal: AbortSignal,
    onEvent: (event: ChatStreamEvent) => void,
  ) => streamChatMessage(conversationId, content, signal, onEvent),
}

async function streamChatMessage(
  conversationId: string,
  content: string,
  signal: AbortSignal,
  onEvent: (event: ChatStreamEvent) => void,
) {
  let response = await sendStreamRequest(conversationId, content, signal)
  if (response.status === 401 && await refreshAccessTokenForStream()) {
    response = await sendStreamRequest(conversationId, content, signal)
  }
  if (response.status === 401) {
    handleStreamSessionExpired()
    throw new RequestError('登录状态已过期，请重新登录', response.status)
  }
  if (!response.ok) {
    throw await toStreamRequestError(response)
  }
  if (!response.body) {
    throw new RequestError('服务端未返回响应流', response.status)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value, { stream: !done })
    buffer = consumeSseEvents(buffer, onEvent)
    if (done) {
      buffer += decoder.decode()
      consumeSseEvents(buffer, onEvent)
      return
    }
  }
}

function sendStreamRequest(conversationId: string, content: string, signal: AbortSignal) {
  const headers: HeadersInit = { 'Content-Type': 'application/json' }
  const accessToken = getAccessToken()
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`
  return fetch(`${apiBaseUrl()}/chat/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ content }),
    credentials: 'include',
    signal,
  })
}

async function refreshAccessTokenForStream() {
  const response = await fetch(`${apiBaseUrl()}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
  })
  if (!response.ok) return false
  const payload = await response.json() as ApiResponse<TokenData>
  if (!payload.success || !payload.data?.accessToken) return false
  setAccessToken(payload.data.accessToken)
  return true
}

async function toStreamRequestError(response: Response) {
  try {
    const payload = await response.json() as ApiResponse<unknown>
    return new RequestError(localizeErrorMessage(payload.message) || '无法发起 AI 回复', response.status)
  } catch {
    return new RequestError('无法发起 AI 回复', response.status)
  }
}

function consumeSseEvents(buffer: string, onEvent: (event: ChatStreamEvent) => void) {
  const events = buffer.split(/\r?\n\r?\n/)
  const remaining = events.pop() || ''
  for (const eventBlock of events) {
    const data = eventBlock
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n')
    if (!data) continue
    let event: ChatStreamEvent
    try {
      event = JSON.parse(data) as ChatStreamEvent
    } catch {
      // Ignore malformed partial events; the next SSE frame remains independent.
      continue
    }
    onEvent(event)
  }
  return remaining
}

function apiBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL || '/api'
}

function handleStreamSessionExpired() {
  clearAccessToken()
  if (window.location.pathname !== '/login') {
    window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
  }
}
