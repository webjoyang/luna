import { FINANCE_PAYMENT_ERROR_CODES } from '@/api/lunaerp-elmui/finance/payment'

/**
 * 格式化金额为千分位表示（保留2位小数）
 * 示例：12345.6 -> "12,345.60", null -> "0.00"
 */
export const formatMoney = (val?: number | string | null): string => {
  if (val === null || val === undefined || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 银行账号脱敏展示：保留前6后4位，中间脱敏
 * 示例：622202100011223344 -> 622202******3344
 */
export const maskBankAccount = (account?: string | null): string => {
  if (!account || !account.trim()) return '—'
  const trimmed = account.trim()
  if (trimmed.length <= 8) {
    if (trimmed.length <= 4) return trimmed
    return `${trimmed.slice(0, 2)}****${trimmed.slice(-2)}`
  }
  const prefix = trimmed.slice(0, 6)
  const suffix = trimmed.slice(-4)
  return `${prefix}******${suffix}`
}

/**
 * 计算对账单剩余可申请上限（金额保证 >= 0，两位小数）
 */
export const calcReconRemainingLimit = (
  totalAmount?: number | null,
  appliedAmount?: number | null
): number => {
  const total = Number(totalAmount || 0)
  const applied = Number(appliedAmount || 0)
  const remaining = total - applied
  return Number(Math.max(0, remaining).toFixed(2))
}

/**
 * 计算付款申请单剩余待核销金额
 */
export const calcApplyRemain = (row?: {
  amount?: number | string | null
  allocatedAmount?: number | string | null
  remainAmount?: number | string | null
} | null): number => {
  if (!row) return 0
  if (row.remainAmount !== undefined && row.remainAmount !== null && row.remainAmount !== '') {
    return Number(Number(row.remainAmount).toFixed(2))
  }
  const total = Number(row.amount || 0)
  const allocated = Number(row.allocatedAmount || 0)
  return Math.max(0, Number((total - allocated).toFixed(2)))
}

/**
 * 计算付款申请审批状态的操作权限标识
 * - 草稿/待提交/已驳回：可编辑、可提交、可删除（仅草稿/待提交可删）
 * - 审批中：可撤回，不可编辑/删除
 * - 已通过：不可在此编辑/删除，提示前往付款登记 Tab
 */
export const getPaymentApplyActionFlags = (approvalStatus?: string) => {
  const status = (approvalStatus || 'PENDING').toUpperCase()
  const isDraftOrPending = status === 'PENDING' || status === 'DRAFT'
  const isApproving = status === 'APPROVING'
  const isApproved = status === 'APPROVED'
  const isRejected = status === 'REJECTED'

  return {
    canEdit: isDraftOrPending || isRejected,
    canSubmit: isDraftOrPending || isRejected,
    canDelete: isDraftOrPending,
    canWithdraw: isApproving,
    isApproved,
    isApproving,
    isRejected
  }
}

/**
 * 后端守卫错误友好化映射
 * 覆盖 022_007、023_001 ~ 023_007 等关键业务错误码
 */
export const parsePaymentErrorMessage = (err: any): string => {
  if (!err) return '操作失败'

  const code = err.code !== undefined ? Number(err.code) : undefined
  const msg = err.msg || err.message || ''

  if (
    code === FINANCE_PAYMENT_ERROR_CODES.SUPPLIER_RECON_APPLY_AMOUNT_EXCEED ||
    msg.includes('022_007')
  ) {
    return '累计申请金额超出对账确认金额，请核实后重试'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.APPLY_STATUS_NOT_ALLOWED || msg.includes('023_001')) {
    return '付款申请当前状态不允许该操作（仅草稿/待提交/驳回单据可操作）'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.APPLY_RECON_NOT_CONFIRMED || msg.includes('023_002')) {
    return '关联合同对账单尚未确认，不可生成对账付款申请'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.ALLOCATE_EXCEED || msg.includes('023_003')) {
    return '核销分配金额超过该付款申请单剩余可核销额'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.ACCOUNT_BALANCE_NOT_ENOUGH || msg.includes('023_004')) {
    return '所选资金账户可用余额不足，扣减失败'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.PREPAY_BALANCE_NOT_ENOUGH || msg.includes('023_005')) {
    return '供应商可用预付余额不足，抵扣金额不能超过当前预付余额'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.ACCOUNT_NOT_AVAILABLE || msg.includes('023_006')) {
    return '所选资金账户不存在或已停用，请选择有效账户'
  }
  if (code === FINANCE_PAYMENT_ERROR_CODES.AMOUNT_INVALID || msg.includes('023_007')) {
    return '付款金额不合法：实付加预付抵扣须等于分配合计，且金额必须大于0'
  }

  return msg || '操作失败，请稍后重试'
}

/**
 * 按申请单顺序自动顺序分配付款资金
 * @param items 按创建时间排序的待分配核销申请列表
 * @param totalFunds 总可用资金（实付 + 预付抵扣）
 */
export const distributePaymentAllocation = (
  items: Array<{ applyId: number; remainAmount: number }>,
  totalFunds: number
): Array<{ applyId: number; allocateAmount: number }> => {
  let remainingPool = Number(Math.max(0, totalFunds).toFixed(2))

  return items.map((item) => {
    const remain = Number(Math.max(0, item.remainAmount || 0).toFixed(2))
    const allocate = Math.min(remain, remainingPool)
    const roundedAllocate = Number(allocate.toFixed(2))
    remainingPool = Number(Math.max(0, remainingPool - roundedAllocate).toFixed(2))

    return {
      applyId: item.applyId,
      allocateAmount: roundedAllocate
    }
  })
}

/**
 * 付款登记提交前校验器
 */
export interface PaymentRegisterValidationParams {
  supplierId?: number
  accountId?: number
  payAmount: number
  prepayUsedAmount: number
  prepayBalance: number
  items: Array<{
    applyId: number
    applyNo?: string
    allocateAmount: number
    remainAmount: number
  }>
}

export const validatePaymentRegister = (
  params: PaymentRegisterValidationParams
): { valid: boolean; error?: string } => {
  if (!params.supplierId) {
    return { valid: false, error: '请选择供应商' }
  }
  if (!params.accountId) {
    return { valid: false, error: '请选择付款资金账户' }
  }
  if (!params.items || params.items.length === 0) {
    return { valid: false, error: '请至少选择一笔待核销的付款申请' }
  }

  const pay = Number(params.payAmount || 0)
  const prepay = Number(params.prepayUsedAmount || 0)

  if (pay < 0 || prepay < 0) {
    return { valid: false, error: '本次实付与预付抵扣金额不可为负数' }
  }

  const totalFunds = Number((pay + prepay).toFixed(2))
  if (totalFunds <= 0) {
    return { valid: false, error: '本次实付金额与预付抵扣合计必须大于 0' }
  }

  const balance = Number(params.prepayBalance || 0)
  if (prepay > balance) {
    return {
      valid: false,
      error: `预付抵扣金额 (¥${formatMoney(prepay)}) 超出该供应商可用预付余额 (¥${formatMoney(balance)})`
    }
  }

  let totalAllocated = 0
  for (const item of params.items) {
    const allocate = Number(item.allocateAmount || 0)
    const remain = Number(item.remainAmount || 0)

    if (allocate <= 0) {
      return {
        valid: false,
        error: `申请单【${item.applyNo || item.applyId}】分配核销金额必须大于 0`
      }
    }

    if (allocate > remain + 0.00001) {
      return {
        valid: false,
        error: `申请单【${item.applyNo || item.applyId}】分配核销金额 (¥${formatMoney(allocate)}) 超出剩余可核销额 (¥${formatMoney(remain)})`
      }
    }

    totalAllocated = Number((totalAllocated + allocate).toFixed(2))
  }

  if (Math.abs(totalAllocated - totalFunds) > 0.005) {
    return {
      valid: false,
      error: `分配合计 (¥${formatMoney(totalAllocated)}) 必须精确等于本次实付 + 预付抵扣合计 (¥${formatMoney(totalFunds)})`
    }
  }

  return { valid: true }
}
