import type {
  ProgressFeedbackCalendarVatVO,
  ProgressFeedbackUpdateVO
} from '@/api/lunaerp-elmui/production/progress-feedback'

/**
 * 格式化 Date 为 YYYY-MM
 */
export function formatYearMonth(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

/**
 * 格式化 Date 为 YYYY-MM-DD
 */
export function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 获取相邻月份（如 offset = 1 为下月，offset = -1 为上月）
 * @param yearMonth 格式 YYYY-MM
 * @param offset 月份偏移量
 */
export function getAdjacentMonth(yearMonth: string, offset: number): string {
  const [yearStr, monthStr] = yearMonth.split('-')
  const year = parseInt(yearStr, 10)
  const month = parseInt(monthStr, 10)
  const targetDate = new Date(year, month - 1 + offset, 1)
  return formatYearMonth(targetDate)
}

/**
 * 按 scheduledDate 将扁平缸列表聚合成日期分组，并按缸号字母升序排序
 */
export function groupVatsByDate(
  vats: ProgressFeedbackCalendarVatVO[]
): Record<string, ProgressFeedbackCalendarVatVO[]> {
  const result: Record<string, ProgressFeedbackCalendarVatVO[]> = {}

  if (!vats || !Array.isArray(vats)) {
    return result
  }

  for (const vat of vats) {
    const dateStr = vat.scheduledDate ? vat.scheduledDate.trim() : ''
    if (!dateStr) {
      continue
    }
    if (!result[dateStr]) {
      result[dateStr] = []
    }
    result[dateStr].push(vat)
  }

  // 对每个日期的缸列表按 vatNo 字母序升序排序
  for (const dateStr of Object.keys(result)) {
    result[dateStr].sort((a, b) => (a.vatNo || '').localeCompare(b.vatNo || ''))
  }

  return result
}

/**
 * 状态样式配置
 */
export interface VatCalendarStatusStyle {
  color: string
  bg: string
  border: string
  tagType: '' | 'default' | 'primary' | 'success' | 'warning' | 'info' | 'danger'
  isCompleted: boolean
}

/**
 * 是否允许改期（COMPLETED 缸前端禁用，后端守卫兜底）
 */
export function canRescheduleVat(status?: string): boolean {
  if (!status) return true
  return status !== 'COMPLETED'
}

/**
 * 获取排缸日历中缸点的状态颜色与灰化样式
 * 遵循字典 lunaerp_progress_feedback_status：
 * - SCHEDULED: primary
 * - DYEING / FIXING / QUALITY_CHECK: warning
 * - COMPLETED: success 语义但月历中灰化处理 (COMPLETED 灰化)
 */
export function getVatCalendarStatusStyle(status?: string): VatCalendarStatusStyle {
  switch (status) {
    case 'SCHEDULED':
      return {
        color: '#409eff',
        bg: '#ecf5ff',
        border: '#d9ecff',
        tagType: 'primary',
        isCompleted: false
      }
    case 'DYEING':
    case 'FIXING':
    case 'QUALITY_CHECK':
      return {
        color: '#e6a23c',
        bg: '#fdf6ec',
        border: '#faecd8',
        tagType: 'warning',
        isCompleted: false
      }
    case 'COMPLETED':
      return {
        color: '#909399',
        bg: '#f4f4f5',
        border: '#e9e9eb',
        tagType: 'info',
        isCompleted: true
      }
    case 'NOT_SCHEDULED':
    default:
      return {
        color: '#909399',
        bg: '#f4f4f5',
        border: '#e9e9eb',
        tagType: 'info',
        isCompleted: false
      }
  }
}

/**
 * 构建排缸改期请求 VO
 * 严格保留既有工序日期、缸号、数量等字段，仅修改 scheduledDate
 */
export function buildRescheduleUpdateVO(
  existingDetail: {
    id?: number
    feedbackId?: number
    vatNo: string
    quantity: number
    pieces?: number
    status?: string
    dyeingStartedDate?: string
    fixingStartedDate?: string
    qualityCheckDate?: string
    completedDate?: string
    delayReason?: string
    remark?: string
  },
  newDate: string
): ProgressFeedbackUpdateVO {
  if (!canRescheduleVat(existingDetail.status)) {
    throw new Error('已完成缸禁止改期')
  }

  const vatId = existingDetail.id ?? existingDetail.feedbackId
  if (!vatId) {
    throw new Error('反馈缸ID不能为空')
  }

  return {
    id: vatId,
    vatNo: existingDetail.vatNo,
    quantity: existingDetail.quantity,
    pieces: existingDetail.pieces,
    status: existingDetail.status,
    scheduledDate: newDate,
    dyeingStartedDate: existingDetail.dyeingStartedDate,
    fixingStartedDate: existingDetail.fixingStartedDate,
    qualityCheckDate: existingDetail.qualityCheckDate,
    completedDate: existingDetail.completedDate,
    delayReason: existingDetail.delayReason,
    remark: existingDetail.remark
  }
}
