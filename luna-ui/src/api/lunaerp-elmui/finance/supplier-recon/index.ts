import request from '@/config/axios'

/**
 * 供应商对账单状态枚举
 */
export type SupplierReconStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED'

/**
 * 对账单行类型枚举
 */
export type SupplierReconLineType = 'PROCESS' | 'PURCHASE' | 'ADJUST'

/**
 * 计价基准枚举
 */
export type SupplierReconPricingBasis = 'KG' | 'METER'

/**
 * 业务错误码常量（对齐 FinanceErrorCodeConstants）
 */
export const SUPPLIER_RECON_ERROR_CODES = {
  NOT_EXISTS: 1030022000,
  STATUS_NOT_ALLOWED: 1030022001,
  SUPPLIER_NOT_EXISTS: 1030022002,
  NO_SOURCE_ITEMS: 1030022003,
  SOURCE_REFERENCED: 1030022004,
  ADJUST_LINE_INVALID: 1030022005,
  HAS_PAYMENT_APPLY: 1030022006,
  APPLY_AMOUNT_EXCEED: 1030022007
} as const

/**
 * 供应商对账单行 Response VO
 */
export interface SupplierReconItemRespVO {
  id?: number
  reconId?: number
  lineType: SupplierReconLineType | string
  sourceRefType?: string
  sourceRefId?: number
  sourceRefItemId?: number
  inspectionOrderId?: number
  inspectionOrderNo?: string
  inspectionType?: string
  stockInId?: number
  stockInNo?: string
  stockInItemId?: number
  yarnOrderId?: number
  yarnOrderNo?: string
  productId?: number
  productCode?: string
  productName?: string
  batchNo?: string
  qtyKg?: number | null
  qtyM?: number | null
  rollCount?: number
  qtyUnit?: string
  pricingBasis?: SupplierReconPricingBasis | string
  billQty?: number | null
  unitPrice?: number | null
  lineAmount: number
  adjustReason?: string
  remark?: string
}

/**
 * 供应商对账取数候选行 Response VO
 */
export interface SupplierReconSourceItemRespVO {
  lineType: SupplierReconLineType | string
  sourceRefType?: string
  sourceRefId?: number
  sourceRefItemId?: number
  inspectionOrderId?: number
  inspectionOrderNo?: string
  inspectionType?: string
  stockInId?: number
  stockInNo?: string
  stockInItemId?: number
  yarnOrderId?: number
  yarnOrderNo?: string
  productId?: number
  productCode?: string
  productName?: string
  batchNo?: string
  qtyKg?: number | null
  qtyM?: number | null
  rollCount?: number
  qtyUnit?: string
  pricingBasis?: SupplierReconPricingBasis | string
  billQty?: number | null
  unitPrice?: number | null
  lineAmount: number
  fingerprint?: string
}

/**
 * 供应商对账单行 Save VO
 */
export interface SupplierReconItemSaveVO {
  id?: number
  lineType: SupplierReconLineType | string
  sourceRefType?: string
  sourceRefId?: number
  sourceRefItemId?: number
  inspectionOrderId?: number
  inspectionOrderNo?: string
  inspectionType?: string
  stockInId?: number
  stockInNo?: string
  stockInItemId?: number
  yarnOrderId?: number
  yarnOrderNo?: string
  productId?: number
  productCode?: string
  productName?: string
  batchNo?: string
  qtyKg?: number | null
  qtyM?: number | null
  qtyUnit?: string
  pricingBasis?: SupplierReconPricingBasis | string
  billQty?: number | null
  unitPrice?: number | null
  lineAmount?: number
  adjustReason?: string
  remark?: string
}

/**
 * 供应商对账单 Response VO
 */
export interface SupplierReconRespVO {
  id: number
  reconNo: string
  supplierId: number
  supplierName: string
  periodStart: string
  periodEnd: string
  status: SupplierReconStatus | string
  totalAmount: number
  appliedAmount: number
  confirmedBy?: number
  confirmedAt?: string
  cancelledBy?: number
  cancelledAt?: string
  remark?: string
  createTime: string
  items?: SupplierReconItemRespVO[]
}

/**
 * 分页查询参数 VO
 */
export interface SupplierReconPageReqVO extends PageParam {
  supplierId?: number
  status?: string
  periodStart?: string
  periodEnd?: string
  keyword?: string
}

/**
 * 取数预览 Request VO
 */
export interface SupplierReconPreviewReqVO {
  supplierId: number
  periodStart: string
  periodEnd: string
}

/**
 * 创建对账单 Request VO
 */
