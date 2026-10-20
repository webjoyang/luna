import axios from 'axios'
import type { AxiosInstance, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getPortalToken, clearPortalToken } from '@/views/portal/utils/portalAuth'

// baseURL 同 /app-api
const appApiBaseUrl = (import.meta.env.VITE_BASE_URL || '') + '/app-api'

const portalAxios: AxiosInstance = axios.create({
  baseURL: appApiBaseUrl,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：仅带 X-Portal-Token，不引 user store、不带 Authorization
portalAxios.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getPortalToken()
  if (token) {
    config.headers['X-Portal-Token'] = token
  }
  return config
})

// 统一跳转到失效页
const redirectToInvalid = () => {
  clearPortalToken()
  if (typeof window !== 'undefined') {
    try {
      if (router?.push) {
        router.push('/portal/invalid')
      } else {
        window.location.href = '/portal/invalid'
      }
    } catch {
      window.location.href = '/portal/invalid'
    }
  }
}

// 响应拦截器
portalAxios.interceptors.response.use(
  (response: AxiosResponse<any>) => {
    const data = response.data
    if (!data) {
      throw new Error('未获取到响应数据')
    }
    const code = data.code
    // 成功状态（业务状态码 0）
    if (code === 0) {
      return data.data
    }
    // 401 或 1030038000: 令牌无效或已过期
    if (code === 401 || code === 1030038000) {
      ElMessage.error('链接已失效，请联系业务员重新获取')
      redirectToInvalid()
      return Promise.reject(new Error(data.msg || '链接已失效，请联系业务员重新获取'))
    }
    // 429 或 1030038002: 请求过于频繁
    if (code === 429 || code === 1030038002) {
      ElMessage.warning('请求过于频繁，请稍后再试')
      return Promise.reject(new Error(data.msg || '请求过于频繁，请稍后再试'))
    }
    ElMessage.error(data.msg || '系统异常')
    return Promise.reject(new Error(data.msg || '系统异常'))
  },
  (error: AxiosError<any>) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    if (status === 401 || code === 401 || code === 1030038000) {
      ElMessage.error('链接已失效，请联系业务员重新获取')
      redirectToInvalid()
      return Promise.reject(error)
    }
    if (status === 429 || code === 429 || code === 1030038002) {
      ElMessage.warning('请求过于频繁，请稍后再试')
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export const portalRequest = {
  get: <T = any>(option: any) => portalAxios.request<any, T>({ method: 'get', ...option }),
  post: <T = any>(option: any) => portalAxios.request<any, T>({ method: 'post', ...option }),
  put: <T = any>(option: any) => portalAxios.request<any, T>({ method: 'put', ...option }),
  delete: <T = any>(option: any) => portalAxios.request<any, T>({ method: 'delete', ...option }),
  request: <T = any>(option: any) => portalAxios.request<any, T>(option)
}

export default portalRequest
