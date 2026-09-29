import { NEAR_DELIVERY_DAYS } from '@/api/lunaerp-elmui/production/production-progress'

/**
 * 生产进度展示层共用口径。
 *
 * 列表页与单合同抽屉必须对同一个数字、同一条预警给出同一种渲染，
 * 否则「口径唯一」（ADR-0034）会在最后一米被前端自己破坏，故这些函数只此一份。
 */

/** el-tag 配色 */
export type ProgressTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 数量：千分位两位小数 */
export const formatKg = (value?: number | string) => {
  return Number(value ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 距交期天数：正数=还剩几天，负数=已逾期几天，无交期或非法日期=null */
export const remainingDays = (deliveryDate?: string): number | null => {
  if (!deliveryDate) return null
  const delivery = new Date(`${deliveryDate}T00:00:00`)
  if (Number.isNaN(delivery.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((delivery.getTime() - today.getTime()) / 86400000)
}

/** 交期着色：已逾期红、临期（≤NEAR_DELIVERY_DAYS 天）橙、无交期灰、其余常规 */
export const deliveryDateClass = (deliveryDate?: string) => {
  const days = remainingDays(deliveryDate)
  if (days === null) return 'text-muted'
  if (days < 0) return 'text-danger'
  if (days <= NEAR_DELIVERY_DAYS) return 'text-warning'
  return ''
}

/** 剩余天数的中文表述，无交期返回空串 */
export const remainingDaysText = (deliveryDate?: string) => {
  const days = remainingDays(deliveryDate)
  if (days === null) return ''
  if (days < 0) return `已逾期 ${Math.abs(days)} 天`
  if (days === 0) return '今日交期'
  return `剩 ${days} 天`
}

/** 超期/延期缸是已发生的延误，临期是风险提示 */
export const alertTagType = (alert: string): ProgressTagType => {
  return alert === '临期' ? 'warning' : 'danger'
}
