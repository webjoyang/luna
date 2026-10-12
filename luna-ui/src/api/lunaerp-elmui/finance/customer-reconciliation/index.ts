import request from '@/config/axios'

/**
 * 客户对账单主表 VO
 */
export interface CustomerStatementVO {
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
  status: string
  confirmType?: string
  confirmByName?: string
  confirmTime?: string
  confirmIp?: string
  confirmMethod?: string
  rejectReason?: string
  replyRemark?: string
  remark?: string
  createTime?: string
  creator?: string
}

/**
 * 客户对账单明细行 VO
 */
export interface CustomerStatementItemVO {
  id: number
  statementId: number
  lineNo: number
  ledgerEntryId?: number
  direction: 'DEBIT' | 'CREDIT'
  bizDate: string
  refType?: string
  refNo?: string
  summary?: string
  debitAmount: number
  creditAmount: number
  signStatus?: string
  remark?: string
}

/**
 * 客户对账单详情 VO
 */
export interface CustomerStatementDetailVO extends CustomerStatementVO {
  items: CustomerStatementItemVO[]
}

/**
 * 候选分录明细 VO
 */
export interface CustomerStatementCandidateItemVO {
  ledgerEntryId: number
  bizDate: string
  direction: 'DEBIT' | 'CREDIT'
  category: string
  currency: string
  refType?: string
  refId?: number
  refNo?: string
  summary?: string
  amount: number
  debitAmount: number
  creditAmount: number
  signStatus?: string
  occupied: boolean
  occupiedStatementNo?: string
  occupiedStatus?: string
}

/**
 * 取数试算请求 VO
 */
export interface CustomerStatementPrefetchReqVO {
  customerId: number
  currency: string
  periodStart: string
  periodEnd: string
  excludeStatementId?: number
}

/**
 * 取数试算响应 VO
 */
export interface CustomerStatementPrefetchRespVO {
  customerId: number
  customerName: string
  currency: string
  periodStart: string
  periodEnd: string
  openingBalance: number
  candidateDebit: number
  candidateCredit: number
  estimatedClosingBalance: number
  items: CustomerStatementCandidateItemVO[]
}

/**
 * 创建对账单请求 VO
 */
export interface CustomerStatementCreateReqVO {
  statementNo?: string
  customerId: number
  currency: string
  periodStart: string
  periodEnd: string
  selectedEntryIds?: number[]
  remark?: string
}

/**
 * 更新对账单请求 VO
 */
export interface CustomerStatementUpdateReqVO {
  id: number
  remark?: string
}

/**
 * 追加分录请求 VO
 */
export interface CustomerStatementAddEntryReqVO {
  statementId: number
  ledgerEntryId: number
}

/**
 * 分页查询参数 VO
 */
export interface CustomerStatementPageReqVO extends PageParam {
  statementNo?: string
  customerId?: number
  customerName?: string
  currency?: string
  status?: string
  periodStart?: string
  periodEnd?: string
}

/** 分页查询客户对账单 */
export const getCustomerStatementPage = (params: CustomerStatementPageReqVO) => {
  return request.get<{ list: CustomerStatementVO[]; total: number }>({
    url: '/lunaerp/finance/statement/page',
    params
  })
}

/** 查询客户对账单详情 */
export const getCustomerStatement = (id: number | string) => {
  return request.get<CustomerStatementDetailVO>({
    url: '/lunaerp/finance/statement/get',
    params: { id }
  })
}

/** 对账期间取数预览及试算 */
export const prefetchCustomerStatementLines = (params: CustomerStatementPrefetchReqVO) => {
  return request.get<CustomerStatementPrefetchRespVO>({
    url: '/lunaerp/finance/statement/prefetch-lines',
    params
  })
}

/** 创建草稿客户对账单 */
export const createCustomerStatement = (data: CustomerStatementCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/statement/create',
    data
  })
}

/** 更新草稿客户对账单备注 */
export const updateCustomerStatement = (data: CustomerStatementUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/statement/update',
    data
  })
}

/** 删除草稿客户对账单（移入回收站） */
export const deleteCustomerStatement = (id: number | string) => {
  return request.delete<boolean>({
    url: '/lunaerp/finance/statement/delete',
    params: { id }
  })
}

/** 草稿手工追加未占用分录 */
export const addCustomerStatementEntry = (data: CustomerStatementAddEntryReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/finance/statement/add-entry',
    data
  })
}

