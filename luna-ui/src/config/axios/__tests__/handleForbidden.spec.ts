import { beforeEach, describe, expect, it, vi } from 'vitest'
import { handleForbidden } from '../service'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import type { AppRouteRecordRaw } from '@/router/types'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    warning: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  },
  ElNotification: {
    error: vi.fn()
  }
}))

vi.mock('@/router', () => {
  const currentRoute = {
    value: {
      path: '/index',
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

vi.mock('@/store/modules/permission', () => ({
  usePermissionStoreWithOut: vi.fn()
}))

vi.mock('@/utils/auth', () => ({
  getAccessToken: vi.fn(),
  getRefreshToken: vi.fn(),
  getTenantId: vi.fn(),
  getVisitTenantId: vi.fn(),
  removeToken: vi.fn(),
  setToken: vi.fn()
}))

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

vi.mock('@/utils/encrypt', () => ({
  ApiEncrypt: {
    encryptRequest: vi.fn(),
    decryptResponse: vi.fn(),
    getEncryptHeader: vi.fn(() => 'Encrypt')
  }
}))

describe('handleForbidden (403 拦截处理)', () => {
  const mockAddRoutersWithSales: AppRouteRecordRaw[] = [
    {
      path: '/business',
      name: 'Business',
      children: [
        {
          path: 'sales-contract',
          name: 'LunaerpElmuiBusinessSalesContract'
        }
      ]
    } as unknown as AppRouteRecordRaw
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('模块未开通场景（当前路由为 /production/greige-order，不在 getAddRouters 中）→ 提示并跳转首页', async () => {
    ;(router.currentRoute as any).value = {
      path: '/production/greige-order',
      name: 'LunaerpElmuiProductionGreigeOrder',
      meta: {}
    }
    vi.mocked(usePermissionStoreWithOut).mockReturnValue({
      getAddRouters: mockAddRoutersWithSales
    } as any)

    await expect(
      handleForbidden({ url: '/lunaerp/production/greige-order/page' })
    ).rejects.toThrow('该模块未开通，请联系管理员')

    expect(ElMessage.error).toHaveBeenCalledWith('该模块未开通，请联系管理员')
    expect(router.push).toHaveBeenCalledWith('/index')
  })

  it('操作权限不足场景（当前菜单在 getAddRouters 中，按钮级 403）→ 提示无操作权限，不跳转', async () => {
    ;(router.currentRoute as any).value = {
      path: '/business/sales-contract',
      name: 'LunaerpElmuiBusinessSalesContract',
      meta: {}
    }
    vi.mocked(usePermissionStoreWithOut).mockReturnValue({
      getAddRouters: mockAddRoutersWithSales
    } as any)

    await expect(
      handleForbidden({ url: '/lunaerp/business/sales-contract/delete' })
    ).rejects.toThrow('无操作权限')

    expect(ElMessage.error).toHaveBeenCalledWith('无操作权限')
    expect(router.push).not.toHaveBeenCalled()
  })

  it('未开通模块 API 被调用（即便当前处于首页）→ 提示模块未开通，当前已在首页无需重复跳转', async () => {
    ;(router.currentRoute as any).value = {
      path: '/index',
      name: 'Home',
      meta: {}
    }
    vi.mocked(usePermissionStoreWithOut).mockReturnValue({
      getAddRouters: mockAddRoutersWithSales
    } as any)

    await expect(
      handleForbidden({ url: '/lunaerp/production/greige-order/export' })
    ).rejects.toThrow('该模块未开通，请联系管理员')

    expect(ElMessage.error).toHaveBeenCalledWith('该模块未开通，请联系管理员')
    // 已经处于 /index，不再重复 push
    expect(router.push).not.toHaveBeenCalled()
  })
})
