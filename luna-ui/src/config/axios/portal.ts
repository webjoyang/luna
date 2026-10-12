import axios, { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import qs from 'qs'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/app-api',
  timeout: 30000,
  withCredentials: false,
  paramsSerializer: (params) => {
    return qs.stringify(params, { allowDots: true })
  }
})

// request 拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 规范化 url: 若以 /app-api 开头，则截断避免与 baseURL 重复拼接
    if (config.url?.startsWith('/app-api')) {
      config.url = config.url.substring('/app-api'.length)
    }

    const token = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('portal_token') : null
    if (token) {
      config.headers['X-Portal-Token'] = token
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 业务错误码拦截（401、1030038000、1030038001 统一进入 invalid 引导）
    if (res?.code === 401 || res?.code === 1030038000 || res?.code === 1030038001) {
      router.push('/portal/invalid')
      return Promise.reject(new Error(res?.msg || '链接已失效，请联系业务员重新获取'))
    }
    if (res?.code === 429 || res?.code === 1030038002) {
      ElMessage.error(res?.msg || '请求过于频繁，请稍后再试')
      return Promise.reject(new Error(res?.msg || '请求过于频繁，请稍后再试'))
    }
    if (res?.code !== undefined && res?.code !== 0) {
      ElMessage.error(res?.msg || '系统异常')
      return Promise.reject(new Error(res?.msg || '系统异常'))
    }
    return res
  },
  (error: AxiosError<any>) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    if (status === 401 || code === 1030038000 || status === 403 || code === 1030038001) {
      router.push('/portal/invalid')
      return Promise.reject(error)
    }
    if (status === 429 || code === 1030038002) {
      ElMessage.error(error.response?.data?.msg || '请求过于频繁，请稍后再试')
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.msg || error.message || '网络请求失败')
    return Promise.reject(error)
  }
)

const request = (option: any) => {
  const { headersType, headers, ...otherOption } = option
  return service({
    ...otherOption,
    headers: {
      'Content-Type': headersType || 'application/json',
      ...headers
    }
  })
}

const portalRequest = {
  get: async <T = any>(option: any): Promise<T> => {
    const res = await request({ method: 'GET', ...option })
    return (res?.data !== undefined ? res.data : res) as T
  },
  post: async <T = any>(option: any): Promise<T> => {
    const res = await request({ method: 'POST', ...option })
    return (res?.data !== undefined ? res.data : res) as T
  },
  put: async <T = any>(option: any): Promise<T> => {
    const res = await request({ method: 'PUT', ...option })
    return (res?.data !== undefined ? res.data : res) as T
  },
  delete: async <T = any>(option: any): Promise<T> => {
    const res = await request({ method: 'DELETE', ...option })
    return (res?.data !== undefined ? res.data : res) as T
  }
}

export default portalRequest
export { service, request }
