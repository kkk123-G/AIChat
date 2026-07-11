const ACCESS_TOKEN_KEY = 'ai-chat-access-token'

export function hasAccessToken() {
  return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY))
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}
