import request from '@/config/axios'

// 货物明细 VO
export interface ExportDocItemVO {
  id?: number
  docId?: number
  lineNo?: number
  nameCn?: string
  nameEn?: string
  hsCode?: string
  spec?: string
  quantity?: number
  unit?: string
  unitPrice?: number
  amount?: number
  netWeight?: number
  grossWeight?: number
  packages?: number
  volume?: number
  origin?: string
  remark?: string
  createTime?: string
}

// 出口单证套 VO
export interface ExportDocVO {
  id?: number
  docNo?: string
  status?: string
  sourceType?: string
  contractId?: number
  contractNo?: string
  shipmentId?: number
  shipmentNo?: string
  customerId?: number
  customerName?: string
  sellerName?: string
  sellerAddress?: string
  sellerBankInfo?: string
  buyerName?: string
  buyerAddress?: string
  buyerBankInfo?: string
  incoterm?: string
  portOfLoading?: string
  portOfDischarge?: string
  countryOfDest?: string
  transportMode?: string
  paymentTerm?: string
  currency?: string
  shippingMark?: string
  piMeta?: string
  ciMeta?: string
  plMeta?: string
  remarks?: string
  items?: ExportDocItemVO[]
  createTime?: string
}

// 卖方出口配置 VO
export interface ExportSellerConfigVO {
  id?: number
  sellerName?: string
  sellerAddress?: string
  bankName?: string
  bankAccount?: string
  bankSwift?: string
  bankAddress?: string
  remark?: string
}

// 出口单证打印 VO（豁免脱敏）
export interface ExportDocPrintVO extends ExportDocVO {
  sellerBankName?: string
  sellerBankAccount?: string
  sellerBankSwift?: string
  sellerBankAddress?: string
  qrUrl?: string
  totalQuantity?: number
  totalAmount?: number
  totalNetWeight?: number
  totalGrossWeight?: number
  totalPackages?: number
  totalVolume?: number
  totalCustom?: boolean
}

// 分页查询出口单证套
export const getExportDocPage = (params: any) => {
  return request.get({ url: '/lunaerp/business/export-doc/page', params })
}

// 获取出口单证套详情
export const getExportDoc = (id: number) => {
  return request.get<ExportDocVO>({ url: '/lunaerp/business/export-doc/get?id=' + id })
}

// 创建出口单证套
export const createExportDoc = (data: ExportDocVO) => {
  return request.post({ url: '/lunaerp/business/export-doc/create', data })
}

// 更新出口单证套
export const updateExportDoc = (data: ExportDocVO) => {
  return request.put({ url: '/lunaerp/business/export-doc/update', data })
}

// 删除出口单证套
export const deleteExportDoc = (id: number) => {
  return request.delete({ url: '/lunaerp/business/export-doc/delete?id=' + id })
}

// 获取建议单证套号
export const getNextDocNo = () => {
  return request.get<string>({ url: '/lunaerp/business/export-doc/next-doc-no' })
}

// 定稿出口单证套
export const finalizeExportDoc = (id: number) => {
  return request.put({ url: '/lunaerp/business/export-doc/finalize?id=' + id })
}

// 撤销定稿出口单证套
export const revokeExportDoc = (id: number) => {
  return request.put({ url: '/lunaerp/business/export-doc/revoke?id=' + id })
}

// 获取租户卖方出口配置
export const getSellerConfig = () => {
  return request.get<ExportSellerConfigVO>({ url: '/lunaerp/business/export-doc/seller-config' })
}

// 保存租户卖方出口配置
export const saveSellerConfig = (data: ExportSellerConfigVO) => {
  return request.put({ url: '/lunaerp/business/export-doc/seller-config', data })
}

// 从销售合同带出草稿
export const getDraftFromContract = (contractId: number) => {
  return request.get<ExportDocVO>({
    url: '/lunaerp/business/export-doc/from-contract',
    params: { contractId }
  })
}

// 从发货单带出草稿
export const getDraftFromShipment = (shipmentId: number) => {
  return request.get<ExportDocVO>({
    url: '/lunaerp/business/export-doc/from-shipment',
    params: { shipmentId }
  })
}

// 重新同步源单据数据
export const resyncFromSource = (id: number) => {
  return request.put({ url: '/lunaerp/business/export-doc/resync-from-source?id=' + id })
}

// 获取出口单证打印数据
export const getExportDocPrintData = (id: string | number, template?: string) => {
  return request.get<ExportDocPrintVO>({
    url: `/lunaerp/business/export-doc/${id}/print-data`,
    params: template ? { template } : undefined
  })
}
