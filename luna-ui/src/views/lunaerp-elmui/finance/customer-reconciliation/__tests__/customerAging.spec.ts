import { describe, it, expect, vi } from 'vitest'

// 纯 Node 环境守卫：mock @/config/axios 防止级联导入 window/webHistory 报错
vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import {
  formatMoney,
  formatAgingBucketTag,
  formatAgingOverdueTag,
  calculateAgingBucketPercentages,
  calculateAgingGrandTotals,
  AGING_BUCKETS
} from '../model/customerAging'
import type { CustomerAgingSummaryRespVO } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

describe('customerAging 领域模型与辅助函数测试', () => {
  it('五桶常量声明校验', () => {
    expect(AGING_BUCKETS).toHaveLength(5)
    expect(AGING_BUCKETS.map((b) => b.key)).toEqual([
      'bucket0To30',
      'bucket31To60',
      'bucket61To90',
      'bucket91To180',
      'bucket181Plus'
    ])
  })

  it('formatMoney 格式化千分位金额', () => {
    expect(formatMoney(0)).toBe('0.00')
    expect(formatMoney(null)).toBe('0.00')
    expect(formatMoney(undefined)).toBe('0.00')
    expect(formatMoney('1234567.89')).toBe('1,234,567.89')
    expect(formatMoney(1000)).toBe('1,000.00')
  })

  it('formatAgingBucketTag 五桶标签颜色与描述映射', () => {
    expect(formatAgingBucketTag('0-30')).toEqual({ type: 'success', label: '0–30天' })
    expect(formatAgingBucketTag('31-60')).toEqual({ type: '', label: '31–60天' })
    expect(formatAgingBucketTag('61-90')).toEqual({ type: 'warning', label: '61–90天' })
    expect(formatAgingBucketTag('91-180')).toEqual({ type: 'danger', label: '91–180天' })
    expect(formatAgingBucketTag('181+')).toEqual({ type: 'info', label: '181天以上' })
    expect(formatAgingBucketTag('unknown')).toEqual({ type: 'info', label: 'unknown' })
  })

  it('formatAgingOverdueTag 逾期标签映射', () => {
    expect(formatAgingOverdueTag(false, 0)).toEqual({ type: 'success', label: '正常未逾期' })
    expect(formatAgingOverdueTag(true, 15)).toEqual({ type: 'danger', label: '已逾期 15 天' })
    expect(formatAgingOverdueTag(true, 0)).toEqual({ type: 'danger', label: '已逾期 0 天' })
  })

  it('calculateAgingBucketPercentages 计算五桶占比', () => {
    const row: CustomerAgingSummaryRespVO = {
      customerId: 100,
      customerName: '测试客户',
      totalArBalance: 10000,
      bucket0To30: 5000,
      bucket31To60: 3000,
      bucket61To90: 1000,
      bucket91To180: 500,
      bucket181Plus: 500,
      advanceBalance: 2000,
      overdueAmount: 1000
    }

    const pcts = calculateAgingBucketPercentages(row)
    expect(pcts.pct0To30).toBe(50)
    expect(pcts.pct31To60).toBe(30)
    expect(pcts.pct61To90).toBe(10)
    expect(pcts.pct91To180).toBe(5)
    expect(pcts.pct181Plus).toBe(5)

    // totalArBalance 为 0 时容错
    const zeroPcts = calculateAgingBucketPercentages({ ...row, totalArBalance: 0 })
    expect(zeroPcts.pct0To30).toBe(0)
    expect(zeroPcts.pct31To60).toBe(0)
  })

  it('calculateAgingGrandTotals 计算多客户行汇总合计', () => {
    const list: CustomerAgingSummaryRespVO[] = [
      {
        customerId: 1,
        customerName: '客户A',
        totalArBalance: 1000,
        bucket0To30: 600,
        bucket31To60: 400,
        bucket61To90: 0,
        bucket91To180: 0,
        bucket181Plus: 0,
        advanceBalance: 500,
        overdueAmount: 200
      },
      {
        customerId: 2,
        customerName: '客户B',
        totalArBalance: 2000,
        bucket0To30: 0,
        bucket31To60: 1000,
        bucket61To90: 500,
        bucket91To180: 300,
        bucket181Plus: 200,
        advanceBalance: 100,
        overdueAmount: 500
      }
    ]

    const totals = calculateAgingGrandTotals(list)
    expect(totals.totalAr).toBe(3000)
    expect(totals.totalAdvance).toBe(600)
    expect(totals.totalOverdue).toBe(700)
    expect(totals.bucket0To30).toBe(600)
    expect(totals.bucket31To60).toBe(1400)
    expect(totals.bucket61To90).toBe(500)
    expect(totals.bucket91To180).toBe(300)
    expect(totals.bucket181Plus).toBe(200)
  })
})
