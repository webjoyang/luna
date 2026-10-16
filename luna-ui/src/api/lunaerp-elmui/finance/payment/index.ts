import request from '@/config/axios'

/**
 * 付款申请类型枚举
 */
export type PaymentApplyType = 'RECON' | 'PREPAY'

/**
 * 付款申请审批状态枚举
 */
export type PaymentApplyStatus = 'PENDING' | 'APPROVING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

/**
 * 收付款方向枚举
 */
export type PaymentDirection = 'IN' | 'OUT'

/**
 * 收付款单状态枚举
 */
export type PaymentStatus = 'COMPLETED' | 'REVERSED'

/**
 * 财务收付款业务错误码常量（对齐 FinanceErrorCodeConstants）
 */
export const FINANCE_PAYMENT_ERROR_CODES = {
  // 对账单累计申请超额
  SUPPLIER_RECON_APPLY_AMOUNT_EXCEED: 1030022007,

  // 收付款与核销错误码 (1-030-023-xxx)
  APPLY_NOT_EXISTS: 1030023000,
  APPLY_STATUS_NOT_ALLOWED: 1030023001,
  APPLY_RECON_NOT_CONFIRMED: 1030023002,
  ALLOCATE_EXCEED: 1030023003,
  ACCOUNT_BALANCE_NOT_ENOUGH: 1030023004,
  PREPAY_BALANCE_NOT_ENOUGH: 1030023005,
  ACCOUNT_NOT_AVAILABLE: 1030023006,
  AMOUNT_INVALID: 1030023007,
  APPLY_NO_EXHAUSTED: 1030023008,
  PAYMENT_NOT_EXISTS: 1030023009,
  PAYMENT_NO_EXHAUSTED: 1030023010
} as const

/**
 * 付款申请单 Response VO
 */
export interface PaymentApplyRespVO {
  id: number
  applyNo: string
  applyType: PaymentApplyType | string
  supplierId: number
  supplierName: string
  reconId?: number
  reconNo?: string
  amount: number
  allocatedAmount?: number
  remainAmount?: number
  purpose?: string
  bankName?: string
  bankAccount?: string
  approvalStatus: PaymentApplyStatus | string
  remark?: string
  creator?: string
  createTime?: string
}

/**
 * 付款申请单分页查询 Request VO
 */
export interface PaymentApplyPageReqVO extends PageParam {
  supplierId?: number
  applyType?: string
  approvalStatus?: string
  keyword?: string
}

/**
 * 付款申请单创建 Request VO
 */
export interface PaymentApplyCreateReqVO {
  applyType: PaymentApplyType | string
  supplierId?: number
  reconId?: number
  amount: number
  purpose?: string
  bankName?: string
  bankAccount?: string
  remark?: string
}

/**
 * 付款申请单修改 Request VO
 */
export interface PaymentApplyUpdateReqVO {
  id: number
  amount: number
  purpose?: string
  remark?: string
}

/**
 * 付款登记核销明细 Request VO
 */
export interface PaymentRegisterItemReqVO {
  applyId: number
  allocateAmount?: number
}

/**
 * 付款登记 Request VO（OUT 侧）
 */
export interface PaymentRegisterReqVO {
  supplierId: number
  accountId: number
  payAmount?: number
  prepayUsedAmount?: number
  remark?: string
  items: PaymentRegisterItemReqVO[]
}

/**
 * 收付款核销明细 Response VO
 */
export interface PaymentRefRespVO {
  id: number
  paymentId: number
  refType: string
  refId: number
  applyNo?: string
  allocateAmount: number
  createTime?: string
}

/**
 * 收付款单 Response VO
 */
export interface PaymentRespVO {
  id: number
  paymentNo: string
  direction: PaymentDirection | string
  supplierId?: number
  supplierName?: string
  customerId?: number
  accountId: number
  accountName?: string
  payAmount: number
  prepayUsedAmount: number
  status: PaymentStatus | string
  remark?: string
  creator?: string
  createTime?: string
  items?: PaymentRefRespVO[]
}

/**
 * 付款单分页查询 Request VO（OUT 侧）
 */
export interface PaymentPageReqVO extends PageParam {
  supplierId?: number
  accountId?: number
  createTime?: string[]
  keyword?: string
}

/**
 * 资金账户简要信息 Response VO
 */
export interface FinanceAccountSimpleRespVO {
  id: number
  name?: string
  accountName?: string
  accountType?: string
  currency?: string
  status?: string | number
  currentBalance?: number
}

// ========== 付款申请 API 接口契约 ==========

/** 分页查询付款申请单 */
export const getPaymentApplyPage = (params: PaymentApplyPageReqVO) => {
  return request.get<PageResult<PaymentApplyRespVO>>({
    url: '/lunaerp/finance/payment-apply/page',
    params
  })
}

/** 获得付款申请单详情 */
export const getPaymentApply = (id: number) => {
  return request.get<PaymentApplyRespVO>({
    url: `/lunaerp/finance/payment-apply/get?id=${id}`
  })
}

/** 创建付款申请单 */
export const createPaymentApply = (data: PaymentApplyCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment-apply/create',
    data
  })
}

/** 修改付款申请单（仅 DRAFT/PENDING/REJECTED） */
export const updatePaymentApply = (data: PaymentApplyUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/payment-apply/update',
    data
  })
}

/** 删除付款申请单（仅 DRAFT/PENDING 软删进回收站） */
export const deletePaymentApply = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/finance/payment-apply/delete?id=${id}`
  })
}

/** 提交付款申请单审批（02号审批引擎） */
export const submitPaymentApply = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/finance/payment-apply/submit?id=${id}`
  })
}

/** 撤回付款申请单审批（仅 APPROVING 态） */
export const cancelApprovePaymentApply = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/finance/payment-apply/cancel-approve?id=${id}`
  })
}

/** 获得供应商实时预付余额 */
export const getPrepayBalance = (supplierId: number) => {
  return request.get<number>({
    url: `/lunaerp/finance/payment-apply/prepay-balance?supplierId=${supplierId}`
  })
}

// ========== 付款登记与收付款单 API 接口契约 ==========

/** 付款登记（出纳执行·OUT 侧） */
export const registerPayment = (data: PaymentRegisterReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment/register',
    data
  })
}

/** 获得收付款单详情（内嵌 ref 核销行） */
export const getPayment = (id: number) => {
  return request.get<PaymentRespVO>({
    url: `/lunaerp/finance/payment/get?id=${id}`
  })
}

/** 获得付款单分页列表（direction=OUT 固定） */
export const getPaymentPage = (params: PaymentPageReqVO) => {
  return request.get<PageResult<PaymentRespVO>>({
    url: '/lunaerp/finance/payment/page',
    params
  })
}

/** 获得资金账户精简列表（simple-list） */
export const getFinanceAccountSimpleList = () => {
  return request.get<FinanceAccountSimpleRespVO[]>({
    url: '/lunaerp/finance/account/simple-list'
  })
}
