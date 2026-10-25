/**
 * 客户门户对账单领域模型与工具函数 (Issue 05)
 */

export type PortalStatementStatus = 'SENT' | 'CONFIRMED' | 'REJECTED'

export interface StatementStatusConfig {
  text: string
  tagType: '' | 'success' | 'warning' | 'info' | 'danger'
}

export const STATEMENT_STATUS_MAP: Record<string, StatementStatusConfig> = {
  SENT: { text: '待确认', tagType: 'warning' },
  CONFIRMED: { text: '已确认', tagType: 'success' },
  REJECTED: { text: '已驳回', tagType: 'danger' }
}

/**
 * 获取对账单状态展示文案
 */
export function getStatementStatusText(status?: string): string {
  if (!status) return '未知'
  return STATEMENT_STATUS_MAP[status]?.text || status
}

/**
 * 获取对账单状态标签类型
 */
export function getStatementStatusTagType(
  status?: string
): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (!status) return 'info'
  return STATEMENT_STATUS_MAP[status]?.tagType || 'info'
}

/**
 * 格式化日期时间
 */
export function formatDateTime(val?: string | Date | null): string {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 格式化金额展示
 */
export function formatAmount(amount?: number | string | null, currency = 'RMB'): string {
  if (amount == null || amount === '') return '¥0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '¥0.00'
  const symbol =
    currency === 'USD' ? '$' : currency === 'EUR' ? '€' : currency === 'HKD' ? 'HK$' : '¥'
  return `${symbol}${num.toFixed(2)}`
}

/**
 * 解析对账单详情入口参数（同时兼容 :id 与 ?no= 双形态直达）
 */
export function resolveStatementDetailParams(
  routeParams: { id?: string | number },
  routeQuery: { no?: string | null; statementNo?: string | null }
): { id?: number; no?: string; isValid: boolean } {
  // 1. 优先使用 :id
  if (routeParams?.id) {
    const parsedId = Number(routeParams.id)
    if (!isNaN(parsedId) && parsedId > 0) {
      return { id: parsedId, isValid: true }
    }
  }

  // 2. 兼容 ?no= / ?statementNo= 二维码直达契约形态
  const no = routeQuery?.no || routeQuery?.statementNo
  if (no && typeof no === 'string' && no.trim().length > 0) {
    return { no: no.trim(), isValid: true }
  }

  return { isValid: false }
}

/**
 * 按 Tab 过滤对账单列表
 */
export function filterStatementListByTab<T extends { status?: string }>(
  list: T[] = [],
  tab: string
): T[] {
  if (!tab || tab === 'ALL') {
    return list
  }
  return list.filter((item) => item.status === tab)
}

/**
 * 校验是否允许确认或驳回（仅 SENT 待确认状态允许操作）
 */
export function canConfirmOrReject(status?: string): boolean {
  return status === 'SENT'
}

/**
 * 校验确认表单（手填姓名必填）
 */
export function validateConfirmForm(form: { confirmByName?: string }): {
  valid: boolean
  message?: string
} {
  if (!form?.confirmByName || !form.confirmByName.trim()) {
    return { valid: false, message: '请输入确认人姓名' }
  }
  return { valid: true }
}

/**
 * 校验驳回表单（驳回原因必填）
 */
export function validateRejectForm(form: { rejectReason?: string }): {
  valid: boolean
  message?: string
} {
  if (!form?.rejectReason || !form.rejectReason.trim()) {
    return { valid: false, message: '请输入驳回/异议原因' }
  }
  return { valid: true }
}
