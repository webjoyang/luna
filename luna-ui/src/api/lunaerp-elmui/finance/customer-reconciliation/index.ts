import request from '@/config/axios'

/**
 * 客户对账单状态枚举
 */
export type CustomerStatementStatus = 'DRAFT' | 'SENT' | 'CONFIRMED' | 'REJECTED' | 'VOID'

/**
 * 客户对账单确认方式枚举
 */
export type CustomerStatementConfirmType = 'ONLINE_PORTAL' | 'ON_BEHALF'

/**
 * 候选分录明细行 Response VO
 */
export interface CustomerStatementCandidateItemRespVO {
  ledgerEntryId: number
  direction?: string
  bizDate: string
  entryType?: string
  refType: string
  refId: number
  refNo: string
  summary?: string
  debitAmount: number
  creditAmount: number
  signStatus?: string
  occupied?: boolean
  isOccupied?: boolean
  occupyingStatementNo?: string
  occupiedStatementNo?: string
  occupiedStatus?: string
}

/**
 * 取数预览 Request VO
 */
export interface CustomerStatementPrefetchReqVO {
  customerId: number
  currency: string
  periodStart: string
  periodEnd: string
}

/**
 * 取数预览 Response VO
 */
export interface CustomerStatementPrefetchRespVO {
  customerId: number
  customerName?: string
  currency: string
  periodStart: string
  periodEnd: string
  openingBalance: number
  periodDebit: number
  periodCredit: number
  closingBalance: number
  lines: CustomerStatementCandidateItemRespVO[]
}

/**
 * 创建对账单（草稿）Request VO
 */
export interface CustomerStatementCreateReqVO {
  customerId: number
  currency: string
  periodStart: string
  periodEnd: string
  statementNo?: string
  openingBalance?: number
  remark?: string
  selectedEntryIds?: number[]
  manualEntryIds?: number[]
}

/**
 * 更新对账单 Request VO
 */
export interface CustomerStatementUpdateReqVO {
  id: number
  remark?: string
  deletedItemIds?: number[]
}

/**
 * 草稿追加分录 Request VO
 */
export interface CustomerStatementAddEntryReqVO {
  statementId: number
  ledgerEntryId: number
  remark?: string
}

/**
 * 分页查询 Request VO
 */
export interface CustomerStatementPageReqVO extends PageParam {
  statementNo?: string
  customerId?: number
  currency?: string
  status?: CustomerStatementStatus | string
  periodStart?: string
  periodEnd?: string
}

/**
 * 对账单明细行 Response VO
 */
export interface CustomerStatementItemRespVO {
  id: number
  statementId: number
  ledgerEntryId: number
  bizDate: string
  entryType: string
  refType: string
  refId: number
  refNo: string
  summary?: string
  debitAmount: number
  creditAmount: number
  signStatus?: string
  remark?: string
  hasReversal?: boolean
}

/**
 * 对账单主表 Response VO
 */
export interface CustomerStatementRespVO {
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
  status: CustomerStatementStatus | string
  confirmType?: CustomerStatementConfirmType | string
  confirmByName?: string
  confirmTime?: string
  confirmMethod?: string
  rejectReason?: string
  replyRemark?: string
  confirmedAt?: string
  confirmedBy?: string
  remark?: string
  createTime?: string
}

/**
 * 对账单详情 Response VO
 */
export interface CustomerStatementDetailRespVO extends CustomerStatementRespVO {
  items: CustomerStatementItemRespVO[]
}

// ======================= API 接口 =======================

/**
 * 取数预览
 */
export const prefetchLines = (params: CustomerStatementPrefetchReqVO) => {
  return request.get<CustomerStatementPrefetchRespVO>({
    url: '/lunaerp/finance/customerrecon/statement/prefetch-lines',
    params
  })
}

/**
 * 生成客户对账单（草稿）
 */
export const createCustomerStatement = (data: CustomerStatementCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/customerrecon/statement/create',
    data
  })
}

/**
 * 获得客户对账单详情
 */
export const getCustomerStatement = (id: number) => {
  return request.get<CustomerStatementDetailRespVO>({
    url: '/lunaerp/finance/customerrecon/statement/get',
    params: { id }
  })
}

/**
 * 获得客户对账单分页
 */
export const getCustomerStatementPage = (params: CustomerStatementPageReqVO) => {
  return request.get<PageResult<CustomerStatementRespVO>>({
    url: '/lunaerp/finance/customerrecon/statement/page',
    params
  })
}

/**
 * 更新客户对账单（仅 DRAFT 允许）
 */
export const updateCustomerStatement = (data: CustomerStatementUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/customerrecon/statement/update',
    data
  })
}

