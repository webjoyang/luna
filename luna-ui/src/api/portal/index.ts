import portalRequest from '@/config/axios/portal'

export interface PortalPageParam {
  pageNo?: number
  pageSize?: number
  status?: string
}

export interface PortalShipmentRespVO {
  id: number
  shipmentNo: string
  shipmentDate: string
  logisticsCompany?: string
  logisticsNo?: string
  signStatus: string
  signedByName?: string
  signedAt?: string
  signRemark?: string
  totalAmount?: number
  freightAmount?: number
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

// 占位 API 契约（业务端点由 03/05 工单填充）
export const getPortalShipmentPage = (params: PortalPageParam) => {
  return portalRequest.get<PageResult<PortalShipmentRespVO>>({
    url: '/lunaerp/portal/shipment/page',
    params
  })
}

export const getPortalStatementList = () => {
  return portalRequest.get<PortalStatementRespVO[]>({
    url: '/lunaerp/portal/finance/statement/list'
  })
}
