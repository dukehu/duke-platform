import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  return config
})

let isRedirectingToLogin = false

request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code !== 200) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
    return data
  },
  error => {
    if (error.response?.status === 401) {
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        removeToken()
        window.parent.postMessage({ type: 'AUTH_EXPIRED' }, 'http://localhost:3000')
        setTimeout(() => {
          isRedirectingToLogin = false
        }, 3000)
      }
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
