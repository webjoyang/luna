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
  formatExchangeRate,
  isBaseCurrency,
  validateExchangeRateInput,
  validateCurrencyInput,
  parseExchangeRateErrorMessage,
  EXCHANGE_RATE_ERROR_CODES
} from '../model/exchange-rate'

describe('Exchange Rate Domain Model & Utilities (Pure Node)', () => {
  describe('formatExchangeRate', () => {
    it('should format normal rates with 8 decimal places by default', () => {
      expect(formatExchangeRate(7.12345678)).toBe('7.12345678')
      expect(formatExchangeRate('7.12')).toBe('7.12000000')
      expect(formatExchangeRate(1)).toBe('1.00000000')
    })

    it('should handle null, undefined, empty string or invalid numbers', () => {
      expect(formatExchangeRate(null)).toBe('—')
      expect(formatExchangeRate(undefined)).toBe('—')
      expect(formatExchangeRate('')).toBe('—')
      expect(formatExchangeRate('invalid')).toBe('—')
    })
  })

  describe('isBaseCurrency', () => {
    it('should identify RMB and CNY as base currency (case-insensitive)', () => {
      expect(isBaseCurrency('RMB')).toBe(true)
      expect(isBaseCurrency('rmb')).toBe(true)
      expect(isBaseCurrency('CNY')).toBe(true)
      expect(isBaseCurrency('cny')).toBe(true)
      expect(isBaseCurrency('  RMB  ')).toBe(true)
    })

    it('should return false for foreign currencies', () => {
      expect(isBaseCurrency('USD')).toBe(false)
      expect(isBaseCurrency('EUR')).toBe(false)
      expect(isBaseCurrency('HKD')).toBe(false)
      expect(isBaseCurrency('')).toBe(false)
      expect(isBaseCurrency(null)).toBe(false)
      expect(isBaseCurrency(undefined)).toBe(false)
    })
  })

  describe('validateExchangeRateInput', () => {
    it('should accept valid positive numbers', () => {
      expect(validateExchangeRateInput(7.1234).valid).toBe(true)
      expect(validateExchangeRateInput('0.00000001').valid).toBe(true)
    })

    it('should reject zero or negative numbers or empty', () => {
      expect(validateExchangeRateInput(0).valid).toBe(false)
      expect(validateExchangeRateInput(-1).valid).toBe(false)
      expect(validateExchangeRateInput('').valid).toBe(false)
      expect(validateExchangeRateInput(null).valid).toBe(false)
    })
  })

  describe('validateCurrencyInput', () => {
    it('should reject RMB and CNY', () => {
      const resRmb = validateCurrencyInput('RMB')
      expect(resRmb.valid).toBe(false)
      expect(resRmb.message).toContain('本位币')

      const resCny = validateCurrencyInput('cny')
      expect(resCny.valid).toBe(false)
    })

    it('should accept valid foreign currency codes', () => {
      expect(validateCurrencyInput('USD').valid).toBe(true)
      expect(validateCurrencyInput('EUR').valid).toBe(true)
    })

    it('should reject empty currency code', () => {
      expect(validateCurrencyInput('').valid).toBe(false)
      expect(validateCurrencyInput('   ').valid).toBe(false)
    })
  })

  describe('parseExchangeRateErrorMessage', () => {
    it('should map domain error codes to user-friendly messages', () => {
      expect(
        parseExchangeRateErrorMessage(EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_NOT_FOUND)
      ).toContain('未找到生效汇率')
      expect(
        parseExchangeRateErrorMessage(EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_NO_DUPLICATE)
      ).toContain('同币种同生效日汇率已存在')
      expect(
        parseExchangeRateErrorMessage(
          EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN
        )
      ).toContain('本位币')
    })

    it('should fallback to default message for unknown error code', () => {
      expect(parseExchangeRateErrorMessage(999999, '系统错误')).toBe('系统错误')
    })
  })
})
