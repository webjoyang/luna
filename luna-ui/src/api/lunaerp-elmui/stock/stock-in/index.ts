import request from '@/config/axios'

// 库位分配放置记录 VO
export interface StockInItemPlacementVO {
  id?: number
  stockInId?: number
  stockInItemId?: number
  locationId?: number
  locationCode?: string
  pieceCount?: number
  quantity?: number
  unit?: string
  remark?: string
  createTime?: Date | string
  _clientKey?: string
}

// 入库明细 VO
export interface StockInItemVO {
  id?: number
  stockInId?: number
  itemType?: string // 'PRODUCT' | 'YARN_SPEC'
  productId?: number
  productCode?: string
  productName?: string
  yarnSpecId?: number
  yarnSpecName?: string
  batchNo?: string
  rollNo?: string
  pieceNo?: string
  weavingTicketId?: number
  inspectionOrderItemId?: number
  pieceCount?: number
  quantity: number
  unit: string
  unitPrice?: number
  amount?: number
  remark?: string
  createTime?: Date | string
  placements?: StockInItemPlacementVO[]
  specCascadeValue?: [number, number]
  _clientKey?: string
}

// 入库单 VO
export interface StockInVO {
  id?: number
  inNo?: string
  inType: string
  status?: string
  warehouseId: number
  warehouseName?: string
  supplierId?: number
  supplierName?: string
  salesContractId?: number
  contractNo?: string
  greigeOrderId?: number
  greigeOrderNo?: string
  dyeingOrderId?: number
  greigeTypeId?: number
  greigeTypeName?: string
  inspectionOrderId?: number
  inspectionOrderNo?: string
  totalAmount?: number
  inDate: string
  remark?: string
  creator?: string
  createTime?: Date | string
  updater?: string
  updateTime?: Date | string
  items?: StockInItemVO[]
}

// 入库单分页查询参数
export interface StockInPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  inType?: string
  status?: string
  warehouseId?: number
  supplierId?: number
  inDate?: string[]
  inDateStart?: string
  inDateEnd?: string
}

// 查询入库单分页
export const getStockInPage = (params: StockInPageParam) => {
  return request.get({ url: '/lunaerp/stock/stock-in/page', params })
}

// 查询入库单详情
export const getStockIn = (id: number) => {
  return request.get<StockInVO>({ url: '/lunaerp/stock/stock-in/get?id=' + id })
}

// 预览下一个入库单号（只读、不占号）
export const getNextInNo = (inDate?: string) => {
  return request.get<string>({
    url: '/lunaerp/stock/stock-in/get-next-no',
    params: inDate ? { inDate } : undefined
  })
}

// 新增入库单
export const createStockIn = (data: StockInVO) => {
  return request.post<number>({ url: '/lunaerp/stock/stock-in/create', data })
}

// 修改入库单
export const updateStockIn = (data: StockInVO) => {
  return request.put({ url: '/lunaerp/stock/stock-in/update', data })
}

// 删除入库单
export const deleteStockIn = (id: number) => {
  return request.delete({ url: '/lunaerp/stock/stock-in/delete?id=' + id })
}

// 审核入库单（DRAFT -> COMPLETED，同事务写正数库存流水）
export const auditStockIn = (id: number) => {
  return request.put({ url: '/lunaerp/stock/stock-in/audit?id=' + id })
}

// 取消入库单（草稿作废 / 已审核红冲）
export const cancelStockIn = (id: number) => {
  return request.put({ url: '/lunaerp/stock/stock-in/cancel?id=' + id })
}

