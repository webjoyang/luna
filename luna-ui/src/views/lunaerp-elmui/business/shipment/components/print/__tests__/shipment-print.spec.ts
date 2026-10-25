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

vi.mock('@/router', () => ({
  default: {}
}))

vi.mock('@/api/lunaerp-elmui/production/color-instruction', () => ({
  getColorInstructionPrintData: vi.fn()
}))

vi.mock(
  '@/views/lunaerp-elmui/production/color-instruction/components/print/ColorInstructionSheet.vue',
  () => ({
    default: { name: 'ColorInstructionSheet' }
  })
)

import {
  formatRmbUppercase,
  computeGrandTotalAmount,
  computeTotalPieces,
  computeTotalQuantityText,
  flattenRollList,
  buildPortalShipmentQrUrl,
  formatPrintTime
} from '../utils'
import { getTemplatesByBizType, getTemplate } from '@/components/Print/registry'

describe('Shipment Print Utilities', () => {
  describe('Portal Shipment QR Code URL Format', () => {
    it('should format QR code URL correctly as {baseUrl}/portal/shipment?no={shipmentNo}', () => {
      const baseUrl = 'https://portal.lunaerp.com'
      const shipmentNo = 'SH-20260927-001'

      const qrUrl = buildPortalShipmentQrUrl(baseUrl, shipmentNo)
      expect(qrUrl).toBe('https://portal.lunaerp.com/portal/shipment?no=SH-20260927-001')
      expect(qrUrl).toContain('/portal/shipment?no=')
    })

    it('should trim trailing slashes from baseUrl', () => {
      const baseUrl = 'https://portal.lunaerp.com///'
      const shipmentNo = 'SH-20260927-002'

      const qrUrl = buildPortalShipmentQrUrl(baseUrl, shipmentNo)
      expect(qrUrl).toBe('https://portal.lunaerp.com/portal/shipment?no=SH-20260927-002')
    })

    it('should return empty string when baseUrl is empty or null', () => {
      expect(buildPortalShipmentQrUrl('', 'SH-20260927-001')).toBe('')
      expect(buildPortalShipmentQrUrl(null, 'SH-20260927-001')).toBe('')
      expect(buildPortalShipmentQrUrl('   ', 'SH-20260927-001')).toBe('')
    })
  })

  describe('RMB Uppercase Conversion', () => {
    it('should convert 0 to 零元整', () => {
      expect(formatRmbUppercase(0)).toBe('零元整')
      expect(formatRmbUppercase(null)).toBe('零元整')
    })

    it('should convert integer amounts correctly', () => {
      expect(formatRmbUppercase(5000)).toBe('伍仟元整')
      expect(formatRmbUppercase(100)).toBe('壹佰元整')
      expect(formatRmbUppercase(10000)).toBe('壹万元整')
    })

    it('should convert decimal amounts with jiao and fen correctly', () => {
      expect(formatRmbUppercase(5150.25)).toBe('伍仟壹佰伍拾元贰角伍分')
      expect(formatRmbUppercase(5150.2)).toBe('伍仟壹佰伍拾元贰角')
      expect(formatRmbUppercase(5150.05)).toBe('伍仟壹佰伍拾元零伍分')
    })
  })

  describe('Amount and Pieces Computations', () => {
    it('should compute grand total amount including freight', () => {
      const shipment: any = {
        totalAmount: 5000,
        freightFee: 150
      }
      expect(computeGrandTotalAmount(shipment)).toBe(5150)
    })

    it('should compute total pieces fallback to items sum if not on shipment', () => {
      const shipment: any = {
        totalPieces: null,
        items: [
          { pieces: 3 },
          { pieces: 7 }
        ]
      }
      expect(computeTotalPieces(shipment)).toBe(10)

      const shipmentWithDirect: any = {
        totalPieces: 15,
        items: [{ pieces: 3 }]
      }
      expect(computeTotalPieces(shipmentWithDirect)).toBe(15)
    })

    it('should format total quantity text grouped by unit', () => {
      const items: any[] = [
        { quantity: 100, unit: 'KG' },
        { quantity: 50, unit: 'KG' },
        { quantity: 200, unit: 'M' }
      ]
      const text = computeTotalQuantityText(items)
      expect(text).toContain('150.00 KG')
      expect(text).toContain('200.00 M')
      expect(text).toContain(' / ')
    })
  })

  describe('Roll List Flattening for Packing List', () => {
    it('should flatten nested rolls into roll items', () => {
      const items: any[] = [
        {
          productName: '斜纹布',
          productCode: 'PRD-001',
          customerColorNo: 'WH-01',
          vatNo: 'LOT-01',
          rolls: [
            { rollNo: 'R-01', weight: 25, lengthM: 50 },
            { rollNo: 'R-02', weight: 26, lengthM: 52 }
          ]
        }
      ]
      const rolls = flattenRollList(items)
      expect(rolls).toHaveLength(2)
      expect(rolls[0].rollNo).toBe('R-01')
      expect(rolls[0].productName).toBe('斜纹布')
      expect(rolls[1].rollNo).toBe('R-02')
    })

    it('should fallback to batch line when rolls are empty', () => {
      const items: any[] = [
        {
          productName: '平纹布',
          productCode: 'PRD-002',
          pieces: 5,
          quantity: 125,
          rolls: []
        }
      ]
      const rolls = flattenRollList(items)
      expect(rolls).toHaveLength(1)
      expect(rolls[0].rollNo).toContain('整批发货 (5匹)')
      expect(rolls[0].weight).toBe(125)
    })
  })

  describe('Print Time Formatting', () => {
    it('should format valid date strings', () => {
      const timeStr = formatPrintTime('2026-10-09T10:30:00')
      expect(timeStr).toContain('2026-10-09 10:30:00')
    })
  })
})

describe('Shipment Print Templates Registry', () => {
  it('should register both SHIPMENT_DOC and PACKING_LIST under SHIPMENT bizType', () => {
    const templates = getTemplatesByBizType('SHIPMENT')
    expect(templates).toHaveLength(2)

    const docTemplate = templates.find((t) => t.templateCode === 'SHIPMENT_DOC')
    expect(docTemplate).toBeDefined()
    expect(docTemplate?.name).toBe('随货发货单')
    expect(docTemplate?.paper).toBe('A4')
    expect(docTemplate?.permission).toBe('lunaerp:business:shipment:print')
    expect(typeof docTemplate?.fetchApi).toBe('function')

    const packingTemplate = templates.find((t) => t.templateCode === 'PACKING_LIST')
    expect(packingTemplate).toBeDefined()
    expect(packingTemplate?.name).toBe('装箱清单（码单）')
    expect(packingTemplate?.paper).toBe('A4')
    expect(packingTemplate?.permission).toBe('lunaerp:business:shipment:print')
    expect(typeof packingTemplate?.fetchApi).toBe('function')
  })

  it('getTemplate should return SHIPMENT_DOC by default for SHIPMENT', () => {
    const defaultTpl = getTemplate('SHIPMENT')
    expect(defaultTpl).toBeDefined()
    expect(defaultTpl?.templateCode).toBe('SHIPMENT_DOC')

    const packingTpl = getTemplate('SHIPMENT', 'PACKING_LIST')
    expect(packingTpl?.templateCode).toBe('PACKING_LIST')
  })
})
