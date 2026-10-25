import { beforeEach, describe, expect, it, vi } from 'vitest'
import { isUnactivatedModuleRoute } from '@/utils/moduleGateway'
import type { AppRouteRecordRaw } from '@/router/types'

describe('Route Guard for Unactivated Modules (未开通模块路由守卫)', () => {
  const mockTenantWithBaseinfoAndSales: AppRouteRecordRaw[] = [
    {
      path: '/baseinfo',
      name: 'Baseinfo',
      children: [
        {
          path: 'customer',
          name: 'LunaerpElmuiBaseinfoCustomer'
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
    } as unknown as AppRouteRecordRaw
  ]

  it('未开通模块 URL (/production/xxx, /production/greige-order) 应被判定为未开通，需重定向首页而非 404', () => {
    expect(isUnactivatedModuleRoute('/production/greige-order', mockTenantWithBaseinfoAndSales)).toBe(true)
    expect(isUnactivatedModuleRoute('/production/xxx', mockTenantWithBaseinfoAndSales)).toBe(true)
    expect(isUnactivatedModuleRoute('/production/anything/nested', mockTenantWithBaseinfoAndSales)).toBe(true)
    expect(isUnactivatedModuleRoute('/sample/sample-analysis', mockTenantWithBaseinfoAndSales)).toBe(true)
    expect(isUnactivatedModuleRoute('/stock/stock-in', mockTenantWithBaseinfoAndSales)).toBe(true)
  })

  it('已开通模块 URL (/baseinfo/customer, /business/sales-contract) 正常放行', () => {
    expect(isUnactivatedModuleRoute('/baseinfo/customer', mockTenantWithBaseinfoAndSales)).toBe(false)
    expect(isUnactivatedModuleRoute('/business/sales-contract', mockTenantWithBaseinfoAndSales)).toBe(false)
  })

  it('非已知业务模块的未知路径 (/random-page) 不应被拦截重定向，正常交由 404 路由处理', () => {
    expect(isUnactivatedModuleRoute('/random-page', mockTenantWithBaseinfoAndSales)).toBe(false)
    expect(isUnactivatedModuleRoute('/system/unknown-tool', mockTenantWithBaseinfoAndSales)).toBe(false)
  })
})
