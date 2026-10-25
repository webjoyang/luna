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
  calcItemBillQty,
  calcItemLineAmount,
  calcTotalAmount,
  formatMoney,
  getProductReferenceFee,
  validateReconDraft,
  parseReconErrorMessage
} from '../model/supplier-recon'

import {
  formatLineTypePrintLabel,
  formatInspectionTypeLabel,
  formatPricingBasisLabel,
  calculatePrintSubtotals,
  formatPrintTime
} from '../components/print/utils'

import * as SupplierReconApi from '@/api/lunaerp-elmui/finance/supplier-recon'
import request from '@/config/axios'

describe('Supplier Recon Domain Model & Calculator (Pure Node)', () => {
  describe('Default Date Range Calculation', () => {
    it('should compute previous month 1st to last day for October 2026', () => {
      const octDate = new Date(2026, 9, 16) // Oct 16, 2026
      const [start, end] = getDefaultPreviousMonthRange(octDate)
      expect(start).toBe('2026-09-01')
      expect(end).toBe('2026-09-30')
    })

    it('should handle January boundary across year correctly', () => {
      const janDate = new Date(2026, 0, 5) // Jan 5, 2026
      const [start, end] = getDefaultPreviousMonthRange(janDate)
      expect(start).toBe('2025-12-01')
      expect(end).toBe('2025-12-31')
    })

    it('should handle leap year February correctly', () => {
      const marDate = new Date(2024, 2, 10) // Mar 10, 2024
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

  describe('Quantity & Line Amount Calculation', () => {
    it('calcItemBillQty for PROCESS row should switch based on pricingBasis', () => {
      const processRow = {
        lineType: 'PROCESS',
        qtyKg: 150.25,
        qtyM: 300.5,
        pricingBasis: 'METER'
      }
      expect(calcItemBillQty(processRow)).toBe(300.5)

      processRow.pricingBasis = 'KG'
      expect(calcItemBillQty(processRow)).toBe(150.25)
    })

    it('calcItemBillQty for PURCHASE row should return billQty or fallback quantity', () => {
      expect(calcItemBillQty({ lineType: 'PURCHASE', billQty: 500 })).toBe(500)
      expect(calcItemBillQty({ lineType: 'PURCHASE', qtyKg: 200 })).toBe(200)
      expect(calcItemBillQty({ lineType: 'PURCHASE', qtyM: 400 })).toBe(400)
    })

    it('calcItemBillQty for ADJUST row should return 1', () => {
      expect(calcItemBillQty({ lineType: 'ADJUST' })).toBe(1)
    })

    it('calcItemLineAmount should multiply billQty by unitPrice for PROCESS and PURCHASE', () => {
      const processRow = {
        lineType: 'PROCESS',
        qtyKg: 100,
        qtyM: 200,
        pricingBasis: 'METER',
        unitPrice: 3.5
      }
      expect(calcItemLineAmount(processRow)).toBe(700.0)

      const purchaseRow = {
        lineType: 'PURCHASE',
        billQty: 80,
        unitPrice: 12.55
      }
      expect(calcItemLineAmount(purchaseRow)).toBe(1004.0)
    })

    it('calcItemLineAmount for ADJUST row should return exact signed lineAmount', () => {
      expect(calcItemLineAmount({ lineType: 'ADJUST', lineAmount: -250.5 })).toBe(-250.5)
      expect(calcItemLineAmount({ lineType: 'ADJUST', lineAmount: 180 })).toBe(180.0)
    })

    it('calcTotalAmount should correctly sum PROCESS, PURCHASE, and negative ADJUST', () => {
      const items = [
        { lineType: 'PROCESS', qtyM: 1000, pricingBasis: 'METER', unitPrice: 2.5 }, // 2500.00
        { lineType: 'PURCHASE', billQty: 500, unitPrice: 10.0 }, // 5000.00
        { lineType: 'ADJUST', lineAmount: -300.0 }, // -300.00
        { lineType: 'ADJUST', lineAmount: 50.0 } // +50.00
      ]
      expect(calcTotalAmount(items)).toBe(7250.0)
    })

    it('calcTotalAmount should return 0 for empty items', () => {
      expect(calcTotalAmount([])).toBe(0)
    })
  })

  describe('Money Formatting with Thousands Separator', () => {
    it('formatMoney should format numbers with thousand commas and 2 decimals', () => {
      expect(formatMoney(12345.6)).toBe('12,345.60')
      expect(formatMoney(1000000)).toBe('1,000,000.00')
      expect(formatMoney(0)).toBe('0.00')
      expect(formatMoney(-500.5)).toBe('-500.50')
      expect(formatMoney(null)).toBe('0.00')
      expect(formatMoney(undefined)).toBe('0.00')
    })
  })

  describe('Product Processing Fee Reference', () => {
    it('getProductReferenceFee should return weaving fee for METER', () => {
      const product = { weavingFeePerMeter: 1.85, knitProcessingFeePerKg: 3.2 }
      const res = getProductReferenceFee(product, 'METER')
      expect(res.label).toBe('织造费')
      expect(res.fee).toBe(1.85)
    })

    it('getProductReferenceFee should return knit fee for KG', () => {
      const product = { weavingFeePerMeter: 1.85, knitProcessingFeePerKg: 3.2 }
      const res = getProductReferenceFee(product, 'KG')
      expect(res.label).toBe('针织加工费')
      expect(res.fee).toBe(3.2)
    })

    it('getProductReferenceFee should handle null product or fees gracefully', () => {
      expect(getProductReferenceFee(null, 'KG').fee).toBeNull()
      expect(getProductReferenceFee({}, 'METER').fee).toBeNull()
    })
  })

  describe('Draft Validation Guard', () => {
    it('should reject missing supplier', () => {
      const res = validateReconDraft({
        supplierId: null,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: [{ lineType: 'PROCESS', unitPrice: 2 }]
      })
      expect(res.valid).toBe(false)
      expect(res.message).toContain('供应商')
    })

    it('should reject missing or inverted dates', () => {
      const res = validateReconDraft({
        supplierId: 1,
        periodStart: '2026-09-30',
        periodEnd: '2026-09-01',
        items: [{ lineType: 'PROCESS', unitPrice: 2 }]
      })
      expect(res.valid).toBe(false)
      expect(res.message).toContain('对账起始日期不能晚于截止日期')
    })

    it('should reject empty items list', () => {
      const res = validateReconDraft({
        supplierId: 1,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: []
      })
      expect(res.valid).toBe(false)
      expect(res.message).toContain('无可对账明细')
    })

    it('should reject ADJUST row missing reason or zero amount', () => {
      const res1 = validateReconDraft({
        supplierId: 1,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: [{ lineType: 'ADJUST', lineAmount: -100, adjustReason: '' }]
      })
      expect(res1.valid).toBe(false)
      expect(res1.message).toContain('原因备注')

      const res2 = validateReconDraft({
        supplierId: 1,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: [{ lineType: 'ADJUST', lineAmount: 0, adjustReason: '扣款' }]
      })
      expect(res2.valid).toBe(false)
      expect(res2.message).toContain('金额不能为 0')
    })

    it('should pass valid draft data', () => {
      const res = validateReconDraft({
        supplierId: 1,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: [
          { lineType: 'PROCESS', unitPrice: 2.5 },
          { lineType: 'ADJUST', lineAmount: -50, adjustReason: '瑕疵扣款' }
        ]
      })
      expect(res.valid).toBe(true)
    })
  })

  describe('Error Message Parsing and Error Codes', () => {
    it('parseReconErrorMessage should detect 004 occupied fingerprint error', () => {
      const err = { code: 1030022004, msg: '源明细已被对账单【SR20261015001】引用' }
      const res = parseReconErrorMessage(err)
      expect(res.isReferenced).toBe(true)
      expect(res.message).toContain('SR20261015001')
    })

    it('parseReconErrorMessage should detect 006 payment apply blocking cancel', () => {
      const err = { code: 1030022006, msg: '对账单存在付款申请，禁止作废' }
      const res = parseReconErrorMessage(err)
      expect(res.isReferenced).toBe(false)
      expect(res.message).toContain('存在审批中或已审批的付款申请')
    })

    it('parseReconErrorMessage should detect 007 exceed confirmed amount', () => {
      const err = { code: 1030022007, msg: '累计申请金额【12000.00】超出对账确认金额【10000.00】' }
      const res = parseReconErrorMessage(err)
      expect(res.message).toContain('超出对账确认金额')
    })
  })

  describe('Print Utilities', () => {
    it('formatLineTypePrintLabel should map line types correctly', () => {
      expect(formatLineTypePrintLabel('PROCESS')).toBe('加工费')
      expect(formatLineTypePrintLabel('PURCHASE')).toBe('采购货款')
      expect(formatLineTypePrintLabel('ADJUST')).toBe('调整项')
      expect(formatLineTypePrintLabel(undefined)).toBe('—')
    })

    it('formatInspectionTypeLabel should map inspection types correctly', () => {
      expect(formatInspectionTypeLabel('GREIGE')).toBe('胚布检验 (织费)')
      expect(formatInspectionTypeLabel('FINISHED')).toBe('成品检验 (染费)')
    })

    it('formatPricingBasisLabel should map pricing basis correctly', () => {
      expect(formatPricingBasisLabel('METER')).toBe('米 (M)')
      expect(formatPricingBasisLabel('KG')).toBe('千克 (KG)')
    })

    it('calculatePrintSubtotals should sum process, purchase, adjust and grandTotal', () => {
      const items = [
        { lineType: 'PROCESS', lineAmount: 5000 },
        { lineType: 'PURCHASE', lineAmount: 3000 },
        { lineType: 'ADJUST', lineAmount: -200 }
      ]
      const subtotals = calculatePrintSubtotals(items)
      expect(subtotals.processTotal).toBe(5000.0)
      expect(subtotals.purchaseTotal).toBe(3000.0)
      expect(subtotals.adjustTotal).toBe(-200.0)
      expect(subtotals.grandTotal).toBe(7800.0)
    })

    it('formatPrintTime should format time string or fallback to current time', () => {
      expect(formatPrintTime('2026-10-15 12:00:00')).toBe('2026-10-15 12:00:00')
      expect(formatPrintTime(null)).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    })
  })

  describe('API Methods Contract', () => {
    it('previewSupplierRecon should send POST to /lunaerp/finance/supplier-recon/preview', async () => {
      const payload = { supplierId: 1, periodStart: '2026-09-01', periodEnd: '2026-09-30' }
      await SupplierReconApi.previewSupplierRecon(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/preview',
        data: payload
      })
    })

    it('createSupplierRecon should send POST to /lunaerp/finance/supplier-recon/create', async () => {
      const payload = {
        supplierId: 1,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        items: []
      }
      await SupplierReconApi.createSupplierRecon(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/create',
        data: payload
      })
    })

    it('confirmSupplierRecon should send PUT to /lunaerp/finance/supplier-recon/confirm?id=123', async () => {
      await SupplierReconApi.confirmSupplierRecon(123)
      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/confirm?id=123'
      })
    })

    it('cancelSupplierRecon should send PUT to /lunaerp/finance/supplier-recon/cancel?id=123', async () => {
      await SupplierReconApi.cancelSupplierRecon(123)
      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/cancel?id=123'
      })
    })

    it('getSupplierReconPrintData should send GET to /lunaerp/finance/supplier-recon/print-data?id=123', async () => {
      await SupplierReconApi.getSupplierReconPrintData(123)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/print-data?id=123'
      })
    })
  })
})
