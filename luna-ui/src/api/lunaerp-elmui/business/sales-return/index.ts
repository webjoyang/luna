import request from '@/config/axios'

// 销售退货可选发货单候选 VO
export interface SalesReturnCandidateVO {
  id: number
  shipmentNo: string
  customerId: number
  customerName: string
  salesContractId?: number
  contractNo?: string
  warehouseId: number
  warehouseName: string
  currency: string
  shippingDate: string
  totalAmount: number
  totalPieces: number
}

// 销售退货原发货卷清单 VO
export interface SalesReturnShipmentRollVO {
  shipmentRollId: number
  rollNo: string
  weight?: number
  lengthM?: number
  remark?: string
}

// 销售退货原发货明细行带可退量 VO
export interface SalesReturnShipmentItemVO {
  shipmentItemId: number
  productId: number
  productCode: string
  productName: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  unit: string
  quantity: number
  pieces?: number
  unitPrice?: number
  occupiedQty: number
  availableQty: number
  rolls?: SalesReturnShipmentRollVO[]
}

// 销售退货发起制单取数详情 VO
export interface SalesReturnShipmentDetailVO {
  shipmentId: number
  shipmentNo: string
  customerId: number
  customerName: string
  salesContractId?: number
  contractNo?: string
  warehouseId: number
  warehouseName: string
  currency: string
  shippingDate: string
  items: SalesReturnShipmentItemVO[]
}

// 销售退货卷保存 VO
export interface SalesReturnRollSaveReqVO {
  id?: number
  shipmentRollId?: number
  rollNo: string
  weight?: number
  lengthM?: number
  remark?: string
}

// 销售退货明细行保存 VO
export interface SalesReturnItemSaveReqVO {
  id?: number
  shipmentItemId: number
  returnQty: number
  pieces?: number
  unitPrice?: number
  returnReason: string
  remark?: string
  rolls: SalesReturnRollSaveReqVO[]
}

// 销售退货单创建 VO
export interface SalesReturnCreateReqVO {
  shipmentId: number
  warehouseId: number
  returnDate?: string
  remark?: string
  items: SalesReturnItemSaveReqVO[]
}

// 销售退货单修改 VO（coding-standards.md §14 属性极小集，无不可变发货单ID）
export interface SalesReturnUpdateReqVO {
  id: number
  warehouseId: number
  returnDate?: string
  remark?: string
  items: SalesReturnItemSaveReqVO[]
}

// 销售退货卷展示 VO
export interface SalesReturnRollVO {
  id?: number
  returnId?: number
  returnItemId?: number
  shipmentRollId?: number
  rollNo: string
  weight?: number
  lengthM?: number
  qcResult?: string
  qcBy?: number
  qcAt?: string
  qcRemark?: string
  locationId?: number
  locationCode?: string
  remark?: string
}

// 销售退货明细行展示 VO
export interface SalesReturnItemVO {
  id?: number
  returnId?: number
  shipmentItemId: number
  productId: number
  productCode: string
  productName: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  returnQty: number
  pieces?: number
  unit: string
  unitPrice?: number
  amount?: number
  returnReason: string
  restockQty?: number
  scrapQty?: number
  remark?: string
  rolls?: SalesReturnRollVO[]
}

// 销售退货单展示 VO
export interface SalesReturnVO {
  id?: number
  returnNo?: string
  status?: string
  approvalStatus?: string
  shipmentId: number
  shipmentNo: string
  customerId: number
  customerName: string
  salesContractId?: number
  contractNo?: string
  warehouseId: number
  warehouseName?: string
  currency: string
  totalAmount?: number
  totalPieces?: number
  stockInId?: number
  stockInNo?: string
  stockInStatus?: string
  arrivedBy?: number
  arrivedAt?: string
  postedBy?: number
  postedAt?: string
  cancelledBy?: number
  cancelledAt?: string
  cancelReason?: string
  returnDate?: string
  remark?: string
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
  items?: SalesReturnItemVO[]
}

// 销售退货分页查询参数
export interface SalesReturnPageParam {
  pageNo?: number
  pageSize?: number
  customerId?: number
  shipmentNo?: string
  status?: string
  approvalStatus?: string
  keyword?: string
  returnDate?: string[]
}