/**
 * 删除客户对账单（仅 DRAFT 允许）
 */
export const deleteCustomerStatement = (id: number) => {
  return request.delete<boolean>({
    url: '/lunaerp/finance/customerrecon/statement/delete',
    params: { id }
  })
}

/**
 * 草稿对账单手工追加未占用分录
 */
export const addStatementEntry = (data: CustomerStatementAddEntryReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/customerrecon/statement/add-entry',
    data
  })
}

/**
 * 代客确认 Request VO
 */
export interface CustomerStatementConfirmOnBehalfReqVO {
  id: number
  confirmMethod: string
  remark: string
}

/**
 * 异议回复 Request VO
 */
export interface CustomerStatementRejectReplyReqVO {
  id: number
  replyRemark?: string
}

/**
 * 打印明细行 Response VO
 */
export interface CustomerStatementPrintItemVO {
  lineNo: number
  bizDate: string
  direction: string
  refType: string
  refNo: string
  summary?: string
  debitAmount: number
  creditAmount: number
  signStatus?: string
  remark?: string
}

/**
 * 客户对账单打印数据 Response VO（豁免金额脱敏）
 */
export interface CustomerStatementPrintRespVO {
  bizType: string
  title: string
  id: number
  statementNo: string
  customerId: number
  customerName: string
  taxNo?: string
  address?: string
  bankName?: string
  bankAccountNo?: string
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
  qrUrl?: string
  remark?: string
  creator?: string
  createTime?: string
  printTime?: string
  items: CustomerStatementPrintItemVO[]
}

/**
 * 发送客户对账单（DRAFT -> SENT，条件更新防并发）
 */
export const sendCustomerStatement = (id: number) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/customerrecon/statement/send',
    params: { id }
  })
}

/**
 * 作废客户对账单（非终态可作废，释放行占用，不删分录）
 */
export const voidCustomerStatement = (id: number) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/customerrecon/statement/void',
    params: { id }
  })
}

/**
 * 代客确认客户对账单（SENT -> CONFIRMED）
 */
export const confirmOnBehalf = (data: CustomerStatementConfirmOnBehalfReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/customerrecon/statement/confirm-on-behalf',
    data
  })
}

/**
 * 回复客户对账单异议并重置为草稿（REJECTED -> DRAFT）
 */
export const rejectReplyCustomerStatement = (data: CustomerStatementRejectReplyReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/customerrecon/statement/reject-reply',
    data
  })
}

/**
 * 获得客户对账单打印数据（豁免金额脱敏）
 */
export const getCustomerStatementPrintData = (id: number) => {
  return request.get<CustomerStatementPrintRespVO>({
    url: '/lunaerp/finance/customerrecon/statement/print-data',
    params: { id }
  })
}

/**
 * 客户账龄汇总 Request VO
 */
export interface CustomerAgingSummaryReqVO {
  agingDate?: string
  customerIds?: number[]
  salesmanId?: number
}

/**
 * 客户账龄汇总一行 Response VO
 */
export interface CustomerAgingSummaryRespVO {
  customerId: number
  customerName: string
  creditDays?: number
  salespersonUserId?: number
  salespersonName?: string
  currency?: string
  totalArBalance: number
  bucket0To30: number
  bucket31To60: number
  bucket61To90: number
  bucket91To180: number
  bucket181Plus: number
  advanceBalance: number
  overdueAmount: number
  agingDate?: string
}

/**
 * 客户账龄下钻明细 Request VO
 */
export interface CustomerAgingDetailReqVO {
  customerId: number
  agingDate?: string
}

/**
 * 客户未核销明细分录下钻 Response VO
 */
export interface CustomerAgingDetailRespVO {
  ledgerEntryId: number
  bizDate: string
  refType?: string
  refId?: number
  refItemId?: number
  refNo?: string
  contractNo?: string
  productName?: string
  currency?: string
  originalAmount: number
  writtenOffAmount: number
  remainingAmount: number
  agingDays: number
  bucket: string
  creditDays?: number
  dueDate?: string
  isOverdue: boolean
  overdueDays: number
}

/**
 * 获得客户账龄汇总列表（按客户一行）
 */
export const getCustomerAgingSummary = (params?: CustomerAgingSummaryReqVO) => {
  return request.get<CustomerAgingSummaryRespVO[]>({
    url: '/lunaerp/finance/customerrecon/aging/summary',
    params
  })
}

/**
 * 获得客户未核销明细分录（下钻）
 */
export const getCustomerAgingDetail = (params: CustomerAgingDetailReqVO) => {
  return request.get<CustomerAgingDetailRespVO[]>({
    url: '/lunaerp/finance/customerrecon/aging/detail',
    params
  })
}

