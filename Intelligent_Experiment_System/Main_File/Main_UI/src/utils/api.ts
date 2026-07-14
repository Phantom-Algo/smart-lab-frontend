import axios, { type AxiosInstance, type AxiosResponse } from 'axios'

// 后端统一响应格式
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  metadata?: Record<string, unknown>
}

export interface PageResult<T = unknown> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：自动附加 Token
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    // 后端 code=0 表示成功
    if (res.code !== 0) {
      // 401 未登录 → 清除登录态，触发登出事件（避免页面刷新循环）
      if (res.code === 40100) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('userId')
        localStorage.removeItem('userRole')
        // 使用自定义事件通知应用登出，避免 window.location.replace 导致的刷新循环
        window.dispatchEvent(new CustomEvent('auth:logout'))
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('userId')
      localStorage.removeItem('userRole')
      // 使用自定义事件通知应用登出，避免 window.location.reload 导致的刷新循环
      window.dispatchEvent(new CustomEvent('auth:logout'))
    }
    return Promise.reject(error)
  }
)

export default instance
