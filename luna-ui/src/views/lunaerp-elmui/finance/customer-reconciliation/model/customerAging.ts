/**
 * 客户账龄汇总纯模型接口
 */
export interface CustomerAgingSummaryData {
  customerId?: number
  customerName?: string
  creditDays?: number
  salespersonUserId?: number
  salespersonName?: string
  currency?: string
  totalArBalance?: number
  bucket0To30?: number
  bucket31To60?: number
  bucket61To90?: number
  bucket91To180?: number
  bucket181Plus?: number
  advanceBalance?: number
  overdueAmount?: number
  agingDate?: string
}

/**
 * 五桶常量定义
 */
export const AGING_BUCKETS = [
  { key: 'bucket0To30', pctKey: 'pct0To30', label: '0–30天', color: '#67c23a' },
  { key: 'bucket31To60', pctKey: 'pct31To60', label: '31–60天', color: '#409eff' },
  { key: 'bucket61To90', pctKey: 'pct61To90', label: '61–90天', color: '#e6a23c' },
  { key: 'bucket91To180', pctKey: 'pct91To180', label: '91–180天', color: '#f56c6c' },
  { key: 'bucket181Plus', pctKey: 'pct181Plus', label: '181天以上', color: '#909399' }
] as const

/**
 * 格式化金额（千分位，保留 2 位小数）
 */
export function formatMoney(val?: number | string | null): string {
  if (val == null || val === '') {
    return '0.00'
  }
  const num = Number(val)
  if (isNaN(num)) {
    return '0.00'
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 五桶标签展示（Tag 类型与描述）
 */
export function formatAgingBucketTag(bucket?: string): {
  type: '' | 'success' | 'warning' | 'danger' | 'info'
  label: string
} {
  switch (bucket) {
    case '0-30':
      return { type: 'success', label: '0–30天' }
    case '31-60':
      return { type: '', label: '31–60天' }
    case '61-90':
      return { type: 'warning', label: '61–90天' }
    case '91-180':
      return { type: 'danger', label: '91–180天' }
    case '181+':
      return { type: 'info', label: '181天以上' }
    default:
      return { type: 'info', label: bucket || '—' }
  }
}

/**
 * 逾期状态标签展示
 */
export function formatAgingOverdueTag(
  isOverdue?: boolean,
  overdueDays?: number
): {
  type: '' | 'success' | 'warning' | 'danger' | 'info'
  label: string
} {
  if (isOverdue) {
    return {
      type: 'danger',
      label: `已逾期 ${overdueDays || 0} 天`
    }
  }
  return {
    type: 'success',
    label: '正常未逾期'
  }
}

/**
 * 计算各桶金额占比（用于柱状/占比条展示）
 */
export function calculateAgingBucketPercentages(row?: CustomerAgingSummaryData): {
  pct0To30: number
  pct31To60: number
  pct61To90: number
  pct91To180: number
  pct181Plus: number
} {
  if (!row || !row.totalArBalance || Number(row.totalArBalance) <= 0) {
    return {
      pct0To30: 0,
      pct31To60: 0,
      pct61To90: 0,
      pct91To180: 0,
      pct181Plus: 0
    }
  }

  const total = Number(row.totalArBalance)
  const calc = (amt?: number) => {
    if (!amt || amt <= 0) return 0
    return Math.round((amt / total) * 1000) / 10 // 保留1位小数百分比
  }

  return {
    pct0To30: calc(row.bucket0To30),
    pct31To60: calc(row.bucket31To60),
    pct61To90: calc(row.bucket61To90),
    pct91To180: calc(row.bucket91To180),
    pct181Plus: calc(row.bucket181Plus)
  }
}

/**
 * 汇总多个客户行的总应收与各桶合计
 */
export function calculateAgingGrandTotals(list: CustomerAgingSummaryData[]): {
  totalAr: number
  totalAdvance: number
  totalOverdue: number
  bucket0To30: number
  bucket31To60: number
  bucket61To90: number
  bucket91To180: number
  bucket181Plus: number
} {
  let totalAr = 0
  let totalAdvance = 0
  let totalOverdue = 0
  let b0To30 = 0
  let b31To60 = 0
  let b61To90 = 0
  let b91To180 = 0
  let b181Plus = 0

  for (const item of list) {
    totalAr += Number(item.totalArBalance || 0)
    totalAdvance += Number(item.advanceBalance || 0)
    totalOverdue += Number(item.overdueAmount || 0)
    b0To30 += Number(item.bucket0To30 || 0)
    b31To60 += Number(item.bucket31To60 || 0)
    b61To90 += Number(item.bucket61To90 || 0)
    b91To180 += Number(item.bucket91To180 || 0)
    b181Plus += Number(item.bucket181Plus || 0)
  }

  const round2 = (val: number) => Math.round((val + Number.EPSILON) * 100) / 100

  return {
    totalAr: round2(totalAr),
    totalAdvance: round2(totalAdvance),
    totalOverdue: round2(totalOverdue),
    bucket0To30: round2(b0To30),
    bucket31To60: round2(b31To60),
    bucket61To90: round2(b61To90),
    bucket91To180: round2(b91To180),
    bucket181Plus: round2(b181Plus)
  }
}