// 查询可选发货单候选列表（仅 COMPLETED 且含实时可退量>0 行）
export const getShipmentCandidates = (params?: { customerId?: number; shipmentNo?: string }) => {
  return request.get<SalesReturnCandidateVO[]>({
    url: '/lunaerp/business/sales-return/shipment-candidates',
    params
  })
}

// 发起退货取数：获得发货单快照、明细行带可退量及卷清单
export const getShipmentDetail = (shipmentId: number) => {
  return request.get<SalesReturnShipmentDetailVO>({
    url: '/lunaerp/business/sales-return/shipment-detail',
    params: { shipmentId }
  })
}

// 创建销售退货草稿
export const createSalesReturn = (data: SalesReturnCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/business/sales-return/create',
    data
  })
}

// 修改销售退货草稿
export const updateSalesReturn = (data: SalesReturnUpdateReqVO) => {
  return request.put({
    url: '/lunaerp/business/sales-return/update',
    data
  })
}

// 删除销售退货草稿
export const deleteSalesReturn = (id: number) => {
  return request.delete({
    url: '/lunaerp/business/sales-return/delete?id=' + id
  })
}

// 获得销售退货单详情
export const getSalesReturn = (id: number) => {
  return request.get<SalesReturnVO>({
    url: '/lunaerp/business/sales-return/get?id=' + id
  })
}

// 销售退货单作废 VO
export interface SalesReturnCancelReqVO {
  cancelReason: string
}

// 获得销售退货单分页
export const getSalesReturnPage = (params: SalesReturnPageParam) => {
  return request.get({
    url: '/lunaerp/business/sales-return/page',
    params
  })
}

// 提交销售退货单审批
export const submitSalesReturn = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/business/sales-return/submit?id=${id}`
  })
}

// 撤回销售退货单审批
export const withdrawSalesReturn = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/business/sales-return/withdraw?id=${id}`
  })
}

// 作废销售退货单（草稿分支）
export const cancelSalesReturn = (id: number, data: SalesReturnCancelReqVO) => {
  return request.post<boolean>({
    url: `/lunaerp/business/sales-return/cancel?id=${id}`,
    data
  })
}

// 销售退货质检卷参数 VO
export interface SalesReturnReceiveRollReqVO {
  id: number
  qcResult: string
  locationId?: number
  qcRemark?: string
}

// 销售退货到货质检参数 VO
export interface SalesReturnReceiveReqVO {
  id: number
  rolls: SalesReturnReceiveRollReqVO[]
}

// 到货质检
export const receiveSalesReturn = (data: SalesReturnReceiveReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/business/sales-return/receive',
    data
  })
}

// 财务确认（过账终态）
export const postSalesReturn = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/business/sales-return/post?id=${id}`
  })
}

// 销售退货打印卷 VO
export interface SalesReturnPrintRollVO {
  id?: number
  returnId?: number
  returnItemId?: number
  shipmentRollId?: number
  rollNo: string
  weight?: number
  lengthM?: number
  qcResult?: string
  qcRemark?: string
  locationId?: number
  locationCode?: string
  remark?: string
}

// 销售退货打印明细行 VO
export interface SalesReturnPrintItemVO {
  id?: number
  returnId?: number
  shipmentItemId: number
  productId: number
  productCode: string
  productName: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  returnQty: number
  pieces?: number
  unit: string
  unitPrice?: number
  amount?: number
  returnReason: string
  restockQty?: number
  scrapQty?: number
  remark?: string
  rolls?: SalesReturnPrintRollVO[]
}

// 销售退货打印全量数据 VO
export interface SalesReturnPrintVO {
  id: number
  returnNo: string
  status: string
  approvalStatus?: string
  shipmentId: number
  shipmentNo: string
  customerId: number
  customerName: string
  contactName?: string
  contactPhone?: string
  customerAddress?: string
  salesContractId?: number
  contractNo?: string
  warehouseId: number
  warehouseName?: string
  currency: string
  totalAmount?: number
  totalPieces?: number
  stockInId?: number
  stockInNo?: string
  stockInStatus?: string
  arrivedBy?: number
  arrivedAt?: string
  postedBy?: number
  postedAt?: string
  returnDate?: string
  remark?: string
  creator?: string
  createTime?: string
  printTime?: string
  items: SalesReturnPrintItemVO[]
}

// 获取销售退货打印数据
export const getSalesReturnPrintData = (id: number) => {
  return request.get<SalesReturnPrintVO>({
    url: `/lunaerp/business/sales-return/print-data?id=${id}`
  })
}
