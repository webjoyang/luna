import request from '@/config/axios'

/**
 * 供应商对账单行明细 VO
 */
export interface SupplierReconItemVO {
  id?: number
  reconId?: number
  /** 行类型：PROCESS(加工费) / PURCHASE(采购货款) / ADJUST(调整项) */
  lineType: string
  /** 源单据类型：INSPECTION / STOCK_IN_ITEM */
  sourceRefType?: string
  sourceRefId?: number
  sourceRefItemId?: number
  /** 检验单快照 */
  inspectionOrderId?: number
  inspectionOrderNo?: string
  /** 检验类型：GREIGE(胚布检验) / FINISHED(成品检验) */
  inspectionType?: string
  /** 卷数（候选/展示用） */
  rollCount?: number
  /** 入库单快照 */
  stockInId?: number
  stockInNo?: string
  /** 关联纱线采购单快照 */
  yarnOrderId?: number
  yarnOrderNo?: string
  /** 产品快照 */
  productId?: number
  productCode?: string
  productName?: string
  /** 批次 / 缸号 */
  batchNo?: string
  /** 重量（KG） */
  qtyKg?: number
  /** 米数（M） */
  qtyM?: number
  /** 数量单位（KG / M） */
  qtyUnit?: string
  /** 原始数量（PURCHASE行） */
  quantity?: number
  /** 原始单位（PURCHASE行） */
  unit?: string
  /** 计价基准：KG / METER */
  pricingBasis?: string
  /** 结算计费数量 */
  billQty?: number
  /** 结算单价（元） */
  unitPrice?: number
  /** 行金额（元） */
  lineAmount?: number
  /** 调整原因（ADJUST行必填） */
  adjustReason?: string
  /** 备注 */
  remark?: string
  /** 源单据日期 */
  orderDate?: string
  /** 前端行唯一标识（用于无 id 时的 key） */
  _rowKey?: string
}

/**
 * 供应商对账单主表 VO
 */
export interface SupplierReconVO {
  id?: number
  /** 对账单号 */
  reconNo?: string
  /** 供应商ID */
  supplierId: number
  /** 供应商名称 */
  supplierName?: string
  /** 对账区间开始日期 */
  periodStart: string
  /** 对账区间结束日期 */
  periodEnd: string
  /** 状态：DRAFT / CONFIRMED / CANCELLED */
  status?: string
  /** 确认金额合计（元） */
  totalAmount?: number
  /** 确认人ID */
  confirmedBy?: number
  /** 确认人姓名 */
  confirmedByName?: string
  /** 确认时间 */
  confirmedAt?: string
  /** 作废人ID */
  cancelledBy?: number
  /** 作废时间 */
  cancelledAt?: string
  /** 备注 */
  remark?: string
  /** 已申请金额（元，实时聚合，只读） */
  appliedAmount?: number
  /** 创建时间 */
  createTime?: string
  /** 对账明细列表 */
  items?: SupplierReconItemVO[]
}

/**
 * 分页查询参数
 */
export interface SupplierReconPageParam {
  pageNo?: number
  pageSize?: number
  supplierId?: number
  status?: string
  periodStart?: string
  periodEnd?: string
  keyword?: string
}

/**
 * 取数预览入参
 */
export interface SupplierReconPreviewReqVO {
  supplierId: number
  periodStart: string
  periodEnd: string
}

/**
 * 取数预览出参
 */
export interface SupplierReconCandidatesRespVO {
  /** 全部候选明细列表（加工费与采购货款混排） */
  items: SupplierReconItemVO[]
  /** 加工费候选明细列表 */
  processItems: SupplierReconItemVO[]
  /** 采购货款候选明细列表 */
  purchaseItems: SupplierReconItemVO[]
}

/**
 * 保存/更新入参明细行
 */
