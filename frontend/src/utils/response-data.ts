export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export class RequestError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'RequestError'
    this.status = status
  }
}
