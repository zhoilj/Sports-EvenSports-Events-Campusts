import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { Result } from '@/types/auth'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data

    // 业务错误
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      // 401 未登录
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return response.data as any
  },
  (error) => {
    console.error('响应错误:', error)

    // 网络错误
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }

    return Promise.reject(error)
  }
)

// 封装请求方法
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.get(url, { params, ...config })
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.put(url, data, config)
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
  return service.delete(url, config)
}

export default service