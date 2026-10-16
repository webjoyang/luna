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
  calcApplyRemain,
  getPaymentApplyActionFlags,
  parsePaymentErrorMessage,
  distributePaymentAllocation,
  validatePaymentRegister,
  getReceiptActionFlags,
  calcArEntryUnallocated,
  distributeReceiptFifo,
  validateReceiptForm,
  getApprovalStatusText,
  getApprovalStatusTagType
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

  describe('calcApplyRemain', () => {
    it('should return remainAmount directly if present', () => {
      expect(calcApplyRemain({ amount: 1000, allocatedAmount: 200, remainAmount: 800 })).toBe(800)
      expect(calcApplyRemain({ remainAmount: '350.50' })).toBe(350.5)
    })

    it('should calculate amount - allocatedAmount when remainAmount is missing', () => {
      expect(calcApplyRemain({ amount: 5000, allocatedAmount: 2000 })).toBe(3000)
      expect(calcApplyRemain({ amount: 5000, allocatedAmount: 5000 })).toBe(0)
      expect(calcApplyRemain({ amount: 5000, allocatedAmount: 6000 })).toBe(0)
    })

    it('should handle null/undefined row gracefully', () => {
      expect(calcApplyRemain(null)).toBe(0)
      expect(calcApplyRemain(undefined)).toBe(0)
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

    // Receipt API Tests
    it('createReceipt should send POST to /lunaerp/finance/payment/create', async () => {
      const payload = {
        customerId: 1,
        accountId: 2,
        currency: 'RMB',
        receiptType: 'RECEIPT',
        payAmount: 1000
      }
      await PaymentApi.createReceipt(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/create',
        data: payload
      })
    })

    it('updateReceipt should send PUT to /lunaerp/finance/payment/update', async () => {
      const payload = { id: 101, payAmount: 2000 }
      await PaymentApi.updateReceipt(payload)
      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/update',
        data: payload
      })
    })

    it('deleteReceipt should send DELETE to /lunaerp/finance/payment/delete?id=101', async () => {
      await PaymentApi.deleteReceipt(101)
      expect(request.delete).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/delete?id=101'
      })
    })

    it('submitReceipt should send PUT to /lunaerp/finance/payment/submit?id=101', async () => {
      await PaymentApi.submitReceipt(101)
      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/submit?id=101'
      })
    })

    it('allocatePreview should send POST to /lunaerp/finance/payment/allocate-preview', async () => {
      const payload = { customerId: 1, currency: 'RMB', payAmount: 5000 }
      await PaymentApi.allocatePreview(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/allocate-preview',
        data: payload
      })
    })

    it('getArBalanceList should send GET to /lunaerp/finance/payment/ar-balance', async () => {
      await PaymentApi.getArBalanceList(1, 'RMB')
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/ar-balance?customerId=1&currency=RMB'
      })
    })

    it('getReceipt should send GET to /lunaerp/finance/payment/receipt-get?id=101', async () => {
      await PaymentApi.getReceipt(101)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/receipt-get?id=101'
      })
    })

    it('getReceiptPage should send GET to /lunaerp/finance/payment/receipt-page', async () => {
      await PaymentApi.getReceiptPage({ pageNo: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/receipt-page',
        params: { pageNo: 1, pageSize: 10 }
      })
    })
  })

  describe('Receipt Domain Model & Utilities', () => {
    describe('getReceiptActionFlags', () => {
      it('should allow edit, submit, and delete for PENDING/DRAFT', () => {
        const flags = getReceiptActionFlags('PENDING')
        expect(flags.canEdit).toBe(true)
        expect(flags.canSubmit).toBe(true)
        expect(flags.canDelete).toBe(true)
        expect(flags.isApproved).toBe(false)
      })

      it('should prevent edit and submit for REJECTED (PENDING only per spec)', () => {
        const flags = getReceiptActionFlags('REJECTED')
        expect(flags.canEdit).toBe(false)
        expect(flags.canSubmit).toBe(false)
        expect(flags.canDelete).toBe(false)
        expect(flags.isRejected).toBe(true)
      })

      it('should prevent edit/delete for APPROVING', () => {
        const flags = getReceiptActionFlags('APPROVING')
        expect(flags.canEdit).toBe(false)
        expect(flags.canSubmit).toBe(false)
        expect(flags.canDelete).toBe(false)
        expect(flags.isApproving).toBe(true)
      })

      it('should mark APPROVED or COMPLETED as immutable', () => {
        const flags = getReceiptActionFlags('APPROVED', 'COMPLETED')
        expect(flags.canEdit).toBe(false)
        expect(flags.canSubmit).toBe(false)
        expect(flags.canDelete).toBe(false)
        expect(flags.isApproved).toBe(true)
      })
    })

    describe('calcArEntryUnallocated', () => {
      it('should return unallocatedAmount directly if present', () => {
        expect(calcArEntryUnallocated({ unallocatedAmount: 1500 })).toBe(1500)
      })

      it('should calculate originalAmount - allocatedAmount when unallocatedAmount missing', () => {
        expect(calcArEntryUnallocated({ originalAmount: 5000, allocatedAmount: 2000 })).toBe(3000)
        expect(calcArEntryUnallocated({ originalAmount: 5000, allocatedAmount: 5000 })).toBe(0)
      })

      it('should handle null/undefined gracefully', () => {
        expect(calcArEntryUnallocated(null)).toBe(0)
      })
    })

    describe('distributeReceiptFifo', () => {
      it('should distribute receipt funds by bizDate ascending (FIFO)', () => {
        const entries = [
          { entryId: 103, bizDate: '2026-10-10', unallocatedAmount: 3000 },
          { entryId: 101, bizDate: '2026-10-01', unallocatedAmount: 1000 },
          { entryId: 102, bizDate: '2026-10-05', unallocatedAmount: 2000 }
        ]

        const res = distributeReceiptFifo(entries, 2500)
        expect(res).toEqual([
          { entryId: 101, allocateAmount: 1000 },
          { entryId: 102, allocateAmount: 1500 },
          { entryId: 103, allocateAmount: 0 }
        ])
      })
    })

    describe('validateReceiptForm', () => {
      it('should reject missing customer, account, currency or non-positive payAmount', () => {
        expect(validateReceiptForm({ payAmount: 100 }).valid).toBe(false)
        expect(validateReceiptForm({ customerId: 1, payAmount: 100 }).valid).toBe(false)
        expect(validateReceiptForm({ customerId: 1, accountId: 1, payAmount: 100 }).valid).toBe(
          false
        )
        expect(
          validateReceiptForm({ customerId: 1, accountId: 1, currency: 'RMB', payAmount: 0 }).valid
        ).toBe(false)
      })

      it('should reject if any item allocateAmount exceeds unallocatedAmount', () => {
        const res = validateReceiptForm({
          customerId: 1,
          accountId: 1,
          currency: 'RMB',
          payAmount: 5000,
          items: [{ entryId: 101, allocateAmount: 2500, unallocatedAmount: 2000 }]
        })
        expect(res.valid).toBe(false)
        expect(res.error).toContain('超出未核销余额')
      })

      it('should reject if total allocated exceeds payAmount', () => {
        const res = validateReceiptForm({
          customerId: 1,
          accountId: 1,
          currency: 'RMB',
          payAmount: 1000,
          items: [
            { entryId: 101, allocateAmount: 800, unallocatedAmount: 1000 },
            { entryId: 102, allocateAmount: 500, unallocatedAmount: 1000 }
          ]
        })
        expect(res.valid).toBe(false)
        expect(res.error).toContain('不得超出收款金额')
      })

      it('should accept valid receipt with items', () => {
        const res = validateReceiptForm({
          customerId: 1,
          accountId: 1,
          currency: 'RMB',
          payAmount: 3000,
          items: [
            { entryId: 101, allocateAmount: 1000, unallocatedAmount: 2000 },
            { entryId: 102, allocateAmount: 2000, unallocatedAmount: 2000 }
          ]
        })
        expect(res.valid).toBe(true)
      })
    })

    describe('parsePaymentErrorMessage for Receipt', () => {
      it('should parse receipt error codes correctly', () => {
        expect(parsePaymentErrorMessage({ code: 1030023100 })).toContain('收款单不存在')
        expect(parsePaymentErrorMessage({ code: 1030023101 })).toContain('收款单当前状态不允许')
        expect(parsePaymentErrorMessage({ code: 1030023102 })).toContain(
          '所选待核销分录与当前收款客户不匹配'
        )
        expect(parsePaymentErrorMessage({ code: 1030023103 })).toContain('币种不一致')
        expect(parsePaymentErrorMessage({ code: 1030023104 })).toContain(
          '核销分配金额超出该分录未核销余额'
        )
        expect(parsePaymentErrorMessage({ code: 1030023105 })).toContain('收款单过账冲突')
        expect(parsePaymentErrorMessage({ code: 1030023106 })).toContain('客户预收余额不足')
        expect(parsePaymentErrorMessage({ code: 1030023107 })).toContain('该收款单不允许红冲')
      })
    })

    describe('getApprovalStatusText & getApprovalStatusTagType', () => {
      it('should map status codes to labels and tag types correctly', () => {
        expect(getApprovalStatusText('PENDING')).toBe('草稿待提交')
        expect(getApprovalStatusTagType('PENDING')).toBe('info')

        expect(getApprovalStatusText('APPROVING')).toBe('审批中')
        expect(getApprovalStatusTagType('APPROVING')).toBe('warning')

        expect(getApprovalStatusText('APPROVED')).toBe('审批通过')
        expect(getApprovalStatusTagType('APPROVED')).toBe('success')

        expect(getApprovalStatusText('REJECTED')).toBe('已驳回')
        expect(getApprovalStatusTagType('REJECTED')).toBe('danger')
      })
    })
  })
})
