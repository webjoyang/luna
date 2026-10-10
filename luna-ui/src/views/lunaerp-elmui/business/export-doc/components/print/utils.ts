/**
 * 出口单证打印工具方法
 */

export function getCurrencySymbol(currency?: string): string {
  if (!currency) return '$'
  const c = currency.trim().toUpperCase()
  switch (c) {
    case 'USD':
      return '$'
    case 'EUR':
      return '€'
    case 'GBP':
      return '£'
    case 'RMB':
    case 'CNY':
      return '¥'
    case 'JPY':
      return '¥'
    case 'HKD':
      return 'HK$'
    default:
      return c + ' '
  }
}

export function formatAmount(val?: number | string | null, decimals = 2): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

export function safeParseJson<T = any>(str?: string | null, defaultVal: T = {} as T): T {
  if (!str || typeof str !== 'string') return defaultVal
  try {
    return JSON.parse(str) as T
  } catch {
    return defaultVal
  }
}

export function formatDate(val?: string | null): string {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
