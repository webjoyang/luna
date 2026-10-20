import { describe, it, expect, vi } from 'vitest'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import { formatPercent, getPeriodPresets, computeGrossMarginMetrics } from '../model/overview'

describe('Finance Overview Domain Model & Utilities', () => {
  describe('formatPercent', () => {
    it('should format decimals as percentages with 2 decimal places by default', () => {
      expect(formatPercent(0.3333)).toBe('33.33%')
      expect(formatPercent(0.5)).toBe('50.00%')
      expect(formatPercent(1)).toBe('100.00%')
      expect(formatPercent(0)).toBe('0.00%')
    })

    it('should handle null, undefined, empty string or invalid input gracefully', () => {
      expect(formatPercent(null)).toBe('0.00%')
      expect(formatPercent(undefined)).toBe('0.00%')
      expect(formatPercent('')).toBe('0.00%')
      expect(formatPercent('invalid')).toBe('0.00%')
    })
  })

  describe('getPeriodPresets', () => {
    it('should calculate valid date range presets for a given date', () => {
      const fixedDate = new Date(2026, 9, 20) // 2026-10-20
      const presets = getPeriodPresets(fixedDate)

      expect(presets.thisMonth[0]).toBe('2026-10-01')
      expect(presets.thisMonth[1]).toBe('2026-10-31')

      expect(presets.lastMonth[0]).toBe('2026-09-01')
      expect(presets.lastMonth[1]).toBe('2026-09-30')

      expect(presets.thisQuarter[0]).toBe('2026-10-01')
      expect(presets.thisQuarter[1]).toBe('2026-12-31')

      expect(presets.thisYear[0]).toBe('2026-01-01')
      expect(presets.thisYear[1]).toBe('2026-12-31')
    })
  })

  describe('computeGrossMarginMetrics', () => {
    it('should calculate gross profit and margin rate accurately', () => {
      const res = computeGrossMarginMetrics(300000, 200000)
      expect(res.grossProfit).toBe(100000)
      expect(res.grossMarginRate).toBeCloseTo(0.333333, 4)
    })

    it('should handle zero revenue without division by zero', () => {
      const res = computeGrossMarginMetrics(0, 50000)
      expect(res.grossProfit).toBe(-50000)
      expect(res.grossMarginRate).toBe(0)
    })
  })
})
