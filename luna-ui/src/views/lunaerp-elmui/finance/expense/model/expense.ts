export interface ExpenseItemData {
  id?: number
  category: string
  originalAmount: number | string
  occurDate: string
  remark: string
}

export interface ExpenseFormData {
  id?: number
  applicantUserId?: number
  accountId?: number
  attachments?: string[]
  remark?: string
  items: ExpenseItemData[]
}

export interface ExpenseActionFlags {
  canEdit: boolean
  canSubmit: boolean
  canDelete: boolean
  canWithdraw: boolean
  isApproved: boolean
  isRejected: boolean
}

/**
 * 费用报销 10 位物理错误码定义（对齐 FinanceErrorCodeConstants.java）
 */
export const EXPENSE_ERROR_CODES = {
  EXPENSE_NOT_EXISTS: 1_030_024_000,
  EXPENSE_STATUS_NOT_ALLOWED: 1_030_024_001,
  EXPENSE_ITEM_REQUIRED: 1_030_024_002,
  EXPENSE_ACCOUNT_INVALID: 1_030_024_003,
  EXPENSE_AMOUNT_INVALID: 1_030_024_004,
  EXPENSE_ATTACHMENT_LIMIT: 1_030_024_005,
  EXPENSE_USER_NOT_EXISTS: 1_030_024_006,
  EXPENSE_OCCUR_DATE_INVALID: 1_030_024_007
} as const

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

/**
 * 根据报销单审批状态推导可用操作标识
 * 状态机规范对齐 Issue 05 & ADR：
 * - PENDING: 草稿态，可编辑、提交审批、删除
 * - APPROVING: 审批中，仅可撤回（cancel-approve）
 * - REJECTED: 已驳回，可修改再提、删除
 * - APPROVED: 终态，只读，不可改/删/撤回，可套打 A4
 * - CANCELLED: 已撤回，回置或只读
 */
export function getExpenseActionFlags(approvalStatus?: string): ExpenseActionFlags {
  const status = (approvalStatus || 'PENDING').toUpperCase()
  switch (status) {
    case 'PENDING':
      return {
        canEdit: true,
        canSubmit: true,
        canDelete: true,
        canWithdraw: false,
        isApproved: false,
        isRejected: false
      }
    case 'APPROVING':
      return {
        canEdit: false,
        canSubmit: false,
        canDelete: false,
        canWithdraw: true,
        isApproved: false,
        isRejected: false
      }
    case 'REJECTED':
      return {
        canEdit: true,
        canSubmit: true,
        canDelete: true,
        canWithdraw: false,
        isApproved: false,
        isRejected: true
      }
    case 'APPROVED':
      return {
        canEdit: false,
        canSubmit: false,
        canDelete: false,
        canWithdraw: false,
        isApproved: true,
        isRejected: false
      }
    case 'CANCELLED':
    default:
      return {
        canEdit: false,
        canSubmit: false,
        canDelete: false,
        canWithdraw: false,
        isApproved: false,
        isRejected: false
      }
  }
}

/**
 * 计算报销单明细行合计金额
 */
export function calculateExpenseTotal(items?: ExpenseItemData[]): number {
  if (!items || !Array.isArray(items)) return 0
  const total = items.reduce((acc, item) => {
    const val = Number(item.originalAmount)
    return isNaN(val) ? acc : acc + val
  }, 0)
  return Math.round(total * 100) / 100
}

/**
 * 前端校验报销表单数据完整性
 */
export function validateExpenseForm(formData: ExpenseFormData): string | null {
  if (!formData.applicantUserId) {
    return '报销人员不能为空'
  }
  if (!formData.accountId) {
    return '付款资金账户不能为空'
  }
  if (!formData.items || formData.items.length === 0) {
    return '报销明细行至少需要一行'
  }
  const today = new Date().toLocaleDateString('en-CA')

  for (let i = 0; i < formData.items.length; i++) {
    const item = formData.items[i]
    const rowNum = i + 1
    if (!item.category) {
      return `第 ${rowNum} 行费用类型不能为空`
    }
    const amount = Number(item.originalAmount)
    if (isNaN(amount) || amount <= 0) {
      return `第 ${rowNum} 行报销金额必须大于0`
    }
    if (!item.occurDate) {
      return `第 ${rowNum} 行费用发生日期不能为空`
    }
    if (item.occurDate > today) {
      return `第 ${rowNum} 行费用发生日期不能晚于今天`
    }
    if (!item.remark || !item.remark.trim()) {
      return `第 ${rowNum} 行费用明细备注说明不能为空`
    }
  }

  if (formData.attachments && formData.attachments.length > 10) {
    return '报销附件最多上传10个'
  }

  return null
}

const NUMERIC_ERROR_MAP: Record<number, string> = {
  [EXPENSE_ERROR_CODES.EXPENSE_NOT_EXISTS]: '报销单不存在',
  [EXPENSE_ERROR_CODES.EXPENSE_STATUS_NOT_ALLOWED]: '当前报销单状态不允许此操作',
  [EXPENSE_ERROR_CODES.EXPENSE_ITEM_REQUIRED]: '报销明细行至少需要一行',
  [EXPENSE_ERROR_CODES.EXPENSE_ACCOUNT_INVALID]: '付款资金账户无效或已停用',
  [EXPENSE_ERROR_CODES.EXPENSE_AMOUNT_INVALID]: '报销金额必须大于0',
  [EXPENSE_ERROR_CODES.EXPENSE_ATTACHMENT_LIMIT]: '报销附件最多上传10个',
  [EXPENSE_ERROR_CODES.EXPENSE_USER_NOT_EXISTS]: '报销人用户不存在',
  [EXPENSE_ERROR_CODES.EXPENSE_OCCUR_DATE_INVALID]: '费用发生日期不能晚于提交日'
}

const STRING_CODE_MAP: Record<string, string> = {
  '024_000': '报销单不存在',
  '024_001': '当前报销单状态不允许此操作',
  '024_002': '报销明细行至少需要一行',
  '024_003': '付款资金账户无效或已停用',
  '024_004': '报销金额必须大于0',
  '024_005': '报销附件最多上传10个',
  '024_006': '报销人用户不存在',
  '024_007': '费用发生日期不能晚于提交日'
}

/**
 * 解析后端返回的异常信息，将 024 系列错误码转为友好中文提示
 * 优先按数字 code 精确匹配，后置根据 024_xxx 模糊字符串兜底
 */
export function parseExpenseErrorMessage(err: any): string {
  if (!err) return '操作失败，请重试'

  // 1. 优先按数字 code 精确匹配
  const codeNum = Number(err?.code ?? (typeof err === 'number' ? err : NaN))
  if (!isNaN(codeNum) && NUMERIC_ERROR_MAP[codeNum]) {
    return NUMERIC_ERROR_MAP[codeNum]
  }

  // 2. 字符串匹配（包含 024_xxx 错误码标识）
  const message = typeof err === 'string' ? err : err.message || err.msg || ''
  for (const [code, desc] of Object.entries(STRING_CODE_MAP)) {
    if (message.includes(code)) {
      return desc
    }
  }

  return message || '操作失败，请重试'
}
