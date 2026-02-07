import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 创建 Axios 实例
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('dbdoctor_token')

    // 如果有 token，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data

    // 支持两种成功码：数字 200 和字符串 'SUCCESS'
    if (code === 200 || code === 'SUCCESS') {
      return response.data
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    // 处理 401 未授权错误
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')

      // 清除本地存储的认证信息
      localStorage.removeItem('dbdoctor_token')
      localStorage.removeItem('dbdoctor_user')
      localStorage.removeItem('dbdoctor_login_time')

      // 跳转到登录页
      window.location.href = '/login'

      return Promise.reject(new Error('未授权'))
    }

    // 其他错误
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export { request }
export default request
