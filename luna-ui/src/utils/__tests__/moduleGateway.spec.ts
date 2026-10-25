import { describe, expect, it } from 'vitest'
import {
  determineForbiddenReason,
  getModuleRootFromPath,
  isModuleActivated,
  isRouteInAddRouters,
  isUnactivatedModuleRoute
} from '../moduleGateway'
import type { AppRouteRecordRaw } from '@/router/types'

describe('moduleGateway utility', () => {
  const mockAddRouters: AppRouteRecordRaw[] = [
    {
      path: '/baseinfo',
      name: 'Baseinfo',
      children: [
        {
          path: 'customer',
          name: 'LunaerpElmuiBaseinfoCustomer'
        },
        {
          path: 'supplier',
          name: 'LunaerpElmuiBaseinfoSupplier'
        }
      ]
    } as unknown as AppRouteRecordRaw,
    {
      path: '/business',
      name: 'Business',
      children: [
        {
          path: 'sales-contract',
          name: 'LunaerpElmuiBusinessSalesContract'
        }
      ]
    } as unknown as AppRouteRecordRaw,
    {
      path: '/stock',
      name: 'Stock',
      children: [
        {
          path: 'stock-in',
          name: 'LunaerpElmuiStockStockIn'
        }
      ]
    } as unknown as AppRouteRecordRaw,
    {
      path: '/:path(.*)*',
      name: '404Page'
    } as unknown as AppRouteRecordRaw
  ]

  describe('getModuleRootFromPath', () => {
    it('should extract module root from direct page path', () => {
      expect(getModuleRootFromPath('/production/greige-order')).toBe('/production')
      expect(getModuleRootFromPath('/production/xxx')).toBe('/production')
      expect(getModuleRootFromPath('/business/sales-contract')).toBe('/business')
      expect(getModuleRootFromPath('/sales/contract')).toBe('/sales')
      expect(getModuleRootFromPath('/stock/stock-in')).toBe('/stock')
      expect(getModuleRootFromPath('/sample/sample-analysis')).toBe('/sample')
      expect(getModuleRootFromPath('/finance/voucher')).toBe('/finance')
      expect(getModuleRootFromPath('/approval/todo')).toBe('/approval')
      expect(getModuleRootFromPath('/baseinfo/customer')).toBe('/baseinfo')
    })

    it('should extract module root from lunaerp-elmui static path', () => {
      expect(
        getModuleRootFromPath('/lunaerp-elmui/production/greige-order/edit/1')
      ).toBe('/production')
      expect(
        getModuleRootFromPath('/lunaerp-elmui/business/sales-contract/edit/2')
      ).toBe('/business')
    })

    it('should extract module root from API URL', () => {
      expect(
        getModuleRootFromPath('/lunaerp/production/greige-order/page')
      ).toBe('/production')
      expect(
        getModuleRootFromPath('/lunaerp/stock/stock-in/get?id=10')
      ).toBe('/stock')
    })

    it('should return undefined for non-business modules', () => {
      expect(getModuleRootFromPath('/system/user')).toBeUndefined()
      expect(getModuleRootFromPath('/index')).toBeUndefined()
      expect(getModuleRootFromPath('/dashboard')).toBeUndefined()
      expect(getModuleRootFromPath('/login')).toBeUndefined()
      expect(getModuleRootFromPath('/some-unknown-path')).toBeUndefined()
    })
  })

  describe('isModuleActivated', () => {
    it('should identify opened modules', () => {
      expect(isModuleActivated('/baseinfo', mockAddRouters)).toBe(true)
      expect(isModuleActivated('/stock', mockAddRouters)).toBe(true)
    })

    it('should identify unopened modules', () => {
      expect(isModuleActivated('/production', mockAddRouters)).toBe(false)
      expect(isModuleActivated('/sample', mockAddRouters)).toBe(false)
      expect(isModuleActivated('/finance', mockAddRouters)).toBe(false)
      expect(isModuleActivated('/approval', mockAddRouters)).toBe(false)
    })

    it('should treat /sales and /business as synonyms', () => {
      // mockAddRouters has /business, so both /business and /sales are considered activated
      expect(isModuleActivated('/business', mockAddRouters)).toBe(true)
      expect(isModuleActivated('/sales', mockAddRouters)).toBe(true)
    })
  })

  describe('isUnactivatedModuleRoute', () => {
    it('should return true for URLs under unopened modules', () => {
      expect(isUnactivatedModuleRoute('/production/greige-order', mockAddRouters)).toBe(true)
      expect(isUnactivatedModuleRoute('/production/xxx', mockAddRouters)).toBe(true)
      expect(isUnactivatedModuleRoute('/sample/analysis', mockAddRouters)).toBe(true)
      expect(
        isUnactivatedModuleRoute('/lunaerp-elmui/production/greige-order/edit/1', mockAddRouters)
      ).toBe(true)
    })

    it('should return false for URLs under opened modules', () => {
      expect(isUnactivatedModuleRoute('/baseinfo/customer', mockAddRouters)).toBe(false)
      expect(isUnactivatedModuleRoute('/stock/stock-in', mockAddRouters)).toBe(false)
      expect(isUnactivatedModuleRoute('/business/sales-contract', mockAddRouters)).toBe(false)
    })

    it('should return false for random non-module URLs (which should show 404)', () => {
      expect(isUnactivatedModuleRoute('/random-404-page', mockAddRouters)).toBe(false)
      expect(isUnactivatedModuleRoute('/system/unknown', mockAddRouters)).toBe(false)
    })
  })

  describe('isRouteInAddRouters', () => {
    it('should return true when menu exists in addRouters', () => {
      expect(
        isRouteInAddRouters(
          { path: '/stock/stock-in' },
          mockAddRouters
        )
      ).toBe(true)

      expect(
        isRouteInAddRouters(
          { name: 'LunaerpElmuiStockStockIn' },
          mockAddRouters
        )
      ).toBe(true)
    })

    it('should return true when activeMenu matches a menu in addRouters', () => {
      expect(
        isRouteInAddRouters(
          {
            path: '/stock/stock-in/workspace/1',
            meta: { activeMenu: '/stock/stock-in' }
          },
          mockAddRouters
        )
      ).toBe(true)
    })

    it('should return false when module is not opened', () => {
      expect(
        isRouteInAddRouters(
          { path: '/production/greige-order' },
          mockAddRouters
        )
      ).toBe(false)
    })
  })

  describe('determineForbiddenReason', () => {
    it('should return MODULE_NOT_ACTIVATED when route is under unopened module', () => {
      const reason = determineForbiddenReason({
        currentRoute: { path: '/production/greige-order' },
        requestUrl: '/lunaerp/production/greige-order/page',
        addRouters: mockAddRouters
      })
      expect(reason).toBe('MODULE_NOT_ACTIVATED')
    })

    it('should return MODULE_NOT_ACTIVATED when API is under unopened module even if on other page', () => {
      const reason = determineForbiddenReason({
        currentRoute: { path: '/index' },
        requestUrl: '/lunaerp/production/greige-order/export',
        addRouters: mockAddRouters
      })
      expect(reason).toBe('MODULE_NOT_ACTIVATED')
    })

    it('should return BUTTON_PERMISSION_DENIED when menu exists in addRouters', () => {
      const reason = determineForbiddenReason({
        currentRoute: { path: '/stock/stock-in' },
        requestUrl: '/lunaerp/stock/stock-in/delete?id=1',
        addRouters: mockAddRouters
      })
      expect(reason).toBe('BUTTON_PERMISSION_DENIED')
    })

    it('should return BUTTON_PERMISSION_DENIED when sub-page activeMenu exists in addRouters', () => {
      const reason = determineForbiddenReason({
        currentRoute: {
          path: '/stock/stock-in/workspace/1',
          meta: { activeMenu: '/stock/stock-in' }
        },
        requestUrl: '/lunaerp/stock/stock-in/update',
        addRouters: mockAddRouters
      })
      expect(reason).toBe('BUTTON_PERMISSION_DENIED')
    })

    it('should handle query parameters and hashes properly', () => {
      const reason = determineForbiddenReason({
        currentRoute: { path: '/production/greige-order?tab=audit#summary' },
        requestUrl: '/lunaerp/production/greige-order/detail?id=1#anchor',
        addRouters: mockAddRouters
      })
      expect(reason).toBe('MODULE_NOT_ACTIVATED')
    })

    it('should identify unactivated module for all known module roots when not in addRouters', () => {
      const unactivatedRoots = ['/production', '/sample', '/finance', '/approval']
      for (const root of unactivatedRoots) {
        expect(isUnactivatedModuleRoute(`${root}/anything`, mockAddRouters)).toBe(true)
      }
    })

    it('should identify activated module for all present roots', () => {
      const activatedRoots = ['/baseinfo', '/business', '/sales', '/stock']
      for (const root of activatedRoots) {
        expect(isUnactivatedModuleRoute(`${root}/anything`, mockAddRouters)).toBe(false)
      }
    })
  })
})
