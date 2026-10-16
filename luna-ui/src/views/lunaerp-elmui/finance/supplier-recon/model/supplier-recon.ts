import {
  SUPPLIER_RECON_ERROR_CODES,
  type SupplierReconItemSaveVO,
  type SupplierReconLineType,
  type SupplierReconPricingBasis
} from '@/api/lunaerp-elmui/finance/supplier-recon'

/**
 * 缺省上月 1 日至月末日期区间
 *
 * @param refDate 参考基准时间，默认当前时间
 * @returns [periodStart, periodEnd] 格式为 YYYY-MM-DD
 */
export function getDefaultPreviousMonthRange(refDate: Date = new Date()): [string, string] {
  const year = refDate.getFullYear()
  const month = refDate.getMonth() // 0-indexed: 0 is Jan, 9 is Oct

  // 上月年份与月份
  const prevYear = month === 0 ? year - 1 : year
  const prevMonth = month === 0 ? 11 : month - 1 // 0-indexed

  const startDate = new Date(prevYear, prevMonth, 1)
  // 下个月的第0天即为当前月的最后一天
  const endDate = new Date(prevYear, prevMonth + 1, 0)

  const pad = (n: number) => String(n).padStart(2, '0')

  const startStr = `${startDate.getFullYear()}-${pad(startDate.getMonth() + 1)}-01`
  const endStr = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())}`

  return [startStr, endStr]
}

/**
 * 计算行计费数量 (billQty)
 */
export function calcItemBillQty(item: {
  lineType?: SupplierReconLineType | string
  pricingBasis?: SupplierReconPricingBasis | string
  qtyKg?: number | null
  qtyM?: number | null
  billQty?: number | null
}): number {
  if (item.lineType === 'PROCESS') {
    if (item.pricingBasis === 'METER') {
      return item.qtyM != null ? Number(item.qtyM) : 0
    }
    if (item.pricingBasis === 'KG') {
      return item.qtyKg != null ? Number(item.qtyKg) : 0
    }
    return item.billQty != null ? Number(item.billQty) : 0
  }

  if (item.lineType === 'PURCHASE') {
    if (item.billQty != null) {
      return Number(item.billQty)
    }
    if (item.qtyKg != null) {
      return Number(item.qtyKg)
    }
    if (item.qtyM != null) {
      return Number(item.qtyM)
    }
    return 0
  }

  // 调整项无数量概念，默认为 1
  return 1
}

/**
 * 计算行金额 (lineAmount)
 */
export function calcItemLineAmount(item: {
  lineType?: SupplierReconLineType | string
  pricingBasis?: SupplierReconPricingBasis | string
  qtyKg?: number | null
  qtyM?: number | null
  billQty?: number | null
  unitPrice?: number | null
  lineAmount?: number | null
}): number {
  if (item.lineType === 'ADJUST') {
    return Number(Number(item.lineAmount || 0).toFixed(2))
  }

  const qty = calcItemBillQty(item)
  const unitPrice = item.unitPrice != null ? Number(item.unitPrice) : 0
  return Number((qty * unitPrice).toFixed(2))
}

/**
 * 汇总对账总金额（含负数调整项）
 */
export function calcTotalAmount(
  items: Array<{
    lineType?: SupplierReconLineType | string
    pricingBasis?: SupplierReconPricingBasis | string
    qtyKg?: number | null
    qtyM?: number | null
    billQty?: number | null
    unitPrice?: number | null
    lineAmount?: number | null
  }>
): number {
  if (!items || items.length === 0) {
    return 0
  }
  const total = items.reduce((sum, item) => sum + calcItemLineAmount(item), 0)
  return Number(total.toFixed(2))
}

/**
 * 格式化金额为千分位形式
 * 例: 12345.67 -> 12,345.67; -500 -> -500.00
 */
export function formatMoney(val: number | string | null | undefined): string {
  if (val == null || val === '') {
    return '0.00'
  }
  const num = Number(val)
  if (isNaN(num)) {
    return '0.00'
  }

  const isNegative = num < 0
  const absNum = Math.abs(num)
  const parts = absNum.toFixed(2).split('.')
  const integerPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const decimalPart = parts[1]

  return `${isNegative ? '-' : ''}${integerPart}.${decimalPart}`
}

/**
 * 获取产品档案加工费参考
 */
export function getProductReferenceFee(
  product: any,
  pricingBasis: string
): { label: string; fee: number | null } {
  if (!product) {
    return { label: '', fee: null }
  }

  if (pricingBasis === 'METER') {
    const fee = product.weavingFeePerMeter != null ? Number(product.weavingFeePerMeter) : null
    return {
      label: '织造费',
      fee
    }
  }

  if (pricingBasis === 'KG') {
    const fee =
      product.knitProcessingFeePerKg != null ? Number(product.knitProcessingFeePerKg) : null
    return {
      label: '针织加工费',
      fee
    }
  }

  return { label: '', fee: null }
}

/**
 * 校验对账单工作台提交数据
 */
export function validateReconDraft(data: {
  supplierId?: number | null
  periodStart?: string
  periodEnd?: string
  items?: SupplierReconItemSaveVO[]
}): { valid: boolean; message?: string } {
  if (!data.supplierId) {
    return { valid: false, message: '请选择供应商' }
  }
  if (!data.periodStart || !data.periodEnd) {
    return { valid: false, message: '请选择完整的对账区间' }
  }
  if (data.periodStart > data.periodEnd) {
    return { valid: false, message: '对账起始日期不能晚于截止日期' }
  }
  if (!data.items || data.items.length === 0) {
    return { valid: false, message: '区间内无可对账明细，无法提交' }
  }

  for (let i = 0; i < data.items.length; i++) {
    const item = data.items[i]
    if (item.lineType === 'ADJUST') {
      if (!item.adjustReason || !item.adjustReason.trim()) {
        return { valid: false, message: `第 ${i + 1} 行调整项必须填写原因备注` }
      }
      if (item.lineAmount == null || Number(item.lineAmount) === 0) {
        return { valid: false, message: `第 ${i + 1} 行调整项金额不能为 0` }
      }
    } else {
      if (item.unitPrice == null || isNaN(Number(item.unitPrice)) || Number(item.unitPrice) < 0) {
        return {
          valid: false,
          message: `第 ${i + 1} 行【${item.lineType === 'PROCESS' ? '加工费' : '采购货款'}】单价不合法`
        }
      }
    }
  }

  return { valid: true }
}

/**
 * 解析后端异常与友好文案映射
 */
export function parseReconErrorMessage(err: any): { isReferenced: boolean; message: string } {
  const code = err?.code
  const msg = err?.msg || err?.message || ''

  if (
    code === SUPPLIER_RECON_ERROR_CODES.SOURCE_REFERENCED ||
    msg.includes('源明细已被对账单')
  ) {
    return {
      isReferenced: true,
      message: msg || '源明细已被其他有效对账单引用，请重新取数！'
    }
  }

  if (
    code === SUPPLIER_RECON_ERROR_CODES.HAS_PAYMENT_APPLY ||
    msg.includes('存在付款申请')
  ) {
    return {
      isReferenced: false,
      message: '对账单存在审批中或已审批的付款申请，禁止作废！'
    }
  }

  if (
    code === SUPPLIER_RECON_ERROR_CODES.APPLY_AMOUNT_EXCEED ||
    msg.includes('超出对账确认金额')
  ) {
    return {
      isReferenced: false,
      message: msg || '累计申请付款金额超出对账确认金额！'
    }
  }

  if (
    code === SUPPLIER_RECON_ERROR_CODES.NO_SOURCE_ITEMS ||
    msg.includes('无可对账明细')
  ) {
    return {
      isReferenced: false,
      message: '所选区间内无可对账明细！'
    }
  }

  return {
    isReferenced: false,
    message: msg || '操作失败，请重试'
  }
}
