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

// ========== 收款单（IN 侧）类型与 API 契约 ==========

/**
 * 收款类型枚举
 */
export type ReceiptType = 'RECEIPT' | 'ADVANCE'

/**
 * 拟核销明细项 Request VO
 */
export interface ReceiptItemReqVO {
  entryId: number
  allocateAmount: number
}

/**
 * 收款单草稿保存 Request VO
 */
export interface ReceiptSaveReqVO {
  paymentNo?: string
  customerId: number
  accountId: number
  currency: string
  receiptType: ReceiptType | string
  payAmount: number
  receiptDate?: string
  bankRefNo?: string
  handlerName?: string
  remark?: string
  items?: ReceiptItemReqVO[]
}

/**
 * 收款单草稿更新 Request VO
 */
export interface ReceiptUpdateReqVO {
  id: number
  accountId?: number
  receiptType?: ReceiptType | string
  payAmount?: number
  receiptDate?: string
  bankRefNo?: string
  handlerName?: string
  remark?: string
  items?: ReceiptItemReqVO[]
}

/**
 * 收款单分页查询 Request VO
 */
export interface ReceiptPageReqVO extends PageParam {
  customerId?: number
  accountId?: number
  receiptType?: string
  approvalStatus?: string
  receiptDate?: string[]
  paymentNo?: string
}

/**
 * 收款单核销明细项 Response VO
 */
export interface ReceiptItemRespVO {
  id: number
  entryId: number
  entryNo?: string
  bizDate?: string
  refType?: string
  refNo?: string
  contractNo?: string
  productName?: string
  currency?: string
  originalAmount?: number
  allocateAmount: number
  unallocatedAmount?: number
}

/**
 * 收款单 Response VO
 */
export interface ReceiptRespVO {
  id: number
  paymentNo: string
  direction: 'IN' | string
  customerId: number
  customerName?: string
  accountId: number
  accountName?: string
  currency: string
  exchangeRate?: number
  receiptType: ReceiptType | string
  payAmount: number
  allocatedTotalAmount?: number
  unallocatedAmount?: number
  receiptDate: string
  bankRefNo?: string
  handlerName?: string
  status: string
  approvalStatus: string
  remark?: string
  creator?: string
  createTime?: string
  items?: ReceiptItemRespVO[]
}

/**
 * FIFO 自动分配试算 Request VO
 */
export interface AllocatePreviewReqVO {
  customerId: number
  currency: string
  payAmount: number
}

/**
 * FIFO 自动分配试算 Response VO
 */
export interface AllocatePreviewRespVO {
  entryId: number
  bizDate: string
  refType: string
  refId: number
  refItemId?: number
  refNo?: string
  contractNo?: string
  productName?: string
  currency: string
  originalAmount: number
  unallocatedAmount: number
  allocateAmount: number
}

/**
 * 未核销 AR 分录明细 Response VO
 */
export interface ArBalanceRespVO {
  entryId: number
  bizDate: string
  refType: string
  refId: number
  refItemId?: number
  refNo?: string
  contractId?: number
  contractNo?: string
  statementId?: number
  statementNo?: string
  productName?: string
  currency: string
  originalAmount: number
  allocatedAmount: number
  unallocatedAmount: number
}

// ========== 收款单 API 接口契约 ==========

/** 创建收款单草稿 */
export const createReceipt = (data: ReceiptSaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment/create',
    data
  })
}

/** 更新收款单草稿（仅 PENDING/REJECTED） */
export const updateReceipt = (data: ReceiptUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/payment/update',
    data
  })
}

/** 删除收款单草稿（仅 PENDING） */
export const deleteReceipt = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/finance/payment/delete?id=${id}`
  })
}

/** 提交收款单审批（02号审批引擎） */
export const submitReceipt = (id: number) => {
  return request.put<boolean>({
    url: `/lunaerp/finance/payment/submit?id=${id}`
  })
}

/** 按发货日 FIFO 自动分配试算（纯内存不落库） */
export const allocatePreview = (data: AllocatePreviewReqVO) => {
  return request.post<AllocatePreviewRespVO[]>({
    url: '/lunaerp/finance/payment/allocate-preview',
    data
  })
}

/** 获得未核销 AR 分录明细列表（选单与核销数据源） */
export const getArBalanceList = (customerId: number, currency: string) => {
  return request.get<ArBalanceRespVO[]>({
    url: `/lunaerp/finance/payment/ar-balance?customerId=${customerId}&currency=${currency}`
  })
}

/** 获得收款单详情（内嵌拟核销明细行） */
export const getReceipt = (id: number) => {
  return request.get<ReceiptRespVO>({
    url: `/lunaerp/finance/payment/receipt-get?id=${id}`
  })
}

/** 获得收款单分页列表 */
export const getReceiptPage = (params: ReceiptPageReqVO) => {
  return request.get<PageResult<ReceiptRespVO>>({
    url: '/lunaerp/finance/payment/receipt-page',
    params
  })
}

/**
 * 预收转核销明细 Request VO
 */
export interface AdvanceWriteoffItemReqVO {
  entryId: number
  allocateAmount: number
}

/**
 * 预收转核销 Request VO
 */
export interface AdvanceWriteoffReqVO {
  paymentId: number
  items: AdvanceWriteoffItemReqVO[]
}

/** 收款单红冲（仅 status=COMPLETED 可冲） */
export const reverseReceipt = (id: number) => {
  return request.put<boolean>({
    url: `/lunaerp/finance/payment/reverse?id=${id}`
  })
}

/** 预收转核销（在原已过账 ADVANCE 单上追加核销行，不建新单，不走审批） */
export const advanceWriteoff = (data: AdvanceWriteoffReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/finance/payment/advance-writeoff',
    data
  })
}