/** 代客确认请求 VO */
export interface CustomerStatementConfirmOnBehalfReqVO {
  id: number
  confirmMethod: string
  remark?: string
}

/** 驳回请求 VO */
export interface CustomerStatementRejectReqVO {
  id: number
  rejectReason: string
}

/** 异议回复处理请求 VO */
export interface CustomerStatementRejectReplyReqVO {
  id: number
  replyRemark?: string
}

/** 客户对账单打印数据 VO */
export interface CustomerStatementPrintVO {
  id: number
  statementNo: string
  customerId: number
  customerName: string
  invoiceTitle?: string
  invoiceAddress?: string
  bankName?: string
  bankAccountNo?: string
  contactPerson?: string
  contactPhone?: string
  currency: string
  periodStart: string
  periodEnd: string
  openingBalance: number
  periodDebit: number
  periodCredit: number
  closingBalance: number
  status: string
  confirmType?: string
  confirmByName?: string
  confirmTime?: string
  confirmMethod?: string
  rejectReason?: string
  replyRemark?: string
  remark?: string
  qrUrl?: string
  qrDegraded?: boolean
  creator?: string
  createTime?: string
  items: CustomerStatementItemVO[]
}

/** 发送对账单给客户（DRAFT / REJECTED -> SENT） */
export const sendCustomerStatement = (id: number | string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/statement/send',
    params: { id }
  })
}

/** 作废对账单（非终态 -> VOID，释放行占用） */
export const voidCustomerStatement = (id: number | string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/statement/void',
    params: { id }
  })
}

/** 内部代客确认对账单（SENT -> CONFIRMED） */
export const confirmCustomerStatementOnBehalf = (data: CustomerStatementConfirmOnBehalfReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/statement/confirm-on-behalf',
    data
  })
}

/** 驳回客户对账单（SENT -> REJECTED） */
export const rejectCustomerStatement = (data: CustomerStatementRejectReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/statement/reject',
    data
  })
}

/** 客户对账单异议回复处理（REJECTED -> DRAFT） */
export const rejectReplyCustomerStatement = (data: CustomerStatementRejectReplyReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/statement/reject-reply',
    data
  })
}

/** 获得客户对账单套打数据（豁免脱敏） */
export const getCustomerStatementPrintData = (id: number | string) => {
  return request.get<CustomerStatementPrintVO>({
    url: '/lunaerp/finance/statement/print-data',
    params: { id }
  })
}

/**
 * 客户账龄汇总查询请求 VO
 */
export interface CustomerAgingSummaryReqVO {
  agingDate?: string
  customerIds?: number[]
  salesmanId?: number
  currency?: string
}

/**
 * 客户账龄汇总响应 VO（按客户一行）
 */
export interface CustomerAgingSummaryVO {
  customerId: number
  customerName: string
  currency: string
  creditDays?: number
  salesmanId?: number
  salesmanName?: string
  totalBalance: number
  bucket0To30: number
  bucket31To60: number
  bucket61To90: number
  bucket91To180: number
  bucket181Plus: number
  overdueAmount: number
  advanceBalance: number
}

/**
 * 客户账龄明细下钻请求 VO
 */
export interface CustomerAgingDetailReqVO {
  customerId: number
  agingDate?: string
  currency?: string
}

/**
 * 客户账龄未核销明细分录 VO
 */
export interface CustomerAgingDetailVO {
  ledgerEntryId: number
  bizDate: string
  refType?: string
  refId?: number
  refNo?: string
  originalAmount: number
  settledAmount: number
  unsettledAmount: number
  currency: string
  agingDays: number
  bucket: string
  creditDays?: number
  dueDate?: string
  isOverdue: boolean
  overdueDays: number
  summary?: string
}

/** 获得客户 FIFO 账龄汇总列表 */
export const getCustomerAgingSummary = (params: CustomerAgingSummaryReqVO) => {
  return request.get<CustomerAgingSummaryVO[]>({
    url: '/lunaerp/finance/statement/aging/summary',
    params
  })
}

/** 获得客户 FIFO 账龄未核销明细下钻 */
export const getCustomerAgingDetail = (params: CustomerAgingDetailReqVO) => {
  return request.get<CustomerAgingDetailVO[]>({
    url: '/lunaerp/finance/statement/aging/detail',
    params
  })
}

