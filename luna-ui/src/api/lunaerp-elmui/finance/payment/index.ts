import request from '@/config/axios'

/**
 * 付款申请单 VO
 */
export interface PaymentApplyVO {
  id: number
  /** 付款申请单号（FK+YYYYMMDD+3位流水） */
  applyNo: string
  /** 申请类型：RECON(对账付款) / PREPAY(预付付款) */
  applyType: string
  /** 供应商ID */
  supplierId: number
  /** 供应商名称 */
  supplierName: string
  /** 关联对账单ID（RECON必填） */
  reconId?: number
  /** 关联对账单号 */
  reconNo?: string
  /** 本次申请金额（元） */
  amount: number
  /** 预付用途（PREPAY必填） */
  purpose?: string
  /** 开户银行（脱敏展示） */
  bankName?: string
  /** 银行账号（脱敏展示） */
  bankAccount?: string
  /** 审批状态：PENDING / APPROVING / APPROVED / REJECTED / CANCELLED */
  approvalStatus: string
  /** 备注 */
  remark?: string
  /** 已核销金额（元，实时聚合，只读） */
  allocatedAmount?: number
  /** 创建时间 */
  createTime?: string
}

/**
 * 付款申请分页查询参数
 */
export interface PaymentApplyPageParam {
  pageNo?: number
  pageSize?: number
  supplierId?: number
  applyType?: string
  approvalStatus?: string
  keyword?: string
}

/**
 * 创建/更新付款申请入参
 */
export interface PaymentApplySaveReqVO {
  id?: number
  applyType: string
  supplierId?: number
  reconId?: number
  amount: number
  purpose?: string
  remark?: string
}

/**
 * 付款单核销明细 VO
 */
export interface PaymentRefVO {
  id?: number
  paymentId?: number
  refType?: string
  refId: number
  applyNo?: string
  allocateAmount: number
}

/**
 * 付款单 VO
 */
export interface PaymentVO {
  id: number
  /** 付款单号（PAY+YYYYMMDD+3位流水） */
  paymentNo: string
  /** 收付款方向：OUT (付款) */
  direction: string
  /** 供应商ID */
  supplierId: number
  /** 供应商名称 */
  supplierName?: string
  /** 资金账户ID */
  accountId: number
  /** 资金账户名称（前端展示用） */
  accountName?: string
  /** 本次资金实付金额（元） */
  payAmount: number
  /** 预付抵扣额（元） */
  prepayUsedAmount: number
  /** 状态：COMPLETED / REVERSED */
  status: string
  /** 备注 */
  remark?: string
  /** 创建/登记时间 */
  createTime?: string
  /** 核销子项列表 */
  items?: PaymentRefVO[]
}

/**
 * 付款单分页查询入参
 */
export interface PaymentPageParam {
  pageNo?: number
  pageSize?: number
  supplierId?: number
  accountId?: number
  createTimeStart?: string
  createTimeEnd?: string
}

/**
 * 付款登记明细项入参
 */
export interface PaymentRegisterItemReqVO {
  applyId: number
  allocateAmount: number
}

/**
 * 付款登记入参
 */
export interface PaymentRegisterReqVO {
  supplierId: number
  accountId: number
  payAmount: number
  prepayUsedAmount?: number
  remark?: string
  items: PaymentRegisterItemReqVO[]
}

// =================== 付款申请接口 ===================

/** 分页查询付款申请列表 */
export const getPaymentApplyPage = (params: PaymentApplyPageParam) => {
  return request.get<{ list: PaymentApplyVO[]; total: number }>({
    url: '/lunaerp/finance/payment-apply/page',
    params
  })
}

/** 查询付款申请单详情 */
export const getPaymentApply = (id: number | string) => {
  return request.get<PaymentApplyVO>({
    url: '/lunaerp/finance/payment-apply/get?id=' + id
  })
}

/** 创建付款申请 */
export const createPaymentApply = (data: PaymentApplySaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment-apply/create',
    data
  })
}

/** 更新付款申请（仅草稿/驳回） */
export const updatePaymentApply = (data: PaymentApplySaveReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/payment-apply/update',
    data
  })
}

/** 删除付款申请（仅草稿，软删进回收站） */
export const deletePaymentApply = (id: number | string) => {
  return request.delete<boolean>({
    url: '/lunaerp/finance/payment-apply/delete?id=' + id
  })
}

/** 提交付款申请审批 */
export const submitPaymentApply = (id: number | string) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment-apply/submit?id=' + id
  })
}

/** 撤回付款申请审批（仅审批中） */
export const cancelApprovePaymentApply = (id: number | string) => {
  return request.post<boolean>({
    url: '/lunaerp/finance/payment-apply/cancel-approve?id=' + id
  })
}

/** 获取供应商实时预付余额（只读聚合） */
export const getPrepayBalance = (supplierId: number | string) => {
  return request.get<number>({
    url: '/lunaerp/finance/payment-apply/prepay-balance?supplierId=' + supplierId
  })
}

// =================== 付款登记接口 ===================

/** 分页查询付款单流水（固定 direction=OUT） */
export const getPaymentPage = (params: PaymentPageParam) => {
  return request.get<{ list: PaymentVO[]; total: number }>({
    url: '/lunaerp/finance/payment/page',
    params
  })
}

/** 查询付款单详情（含核销行列表） */
export const getPayment = (id: number | string) => {
  return request.get<PaymentVO>({
    url: '/lunaerp/finance/payment/get?id=' + id
  })
}

/** 付款登记（原子事务核销多张申请） */
export const registerPayment = (data: PaymentRegisterReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment/register',
    data
  })
}
