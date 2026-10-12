import { beforeEach, describe, expect, it, vi } from 'vitest'
import { service } from '../portal'
import router from '@/router'
import { ElMessage } from 'element-plus'

class MockStorage {
  private store: Record<string, string> = {}
  getItem(key: string) {
    return this.store[key] || null
  }
  setItem(key: string, value: string) {
    this.store[key] = String(value)
  }
  removeItem(key: string) {
    delete this.store[key]
  }
  clear() {
    this.store = {}
  }
}

const mockSessionStorage = new MockStorage()
vi.stubGlobal('sessionStorage', mockSessionStorage)

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
    replace: vi.fn()
  }
}))

describe('Portal Axios 独立实例与拦截器测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStorage.clear()
  })

  describe('请求拦截器 (Request Interceptor)', () => {
    it('当 sessionStorage 中存在 portal_token 时，自动在请求头添加 X-Portal-Token', async () => {
      mockSessionStorage.setItem('portal_token', 'token-abc-123')
      const requestInterceptor = (service.interceptors.request as any).handlers[0]

      const config = {
        url: '/lunaerp/portal/auth/info',
        headers: {}
      }

      const result = await requestInterceptor.fulfilled(config)
      expect(result.headers['X-Portal-Token']).toBe('token-abc-123')
      // 不引入 Authorization
      expect(result.headers['Authorization']).toBeUndefined()
    })

    it('当 sessionStorage 中无 portal_token 时，不添加 X-Portal-Token 头', async () => {
      const requestInterceptor = (service.interceptors.request as any).handlers[0]

      const config = {
        url: '/lunaerp/portal/auth/info',
        headers: {}
      }

      const result = await requestInterceptor.fulfilled(config)
      expect(result.headers['X-Portal-Token']).toBeUndefined()
      expect(result.headers['Authorization']).toBeUndefined()
    })

    it('若 url 以 /app-api 开头，自动截断避免与 baseURL 重复拼接', async () => {
      const requestInterceptor = (service.interceptors.request as any).handlers[0]

      const config = {
        url: '/app-api/lunaerp/portal/auth/info',
        headers: {}
      }

      const result = await requestInterceptor.fulfilled(config)
      expect(result.url).toBe('/lunaerp/portal/auth/info')
    })
  })

  describe('响应拦截器 (Response Interceptor)', () => {
    const responseInterceptor = (service.interceptors.response as any).handlers[0]

    it('正常响应 (code = 0) 正常放行并返回数据', async () => {
      const response = {
        data: {
          code: 0,
          data: { customerId: 100, customerName: '测试客户' },
          msg: ''
        }
      }

      const result = await responseInterceptor.fulfilled(response)
      expect(result).toEqual(response.data)
    })

    it('业务错误码 401 或 1030038000 (PORTAL_TOKEN_INVALID) → 跳转 /portal/invalid 并 reject', async () => {
      const response = {
        data: {
          code: 1030038000,
          msg: '门户令牌无效或已过期',
          data: null
        }
      }

      await expect(responseInterceptor.fulfilled(response)).rejects.toThrow()
      expect(router.push).toHaveBeenCalledWith('/portal/invalid')
    })

    it('HTTP 401 状态码 → 跳转 /portal/invalid 并 reject', async () => {
      const error = {
        response: {
          status: 401,
          data: { code: 1030038000, msg: '门户令牌无效或已过期' }
        }
      }

      await expect(responseInterceptor.rejected(error)).rejects.toEqual(error)
      expect(router.push).toHaveBeenCalledWith('/portal/invalid')
    })

    it('HTTP 403 / 业务码 1030038001 (PORTAL_DATA_ACCESS_DENIED) → 跳转 /portal/invalid 并 reject', async () => {
      const response = {
        data: {
          code: 1030038001,
          msg: '门户数据归属不符',
          data: null
        }
      }

      await expect(responseInterceptor.fulfilled(response)).rejects.toThrow()
      expect(router.push).toHaveBeenCalledWith('/portal/invalid')
    })

    it('业务错误码 429 或 1030038002 (PORTAL_RATE_LIMIT_EXCEEDED) → Toast 提示并 reject', async () => {
      const response = {
        data: {
          code: 1030038002,
          msg: '门户请求过于频繁，请稍后再试',
          data: null
        }
      }

      await expect(responseInterceptor.fulfilled(response)).rejects.toThrow('请求过于频繁，请稍后再试')
      expect(ElMessage.error).toHaveBeenCalledWith('门户请求过于频繁，请稍后再试')
      expect(router.push).not.toHaveBeenCalled()
    })

    it('HTTP 429 状态码 → Toast 提示并 reject', async () => {
      const error = {
        response: {
          status: 429,
          data: { code: 1030038002, msg: '请求过于频繁，请稍后再试' }
        }
      }

      await expect(responseInterceptor.rejected(error)).rejects.toEqual(error)
      expect(ElMessage.error).toHaveBeenCalledWith('请求过于频繁，请稍后再试')
      expect(router.push).not.toHaveBeenCalled()
    })
  })
})
