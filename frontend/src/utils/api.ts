import request from '@/utils/request'
import type { MenuCode } from '@/utils/authorization'

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

export interface UserBalance {
  balance: number
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
}

export const userAccountApi = {
  balance: () => request.get<UserBalance>('/user/balance'),
}
