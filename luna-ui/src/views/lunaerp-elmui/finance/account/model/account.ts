/**
 * 资金账户简易实体接口（领域层）
 */
export interface SimpleAccountItem {
  id: number
  accountName: string
  currency: string
  currentBalance?: number
}

/**
 * 资金账户域错误码常量（对齐 FinanceErrorCodeConstants）
 */
export const FINANCE_ACCOUNT_ERROR_CODES = {
  ACCOUNT_NOT_EXISTS: 1030020100, // 020_100
  ACCOUNT_DISABLED: 1030020101, // 020_101
  ACCOUNT_NAME_DUPLICATE: 1030020102, // 020_102
  ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN: 1030020103, // 020_103
  ACCOUNT_OPENING_BALANCE_IMMUTABLE: 1030020104, // 020_104
  ACCOUNT_NO_REQUIRED: 1030020105, // 020_105
  ACCOUNT_CURRENCY_MISMATCH: 1030020106, // 020_106
  TRANSFER_ACCOUNT_SAME: 1030020107, // 020_107
  TRANSFER_NOT_EXISTS: 1030020108, // 020_108
  OTHER_TXN_NOT_EXISTS: 1030020109, // 020_109
  OTHER_TXN_CATEGORY_DIRECTION_MISMATCH: 1030020110, // 020_110
  PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH: 1030023004 // 023_004
} as const

/**
 * 格式化金额（千分位，保留 2 位小数）
 */
export const formatAmount = (amount: number | string | null | undefined, precision = 2): string => {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const num = Number(amount)
  if (isNaN(num)) {
    return '0.00'
  }
  const parts = num.toFixed(precision).split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.join('.')
}

/**
 * 账号按账户类型条件必填判断（BANK/WECHAT/ALIPAY 必填，CASH 可空）
 */
export const isAccountNoRequired = (accountType?: string | null): boolean => {
  if (!accountType) return false
  const upper = accountType.trim().toUpperCase()
  return upper === 'BANK' || upper === 'WECHAT' || upper === 'ALIPAY'
}

/**
 * 账号脱敏掩码展示
 */
export const maskAccountNo = (accountNo?: string | null): string => {
  if (!accountNo || !accountNo.trim()) {
    return '—'
  }
  const str = accountNo.trim()
  if (str.includes('*')) {
    return str
  }
  if (str.length <= 8) {
    return str
  }
  const start = str.slice(0, 4)
  const end = str.slice(-4)
  return `${start} **** **** ${end}`
}

/**
 * 其他收支类别按收支方向联动过滤
 */
export const OTHER_TXN_CATEGORIES_DEBIT = [
  { value: 'INTEREST_IN', label: '利息收入' },
  { value: 'WASTE_IN', label: '废品收入' },
  { value: 'OTHER_IN', label: '其他收入' }
]

export const OTHER_TXN_CATEGORIES_CREDIT = [
  { value: 'FEE_OUT', label: '手续费' },
  { value: 'COST_OUT', label: '工本费' },
  { value: 'OTHER_OUT', label: '其他支出' }
]

export const getOtherTxnCategoriesByDirection = (direction: 'DEBIT' | 'CREDIT' | string) => {
  if (direction === 'DEBIT') {
    return OTHER_TXN_CATEGORIES_DEBIT
  }
  if (direction === 'CREDIT') {
    return OTHER_TXN_CATEGORIES_CREDIT
  }
  return [...OTHER_TXN_CATEGORIES_DEBIT, ...OTHER_TXN_CATEGORIES_CREDIT]
}

/**
 * 转账前置业务校验
 */
export const validateTransfer = (
  fromAccount?: SimpleAccountItem | null,
  toAccount?: SimpleAccountItem | null,
  amount?: number | null
): { valid: boolean; message?: string } => {
  if (!fromAccount || !toAccount) {
    return { valid: false, message: '请选择转出与转入资金账户' }
  }
  if (fromAccount.id === toAccount.id) {
    return { valid: false, message: '转出账户与转入账户不能相同（错误码 020_107）' }
  }
  if (fromAccount.currency !== toAccount.currency) {
    return {
      valid: false,
      message: `转出账户（${fromAccount.currency}）与转入账户（${toAccount.currency}）币种不一致，暂不支持跨币种转账（错误码 020_106）`
    }
  }
  if (!amount || amount <= 0) {
    return { valid: false, message: '转账金额必须大于 0' }
  }
  if (fromAccount.currentBalance !== undefined && amount > fromAccount.currentBalance) {
    return {
      valid: false,
      message: `转账金额（${formatAmount(amount)}）超出转出账户可用余额（${formatAmount(fromAccount.currentBalance)}）`
    }
  }
  return { valid: true }
}

/**
 * 解析资金账户业务错误友好提示
 */
export const parseAccountErrorMessage = (
  code: number | string | undefined,
  defaultMsg = '操作失败'
): string => {
  const numericCode = Number(code)
  switch (numericCode) {
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_NOT_EXISTS:
      return '资金账户不存在或已被删除'
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_DISABLED:
      return '资金账户已停用，无法操作'
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_NAME_DUPLICATE:
      return '账户名称已存在（含已删除账户），请更换账户名称'
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN:
      return '该账户已有分录引用，禁止删除。如不再使用请执行【停用】操作'
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_OPENING_BALANCE_IMMUTABLE:
      return '该账户已产生分录，期初余额不可修改'
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_NO_REQUIRED:
      return '银行、微信与支付宝账户必须填写账号或标识'
    case FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_CURRENCY_MISMATCH:
      return '账户币种不一致，暂不支持跨币种转账'
    case FINANCE_ACCOUNT_ERROR_CODES.TRANSFER_ACCOUNT_SAME:
      return '转出账户与转入账户不能相同'
    case FINANCE_ACCOUNT_ERROR_CODES.TRANSFER_NOT_EXISTS:
      return '转账单不存在'
    case FINANCE_ACCOUNT_ERROR_CODES.OTHER_TXN_NOT_EXISTS:
      return '其他收支单不存在'
    case FINANCE_ACCOUNT_ERROR_CODES.OTHER_TXN_CATEGORY_DIRECTION_MISMATCH:
      return '收支类别与收支方向不匹配'
    case FINANCE_ACCOUNT_ERROR_CODES.PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH:
      return '资金账户可用余额不足'
    default:
      return defaultMsg
  }
}
