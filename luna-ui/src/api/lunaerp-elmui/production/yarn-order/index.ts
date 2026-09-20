import request from '@/config/axios'

// 纱线采购单明细 VO
export interface YarnOrderItemVO {
  id?: number
  orderId?: number
  yarnTypeId: number
  yarnTypeName?: string
  yarnSpecId: number
  yarnSpecName?: string
  supplierId?: number
  supplierName?: string
  ratio?: number
  lossRate?: number
  baseQty?: number
  requiredQty?: number
  stockQty?: number
  purchaseQty?: number
  unitPrice?: number
  totalAmount?: number
  refUnitPrice?: number
  remark?: string
}

// 纱线采购单明细保存 Req VO
export interface YarnOrderItemSaveReqVO {
  id?: number
  yarnTypeId: number
  yarnSpecId: number
  supplierId?: number
  supplierName?: string
  ratio?: number
  lossRate?: number
  baseQty?: number
  requiredQty?: number
  stockQty?: number
  purchaseQty?: number
  unitPrice?: number
  totalAmount?: number
  refUnitPrice?: number
  remark?: string
}

// 纱线采购单保存 Req VO
export interface YarnOrderSaveReqVO {
  id?: number
  orderNo?: string
  sourceInstructions?: string
  orderDate?: string
  deliveryDate?: string
  lossRate?: number
  status?: string
  remark?: string
  items?: YarnOrderItemSaveReqVO[]
}

// 纱线采购单分页 Req VO
export interface YarnOrderPageReqVO extends PageParam {
  keyword?: string
  status?: string
  orderDateStart?: string
  orderDateEnd?: string
  deliveryDateStart?: string
  deliveryDateEnd?: string
  orderDate?: string[]
  deliveryDate?: string[]
}

// 关联胚布单展示快照 VO
export interface YarnOrderGreigeOrderSnapshotVO {
  id: number
  orderNo: string
  productId: number
  productCode?: string
  productName?: string
  finishedQty?: number
  suggestedLoss?: number
}

// 纱线采购单 Resp VO
export interface YarnOrderVO {
  id: number
  orderNo: string
  sourceInstructions?: string
  orderDate: string
  deliveryDate?: string
  lossRate?: number
  status: string
  remark?: string
  creator?: string
  createTime?: string
  items?: YarnOrderItemVO[]
  sourceGreigeOrders?: YarnOrderGreigeOrderSnapshotVO[]
  sourceGreigeOrderNos?: string
  itemCount?: number
  totalAmount?: number
}

// 核心测算 Req VO
export interface YarnOrderCalculateReqVO {
  greigeOrderIds: number[]
  lossRate?: number
}

// 核心测算明细行 VO
export interface YarnOrderCalculateItemVO {
  yarnTypeId: number
  yarnTypeName?: string
  yarnSpecId: number
  yarnSpecName?: string
  supplierId?: number
  supplierName?: string
  ratio?: number
  baseQty?: number
  lossRate?: number
  requiredQty?: number
  stockQty?: number
  purchaseQty?: number
  refUnitPrice?: number
  sourceGreigeOrderIds?: number[]
  sourceGreigeOrderNos?: string[]
}

// 核心测算 Resp VO
export interface YarnOrderCalculateRespVO {
  items: YarnOrderCalculateItemVO[]
  sourceGreigeOrders: YarnOrderGreigeOrderSnapshotVO[]
}

// 获得采购单分页
export const getYarnOrderPage = (params: YarnOrderPageReqVO) => {
  return request.get({ url: '/admin-api/lunaerp/production/yarn-order/page', params })
}

// 获得采购单详情
export const getYarnOrder = (id: number) => {
  return request.get({ url: `/admin-api/lunaerp/production/yarn-order/get?id=${id}` })
}

// 创建采购单
export const createYarnOrder = (data: YarnOrderSaveReqVO) => {
  return request.post({ url: '/admin-api/lunaerp/production/yarn-order/create', data })
}

// 更新采购单
export const updateYarnOrder = (data: YarnOrderSaveReqVO) => {
  return request.put({ url: '/admin-api/lunaerp/production/yarn-order/update', data })
}

// 删除采购单
export const deleteYarnOrder = (id: number) => {
  return request.delete({ url: `/admin-api/lunaerp/production/yarn-order/delete?id=${id}` })
}

// 下达采购单
export const effectYarnOrder = (id: number) => {
  return request.put({ url: `/admin-api/lunaerp/production/yarn-order/effect?id=${id}` })
}

// 完成采购单
export const completeYarnOrder = (id: number) => {
  return request.put({ url: `/admin-api/lunaerp/production/yarn-order/complete?id=${id}` })
}

// 取消采购单
export const cancelYarnOrder = (id: number) => {
  return request.put({ url: `/admin-api/lunaerp/production/yarn-order/cancel?id=${id}` })
}

// 核心测算
export const calculateYarnOrder = (data: YarnOrderCalculateReqVO) => {
  return request.post({ url: '/admin-api/lunaerp/production/yarn-order/calculate', data })
}

// 预览下一个采购单号
export const getNextYarnOrderNo = () => {
  return request.get({ url: '/admin-api/lunaerp/production/yarn-order/get-next-no' })
}
