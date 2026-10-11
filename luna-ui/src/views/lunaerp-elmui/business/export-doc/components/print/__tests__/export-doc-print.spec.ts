import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/lunaerp-elmui/production/weaving-ticket', () => ({
  getWeavingTicketPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp-elmui/production/weaving-order/components/print/WeavingTicketLabel.vue', () => ({
  default: { name: 'WeavingTicketLabel' }
}))

vi.mock('@/api/lunaerp-elmui/business/shipment', () => ({
  getShipmentPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp-elmui/business/shipment/components/print/ShipmentDoc.vue', () => ({
  default: { name: 'ShipmentDoc' }
}))

vi.mock('@/views/lunaerp-elmui/business/shipment/components/print/PackingList.vue', () => ({
  default: { name: 'PackingList' }
}))

vi.mock('@/api/lunaerp-elmui/business/export-doc', () => ({
  getExportDocPrintData: vi.fn()
}))

vi.mock('../ExportDocPrintPi.vue', () => ({
  default: { name: 'ExportDocPrintPi' }
}))

vi.mock('../ExportDocPrintCi.vue', () => ({
  default: { name: 'ExportDocPrintCi' }
}))

vi.mock('../ExportDocPrintPl.vue', () => ({
  default: { name: 'ExportDocPrintPl' }
}))

vi.mock('@/api/lunaerp/business/quotation', () => ({
  getPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp/business/quotation/print/QuotationPrintTemplate.vue', () => ({
  default: { name: 'QuotationPrintTemplate' }
}))

vi.mock('@/api/lunaerp-elmui/production/color-instruction', () => ({
  getPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp-elmui/production/color-instruction/components/print/ColorInstructionSheet.vue', () => ({
  default: { name: 'ColorInstructionSheet' }
}))

vi.mock('@/api/lunaerp-elmui/finance/supplier-recon', () => ({
  getSupplierReconPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp-elmui/finance/supplier-recon/components/print/SupplierReconPrintTemplate.vue', () => ({
  default: { name: 'SupplierReconPrintTemplate' }
}))

import { getCurrencySymbol, formatAmount, safeParseJson, formatDate } from '../utils'
import { getTemplatesByBizType, getTemplate } from '@/components/Print/registry'

describe('Export Doc Print Utilities & Templates', () => {
  describe('Currency Symbols', () => {
    it('should map standard currencies to symbols correctly', () => {
      expect(getCurrencySymbol('USD')).toBe('$')
      expect(getCurrencySymbol('EUR')).toBe('€')
      expect(getCurrencySymbol('GBP')).toBe('£')
      expect(getCurrencySymbol('RMB')).toBe('¥')
      expect(getCurrencySymbol('CNY')).toBe('¥')
      expect(getCurrencySymbol('JPY')).toBe('¥')
      expect(getCurrencySymbol('HKD')).toBe('HK$')
      expect(getCurrencySymbol('AUD')).toBe('AUD ')
      expect(getCurrencySymbol(undefined)).toBe('$')
    })
  })

  describe('Amount & JSON Utilities', () => {
    it('should format amounts with standard decimals', () => {
      expect(formatAmount(1234.5)).toBe('1,234.50')
      expect(formatAmount(0)).toBe('0.00')
      expect(formatAmount(null)).toBe('0.00')
      expect(formatAmount(1.2345, 3)).toBe('1.235')
    })

    it('should safely parse JSON with fallback', () => {
      expect(safeParseJson('{"a":1}', {})).toEqual({ a: 1 })
      expect(safeParseJson('invalid-json', { fallback: true })).toEqual({ fallback: true })
      expect(safeParseJson(null, { fallback: true })).toEqual({ fallback: true })
    })

    it('should format dates properly', () => {
      expect(formatDate('2026-10-10T08:00:00')).toBe('2026-10-10')
      expect(formatDate(null)).toBe('')
    })
  })

  describe('Registry Configuration for EXPORT_DOC', () => {
    it('should have 3 templates registered for EXPORT_DOC', () => {
      const templates = getTemplatesByBizType('EXPORT_DOC')
      expect(templates).toHaveLength(3)
      const codes = templates.map((t) => t.templateCode)
      expect(codes).toContain('EXPORT_DOC_PI')
      expect(codes).toContain('EXPORT_DOC_CI')
      expect(codes).toContain('EXPORT_DOC_PL')
    })

    it('should retrieve specific export doc template by templateCode', () => {
      const pi = getTemplate('EXPORT_DOC', 'EXPORT_DOC_PI')
      expect(pi).toBeDefined()
      expect(pi?.paper).toBe('A4')
      expect(pi?.permission).toBe('lunaerp:business:export-doc:print')

      const ci = getTemplate('EXPORT_DOC', 'EXPORT_DOC_CI')
      expect(ci).toBeDefined()
      expect(ci?.paper).toBe('A4')

      const pl = getTemplate('EXPORT_DOC', 'EXPORT_DOC_PL')
      expect(pl).toBeDefined()
      expect(pl?.paper).toBe('A4')
    })
  })
})
