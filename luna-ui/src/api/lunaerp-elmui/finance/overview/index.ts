import request from '@/config/axios'

// ==================== 收支总览 (Overview) ====================

export interface AccountSummaryRespVO {
  accountId?: number
  accountName: string
  accountType?: string
  accountNature?: string
  currency?: string
  openingBalance: number
  inflowAmount: number
  outflowAmount: number
  endingBalance: number
  currentBalance: number
}

export interface CategorySummaryRespVO {
  category: string
  direction: 'DEBIT' | 'CREDIT' | string
  debitAmount: number
  creditAmount: number
  amount: number
  netAmount: number
  count: number
}

export interface OverviewSummaryRespVO {
  periodStart: string
  periodEnd: string
  accountId?: number
  accountList: AccountSummaryRespVO[]
  total: AccountSummaryRespVO
  categoryList: CategorySummaryRespVO[]
}

export interface OverviewSummaryReqVO {
  periodStart?: string
  periodEnd?: string
  accountId?: number
}

export interface GrossMarginRespVO {
  periodStart: string
  periodEnd: string
  revenue: number
  cost: number
  grossProfit: number
  grossMarginRate: number
  hasArData: boolean
  disclaimer: string
  emptyStateNotice?: string
}

export interface GrossMarginReqVO {
  periodStart?: string
  periodEnd?: string
}

export const getOverviewSummary = (params: OverviewSummaryReqVO) => {
  return request.get<OverviewSummaryRespVO>({
    url: '/lunaerp/finance/overview/summary',
    params
  })
}

export const getGrossMargin = (params: GrossMarginReqVO) => {
  return request.get<GrossMarginRespVO>({
    url: '/lunaerp/finance/overview/gross-margin',
    params
  })
}
