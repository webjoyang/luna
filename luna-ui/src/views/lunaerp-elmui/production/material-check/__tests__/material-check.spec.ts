import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as MaterialCheckApi from '@/api/lunaerp-elmui/production/material-check'
import request from '@/config/axios'
import type {
  MaterialCheckReqVO,
  MaterialCheckRespVO,
  MaterialCheckRowVO,
  MissingBomOrderVO,
  MissingWeightOrderVO
} from '@/api/lunaerp-elmui/production/material-check'
import {
  deriveMaterialStatus,
  formatSuggestedQty,
  calculateSummaryBadges,
  IN_TRANSIT_LIMITS,
  getJumpToBomLocation,
  getJumpToWeavingOrderLocation,
  getJumpToYarnOrderLocation
} from '../helper'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Issue 09: 齐套检查前端 API 与业务逻辑单测', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API Client 端点交互测试', () => {
    it('runMaterialCheck 应向 POST /lunaerp/production/material-check/run 发起请求', async () => {
      const mockResp: MaterialCheckRespVO = {
        rows: [],
        missingBomOrders: [],
        missingWeightOrders: [],
        shortageSpecCount: 0,
        totalShortageQty: 0
      }
      vi.mocked(request.post).mockResolvedValueOnce(mockResp)

      const req: MaterialCheckReqVO = {
        weavingOrderIds: [1, 2],
        warehouseIds: [10],
        includeInTransit: true
      }

      const res = await MaterialCheckApi.runMaterialCheck(req)

      expect(request.post).toHaveBeenCalledTimes(1)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/material-check/run',
        data: req
      })
      expect(res).toEqual(mockResp)
    })

    it('runMaterialCheck 传空参时应正常传递', async () => {
      vi.mocked(request.post).mockResolvedValueOnce({ rows: [] } as any)

      await MaterialCheckApi.runMaterialCheck({})

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/material-check/run',
        data: {}
      })
    })
  })

  describe('2. 状态标签派生逻辑 (deriveMaterialStatus)', () => {
    it('当可用库存 >= 需求量时，派生为【齐套】(success)', () => {
      // 可用 600 >= 需求 500
      const row: MaterialCheckRowVO = {
        yarnSpecId: 1,
        demandQty: 500,
        stockQty: 600,
        availableStock: 600,
        inTransitQty: 0,
        shortageWithoutInTransit: 0,
        shortageWithInTransit: 0,
        suggestedPurchaseQty: 0
      }

      const result = deriveMaterialStatus(row)
      expect(result.status).toBe('COMPLETE')
      expect(result.label).toBe('齐套')
      expect(result.tagType).toBe('success')
    })

    it('当可用库存刚好等于需求量时，派生为【齐套】(success)', () => {
      const row: MaterialCheckRowVO = {
        yarnSpecId: 1,
        demandQty: 500,
        stockQty: 500,
        inTransitQty: 100,
        shortageWithoutInTransit: 0,
        shortageWithInTransit: 0,
        suggestedPurchaseQty: 0
      }

      const result = deriveMaterialStatus(row)
      expect(result.status).toBe('COMPLETE')
      expect(result.label).toBe('齐套')
      expect(result.tagType).toBe('success')
    })

    it('当可用库存不足，但在途量充足可覆盖缺口时，派生为【在途可覆盖】(warning)', () => {
      // 需求 500, 可用 300, 不含在途缺口 200; 在途 250, 含在途缺口 0
      const row: MaterialCheckRowVO = {
        yarnSpecId: 2,
        demandQty: 500,
        stockQty: 300,
        inTransitQty: 250,
        shortageWithoutInTransit: 200,
        shortageWithInTransit: 0,
        suggestedPurchaseQty: 0
      }

      const result = deriveMaterialStatus(row)
      expect(result.status).toBe('TRANSIT_COVERED')
      expect(result.label).toBe('在途可覆盖')
      expect(result.tagType).toBe('warning')
    })

    it('当可用库存与在途量之和仍不足以覆盖需求时，派生为【缺料】(danger)', () => {
      // 需求 500, 可用 200, 在途 100, 含在途缺口 200
      const row: MaterialCheckRowVO = {
        yarnSpecId: 3,
        demandQty: 500,
        stockQty: 200,
        inTransitQty: 100,
        shortageWithoutInTransit: 300,
        shortageWithInTransit: 200,
        suggestedPurchaseQty: 200
      }

      const result = deriveMaterialStatus(row)
      expect(result.status).toBe('SHORTAGE')
      expect(result.label).toBe('缺料')
      expect(result.tagType).toBe('danger')
    })
  })

  describe('3. 建议采购量展示 (formatSuggestedQty)', () => {
    it('建议采购量 <= 0 时，显示【已齐套/在途中】', () => {
      expect(formatSuggestedQty(0)).toEqual({ isZero: true, text: '已齐套/在途中' })
      expect(formatSuggestedQty(-10)).toEqual({ isZero: true, text: '已齐套/在途中' })
      expect(formatSuggestedQty(undefined)).toEqual({ isZero: true, text: '已齐套/在途中' })
    })

    it('建议采购量 > 0 时，显示具体 KG 格式', () => {
      expect(formatSuggestedQty(150.5)).toEqual({ isZero: false, text: '150.50 KG' })
      expect(formatSuggestedQty(200)).toEqual({ isZero: false, text: '200.00 KG' })
    })
  })

  describe('4. 顶部汇总徽标口径切换 (calculateSummaryBadges)', () => {
    const mockRows: MaterialCheckRowVO[] = [
      {
        yarnSpecId: 1,
        yarnSpecName: '32S 纯棉',
        demandQty: 500,
        stockQty: 600,
        inTransitQty: 0,
        shortageWithoutInTransit: 0,
        shortageWithInTransit: 0,
        suggestedPurchaseQty: 0
      },
      {
        yarnSpecId: 2,
        yarnSpecName: '40S 精梳棉',
        demandQty: 400,
        stockQty: 100,
        inTransitQty: 300,
        shortageWithoutInTransit: 300, // 不含在途有缺口 300
        shortageWithInTransit: 0, // 含在途缺口 0（在途覆盖）
        suggestedPurchaseQty: 0
      },
      {
        yarnSpecId: 3,
        yarnSpecName: '50D 涤纶',
        demandQty: 600,
        stockQty: 100,
        inTransitQty: 150,
        shortageWithoutInTransit: 500, // 不含在途缺口 500
        shortageWithInTransit: 350, // 含在途缺口 350
        suggestedPurchaseQty: 350
      }
    ]

    it('不含在途口径 (includeInTransit = false) 下：缺料规格数与合计 KG 计入不含在途缺口', () => {
      // 规格 2 和规格 3 不含在途缺口 > 0 -> 2 种规格，合计 300 + 500 = 800 KG
      const badges = calculateSummaryBadges(mockRows, false)
      expect(badges.shortageSpecCount).toBe(2)
      expect(badges.totalShortageKg).toBe(800.0)
    })

    it('含在途口径 (includeInTransit = true) 下：缺料规格数与合计 KG 计入含在途缺口', () => {
      // 仅规格 3 含在途缺口 > 0 (350) -> 1 种规格，合计 350 KG
      const badges = calculateSummaryBadges(mockRows, true)
      expect(badges.shortageSpecCount).toBe(1)
      expect(badges.totalShortageKg).toBe(350.0)
    })

    it('空列表时应返回 0', () => {
      expect(calculateSummaryBadges([], false)).toEqual({ shortageSpecCount: 0, totalShortageKg: 0 })
      expect(calculateSummaryBadges([], true)).toEqual({ shortageSpecCount: 0, totalShortageKg: 0 })
    })
  })

  describe('5. 在途口径限制说明 (IN_TRANSIT_LIMITS)', () => {
    it('必须完整包含 3 条在途口径限制', () => {
      expect(IN_TRANSIT_LIMITS.length).toBe(3)
      // ① 在途以采购单状态近似，无法自动扣减部分到货
      expect(IN_TRANSIT_LIMITS[0]).toContain('在途以采购单状态近似')
      expect(IN_TRANSIT_LIMITS[0]).toContain('部分到货无法自动扣减')
      // ② 时点快照，不预留不冻结库存
      expect(IN_TRANSIT_LIMITS[1]).toContain('时点快照')
      expect(IN_TRANSIT_LIMITS[1]).toContain('不预留/不冻结库存')
      // ③ 理论折算，不含损耗率差异
      expect(IN_TRANSIT_LIMITS[2]).toContain('理论折算')
      expect(IN_TRANSIT_LIMITS[2]).toContain('损耗率')
    })
  })

  describe('6. 页面跳转与路由参数生成测试', () => {
    it('getJumpToBomLocation: 缺配比BOM时跳转 /production/yarn-recipe-bom 并携带 productId/productCode', () => {
      const order: MissingBomOrderVO = {
        weavingOrderId: 101,
        weavingOrderCode: 'ZZ-20261001-01',
        productId: 205,
        productCode: 'P-COTTON-01',
        productName: '纯棉针织汗布'
      }

      const loc = getJumpToBomLocation(order)
      expect(loc.path).toBe('/production/yarn-recipe-bom')
      expect(loc.query.productId).toBe('205')
      expect(loc.query.productCode).toBe('P-COTTON-01')
      expect(loc.query.productName).toBe('纯棉针织汗布')
    })

    it('getJumpToWeavingOrderLocation: 缺重量/匹数时跳转 /production/weaving-order 并携带 id/code', () => {
      const order: MissingWeightOrderVO = {
        weavingOrderId: 102,
        weavingOrderCode: 'ZZ-20261001-02',
        reason: '计划总重量为空'
      }

      const loc = getJumpToWeavingOrderLocation(order)
      expect(loc.path).toBe('/production/weaving-order')
      expect(loc.query.id).toBe('102')
      expect(loc.query.code).toBe('ZZ-20261001-02')
    })

    it('getJumpToYarnOrderLocation: 缺料行跳转 /production/yarn-order 并携带规格关键词', () => {
      const row: MaterialCheckRowVO = {
        yarnSpecId: 55,
        yarnSpecName: '32S 精梳棉针织纱',
        demandQty: 800,
        stockQty: 200,
        inTransitQty: 100,
        shortageWithoutInTransit: 600,
        shortageWithInTransit: 500,
        suggestedPurchaseQty: 500
      }

      const loc = getJumpToYarnOrderLocation(row)
      expect(loc.path).toBe('/production/yarn-order')
      expect(loc.query.keyword).toBe('32S 精梳棉针织纱')
    })
  })
})
