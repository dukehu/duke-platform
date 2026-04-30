const TOKEN_KEY = 'token'

// 内存存储防止 XSS 通过 localStorage 读取 token，页面刷新时从 sessionStorage 恢复
let memoryToken: string | null = sessionStorage.getItem(TOKEN_KEY)

export function getToken(): string | null {
  return memoryToken
}

export function setToken(token: string): void {
  memoryToken = token
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  memoryToken = null
  sessionStorage.removeItem(TOKEN_KEY)
}
