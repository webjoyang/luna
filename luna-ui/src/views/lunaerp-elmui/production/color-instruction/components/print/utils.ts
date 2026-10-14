import dayjs from 'dayjs'
import type { ColorInstructionPrintVO, ColorInstructionPrintColorItemVO } from '@/api/lunaerp-elmui/production/color-instruction'

/**
 * 格式化打印时间
 */
export const formatPrintTime = (time?: string | Date | null): string => {
  if (!time) return dayjs().format('YYYY-MM-DD HH:mm:ss')
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 单据主状态映射
 */
export const formatStatusText = (status?: string): string => {
  if (!status) return '—'
  const map: Record<string, string> = {
    PENDING: '待打色',
    IN_PROGRESS: '打色中',
    CONFIRMED: '已确认',
    IN_PRODUCTION: '已投产',
    VOID: '已作废'
  }
  return map[status] || status
}

/**
 * 颜色行状态映射
 */
export const formatLineStatusText = (status?: string): string => {
  if (!status) return '—'
  const map: Record<string, string> = {
    PENDING: '待结果',
    APPROVED: '通过',
    REJECTED: '驳回'
  }
  return map[status] || status
}

/**
 * 确认类型映射
 */
export const formatConfirmType = (type?: string): string => {
  if (!type) return '—'
  const map: Record<string, string> = {
    CUSTOMER: '客户确认',
    INTERNAL: '内部确认'
  }
  return map[type] || type
}

/**
 * 格式化产品编号与品名
 */
export const formatProductInfo = (sheet?: Partial<ColorInstructionPrintVO> | null): string => {
  if (!sheet) return '—'
  const parts: string[] = []
  if (sheet.productCode) parts.push(sheet.productCode)
  if (sheet.productName) parts.push(sheet.productName)
  return parts.length > 0 ? parts.join(' / ') : '—'
}

/**
 * 格式化承接方文本（空为自做）
 */
export const formatLabFactoryName = (sheet?: Partial<ColorInstructionPrintVO> | null): string => {
  if (!sheet) return '本厂打色房（自做）'
  return sheet.labFactoryName || '本厂打色房（自做）'
}

/**
 * 是否历史驳回行
 */
export const isColorLineRejected = (color?: Partial<ColorInstructionPrintColorItemVO> | null): boolean => {
  return color?.status === 'REJECTED'
}

/**
 * 轮次文本展示
 */
export const formatColorRound = (round?: number): string => {
  return `第 ${round || 1} 轮`
}

/**
 * 格式化驳回标注说明
 */
export const formatRejectionText = (color?: Partial<ColorInstructionPrintColorItemVO> | null): string => {
  if (!color || color.status !== 'REJECTED') return ''
  const roundStr = `第 ${color.round || 1} 轮`
  const reason = color.rejectReason ? `：${color.rejectReason}` : ''
  return `[已驳回 - ${roundStr}]${reason}`
}
