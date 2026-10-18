/**
 * 客户对账业务错误码（对齐 FinanceErrorCodeConstants）
 */
export const CUSTOMER_STATEMENT_ERROR_CODES = {
  NOT_EXISTS: 1030021000,
  STATUS_NOT_DRAFT: 1030021001,
  PERIOD_INVALID: 1030021002,
  CURRENCY_MIXED: 1030021003,
  ENTRY_OCCUPIED: 1030021004,
  ENTRY_NOT_FOUND: 1030021005,
  NO_CANDIDATE_LINES: 1030021006,
  CUSTOMER_NOT_EXISTS: 1030021007,
  STATEMENT_NO_EXHAUSTED: 1030021008
} as const

/**
 * 缺省上一自然月 1 日至月末日期区间
 *
 * @param refDate 参考基准时间，默认当前时间
 * @returns [periodStart, periodEnd] 格式为 YYYY-MM-DD
 */
export function getDefaultPreviousMonthRange(refDate: Date = new Date()): [string, string] {
  const year = refDate.getFullYear()
  const month = refDate.getMonth() // 0-indexed: 0 is Jan, 9 is Oct

  const prevYear = month === 0 ? year - 1 : year
  const prevMonth = month === 0 ? 11 : month - 1

  const startDate = new Date(prevYear, prevMonth, 1)
  const endDate = new Date(prevYear, prevMonth + 1, 0)

  const pad = (n: number) => String(n).padStart(2, '0')

  const startStr = `${startDate.getFullYear()}-${pad(startDate.getMonth() + 1)}-01`
  const endStr = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())}`

  return [startStr, endStr]
}

/**
 * 校验对账区间合法性
 */
export function validateStatementPeriod(
  periodStart?: string,
  periodEnd?: string
): { valid: boolean; message?: string } {
  if (!periodStart || !periodEnd) {
    return { valid: false, message: '对账区间起止日期不能为空' }
  }
  if (periodStart > periodEnd) {
    return { valid: false, message: '起始日期不能晚于截止日期' }
  }
  return { valid: true }
}

/**
 * 重算对账单汇总数据（借方、贷方、期末余额）
 * 期末 = 期初 + 借方合计 - 贷方合计
 */
export function calculateStatementSummary(
  openingBalance: number = 0,
  items: Array<{ debitAmount?: number | null; creditAmount?: number | null }> = []
): { periodDebit: number; periodCredit: number; closingBalance: number } {
  let periodDebit = 0
  let periodCredit = 0

  for (const item of items) {
    if (item.debitAmount != null) {
      periodDebit += Number(item.debitAmount)
    }
    if (item.creditAmount != null) {
      periodCredit += Number(item.creditAmount)
    }
  }

  // 精度规整至 2 位小数
  const round2 = (val: number) => Math.round((val + Number.EPSILON) * 100) / 100
  const d = round2(periodDebit)
  const c = round2(periodCredit)
  const op = round2(Number(openingBalance) || 0)
  const cl = round2(op + d - c)

  return {
    periodDebit: d,
    periodCredit: c,
    closingBalance: cl
  }
}

/**
 * 客户对账单操作权限与行为标识（单一信任源）
 * 严格守卫：
 * - 发送：仅 DRAFT
 * - 作废：非终态（DRAFT / SENT / REJECTED），CONFIRMED 不可作废
 * - 代客确认：仅 SENT
 * - 驳回：仅 SENT
 * - 回复异议：仅 REJECTED
 * - 增删改追加：仅 DRAFT
 * - 打印：非 VOID 态
 */
export function getCustomerStatementActionFlags(status?: string): {
  canEdit: boolean
  canDelete: boolean
  canAddEntry: boolean
  canSend: boolean
  canVoid: boolean
  canConfirmOnBehalf: boolean
  canRejectReply: boolean
  canPrint: boolean
} {
  const isDraft = status === 'DRAFT'
  const isSent = status === 'SENT'
  const isRejected = status === 'REJECTED'
  const isConfirmed = status === 'CONFIRMED'
  const isVoid = status === 'VOID'

  return {
    canEdit: isDraft,
    canDelete: isDraft,
    canAddEntry: isDraft,
    canSend: isDraft,
    canVoid: !isConfirmed && !isVoid && Boolean(status),
    canConfirmOnBehalf: isSent,
    canRejectReply: isRejected,
    canPrint: !isVoid && Boolean(status)
  }
}

/**
 * 校验代客确认表单
 */
export function validateConfirmOnBehalfForm(
  confirmMethod?: string,
  remark?: string
): {
  valid: boolean
  message?: string
} {
  if (!confirmMethod || !confirmMethod.trim()) {
    return { valid: false, message: '请选择或填写代客确认方式' }
  }
  if (!remark || !remark.trim()) {
    return { valid: false, message: '请填写代客确认依据及备注' }
  }
  return { valid: true }
}

/**
 * 格式化金额（千分位保留2位小数）
 */
export function formatMoney(amount?: number | null, decimals: number = 2): string {
  if (amount == null || isNaN(Number(amount))) {
    return '0.00'
  }
  return Number(amount).toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

/**
 * 签收状态标签展示映射
 */
export function formatSignStatusTag(signStatus?: string): {
  label: string
  type: 'success' | 'warning' | 'info' | 'danger'
} {
  switch (signStatus) {
    case 'SIGNED':
      return { label: '已签收', type: 'success' }
    case 'UN_SIGN':
    case 'UNSIGNED':
      return { label: '未签收', type: 'warning' }
    case 'REJECTED':
      return { label: '拒签', type: 'danger' }
    case 'PARTIAL_SIGNED':
      return { label: '部分签收', type: 'info' }
    default:
      return { label: signStatus || '未知', type: 'info' }
  }
}

/**
 * 对账单状态标签展示映射（对齐字典 2272）
 */
export function formatStatementStatusTag(status?: string): {
  label: string
  type: 'info' | 'primary' | 'success' | 'danger' | ''
} {
  switch (status) {
    case 'DRAFT':
      return { label: '草稿', type: 'info' }
    case 'SENT':
      return { label: '已发出', type: 'primary' }
    case 'CONFIRMED':
      return { label: '已确认', type: 'success' }
    case 'REJECTED':
      return { label: '已驳回', type: 'danger' }
    case 'VOID':
      return { label: '已作废', type: 'info' }
    default:
      return { label: status || '未知', type: '' }
  }
}

/**
 * 错误码转人类可读提示
 */
export function parseCustomerStatementError(code?: number): string {
  switch (code) {
    case CUSTOMER_STATEMENT_ERROR_CODES.NOT_EXISTS:
      return '客户对账单不存在'
    case CUSTOMER_STATEMENT_ERROR_CODES.STATUS_NOT_DRAFT:
      return '只有草稿（DRAFT）状态的对账单才允许修改或删除'
    case CUSTOMER_STATEMENT_ERROR_CODES.PERIOD_INVALID:
      return '对账区间起止日期不合法'
    case CUSTOMER_STATEMENT_ERROR_CODES.CURRENCY_MIXED:
      return '客户存在多币种业务，请分单按单一币种生成对账单'
    case CUSTOMER_STATEMENT_ERROR_CODES.ENTRY_OCCUPIED:
      return '该分录已被其他已确认或处理中的对账单占用'
    case CUSTOMER_STATEMENT_ERROR_CODES.ENTRY_NOT_FOUND:
      return '指定的经营分录不存在'
    case CUSTOMER_STATEMENT_ERROR_CODES.NO_CANDIDATE_LINES:
      return '该客户在指定期间内无可对账分录'
    case CUSTOMER_STATEMENT_ERROR_CODES.CUSTOMER_NOT_EXISTS:
      return '客户不存在'
    case CUSTOMER_STATEMENT_ERROR_CODES.STATEMENT_NO_EXHAUSTED:
      return '对账单编号序号已耗尽，请稍后重试'
    default:
      return '对账单操作失败，请重试'
  }
}
