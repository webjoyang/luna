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
  formatAmount,
  getExpenseActionFlags,
  calculateExpenseTotal,
  validateExpenseForm,
  parseExpenseErrorMessage,
  EXPENSE_ERROR_CODES,
  type ExpenseFormData
} from '../model/expense'

describe('Expense Domain Model & Utility Tests (Pure Node)', () => {
  describe('formatAmount', () => {
    it('formats normal numbers to comma-separated 2-decimal strings', () => {
      expect(formatAmount(1234.56)).toBe('1,234.56')
      expect(formatAmount('9876543.21')).toBe('9,876,543.21')
      expect(formatAmount(0)).toBe('0.00')
      expect(formatAmount(100)).toBe('100.00')
    })

    it('formats null, undefined and empty string to 0.00', () => {
      expect(formatAmount(null)).toBe('0.00')
      expect(formatAmount(undefined)).toBe('0.00')
      expect(formatAmount('')).toBe('0.00')
    })

    it('formats numbers with currency prefix', () => {
      expect(formatAmount(500, 'RMB')).toBe('RMB 500.00')
      expect(formatAmount(1234.5, 'USD')).toBe('USD 1,234.50')
    })

    it('preserves masked sensitive asterisks string', () => {
      expect(formatAmount('****')).toBe('****')
      expect(formatAmount('***', 'RMB')).toBe('RMB ***')
      expect(formatAmount('***.***', 'USD')).toBe('USD ***.***')
    })
  })

  describe('getExpenseActionFlags', () => {
    it('handles PENDING (draft) status', () => {
      const flags = getExpenseActionFlags('PENDING')
      expect(flags.canEdit).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.isApproved).toBe(false)
      expect(flags.isRejected).toBe(false)
    })

    it('handles APPROVING status', () => {
      const flags = getExpenseActionFlags('APPROVING')
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canWithdraw).toBe(true)
      expect(flags.isApproved).toBe(false)
      expect(flags.isRejected).toBe(false)
    })

    it('handles REJECTED status', () => {
      const flags = getExpenseActionFlags('REJECTED')
      expect(flags.canEdit).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.isApproved).toBe(false)
      expect(flags.isRejected).toBe(true)
    })

    it('handles APPROVED status', () => {
      const flags = getExpenseActionFlags('APPROVED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.isApproved).toBe(true)
      expect(flags.isRejected).toBe(false)
    })

    it('handles CANCELLED and unknown status', () => {
      const flags = getExpenseActionFlags('CANCELLED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.isApproved).toBe(false)
    })
  })

  describe('calculateExpenseTotal', () => {
    it('returns 0 for empty or null list', () => {
      expect(calculateExpenseTotal([])).toBe(0)
      expect(calculateExpenseTotal(null as any)).toBe(0)
    })

    it('calculates total correctly for multiple items', () => {
      const items = [
        { category: 'TRAVEL', originalAmount: 150.25, occurDate: '2026-10-10', remark: '火车票' },
        { category: 'OFFICE', originalAmount: '49.75', occurDate: '2026-10-11', remark: '文具' },
        { category: 'COMM', originalAmount: 100, occurDate: '2026-10-12', remark: '话费' }
      ]
      expect(calculateExpenseTotal(items)).toBe(300)
    })

    it('safely skips invalid amounts', () => {
      const items = [
        { category: 'TRAVEL', originalAmount: 'abc', occurDate: '2026-10-10', remark: '无效' },
        { category: 'COMM', originalAmount: 88.5, occurDate: '2026-10-12', remark: '话费' }
      ]
      expect(calculateExpenseTotal(items)).toBe(88.5)
    })
  })

  describe('validateExpenseForm', () => {
    const today = new Date().toLocaleDateString('en-CA')
    const futureDate = '2099-01-01'

    const validForm: ExpenseFormData = {
      accountId: 1001,
      applicantUserId: 10,
      attachments: ['https://oss.example.com/inv1.pdf'],
      items: [{ category: 'TRAVEL', originalAmount: 100, occurDate: today, remark: '打车' }],
      remark: '出差报销'
    }

    it('passes on valid form', () => {
      expect(validateExpenseForm(validForm)).toBeNull()
    })

    it('fails if applicantUserId is missing', () => {
      const form = { ...validForm, applicantUserId: undefined as any }
      expect(validateExpenseForm(form)).toBe('报销人员不能为空')
    })

    it('fails if accountId is missing', () => {
      const form = { ...validForm, accountId: undefined as any }
      expect(validateExpenseForm(form)).toBe('付款资金账户不能为空')
    })

    it('fails if items is empty', () => {
      const form = { ...validForm, items: [] }
      expect(validateExpenseForm(form)).toBe('报销明细行至少需要一行')
    })

    it('fails if item has invalid originalAmount', () => {
      const form = {
        ...validForm,
        items: [{ category: 'TRAVEL', originalAmount: 0, occurDate: today, remark: '打车' }]
      }
      expect(validateExpenseForm(form)).toBe('第 1 行报销金额必须大于0')

      const formNegative = {
        ...validForm,
        items: [{ category: 'TRAVEL', originalAmount: -10, occurDate: today, remark: '打车' }]
      }
      expect(validateExpenseForm(formNegative)).toBe('第 1 行报销金额必须大于0')
    })

    it('fails if item has future occurDate', () => {
      const form = {
        ...validForm,
        items: [{ category: 'TRAVEL', originalAmount: 50, occurDate: futureDate, remark: '超前' }]
      }
      expect(validateExpenseForm(form)).toBe('第 1 行费用发生日期不能晚于今天')
    })

    it('fails if item remark is empty', () => {
      const form = {
        ...validForm,
        items: [{ category: 'TRAVEL', originalAmount: 50, occurDate: today, remark: '  ' }]
      }
      expect(validateExpenseForm(form)).toBe('第 1 行费用明细备注说明不能为空')
    })

    it('fails if attachments exceed 10', () => {
      const form = {
        ...validForm,
        attachments: Array(11).fill('https://oss.example.com/file.pdf')
      }
      expect(validateExpenseForm(form)).toBe('报销附件最多上传10个')
    })
  })

  describe('parseExpenseErrorMessage and EXPENSE_ERROR_CODES', () => {
    it('defines correct 10-digit error codes', () => {
      expect(EXPENSE_ERROR_CODES.EXPENSE_NOT_EXISTS).toBe(1_030_024_000)
      expect(EXPENSE_ERROR_CODES.EXPENSE_STATUS_NOT_ALLOWED).toBe(1_030_024_001)
      expect(EXPENSE_ERROR_CODES.EXPENSE_ITEM_REQUIRED).toBe(1_030_024_002)
      expect(EXPENSE_ERROR_CODES.EXPENSE_ACCOUNT_INVALID).toBe(1_030_024_003)
      expect(EXPENSE_ERROR_CODES.EXPENSE_AMOUNT_INVALID).toBe(1_030_024_004)
      expect(EXPENSE_ERROR_CODES.EXPENSE_ATTACHMENT_LIMIT).toBe(1_030_024_005)
      expect(EXPENSE_ERROR_CODES.EXPENSE_USER_NOT_EXISTS).toBe(1_030_024_006)
      expect(EXPENSE_ERROR_CODES.EXPENSE_OCCUR_DATE_INVALID).toBe(1_030_024_007)
    })

    it('maps numeric error code directly or in Axios err.code', () => {
      expect(parseExpenseErrorMessage({ code: 1_030_024_000 })).toBe('报销单不存在')
      expect(parseExpenseErrorMessage({ code: 1_030_024_001 })).toBe('当前报销单状态不允许此操作')
      expect(parseExpenseErrorMessage({ code: 1_030_024_002 })).toBe('报销明细行至少需要一行')
      expect(parseExpenseErrorMessage({ code: 1_030_024_003 })).toBe('付款资金账户无效或已停用')
      expect(parseExpenseErrorMessage(1_030_024_004)).toBe('报销金额必须大于0')
      expect(parseExpenseErrorMessage({ code: 1_030_024_005 })).toBe('报销附件最多上传10个')
      expect(parseExpenseErrorMessage({ code: 1_030_024_006 })).toBe('报销人用户不存在')
      expect(parseExpenseErrorMessage(1_030_024_007)).toBe('费用发生日期不能晚于提交日')
    })

    it('maps known 024 string error codes to readable messages', () => {
      expect(parseExpenseErrorMessage('024_000')).toBe('报销单不存在')
      expect(parseExpenseErrorMessage({ message: '操作失败: 024_001' })).toBe(
        '当前报销单状态不允许此操作'
      )
      expect(parseExpenseErrorMessage('024_002')).toBe('报销明细行至少需要一行')
      expect(parseExpenseErrorMessage('024_003')).toBe('付款资金账户无效或已停用')
      expect(parseExpenseErrorMessage('024_004')).toBe('报销金额必须大于0')
      expect(parseExpenseErrorMessage('024_005')).toBe('报销附件最多上传10个')
      expect(parseExpenseErrorMessage('024_006')).toBe('报销人用户不存在')
      expect(parseExpenseErrorMessage('024_007')).toBe('费用发生日期不能晚于提交日')
    })

    it('falls back to error message or default text', () => {
      expect(parseExpenseErrorMessage({ message: '网络超时' })).toBe('网络超时')
      expect(parseExpenseErrorMessage(null)).toBe('操作失败，请重试')
    })
  })
})
