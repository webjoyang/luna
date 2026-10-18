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
  isBaseCurrency,
  formatExchangeRate,
  validateExchangeRateInput,
  validateCurrencyInput,
  parseExchangeRateErrorMessage,
  EXCHANGE_RATE_ERROR_CODES
} from '../exchange-rate/model/exchange-rate'

import {
  formatMoney as formatPaymentMoney,
  maskBankAccount,
  calcReconRemainingLimit,
  calcApplyRemain,
  getPaymentApplyActionFlags,
  FINANCE_PAYMENT_ERROR_CODES
} from '../payment/model/payment'

import {
  getDefaultPreviousMonthRange,
  validateStatementPeriod,
  calculateStatementSummary,
  getCustomerStatementActionFlags,
  validateConfirmOnBehalfForm,
  formatStatementStatusTag,
  parseCustomerStatementError,
  CUSTOMER_STATEMENT_ERROR_CODES
} from '../customer-reconciliation/model/customerStatement'

import {
  formatAgingBucketTag,
  formatAgingOverdueTag,
  calculateAgingBucketPercentages,
  calculateAgingGrandTotals,
  AGING_BUCKETS
} from '../customer-reconciliation/model/customerAging'

import {
  calculateTax,
  formatInvoiceStatusTag,
  TAX_RATE_OPTIONS,
  INVOICE_KIND_OPTIONS,
  INVOICE_ERROR_CODES
} from '../invoice-application/model/invoice'

