import { defineAsyncComponent } from 'vue'
import type { Component } from 'vue'
import { getWeavingTicketPrintData } from '@/api/lunaerp-elmui/production/weaving-ticket'
import WeavingTicketLabel from '@/views/lunaerp-elmui/production/weaving-order/components/print/WeavingTicketLabel.vue'
import { getShipmentPrintData } from '@/api/lunaerp-elmui/business/shipment'
import ShipmentDoc from '@/views/lunaerp-elmui/business/shipment/components/print/ShipmentDoc.vue'
import PackingList from '@/views/lunaerp-elmui/business/shipment/components/print/PackingList.vue'
import { shipmentPrintCss } from '@/views/lunaerp-elmui/business/shipment/components/print/print-styles'
import { getExportDocPrintData } from '@/api/lunaerp-elmui/business/export-doc'
import ExportDocPrintPi from '@/views/lunaerp-elmui/business/export-doc/components/print/ExportDocPrintPi.vue'
import ExportDocPrintCi from '@/views/lunaerp-elmui/business/export-doc/components/print/ExportDocPrintCi.vue'
import ExportDocPrintPl from '@/views/lunaerp-elmui/business/export-doc/components/print/ExportDocPrintPl.vue'
import { exportDocPrintCss } from '@/views/lunaerp-elmui/business/export-doc/components/print/print-styles'
import { getPrintData as getQuotationPrintData } from '@/api/lunaerp/business/quotation'
import QuotationPrintTemplate from '@/views/lunaerp/business/quotation/print/QuotationPrintTemplate.vue'
import { getColorInstructionPrintData } from '@/api/lunaerp-elmui/production/color-instruction'
import ColorInstructionSheet from '@/views/lunaerp-elmui/production/color-instruction/components/print/ColorInstructionSheet.vue'
import { colorInstructionPrintCss } from '@/views/lunaerp-elmui/production/color-instruction/components/print/print-styles'
import { supplierReconPrintCss } from '@/views/lunaerp-elmui/finance/supplier-recon/components/print/print-styles'
import { customerStatementPrintCss } from '@/views/lunaerp-elmui/finance/customer-reconciliation/components/print/print-styles'

/**
 * 打印模板注册项接口（规范见 W0 spec）
 */
export interface PrintTemplateReg {
  /** 业务对象类型，与回收站/消息的 bizType 同字典，如 WEAVING_TICKET, SHIPMENT */
  bizType: string
  /** 模板编码，如 WEAVING_TICKET_LABEL / SHIPMENT_DOC / PACKING_LIST */
  templateCode: string
  /** 版式名（切换与按钮文案） */
  name: string
  /** 纸张尺寸 */
  paper: 'A4' | 'LABEL-100x60'
  /** 权限码 */
  permission: string
  /** 模板组件 */
  component: Component
  /** 后端 print-data 取数 API */
  fetchApi: (bizId: string | number) => Promise<any>
  /** 模板特定打印 CSS（注入独立打印 iframe） */
  customCss?: string
  /** 可选：提取单据编号（用于弹窗标题展示） */
  getDocNo?: (data: any) => string
}

/**
 * 静态模板注册表（不做自动发现、不做模板设计器）
 *
 * 【Vitest 纯 Node 测试守卫规约】：
 * 新增业务模板必须统一使用 `defineAsyncComponent(() => import(...))` 与动态 `fetchApi: (id) => import('@/api/...').then(...)`。
 * 严禁在文件顶层静态 import Vue SFC 组件或带路由副作用的 API，避免破坏 Vitest 纯 Node 测试环境。
 */
