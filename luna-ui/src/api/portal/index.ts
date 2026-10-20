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

export interface PortalShipmentSignReqVO {
  id: number
  signStatus: 'SIGNED' | 'REFUSED'
  signedByName: string
  signRemark?: string
}

export interface PortalStatementRespVO {
  id: number
  statementNo: string
  statementDate?: string
  periodStart?: string
  periodEnd?: string
  currency?: string
  openingBalance?: number
  periodDebit?: number
  periodCredit?: number
  closingBalance?: number
  totalAmount?: number
  status: string
  confirmType?: string
  confirmByName?: string
  confirmTime?: string
  rejectReason?: string
  createTime?: string
}

export interface PortalStatementItemRespVO {
  id: number
  statementId: number
  lineNo: number
  sourceType?: string
  bizNo?: string
  bizDate?: string
  summary?: string
  currency?: string
  debitAmount?: number
  creditAmount?: number
  balanceAmount?: number
  remark?: string
  isReversed?: boolean
}

export interface PortalStatementDetailRespVO {
  id: number
  statementNo: string
  statementDate: string
  customerId: number
  customerName?: string
  periodStart?: string
  periodEnd?: string
  currency?: string
  openingBalance?: number
  periodDebit?: number
  periodCredit?: number
  closingBalance?: number
  status: string
  confirmType?: string
  confirmByName?: string
  confirmTime?: string
  confirmIp?: string
  rejectReason?: string
  remark?: string
  creator?: string
  createTime?: string
  items?: PortalStatementItemRespVO[]
}

export interface PortalStatementConfirmReqVO {
  id: number
  confirmByName: string
}

export interface PortalStatementRejectReqVO {
  id: number
  rejectReason: string
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
 * 客户门户发货单签收/拒收登记
 */
export const signPortalShipment = (data: PortalShipmentSignReqVO) => {
  return portalRequest.put<boolean>({
    url: '/lunaerp/portal/shipment/sign',
    data
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

/**
 * 获得客户门户对账单详情（支持按 id 或 no 查询）
 */
export const getPortalStatement = (params: { id?: number; no?: string; statementNo?: string }) => {
  return portalRequest.get<PortalStatementDetailRespVO>({
    url: '/lunaerp/portal/finance/statement/get',
    params
  })
}

/**
 * 客户门户对账单在线确认（SENT -> CONFIRMED）
 */
export const confirmPortalStatement = (data: PortalStatementConfirmReqVO) => {
  return portalRequest.put<boolean>({
    url: '/lunaerp/portal/finance/statement/confirm',
    data
  })
}

/**
 * 客户门户对账单在线驳回（SENT -> REJECTED）
 */
export const rejectPortalStatement = (data: PortalStatementRejectReqVO) => {
  return portalRequest.put<boolean>({
    url: '/lunaerp/portal/finance/statement/reject',
    data
  })
}
