import request from '@/config/axios'

/**
 * 发票申请明细行 VO
 */
export interface InvoiceItemVO {
  id?: number
  lineNo?: number
  shipmentId: number
  shipmentNo?: string
  shipmentAmount?: number
  amount: number
  remark?: string
}

/**
 * 发票申请 VO
 */
export interface InvoiceVO {
  id?: number
  invoiceNo?: string
  customerId: number
  customerName?: string
  currency?: string
  taxRate?: string
  invoiceKind: string
  electronic?: number
  invoiceTitle?: string
  taxNo?: string
  address?: string
  bankInfo?: string
  status?: string
  officialInvoiceNo?: string
  invoiceCode?: string
  invoiceDate?: string
  attachmentUrl?: string
  totalAmountExclTax?: number
  totalTax?: number
  totalAmountInclTax?: number
  issueBy?: string
  issueTime?: string
  submitBy?: string
  submitTime?: string
  rejectReason?: string
  remark?: string
  createTime?: string
  items?: InvoiceItemVO[]
}

/**
 * 发票申请分页查询参数
 */
export interface InvoicePageParam {
  pageNo: number
  pageSize: number
  keyword?: string
  customerId?: number
  status?: string
  invoiceKind?: string
  currency?: string
  createTime?: [string, string]
  invoiceDate?: [string, string]
}

/**
 * 发票申请保存明细入参
 */
export interface InvoiceItemSaveVO {
  id?: number
  shipmentId: number
  amount: number
  remark?: string
}

/**
 * 发票申请保存/修改入参
 */
export interface InvoiceSaveReqVO {
  id?: number
  customerId: number
  currency?: string
  taxRate?: string
  invoiceKind: string
  electronic?: number
  invoiceTitle?: string
  taxNo?: string
  address?: string
  bankInfo?: string
  remark?: string
  items: InvoiceItemSaveVO[]
}

/**
 * 发票登记入参
 */
export interface InvoiceIssueReqVO {
  id: number
  officialInvoiceNo: string
  invoiceDate?: string
  attachmentUrl?: string
}

/**
 * 候选可开票发货单 VO
 */
export interface InvoiceCandidateShipmentVO {
  id: number
  shipmentNo: string
  customerId: number
  currency: string
  shippingDate?: string
  totalAmount?: number
  freightFee?: number
  shipmentAmount: number
  invoicedAmount: number
  uninvoicedBalance: number
}

/** 分页查询发票申请 */
export const getInvoicePage = (params: InvoicePageParam) => {
  return request.get<{ list: InvoiceVO[]; total: number }>({
    url: '/lunaerp/finance/invoice/page',
    params
  })
}

/** 查询发票申请详情 */
export const getInvoice = (id: number | string) => {
  return request.get<InvoiceVO>({
    url: '/lunaerp/finance/invoice/get?id=' + id
  })
}

/** 创建发票申请（草稿） */
export const createInvoice = (data: InvoiceSaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/invoice/create',
    data
  })
}

/** 更新发票申请 */
export const updateInvoice = (data: InvoiceSaveReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/update',
    data
  })
}

/** 删除发票申请（仅草稿） */
export const deleteInvoice = (id: number | string) => {
  return request.delete<boolean>({
    url: '/lunaerp/finance/invoice/delete?id=' + id
  })
}

/** 提交发票申请 */
export const submitInvoice = (id: number | string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/submit?id=' + id
  })
}

/** 发票登记回填（开票） */
export const issueInvoice = (data: InvoiceIssueReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/issue',
    data
  })
}

/** 驳回发票申请 */
export const rejectInvoice = (id: number | string, reason?: string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/invoice/reject',
    params: { id, reason }
  })
}

/** 获取客户候选可开票发货单列表 */
export const getCandidateShipments = (customerId: number | string) => {
  return request.get<InvoiceCandidateShipmentVO[]>({
    url: '/lunaerp/finance/invoice/candidate-shipments?customerId=' + customerId
  })
}