export const printTemplateRegistry: PrintTemplateReg[] = [
  {
    bizType: 'WEAVING_TICKET',
    templateCode: 'WEAVING_TICKET_LABEL',
    name: '布票标签 (100×60mm)',
    paper: 'LABEL-100x60',
    permission: 'lunaerp:production:weaving-ticket:print',
    component: WeavingTicketLabel,
    fetchApi: (bizId: string | number) => getWeavingTicketPrintData(bizId)
  },
  {
    bizType: 'SHIPMENT',
    templateCode: 'SHIPMENT_DOC',
    name: '随货发货单',
    paper: 'A4',
    permission: 'lunaerp:business:shipment:print',
    component: ShipmentDoc,
    fetchApi: (bizId: string | number) => getShipmentPrintData(bizId),
    customCss: shipmentPrintCss
  },
  {
    bizType: 'SHIPMENT',
    templateCode: 'PACKING_LIST',
    name: '装箱清单（码单）',
    paper: 'A4',
    permission: 'lunaerp:business:shipment:print',
    component: PackingList,
    fetchApi: (bizId: string | number) => getShipmentPrintData(bizId),
    customCss: shipmentPrintCss
  },
  {
    bizType: 'EXPORT_DOC',
    templateCode: 'EXPORT_DOC_PI',
    name: '形式发票 (PI)',
    paper: 'A4',
    permission: 'lunaerp:business:export-doc:print',
    component: ExportDocPrintPi,
    fetchApi: (bizId: string | number) => getExportDocPrintData(bizId, 'PI'),
    customCss: exportDocPrintCss
  },
  {
    bizType: 'EXPORT_DOC',
    templateCode: 'EXPORT_DOC_CI',
    name: '商业发票 (CI)',
    paper: 'A4',
    permission: 'lunaerp:business:export-doc:print',
    component: ExportDocPrintCi,
    fetchApi: (bizId: string | number) => getExportDocPrintData(bizId, 'CI'),
    customCss: exportDocPrintCss
  },
  {
    bizType: 'EXPORT_DOC',
    templateCode: 'EXPORT_DOC_PL',
    name: '装箱单 (PL)',
    paper: 'A4',
    permission: 'lunaerp:business:export-doc:print',
    component: ExportDocPrintPl,
    fetchApi: (bizId: string | number) => getExportDocPrintData(bizId, 'PL'),
    customCss: exportDocPrintCss
  },
  {
    bizType: 'QUOTATION',
    templateCode: 'quotation-default',
    name: '报价单',
    paper: 'A4',
    permission: 'lunaerp:business:quotation:print',
    component: QuotationPrintTemplate,
    fetchApi: (bizId: string | number) => getQuotationPrintData(Number(bizId))
  },
  {
    bizType: 'COLOR_INSTRUCTION',
    templateCode: 'COLOR_INSTRUCTION_SHEET',
    name: '打色指示单',
    paper: 'A4',
    permission: 'lunaerp:production:color-instruction:print',
    component: ColorInstructionSheet,
    fetchApi: (bizId: string | number) => getColorInstructionPrintData(bizId),
    customCss: colorInstructionPrintCss,
    getDocNo: (data: any) => data?.instructionNo || ''
  },
  {
    bizType: 'VENDOR_RECON',
    templateCode: 'VENDOR_RECON_SHEET',
    name: '供应商对账单',
    paper: 'A4',
    permission: 'lunaerp:finance:supplierrecon:print',
    component: defineAsyncComponent(
      () =>
        import('@/views/lunaerp-elmui/finance/supplier-recon/components/print/SupplierReconSheet.vue')
    ),
    fetchApi: (bizId: string | number) =>
      import('@/api/lunaerp-elmui/finance/supplier-recon').then((m) =>
        m.getSupplierReconPrintData(Number(bizId))
      ),
    customCss: supplierReconPrintCss,
    getDocNo: (data: any) => data?.reconNo || ''
  },
  {
    bizType: 'FINANCE_STATEMENT',
    templateCode: 'FINANCE_STATEMENT',
    name: '客户对账单',
    paper: 'A4',
    permission: 'lunaerp:finance:customerrecon:print',
    component: defineAsyncComponent(
      () =>
        import('@/views/lunaerp-elmui/finance/customer-reconciliation/components/print/CustomerStatementSheet.vue')
    ),
    fetchApi: (bizId: string | number) =>
      import('@/api/lunaerp-elmui/finance/customer-reconciliation').then((m) =>
        m.getCustomerStatementPrintData(Number(bizId))
      ),
    customCss: customerStatementPrintCss,
    getDocNo: (data: any) => data?.statementNo || ''
  }
]

/**
 * 动态注册模板（供后续模块追加）
 */
export function registerPrintTemplate(template: PrintTemplateReg) {
  const existingIndex = printTemplateRegistry.findIndex(
    (item) => item.bizType === template.bizType && item.templateCode === template.templateCode
  )
  if (existingIndex >= 0) {
    printTemplateRegistry[existingIndex] = template
  } else {
    printTemplateRegistry.push(template)
  }
}

/**
 * 按 bizType 获取模板列表
 */
export function getTemplatesByBizType(bizType: string): PrintTemplateReg[] {
  return printTemplateRegistry.filter((item) => item.bizType === bizType)
}

/**
 * 获取指定模板（若未指定 templateCode 且只有单个模板，默认取首个）
 */
export function getTemplate(bizType: string, templateCode?: string): PrintTemplateReg | undefined {
  const templates = getTemplatesByBizType(bizType)
  if (templateCode) {
    return templates.find((item) => item.templateCode === templateCode)
  }
  return templates[0]
}
