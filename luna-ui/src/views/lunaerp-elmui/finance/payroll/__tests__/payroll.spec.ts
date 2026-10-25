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
  getPayrollActionFlags,
  calculateLineItemAmount,
  calculateEmployeeTotals,
  calculatePayrollTotals,
  validateEmployeeNetAmounts,
  validatePayrollForm,
  transformDefectPreviewToItems,
  parsePayrollErrorMessage,
  PAYROLL_ERROR_CODES,
  type PayrollFormData,
  type PayrollEmployeeData,
  type PayrollLineItemData
} from '../model/payroll'

describe('Payroll Domain Model & Calculation Tests (Pure Node TDD)', () => {
  describe('formatAmount', () => {
    it('formats numbers to comma-separated 2-decimal strings', () => {
      expect(formatAmount(5000)).toBe('5,000.00')
      expect(formatAmount('12345.67')).toBe('12,345.67')
      expect(formatAmount(0)).toBe('0.00')
      expect(formatAmount(null)).toBe('0.00')
      expect(formatAmount(undefined)).toBe('0.00')
      expect(formatAmount('')).toBe('0.00')
    })

    it('formats numbers with currency prefix', () => {
      expect(formatAmount(5000, 'RMB')).toBe('RMB 5,000.00')
    })

    it('preserves masked sensitive asterisks', () => {
      expect(formatAmount('****')).toBe('****')
      expect(formatAmount('***', 'RMB')).toBe('RMB ***')
    })
  })

  describe('calculateLineItemAmount', () => {
    it('calculates amount from qty and unitPrice rounded to 2 decimals', () => {
      expect(calculateLineItemAmount(100, 5.5)).toBe(550.0)
      expect(calculateLineItemAmount(3, 10.333)).toBe(31.0)
      expect(calculateLineItemAmount(undefined, 50)).toBe(0)
      expect(calculateLineItemAmount(10, undefined)).toBe(0)
    })
  })

  describe('calculateEmployeeTotals & calculatePayrollTotals', () => {
    it('calculates employee earning, deduction and net amounts correctly', () => {
      const items: PayrollLineItemData[] = [
        { itemKind: 'EARNING', itemType: 'BASE', amount: 5000 },
        { itemKind: 'EARNING', itemType: 'PIECE', qty: 100, unitPrice: 15, amount: 1500 },
        { itemKind: 'DEDUCTION', itemType: 'OTHER', amount: 200 },
        { itemKind: 'DEDUCTION', itemType: 'DEFECT', qty: 2, unitPrice: 50, amount: 100 }
      ]

      const totals = calculateEmployeeTotals(items)
      expect(totals.earningTotal).toBe(6500.0)
      expect(totals.deductionTotal).toBe(300.0)
      expect(totals.netAmount).toBe(6200.0)
    })

    it('aggregates payroll-level totals across multiple employees', () => {
      const employees: PayrollEmployeeData[] = [
        {
          userId: 1,
          nickname: '张三',
          items: [
            { itemKind: 'EARNING', itemType: 'BASE', amount: 5000 },
            { itemKind: 'DEDUCTION', itemType: 'OTHER', amount: 200 }
          ]
        },
        {
          userId: 2,
          nickname: '李四',
          items: [
            { itemKind: 'EARNING', itemType: 'PIECE', amount: 8000 },
            { itemKind: 'DEDUCTION', itemType: 'DEFECT', amount: 500 }
          ]
        }
      ]

      const payrollTotals = calculatePayrollTotals(employees, 3000)
      expect(payrollTotals.earningTotal).toBe(13000.0)
      expect(payrollTotals.deductionTotal).toBe(700.0)
      expect(payrollTotals.netAmount).toBe(12300.0)
      expect(payrollTotals.paidAmount).toBe(3000.0)
      expect(payrollTotals.unpaidAmount).toBe(9300.0)
    })
  })

  describe('validateEmployeeNetAmounts (025_011)', () => {
    it('detects negative net amounts and returns error with offending nicknames', () => {
      const validEmployees: PayrollEmployeeData[] = [
        {
          userId: 1,
          nickname: '张三',
          items: [
            { itemKind: 'EARNING', itemType: 'BASE', amount: 5000 },
            { itemKind: 'DEDUCTION', itemType: 'DEFECT', amount: 200 }
          ]
        }
      ]
      expect(validateEmployeeNetAmounts(validEmployees)).toBeNull()

      const invalidEmployees: PayrollEmployeeData[] = [
        {
          userId: 1,
          nickname: '张三',
          items: [
            { itemKind: 'EARNING', itemType: 'BASE', amount: 1000 },
            { itemKind: 'DEDUCTION', itemType: 'DEFECT', amount: 1200 }
          ]
        },
        {
          userId: 2,
          nickname: '李四',
          items: [{ itemKind: 'EARNING', itemType: 'BASE', amount: 3000 }]
        }
      ]
      const errorMsg = validateEmployeeNetAmounts(invalidEmployees)
      expect(errorMsg).toContain('张三')
      expect(errorMsg).toContain('实发工资不能为负数')
    })
  })

  describe('validatePayrollForm', () => {
    it('validates required fields, employee count, item counts and duplicate employees', () => {
      const emptyForm: PayrollFormData = {
        period: '',
        title: '',
        employees: []
      }
      expect(validatePayrollForm(emptyForm)).toContain('工资期间')

      const invalidPeriodForm: PayrollFormData = {
        period: '202610',
        title: '',
        employees: []
      }
      expect(validatePayrollForm(invalidPeriodForm)).toContain('YYYY-MM')

      const noEmployeesForm: PayrollFormData = {
        period: '2026-10',
        title: '',
        employees: []
      }
      expect(validatePayrollForm(noEmployeesForm)).toContain('至少需要一人')

      const duplicateEmployeeForm: PayrollFormData = {
        period: '2026-10',
        title: '',
        employees: [
          {
            userId: 1,
            nickname: '张三',
            items: [{ itemKind: 'EARNING', itemType: 'BASE', amount: 100 }]
          },
          {
            userId: 1,
            nickname: '张三',
            items: [{ itemKind: 'EARNING', itemType: 'BASE', amount: 200 }]
          }
        ]
      }
      expect(validatePayrollForm(duplicateEmployeeForm)).toContain('不能重复')

      const invalidItemAmountForm: PayrollFormData = {
        period: '2026-10',
        title: '',
        employees: [
          {
            userId: 1,
            nickname: '张三',
            items: [{ itemKind: 'EARNING', itemType: 'BASE', amount: 0 }]
          }
        ]
      }
      expect(validatePayrollForm(invalidItemAmountForm)).toContain('金额必须大于0')
    })
  })

  describe('getPayrollActionFlags', () => {
    it('handles PENDING status', () => {
      const flags = getPayrollActionFlags('PENDING', 'UNPAID', 0)
      expect(flags.canEdit).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canCancelApprove).toBe(false)
      expect(flags.canCancel).toBe(false)
      expect(flags.canPay).toBe(false)
      expect(flags.canPrint).toBe(false)
    })

    it('handles APPROVING status', () => {
      const flags = getPayrollActionFlags('APPROVING', 'UNPAID', 0)
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canCancelApprove).toBe(true)
      expect(flags.canCancel).toBe(false)
      expect(flags.canPay).toBe(false)
      expect(flags.canPrint).toBe(false)
    })

    it('handles APPROVED with 0 payment (can pay, print, and cancel)', () => {
      const flags = getPayrollActionFlags('APPROVED', 'UNPAID', 0)
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canCancelApprove).toBe(false)
      expect(flags.canCancel).toBe(true) // 零发放可作废
      expect(flags.canPay).toBe(true)
      expect(flags.canPrint).toBe(true)
    })

    it('handles APPROVED with partial or full payment (cannot cancel)', () => {
      const flagsPartial = getPayrollActionFlags('APPROVED', 'PARTIAL', 2000)
      expect(flagsPartial.canCancel).toBe(false) // 有发放禁止作废
      expect(flagsPartial.canPay).toBe(true)
      expect(flagsPartial.canPrint).toBe(true)

      const flagsPaid = getPayrollActionFlags('APPROVED', 'PAID', 5000)
      expect(flagsPaid.canCancel).toBe(false)
      expect(flagsPaid.canPay).toBe(false) // 已发清不能再发
      expect(flagsPaid.canPrint).toBe(true)
    })

    it('handles REJECTED status', () => {
      const flags = getPayrollActionFlags('REJECTED', 'UNPAID', 0)
      expect(flags.canEdit).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canCancel).toBe(false)
    })

    it('handles CANCELLED status', () => {
      const flags = getPayrollActionFlags('CANCELLED', 'UNPAID', 0)
      expect(flags.canEdit).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canCancel).toBe(false)
      expect(flags.canPay).toBe(false)
      expect(flags.canPrint).toBe(false)
    })
  })

  describe('transformDefectPreviewToItems', () => {
    it('transforms defect groups into line items and defectRefs correctly', () => {
      const groups = [
        {
          workerUserId: 101,
          workerName: '王五',
          count: 2,
          records: [
            {
              confirmationId: 901,
              confirmationNo: 'QC001',
              rollNo: 'R1',
              vatNo: 'V1',
              grade: 'C',
              confirmedAt: '2026-10-15 10:00:00',
              workerUserId: 101,
              workerName: '王五'
            },
            {
              confirmationId: 902,
              confirmationNo: 'QC002',
              rollNo: 'R2',
              vatNo: 'V1',
              grade: 'C',
              confirmedAt: '2026-10-16 11:00:00',
              workerUserId: 101,
              workerName: '王五'
            }
          ]
        }
      ]

      const unitPrice = 60
      const itemsMap = transformDefectPreviewToItems(groups, unitPrice, [101])
      expect(itemsMap.has(101)).toBe(true)
      const lineItem = itemsMap.get(101)!
      expect(lineItem.itemKind).toBe('DEDUCTION')
      expect(lineItem.itemType).toBe('DEFECT')
      expect(lineItem.qty).toBe(2)
      expect(lineItem.unitPrice).toBe(60)
      expect(lineItem.amount).toBe(120)
      expect(lineItem.defectRefs?.length).toBe(2)
      expect(lineItem.defectRefs?.[0].confirmationId).toBe(901)
      expect(lineItem.defectRefs?.[0].deductPrice).toBe(60)
    })
  })

  describe('parsePayrollErrorMessage', () => {
    it('decodes numeric and string 025_ / 023_ codes to friendly text', () => {
      expect(parsePayrollErrorMessage({ code: PAYROLL_ERROR_CODES.PAYROLL_PERIOD_DUPLICATE })).toBe(
        '同期间有效工资单已存在'
      )
      expect(
        parsePayrollErrorMessage({
          code: PAYROLL_ERROR_CODES.PAYROLL_DEFECT_ALREADY_REFERENCED,
          msg: '次品记录【QC001】已被工资单【GZ202610001】引用或已失效'
        })
      ).toBe('次品记录【QC001】已被工资单【GZ202610001】引用或已失效')
      expect(
        parsePayrollErrorMessage({ code: PAYROLL_ERROR_CODES.PAYROLL_HAS_PAYMENT_NO_CANCEL })
      ).toBe('工资单已存在发放记录，禁止作废')
      expect(
        parsePayrollErrorMessage({ code: PAYROLL_ERROR_CODES.PAYROLL_PAYMENT_AMOUNT_EXCEED })
      ).toBe('本次发放金额超出剩余应发金额')
      expect(
        parsePayrollErrorMessage({ code: PAYROLL_ERROR_CODES.ACCOUNT_BALANCE_NOT_ENOUGH })
      ).toBe('资金账户可用余额不足')
      expect(parsePayrollErrorMessage('业务错误: 025_011 实发不能为负数')).toContain('实发')
    })
  })
})
