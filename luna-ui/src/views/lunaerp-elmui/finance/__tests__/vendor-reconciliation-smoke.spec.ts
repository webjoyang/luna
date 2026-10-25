import { describe, it, expect, vi } from 'vitest'

// AGENTS.md 规约：纯 Node 架构下被测模块级联依赖 axios 时必须顶层 mock，避免触发 createWebHistory window is not defined
vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import {
  calcItemBillQty,
  calcItemLineAmount,
  calcTotalAmount,
  validateReconDraft,
  formatMoney
} from '../supplier-recon/model/supplier-recon'

import {
  calculatePrintSubtotals,
  formatLineTypePrintLabel
} from '../supplier-recon/components/print/utils'

import {
  calcReconRemainingLimit,
  calcApplyRemain,
  getPaymentApplyActionFlags,
  distributePaymentAllocation,
  validatePaymentRegister
} from '../payment/model/payment'

describe('Finance Vendor Reconciliation Full Chain Frontend Smoke Test', () => {
  it('should run end-to-end frontend business flow from candidate preview to payment register', async () => {
    // ----------------------------------------------------
    // Step 1: 取数候选与单据行装配计算
    // ----------------------------------------------------
    const processItem = {
      lineType: 'PROCESS',
      qtyKg: 1200.0,
      qtyM: 2000.0,
      pricingBasis: 'METER',
      unitPrice: 5.5
    }
    expect(calcItemBillQty(processItem)).toBe(2000.0)
    expect(calcItemLineAmount(processItem)).toBe(11000.0)

    // 计价基准动态切换至 KG
    processItem.pricingBasis = 'KG'
    expect(calcItemBillQty(processItem)).toBe(1200.0)
    processItem.pricingBasis = 'METER' // 恢复 METER

    const purchaseItem = {
      lineType: 'PURCHASE',
      billQty: 500.0,
      unitPrice: 20.0
    }
    expect(calcItemBillQty(purchaseItem)).toBe(500.0)
    expect(calcItemLineAmount(purchaseItem)).toBe(10000.0)

    const adjustItem = {
      lineType: 'ADJUST',
      lineAmount: -1000.0,
      adjustReason: '布面油污扣款'
    }
    expect(calcItemBillQty(adjustItem)).toBe(1)
    expect(calcItemLineAmount(adjustItem)).toBe(-1000.0)

    // ----------------------------------------------------
    // Step 2: 草稿校验与总额计算
    // ----------------------------------------------------
    const items = [processItem, purchaseItem, adjustItem]
    const draftPayload = {
      supplierId: 101,
      periodStart: '2026-09-01',
      periodEnd: '2026-09-30',
      items
    }
    const draftValidation = validateReconDraft(draftPayload)
    expect(draftValidation.valid).toBe(true)

    // 调整项非法情况校验：金额为 0 或未填原因
    expect(
      validateReconDraft({
        supplierId: 101,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: [{ lineType: 'ADJUST', lineAmount: 0, adjustReason: '扣款' }]
      }).valid
    ).toBe(false)
    expect(
      validateReconDraft({
        supplierId: 101,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: [{ lineType: 'ADJUST', lineAmount: -500, adjustReason: '   ' }]
      }).valid
    ).toBe(false)

    // 服务端重算一致性：11000 + 10000 - 1000 = 20000.00
    const totalAmount = calcTotalAmount(items)
    expect(totalAmount).toBe(20000.0)
    expect(formatMoney(totalAmount)).toBe('20,000.00')

    // ----------------------------------------------------
    // Step 3: A4 打印汇总与标签格式化
    // ----------------------------------------------------
    expect(formatLineTypePrintLabel('PROCESS')).toBe('加工费')
    expect(formatLineTypePrintLabel('PURCHASE')).toBe('采购货款')
    expect(formatLineTypePrintLabel('ADJUST')).toBe('调整项')

    const printItems = [
      { ...processItem, lineAmount: calcItemLineAmount(processItem) },
      { ...purchaseItem, lineAmount: calcItemLineAmount(purchaseItem) },
      adjustItem
    ]
    const printSubtotals = calculatePrintSubtotals(printItems)
    expect(printSubtotals.processTotal).toBe(11000.0)
    expect(printSubtotals.purchaseTotal).toBe(10000.0)
    expect(printSubtotals.adjustTotal).toBe(-1000.0)
    expect(printSubtotals.grandTotal).toBe(20000.0)

    // ----------------------------------------------------
    // Step 4: 确认对账单后生成对账付款申请 (RECON)
    // ----------------------------------------------------
    // 确认金额 20000，已申请 0 -> 可申上限 20000
    expect(calcReconRemainingLimit(20000, 0)).toBe(20000)

    // 本次申请 15000，已申请变为 15000 -> 剩余可申 5000
    expect(calcReconRemainingLimit(20000, 15000)).toBe(5000)

    // ----------------------------------------------------
    // Step 5: 付款申请五态动作按钮指示器
    // ----------------------------------------------------
    const pendingFlags = getPaymentApplyActionFlags('PENDING')
    expect(pendingFlags.canEdit).toBe(true)
    expect(pendingFlags.canSubmit).toBe(true)
    expect(pendingFlags.canDelete).toBe(true)

    const approvingFlags = getPaymentApplyActionFlags('APPROVING')
    expect(approvingFlags.canWithdraw).toBe(true)
    expect(approvingFlags.canEdit).toBe(false)

    const rejectedFlags = getPaymentApplyActionFlags('REJECTED')
    expect(rejectedFlags.canEdit).toBe(true)
    expect(rejectedFlags.canSubmit).toBe(true)

    const approvedFlags = getPaymentApplyActionFlags('APPROVED')
    expect(approvedFlags.isApproved).toBe(true)
    expect(approvedFlags.canEdit).toBe(false)

    // ----------------------------------------------------
    // Step 6: 顺序分配算法与出纳付款登记校验
    // ----------------------------------------------------
    // 场景：两笔 APPROVED 申请
    // 申请 1 (PREPAY): 4000 申请额，已核销 0
    // 申请 2 (RECON): 15000 申请额，已核销 0
    const applyRows = [
      { applyId: 1, applyNo: 'FK-PREPAY-001', remainAmount: 4000 },
      { applyId: 2, applyNo: 'FK-RECON-002', remainAmount: 15000 }
    ]

    // 6.1 出纳登记 PREPAY 申请（实付 4000）
    const prepayAlloc = distributePaymentAllocation([applyRows[0]], 4000)
    expect(prepayAlloc).toEqual([{ applyId: 1, allocateAmount: 4000 }])

    const prepayRegisterValidation = validatePaymentRegister({
      supplierId: 101,
      accountId: 201,
      payAmount: 4000,
      prepayUsedAmount: 0,
      prepayBalance: 0,
      items: [{ applyId: 1, applyNo: 'FK-PREPAY-001', allocateAmount: 4000, remainAmount: 4000 }]
    })
    expect(prepayRegisterValidation.valid).toBe(true)

    // 6.2 PREPAY 登记后形成 4000 预付余额，出纳登记 RECON 申请：
    // 实付 8000 + 预付抵扣 2000 = 合计分配 10000（部分核销，剩余挂应付 5000）
    const reconAlloc = distributePaymentAllocation([applyRows[1]], 10000)
    expect(reconAlloc).toEqual([{ applyId: 2, allocateAmount: 10000 }])

    const reconRegisterValidation = validatePaymentRegister({
      supplierId: 101,
      accountId: 201,
      payAmount: 8000,
      prepayUsedAmount: 2000,
      prepayBalance: 4000, // 预付池足够抵扣 2000
      items: [{ applyId: 2, applyNo: 'FK-RECON-002', allocateAmount: 10000, remainAmount: 15000 }]
    })
    expect(reconRegisterValidation.valid).toBe(true)

    // 异常防御：预付抵扣超过预付池 (2000 > 1000) 拦截
    const overPrepayValidation = validatePaymentRegister({
      supplierId: 101,
      accountId: 201,
      payAmount: 8000,
      prepayUsedAmount: 2000,
      prepayBalance: 1000,
      items: [{ applyId: 2, applyNo: 'FK-RECON-002', allocateAmount: 10000, remainAmount: 15000 }]
    })
    expect(overPrepayValidation.valid).toBe(false)
    expect(overPrepayValidation.error).toContain('超出该供应商可用预付余额')

    // ----------------------------------------------------
    // Step 7: 聚合口径核算
    // ----------------------------------------------------
    // RECON 申请已核销 10000，剩余应为 5000
    expect(calcApplyRemain({ amount: 15000, allocatedAmount: 10000 })).toBe(5000)

    // 预付池两条 SUM：4000 (已核销 PREPAY) - 2000 (已抵扣 prepay_used) = 2000
    const prepayAllocatedTotal = 4000
    const prepayUsedTotal = 2000
    expect(prepayAllocatedTotal - prepayUsedTotal).toBe(2000)
  })
})
