import axios, { type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

/** 后端统一响应体：{ code, message, data } */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown> | undefined
    // 业务错误（HTTP 200 + code != 0）：统一提示后中断
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    const onAuthPage = ['/login', '/register', '/forgot-password', '/sso-callback'].some((path) =>
      window.location.pathname.startsWith(path),
    )

    if (status === 401) {
      if (!onAuthPage) {
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_user')
        ElMessage.warning('登录已过期，请重新登录')
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      } else {
        ElMessage.error(message || '账号或凭证错误')
      }
    } else if (status === 403) {
      ElMessage.error(message || '无权限访问')
    } else {
      ElMessage.error(message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default http
