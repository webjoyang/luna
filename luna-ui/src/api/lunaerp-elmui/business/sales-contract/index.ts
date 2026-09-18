import request from '@/config/axios'

// 销售合同 VO（本票仅基本信息，不含商品明细）
export interface SalesContractVO {
  id?: number
  contractNo?: string
  customerId: number
  clientName?: string
  clientContact?: string
  clientAddress?: string
  clientContractNo?: string
  salespersonUserId?: number
  signDate?: string
  deliveryDate?: string
  unit?: string
  currency?: string
  tradeType?: string
  orderType?: string
  totalQuantity?: number
  totalAmount?: number
  remarks?: string
  status?: string
  createTime?: Date | string
}

// 销售合同分页查询参数
export interface SalesContractPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
  tradeType?: string
  customerId?: number
}

// 查询销售合同分页
export const getSalesContractPage = (params: SalesContractPageParam) => {
  return request.get({ url: '/lunaerp/business/sales-contract/page', params })
}

// 查询销售合同详情
export const getSalesContract = (id: number) => {
  return request.get({ url: '/lunaerp/business/sales-contract/get?id=' + id })
}

// 新增销售合同（contractNo 留空时后端自动生成）
export const createSalesContract = (data: SalesContractVO) => {
  return request.post<number>({ url: '/lunaerp/business/sales-contract/create', data })
}

// 修改销售合同
export const updateSalesContract = (data: SalesContractVO) => {
  return request.put({ url: '/lunaerp/business/sales-contract/update', data })
}

// 删除销售合同
export const deleteSalesContract = (id: number) => {
  return request.delete({ url: '/lunaerp/business/sales-contract/delete?id=' + id })
}
