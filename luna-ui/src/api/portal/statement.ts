import portalRequest from '@/config/axios/portal'

export interface PortalStatementRespVO {
  id: number
  statementNo: string
  customerId: number
  customerName: string
  currency: string
  periodStart: string
  periodEnd: string
  openingBalance: number
  periodDebit: number
  periodCredit: number
  closingBalance: number
  status: 'SENT' | 'CONFIRMED' | 'REJECTED' | string
  confirmType?: 'ONLINE_PORTAL' | 'ON_BEHALF' | string
  confirmByName?: string
  confirmTime?: string
  rejectReason?: string
  remark?: string
}

export interface PortalStatementItemRespVO {
  id: number
  lineNo: number
  direction: 'DEBIT' | 'CREDIT' | string
  bizDate: string
  refType?: string
  refNo?: string
  summary?: string
  debitAmount: number
  creditAmount: number
  signStatus?: string
  remark?: string
}

export interface PortalStatementDetailRespVO extends PortalStatementRespVO {
  items: PortalStatementItemRespVO[]
}

export interface PortalStatementConfirmReqVO {
  id: number
  confirmByName: string
  customerId?: number
}

export interface PortalStatementRejectReqVO {
  id: number
  rejectReason: string
  customerId?: number
}

/**
 * 获取客户门户对账单列表
 */
export const getPortalStatementList = (params?: { status?: string; customerId?: number }) => {
  return portalRequest.get<PortalStatementRespVO[]>({
    url: '/app-api/lunaerp/portal/finance/statement/list',
    params
  })
}

/**
 * 获取客户门户对账单详情（支持 id、no 单独或组合查询）
 */
export const getPortalStatementDetail = (
  idOrParams: number | string | { id?: number | string; no?: string; customerId?: number }
) => {
  if (typeof idOrParams === 'number' || (typeof idOrParams === 'string' && /^\d+$/.test(idOrParams))) {
    return portalRequest.get<PortalStatementDetailRespVO>({
      url: `/app-api/lunaerp/portal/finance/statement/${idOrParams}`
    })
  }

  const params = typeof idOrParams === 'object' ? idOrParams : { no: idOrParams }
  if (params.id) {
    return portalRequest.get<PortalStatementDetailRespVO>({
      url: `/app-api/lunaerp/portal/finance/statement/${params.id}`,
      params: {
        no: params.no,
        customerId: params.customerId
      }
    })
  }

  return portalRequest.get<PortalStatementDetailRespVO>({
    url: '/app-api/lunaerp/portal/finance/statement',
    params
  })
}

/**
 * 客户门户对账单确认（手填姓名必填）
 */
export const confirmPortalStatement = (data: PortalStatementConfirmReqVO) => {
  return portalRequest.put<boolean>({
    url: '/app-api/lunaerp/portal/finance/statement/confirm',
    data
  })
}

/**
 * 客户门户对账单驳回（驳回原因必填）
 */
export const rejectPortalStatement = (data: PortalStatementRejectReqVO) => {
  return portalRequest.put<boolean>({
    url: '/app-api/lunaerp/portal/finance/statement/reject',
    data
  })
}
