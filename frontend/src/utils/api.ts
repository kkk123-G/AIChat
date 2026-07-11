import request from '@/utils/request'

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
}

export const authApi = {
  register: (payload: RegisterPayload) => request.post<void>('/auth/register', payload, { skipAuthRefresh: true }),
  login: (payload: LoginPayload) => request.post<TokenData>('/auth/login', payload, { skipAuthRefresh: true }),
  refresh: () => request.post<TokenData>('/auth/refresh', undefined, { skipAuthRefresh: true, silent: true }),
  logout: () => request.post<void>('/auth/logout', undefined, { skipAuthRefresh: true, silent: true }),
  currentUser: () => request.get<CurrentUser>('/auth/me'),
}
