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

vi.mock('@/api/lunaerp-elmui/finance/customer-reconciliation', () => ({
  getCustomerStatementPrintData: vi.fn()
}))

vi.mock(
  '@/views/lunaerp-elmui/finance/customer-reconciliation/components/print/CustomerStatementSheet.vue',
  () => ({
    default: { name: 'CustomerStatementSheet' }
  })
)

import { getTemplatesByBizType, getTemplate } from '@/components/Print/registry'
import { salesReturnPrintCss } from '../print-styles'

describe('Sales Return Print Template Registration', () => {
  it('正确注册 SALES_RETURN_DOC 打印模板至 04 模板注册表', () => {
    const templates = getTemplatesByBizType('SALES_RETURN')
    expect(templates).toHaveLength(1)

    const tpl = templates[0]
    expect(tpl.bizType).toBe('SALES_RETURN')
    expect(tpl.templateCode).toBe('SALES_RETURN_DOC')
    expect(tpl.name).toBe('销售退货单')
    expect(tpl.paper).toBe('A4')
    expect(tpl.permission).toBe('lunaerp:business:sales-return:print')
    expect(typeof tpl.fetchApi).toBe('function')
    expect(typeof tpl.getDocNo).toBe('function')
    expect(tpl.getDocNo?.({ returnNo: 'RT-20261019-001' })).toBe('RT-20261019-001')
  })

  it('getTemplate 通过 templateCode 可获取到该模板', () => {
    const tpl = getTemplate('SALES_RETURN', 'SALES_RETURN_DOC')
    expect(tpl).toBeDefined()
    expect(tpl?.name).toBe('销售退货单')
  })

  it('模板自定义 CSS 包含 A4 纵向与草稿水印关键样式规则', () => {
    expect(salesReturnPrintCss).toContain('size: A4 portrait')
    expect(salesReturnPrintCss).toContain('.draft-watermark')
    expect(salesReturnPrintCss).toContain('.sales-return-doc-wrapper')
    expect(salesReturnPrintCss).toContain('.print-table')
  })
})
