import type { MaterialCheckRowVO } from '@/api/lunaerp-elmui/production/material-check'

export type MaterialCheckStatusCode = 'READY' | 'TRANSIT_COVERED' | 'SHORTAGE'

export interface MaterialCheckStatusMeta {
  code: MaterialCheckStatusCode
  label: string
  tagType: 'success' | 'warning' | 'danger'
}

export type MaterialCheckRowDisplay = MaterialCheckRowVO

/**
 * 前端派生齐套状态标签（不入字典，spec §4.3 & §6.2）
 * - 齐套：可用库存 >= 需求量
 * - 在途可覆盖：可用库存 < 需求量，但含在途缺口 <= 0（可用 + 在途 >= 需求）
 * - 缺料：含在途缺口 > 0
 */
export function deriveMaterialCheckStatus(row: MaterialCheckRowDisplay): MaterialCheckStatusMeta {
  const req = Number(row.requiredQty || 0)
  const avail = Number(row.availableQty || 0)
  const shortageIncl = Number(row.shortageIncludeInTransit || 0)

  if (avail >= req) {
    return {
      code: 'READY',
      label: '齐套',
      tagType: 'success'
    }
  }

  if (shortageIncl <= 0) {
    return {
      code: 'TRANSIT_COVERED',
      label: '在途可覆盖',
      tagType: 'warning'
    }
  }

  return {
    code: 'SHORTAGE',
    label: '缺料',
    tagType: 'danger'
  }
}

/**
 * 建议采购量显示格式化（spec §4.3）
 * 为 0（或 <=0）显示"已齐套/在途中"，否则保留两位小数
 */
export function formatSuggestedPurchaseQty(qty: number): string {
  const num = Number(qty || 0)
  if (num <= 0) {
    return '已齐套/在途中'
  }
  return num.toFixed(2)
}

/**
 * 计算汇总徽标统计（缺料规格数与合计 KG，口径随"含在途"开关切换，spec §4.1, §4.3, §6.2）
 */
export function calculateSummaryStats(
  rows: MaterialCheckRowDisplay[],
  includeInTransit: boolean
): { shortageSpecCount: number; totalShortageQty: number } {
  let shortageSpecCount = 0
  let totalShortageQty = 0

  for (const row of rows) {
    const shortage = includeInTransit
      ? Number(row.shortageIncludeInTransit || 0)
      : Number(row.shortageExcludeInTransit || 0)

    if (shortage > 0) {
      shortageSpecCount += 1
      totalShortageQty += shortage
    }
  }

  return {
    shortageSpecCount,
    totalShortageQty: Number(totalShortageQty.toFixed(2))
  }
}

/**
 * 导航跳转路由辅助：跳纱线采购单带规格关键词（spec §4.3, §6.2）
 */
export function buildYarnOrderLocation(yarnSpecName?: string) {
  return {
    path: '/production/yarn-order',
    query: yarnSpecName ? { keyword: yarnSpecName } : {}
  }
}

/**
 * 导航跳转路由辅助：跳配比 BOM 管理页按产品过滤（spec §4.3, §6.2）
 */
export function buildYarnRecipeBomLocation(
  productId?: number,
  productCode?: string,
  productName?: string
) {
  const query: Record<string, string> = {}
  if (productId != null) query.productId = String(productId)
  if (productCode) query.productCode = productCode
  if (productName) query.productName = productName
  return {
    path: '/production/yarn-recipe-bom',
    query
  }
}

/**
 * 导航跳转路由辅助：跳织造单工作台补录计划重量/匹数（spec §4.3, §6.2）
 */
export function buildWeavingOrderWorkspaceLocation(weavingOrderId: number) {
  return {
    path: `/production/weaving-order/workspace/${weavingOrderId}`
  }
}

/**
 * 在途口径已知限制三条（spec §4.4，写入页面说明 tooltip）
 */
export const IN_TRANSIT_TOOLTIP_RESTRICTIONS: string[] = [
  '在途以"采购单状态"近似：部分到货无法自动扣减（入库单不关联采购单），需人工及时把收齐的 ACTIVE 采购单置 COMPLETED；未及时完结会高估在途。',
  '试算为时点快照，不预留/不冻结库存；试算后库存变动不会通知。',
  '需求按计划重量理论折算，不含实际损耗差异；胚布单 loss_rate 口径不参与织造齐套。'
]
