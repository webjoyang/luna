/**
 * 格式化金额千分位（保留2位小数）
 */
export function formatAmount(val?: number | string | null): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 预设资金账户列表（桩定义）
 */
export const DEFAULT_FINANCE_ACCOUNTS = [
  { id: 1, name: '基本户 - 中国工商银行 (622202******001)', disabled: false },
  { id: 2, name: '一般户 - 招商银行 (621483******002)', disabled: false },
  { id: 3, name: '企业现金账户 (CASH-001)', disabled: false },
  { id: 4, name: '已停用账户 (DISABLED)', disabled: true }
]
