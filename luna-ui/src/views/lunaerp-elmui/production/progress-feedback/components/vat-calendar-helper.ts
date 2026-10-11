import type { VatCalendarVatVO } from '@/api/lunaerp-elmui/production/progress-feedback'

/**
 * 状态主题配置
 */
export interface VatStatusTheme {
  label: string
  color: string
  bg: string
  border: string
  dotColor: string
  isCompleted: boolean
  className: string
}

/**
 * 进度反馈状态映射表
 * 对齐字典 lunaerp_progress_feedback_status
 * COMPLETED 态强制灰化处理
 */
export function getVatStatusTheme(status?: string): VatStatusTheme {
  const normalized = (status || '').toUpperCase()

  if (normalized === 'COMPLETED') {
    return {
      label: '已完成',
      color: '#909399',
      bg: '#f4f4f5',
      border: '#e9e9eb',
      dotColor: '#909399',
      isCompleted: true,
      className: 'status-completed'
    }
  }

  if (normalized === 'SCHEDULED') {
    return {
      label: '已排缸',
      color: '#409eff',
      bg: '#ecf5ff',
      border: '#d9ecff',
      dotColor: '#409eff',
      isCompleted: false,
      className: 'status-scheduled'
    }
  }

  if (normalized === 'DYEING') {
    return {
      label: '染色中',
      color: '#e6a23c',
      bg: '#fdf6ec',
      border: '#faecd8',
      dotColor: '#e6a23c',
      isCompleted: false,
      className: 'status-dyeing'
    }
  }

  if (normalized === 'FIXING') {
    return {
      label: '固色中',
      color: '#e6a23c',
      bg: '#fdf6ec',
      border: '#faecd8',
      dotColor: '#e6a23c',
      isCompleted: false,
      className: 'status-fixing'
    }
  }

  if (normalized === 'QUALITY_CHECK') {
    return {
      label: '质检中',
      color: '#e6a23c',
      bg: '#fdf6ec',
      border: '#faecd8',
      dotColor: '#e6a23c',
      isCompleted: false,
      className: 'status-quality-check'
    }
  }

  return {
    label: status || '未排缸',
    color: '#909399',
    bg: '#f4f4f5',
    border: '#dcdfe6',
    dotColor: '#909399',
    isCompleted: false,
    className: 'status-default'
  }
}

/**
 * 按排缸日期聚合缸列表
 */
export function groupVatsByDate(vats: VatCalendarVatVO[]): Record<string, VatCalendarVatVO[]> {
  const map: Record<string, VatCalendarVatVO[]> = {}
  if (!vats || !Array.isArray(vats)) return map

  for (const vat of vats) {
    if (!vat.scheduledDate) continue
    if (!map[vat.scheduledDate]) {
      map[vat.scheduledDate] = []
    }
    map[vat.scheduledDate].push(vat)
  }
  return map
}

/**
 * 过滤缸列表
 */
export function filterVats(
  vats: VatCalendarVatVO[],
  filters: { dyeingFactoryId?: number; status?: string }
): VatCalendarVatVO[] {
  if (!vats || !Array.isArray(vats)) return []

  return vats.filter((vat) => {
    if (filters.dyeingFactoryId && vat.dyeingFactoryId !== filters.dyeingFactoryId) {
      return false
    }
    if (filters.status && vat.status !== filters.status) {
      return false
    }
    return true
  })
}

/**
 * 校验指定缸是否允许改期（COMPLETED 缸前端禁用）
 */
export function canRescheduleVat(vat: { status?: string } | null | undefined): boolean {
  if (!vat || !vat.status) return false
  return vat.status.toUpperCase() !== 'COMPLETED'
}

/**
 * 提取并格式化日期天数显示
 */
export function formatDayNumber(dayStr: string): string {
  if (!dayStr) return ''
  const parts = dayStr.split('-')
  return parts.length >= 3 ? String(parseInt(parts[2], 10)) : dayStr
}

/**
 * 月份位移计算
 */
export function shiftMonth(baseDate: Date, deltaMonths: number): Date {
  const d = new Date(baseDate.getTime())
  d.setDate(1) // 避免月末溢出
  d.setMonth(d.getMonth() + deltaMonths)
  return d
}

/**
 * 构造改期更新请求参数
 */
export function buildReschedulePayload(vat: VatCalendarVatVO, newDate: string) {
  if (!canRescheduleVat(vat)) {
    throw new Error('已完成的缸不允许修改排缸日期')
  }
  if (!newDate) {
    throw new Error('新排缸日期不能为空')
  }
  return {
    id: vat.feedbackId,
    vatNo: vat.vatNo,
    quantity: vat.quantity ?? 0,
    pieces: vat.pieces,
    scheduledDate: newDate
  }
}
