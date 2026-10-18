import request from '@/config/axios'
import { CUSTOMER_STATEMENT_ERROR_CODES } from '@/views/lunaerp-elmui/finance/customer-reconciliation/model/customerStatement'

export { CUSTOMER_STATEMENT_ERROR_CODES }

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
