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

vi.mock('@/views/lunaerp-elmui/business/export-doc/components/print/ExportDocPrintPi.vue', () => ({
  default: { name: 'ExportDocPrintPi' }
}))

vi.mock('@/views/lunaerp-elmui/business/export-doc/components/print/ExportDocPrintCi.vue', () => ({
  default: { name: 'ExportDocPrintCi' }
}))

vi.mock('@/views/lunaerp-elmui/business/export-doc/components/print/ExportDocPrintPl.vue', () => ({
  default: { name: 'ExportDocPrintPl' }
}))

vi.mock('@/api/lunaerp/business/quotation', () => ({
  getPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp/business/quotation/print/QuotationPrintTemplate.vue', () => ({
  default: { name: 'QuotationPrintTemplate' }
}))

vi.mock('@/api/lunaerp-elmui/production/color-instruction', () => ({
  getColorInstructionPrintData: vi.fn()
}))

vi.mock('../ColorInstructionSheet.vue', () => ({
  default: { name: 'ColorInstructionSheet' }
}))

import {
  formatStatusText,
  formatLineStatusText,
  formatConfirmType,
  formatProductInfo,
  formatLabFactoryName,
  isColorLineRejected,
  formatColorRound,
  formatRejectionText,
  formatPrintTime
} from '../utils'
import { getTemplatesByBizType, getTemplate } from '@/components/Print/registry'
import { colorInstructionPrintCss } from '../print-styles'
import ColorInstructionSheet from '../ColorInstructionSheet.vue'

describe('Color Instruction Print Template & Registry', () => {
  describe('Print Template Registry Integration', () => {
    it('should register COLOR_INSTRUCTION_SHEET under COLOR_INSTRUCTION bizType', () => {
      const templates = getTemplatesByBizType('COLOR_INSTRUCTION')
      expect(templates).toHaveLength(1)

      const sheetTpl = templates[0]
      expect(sheetTpl.templateCode).toBe('COLOR_INSTRUCTION_SHEET')
      expect(sheetTpl.name).toBe('打色指示单')
      expect(sheetTpl.paper).toBe('A4')
      expect(sheetTpl.permission).toBe('lunaerp:production:color-instruction:print')
      expect(typeof sheetTpl.fetchApi).toBe('function')
      expect(sheetTpl.customCss).toBe(colorInstructionPrintCss)
      expect(sheetTpl.customCss).toContain('@page')
      expect(sheetTpl.customCss).toContain('rejected-row')
      expect(sheetTpl.customCss).toContain('strike-text')
    })

    it('getTemplate should retrieve COLOR_INSTRUCTION_SHEET by default for COLOR_INSTRUCTION', () => {
      const defaultTpl = getTemplate('COLOR_INSTRUCTION')
      expect(defaultTpl).toBeDefined()
      expect(defaultTpl?.templateCode).toBe('COLOR_INSTRUCTION_SHEET')
      expect(defaultTpl?.name).toBe('打色指示单')
    })
  })

  describe('Print Format & Data Mapping Utilities', () => {
    it('should map header status text correctly', () => {
      expect(formatStatusText('PENDING')).toBe('待打色')
      expect(formatStatusText('IN_PROGRESS')).toBe('打色中')
      expect(formatStatusText('CONFIRMED')).toBe('已确认')
      expect(formatStatusText('IN_PRODUCTION')).toBe('已投产')
      expect(formatStatusText('VOID')).toBe('已作废')
      expect(formatStatusText(undefined)).toBe('—')
    })

    it('should map color line status text correctly', () => {
      expect(formatLineStatusText('PENDING')).toBe('待结果')
      expect(formatLineStatusText('APPROVED')).toBe('通过')
      expect(formatLineStatusText('REJECTED')).toBe('驳回')
      expect(formatLineStatusText(undefined)).toBe('—')
    })

    it('should map confirmation type correctly', () => {
      expect(formatConfirmType('CUSTOMER')).toBe('客户确认')
      expect(formatConfirmType('INTERNAL')).toBe('内部确认')
      expect(formatConfirmType(undefined)).toBe('—')
    })

    it('should format product code and name', () => {
      expect(formatProductInfo({ productCode: 'P001', productName: '人棉弹力汗布' })).toBe('P001 / 人棉弹力汗布')
      expect(formatProductInfo({ productCode: 'P001' })).toBe('P001')
      expect(formatProductInfo({ productName: '人棉弹力汗布' })).toBe('人棉弹力汗布')
      expect(formatProductInfo(null)).toBe('—')
    })

    it('should format lab factory name with self-produce fallback', () => {
      expect(formatLabFactoryName({ labFactoryName: '绍兴XX打色房' })).toBe('绍兴XX打色房')
      expect(formatLabFactoryName({ labFactoryName: undefined })).toBe('本厂打色房（自做）')
      expect(formatLabFactoryName(null)).toBe('本厂打色房（自做）')
    })

    it('should identify rejected lines and format rejection text', () => {
      const rejectedLine = { status: 'REJECTED', round: 1, rejectReason: '偏红，色光不符' }
      expect(isColorLineRejected(rejectedLine)).toBe(true)
      expect(formatColorRound(1)).toBe('第 1 轮')
      expect(formatRejectionText(rejectedLine)).toBe('[已驳回 - 第 1 轮]：偏红，色光不符')

      const approvedLine = { status: 'APPROVED', round: 2 }
      expect(isColorLineRejected(approvedLine)).toBe(false)
      expect(formatColorRound(2)).toBe('第 2 轮')
      expect(formatRejectionText(approvedLine)).toBe('')
    })

    it('should format print time correctly', () => {
      const formatted = formatPrintTime('2026-10-14 11:20:00')
      expect(formatted).toBe('2026-10-14 11:20:00')
      const nowFormatted = formatPrintTime(null)
      expect(nowFormatted).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    })
  })

  describe('Component Definition', () => {
    it('ColorInstructionSheet component should have correct name', () => {
      expect(ColorInstructionSheet.name).toBe('ColorInstructionSheet')
    })
  })
})
