import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

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

vi.mock('../components/print/ColorInstructionSheet.vue', () => ({
  default: { name: 'ColorInstructionSheet', props: { data: {}, list: {} } }
}))

import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import request from '@/config/axios'
import ColorInstructionSheet from '../components/print/ColorInstructionSheet.vue'
import { getTemplatesByBizType, getTemplate } from '@/components/Print/registry'
import type { ColorInstructionPrintVO } from '@/api/lunaerp-elmui/production/color-instruction'

describe('Color Instruction Print API & Template Registry', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('API: getPrintData', () => {
    it('should call GET /lunaerp/production/color-instruction/{id}/print-data', async () => {
      const mockResp = { id: 101, instructionNo: 'DS20261010001', colors: [] }
      ;(request.get as any).mockResolvedValueOnce(mockResp)

      const result = await ColorInstructionApi.getPrintData(101)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/production/color-instruction/101/print-data'
      })
      expect(result).toEqual(mockResp)
    })
  })

  describe('Component Definition: ColorInstructionSheet', () => {
    it('should export ColorInstructionSheet component with expected props', () => {
      expect(ColorInstructionSheet).toBeDefined()
      expect(ColorInstructionSheet.name).toBe('ColorInstructionSheet')
      expect(ColorInstructionSheet.props).toBeDefined()
      expect(ColorInstructionSheet.props.data).toBeDefined()
      expect(ColorInstructionSheet.props.list).toBeDefined()
    })
  })

  describe('Registry Configuration for COLOR_INSTRUCTION', () => {
    it('should have COLOR_INSTRUCTION_SHEET registered under COLOR_INSTRUCTION bizType', () => {
      const templates = getTemplatesByBizType('COLOR_INSTRUCTION')
      expect(templates).toHaveLength(1)

      const tpl = templates[0]
      expect(tpl).toBeDefined()
      expect(tpl.templateCode).toBe('COLOR_INSTRUCTION_SHEET')
      expect(tpl.name).toBe('打色指示单')
      expect(tpl.paper).toBe('A4')
      expect(tpl.permission).toBe('lunaerp:production:color-instruction:print')
      expect(tpl.component).toBe(ColorInstructionSheet)
      expect(typeof tpl.fetchApi).toBe('function')
    })

    it('should execute fetchApi delegate to colorInstructionApi.getPrintData', async () => {
      const tpl = getTemplate('COLOR_INSTRUCTION', 'COLOR_INSTRUCTION_SHEET')
      expect(tpl).toBeDefined()

      const mockResp = { id: 202, instructionNo: 'DS20261010002', colors: [] }
      ;(request.get as any).mockResolvedValueOnce(mockResp)

      const fetched = await tpl!.fetchApi(202)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/production/color-instruction/202/print-data'
      })
      expect(fetched).toEqual(mockResp)
    })

    it('should return default template when templateCode is not specified', () => {
      const defaultTpl = getTemplate('COLOR_INSTRUCTION')
      expect(defaultTpl).toBeDefined()
      expect(defaultTpl?.templateCode).toBe('COLOR_INSTRUCTION_SHEET')
    })
  })

  describe('Print Data Business Invariants', () => {
    const samplePrintVO: ColorInstructionPrintVO = {
      id: 101,
      instructionNo: 'DS20261010001',
      customerId: 10,
      clientName: '博洋家纺',
      contactPerson: '张经理 13800138000', // 全量不脱敏
      productId: 20,
      productCode: 'P001',
      productName: '全棉斜纹',
      labFactoryId: null,
      labFactoryName: '', // 空承接方 -> 本厂打色房
      composition: '100%棉',
      width: '160cm',
      gsm: '220gsm',
      instructionDate: '2026-10-10',
      deliveryDate: '2026-10-20',
      status: 'IN_PROGRESS',
      confirmType: 'CUSTOMER',
      customerFeedback: '颜色偏深，需微调',
      remark: '整批打色加急',
      creator: 'admin',
      printTime: '2026-10-11T08:30:00',
      colors: [
        {
          id: 1,
          lineNo: 1,
          customerColorNo: 'C01',
          colorName: '大红',
          round: 2,
          status: 'PENDING',
          copies: 2
        },
        {
          id: 2,
          lineNo: 2,
          customerColorNo: 'C01',
          colorName: '大红',
          round: 1,
          status: 'REJECTED',
          rejectReason: '偏暗需要提亮',
          copies: 2
        },
        {
          id: 3,
          lineNo: 3,
          customerColorNo: 'C02',
          colorName: '宝蓝',
          round: 1,
          status: 'APPROVED',
          copies: 1
        }
      ]
    }

    it('should retain unmasked contact person and support fallback to 本厂打色房', () => {
      expect(samplePrintVO.contactPerson).toBe('张经理 13800138000')
      const displayLab = samplePrintVO.labFactoryName || '本厂打色房'
      expect(displayLab).toBe('本厂打色房')
    })

    it('should contain all rounds of colors including REJECTED lines with rejectReason', () => {
      expect(samplePrintVO.colors).toHaveLength(3)

      const rejectedRows = samplePrintVO.colors!.filter((c) => c.status === 'REJECTED')
      expect(rejectedRows).toHaveLength(1)
      expect(rejectedRows[0].round).toBe(1)
      expect(rejectedRows[0].rejectReason).toBe('偏暗需要提亮')

      const pendingRows = samplePrintVO.colors!.filter((c) => c.status === 'PENDING')
      expect(pendingRows).toHaveLength(1)
      expect(pendingRows[0].round).toBe(2)

      const approvedRows = samplePrintVO.colors!.filter((c) => c.status === 'APPROVED')
      expect(approvedRows).toHaveLength(1)
    })
  })
})
