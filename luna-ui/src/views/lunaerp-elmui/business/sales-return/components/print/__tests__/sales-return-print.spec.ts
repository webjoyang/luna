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
import {
  computeSalesReturnTotalQtyText,
  formatStatusText,
  formatApprovalStatusText,
  formatReasonText,
  formatRollQty,
  calcTotalPieces,
  calcTotalAmount,
  formatChineseAmount
} from '../utils'

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

describe('Sales Return Print Utils (coding-standards §23 Multi-Unit & Static Dicts)', () => {
  it('跨单位退货物理量分组汇总：多单位并存时按单位独立分组（如 100.00 KG / 50.00 M），严禁无量纲混加', () => {
    const items = [
      { unit: 'KG', returnQty: 60.5 },
      { unit: 'KG', returnQty: 39.5 },
      { unit: 'M', returnQty: 50.0 }
    ]
    const text = computeSalesReturnTotalQtyText(items)
    expect(text).toBe('100.00 KG / 50.00 M')
  })

  it('单单位退货物理量汇总：单单位时正常汇总', () => {
    const items = [
      { unit: 'KG', returnQty: 30 },
      { unit: 'KG', returnQty: 20 }
    ]
    expect(computeSalesReturnTotalQtyText(items)).toBe('50.00 KG')
  })

  it('空明细列表时返回 0.00', () => {
    expect(computeSalesReturnTotalQtyText([])).toBe('0.00')
    expect(computeSalesReturnTotalQtyText(null)).toBe('0.00')
  })

  it('状态与审批状态文本映射使用静态字典常量正确转换', () => {
    expect(formatStatusText('DRAFT')).toBe('草稿')
    expect(formatStatusText('APPROVED')).toBe('已审批')
    expect(formatStatusText('RECEIVED')).toBe('已收货')
    expect(formatStatusText('POSTED')).toBe('已过账')
    expect(formatStatusText('CANCELLED')).toBe('已作废')
    expect(formatStatusText(undefined)).toBe('—')

    expect(formatApprovalStatusText('PENDING')).toBe('待提交')
    expect(formatApprovalStatusText('APPROVING')).toBe('审批中')
    expect(formatApprovalStatusText('APPROVED')).toBe('已通过')
    expect(formatApprovalStatusText('REJECTED')).toBe('已驳回')
    expect(formatApprovalStatusText('CANCELLED')).toBe('已撤回')
    expect(formatApprovalStatusText(undefined)).toBe('')

    expect(formatReasonText('QUALITY')).toBe('质量问题')
    expect(formatReasonText('WRONG_GOODS')).toBe('发错货')
    expect(formatReasonText('CUSTOMER_CANCEL')).toBe('客户取消')
    expect(formatReasonText('EXCESS')).toBe('多发退回')
    expect(formatReasonText('OTHER')).toBe('其他原因')
    expect(formatReasonText(undefined)).toBe('—')
  })

  it('卷数量与大写金额格式化正确', () => {
    expect(formatRollQty('KG', { rollNo: 'R1', weight: 45.2 })).toBe('45.2kg')
    expect(formatRollQty('M', { rollNo: 'R2', lengthM: 100.5 })).toBe('100.5m')
    expect(formatChineseAmount(1250)).toBe('壹仟贰佰伍拾元整')
    expect(formatChineseAmount(0)).toBe('零元整')
  })
})
