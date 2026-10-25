import request from '@/config/axios'

// ==================== 资金账户 (Account) ====================

export interface AccountRespVO {
  id: number
  accountName: string
  accountType: 'CASH' | 'BANK' | 'WECHAT' | 'ALIPAY' | string
  accountNo?: string
  accountNature?: 'PUBLIC' | 'PRIVATE' | string
  currency: string
  openingBalance: number
  currentBalance: number
  status: 'ENABLE' | 'DISABLE' | string
  remark?: string
  createTime?: string
}

export interface AccountPageReqVO extends PageParam {
  accountName?: string
  accountType?: string
  accountNature?: string
  currency?: string
  status?: string
}

export interface AccountSaveReqVO {
  accountName: string
  accountType: string
  accountNo?: string
  accountNature?: string
  currency: string
  openingBalance: number
  remark?: string
}

export interface AccountUpdateReqVO {
  id: number
  accountName?: string
  accountNo?: string
  accountNature?: string
  openingBalance?: number
  status?: string
  remark?: string
}

export interface FinanceAccountSimpleRespVO {
  id: number
  accountName: string
  accountType: string
  currency: string
  status: string
  currentBalance: number
}

export const getAccountPage = (params: AccountPageReqVO) => {
  return request.get<PageResult<AccountRespVO>>({
    url: '/lunaerp/finance/account/page',
    params
  })
}

export const getAccount = (id: number) => {
  return request.get<AccountRespVO>({
    url: `/lunaerp/finance/account/get?id=${id}`
  })
}

export const createAccount = (data: AccountSaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/account/create',
    data
  })
}

export const updateAccount = (data: AccountUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/account/update',
    data
  })
}

export const deleteAccount = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/finance/account/delete?id=${id}`
  })
}

export const getSimpleAccountList = () => {
  return request.get<FinanceAccountSimpleRespVO[]>({
    url: '/lunaerp/finance/account/simple-list'
  })
}

// ==================== 账户间转账 (Transfer) ====================

export interface TransferRespVO {
  id: number
  transferNo: string
  fromAccountId: number
  fromAccountName: string
  toAccountId: number
  toAccountName: string
  currency: string
  amount: number
  transferDate: string
  status: string
  remark?: string
  createTime?: string
}

export interface TransferPageReqVO extends PageParam {
  transferNo?: string
  fromAccountId?: number
  toAccountId?: number
  transferDateStart?: string
  transferDateEnd?: string
}

export interface TransferCreateReqVO {
  fromAccountId: number
  toAccountId: number
  currency?: string
  amount: number
  transferDate: string
  remark?: string
}

export const createTransfer = (data: TransferCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/transfer/create',
    data
  })
}

export const getTransfer = (id: number) => {
  return request.get<TransferRespVO>({
    url: `/lunaerp/finance/transfer/get?id=${id}`
  })
}

export const getTransferPage = (params: TransferPageReqVO) => {
  return request.get<PageResult<TransferRespVO>>({
    url: '/lunaerp/finance/transfer/page',
    params
  })
}

// ==================== 其他收支单 (OtherTxn) ====================

export interface OtherTxnRespVO {
  id: number
  txnNo: string
  accountId: number
  accountName: string
  direction: 'DEBIT' | 'CREDIT' | string
  category: string
  currency: string
  originalAmount: number
  txnDate: string
  remark?: string
  createTime?: string
}

export interface OtherTxnPageReqVO extends PageParam {
  txnNo?: string
  accountId?: number
  direction?: string
  category?: string
  txnDateStart?: string
  txnDateEnd?: string
}

export interface OtherTxnCreateReqVO {
  accountId: number
  direction: 'DEBIT' | 'CREDIT' | string
  category: string
  originalAmount: number
  txnDate: string
  remark?: string
}

export const createOtherTxn = (data: OtherTxnCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/other-txn/create',
    data
  })
}

export const getOtherTxn = (id: number) => {
  return request.get<OtherTxnRespVO>({
    url: `/lunaerp/finance/other-txn/get?id=${id}`
  })
}

export const getOtherTxnPage = (params: OtherTxnPageReqVO) => {
  return request.get<PageResult<OtherTxnRespVO>>({
    url: '/lunaerp/finance/other-txn/page',
    params
  })
}

// ==================== 资金流水 (Ledger) ====================

export interface LedgerRespVO {
  id: number
  bizDate: string
  partyType?: string
  partyId?: number
  partyName?: string
  accountId: number
  category: string
  direction: 'DEBIT' | 'CREDIT' | string
  currency: string
  exchangeRate: number
  originalAmount: number
  baseAmount: number
  refType?: string
  refId?: number
  refItemId?: number
  refNo?: string
  reversalOfId?: number
  remark?: string
  sourceRoute?: string
  createTime?: string
}

export interface LedgerPageReqVO extends PageParam {
  accountId?: number
  category?: string
  direction?: string
  partyType?: string
  partyId?: number
  partyName?: string
  refNo?: string
  periodStart?: string
  periodEnd?: string
  keyword?: string
}

export const getLedgerPage = (params: LedgerPageReqVO) => {
  return request.get<PageResult<LedgerRespVO>>({
    url: '/lunaerp/finance/ledger/page',
    params
  })
}
