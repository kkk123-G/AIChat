import type { AxiosRequestConfig } from 'axios'

export interface RequestConfig extends AxiosRequestConfig {
  skipAuthRefresh?: boolean
  silent?: boolean
}

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthRefresh?: boolean
    silent?: boolean
    _retry?: boolean
  }

  export interface InternalAxiosRequestConfig {
    skipAuthRefresh?: boolean
    silent?: boolean
    _retry?: boolean
  }
}
