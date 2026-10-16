import type {
  ExchangeRateRespVO,
  ExchangeRatePageReqVO,
  ExchangeRateSaveReqVO,
  ExchangeRateUpdateReqVO
} from '@/api/lunaerp-elmui/finance/exchange-rate'

export type {
  ExchangeRateRespVO,
  ExchangeRatePageReqVO,
  ExchangeRateSaveReqVO,
  ExchangeRateUpdateReqVO
}

/**
 * 财务汇率业务错误码常量（对齐 FinanceErrorCodeConstants 1-030-020-200~202）
 */
export const EXCHANGE_RATE_ERROR_CODES = {
  EXCHANGE_RATE_NOT_FOUND: 1030020200,
  EXCHANGE_RATE_NO_DUPLICATE: 1030020201,
  EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN: 1030020202
} as const

/**
 * 判断是否为本位币（RMB / CNY，不区分大小写）
 */
export const isBaseCurrency = (currency?: string | null): boolean => {
  if (!currency || typeof currency !== 'string') return false
  const upper = currency.trim().toUpperCase()
  return upper === 'RMB' || upper === 'CNY'
}

/**
 * 格式化汇率（展示 8 位小数高精度）
 */
export const formatExchangeRate = (
  rate: number | string | null | undefined,
  precision = 8
): string => {
  if (rate === null || rate === undefined || rate === '') {
    return '—'
  }
  const num = Number(rate)
  if (isNaN(num)) {
    return '—'
  }
  return num.toFixed(precision)
}

/**
 * 校验汇率输入合法性（必须为正数 > 0）
 */
export const validateExchangeRateInput = (
  rate: number | string | null | undefined
): { valid: boolean; message?: string } => {
  if (rate === null || rate === undefined || rate === '') {
    return { valid: false, message: '请输入汇率' }
  }
  const num = Number(rate)
  if (isNaN(num) || num <= 0) {
    return { valid: false, message: '汇率必须为大于 0 的有效数值' }
  }
  return { valid: true }
}

/**
 * 校验币种输入合法性（不可为空，不可为本位币）
 */
export const validateCurrencyInput = (
  currency: string | null | undefined
): { valid: boolean; message?: string } => {
  if (!currency || !currency.trim()) {
    return { valid: false, message: '请输入或选择币种' }
  }
  if (isBaseCurrency(currency)) {
    return { valid: false, message: '本位币（RMB/CNY）汇率固定为1，无需且禁止配置' }
  }
  return { valid: true }
}

/**
 * 解析汇率业务错误提示
 */
export const parseExchangeRateErrorMessage = (code: number, defaultMsg = '操作失败'): string => {
  switch (code) {
    case EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_NOT_FOUND:
      return '未找到生效汇率'
    case EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_NO_DUPLICATE:
      return '同币种同生效日汇率已存在，不可重复配置'
    case EXCHANGE_RATE_ERROR_CODES.EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN:
      return '本位币（RMB/CNY）固定为1，无需且禁止配置'
    default:
      return defaultMsg
  }
}
