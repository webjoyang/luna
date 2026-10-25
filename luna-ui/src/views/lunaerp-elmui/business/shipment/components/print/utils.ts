import type { ShipmentItemVO, ShipmentPrintVO } from '@/api/lunaerp-elmui/business/shipment'

export interface FlatRoll {
  rollNo: string
  productName: string
  productCode: string
  customerColorNo?: string
  vatNo?: string
  weight?: number
  lengthM?: number
  remark?: string
}

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

/**
 * 计算明细合计金额
 */
export function computeItemsTotalAmount(items?: ShipmentItemVO[] | null): number {
  if (!items || !items.length) return 0
  return items.reduce((acc, cur) => acc + (cur.amount || 0), 0)
}

/**
 * 计算总计金额（含运费）
 */
export function computeGrandTotalAmount(shipment?: ShipmentPrintVO | null): number {
  if (!shipment) return 0
  const total = shipment.totalAmount != null ? Number(shipment.totalAmount) : computeItemsTotalAmount(shipment.items)
  const freight = shipment.freightFee != null ? Number(shipment.freightFee) : 0
  return total + freight
}

/**
 * 计算总匹数
 */
export function computeTotalPieces(shipment?: ShipmentPrintVO | null): number {
  if (!shipment) return 0
  if (shipment.totalPieces != null) {
    return shipment.totalPieces
  }
  const items = shipment.items || []
  return items.reduce((acc, cur) => acc + (cur.pieces || 0), 0)
}

/**
 * 汇总多单位发货数量文本，如 "100.00 KG / 50.00 M"
 */
export function computeTotalQuantityText(items?: ShipmentItemVO[] | null): string {
  if (!items || !items.length) return '0'
  const unitMap: Record<string, number> = {}
  for (const item of items) {
    const itemUnit = item.unit || 'KG'
    unitMap[itemUnit] = (unitMap[itemUnit] || 0) + (item.quantity != null ? Number(item.quantity) : 0)
  }
  return Object.entries(unitMap)
    .map(([unit, quantity]) => `${quantity.toFixed(2)} ${unit}`)
    .join(' / ')
}

/**
 * 扁平化装箱清单细码数据
 */
export function flattenRollList(items?: ShipmentItemVO[] | null): FlatRoll[] {
  if (!items || !items.length) return []
  const list: FlatRoll[] = []
  for (const item of items) {
    if (item.rolls && item.rolls.length > 0) {
      for (const roll of item.rolls) {
        list.push({
          rollNo: roll.rollNo || '—',
          productName: item.productName || '—',
          productCode: item.productCode || '',
          customerColorNo: item.customerColorNo,
          vatNo: item.vatNo,
          weight: roll.weight,
          lengthM: roll.lengthM,
          remark: roll.remark || item.remark
        })
      }
    } else {
      // 若明细未录入细卷，以整批一行呈现
      list.push({
        rollNo: `整批发货 (${item.pieces || 1}匹)`,
        productName: item.productName || '—',
        productCode: item.productCode || '',
        customerColorNo: item.customerColorNo,
        vatNo: item.vatNo,
        weight: item.quantity,
        lengthM: undefined,
        remark: item.remark ? `明细备注：${item.remark}` : '未细分单卷'
      })
    }
  }
  return list
}

/**
 * 构建发货单门户签收二维码 URL
 */
export function buildPortalShipmentQrUrl(baseUrl?: string | null, shipmentNo?: string | null): string {
  if (!baseUrl || !baseUrl.trim() || !shipmentNo) return ''
  const cleanBase = baseUrl.trim().replace(/\/+$/, '')
  return `${cleanBase}/portal/shipment?no=${encodeURIComponent(shipmentNo)}`
}
