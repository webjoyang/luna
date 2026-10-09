import request from '@/config/axios'

// 卷清单 VO
export interface ShipmentRollVO {
  id?: number
  shipmentId?: number
  shipmentItemId?: number
  rollNo?: string
  weight?: number
  lengthM?: number
  remark?: string
  createTime?: Date | string
}

// 发货明细 VO
export interface ShipmentItemVO {
  id?: number
  shipmentId?: number
  productId: number
  productCode?: string
  productName?: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  quantity: number
  pieces?: number
  unit: string
  unitPrice?: number
  amount?: number
  sourceStockInItemId?: number
  remark?: string
  createTime?: Date | string
  rolls?: ShipmentRollVO[]
}

// 发货单 VO
export interface ShipmentVO {
  id?: number
  shipmentNo?: string
  status?: string
  customerId: number
  customerName?: string
  salesContractId?: number
  contractNo?: string
  warehouseId: number
  warehouseName?: string
  shippingAddress?: string
  receiveAddress?: string
  carrier?: string
  trackingNumber?: string
  shippingDate?: string
  expectedDeliveryDate?: string
  freightFee?: number
  totalAmount?: number
  totalPieces?: number
  salespersonUserId?: number
  salespersonName?: string
  currency?: string
  signStatus?: string
  signedByName?: string
  signedAt?: string
  signRemark?: string
  remark?: string
  creator?: string
  createTime?: Date | string
  updater?: string
  updateTime?: Date | string
  items?: ShipmentItemVO[]
}

// 发货单分页查询参数
export interface ShipmentPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
  signStatus?: string
  customerId?: number
  warehouseId?: number
  contractNo?: string
  shippingDate?: string[]
  shippingDateStart?: string
  shippingDateEnd?: string
}

// 发货单签收登记参数
export interface ShipmentUpdateSignReqVO {
  id: number
  signStatus: string
  signedByName?: string
  signedAt?: string
  signRemark?: string
}

// 查询发货单分页
export const getShipmentPage = (params: ShipmentPageParam) => {
  return request.get({ url: '/lunaerp/business/shipment/page', params })
}

// 查询发货单详情
export const getShipment = (id: number) => {
  return request.get<ShipmentVO>({ url: '/lunaerp/business/shipment/get?id=' + id })
}

// 预览下一个发货单号（只读、不占号）
export const getNextShipmentNo = (shippingDate?: string) => {
  return request.get<string>({
    url: '/lunaerp/business/shipment/get-next-no',
    params: shippingDate ? { shippingDate } : undefined
  })
}

// 新增发货单
export const createShipment = (data: ShipmentVO) => {
  return request.post<number>({ url: '/lunaerp/business/shipment/create', data })
}

// 修改发货单
export const updateShipment = (data: ShipmentVO) => {
  return request.put({ url: '/lunaerp/business/shipment/update', data })
}

// 删除发货单
export const deleteShipment = (id: number) => {
  return request.delete({ url: '/lunaerp/business/shipment/delete?id=' + id })
}

// 审核发货单（DRAFT -> COMPLETED，同事务写负数库存流水）
export const auditShipment = (id: number) => {
  return request.put({ url: '/lunaerp/business/shipment/audit?id=' + id })
}

// 取消发货单（草稿作废 / 已发货红冲）
export const cancelShipment = (id: number) => {
  return request.put({ url: '/lunaerp/business/shipment/cancel?id=' + id })
}

// 登记/修改发货单签收信息（仅 COMPLETED 单据可登记）
export const updateShipmentSign = (data: ShipmentUpdateSignReqVO) => {
  return request.put({ url: '/lunaerp/business/shipment/update-sign', data })
}

// 从销售合同带入发货明细（实时计算未发量）
export const getShipmentItemsFromContract = (contractId: number) => {
  return request.get<ShipmentItemVO[]>({
    url: '/lunaerp/business/shipment/from-contract',
    params: { contractId }
  })
}

// 发货单/装箱清单独立打印 VO（豁免脱敏）
export interface ShipmentPrintVO extends ShipmentVO {
  qrUrl?: string
  printTime?: string
}

// 获得发货单/装箱清单独立打印数据
export const getShipmentPrintData = (id: string | number) => {
  return request.get<ShipmentPrintVO>({
    url: `/lunaerp/business/shipment/${id}/print-data`
  })
}


