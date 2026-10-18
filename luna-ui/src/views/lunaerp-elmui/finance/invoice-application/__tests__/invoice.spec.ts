import { describe, it, expect } from 'vitest'
import {
  calculateTax,
  formatMoney,
  formatInvoiceStatusTag,
  getInvoiceActionFlags,
  validateOfficialInvoiceNo
} from '../model/invoice'

describe('发票申请前端领域模型与工具函数单元测试', () => {
  describe('税额反算计算器 calculateTax', () => {
    it('标准税率 13% 反算：113.00 价税合计 -> 100.00 不含税 + 13.00 税额', () => {
      const result = calculateTax(113.0, 13.0)
      expect(result.totalAmountExclTax).toBe(100.0)
      expect(result.totalTax).toBe(13.0)
      expect(result.totalAmountInclTax).toBe(113.0)
    })

    it('舍入补偿反算：100.00 价税合计，13% 税率 -> 88.50 不含税 + 11.50 税额，相加必等于 100', () => {
      const result = calculateTax(100.0, 13.0)
      expect(result.totalAmountExclTax).toBe(88.5)
      expect(result.totalTax).toBe(11.5)
      expect(result.totalAmountExclTax + result.totalTax).toBe(100.0)
    })

    it('零税率或免税反算：税额恒为 0，不含税等于价税合计', () => {
      const resultZero = calculateTax(100.0, 0.0)
      expect(resultZero.totalAmountExclTax).toBe(100.0)
      expect(resultZero.totalTax).toBe(0)

      const resultNegative = calculateTax(100.0, -1)
      expect(resultNegative.totalAmountExclTax).toBe(100.0)
      expect(resultNegative.totalTax).toBe(0)
    })
  })

  describe('发票状态按钮权限与动作标记 getInvoiceActionFlags', () => {
    it('草稿 DRAFT 态：允许编辑、删除、提交；不允许开票、驳回', () => {
      const flags = getInvoiceActionFlags('DRAFT')
      expect(flags.canEdit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canIssue).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.isFinal).toBe(false)
    })

    it('已提交 SUBMITTED 态：允许开票登记、驳回；不允许编辑、删除', () => {
      const flags = getInvoiceActionFlags('SUBMITTED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canIssue).toBe(true)
      expect(flags.canReject).toBe(true)
      expect(flags.isFinal).toBe(false)
    })

    it('已驳回 REJECTED 态：允许重新提交；不允许开票、删除', () => {
      const flags = getInvoiceActionFlags('REJECTED')
      expect(flags.canSubmit).toBe(true)
      expect(flags.canIssue).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.isFinal).toBe(false)
    })

    it('已开票 ISSUED 态：终态，所有写操作禁用', () => {
      const flags = getInvoiceActionFlags('ISSUED')
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canIssue).toBe(false)
      expect(flags.canReject).toBe(false)
      expect(flags.isFinal).toBe(true)
    })
  })

  describe('官方发票号校验 validateOfficialInvoiceNo', () => {
    it('合法 20 位发票号验证通过', () => {
      expect(validateOfficialInvoiceNo('24322000000000000001')).toBe(true)
      expect(validateOfficialInvoiceNo('ABCD1234EFGH5678IJKL')).toBe(true)
    })

    it('非 20 位或包含特殊字符拒绝', () => {
      expect(validateOfficialInvoiceNo('12345')).toBe(false)
      expect(validateOfficialInvoiceNo('243220000000000000001')).toBe(false) // 21位
      expect(validateOfficialInvoiceNo('')).toBe(false)
      expect(validateOfficialInvoiceNo(undefined)).toBe(false)
      expect(validateOfficialInvoiceNo('2432200000000000000-')).toBe(false)
    })
  })

  describe('状态 Tag 格式化 formatInvoiceStatusTag', () => {
    it('正确映射状态与颜色', () => {
      expect(formatInvoiceStatusTag('DRAFT')).toEqual({ label: '草稿', type: 'info' })
      expect(formatInvoiceStatusTag('SUBMITTED')).toEqual({ label: '已提交', type: 'warning' })
      expect(formatInvoiceStatusTag('ISSUED')).toEqual({ label: '已开票', type: 'success' })
      expect(formatInvoiceStatusTag('REJECTED')).toEqual({ label: '已驳回', type: 'danger' })
    })
  })

  describe('金额格式化 formatMoney', () => {
    it('千分位与两位小数格式化', () => {
      expect(formatMoney(1000)).toBe('1,000.00')
      expect(formatMoney(1234567.89)).toBe('1,234,567.89')
      expect(formatMoney(0)).toBe('0.00')
      expect(formatMoney(undefined)).toBe('0.00')
    })
  })
})
