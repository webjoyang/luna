/**
 * 发票业务错误码（对齐 FinanceErrorCodeConstants 1-030-026-xxx）
 */
export const INVOICE_ERROR_CODES = {
  NOT_EXISTS: 1030026000,
  STATUS_NOT_ALLOW: 1030026001,
  EXCEED_SHIPMENT_AMOUNT: 1030026002,
  CURRENCY_MIXED: 1030026003,
  CUSTOMER_MISMATCH: 1030026004,
  OFFICIAL_NO_REQUIRED: 1030026005,
  OFFICIAL_NO_DUPLICATE: 1030026006,
  TAX_RATE_INVALID: 1030026007
} as const

/**
 * 开票税率选项列表（字典 2276）
 */
export const TAX_RATE_OPTIONS = [
  { label: '13% (纺织默认)', value: 13.0 },
  { label: '9%', value: 9.0 },
  { label: '6%', value: 6.0 },
  { label: '3%', value: 3.0 },
  { label: '1%', value: 1.0 },
  { label: '0% (外贸出口)', value: 0.0 },
  { label: '免税', value: 0.0 }
]

/**
 * 发票种类选项列表（字典 2275）
 */
export const INVOICE_KIND_OPTIONS = [
  { label: '数电专票', value: 'DIGITAL_SPECIAL' },
  { label: '数电普票', value: 'DIGITAL_NORMAL' },
  { label: '增值税专用发票', value: 'VAT_SPECIAL' },
  { label: '增值税普通发票', value: 'VAT_NORMAL' }
]

/**
 * 税额纯函数反算计算器（与后端 InvoiceTaxCalculator 逻辑对齐）
 *
 * 公式：
 * exclTax = Math.round((inclTax / (1 + taxRate / 100)) * 100) / 100
 * tax = Math.round((inclTax - exclTax) * 100) / 100
 * 确保 exclTax + tax === inclTax
 */
export function calculateTax(
  totalAmountInclTax: number,
  taxRate: number
): {
  totalAmountExclTax: number
  totalTax: number
  totalAmountInclTax: number
} {
  const incl = Math.round((totalAmountInclTax || 0) * 100) / 100
  const rate = Number(taxRate) || 0

  if (rate <= 0) {
    return {
      totalAmountExclTax: incl,
      totalTax: 0,
      totalAmountInclTax: incl
    }
  }

  const divisor = 1 + rate / 100
  const excl = Math.round((incl / divisor) * 100) / 100
  const tax = Math.round((incl - excl) * 100) / 100

  return {
    totalAmountExclTax: excl,
    totalTax: tax,
    totalAmountInclTax: incl
  }
}

/**
 * 金额格式化（千分位，保留 2 位小数）
 */
export function formatMoney(val?: number): string {
  if (val === undefined || val === null || isNaN(val)) {
    return '0.00'
  }
  return Number(val).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 发票状态 Tag 样式与文案
 */
export function formatInvoiceStatusTag(status?: string): {
  label: string
  type: '' | 'default' | 'success' | 'warning' | 'info' | 'danger'
} {
  switch (status) {
    case 'DRAFT':
      return { label: '草稿', type: 'info' }
    case 'SUBMITTED':
      return { label: '已提交', type: 'warning' }
    case 'ISSUED':
      return { label: '已开票', type: 'success' }
    case 'REJECTED':
      return { label: '已驳回', type: 'danger' }
    default:
      return { label: status || '未知', type: 'info' }
  }
}

/**
 * 发票状态按钮操作可用性
 */
export function getInvoiceActionFlags(status?: string) {
  const isDraft = status === 'DRAFT'
  const isSubmitted = status === 'SUBMITTED'
  const isRejected = status === 'REJECTED'
  const isIssued = status === 'ISSUED'

  return {
    canEdit: isDraft,
    canDelete: isDraft,
    canSubmit: isDraft || isRejected,
    canIssue: isSubmitted,
    canReject: isSubmitted,
    isFinal: isIssued
  }
}

/**
 * 官方发票号校验（必须为 20 位字符）
 */
export function validateOfficialInvoiceNo(no?: string): boolean {
  if (!no) return false
  return /^[0-9A-Za-z]{20}$/.test(no.trim())
}
