import portalRequest from '@/config/axios/portal'

export interface PortalPageParam {
  pageNo?: number
  pageSize?: number
  status?: string
  signStatus?: string
}

export interface PortalShipmentItemRespVO {
  id: number
  shipmentId: number
  productId: number
  productCode: string
  productName: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  quantity: number
  pieces?: number
  unit?: string
  unitPrice?: number
  amount?: number
}

/**
 * 发货单分页列表 Response VO（遵循详情不入列表原则）
 */
export interface PortalShipmentPageRespVO {
  id: number
  shipmentNo: string
  customerName?: string
  shippingDate?: string
  carrier?: string
  trackingNumber?: string
  signStatus: string
  totalAmount?: number
  freightFee?: number
  totalPieces?: number
  currency?: string
}

/**
 * 发货单详情 Response VO（头+逐行明细）
 */
export interface PortalShipmentDetailRespVO {
  id: number
  shipmentNo: string
  customerName?: string
  shippingDate?: string
  receiveAddress?: string
  carrier?: string
  trackingNumber?: string
  signStatus: string
  signedByName?: string
  signedAt?: string
  signRemark?: string
  totalAmount?: number
  freightFee?: number
  totalPieces?: number
  currency?: string
  items?: PortalShipmentItemRespVO[]
}

export interface PortalStatementRespVO {
  id: number
  statementNo: string
  statementDate: string
  periodStart?: string
  periodEnd?: string
  status: string
  totalAmount?: number
  confirmByName?: string
  confirmTime?: string
  rejectReason?: string
}

/**
 * 获得客户门户发货单分页
 */
export const getPortalShipmentPage = (params: PortalPageParam) => {
  return portalRequest.get<PageResult<PortalShipmentPageRespVO>>({
    url: '/lunaerp/portal/shipment/page',
    params
  })
}

/**
 * 获得客户门户发货单详情（按 id 或 no 查询）
 */
export const getPortalShipment = (params: { id?: number; no?: string; shipmentNo?: string }) => {
  return portalRequest.get<PortalShipmentDetailRespVO>({
    url: '/lunaerp/portal/shipment/get',
    params
  })
}

/**
 * 获得客户门户对账单列表
 */
export const getPortalStatementList = () => {
  return portalRequest.get<PortalStatementRespVO[]>({
    url: '/lunaerp/portal/finance/statement/list'
  })
}
