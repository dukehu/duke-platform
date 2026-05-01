import axios, { AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

let _token: string | null = null

export function setToken(token: string) {
  _token = token
}

export function getToken(): string | null {
  return _token
}

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  if (_token) {
    config.headers.Authorization = `Bearer ${_token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data as any
    if (res.code !== 200) {
      const errorMsg = res.message || '请求失败'
      ElMessage.error(errorMsg)
      return Promise.reject(new Error(errorMsg))
    }
    return res.data
  },
  error => {
    // 处理响应体的错误（HTTP 4xx/5xx 但有 data body）
    if (error.response?.data) {
      const res = error.response.data as any
      const errorMsg = res.message || error.response.statusText || error.message || '网络错误'
      ElMessage.error(errorMsg)
    } else if (error.response?.status === 401) {
      window.parent.postMessage({ type: 'AUTH_EXPIRED' }, '*')
    } else {
      const errorMsg = error.message || '网络错误'
      ElMessage.error(errorMsg)
    }
    return Promise.reject(error)
  }
) as any

export default request
