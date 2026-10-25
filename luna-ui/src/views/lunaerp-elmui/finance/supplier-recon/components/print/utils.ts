/**
 * 供应商对账单打印格式化纯函数工具库
 */

/**
 * 行类型打印文案映射
 */
export function formatLineTypePrintLabel(lineType?: string): string {
  switch (lineType) {
    case 'PROCESS':
      return '加工费'
    case 'PURCHASE':
      return '采购货款'
    case 'ADJUST':
      return '调整项'
    default:
      return lineType || '—'
  }
}

/**
 * 检验类型文案映射
 */
export function formatInspectionTypeLabel(type?: string): string {
  switch (type) {
    case 'GREIGE':
      return '胚布检验 (织费)'
    case 'FINISHED':
      return '成品检验 (染费)'
    default:
      return type || ''
  }
}

/**
 * 计价基准文案映射
 */
export function formatPricingBasisLabel(basis?: string): string {
  switch (basis) {
    case 'METER':
      return '米 (M)'
    case 'KG':
      return '千克 (KG)'
    default:
      return basis || '—'
  }
}

/**
 * 计算打印明细分类小计
 */
export function calculatePrintSubtotals(items: Array<{ lineType?: string; lineAmount?: number }>): {
  processTotal: number
  purchaseTotal: number
  adjustTotal: number
  grandTotal: number
} {
  let processTotal = 0
  let purchaseTotal = 0
  let adjustTotal = 0

  if (items && items.length > 0) {
    for (const it of items) {
      const amount = Number(it.lineAmount || 0)
      if (it.lineType === 'PROCESS') {
        processTotal += amount
      } else if (it.lineType === 'PURCHASE') {
        purchaseTotal += amount
      } else if (it.lineType === 'ADJUST') {
        adjustTotal += amount
      }
    }
  }

  const grandTotal = processTotal + purchaseTotal + adjustTotal

  return {
    processTotal: Number(processTotal.toFixed(2)),
    purchaseTotal: Number(purchaseTotal.toFixed(2)),
    adjustTotal: Number(adjustTotal.toFixed(2)),
    grandTotal: Number(grandTotal.toFixed(2))
  }
}

/**
 * 格式化打印时间
 */
export function formatPrintTime(timeStr?: string | null): string {
  if (timeStr) {
    return timeStr
  }
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}
