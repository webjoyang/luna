import { describe, it, expect, vi } from 'vitest'

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
  maskBankAccount,
  calcReconRemainingLimit,
  getPaymentApplyActionFlags,
  parsePaymentErrorMessage,
  distributePaymentAllocation,
  validatePaymentRegister
} from '../model/payment'

import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import request from '@/config/axios'

describe('Payment Domain Model & Utilities (Pure Node)', () => {
  describe('formatMoney', () => {
    it('should format normal numbers to 2 decimal places with comma separation', () => {
      expect(formatMoney(1234567.89)).toBe('1,234,567.89')
      expect(formatMoney(100)).toBe('100.00')
      expect(formatMoney('5000.5')).toBe('5,000.50')
    })

    it('should handle zero, null, undefined, empty string gracefully', () => {
      expect(formatMoney(0)).toBe('0.00')
      expect(formatMoney(null)).toBe('0.00')
      expect(formatMoney(undefined)).toBe('0.00')
      expect(formatMoney('')).toBe('0.00')
      expect(formatMoney('invalid-num')).toBe('0.00')
    })
  })

  describe('maskBankAccount', () => {
    it('should mask standard bank card numbers keeping first 6 and last 4 digits', () => {
      expect(maskBankAccount('622202100011223344')).toBe('622202******3344')
      expect(maskBankAccount('1234567890123456')).toBe('123456******3456')
    })

    it('should handle short or empty accounts gracefully', () => {
      expect(maskBankAccount('')).toBe('—')
      expect(maskBankAccount(null)).toBe('—')
      expect(maskBankAccount('1234')).toBe('1234')
      expect(maskBankAccount('12345678')).toBe('12****78')
    })
  })

  describe('calcReconRemainingLimit', () => {
    it('should calculate correct remaining limit when total > applied', () => {
      expect(calcReconRemainingLimit(10000, 3000)).toBe(7000)
      expect(calcReconRemainingLimit(5000.55, 2000.22)).toBe(3000.33)
    })

    it('should return 0 when applied >= total or negative', () => {
      expect(calcReconRemainingLimit(10000, 10000)).toBe(0)
      expect(calcReconRemainingLimit(5000, 6000)).toBe(0)
      expect(calcReconRemainingLimit(0, 0)).toBe(0)
    })
  })

  describe('getPaymentApplyActionFlags', () => {
    it('should allow edit, submit, and delete for PENDING/DRAFT', () => {
      const pendingFlags = getPaymentApplyActionFlags('PENDING')
      expect(pendingFlags.canEdit).toBe(true)
      expect(pendingFlags.canSubmit).toBe(true)
      expect(pendingFlags.canDelete).toBe(true)
      expect(pendingFlags.canWithdraw).toBe(false)
      expect(pendingFlags.isApproved).toBe(false)

      const draftFlags = getPaymentApplyActionFlags('DRAFT')
      expect(draftFlags.canEdit).toBe(true)
      expect(draftFlags.canSubmit).toBe(true)
      expect(draftFlags.canDelete).toBe(true)
    })

    it('should allow edit and re-submit for REJECTED, but cannot delete', () => {
      const flags = getPaymentApplyActionFlags('REJECTED')
      expect(flags.canEdit).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canDelete).toBe(false)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.isRejected).toBe(true)
    })

    it('should allow withdraw for APPROVING, but cannot edit or delete', () => {
      const flags = getPaymentApplyActionFlags('APPROVING')
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canWithdraw).toBe(true)
      expect(flags.isApproving).toBe(true)
    })

    it('should mark APPROVED as isApproved with no edit/delete/withdraw actions', () => {
      const flags = getPaymentApplyActionFlags('APPROVED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.isApproved).toBe(true)
    })
  })

  describe('parsePaymentErrorMessage', () => {
    it('should parse 022_007 recon amount exceeded error', () => {
      expect(parsePaymentErrorMessage({ code: 1030022007 })).toContain(
        '累计申请金额超出对账确认金额'
      )
      expect(parsePaymentErrorMessage({ message: '022_007 exceed limit' })).toContain(
        '累计申请金额超出对账确认金额'
      )
    })

    it('should parse 023_001 status not allowed error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023001 })).toContain(
        '付款申请当前状态不允许该操作'
      )
    })

    it('should parse 023_002 recon not confirmed error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023002 })).toContain('关联合同对账单尚未确认')
    })

    it('should parse 023_003 allocate exceed error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023003 })).toContain(
        '核销分配金额超过该付款申请单剩余可核销额'
      )
    })

    it('should parse 023_004 account balance not enough error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023004 })).toContain('资金账户可用余额不足')
    })

    it('should parse 023_005 prepay balance not enough error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023005 })).toContain('供应商可用预付余额不足')
    })

    it('should parse 023_006 account not available error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023006 })).toContain('资金账户不存在或已停用')
    })

    it('should parse 023_007 amount invalid error', () => {
      expect(parsePaymentErrorMessage({ code: 1030023007 })).toContain('付款金额不合法')
    })

    it('should fallback to err.msg, message or default message', () => {
      expect(parsePaymentErrorMessage({ msg: '自定义错误说明' })).toBe('自定义错误说明')
      expect(parsePaymentErrorMessage({})).toBe('操作失败，请稍后重试')
      expect(parsePaymentErrorMessage(null)).toBe('操作失败')
    })
  })

  describe('distributePaymentAllocation', () => {
    it('should sequentially distribute funds to ordered applications', () => {
      const items = [
        { applyId: 1, remainAmount: 3000 },
        { applyId: 2, remainAmount: 5000 },
        { applyId: 3, remainAmount: 2000 }
      ]

      // Case 1: Partial payment of 4000:
      // applyId 1 gets 3000 (fully covered)
      // applyId 2 gets 1000 (partially covered)
      // applyId 3 gets 0
      const res1 = distributePaymentAllocation(items, 4000)
      expect(res1).toEqual([
        { applyId: 1, allocateAmount: 3000 },
        { applyId: 2, allocateAmount: 1000 },
        { applyId: 3, allocateAmount: 0 }
      ])

      // Case 2: Full payment of 10000:
      const res2 = distributePaymentAllocation(items, 10000)
      expect(res2).toEqual([
        { applyId: 1, allocateAmount: 3000 },
        { applyId: 2, allocateAmount: 5000 },
        { applyId: 3, allocateAmount: 2000 }
      ])

      // Case 3: Over-funds (15000) caps at remainAmount:
      const res3 = distributePaymentAllocation(items, 15000)
      expect(res3).toEqual([
        { applyId: 1, allocateAmount: 3000 },
        { applyId: 2, allocateAmount: 5000 },
        { applyId: 3, allocateAmount: 2000 }
      ])
    })
  })

  describe('validatePaymentRegister', () => {
    const baseParams = {
      supplierId: 101,
      accountId: 201,
      payAmount: 4000,
      prepayUsedAmount: 1000,
      prepayBalance: 2000,
      items: [
        { applyId: 1, applyNo: 'FK001', allocateAmount: 3000, remainAmount: 3000 },
        { applyId: 2, applyNo: 'FK002', allocateAmount: 2000, remainAmount: 5000 }
      ]
    }

    it('should validate successfully when parameters match exact allocation sums', () => {
      const res = validatePaymentRegister(baseParams)
      expect(res.valid).toBe(true)
      expect(res.error).toBeUndefined()
    })

    it('should fail if supplierId or accountId is missing', () => {
      expect(validatePaymentRegister({ ...baseParams, supplierId: undefined }).error).toContain(
        '请选择供应商'
      )
      expect(validatePaymentRegister({ ...baseParams, accountId: undefined }).error).toContain(
        '请选择付款资金账户'
      )
    })

    it('should fail if items list is empty', () => {
      expect(validatePaymentRegister({ ...baseParams, items: [] }).error).toContain(
        '请至少选择一笔'
      )
    })

    it('should fail if payAmount or prepayUsedAmount is negative', () => {
      expect(validatePaymentRegister({ ...baseParams, payAmount: -10 }).error).toContain(
        '不可为负数'
      )
      expect(validatePaymentRegister({ ...baseParams, prepayUsedAmount: -5 }).error).toContain(
        '不可为负数'
      )
    })

    it('should fail if total funds is 0', () => {
      expect(
        validatePaymentRegister({ ...baseParams, payAmount: 0, prepayUsedAmount: 0 }).error
      ).toContain('必须大于 0')
    })

    it('should fail if prepayUsedAmount exceeds prepayBalance', () => {
      const res = validatePaymentRegister({
        ...baseParams,
        prepayUsedAmount: 2500,
        prepayBalance: 2000,
        payAmount: 2500
      })
      expect(res.valid).toBe(false)
      expect(res.error).toContain('超出该供应商可用预付余额')
    })

    it('should fail if any item allocateAmount exceeds its remainAmount', () => {
      const res = validatePaymentRegister({
        ...baseParams,
        items: [
          { applyId: 1, applyNo: 'FK001', allocateAmount: 3500, remainAmount: 3000 },
          { applyId: 2, applyNo: 'FK002', allocateAmount: 1500, remainAmount: 5000 }
        ]
      })
      expect(res.valid).toBe(false)
      expect(res.error).toContain('超出剩余可核销额')
    })

    it('should fail if allocated sum does not equal pay + prepay', () => {
      const res = validatePaymentRegister({
        ...baseParams,
        payAmount: 4000,
        prepayUsedAmount: 1000, // sum is 5000
        items: [
          { applyId: 1, applyNo: 'FK001', allocateAmount: 3000, remainAmount: 3000 },
          { applyId: 2, applyNo: 'FK002', allocateAmount: 1000, remainAmount: 5000 } // allocated is 4000
        ]
      })
      expect(res.valid).toBe(false)
      expect(res.error).toContain('必须精确等于本次实付 + 预付抵扣合计')
    })
  })

  describe('Payment API Contracts', () => {
    it('getPaymentApplyPage should send GET to /lunaerp/finance/payment-apply/page', async () => {
      await PaymentApi.getPaymentApplyPage({ pageNo: 1, pageSize: 10, supplierId: 101 })
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/page',
        params: { pageNo: 1, pageSize: 10, supplierId: 101 }
      })
    })

    it('createPaymentApply should send POST to /lunaerp/finance/payment-apply/create', async () => {
      const payload = {
        applyType: 'RECON',
        reconId: 201,
        amount: 5000
      }
      await PaymentApi.createPaymentApply(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/create',
        data: payload
      })
    })

    it('submitPaymentApply should send POST to /lunaerp/finance/payment-apply/submit?id=1024', async () => {
      await PaymentApi.submitPaymentApply(1024)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/submit?id=1024'
      })
    })

    it('cancelApprovePaymentApply should send POST to /lunaerp/finance/payment-apply/cancel-approve?id=1024', async () => {
      await PaymentApi.cancelApprovePaymentApply(1024)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/cancel-approve?id=1024'
      })
    })

    it('getPrepayBalance should send GET to /lunaerp/finance/payment-apply/prepay-balance?supplierId=101', async () => {
      await PaymentApi.getPrepayBalance(101)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/prepay-balance?supplierId=101'
      })
    })

    it('registerPayment should send POST to /lunaerp/finance/payment/register', async () => {
      const payload = {
        supplierId: 101,
        accountId: 201,
        payAmount: 4000,
        prepayUsedAmount: 1000,
        items: [{ applyId: 1, allocateAmount: 5000 }]
      }
      await PaymentApi.registerPayment(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/register',
        data: payload
      })
    })

    it('getPaymentPage should send GET to /lunaerp/finance/payment/page', async () => {
      await PaymentApi.getPaymentPage({ pageNo: 1, pageSize: 20 })
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/page',
        params: { pageNo: 1, pageSize: 20 }
      })
    })

    it('getFinanceAccountSimpleList should send GET to /lunaerp/finance/account/simple-list', async () => {
      await PaymentApi.getFinanceAccountSimpleList()
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/account/simple-list'
      })
    })
  })
})
