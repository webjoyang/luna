import type {
  SalesReturnPrintItemVO,
  SalesReturnPrintRollVO
} from '@/api/lunaerp-elmui/business/sales-return'

export const STATUS_MAP: Record<string, string> = {
  DRAFT: '草稿',
  APPROVED: '已审批',
  RECEIVED: '已收货',
  POSTED: '已过账',
  CANCELLED: '已作废'
}

export const APPROVAL_STATUS_MAP: Record<string, string> = {
  PENDING: '待提交',
  APPROVING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已撤回'
}

export const REASON_MAP: Record<string, string> = {
  QUALITY: '质量问题',
  WRONG_GOODS: '发错货',
  CUSTOMER_CANCEL: '客户取消',
  EXCESS: '多发退回',
  OTHER: '其他原因'
}

export function formatStatusText(status?: string): string {
  return (status && STATUS_MAP[status]) || status || '—'
}

export function formatApprovalStatusText(approvalStatus?: string): string {
  return (approvalStatus && APPROVAL_STATUS_MAP[approvalStatus]) || approvalStatus || ''
}

export function formatReasonText(reason?: string): string {
  return (reason && REASON_MAP[reason]) || reason || '—'
}

export function formatRollQty(unit?: string, roll?: SalesReturnPrintRollVO): string {
  if (!roll) return '0'
  const u = (unit || '').trim().toUpperCase()
  if (u === 'M' || u === 'METER') {
    return `${roll.lengthM ?? 0}m`
  }
  return `${roll.weight ?? 0}kg`
}

export function formatPrintTime(time?: string): string {
  if (!time) {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return time.replace('T', ' ').substring(0, 16)
}

/**
 * 汇总多单位退货数量文本（遵循 coding-standards.md §23，防无量纲混加）
 * 例如：100.00 KG / 50.00 M
 */
export function computeSalesReturnTotalQtyText(
  items?: Array<Pick<SalesReturnPrintItemVO, 'unit' | 'returnQty'>> | null
): string {
  if (!items || !items.length) return '0.00'
  const unitMap: Record<string, number> = {}
  for (const item of items) {
    const unit = (item.unit || 'KG').trim().toUpperCase()
    unitMap[unit] = (unitMap[unit] || 0) + (item.returnQty != null ? Number(item.returnQty) : 0)
  }
  const parts = Object.entries(unitMap).map(([unit, qty]) => `${qty.toFixed(2)} ${unit}`)
  return parts.join(' / ') || '0.00'
}

export function calcTotalPieces(items?: SalesReturnPrintItemVO[]): number {
  if (!items || items.length === 0) return 0
  return items.reduce((acc, it) => acc + (Number(it.pieces) || 1), 0)
}

export function calcTotalAmount(items?: SalesReturnPrintItemVO[]): number {
  if (!items || items.length === 0) return 0
  return items.reduce((acc, it) => acc + (Number(it.amount) || 0), 0)
}

/**
 * 人民币大写金额转换函数
 */
export function formatChineseAmount(money?: number | string | null): string {
  if (money == null || isNaN(Number(money))) return '零元整'
  const n = Math.abs(Number(money))
  if (n === 0) return '零元整'

  const fraction = ['角', '分']
  const digit = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = [
    ['元', '万', '亿'],
    ['', '拾', '佰', '仟']
  ]

  let s = ''
  const head = n < 0 ? '欠' : ''
  const numStr = n.toFixed(2)
  const [intPart, decPart] = numStr.split('.')

  for (let i = 0; i < fraction.length; i++) {
    const d = parseInt(decPart[i] || '0')
    if (d > 0) {
      s += digit[d] + fraction[i]
    }
  }

  let p = ''
  for (let i = 0; i < unit[0].length && parseInt(intPart) > 0; i++) {
    let sub = ''
    for (let j = 0; j < unit[1].length && intPart.length > 0; j++) {
      const d = parseInt(intPart[intPart.length - 1 - (i * 4 + j)] || '-1')
      if (d >= 0) {
        sub = digit[d] + unit[1][j] + sub
      }
    }
    sub = sub
      .replace(/零[拾佰仟]/g, '零')
      .replace(/零+/g, '零')
      .replace(/零$/, '')
    if (sub) {
      p = sub + unit[0][i] + p
    }
  }

  p = p
    .replace(/零[万亿]/g, '')
    .replace(/亿万/g, '亿')
    .replace(/零+/g, '零')
    .replace(/零$/, '')

  if (!p) p = '零元'
  else if (!p.endsWith('元')) p += '元'

  return head + (s ? p + s : p + '整')
}
