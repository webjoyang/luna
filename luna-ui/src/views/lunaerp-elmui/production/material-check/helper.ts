import type {
  MaterialCheckRowVO,
  MissingBomOrderVO,
  MissingWeightOrderVO
} from '@/api/lunaerp-elmui/production/material-check'

/**
 * 齐套状态定义（前端派生，不入字典，ADR-0041）
 * - 齐套：可用库存 >= 需求量
 * - 在途可覆盖：可用库存 < 需求量，但可用库存 + 在途量 >= 需求量（即含在途缺口 = 0）
 * - 缺料：可用库存 + 在途量 < 需求量（即含在途缺口 > 0）
 */
export type MaterialCheckStatus = 'COMPLETE' | 'TRANSIT_COVERED' | 'SHORTAGE'

export interface MaterialStatusResult {
  status: MaterialCheckStatus
  label: string
  tagType: 'success' | 'warning' | 'danger'
}

/**
 * 派生单行纱线规格的齐套状态
 */
export function deriveMaterialStatus(
  row: Pick<
    MaterialCheckRowVO,
    'demandQty' | 'stockQty' | 'availableStock' | 'inTransitQty' | 'shortageWithoutInTransit' | 'shortageWithInTransit'
  >
): MaterialStatusResult {
  const stock = row.availableStock ?? row.stockQty ?? 0
  const demand = row.demandQty ?? 0
  const inTransit = row.inTransitQty ?? 0

  // 1. 齐套：可用库存 >= 需求量（或 shortageWithoutInTransit <= 0）
  const shortageWithout =
    row.shortageWithoutInTransit !== undefined
      ? Number(row.shortageWithoutInTransit)
      : Math.max(0, demand - stock)

  if (shortageWithout <= 0) {
    return {
      status: 'COMPLETE',
      label: '齐套',
      tagType: 'success'
    }
  }

  // 2. 在途可覆盖：含在途缺口 <= 0（或 stock + inTransit >= demand）
  const shortageWith =
    row.shortageWithInTransit !== undefined
      ? Number(row.shortageWithInTransit)
      : Math.max(0, demand - stock - inTransit)

  if (shortageWith <= 0) {
    return {
      status: 'TRANSIT_COVERED',
      label: '在途可覆盖',
      tagType: 'warning'
    }
  }

  // 3. 缺料：含在途缺口 > 0
  return {
    status: 'SHORTAGE',
    label: '缺料',
    tagType: 'danger'
  }
}

/**
 * 格式化建议采购量展示
 * 为 0 或更小时显示 "已齐套/在途中"，> 0 显示具体 KG 数值
 */
export function formatSuggestedQty(qty?: number): { isZero: boolean; text: string } {
  const val = Number(qty ?? 0)
  if (val <= 0) {
    return {
      isZero: true,
      text: '已齐套/在途中'
    }
  }
  return {
    isZero: false,
    text: `${val.toFixed(2)} KG`
  }
}

/**
 * 顶部汇总徽标计算（随"含在途"开关切换口径）
 * - 不含在途：以 shortageWithoutInTransit 为准
 * - 含在途：以 shortageWithInTransit 为准
 */
export function calculateSummaryBadges(
  rows: MaterialCheckRowVO[] = [],
  includeInTransit = false
): { shortageSpecCount: number; totalShortageKg: number } {
  if (!rows || rows.length === 0) {
    return { shortageSpecCount: 0, totalShortageKg: 0 }
  }

  let count = 0
  let total = 0

  for (const r of rows) {
    const shortage = includeInTransit
      ? Number(r.shortageWithInTransit ?? 0)
      : Number(r.shortageWithoutInTransit ?? 0)

    if (shortage > 0) {
      count++
      total += shortage
    }
  }

  return {
    shortageSpecCount: count,
    totalShortageKg: Number(total.toFixed(2))
  }
}

/**
 * 在途口径已知限制（写入页面说明 tooltip，spec §4.4）
 */
export const IN_TRANSIT_LIMITS = [
  '① 在途以采购单状态近似：部分到货无法自动扣减（入库单不关联采购单），需人工及时把收齐的 ACTIVE 采购单置 COMPLETED；未及时完结会高估在途。',
  '② 试算为时点快照，不预留/不冻结库存；试算后库存变动不会通知。',
  '③ 需求按计划重量理论折算，不含实际损耗差异；胚布单损耗率口径（ADR-0010）不参与织造齐套。'
]

/**
 * 跳转配比 BOM 路由参数
 */
export function getJumpToBomLocation(order: MissingBomOrderVO) {
  return {
    path: '/production/yarn-recipe-bom',
    query: {
      productId: order.productId !== undefined ? String(order.productId) : undefined,
      productCode: order.productCode,
      productName: order.productName
    }
  }
}

/**
 * 跳转织造单工作台路由参数
 */
export function getJumpToWeavingOrderLocation(order: MissingWeightOrderVO) {
  return {
    path: '/production/weaving-order',
    query: {
      id: order.weavingOrderId !== undefined ? String(order.weavingOrderId) : undefined,
      code: order.weavingOrderCode
    }
  }
}

/**
 * 跳转纱线采购单路由参数（带纱线规格关键词）
 */
export function getJumpToYarnOrderLocation(row: MaterialCheckRowVO) {
  return {
    path: '/production/yarn-order',
    query: {
      keyword: row.yarnSpecName || undefined
    }
  }
}
