import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

import { clearAccessToken, getAccessToken, setAccessToken } from '@/utils/auth'
import type { RequestConfig } from '@/utils/request-data'
import { RequestError, type ApiResponse } from '@/utils/response-data'
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
    throw new RequestError(payload.message || 'Request failed', response.status)
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

  const message = error.response?.data?.message || defaultErrorMessage(status)
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
  if (status === 400) return 'Request parameters are invalid'
  if (status === 401) return 'Your session has expired, please sign in again'
  if (status === 403) return 'You do not have permission to perform this action'
  if (status === 404) return 'Requested resource was not found'
  if (status === 429) return 'Too many requests, please try again later'
  if (status >= 500) return 'The server is temporarily unavailable'
  return 'Network request failed'
}

const request = {
  get: <T>(url: string, config?: RequestConfig) => send<T>({ ...config, method: 'get', url }),
  post: <T>(url: string, data?: unknown, config?: RequestConfig) => send<T>({ ...config, method: 'post', url, data }),
  put: <T>(url: string, data?: unknown, config?: RequestConfig) => send<T>({ ...config, method: 'put', url, data }),
  delete: <T>(url: string, config?: RequestConfig) => send<T>({ ...config, method: 'delete', url }),
}

export default request
