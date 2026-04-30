import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRedirectingToLogin = false

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      const errorMsg = res.message || '请求失败'
      ElMessage.error(errorMsg)
      const error = new Error(errorMsg)
      ;(error as any).response = res
      return Promise.reject(error)
    }
    return res
  },
  error => {
    let errorMsg = '网络错误'

    if (error.response?.status === 401) {
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        removeToken()
        router.push('/login').finally(() => {
          isRedirectingToLogin = false
        })
      }
    } else {
      errorMsg = error.response?.data?.message || error.message || '网络错误'
      ElMessage.error(errorMsg)
    }

    const err = new Error(errorMsg)
    ;(err as any).response = error.response?.data
    return Promise.reject(err)
  }
)

export default request
