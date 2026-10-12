import portalRequest from '@/config/axios/portal'

export interface PortalShipmentPageReqVO {
  pageNo: number
  pageSize: number
  signStatus?: string
}

export interface PortalShipmentPageItemRespVO {
  id: number
  shipmentNo: string
  shippingDate: string
  logisticsCompany?: string
  logisticsTrackingNo?: string
  signStatus: string
  totalAmount: number
  freightFee: number
  totalPieces?: number
}

export interface PortalShipmentRollRespVO {
  id: number
  rollNo?: string
  weight?: number
  lengthM?: number
}

export interface PortalShipmentDetailItemRespVO {
  id: number
  productCode?: string
  productName?: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  quantity: number
  pieces?: number
  unit?: string
  unitPrice?: number
  amount: number
  rolls?: PortalShipmentRollRespVO[]
}

export interface PortalShipmentDetailRespVO {
  id: number
  shipmentNo: string
  shippingDate: string
  logisticsCompany?: string
  logisticsTrackingNo?: string
  signStatus: string
  signedByName?: string
  signedAt?: string
  signRemark?: string
  totalAmount: number
  freightFee: number
  totalPieces?: number
  items: PortalShipmentDetailItemRespVO[]
}

/**
 * 获取客户门户发货单分页列表
 */
export const getPortalShipmentPage = (params: PortalShipmentPageReqVO) => {
  return portalRequest.get<{ list: PortalShipmentPageItemRespVO[]; total: number }>({
    url: '/app-api/lunaerp/portal/shipment/page',
    params
  })
}

/**
 * 获取客户门户发货单详情（支持 id 或 no）
 */
export const getPortalShipmentDetail = (params: { id?: number | string; no?: string }) => {
  return portalRequest.get<PortalShipmentDetailRespVO>({
    url: '/app-api/lunaerp/portal/shipment/get',
    params
  })
}
