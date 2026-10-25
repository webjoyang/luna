import request from '@/config/axios'

/**
 * 齐套检查试算入参 VO
 */
export interface MaterialCheckReqVO {
  /**
   * 织造单 ID 列表；为空或空集合时，试算当前租户全部 DRAFT/ACTIVE 织造单（前端"全选未完工"）
   */
  weavingOrderIds?: number[]
  /**
   * 仓库 ID 列表；为空或空集合时，统计全部仓库合计
   */
  warehouseIds?: number[]
  /**
   * 汇总徽标是否含在途（缺省 false，仅影响顶部汇总徽标统计口径，表格两缺口列常驻计算）
   */
  includeInTransit?: boolean
}

/**
 * 织造单贡献展开行 VO
 */
export interface MaterialCheckContributionVO {
  weavingOrderId: number
  weavingOrderCode: string
  productId: number
  productCode?: string
  productName?: string
  plannedQty?: number
  plannedWeight?: number
  ratio?: number
  requiredQty: number
}

/**
 * 无法试算织造单 VO（缺 BOM 或缺计划重量/匹数）
 */
export interface MaterialCheckMissingOrderVO {
  weavingOrderId: number
  weavingOrderCode: string
  productId: number
  productCode?: string
  productName?: string
  plannedQty?: number
  plannedWeight?: number
  reason?: string
}

/**
 * 纱线规格聚合行 VO
 */
export interface MaterialCheckRowVO {
  yarnSpecId: number
  yarnSpecName?: string
  yarnTypeId?: number
  yarnTypeName?: string
  requiredQty: number
  availableQty: number
  inTransitQty: number
  shortageExcludeInTransit: number
  shortageIncludeInTransit: number
  suggestedPurchaseQty: number
  contributions?: MaterialCheckContributionVO[]
}

/**
 * 齐套检查试算响应 VO
 */
export interface MaterialCheckRespVO {
  rows: MaterialCheckRowVO[]
  missingBomOrders: MaterialCheckMissingOrderVO[]
  missingWeightOrders: MaterialCheckMissingOrderVO[]
  shortageSpecCount: number
  totalShortageQty: number
}

/**
 * 齐套检查纯只读试算
 */
export const runMaterialCheck = (data?: MaterialCheckReqVO) => {
  return request.post<MaterialCheckRespVO>({
    url: '/lunaerp/production/material-check/run',
    data: data || {}
  })
}