export interface SupplierReconCreateReqVO {
  supplierId: number
  periodStart: string
  periodEnd: string
  remark?: string
  items: SupplierReconItemSaveVO[]
}

/**
 * 更新对账单 Request VO
 */
export interface SupplierReconUpdateReqVO {
  id: number
  remark?: string
  items: SupplierReconItemSaveVO[]
}

/**
 * 打印明细行 Response VO
 */
export interface SupplierReconPrintItemRespVO {
  id?: number
  reconId?: number
  lineType: string
  sourceRefType?: string
  sourceRefId?: number
  sourceRefItemId?: number
  inspectionOrderId?: number
  inspectionOrderNo?: string
  inspectionType?: string
  stockInId?: number
  stockInNo?: string
  stockInItemId?: number
  yarnOrderId?: number
  yarnOrderNo?: string
  productId?: number
  productCode?: string
  productName?: string
  batchNo?: string
  qtyKg?: number | null
  qtyM?: number | null
  qtyUnit?: string
  pricingBasis?: string
  billQty?: number | null
  unitPrice?: number | null
  lineAmount: number
  adjustReason?: string
  remark?: string
}

/**
 * 打印数据 Response VO
 */
export interface SupplierReconPrintRespVO {
  bizType: string
  title: string
  id: number
  reconNo: string
  supplierId: number
  supplierName: string
  supplierContact?: string
  supplierPhone?: string
  periodStart: string
  periodEnd: string
  status: string
  totalAmount: number
  totalProcessAmount: number
  totalPurchaseAmount: number
  totalAdjustAmount: number
  confirmedBy?: number
  confirmedByName?: string
  confirmedAt?: string
  cancelledBy?: number
  cancelledByName?: string
  cancelledAt?: string
  remark?: string
  creator?: string
  createTime?: string
  printTime?: string
  items: SupplierReconPrintItemRespVO[]
}

// ========== API 接口契约 ==========

/** 分页查询供应商对账单 */
export const getSupplierReconPage = (params: SupplierReconPageReqVO) => {
  return request.get<PageResult<SupplierReconRespVO>>({
    url: '/lunaerp/finance/supplier-recon/page',
    params
  })
}

/** 获得供应商对账单详情 */
export const getSupplierRecon = (id: number) => {
  return request.get<SupplierReconRespVO>({
    url: `/lunaerp/finance/supplier-recon/get?id=${id}`
  })
}

/** 取数预览（候选行不落库） */
export const previewSupplierRecon = (data: SupplierReconPreviewReqVO) => {
  return request.post<SupplierReconSourceItemRespVO[]>({
    url: '/lunaerp/finance/supplier-recon/preview',
    data
  })
}

/** 创建供应商对账单（草稿） */
export const createSupplierRecon = (data: SupplierReconCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/supplier-recon/create',
    data
  })
}

/** 更新供应商对账单（仅草稿） */
export const updateSupplierRecon = (data: SupplierReconUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/supplier-recon/update',
    data
  })
}

/** 重新取数（仅草稿，替换全部 PROCESS/PURCHASE 行，保留 ADJUST 行） */
export const reFetchSupplierRecon = (id: number) => {
  return request.put<boolean>({
    url: `/lunaerp/finance/supplier-recon/re-fetch?id=${id}`
  })
}

/** 删除供应商对账单（仅草稿，软删进回收站） */
export const deleteSupplierRecon = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/finance/supplier-recon/delete?id=${id}`
  })
}

/** 确认供应商对账单（仅草稿，冻结快照） */
export const confirmSupplierRecon = (id: number) => {
  return request.put<boolean>({
    url: `/lunaerp/finance/supplier-recon/confirm?id=${id}`
  })
}

/** 作废供应商对账单（仅已确认，释放指纹） */
export const cancelSupplierRecon = (id: number) => {
  return request.put<boolean>({
    url: `/lunaerp/finance/supplier-recon/cancel?id=${id}`
  })
}

/** 获得供应商对账单打印数据（豁免金额脱敏） */
export const getSupplierReconPrintData = (id: number) => {
  return request.get<SupplierReconPrintRespVO>({
    url: `/lunaerp/finance/supplier-recon/print-data?id=${id}`
  })
}

/** 对已确认对账单生成付款申请 */
export const createPaymentApplyForRecon = (data: {
  reconId: number
  amount: number
  remark?: string
}) => {
  return request.post<number>({
    url: '/lunaerp/finance/payment-apply/create',
    data: {
      applyType: 'RECON',
      reconId: data.reconId,
      amount: data.amount,
      remark: data.remark
    }
  })
}
