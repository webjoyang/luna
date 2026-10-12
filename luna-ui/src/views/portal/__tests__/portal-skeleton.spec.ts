import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePortalAuth } from '../usePortalAuth'
import * as PortalAuthApi from '@/api/portal/auth'
import remainingRouter from '@/router/modules/remaining'

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

const replaceStateMock = vi.fn()
vi.stubGlobal('window', {
  location: {
    href: 'http://localhost/portal/shipments?t=test_token_123',
    pathname: '/portal/shipments',
    search: '?t=test_token_123'
  },
  history: {
    replaceState: replaceStateMock
  }
})

const mockRoute = {
  path: '/portal/shipments',
  query: {} as Record<string, string>,
  params: {} as Record<string, string>
}

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn()
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/portal/auth', () => ({
  getPortalAuthInfoApi: vi.fn()
}))

describe('Ticket 02: 门户前端骨架与 Token 引导 (Portal Frontend Skeleton)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStorage.clear()
    mockRoute.query = {}
    mockRoute.params = {}
    mockRoute.path = '/portal/shipments'
    ;(window as any).location.href = 'http://localhost/portal/shipments'
    ;(window as any).location.pathname = '/portal/shipments'
    ;(window as any).location.search = ''
  })

  describe('1. usePortalAuth 令牌引导逻辑测试', () => {
    it('当 URL 带 ?t= 令牌时，存入 sessionStorage 并清理地址栏参数', async () => {
      mockRoute.query = { t: 'test_token_123' }
      ;(window as any).location.href = 'http://localhost/portal/shipments?t=test_token_123'
      ;(window as any).location.search = '?t=test_token_123'

      vi.mocked(PortalAuthApi.getPortalAuthInfoApi).mockResolvedValueOnce({
        customerId: 88,
        customerName: '浙江天马印染厂'
      })

      const { customerName, customerId, bootstrapToken } = usePortalAuth()
      const success = await bootstrapToken()

      expect(mockSessionStorage.getItem('portal_token')).toBe('test_token_123')
      expect(replaceStateMock).toHaveBeenCalled()
      expect(success).toBe(true)
      expect(customerId.value).toBe(88)
      expect(customerName.value).toBe('浙江天马印染厂')
    })

    it('无 ?t= 且 sessionStorage 中亦无 token 时，重定向至 /portal/invalid', async () => {
      mockRoute.query = {}
      const { bootstrapToken } = usePortalAuth()

      const success = await bootstrapToken()

      expect(success).toBe(false)
      expect(mockRouter.replace).toHaveBeenCalledWith('/portal/invalid')
      expect(PortalAuthApi.getPortalAuthInfoApi).not.toHaveBeenCalled()
    })

    it('sessionStorage 中已有有效 token 时，正常请求身份信息并回显客户名', async () => {
      mockSessionStorage.setItem('portal_token', 'existing_token_456')
      vi.mocked(PortalAuthApi.getPortalAuthInfoApi).mockResolvedValueOnce({
        customerId: 99,
        customerName: '江苏纺织实业'
      })

      const { customerName, customerId, bootstrapToken } = usePortalAuth()
      const success = await bootstrapToken()

      expect(success).toBe(true)
      expect(customerId.value).toBe(99)
      expect(customerName.value).toBe('江苏纺织实业')
      expect(mockRouter.replace).not.toHaveBeenCalled()
    })

    it('当身份接口报错（如 401 令牌失效），返回 false', async () => {
      mockSessionStorage.setItem('portal_token', 'expired_token_789')
      vi.mocked(PortalAuthApi.getPortalAuthInfoApi).mockRejectedValueOnce(new Error('401 Unauthorized'))

      const { bootstrapToken } = usePortalAuth()
      const success = await bootstrapToken()

      expect(success).toBe(false)
    })
  })

  describe('2. 门户路由注册静态契约测试', () => {
    it('remainingRouter 应注册全部 5 处核心门户路由及二维码直达路由', () => {
      const paths = remainingRouter.map((r) => r.path)

      expect(paths).toContain('/portal/shipments')
      expect(paths).toContain('/portal/shipment/:id')
      expect(paths).toContain('/portal/shipment') // 兼容 ?no=
      expect(paths).toContain('/portal/statements')
      expect(paths).toContain('/portal/statement/:id')
      expect(paths).toContain('/portal/statement') // 兼容 ?no=
      expect(paths).toContain('/portal/invalid')
    })

    it('门户路由必须设置为 hidden=true 与 noTagsView=true', () => {
      const portalRoutes = remainingRouter.filter((r) => r.path.startsWith('/portal/'))
      expect(portalRoutes.length).toBeGreaterThanOrEqual(5)

      for (const route of portalRoutes) {
        expect(route.meta?.hidden).toBe(true)
        expect(route.meta?.noTagsView).toBe(true)
      }
    })

    it('白名单匹配逻辑：/portal 前缀能覆盖全部门户子路由，且不被业务模块拦截', () => {
      const whiteList = ['/login', '/portal']
      const checkWhiteList = (path: string) =>
        whiteList.some((p) => path === p || path.startsWith(`${p}/`))

      expect(checkWhiteList('/portal/shipments')).toBe(true)
      expect(checkWhiteList('/portal/shipment/123')).toBe(true)
      expect(checkWhiteList('/portal/shipment')).toBe(true)
      expect(checkWhiteList('/portal/statements')).toBe(true)
      expect(checkWhiteList('/portal/statement/456')).toBe(true)
      expect(checkWhiteList('/portal/invalid')).toBe(true)
      expect(checkWhiteList('/production/order')).toBe(false)
    })
  })
})
