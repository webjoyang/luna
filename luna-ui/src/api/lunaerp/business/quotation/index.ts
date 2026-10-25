import request from '@/config/axios'

export interface QuotationItemVO {
  id?: number
  quotationId?: number
  lineNo?: number
  productId?: number
  productCode?: string
  productName: string
  customerColor?: string
  composition?: string
  width?: string
  weight?: string
  unit?: string
  quantity: number
  costCardId?: number
  costCardNo?: string
  costSource?: 'CARD' | 'MANUAL'
  costSnapshot?: string
  costUnitPriceRmb?: number
  exchangeRateSnapshot?: number
  costUnitPrice?: number
  targetMarginRate?: number
  unitPrice?: number
  costAmount?: number
  amount?: number
  marginRate?: number
  lowMarginWarning?: boolean
  remark?: string
}

export interface QuotationVO {
  id?: number
  quotationNo?: string
  versionNo?: number
  customerId: number
  clientName: string
  clientContact?: string
  phone?: string
  salespersonUserId?: number
  deptId?: number
  quotationDate: string
  validFrom: string
  validTo?: string
  currency: string
  exchangeRate: number
  totalQuantity?: number
  totalAmount?: number
  totalCostAmount?: number
  avgMarginRate?: number
  status?: 'DRAFT' | 'CONFIRMED' | 'SUPERSEDED' | 'CANCELLED'
  expired?: boolean
  lowMarginWarning?: boolean
  remarks?: string
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
  items?: QuotationItemVO[]
}

export interface QuotationPageReqVO extends PageParam {
  keyword?: string
  status?: string
  customerId?: number
  latestOnly?: boolean
}

// 创建报价单
export const createQuotation = (data: QuotationVO) => {
  return request.post<number>({ url: '/lunaerp/business/quotation/create', data })
}

// 更新报价单
export const updateQuotation = (data: QuotationVO) => {
  return request.put<boolean>({ url: '/lunaerp/business/quotation/update', data })
}

// 删除报价单（仅草稿）
export const deleteQuotation = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/business/quotation/delete?id=${id}` })
}

// 确认报价（DRAFT -> CONFIRMED）
export const confirmQuotation = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/business/quotation/confirm?id=${id}` })
}

// 取消报价（CONFIRMED -> CANCELLED）
export const cancelQuotation = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/business/quotation/cancel?id=${id}` })
}

// 修订报价单（CONFIRMED -> SUPERSEDED，生成新草稿）
export const reviseQuotation = (id: number) => {
  return request.put<number>({ url: `/lunaerp/business/quotation/revise?id=${id}` })
}

// 获取报价单详情
export const getQuotation = (id: number) => {
  return request.get<QuotationVO>({ url: `/lunaerp/business/quotation/get?id=${id}` })
}

// 分页查询报价单
export const getQuotationPage = (params: QuotationPageReqVO) => {
  return request.get<PageResult<QuotationVO>>({ url: '/lunaerp/business/quotation/page', params })
}

// 预览下一个报价单号
export const getNextQuotationNo = () => {
  return request.get<string>({ url: '/lunaerp/business/quotation/get-next-no' })
}

// 查询历史版本列表
export const getQuotationVersions = (quotationNo: string) => {
  return request.get<QuotationVO[]>({ url: `/lunaerp/business/quotation/versions?quotationNo=${quotationNo}` })
}

export interface QuotationContractPrefillItemVO {
  productCode?: string
  productName: string
  customerColor?: string
  composition?: string
  width?: string
  weight?: string
  quantity: number
  unitPrice: number
  otherFees?: number
  amount?: number
  remark?: string
}

export interface QuotationContractPrefillVO {
  sourceQuotationId: number
  sourceQuotationNo: string
  customerId: number
  clientName: string
  clientContact?: string
  phone?: string
  currency: string
  tradeType: string
  salespersonUserId?: number
  deptId?: number
  expiredWarning: boolean
  items: QuotationContractPrefillItemVO[]
}

export interface QuotationPrintItemVO {
  id: number
  lineNo: number
  productCode?: string
  productName: string
  customerColor?: string
  composition?: string
  width?: string
  weight?: string
  unit?: string
  quantity: number
  unitPrice: number
  amount: number
  remark?: string
}

export interface QuotationPrintVO {
  id: number
  quotationNo: string
  versionNo: number
  displayNo: string
  customerId: number
  clientName: string
  clientContact?: string
  phone?: string
  salespersonUserId?: number
  salespersonName?: string
  quotationDate: string
  validFrom: string
  validTo?: string
  currency: string
  exchangeRate: number
  totalQuantity: number
  totalAmount: number
  remarks?: string
  items: QuotationPrintItemVO[]
}

// 获取转销售合同预填数据
export const getContractPrefill = (id: number) => {
  return request.get<QuotationContractPrefillVO>({ url: `/lunaerp/business/quotation/contract-prefill?id=${id}` })
}

// 获取打印数据（白诺结构）
export const getPrintData = (id: number) => {
  return request.get<QuotationPrintVO>({ url: `/lunaerp/business/quotation/print-data?id=${id}` })
}