export interface SupplierReconItemSaveReqVO {
  id?: number
  lineType: string
  sourceRefType?: string
  sourceRefId?: number
  sourceRefItemId?: number
  inspectionOrderId?: number
  inspectionOrderNo?: string
  inspectionType?: string
  stockInId?: number
  stockInNo?: string
  yarnOrderId?: number
  yarnOrderNo?: string
  productId?: number
  productCode?: string
  productName?: string
  batchNo?: string
  qtyKg?: number
  qtyM?: number
  qtyUnit?: string
  pricingBasis?: string
  billQty?: number
  unitPrice?: number
  lineAmount?: number
  adjustReason?: string
  remark?: string
}

/**
 * 创建/更新主表入参
 */
export interface SupplierReconSaveReqVO {
  id?: number
  supplierId: number
  periodStart: string
  periodEnd: string
  remark?: string
  items: SupplierReconItemSaveReqVO[]
}

/**
 * 打印明细行 VO
 */
export interface SupplierReconPrintItemVO {
  id?: number
  lineType: string
  inspectionOrderNo?: string
  inspectionType?: string
  stockInNo?: string
  yarnOrderNo?: string
  productCode?: string
  productName?: string
  batchNo?: string
  qtyKg?: number
  qtyM?: number
  qtyUnit?: string
  pricingBasis?: string
  billQty?: number
  unitPrice?: number
  lineAmount?: number
  adjustReason?: string
  remark?: string
}

/**
 * 打印数据 VO（豁免脱敏）
 */
export interface SupplierReconPrintVO {
  companyTitle?: string
  id: number
  reconNo: string
  supplierId: number
  supplierName: string
  periodStart: string
  periodEnd: string
  status: string
  totalAmount: number
  confirmedBy?: number
  confirmedByName?: string
  confirmedAt?: string
  cancelledBy?: number
  cancelledAt?: string
  remark?: string
  printTime?: string
  items?: SupplierReconPrintItemVO[]
}

/** 分页查询供应商对账单 */
export const getSupplierReconPage = (params: SupplierReconPageParam) => {
  return request.get<{ list: SupplierReconVO[]; total: number }>({
    url: '/lunaerp/finance/supplier-recon/page',
    params
  })
}

/** 查询供应商对账单详情 */
export const getSupplierRecon = (id: number | string) => {
  return request.get<SupplierReconVO>({
    url: '/lunaerp/finance/supplier-recon/get?id=' + id
  })
}

/** 取数预览候选明细 */
export const previewSupplierRecon = (data: SupplierReconPreviewReqVO) => {
  return request.post<SupplierReconCandidatesRespVO>({
    url: '/lunaerp/finance/supplier-recon/preview',
    data
  })
}

/** 创建供应商对账单 */
export const createSupplierRecon = (data: SupplierReconSaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/supplier-recon/create',
    data
  })
}

/** 更新草稿供应商对账单 */
export const updateSupplierRecon = (data: SupplierReconSaveReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/supplier-recon/update',
    data
  })
}

/** 重取草稿供应商对账单（替换全部取数行，保留调整项） */
export const reFetchSupplierRecon = (id: number | string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/supplier-recon/re-fetch?id=' + id
  })
}

/** 确认供应商对账单 */
export const confirmSupplierRecon = (id: number | string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/supplier-recon/confirm?id=' + id
  })
}

/** 作废供应商对账单 */
export const cancelSupplierRecon = (id: number | string) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/supplier-recon/cancel?id=' + id
  })
}

/** 删除草稿供应商对账单（移入回收站） */
export const deleteSupplierRecon = (id: number | string) => {
  return request.delete<boolean>({
    url: '/lunaerp/finance/supplier-recon/delete?id=' + id
  })
}

/** 获取供应商对账单打印数据 */
export const getSupplierReconPrintData = (id: number | string) => {
  return request.get<SupplierReconPrintVO>({
    url: '/lunaerp/finance/supplier-recon/print-data?id=' + id
  })
}
