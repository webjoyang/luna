/**
 * 客户门户发货单领域模型与工具函数 (Issue 03)
 */

export type PortalShipmentSignStatus = 'PENDING' | 'SIGNED' | 'REFUSED'

export interface SignStatusConfig {
  text: string
  tagType: '' | 'success' | 'warning' | 'info' | 'danger'
}

export const SIGN_STATUS_MAP: Record<string, SignStatusConfig> = {
  PENDING: { text: '待签收', tagType: 'warning' },
  SIGNED: { text: '已签收', tagType: 'success' },
  REFUSED: { text: '已拒收', tagType: 'danger' }
}

/**
 * 获取签收状态展示文案
 */
export function getSignStatusText(status?: string): string {
  if (!status) return '未知'
  return SIGN_STATUS_MAP[status]?.text || status
}

/**
 * 获取签收状态标签类型
 */
export function getSignStatusTagType(
  status?: string
): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (!status) return 'info'
  return SIGN_STATUS_MAP[status]?.tagType || 'info'
}

/**
 * 格式化金额展示
 */
export function formatAmount(amount?: number | string | null): string {
  if (amount == null || amount === '') return '¥0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '¥0.00'
  return `¥${num.toFixed(2)}`
}

/**
 * 解析发货单详情入口参数（同时兼容 :id 与 ?no= 双形态直达）
 */
export function resolveShipmentDetailParams(
  routeParams: { id?: string | number },
  routeQuery: { no?: string | null }
): { id?: number; no?: string; isValid: boolean } {
  // 1. 优先使用 :id
  if (routeParams?.id) {
    const parsedId = Number(routeParams.id)
    if (!isNaN(parsedId) && parsedId > 0) {
      return { id: parsedId, isValid: true }
    }
  }

  // 2. 兼容 ?no=
  if (routeQuery?.no && typeof routeQuery.no === 'string' && routeQuery.no.trim().length > 0) {
    return { no: routeQuery.no.trim(), isValid: true }
  }

  return { isValid: false }
}

/**
 * 判断发货单是否处于可签收状态（仅 PENDING 允许签收或拒收）
 */
export function canSignShipment(signStatus?: string): boolean {
  return signStatus === 'PENDING'
}

/**
 * 校验门户发货单签收/拒收表单
 */
export function validateSignForm(
  signedByName?: string | null,
  _signRemark?: string | null
): { valid: boolean; error?: string } {
  if (!signedByName || signedByName.trim().length === 0) {
    return { valid: false, error: '请输入签收人姓名' }
  }
  return { valid: true }
}

/**
 * 获取签收/拒收弹窗标题
 */
export function getSignActionTitle(action: 'SIGNED' | 'REFUSED'): string {
  return action === 'SIGNED' ? '发货单签收确认' : '发货单拒收确认'
}

/**
 * 获取签收/拒收确定按钮文本
 */
export function getSignActionConfirmText(action: 'SIGNED' | 'REFUSED'): string {
  return action === 'SIGNED' ? '确认签收' : '确认拒收'
}
