import { describe, expect, it } from 'vitest'
import type {
  SampleAnalysisVO,
  SampleAnalysisPaymentStatsRespVO,
  SampleAnalysisPaymentReqVO
} from '@/api/lunaerp-elmui/sample/sample-analysis'

/**
 * 模拟 Issue 02 的统计计算与付款标记前端状态行为
 */
function computeClientSideStats(items: Array<Partial<SampleAnalysisVO>>): SampleAnalysisPaymentStatsRespVO {
  let unpaidAmount = 0
  let paidAmount = 0
  for (const item of items) {
    const price = item.unitPrice ?? 30
    if (item.isPaid) {
      paidAmount += price
    } else {
      unpaidAmount += price
    }
  }
  return {
    unpaidAmount: Number(unpaidAmount.toFixed(2)),
    paidAmount: Number(paidAmount.toFixed(2)),
    totalCount: items.length
  }
}

function filterByPaymentState(
  items: Array<Partial<SampleAnalysisVO>>,
  isPaid?: boolean
): Array<Partial<SampleAnalysisVO>> {
  if (isPaid === undefined) {
    return items
  }
  return items.filter((item) => item.isPaid === isPaid)
}

function applyBatchPayment(
  items: Array<SampleAnalysisVO>,
  req: SampleAnalysisPaymentReqVO
): Array<SampleAnalysisVO> {
  const idSet = new Set(req.ids)
  return items.map((item) => {
    if (item.id && idSet.has(item.id)) {
      return { ...item, isPaid: req.isPaid }
    }
    return item
  })
}

describe('Issue 02: 月结收费闭环业务行为测试', () => {
  it('默认字段：新建时缺省 unitPrice=30, isPaid=false', () => {
    const defaultData: Partial<SampleAnalysisVO> = {
      productName: '测试布',
      unitPrice: 30,
      isPaid: false
    }
    expect(defaultData.unitPrice).toBe(30)
    expect(defaultData.isPaid).toBe(false)
  })

  it('三态筛选逻辑：全部包含所有，未付款仅列 isPaid=false，已付款仅列 isPaid=true', () => {
    const dataset: Array<Partial<SampleAnalysisVO>> = [
      { id: 1, productName: 'A', isPaid: false, unitPrice: 30 },
      { id: 2, productName: 'B', isPaid: true, unitPrice: 40 },
      { id: 3, productName: 'C', isPaid: false, unitPrice: 50 },
      { id: 4, productName: 'D', isPaid: true, unitPrice: 30 }
    ]

    expect(filterByPaymentState(dataset, undefined)).toHaveLength(4)
    const unpaidList = filterByPaymentState(dataset, false)
    expect(unpaidList).toHaveLength(2)
    expect(unpaidList.map((i) => i.id)).toEqual([1, 3])

    const paidList = filterByPaymentState(dataset, true)
    expect(paidList).toHaveLength(2)
    expect(paidList.map((i) => i.id)).toEqual([2, 4])
  })

  it('统计三值口径：按 unitPrice 累加，对外报价 quotedPrice 绝不参与', () => {
    const dataset: Array<Partial<SampleAnalysisVO>> = [
      { id: 1, isPaid: false, unitPrice: 30, quotedPrice: 100 },
      { id: 2, isPaid: true, unitPrice: 40, quotedPrice: 200 },
      { id: 3, isPaid: false, unitPrice: 50, quotedPrice: 300 }
    ]

    const stats = computeClientSideStats(dataset)
    expect(stats.unpaidAmount).toBe(80) // 30 + 50
    expect(stats.paidAmount).toBe(40)   // 40
    expect(stats.totalCount).toBe(3)
  })

  it('批量/单条付款标记：仅影响目标 id 列表中的记录，其余保持不变', () => {
    const dataset: Array<SampleAnalysisVO> = [
      { id: 1, productName: 'A', isPaid: false, unitPrice: 30 },
      { id: 2, productName: 'B', isPaid: false, unitPrice: 30 },
      { id: 3, productName: 'C', isPaid: true, unitPrice: 30 }
    ]

    // 批量标记 1 和 2 为已付款
    const batchUpdated = applyBatchPayment(dataset, { ids: [1, 2], isPaid: true })
    expect(batchUpdated.find((i) => i.id === 1)?.isPaid).toBe(true)
    expect(batchUpdated.find((i) => i.id === 2)?.isPaid).toBe(true)
    expect(batchUpdated.find((i) => i.id === 3)?.isPaid).toBe(true)

    // 单条标记 1 为未付款
    const singleUpdated = applyBatchPayment(batchUpdated, { ids: [1], isPaid: false })
    expect(singleUpdated.find((i) => i.id === 1)?.isPaid).toBe(false)
    expect(singleUpdated.find((i) => i.id === 2)?.isPaid).toBe(true)
    expect(singleUpdated.find((i) => i.id === 3)?.isPaid).toBe(true)
  })
})
