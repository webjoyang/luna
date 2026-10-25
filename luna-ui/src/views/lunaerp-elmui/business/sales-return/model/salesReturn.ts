/**
 * 销售退货领域纯模型与计算工具（无 DOM / 无 Vue SFC 依赖，可在 Vitest Node 环境下纯测试）
 * 遵循 coding-standards.md §6 与 §7
 */

export interface SalesReturnRollItem {
  id?: number
  shipmentRollId?: number
  rollNo: string
  weight?: number
  lengthM?: number
  remark?: string
}

export interface SalesReturnLineItem {
  id?: number
  shipmentItemId: number
  unit: string
  returnQty: number
  unitPrice?: number
  returnReason: string
  rolls: SalesReturnRollItem[]
}

export interface SalesReturnActionFlags {
  canDetail: boolean
  canEdit: boolean
  canDelete: boolean
  canSubmit: boolean
  canWithdraw: boolean
  canReceive: boolean
  canPost: boolean
  canCancel: boolean
  canPrint: boolean
}

/**
 * 状态机驱动交互动作单一来源指示器
 *
 * @param status 业务状态（DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）
 * @param approvalStatus 审批状态（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）
 */
export function getSalesReturnActionFlags(
  status?: string,
  approvalStatus?: string
): SalesReturnActionFlags {
  const isDraft = status === 'DRAFT'
  const isPending = approvalStatus === 'PENDING'
  const isRejected = approvalStatus === 'REJECTED'
  const isApproving = approvalStatus === 'APPROVING'
  const isApproved = status === 'APPROVED'
  const isReceived = status === 'RECEIVED'
  const isPosted = status === 'POSTED'
  const isCancelled = status === 'CANCELLED'

  return {
    canDetail: Boolean(status),
    canEdit: isDraft && (isPending || isRejected),
    canDelete: isDraft && (isPending || isRejected),
    canSubmit: isDraft && (isPending || isRejected),
    canWithdraw: isDraft && isApproving,
    canReceive: isApproved,
    canPost: isReceived,
    canCancel: (isDraft && (isPending || isRejected)) || isApproved,
    canPrint: !isCancelled && Boolean(status)
  }
}

/**
 * 计算卷列表实称重量合计
 */
export function calculateRollTotalWeight(rolls?: SalesReturnRollItem[]): number {
  if (!rolls || rolls.length === 0) return 0
  return Number(rolls.reduce((sum, r) => sum + (Number(r.weight) || 0), 0).toFixed(2))
}

/**
 * 计算卷列表米数合计
 */
export function calculateRollTotalLength(rolls?: SalesReturnRollItem[]): number {
  if (!rolls || rolls.length === 0) return 0
  return Number(rolls.reduce((sum, r) => sum + (Number(r.lengthM) || 0), 0).toFixed(2))
}

/**
 * 按行单位（KG/M）汇总卷数量
 */
export function getLineRollQty(unit: string, rolls?: SalesReturnRollItem[]): number {
  if (!rolls || rolls.length === 0) return 0
  const normalizedUnit = (unit || '').trim().toUpperCase()
  if (normalizedUnit === 'M' || normalizedUnit === 'METER') {
    return calculateRollTotalLength(rolls)
  }
  return calculateRollTotalWeight(rolls)
}

/**
 * 判定行退货数量与卷合计是否不一致（软齐套黄色提示）
 *
 * @param returnQty 行退货数量
 * @param unit 行单位（KG/M）
 * @param rolls 卷列表
 * @returns true 表示存在差异，前端需给出黄色警示条
 */
export function isQtyRollMismatch(
  returnQty: number,
  unit: string,
  rolls?: SalesReturnRollItem[]
): boolean {
  if (!rolls || rolls.length === 0) return false
  const rollSum = getLineRollQty(unit, rolls)
  return Math.abs(Number(returnQty || 0) - rollSum) > 0.001
}

/**
 * 校验单行退货项及卷的完整性
 */
export function validateSalesReturnLine(item: SalesReturnLineItem): {
  valid: boolean
  message?: string
} {
  if (!item.shipmentItemId) {
    return { valid: false, message: '必须关联发货明细行' }
  }
  if (!item.returnQty || item.returnQty <= 0) {
    return { valid: false, message: '退货数量必须大于0' }
  }
  if (!item.returnReason || !item.returnReason.trim()) {
    return { valid: false, message: '退货原因不能为空' }
  }
  if (!item.rolls || item.rolls.length === 0) {
    return { valid: false, message: '每行必须至少登记一卷' }
  }
  for (let i = 0; i < item.rolls.length; i++) {
    const r = item.rolls[i]
    if (!r.rollNo || !r.rollNo.trim()) {
      return { valid: false, message: `第 ${i + 1} 卷卷号不能为空` }
    }
    const hasWeight = r.weight !== undefined && r.weight !== null && r.weight > 0
    const hasLength = r.lengthM !== undefined && r.lengthM !== null && r.lengthM > 0
    if (!hasWeight && !hasLength) {
      return { valid: false, message: `第 ${i + 1} 卷重量与米数至少需填写一项` }
    }
  }
  return { valid: true }
}

export interface QcRollItem {
  id?: number
  rollNo?: string
  weight?: number
  lengthM?: number
  qcResult?: string
  locationId?: number
  qcRemark?: string
}

/**
 * 按行单位重算 RESTOCK 卷的 restockQty 与 SCRAP 卷的 scrapQty
 */
export function calculateQcRestockAndScrapQty(
  unit: string,
  rolls?: QcRollItem[]
): { restockQty: number; scrapQty: number } {
  if (!rolls || rolls.length === 0) {
    return { restockQty: 0, scrapQty: 0 }
  }
  const isMeter =
    (unit || '').trim().toUpperCase() === 'M' || (unit || '').trim().toUpperCase() === 'METER'
  let totalQty = 0
  let restockQty = 0

  for (const r of rolls) {
    const qty = isMeter ? Number(r.lengthM) || 0 : Number(r.weight) || 0
    totalQty += qty
    if (r.qcResult === 'RESTOCK') {
      restockQty += qty
    }
  }

  const scrapQty = Math.max(0, totalQty - restockQty)
  return {
    restockQty: Number(restockQty.toFixed(2)),
    scrapQty: Number(scrapQty.toFixed(2))
  }
}

/**
 * 校验到货质检提交数据：所有卷必须均有判定 (RESTOCK / SCRAP)
 */
export function validateQcDecision(rolls?: QcRollItem[]): {
  valid: boolean
  message?: string
} {
  if (!rolls || rolls.length === 0) {
    return { valid: false, message: '卷质检清单不能为空' }
  }
  for (let i = 0; i < rolls.length; i++) {
    const r = rolls[i]
    if (!r.qcResult || (r.qcResult !== 'RESTOCK' && r.qcResult !== 'SCRAP')) {
      return { valid: false, message: `第 ${i + 1} 卷（${r.rollNo || ''}）未完成质检判定` }
    }
  }
  return { valid: true }
}

/**
 * 格式化金额（千分位，2位小数），安全兼容脱敏星号字符串（如 '****'）与空值
 *
 * @param val 金额数值或脱敏字符串
 * @param currency 可选币种前缀（如 'RMB', 'USD'）
 */
export function formatAmount(val?: number | string | null, currency?: string): string {
  if (val === undefined || val === null || val === '') return '0.00'
  if (typeof val === 'string' && val.includes('*')) {
    return currency ? `${currency} ${val}` : val
  }
  const num = Number(val)
  if (isNaN(num)) return typeof val === 'string' ? val : '0.00'
  const formatted = num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
  return currency ? `${currency} ${formatted}` : formatted
}
