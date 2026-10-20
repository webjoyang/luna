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
  isAccountNoRequired,
  maskAccountNo,
  getOtherTxnCategoriesByDirection,
  validateTransfer,
  parseAccountErrorMessage,
  FINANCE_ACCOUNT_ERROR_CODES
} from '../model/account'

describe('Finance Account Domain Model & Utilities', () => {
  describe('formatAmount', () => {
    it('should format numbers with thousands separators and 2 decimal places', () => {
      expect(formatAmount(1234.56)).toBe('1,234.56')
      expect(formatAmount(1000000)).toBe('1,000,000.00')
      expect(formatAmount('500.5')).toBe('500.50')
      expect(formatAmount(0)).toBe('0.00')
    })

    it('should handle null, undefined, empty string or invalid input gracefully', () => {
      expect(formatAmount(null)).toBe('0.00')
      expect(formatAmount(undefined)).toBe('0.00')
      expect(formatAmount('')).toBe('0.00')
      expect(formatAmount('abc')).toBe('0.00')
    })
  })

  describe('isAccountNoRequired', () => {
    it('should require account_no for BANK, WECHAT, ALIPAY', () => {
      expect(isAccountNoRequired('BANK')).toBe(true)
      expect(isAccountNoRequired('bank')).toBe(true)
      expect(isAccountNoRequired('WECHAT')).toBe(true)
      expect(isAccountNoRequired('ALIPAY')).toBe(true)
    })

    it('should NOT require account_no for CASH or empty type', () => {
      expect(isAccountNoRequired('CASH')).toBe(false)
      expect(isAccountNoRequired('')).toBe(false)
      expect(isAccountNoRequired(null)).toBe(false)
      expect(isAccountNoRequired(undefined)).toBe(false)
    })
  })

  describe('maskAccountNo', () => {
    it('should mask middle digits of account_no when length > 8', () => {
      expect(maskAccountNo('6225880123456789')).toBe('6225 **** **** 6789')
      expect(maskAccountNo('alipay_user_012345')).toBe('alip **** **** 2345')
    })

    it('should preserve already masked backend strings', () => {
      expect(maskAccountNo('6225****6789')).toBe('6225****6789')
    })

    it('should return dash for empty/null accounts', () => {
      expect(maskAccountNo(null)).toBe('—')
      expect(maskAccountNo('')).toBe('—')
    })
  })

  describe('getOtherTxnCategoriesByDirection', () => {
    it('should return 3 inflow categories for DEBIT', () => {
      const debitList = getOtherTxnCategoriesByDirection('DEBIT')
      expect(debitList.length).toBe(3)
      const values = debitList.map((item) => item.value)
      expect(values).toContain('INTEREST_IN')
      expect(values).toContain('WASTE_IN')
      expect(values).toContain('OTHER_IN')
    })

    it('should return 3 outflow categories for CREDIT', () => {
      const creditList = getOtherTxnCategoriesByDirection('CREDIT')
      expect(creditList.length).toBe(3)
      const values = creditList.map((item) => item.value)
      expect(values).toContain('FEE_OUT')
      expect(values).toContain('COST_OUT')
      expect(values).toContain('OTHER_OUT')
    })
  })

  describe('validateTransfer', () => {
    const acc1 = {
      id: 1,
      accountName: '账户A',
      accountType: 'BANK',
      currency: 'RMB',
      status: 'ENABLE',
      currentBalance: 1000
    }
    const acc2 = {
      id: 2,
      accountName: '账户B',
      accountType: 'BANK',
      currency: 'RMB',
      status: 'ENABLE',
      currentBalance: 500
    }
    const accUSD = {
      id: 3,
      accountName: '美金户',
      accountType: 'BANK',
      currency: 'USD',
      status: 'ENABLE',
      currentBalance: 200
    }

    it('should validate successful transfer', () => {
      const res = validateTransfer(acc1, acc2, 300)
      expect(res.valid).toBe(true)
    })

    it('should reject when transferring to same account (020_107)', () => {
      const res = validateTransfer(acc1, acc1, 100)
      expect(res.valid).toBe(false)
      expect(res.message).toContain('转出账户与转入账户不能相同')
    })

    it('should reject cross-currency transfer (020_108)', () => {
      const res = validateTransfer(acc1, accUSD, 100)
      expect(res.valid).toBe(false)
      expect(res.message).toContain('跨币种')
    })

    it('should reject when transfer amount exceeds balance', () => {
      const res = validateTransfer(acc1, acc2, 2000)
      expect(res.valid).toBe(false)
      expect(res.message).toContain('超出转出账户可用余额')
    })

    it('should reject non-positive amounts', () => {
      const res = validateTransfer(acc1, acc2, 0)
      expect(res.valid).toBe(false)
      expect(res.message).toContain('大于 0')
    })
  })

  describe('parseAccountErrorMessage', () => {
    it('should return friendly error messages for specific error codes', () => {
      expect(
        parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_NAME_DUPLICATE)
      ).toContain('账户名称已存在')
      expect(
        parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN)
      ).toContain('已有分录引用')
      expect(
        parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_OPENING_BALANCE_IMMUTABLE)
      ).toContain('期初余额不可修改')
      expect(parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_NO_REQUIRED)).toContain(
        '必须填写账号或标识'
      )
      expect(
        parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH)
      ).toContain('可用余额不足')
      expect(parseAccountErrorMessage(999999999, '自定义失败')).toBe('自定义失败')
    })
  })
})
