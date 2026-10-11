import request from '@/config/axios'

/**
 * 生产管理 - 齐套检查只读试算请求 VO (ADR-0041)
 */
export interface MaterialCheckReqVO {
  /** 织造单 ID 列表；空列表=试算全部 DRAFT/ACTIVE 织造单 */
  weavingOrderIds?: number[]
  /** 仓库 ID 列表；空=全仓库合计 */
  warehouseIds?: number[]
  /** 是否含在途（缺省 false；仅影响汇总徽标口径，缺口两列常驻） */
  includeInTransit?: boolean
}

/**
 * 织造单贡献展开行 VO
 */
export interface MaterialCheckContributionVO {
  weavingOrderId: number
  weavingOrderCode: string
  productId?: number
  productCode?: string
  productName?: string
  plannedWeight?: number
  ratio?: number
  demandQty: number
}

/**
 * 纱线规格聚合行 VO
 */
export interface MaterialCheckRowVO {
  yarnTypeId?: number
  yarnTypeName?: string
  yarnSpecId: number
  yarnSpecName?: string
  demandQty: number
  /** 可用库存（实时台账余额），后端字段为 stockQty，兼容 availableStock */
  stockQty: number
  availableStock?: number
  inTransitQty: number
  shortageWithoutInTransit: number
  shortageWithInTransit: number
  suggestedPurchaseQty: number
  contributions?: MaterialCheckContributionVO[]
}

/**
 * 无法试算织造单信息 VO
 */
export interface MissingOrderVO {
  weavingOrderId: number
  weavingOrderCode: string
  productId?: number
  productCode?: string
  productName?: string
  plannedQty?: number
  plannedWeight?: number
  reason?: string
}

export type MissingBomOrderVO = MissingOrderVO
export type MissingWeightOrderVO = MissingOrderVO

/**
 * 齐套检查试算响应 VO
 */
export interface MaterialCheckRespVO {
  rows: MaterialCheckRowVO[]
  missingBomOrders: MissingBomOrderVO[]
  missingWeightOrders: MissingWeightOrderVO[]
  shortageSpecCount: number
  totalShortageQty: number
}

/**
 * 执行齐套检查只读试算
 *
 * 端点：POST /lunaerp/production/material-check/run
 */
export const runMaterialCheck = (data: MaterialCheckReqVO): Promise<MaterialCheckRespVO> => {
  return request.post({
    url: '/lunaerp/production/material-check/run',
    data
  })
}