describe('Finance Customer Reconciliation Full Chain Frontend Smoke Test', () => {
  describe('1. 汇率主档纯模型与校验', () => {
    it('应正确判定本位币（RMB / CNY，不区分大小写）', () => {
      expect(isBaseCurrency('RMB')).toBe(true)
      expect(isBaseCurrency('rmb')).toBe(true)
      expect(isBaseCurrency('CNY')).toBe(true)
      expect(isBaseCurrency('cny')).toBe(true)
      expect(isBaseCurrency('USD')).toBe(false)
      expect(isBaseCurrency('EUR')).toBe(false)
      expect(isBaseCurrency(null)).toBe(false)
      expect(isBaseCurrency(undefined)).toBe(false)
    })

    it('币种输入校验：禁止本位币配置，允许外币', () => {
      expect(validateCurrencyInput('RMB').valid).toBe(false)
      expect(validateCurrencyInput('RMB').message).toContain('本位币')
      expect(validateCurrencyInput('').valid).toBe(false)
      expect(validateCurrencyInput('USD').valid).toBe(true)
    })

    it('汇率输入校验与8位高精度格式化', () => {
      expect(validateExchangeRateInput(0).valid).toBe(false)
      expect(validateExchangeRateInput(-1.2).valid).toBe(false)
      expect(validateExchangeRateInput(7.15).valid).toBe(true)

      expect(formatExchangeRate(7.15)).toBe('7.15000000')
      expect(formatExchangeRate(null)).toBe('—')
    })

    it('汇率错误码映射', () => {
      expect(
        parseExchangeRateErrorMessage(
          EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN
        )
      ).toContain('本位币')
    })
  })

  describe('2. 收款与核销纯模型与工具', () => {
    it('金额千分位格式化与银行账号脱敏', () => {
      expect(formatPaymentMoney(12345.6)).toBe('12,345.60')
      expect(formatPaymentMoney(0)).toBe('0.00')
      expect(formatPaymentMoney(null)).toBe('0.00')

      expect(maskBankAccount('622202100011223344')).toBe('622202******3344')
      expect(maskBankAccount('')).toBe('—')
    })

    it('计算剩余核销上限与申请余额', () => {
      expect(calcReconRemainingLimit(10000, 3500)).toBe(6500.0)
      expect(calcReconRemainingLimit(1000, 1500)).toBe(0) // 下限兜底 0

      expect(calcApplyRemain({ amount: 5000, allocatedAmount: 2000 })).toBe(3000.0)
      expect(calcApplyRemain({ remainAmount: 888.88 })).toBe(888.88)
    })

    it('收款与申请审批状态操作标识', () => {
      const draftFlags = getPaymentApplyActionFlags('DRAFT')
      expect(draftFlags.canEdit).toBe(true)
      expect(draftFlags.canSubmit).toBe(true)
      expect(draftFlags.canDelete).toBe(true)

      const approvedFlags = getPaymentApplyActionFlags('APPROVED')
      expect(approvedFlags.canEdit).toBe(false)
      expect(approvedFlags.canDelete).toBe(false)
      expect(approvedFlags.isApproved).toBe(true)
    })
  })

  describe('3. 客户对账单纯模型与勾稽计算', () => {
    it('自然月区间推算与对账期间校验', () => {
      const [start, end] = getDefaultPreviousMonthRange(new Date('2026-10-15'))
      expect(start).toBe('2026-09-01')
      expect(end).toBe('2026-09-30')

      expect(validateStatementPeriod('2026-10-01', '2026-10-31').valid).toBe(true)
      expect(validateStatementPeriod('2026-10-31', '2026-10-01').valid).toBe(false)
    })

    it('对账单勾稽恒等式：期末 = 期初 + 借方合计 - 贷方合计', () => {
      const summary = calculateStatementSummary(1000.0, [
        { debitAmount: 4000.0, creditAmount: 0 },
        { debitAmount: 2500.0, creditAmount: 0 },
        { debitAmount: 0, creditAmount: 3500.0 },
        { debitAmount: 0, creditAmount: 1500.0 }
      ])
      expect(summary.periodDebit).toBe(6500.0)
      expect(summary.periodCredit).toBe(5000.0)
      expect(summary.closingBalance).toBe(2500.0)
    })

    it('对账单单一信任源操作权限守卫', () => {
      // DRAFT 态：可编辑、可删除、可追加行、可发送、可作废、可打印
      const draftFlags = getCustomerStatementActionFlags('DRAFT')
      expect(draftFlags.canEdit).toBe(true)
      expect(draftFlags.canDelete).toBe(true)
      expect(draftFlags.canAddEntry).toBe(true)
      expect(draftFlags.canSend).toBe(true)
      expect(draftFlags.canVoid).toBe(true)
      expect(draftFlags.canPrint).toBe(true)
      expect(draftFlags.canConfirmOnBehalf).toBe(false)

      // SENT 态：不可编辑/删除/追加，可代客确认、可作废、可打印
      const sentFlags = getCustomerStatementActionFlags('SENT')
      expect(sentFlags.canEdit).toBe(false)
      expect(sentFlags.canSend).toBe(false)
      expect(sentFlags.canConfirmOnBehalf).toBe(true)
      expect(sentFlags.canVoid).toBe(true)
      expect(sentFlags.canPrint).toBe(true)

      // CONFIRMED 终态：不可作废、不可代客确认，可打印
      const confirmedFlags = getCustomerStatementActionFlags('CONFIRMED')
      expect(confirmedFlags.canVoid).toBe(false)
      expect(confirmedFlags.canPrint).toBe(true)

      // VOID 终态：不可打印，不可作废
      const voidFlags = getCustomerStatementActionFlags('VOID')
      expect(voidFlags.canPrint).toBe(false)
      expect(voidFlags.canVoid).toBe(false)
    })

    it('代客确认表单与状态标签', () => {
      expect(validateConfirmOnBehalfForm('', '依据齐全').valid).toBe(false)
      expect(validateConfirmOnBehalfForm('PHONE', '   ').valid).toBe(false)
      expect(validateConfirmOnBehalfForm('WECHAT', '微信聊天记录截图').valid).toBe(true)

      expect(formatStatementStatusTag('SENT').label).toBe('已发出')
      expect(formatStatementStatusTag('CONFIRMED').label).toBe('已确认')
      expect(parseCustomerStatementError(CUSTOMER_STATEMENT_ERROR_CODES.ENTRY_OCCUPIED)).toContain(
        '占用'
      )
    })
  })

  describe('4. FIFO 客户账龄纯模型与五桶聚合', () => {
    it('五桶标签与逾期标签展示', () => {
      expect(formatAgingBucketTag('0-30').label).toBe('0–30天')
      expect(formatAgingBucketTag('91-180').type).toBe('danger')

      expect(formatAgingOverdueTag(false).label).toBe('正常未逾期')
      expect(formatAgingOverdueTag(true, 15).label).toBe('已逾期 15 天')
    })

    it('计算各桶占比与大汇总合计', () => {
      const percentages = calculateAgingBucketPercentages({
        totalArBalance: 10000,
        bucket0To30: 4000,
        bucket31To60: 3000,
        bucket61To90: 2000,
        bucket91To180: 1000,
        bucket181Plus: 0
      })
      expect(percentages.pct0To30).toBe(40.0)
      expect(percentages.pct31To60).toBe(30.0)
      expect(percentages.pct61To90).toBe(20.0)
      expect(percentages.pct91To180).toBe(10.0)
      expect(percentages.pct181Plus).toBe(0.0)

      const grandTotals = calculateAgingGrandTotals([
        {
          totalArBalance: 6000,
          advanceBalance: 500,
          overdueAmount: 0,
          bucket0To30: 6000,
          bucket31To60: 0,
          bucket61To90: 0,
          bucket91To180: 0,
          bucket181Plus: 0
        },
        {
          totalArBalance: 4000,
          advanceBalance: 200,
          overdueAmount: 1000,
          bucket0To30: 0,
          bucket31To60: 4000,
          bucket61To90: 0,
          bucket91To180: 0,
          bucket181Plus: 0
        }
      ])
      expect(grandTotals.totalAr).toBe(10000.0)
      expect(grandTotals.totalAdvance).toBe(700.0)
      expect(grandTotals.totalOverdue).toBe(1000.0)
      expect(grandTotals.bucket0To30).toBe(6000.0)
      expect(grandTotals.bucket31To60).toBe(4000.0)
    })
  })

  describe('5. 发票申请价税纯函数与开票状态', () => {
    it('价税分离严格恒等式：不含税金额 + 税额 === 含税总额', () => {
      // 5650.00 @ 13% -> 不含税 5000.00, 税额 650.00
      const res13 = calculateTax(5650.0, 13.0)
      expect(res13.totalAmountExclTax).toBe(5000.0)
      expect(res13.totalTax).toBe(650.0)
      expect(res13.totalAmountInclTax).toBe(5650.0)
      expect(res13.totalAmountExclTax + res13.totalTax).toBe(res13.totalAmountInclTax)

      // 9% 税率
      const res9 = calculateTax(1090.0, 9.0)
      expect(res9.totalAmountExclTax).toBe(1000.0)
      expect(res9.totalTax).toBe(90.0)
      expect(res9.totalAmountExclTax + res9.totalTax).toBe(res9.totalAmountInclTax)

      // 0% 外贸免税
      const res0 = calculateTax(8000.0, 0.0)
      expect(res0.totalAmountExclTax).toBe(8000.0)
      expect(res0.totalTax).toBe(0.0)
      expect(res0.totalAmountInclTax).toBe(8000.0)
    })

    it('开票税率与种类选项及状态标签', () => {
      expect(TAX_RATE_OPTIONS.some((opt) => opt.value === 13.0)).toBe(true)
      expect(INVOICE_KIND_OPTIONS.some((opt) => opt.value === 'DIGITAL_SPECIAL')).toBe(true)

      expect(formatInvoiceStatusTag('DRAFT').label).toBe('草稿')
      expect(formatInvoiceStatusTag('SUBMITTED').label).toBe('已提交')
      expect(formatInvoiceStatusTag('ISSUED').label).toBe('已开票')
      expect(formatInvoiceStatusTag('REJECTED').label).toBe('已驳回')
      expect(INVOICE_ERROR_CODES.OFFICIAL_NO_REQUIRED).toBe(1030026005)
    })
  })

  describe('6. 客户对账全链路业务闭环端到端验证', () => {
    it('全流程业务逻辑勾稽校验：汇率 -> 收款核销 -> 预收转核销 -> 对账单勾稽 -> 账龄五桶 -> 数电发票反算', () => {
      // 1. 汇率校验：USD 汇率 7.15000000
      const fxValid = validateExchangeRateInput(7.15)
      expect(fxValid.valid).toBe(true)
      expect(formatExchangeRate(7.15)).toBe('7.15000000')

      // 2. 发货产生应收 6500.00，第一笔收款 3500.00 核销
      const originalAr = 6500.0
      const receipt1 = 3500.0
      const remainAfterReceipt = originalAr - receipt1
      expect(remainAfterReceipt).toBe(3000.0)

      // 3. 预收款 2000.00，转核销 1500.00
      const advanceTotal = 2000.0
      const advanceWriteoff = 1500.0
      const advanceBalance = advanceTotal - advanceWriteoff
      expect(advanceBalance).toBe(500.0)

      // 剩余应收余额 = 3000.00 - 1500.00 = 1500.00
      const finalArBalance = remainAfterReceipt - advanceWriteoff
      expect(finalArBalance).toBe(1500.0)

      // 4. 对账单汇总勾稽：期初 0，借方 6500.00，贷方 5000.00（3500 + 1500），期末 1500.00
      const stmtSummary = calculateStatementSummary(0, [
        { debitAmount: 6500.0, creditAmount: 0 },
        { debitAmount: 0, creditAmount: 3500.0 },
        { debitAmount: 0, creditAmount: 1500.0 }
      ])
      expect(stmtSummary.periodDebit).toBe(6500.0)
      expect(stmtSummary.periodCredit).toBe(5000.0)
      expect(stmtSummary.closingBalance).toBe(1500.0)
      expect(stmtSummary.closingBalance).toBe(finalArBalance)

      // 5. 对账单状态流转：DRAFT -> SENT -> CONFIRMED
      let flags = getCustomerStatementActionFlags('DRAFT')
      expect(flags.canSend).toBe(true)
      flags = getCustomerStatementActionFlags('SENT')
      expect(flags.canConfirmOnBehalf).toBe(true)
      flags = getCustomerStatementActionFlags('CONFIRMED')
      expect(flags.canPrint).toBe(true)
      expect(flags.canVoid).toBe(false)

      // 6. 账龄五桶：1500.00 落入 0-30天桶，预收余额 500.00 单列不进桶
      const agingTotals = calculateAgingGrandTotals([
        {
          totalArBalance: finalArBalance,
          advanceBalance: advanceBalance,
          overdueAmount: 0,
          bucket0To30: finalArBalance,
          bucket31To60: 0,
          bucket61To90: 0,
          bucket91To180: 0,
          bucket181Plus: 0
        }
      ])
      expect(agingTotals.totalAr).toBe(1500.0)
      expect(agingTotals.bucket0To30).toBe(1500.0)
      expect(agingTotals.totalAdvance).toBe(500.0)
      expect(agingTotals.totalOverdue).toBe(0.0)

      // 7. 发票申请与开票：对 5650.00 申请数电专票（13% 税率）
      const invoiceTax = calculateTax(5650.0, 13.0)
      expect(invoiceTax.totalAmountExclTax).toBe(5000.0)
      expect(invoiceTax.totalTax).toBe(650.0)
      expect(invoiceTax.totalAmountExclTax + invoiceTax.totalTax).toBe(5650.0)

      // 官方数电发票 20 位校验
      const officialInvoiceNo = '24322000000012345678'
      expect(officialInvoiceNo.length).toBe(20)
      expect(formatInvoiceStatusTag('ISSUED').label).toBe('已开票')
    })
  })
})
