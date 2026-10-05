import { describe, expect, it } from 'vitest'
import {
  calculateSampleCost,
  validateLossRate,
  round2,
  round4
} from '../sample-cost-calculator'

describe('sample-cost-calculator (样品分析成本核算纯函数)', () => {
  describe('validateLossRate', () => {
    it('合法损耗率（0 <= lossRate < 100）校验通过', () => {
      expect(validateLossRate(0)).toEqual({ isValid: true, value: 0 })
      expect(validateLossRate(5)).toEqual({ isValid: true, value: 5 })
      expect(validateLossRate(99.99)).toEqual({ isValid: true, value: 99.99 })
      expect(validateLossRate('5.5')).toEqual({ isValid: true, value: 5.5 })
    })

    it('空值或未填写时兜底为 0 且合法', () => {
      expect(validateLossRate(null)).toEqual({ isValid: true, value: 0 })
      expect(validateLossRate(undefined)).toEqual({ isValid: true, value: 0 })
      expect(validateLossRate('')).toEqual({ isValid: true, value: 0 })
      expect(validateLossRate('   ')).toEqual({ isValid: true, value: 0 })
    })

    it('非法损耗率（负数、>=100、非数字）返回错误信息', () => {
      expect(validateLossRate(-1).isValid).toBe(false)
      expect(validateLossRate(100).isValid).toBe(false)
      expect(validateLossRate(105).isValid).toBe(false)
      expect(validateLossRate('abc').isValid).toBe(false)
    })
  })

  describe('calculateSampleCost 权威算法', () => {
    it('规格文档典型例：30%*23000(吨价) + 70%*30 + 工费2 + 染整1 + 其他0.5，损耗5% => 33.05', () => {
      const yarnSpecs = [
        { yarnSpecId: 1, unitPrice: 23000, ratio: 30 },
        { yarnSpecId: 2, unitPrice: 30, ratio: 70 }
      ]
      const settings = {
        productionCost: 2,
        dyeingCost: 1,
        otherCost: 0.5,
        lossRate: 5
      }

      const result = calculateSampleCost(yarnSpecs, settings)
      expect(result.ratioTotal).toBe(100)
      expect(result.yarnCost).toBe(27.9) // 23 * 0.3 + 30 * 0.7 = 6.9 + 21 = 27.9
      expect(result.productionCost).toBe(2)
      expect(result.dyeingCost).toBe(1)
      expect(result.otherCost).toBe(0.5)
      expect(result.lossRate).toBe(5)
      expect(result.isLossRateValid).toBe(true)
      // (27.9 + 2 + 1 + 0.5) / (1 - 0.05) = 31.4 / 0.95 = 33.0526... -> 33.05
      expect(result.totalCost).toBe(33.05)
    })

    it('单价 > 1000 自动折算为千分之一（吨价转公斤价），<= 1000 保持不变', () => {
      const yarnSpecs = [
        { yarnSpecId: 1, unitPrice: 1000, ratio: 100 }
      ]
      const res1000 = calculateSampleCost(yarnSpecs, { lossRate: 0 })
      expect(res1000.yarnCost).toBe(1000)

      const yarnSpecsHigh = [
        { yarnSpecId: 1, unitPrice: 1001, ratio: 100 }
      ]
      const res1001 = calculateSampleCost(yarnSpecsHigh, { lossRate: 0 })
      expect(res1001.yarnCost).toBe(1.001)
    })

    it('其他费用 (otherCost) 必须计入总成本', () => {
      const yarnSpecs = [{ yarnSpecId: 1, unitPrice: 20, ratio: 100 }]
      const withoutOther = calculateSampleCost(yarnSpecs, { productionCost: 5, dyeingCost: 3, otherCost: 0, lossRate: 0 })
      expect(withoutOther.totalCost).toBe(28)

      const withOther = calculateSampleCost(yarnSpecs, { productionCost: 5, dyeingCost: 3, otherCost: 4.5, lossRate: 0 })
      expect(withOther.totalCost).toBe(32.5)
    })

    it('费用项留空或非法时兜底按 0 计算', () => {
      const yarnSpecs = [{ yarnSpecId: 1, unitPrice: 10, ratio: 100 }]
      const result = calculateSampleCost(yarnSpecs, {
        productionCost: '',
        dyeingCost: null,
        otherCost: undefined,
        lossRate: null
      })
      expect(result.productionCost).toBe(0)
      expect(result.dyeingCost).toBe(0)
      expect(result.otherCost).toBe(0)
      expect(result.lossRate).toBe(0)
      expect(result.totalCost).toBe(10)
    })

    it('损耗率为 0 时直接为各项费用之和', () => {
      const yarnSpecs = [{ yarnSpecId: 1, unitPrice: 50, ratio: 100 }]
      const result = calculateSampleCost(yarnSpecs, {
        productionCost: 10,
        dyeingCost: 5,
        otherCost: 2,
        lossRate: 0
      })
      expect(result.totalCost).toBe(67)
    })

    it('损耗率非法时 (>=100 或 <0)，isLossRateValid 为 false 且 totalCost 为 null', () => {
      const yarnSpecs = [{ yarnSpecId: 1, unitPrice: 50, ratio: 100 }]
      const res100 = calculateSampleCost(yarnSpecs, { lossRate: 100 })
      expect(res100.isLossRateValid).toBe(false)
      expect(res100.totalCost).toBeNull()
      expect(res100.errorMessage).toBeDefined()

      const resNegative = calculateSampleCost(yarnSpecs, { lossRate: -5 })
      expect(resNegative.isLossRateValid).toBe(false)
      expect(resNegative.totalCost).toBeNull()
    })

    it('比例合计正确统计，即使不等于 100 也能计算成本', () => {
      const yarnSpecs = [
        { yarnSpecId: 1, unitPrice: 40, ratio: 30 },
        { yarnSpecId: 2, unitPrice: 60, ratio: 40 }
      ]
      const result = calculateSampleCost(yarnSpecs, { lossRate: 0 })
      expect(result.ratioTotal).toBe(70)
      // 40 * 0.3 + 60 * 0.4 = 12 + 24 = 36
      expect(result.yarnCost).toBe(36)
      expect(result.totalCost).toBe(36)
    })
  })

  describe('Issue 06 回归断言：供应商报价与对外报价不参与内部总成本计算', () => {
    it('设定任意供应商报价与对外报价，内部成本核算结果恒保持不变', () => {
      const yarnSpecs = [
        { yarnSpecId: 1, unitPrice: 30, ratio: 100 }
      ]
      const baseSettings = {
        productionCost: 2,
        dyeingCost: 1,
        otherCost: 0.5,
        lossRate: 5
      }
      const baseResult = calculateSampleCost(yarnSpecs, baseSettings)

      // 无论带多少供应商胚布报价与客户对外报价
      const settingsWithQuotes = {
        ...baseSettings,
        quotedPrice: 99999.99,
        quotedRemark: '含税含运费',
        supplierQuotes: [
          { supplierId: 101, supplierName: '昂贵供应商', greigePrice: 88888.88 },
          { supplierId: null, supplierName: '纯手填档口', greigePrice: 0.01 }
        ]
      }
      const quoteResult = calculateSampleCost(yarnSpecs, settingsWithQuotes as any)

      expect(quoteResult.yarnCost).toBe(baseResult.yarnCost)
      expect(quoteResult.productionCost).toBe(baseResult.productionCost)
      expect(quoteResult.dyeingCost).toBe(baseResult.dyeingCost)
      expect(quoteResult.otherCost).toBe(baseResult.otherCost)
      expect(quoteResult.lossRate).toBe(baseResult.lossRate)
      expect(quoteResult.totalCost).toBe(baseResult.totalCost)
      expect(quoteResult.totalCost).toBe(35.26) // (30 + 2 + 1 + 0.5) / 0.95 = 33.5 / 0.95 = 35.263... -> 35.26
    })
  })

  describe('辅助工具函数 round2, round4', () => {
    it('round2 正确四舍五入保留 2 位小数', () => {
      expect(round2(33.0526)).toBe(33.05)
      expect(round2(33.056)).toBe(33.06)
      expect(round2(10)).toBe(10)
    })

    it('round4 正确保留 4 位小数', () => {
      expect(round4(1.23456)).toBe(1.2346)
      expect(round4(1.23451)).toBe(1.2345)
    })
  })
})
