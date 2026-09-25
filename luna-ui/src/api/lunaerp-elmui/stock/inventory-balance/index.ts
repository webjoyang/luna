import request from '@/config/axios'

// 库存余额 VO
export interface InventoryBalanceVO {
  productId: number
  productCode?: string
  productName?: string
  warehouseId: number
  warehouseName?: string
  unit: string
  quantity: number
}

// 库存余额分页查询参数
export interface InventoryBalancePageParam {
  pageNo?: number
  pageSize?: number
  productKeyword?: string
  warehouseId?: number
}

// 查询库存余额分页
export const getInventoryBalancePage = (params: InventoryBalancePageParam) => {
  return request.get({ url: '/lunaerp/stock/inventory-balance/page', params })
}
