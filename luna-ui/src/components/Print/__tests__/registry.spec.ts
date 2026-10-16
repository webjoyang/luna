import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/lunaerp-elmui/production/weaving-ticket', () => ({
  getWeavingTicketPrintData: vi.fn()
}))

vi.mock(
  '@/views/lunaerp-elmui/production/weaving-order/components/print/WeavingTicketLabel.vue',
  () => ({
    default: { name: 'WeavingTicketLabel' }
  })
)

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

vi.mock(
  '@/views/lunaerp-elmui/production/color-instruction/components/print/ColorInstructionSheet.vue',
  () => ({
    default: { name: 'ColorInstructionSheet' }
  })
)

vi.mock('@/api/lunaerp-elmui/finance/supplier-recon', () => ({
  getSupplierReconPrintData: vi.fn()
}))

vi.mock(
  '@/views/lunaerp-elmui/finance/supplier-recon/components/print/SupplierReconSheet.vue',
  () => ({
    default: { name: 'SupplierReconSheet' }
  })
)

import {
  getTemplatesByBizType,
  getTemplate,
  registerPrintTemplate,
  PrintTemplateReg
} from '../registry'

describe('Print Template Registry', () => {
  it('should have WEAVING_TICKET_LABEL statically registered', () => {
    const weavingTemplates = getTemplatesByBizType('WEAVING_TICKET')
    expect(weavingTemplates.length).toBeGreaterThanOrEqual(1)

    const labelTemplate = weavingTemplates.find((t) => t.templateCode === 'WEAVING_TICKET_LABEL')
    expect(labelTemplate).toBeDefined()
    expect(labelTemplate?.paper).toBe('LABEL-100x60')
    expect(labelTemplate?.permission).toBe('lunaerp:production:weaving-ticket:print')
    expect(typeof labelTemplate?.fetchApi).toBe('function')
  })

  it('should have SHIPMENT_DOC and PACKING_LIST statically registered for SHIPMENT', () => {
    const shipmentTemplates = getTemplatesByBizType('SHIPMENT')
    expect(shipmentTemplates).toHaveLength(2)

    const docTemplate = shipmentTemplates.find((t) => t.templateCode === 'SHIPMENT_DOC')
    expect(docTemplate).toBeDefined()
    expect(docTemplate?.name).toBe('随货发货单')
    expect(docTemplate?.paper).toBe('A4')
    expect(docTemplate?.permission).toBe('lunaerp:business:shipment:print')
    expect(typeof docTemplate?.fetchApi).toBe('function')

    const packingTemplate = shipmentTemplates.find((t) => t.templateCode === 'PACKING_LIST')
    expect(packingTemplate).toBeDefined()
    expect(packingTemplate?.name).toBe('装箱清单（码单）')
    expect(packingTemplate?.paper).toBe('A4')
    expect(packingTemplate?.permission).toBe('lunaerp:business:shipment:print')
    expect(typeof packingTemplate?.fetchApi).toBe('function')
  })

  it('should have EXPORT_DOC_PI, EXPORT_DOC_CI, and EXPORT_DOC_PL statically registered for EXPORT_DOC', () => {
    const exportDocTemplates = getTemplatesByBizType('EXPORT_DOC')
    expect(exportDocTemplates).toHaveLength(3)

    const piTemplate = exportDocTemplates.find((t) => t.templateCode === 'EXPORT_DOC_PI')
    expect(piTemplate).toBeDefined()
    expect(piTemplate?.name).toBe('形式发票 (PI)')
    expect(piTemplate?.paper).toBe('A4')
    expect(piTemplate?.permission).toBe('lunaerp:business:export-doc:print')

    const ciTemplate = exportDocTemplates.find((t) => t.templateCode === 'EXPORT_DOC_CI')
    expect(ciTemplate).toBeDefined()
    expect(ciTemplate?.name).toBe('商业发票 (CI)')
    expect(ciTemplate?.paper).toBe('A4')
    expect(ciTemplate?.permission).toBe('lunaerp:business:export-doc:print')

    const plTemplate = exportDocTemplates.find((t) => t.templateCode === 'EXPORT_DOC_PL')
    expect(plTemplate).toBeDefined()
    expect(plTemplate?.name).toBe('装箱单 (PL)')
    expect(plTemplate?.paper).toBe('A4')
    expect(plTemplate?.permission).toBe('lunaerp:business:export-doc:print')
  })

  it('should have quotation-default statically registered for QUOTATION', () => {
    const quotationTemplates = getTemplatesByBizType('QUOTATION')
    expect(quotationTemplates).toHaveLength(1)

    const tpl = quotationTemplates[0]
    expect(tpl.templateCode).toBe('quotation-default')
    expect(tpl.name).toBe('报价单')
    expect(tpl.paper).toBe('A4')
    expect(tpl.permission).toBe('lunaerp:business:quotation:print')
    expect(typeof tpl.fetchApi).toBe('function')
  })

  it('should have COLOR_INSTRUCTION_SHEET statically registered for COLOR_INSTRUCTION', () => {
    const colorTemplates = getTemplatesByBizType('COLOR_INSTRUCTION')
    expect(colorTemplates).toHaveLength(1)

    const tpl = colorTemplates[0]
    expect(tpl.templateCode).toBe('COLOR_INSTRUCTION_SHEET')
    expect(tpl.name).toBe('打色指示单')
    expect(tpl.paper).toBe('A4')
    expect(tpl.permission).toBe('lunaerp:production:color-instruction:print')
    expect(typeof tpl.fetchApi).toBe('function')
    expect(typeof tpl.getDocNo).toBe('function')
    expect(tpl.getDocNo?.({ instructionNo: 'DS20261014001' })).toBe('DS20261014001')
  })

  it('should have VENDOR_RECON_SHEET statically registered for VENDOR_RECON', () => {
    const reconTemplates = getTemplatesByBizType('VENDOR_RECON')
    expect(reconTemplates).toHaveLength(1)

    const tpl = reconTemplates[0]
    expect(tpl.templateCode).toBe('VENDOR_RECON_SHEET')
    expect(tpl.name).toBe('供应商对账单')
    expect(tpl.paper).toBe('A4')
    expect(tpl.permission).toBe('lunaerp:finance:supplierrecon:print')
    expect(typeof tpl.fetchApi).toBe('function')
    expect(typeof tpl.getDocNo).toBe('function')
    expect(tpl.getDocNo?.({ reconNo: 'SR20261015001' })).toBe('SR20261015001')
  })

  it('getTemplate should return default or specific template', () => {
    const defaultTpl = getTemplate('WEAVING_TICKET')
    expect(defaultTpl).toBeDefined()
    expect(defaultTpl?.templateCode).toBe('WEAVING_TICKET_LABEL')

    const specificTpl = getTemplate('WEAVING_TICKET', 'WEAVING_TICKET_LABEL')
    expect(specificTpl?.templateCode).toBe('WEAVING_TICKET_LABEL')

    const nonExistent = getTemplate('NON_EXISTENT_BIZ')
    expect(nonExistent).toBeUndefined()
  })

  it('registerPrintTemplate should allow registering new templates dynamically', () => {
    const dummyComponent = { name: 'DummyDoc' } as any
    const dummyTemplate: PrintTemplateReg = {
      bizType: 'DUMMY_BIZ',
      templateCode: 'DUMMY_DOC',
      name: '测试文档',
      paper: 'A4',
      permission: 'lunaerp:dummy:print',
      component: dummyComponent,
      fetchApi: async () => ({})
    }

    registerPrintTemplate(dummyTemplate)
    const found = getTemplate('DUMMY_BIZ', 'DUMMY_DOC')
    expect(found).toBeDefined()
    expect(found?.name).toBe('测试文档')
    expect(found?.paper).toBe('A4')
  })
})
