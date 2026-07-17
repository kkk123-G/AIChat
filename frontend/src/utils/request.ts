import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

import { clearAccessToken, getAccessToken, setAccessToken } from '@/utils/auth'
import type { RequestConfig } from '@/utils/request-data'
import { localizeErrorMessage, RequestError, type ApiResponse } from '@/utils/response-data'
import type {} from '@/utils/request-data'

const AUTH_REFRESH_PATH = '/auth/refresh'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15_000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

let refreshPromise: Promise<string> | null = null

client.interceptors.request.use((config) => {
  const accessToken = getAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

client.interceptors.response.use(undefined, async (error: AxiosError<ApiResponse<unknown>>) => handleResponseError(error))

function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>): T {
  const payload = response.data
  if (!payload.success) {
    throw new RequestError(localizeErrorMessage(payload.message) || '请求失败', response.status)
  }
  return payload.data
}

async function handleResponseError(error: AxiosError<ApiResponse<unknown>>): Promise<AxiosResponse<ApiResponse<unknown>>> {
  const config = error.config
  const status = error.response?.status ?? 0

  if (config && status === 401 && shouldRefresh(config)) {
    try {
      const accessToken = await refreshAccessToken()
      config._retry = true
      config.headers.Authorization = `Bearer ${accessToken}`
      return client(config)
    } catch {
      handleSessionExpired()
    }
  }

  const payload = error.response?.data
  const serverMessage = typeof payload === 'string' ? payload : payload?.message
  const message = localizeErrorMessage(serverMessage) || defaultErrorMessage(status)
  if (!config?.silent) {
    ElMessage.error(message)
  }
  throw new RequestError(message, status)
}

function shouldRefresh(config: RequestConfig): boolean {
  return !config._retry && !config.skipAuthRefresh
}

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = send<{ accessToken: string }>({
      method: 'post',
      url: AUTH_REFRESH_PATH,
      data: undefined,
      skipAuthRefresh: true,
      silent: true,
    })
      .then((data) => {
        setAccessToken(data.accessToken)
        return data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

async function send<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResponse<T>>(config)
  return unwrapResponse(response)
}

function handleSessionExpired() {
  clearAccessToken()
  if (window.location.pathname !== '/login') {
    window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
  }
}

function defaultErrorMessage(status: number): string {
  if (status === 400) return '请求参数不正确'
  if (status === 401) return '登录状态已过期，请重新登录'
  if (status === 402) return '余额不足'
  if (status === 403) return '无权执行此操作'
  if (status === 404) return '请求资源不存在'
  if (status === 429) return '请求过于频繁，请稍后再试'
  if (status >= 500) return '服务器暂时不可用，请稍后再试'
  return '网络请求失败，请检查网络连接'
}

const request = {
  get: <T>(url: string, config?: RequestConfig) => send<T>({ ...config, method: 'get', url }),
  post: <T>(url: string, data?: unknown, config?: RequestConfig) => send<T>({ ...config, method: 'post', url, data }),
  put: <T>(url: string, data?: unknown, config?: RequestConfig) => send<T>({ ...config, method: 'put', url, data }),
  delete: <T>(url: string, config?: RequestConfig) => send<T>({ ...config, method: 'delete', url }),
}

export default request
