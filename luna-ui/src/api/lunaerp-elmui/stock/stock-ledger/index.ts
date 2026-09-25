import request from '@/config/axios'

// 库存流水台账 VO
export interface StockLedgerVO {
  id: number
  productId: number
  productCode?: string
  productName?: string
  warehouseId: number
  warehouseName?: string
  locationId?: number
  locationCode?: string
  batchNo?: string
  txnType: string
  quantity: number
  unit: string
  unitPrice?: number
  amount?: number
  relatedDocType: string
  relatedDocId: number
  relatedDocNo?: string
  remark?: string
  creator?: string
  createTime: Date | string
}

// 库存流水台账分页查询参数（与 Spec line 275 对齐）
export interface StockLedgerPageParam {
  pageNo?: number
  pageSize?: number
  productKeyword?: string
  warehouseId?: number
  locationId?: number
  txnType?: string
  relatedDocNo?: string
  createTime?: string[]
}

// 查询库存流水台账分页（只读）
export const getStockLedgerPage = (params: StockLedgerPageParam) => {
  return request.get({ url: '/lunaerp/stock/stock-ledger/page', params })
}
