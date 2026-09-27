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
  customerId?: number
  warehouseId?: number
  contractNo?: string
  shippingDate?: string[]
  shippingDateStart?: string
  shippingDateEnd?: string
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
