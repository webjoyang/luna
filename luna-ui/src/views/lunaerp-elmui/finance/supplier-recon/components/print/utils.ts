/**
 * 格式化当前/指定打印时间
 */
export function formatPrintTime(dateInput?: string | Date): string {
  const d = dateInput ? new Date(dateInput) : new Date()
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

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
 * 人民币金额大写转换（采用整分计算，杜绝浮点精度丢失）
 */
export function formatRmbUppercase(totalAmount?: number | null): string {
  if (totalAmount == null || isNaN(totalAmount) || totalAmount === 0) return '零元整'
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const bigUnits = [
    ['元', '万', '亿'],
    ['', '拾', '佰', '仟']
  ]
  const prefix = totalAmount < 0 ? '负' : ''
  const totalCents = Math.round(Math.abs(totalAmount) * 100)
  const integerPart = Math.floor(totalCents / 100)
  const jiao = Math.floor(totalCents / 10) % 10
  const fen = totalCents % 10

  let fractionStr = ''
  if (jiao === 0 && fen === 0) {
    fractionStr = '整'
  } else {
    fractionStr =
      (jiao > 0 ? digits[jiao] + '角' : integerPart > 0 ? '零' : '') +
      (fen > 0 ? digits[fen] + '分' : '')
  }

  let integerStr = ''
  let remainingInteger = integerPart
  for (let i = 0; i < bigUnits[0].length && remainingInteger > 0; i++) {
    let sectionStr = ''
    for (let j = 0; j < bigUnits[1].length && remainingInteger > 0; j++) {
      sectionStr = digits[remainingInteger % 10] + bigUnits[1][j] + sectionStr
      remainingInteger = Math.floor(remainingInteger / 10)
    }
    integerStr =
      sectionStr.replace(/(零.)*零$/, '').replace(/^$/, '零') + bigUnits[0][i] + integerStr
  }

  const result =
    prefix +
    integerStr.replace(/(零.)*零元/, '元').replace(/(零.)+/g, '零').replace(/^整$/, '零元整') +
    fractionStr
  return result || '零元整'
}
