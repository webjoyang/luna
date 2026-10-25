import { describe, it, expect, vi } from 'vitest'

vi.mock('@/config/axios', () => ({
  default: {
    post: vi.fn().mockResolvedValue({
      rows: [],
      missingBomOrders: [],
      missingWeightOrders: [],
      shortageSpecCount: 0,
      totalShortageQty: 0
    })
  }
}))

import request from '@/config/axios'
import { runMaterialCheck } from '@/api/lunaerp-elmui/production/material-check'
import {
  deriveMaterialCheckStatus,
  formatSuggestedPurchaseQty,
  calculateSummaryStats,
  buildYarnOrderLocation,
  buildYarnRecipeBomLocation,
  buildWeavingOrderWorkspaceLocation,
  IN_TRANSIT_TOOLTIP_RESTRICTIONS,
  type MaterialCheckRowDisplay
} from '../model/material-check'

describe('Seam 1 & 2: Material Check Model & Pure Logic (Issue 09)', () => {
  describe('Status Tag Derivation (spec §4.3 & §6.2)', () => {
    it('当可用库存 >= 需求量时，派生为齐套 (READY / success)', () => {
      const row: Partial<MaterialCheckRowDisplay> = {
        requiredQty: 100,
        availableQty: 120,
        inTransitQty: 0,
        shortageExcludeInTransit: 0,
        shortageIncludeInTransit: 0
      }
      const status = deriveMaterialCheckStatus(row as MaterialCheckRowDisplay)
      expect(status.code).toBe('READY')
      expect(status.label).toBe('齐套')
      expect(status.tagType).toBe('success')
    })

    it('当可用库存 < 需求量，但在途可覆盖（shortageIncludeInTransit <= 0）时，派生为在途可覆盖 (TRANSIT_COVERED / warning)', () => {
      const row: Partial<MaterialCheckRowDisplay> = {
        requiredQty: 100,
        availableQty: 60,
        inTransitQty: 50,
        shortageExcludeInTransit: 40,
        shortageIncludeInTransit: 0
      }
      const status = deriveMaterialCheckStatus(row as MaterialCheckRowDisplay)
      expect(status.code).toBe('TRANSIT_COVERED')
      expect(status.label).toBe('在途可覆盖')
      expect(status.tagType).toBe('warning')
    })

    it('当含在途缺口 > 0 时，派生为缺料 (SHORTAGE / danger)', () => {
      const row: Partial<MaterialCheckRowDisplay> = {
        requiredQty: 100,
        availableQty: 40,
        inTransitQty: 20,
        shortageExcludeInTransit: 60,
        shortageIncludeInTransit: 40
      }
      const status = deriveMaterialCheckStatus(row as MaterialCheckRowDisplay)
      expect(status.code).toBe('SHORTAGE')
      expect(status.label).toBe('缺料')
      expect(status.tagType).toBe('danger')
    })
  })

  describe('Suggested Purchase Qty Formatting (spec §4.3)', () => {
    it('建议采购量 <= 0 时显示"已齐套/在途中"', () => {
      expect(formatSuggestedPurchaseQty(0)).toBe('已齐套/在途中')
      expect(formatSuggestedPurchaseQty(-5)).toBe('已齐套/在途中')
    })

    it('建议采购量 > 0 时显示两位小数数值', () => {
      expect(formatSuggestedPurchaseQty(250)).toBe('250.00')
      expect(formatSuggestedPurchaseQty(123.456)).toBe('123.46')
    })
  })

  describe('Summary Stats Calculation & Switch 口径 (spec §4.1, §4.3, §6.2)', () => {
    const mockRows: MaterialCheckRowDisplay[] = [
      {
        yarnSpecId: 1,
        yarnSpecName: '32S 纯棉',
        requiredQty: 100,
        availableQty: 120,
        inTransitQty: 0,
        shortageExcludeInTransit: 0,
        shortageIncludeInTransit: 0,
        suggestedPurchaseQty: 0
      },
      {
        yarnSpecId: 2,
        yarnSpecName: '40S 涤棉',
        requiredQty: 200,
        availableQty: 150,
        inTransitQty: 50,
        shortageExcludeInTransit: 50,
        shortageIncludeInTransit: 0,
        suggestedPurchaseQty: 0
      },
      {
        yarnSpecId: 3,
        yarnSpecName: '50S 人棉',
        requiredQty: 300,
        availableQty: 100,
        inTransitQty: 50,
        shortageExcludeInTransit: 200,
        shortageIncludeInTransit: 150,
        suggestedPurchaseQty: 150
      }
    ]

    it('不含在途时（includeInTransit=false），缺料统计基于 shortageExcludeInTransit', () => {
      const stats = calculateSummaryStats(mockRows, false)
      // 规格 2 缺口 50，规格 3 缺口 200 -> 共 2 种规格，合计 250 KG
      expect(stats.shortageSpecCount).toBe(2)
      expect(stats.totalShortageQty).toBe(250)
    })

    it('含在途时（includeInTransit=true），缺料统计基于 shortageIncludeInTransit', () => {
      const stats = calculateSummaryStats(mockRows, true)
      // 规格 2 含在途缺口 0（在途可覆盖），仅规格 3 缺口 150 -> 共 1 种规格，合计 150 KG
      expect(stats.shortageSpecCount).toBe(1)
      expect(stats.totalShortageQty).toBe(150)
    })
  })

  describe('Navigation Helpers (spec §4.3, §6.2)', () => {
    it('去纱线采购单带规格关键词', () => {
      const loc = buildYarnOrderLocation('32S 棉')
      expect(loc.path).toBe('/production/yarn-order')
      expect(loc.query).toEqual({ keyword: '32S 棉' })
    })

    it('去配比 BOM 管理页按产品过滤', () => {
      const loc = buildYarnRecipeBomLocation(10, 'P001', '纯棉汗布')
      expect(loc.path).toBe('/production/yarn-recipe-bom')
      expect(loc.query).toEqual({
        productId: '10',
        productCode: 'P001',
        productName: '纯棉汗布'
      })
    })

    it('去织造单工作台补录计划重量/匹数', () => {
      const loc = buildWeavingOrderWorkspaceLocation(105)
      expect(loc.path).toBe('/production/weaving-order/workspace/105')
    })
  })

  describe('In-transit Tooltip Restrictions (spec §4.4)', () => {
    it('包含全部三条已知口径限制说明', () => {
      expect(IN_TRANSIT_TOOLTIP_RESTRICTIONS).toHaveLength(3)
      expect(IN_TRANSIT_TOOLTIP_RESTRICTIONS[0]).toContain('采购单状态')
      expect(IN_TRANSIT_TOOLTIP_RESTRICTIONS[1]).toContain('时点快照')
      expect(IN_TRANSIT_TOOLTIP_RESTRICTIONS[2]).toContain('理论折算')
    })
  })

  describe('API Layer Integration (spec §4, §7)', () => {
    it('runMaterialCheck API function sends POST request to /lunaerp/production/material-check/run', async () => {
      const req = {
        weavingOrderIds: [1, 2],
        warehouseIds: [10],
        includeInTransit: true
      }
      await runMaterialCheck(req)

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/material-check/run',
        data: req
      })
    })
  })
})
