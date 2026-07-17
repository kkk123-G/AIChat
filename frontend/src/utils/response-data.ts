export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export function localizeErrorMessage(message: string | null | undefined): string | null {
  if (!message) return null

  if (message === 'Invalid CORS request') return '跨域请求被拒绝，请检查访问域名配置'
  if (message === 'Request failed') return '请求失败'
  if (message === 'Unauthorized') return '请先登录'
  if (message === 'Forbidden') return '无权执行此操作'
  if (message === 'Not Found') return '请求资源不存在'

  return message
}

export class RequestError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'RequestError'
    this.status = status
  }
}
