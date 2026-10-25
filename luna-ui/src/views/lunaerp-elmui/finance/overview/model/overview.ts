/**
 * 格式化百分比
 */
export const formatPercent = (rate: number | string | null | undefined, precision = 2): string => {
  if (rate === null || rate === undefined || rate === '') {
    return '0.00%'
  }
  const num = Number(rate)
  if (isNaN(num)) {
    return '0.00%'
  }
  return `${(num * 100).toFixed(precision)}%`
}

/**
 * 日期补零辅助函数
 */
const padZero = (n: number): string => (n < 10 ? `0${n}` : `${n}`)

/**
 * 获取标准期间预设（返回 [YYYY-MM-DD, YYYY-MM-DD]）
 */
export const getPeriodPresets = (nowDate = new Date()) => {
  const year = nowDate.getFullYear()
  const month = nowDate.getMonth() // 0-based

  // 1. 本月 (当月1号 ~ 当月最后一天)
  const thisMonthStart = `${year}-${padZero(month + 1)}-01`
  const lastDayOfThisMonth = new Date(year, month + 1, 0).getDate()
  const thisMonthEnd = `${year}-${padZero(month + 1)}-${padZero(lastDayOfThisMonth)}`

  // 2. 上月
  const lastMonthDate = new Date(year, month - 1, 1)
  const lastMonthYear = lastMonthDate.getFullYear()
  const lastMonthIndex = lastMonthDate.getMonth()
  const lastDayOfLastMonth = new Date(lastMonthYear, lastMonthIndex + 1, 0).getDate()
  const lastMonthStart = `${lastMonthYear}-${padZero(lastMonthIndex + 1)}-01`
  const lastMonthEnd = `${lastMonthYear}-${padZero(lastMonthIndex + 1)}-${padZero(lastDayOfLastMonth)}`

  // 3. 本季度
  const quarterStartMonth = Math.floor(month / 3) * 3
  const quarterStart = `${year}-${padZero(quarterStartMonth + 1)}-01`
  const quarterEndLastDay = new Date(year, quarterStartMonth + 3, 0).getDate()
  const quarterEnd = `${year}-${padZero(quarterStartMonth + 3)}-${padZero(quarterEndLastDay)}`

  // 4. 本年度
  const thisYearStart = `${year}-01-01`
  const thisYearEnd = `${year}-12-31`

  return {
    thisMonth: [thisMonthStart, thisMonthEnd] as [string, string],
    lastMonth: [lastMonthStart, lastMonthEnd] as [string, string],
    thisQuarter: [quarterStart, quarterEnd] as [string, string],
    thisYear: [thisYearStart, thisYearEnd] as [string, string]
  }
}

/**
 * 计算经营毛利指标
 */
export const computeGrossMarginMetrics = (revenue: number, cost: number) => {
  const grossProfit = revenue - cost
  const grossMarginRate = revenue > 0 ? grossProfit / revenue : 0
  return {
    grossProfit,
    grossMarginRate
  }
}
