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
    canDetail: isCancelled || isApproved || isReceived || isPosted,
    canEdit: isDraft && (isPending || isRejected),
    canDelete: isDraft && isPending,
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
