import request from '@/config/axios'

/**
 * 发票申请状态枚举
 */
export type InvoiceApplicationStatus = 'DRAFT' | 'SUBMITTED' | 'ISSUED' | 'REJECTED'

/**
 * 发票种类枚举
 */
export type InvoiceKind = 'DIGITAL_SPECIAL' | 'DIGITAL_NORMAL' | 'VAT_SPECIAL' | 'VAT_NORMAL'

/**
 * 发票申请明细行保存 Request VO
 */
export interface InvoiceApplicationItemSaveReqVO {
  shipmentId: number
  amount: number
  remark?: string
}

/**
 * 发票申请创建 Request VO
 */
export interface InvoiceApplicationCreateReqVO {
  customerId: number
  currency: string
  taxRate: number
  invoiceKind: string
  electronic?: number
  invoiceTitle?: string
  taxNo?: string
  address?: string
  bankInfo?: string
  remark?: string
  items: InvoiceApplicationItemSaveReqVO[]
}

/**
 * 发票申请修改 Request VO
 */
export interface InvoiceApplicationUpdateReqVO {
  id: number
  taxRate?: number
  invoiceKind?: string
  electronic?: number
  invoiceTitle?: string
  taxNo?: string
  address?: string
  bankInfo?: string
  remark?: string
  items?: InvoiceApplicationItemSaveReqVO[]
}

/**
 * 发票申请分页查询 Request VO
 */
export interface InvoiceApplicationPageReqVO extends PageParam {
  invoiceNo?: string
  customerId?: number
  customerName?: string
  status?: string
  invoiceKind?: string
  officialInvoiceNo?: string
}

/**
 * 发票登记开票 Request VO
 */
export interface InvoiceApplicationIssueReqVO {
  id: number
  officialInvoiceNo: string
  invoiceDate: string
  invoiceCode?: string
  attachmentUrl?: string
}

/**
 * 发票申请驳回 Request VO
 */
export interface InvoiceApplicationRejectReqVO {
  id: number
  rejectReason?: string
}

/**
 * 发票申请明细行 Response VO
 */
export interface InvoiceApplicationItemRespVO {
  id: number
  applicationId: number
  lineNo: number
  shipmentId: number
  shipmentNo: string
  shipmentAmount: number
  amount: number
  remark?: string
}

/**
 * 发票申请列表项 Response VO
 */
export interface InvoiceApplicationRespVO {
  id: number
  invoiceNo: string
  customerId: number
  customerName: string
  currency: string
  taxRate: number
  invoiceKind: string
  electronic: number
  invoiceTitle: string
  taxNo?: string
  address?: string
  bankInfo?: string
  status: InvoiceApplicationStatus
  officialInvoiceNo?: string
  invoiceCode?: string
  invoiceDate?: string
  attachmentUrl?: string
  totalAmountExclTax: number
  totalTax: number
  totalAmountInclTax: number
  issueBy?: string
  issueTime?: string
  submitBy?: string
  submitTime?: string
  remark?: string
  createTime: string
}

/**
 * 发票申请详情 Response VO
 */
export interface InvoiceApplicationDetailRespVO extends InvoiceApplicationRespVO {
  items: InvoiceApplicationItemRespVO[]
}

/**
 * 候选发货单 Response VO
 */
export interface InvoiceCandidateShipmentRespVO {
  shipmentId: number
  shipmentNo: string
  customerId: number
  customerName: string
  currency: string
  shippingDate: string
  salesContractId?: number
  contractNo?: string
  totalAmount: number
  freightFee: number
  shipmentAmount: number
  invoicedAmount: number
  availableAmount: number
}

// ==================== API 接口 ====================

/**
 * 获得发票申请分页
 */
export const getInvoiceApplicationPage = (params: InvoiceApplicationPageReqVO) => {
  return request.get<PageResult<InvoiceApplicationRespVO>>({
    url: '/lunaerp/finance/invoice/page',
    params
  })
}

/**
 * 获得发票申请详情
 */
export const getInvoiceApplication = (id: number) => {
  return request.get<InvoiceApplicationDetailRespVO>({
    url: '/lunaerp/finance/invoice/get',
    params: { id }
  })
}

/**
 * 创建发票申请（草稿）
 */
export const createInvoiceApplication = (data: InvoiceApplicationCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/invoice/create',
    data
  })
}

/**
 * 更新发票申请
 */
export const updateInvoiceApplication = (data: InvoiceApplicationUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/update',
    data
  })
}

/**
 * 删除发票申请（仅 DRAFT 允许）
 */
export const deleteInvoiceApplication = (id: number) => {
  return request.delete<boolean>({
    url: '/lunaerp/finance/invoice/delete',
    params: { id }
  })
}

/**
 * 提交发票申请（DRAFT -> SUBMITTED）
 */
export const submitInvoiceApplication = (id: number) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/submit',
    params: { id }
  })
}

/**
 * 登记开票回填（SUBMITTED -> ISSUED）
 */
export const issueInvoiceApplication = (data: InvoiceApplicationIssueReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/issue',
    data
  })
}

/**
 * 驳回发票申请（SUBMITTED -> REJECTED）
 */
export const rejectInvoiceApplication = (data: InvoiceApplicationRejectReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/reject',
    data
  })
}

/**
 * 查询指定客户和币种的可开票候选发货单列表
 */
export const getCandidateShipments = (params: { customerId: number; currency: string }) => {
  return request.get<InvoiceCandidateShipmentRespVO[]>({
    url: '/lunaerp/finance/invoice/candidate-shipments',
    params
  })
}
