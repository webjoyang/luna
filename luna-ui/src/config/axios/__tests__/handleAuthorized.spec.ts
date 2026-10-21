import { beforeEach, describe, expect, it, vi } from 'vitest'
import { handleAuthorized, isRelogin } from '../service'
import { ElMessageBox } from 'element-plus'
import router, { resetRouter } from '@/router'
import { deleteUserCache } from '@/hooks/web/useCache'
import { removeToken } from '@/utils/auth'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    warning: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn(() => Promise.resolve())
  },
  ElNotification: {
    error: vi.fn()
  }
}))

vi.mock('@/router', () => {
  const currentRoute = {
    value: {
      path: '/index',
      fullPath: '/index',
      name: 'Home',
      meta: {}
    }
  }
  return {
    default: {
      currentRoute,
      push: vi.fn()
    },
    resetRouter: vi.fn()
  }
})

vi.mock('@/hooks/web/useCache', () => ({
  deleteUserCache: vi.fn(),
  useCache: () => ({
    wsCache: {
      get: vi.fn(),
      set: vi.fn(),
      delete: vi.fn()
    }
  })
}))

vi.mock('@/utils/auth', () => ({
  getAccessToken: vi.fn(),
  getRefreshToken: vi.fn(),
  getTenantId: vi.fn(),
  getVisitTenantId: vi.fn(),
  removeToken: vi.fn(),
  setToken: vi.fn()
}))

vi.mock('@/hooks/web/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

describe('401 登出/Token 失效处理 handleAuthorized（06-h5-layout-route-guard-login-403）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    isRelogin.show = false
  })

  it('PC 环境：弹出 ElMessageBox.confirm 提示框，不直接调用 router.push', async () => {
    // 模拟 PC 路由路径
    ;(globalThis as any).window = {
      location: {
        pathname: '/production/greige-order',
        search: '?id=123',
        href: 'http://localhost/production/greige-order?id=123'
      }
    }

    try {
      await handleAuthorized()
    } catch (e) {
      // handleAuthorized 返回 Promise.reject
    }

    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(1)
    expect(router.push).not.toHaveBeenCalled()
  })

  it('H5 环境：静默清除 token、缓存与路由，直接 router.push 到 /h5/login?redirect=...，严禁弹出 PC ElMessageBox', async () => {
    // 模拟 H5 路由路径
    ;(globalThis as any).window = {
      location: {
        pathname: '/h5/report',
        search: '?tab=tasks',
        href: 'http://localhost/h5/report?tab=tasks'
      }
    }

    try {
      await handleAuthorized()
    } catch (e) {
      // 期望 Promise.reject 被捕获
    }

    // 严禁弹出 PC 对话框
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()

    // 状态清理检查
    expect(resetRouter).toHaveBeenCalledTimes(1)
    expect(deleteUserCache).toHaveBeenCalledTimes(1)
    expect(removeToken).toHaveBeenCalledTimes(1)
    expect(isRelogin.show).toBe(false)

    // 跳转至 H5 登录页并保留 redirect 参数
    expect(router.push).toHaveBeenCalledWith(
      '/h5/login?redirect=' + encodeURIComponent('/h5/report?tab=tasks')
    )
  })

  it('H5 处于 /h5/login 时：避免重复 push 造成路由死循环', async () => {
    ;(globalThis as any).window = {
      location: {
        pathname: '/h5/login',
        search: '',
        href: 'http://localhost/h5/login'
      }
    }

    try {
      await handleAuthorized()
    } catch (e) {}

    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(router.push).not.toHaveBeenCalled()
  })
})
