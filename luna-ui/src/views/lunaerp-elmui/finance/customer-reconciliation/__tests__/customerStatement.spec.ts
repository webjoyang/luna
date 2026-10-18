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
  getDefaultPreviousMonthRange,
  validateStatementPeriod,
  calculateStatementSummary,
  getCustomerStatementActionFlags,
  validateConfirmOnBehalfForm,
  validateRejectForm,
  formatMoney,
  formatSignStatusTag,
  formatStatementStatusTag,
  parseCustomerStatementError,
  CUSTOMER_STATEMENT_ERROR_CODES
} from '../model/customerStatement'
import * as CustomerStatementApi from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import request from '@/config/axios'

describe('Customer Statement Domain Model & Pure Functions (Node Test)', () => {
  describe('Default Date Range Calculation', () => {
    it('should compute previous month 1st to last day for October 2026', () => {
      const octDate = new Date(2026, 9, 18) // Oct 18, 2026
      const [start, end] = getDefaultPreviousMonthRange(octDate)
      expect(start).toBe('2026-09-01')
      expect(end).toBe('2026-09-30')
    })

    it('should handle January boundary across year correctly', () => {
      const janDate = new Date(2026, 0, 10) // Jan 10, 2026
      const [start, end] = getDefaultPreviousMonthRange(janDate)
      expect(start).toBe('2025-12-01')
      expect(end).toBe('2025-12-31')
    })

    it('should handle leap year February correctly', () => {
      const marDate = new Date(2024, 2, 15) // Mar 15, 2024 (leap year)
      const [start, end] = getDefaultPreviousMonthRange(marDate)
      expect(start).toBe('2024-02-01')
      expect(end).toBe('2024-02-29')
    })

    it('should handle non-leap year February correctly', () => {
      const marDate = new Date(2025, 2, 1) // Mar 1, 2025
      const [start, end] = getDefaultPreviousMonthRange(marDate)
      expect(start).toBe('2025-02-01')
      expect(end).toBe('2025-02-28')
    })
  })

  describe('Period Validation', () => {
    it('should pass when start is before or equal to end', () => {
      expect(validateStatementPeriod('2026-09-01', '2026-09-30').valid).toBe(true)
      expect(validateStatementPeriod('2026-09-01', '2026-09-01').valid).toBe(true)
    })

    it('should fail when start is after end', () => {
      const res = validateStatementPeriod('2026-10-01', '2026-09-30')
      expect(res.valid).toBe(false)
      expect(res.message).toContain('不能晚于')
    })

    it('should fail when dates are missing', () => {
      expect(validateStatementPeriod(undefined, '2026-09-30').valid).toBe(false)
      expect(validateStatementPeriod('2026-09-01', '').valid).toBe(false)
    })
  })

  describe('Statement Summary Calculation', () => {
    it('should correctly calculate debit, credit, and closing balance with rounding', () => {
      const items = [
        { debitAmount: 100.15, creditAmount: 0 },
        { debitAmount: 200.25, creditAmount: 50.1 },
        { debitAmount: 0, creditAmount: 120.3 }
      ]
      const summary = calculateStatementSummary(500.0, items)
      expect(summary.periodDebit).toBe(300.4)
      expect(summary.periodCredit).toBe(170.4)
      // closing = 500.0 + 300.4 - 170.4 = 630.0
      expect(summary.closingBalance).toBe(630.0)
    })

    it('should handle empty items list and default opening balance', () => {
      const summary = calculateStatementSummary()
      expect(summary.periodDebit).toBe(0)
      expect(summary.periodCredit).toBe(0)
      expect(summary.closingBalance).toBe(0)
    })
  })

  describe('Action Flags by Status Matrix', () => {
    it('DRAFT should allow edit, delete, addEntry, send, void, print', () => {
      const flags = getCustomerStatementActionFlags('DRAFT')
      expect(flags.canEdit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canAddEntry).toBe(true)
      expect(flags.canSend).toBe(true)
      expect(flags.canVoid).toBe(true)
      expect(flags.canConfirmOnBehalf).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.canRejectReply).toBe(false)
      expect(flags.canPrint).toBe(true)
    })

    it('SENT should allow confirmOnBehalf, reject, void, print', () => {
      const flags = getCustomerStatementActionFlags('SENT')
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canAddEntry).toBe(false)
      expect(flags.canSend).toBe(false)
      expect(flags.canVoid).toBe(true)
      expect(flags.canConfirmOnBehalf).toBe(true)
      expect(flags.canReject).toBe(true)
      expect(flags.canRejectReply).toBe(false)
      expect(flags.canPrint).toBe(true)
    })

    it('CONFIRMED terminal state should only allow print, forbid void/edit/send', () => {
      const flags = getCustomerStatementActionFlags('CONFIRMED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canAddEntry).toBe(false)
      expect(flags.canSend).toBe(false)
      expect(flags.canVoid).toBe(false)
      expect(flags.canConfirmOnBehalf).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.canRejectReply).toBe(false)
      expect(flags.canPrint).toBe(true)
    })

    it('REJECTED should allow rejectReply, void, print', () => {
      const flags = getCustomerStatementActionFlags('REJECTED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canAddEntry).toBe(false)
      expect(flags.canSend).toBe(false)
      expect(flags.canVoid).toBe(true)
      expect(flags.canConfirmOnBehalf).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.canRejectReply).toBe(true)
      expect(flags.canPrint).toBe(true)
    })

    it('VOID terminal state should forbid all operations', () => {
      const flags = getCustomerStatementActionFlags('VOID')
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canAddEntry).toBe(false)
      expect(flags.canSend).toBe(false)
      expect(flags.canVoid).toBe(false)
      expect(flags.canConfirmOnBehalf).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.canRejectReply).toBe(false)
      expect(flags.canPrint).toBe(false)
    })
  })

  describe('Form Validations', () => {
    it('validateConfirmOnBehalfForm should require confirmMethod', () => {
      expect(validateConfirmOnBehalfForm('微信').valid).toBe(true)
      expect(validateConfirmOnBehalfForm('').valid).toBe(false)
      expect(validateConfirmOnBehalfForm('   ').valid).toBe(false)
      expect(validateConfirmOnBehalfForm(undefined).valid).toBe(false)
    })

    it('validateRejectForm should require rejectReason', () => {
      expect(validateRejectForm('金额有误').valid).toBe(true)
      expect(validateRejectForm('').valid).toBe(false)
      expect(validateRejectForm('   ').valid).toBe(false)
      expect(validateRejectForm(undefined).valid).toBe(false)
    })
  })

  describe('Format and Error Mapping Helpers', () => {
    it('formatMoney should format numbers with commas and decimals', () => {
      expect(formatMoney(1234567.89)).toBe('1,234,567.89')
      expect(formatMoney(0)).toBe('0.00')
      expect(formatMoney(null)).toBe('0.00')
      expect(formatMoney(undefined)).toBe('0.00')
    })

    it('formatSignStatusTag should map correctly', () => {
      expect(formatSignStatusTag('SIGNED').label).toBe('已签收')
      expect(formatSignStatusTag('UNSIGNED').label).toBe('未签收')
      expect(formatSignStatusTag('REJECTED').label).toBe('拒签')
      expect(formatSignStatusTag('UNKNOWN').label).toBe('UNKNOWN')
    })

    it('formatStatementStatusTag should map correctly', () => {
      expect(formatStatementStatusTag('DRAFT').label).toBe('草稿')
      expect(formatStatementStatusTag('SENT').label).toBe('已发出')
      expect(formatStatementStatusTag('SENT').type).toBe('primary')
      expect(formatStatementStatusTag('CONFIRMED').label).toBe('已确认')
      expect(formatStatementStatusTag('CONFIRMED').type).toBe('success')
      expect(formatStatementStatusTag('REJECTED').label).toBe('已驳回')
      expect(formatStatementStatusTag('REJECTED').type).toBe('danger')
      expect(formatStatementStatusTag('VOID').label).toBe('已作废')
      expect(formatStatementStatusTag('VOID').type).toBe('info')
    })

    it('parseCustomerStatementError should map code to message', () => {
      expect(
        parseCustomerStatementError(
          CUSTOMER_STATEMENT_ERROR_CODES.STATUS_NOT_DRAFT
        )
      ).toContain('草稿（DRAFT）')
      expect(
        parseCustomerStatementError(
          CUSTOMER_STATEMENT_ERROR_CODES.CURRENCY_MIXED
        )
      ).toContain('多币种')
      expect(
        parseCustomerStatementError(
          CUSTOMER_STATEMENT_ERROR_CODES.ENTRY_OCCUPIED
        )
      ).toContain('占用')
      expect(parseCustomerStatementError(999999)).toBe('对账单操作失败，请重试')
    })
  })

  describe('API Client Endpoints', () => {
    it('should invoke prefetchLines with GET', async () => {
      const mockResp = { customerId: 101, openingBalance: 100, lines: [] }
      ;(request.get as any).mockResolvedValueOnce(mockResp)

      const res = await CustomerStatementApi.prefetchLines({
        customerId: 101,
        currency: 'CNY',
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30'
      })

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/prefetch-lines',
        params: {
          customerId: 101,
          currency: 'CNY',
          periodStart: '2026-09-01',
          periodEnd: '2026-09-30'
        }
      })
      expect(res).toEqual(mockResp)
    })

    it('should invoke createCustomerStatement with POST', async () => {
      ;(request.post as any).mockResolvedValueOnce(200)

      const res = await CustomerStatementApi.createCustomerStatement({
        customerId: 101,
        currency: 'CNY',
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30'
      })

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/create',
        data: {
          customerId: 101,
          currency: 'CNY',
          periodStart: '2026-09-01',
          periodEnd: '2026-09-30'
        }
      })
      expect(res).toBe(200)
    })

    it('should invoke updateCustomerStatement with PUT', async () => {
      ;(request.put as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.updateCustomerStatement({
        id: 200,
        remark: '修改备注',
        deletedItemIds: [1, 2]
      })

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/update',
        data: {
          id: 200,
          remark: '修改备注',
          deletedItemIds: [1, 2]
        }
      })
      expect(res).toBe(true)
    })

    it('should invoke deleteCustomerStatement with DELETE', async () => {
      ;(request.delete as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.deleteCustomerStatement(200)

      expect(request.delete).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/delete',
        params: { id: 200 }
      })
      expect(res).toBe(true)
    })

    it('should invoke addStatementEntry with POST', async () => {
      ;(request.post as any).mockResolvedValueOnce(999)

      const res = await CustomerStatementApi.addStatementEntry({
        statementId: 200,
        ledgerEntryId: 555,
        remark: '补记'
      })

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/add-entry',
        data: {
          statementId: 200,
          ledgerEntryId: 555,
          remark: '补记'
        }
      })
      expect(res).toBe(999)
    })

    it('should invoke sendCustomerStatement with PUT', async () => {
      ;(request.put as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.sendCustomerStatement(200)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/send',
        params: { id: 200 }
      })
      expect(res).toBe(true)
    })

    it('should invoke voidCustomerStatement with PUT', async () => {
      ;(request.put as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.voidCustomerStatement(200)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/void',
        params: { id: 200 }
      })
      expect(res).toBe(true)
    })

    it('should invoke confirmOnBehalf with PUT', async () => {
      ;(request.put as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.confirmOnBehalf({
        id: 200,
        confirmMethod: '微信',
        remark: '李总确认'
      })

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/confirm-on-behalf',
        data: {
          id: 200,
          confirmMethod: '微信',
          remark: '李总确认'
        }
      })
      expect(res).toBe(true)
    })

    it('should invoke rejectCustomerStatement with PUT', async () => {
      ;(request.put as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.rejectCustomerStatement({
        id: 200,
        rejectReason: '数量有异议'
      })

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/reject',
        data: {
          id: 200,
          rejectReason: '数量有异议'
        }
      })
      expect(res).toBe(true)
    })

    it('should invoke rejectReplyCustomerStatement with PUT', async () => {
      ;(request.put as any).mockResolvedValueOnce(true)

      const res = await CustomerStatementApi.rejectReplyCustomerStatement({
        id: 200,
        replyRemark: '已按要求核减'
      })

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/reject-reply',
        data: {
          id: 200,
          replyRemark: '已按要求核减'
        }
      })
      expect(res).toBe(true)
    })

    it('should invoke getCustomerStatementPrintData with GET', async () => {
      const mockPrintData = { id: 200, statementNo: 'CS20260001', items: [] }
      ;(request.get as any).mockResolvedValueOnce(mockPrintData)

      const res = await CustomerStatementApi.getCustomerStatementPrintData(200)

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/customerrecon/statement/print-data',
        params: { id: 200 }
      })
      expect(res).toEqual(mockPrintData)
    })
  })
})
