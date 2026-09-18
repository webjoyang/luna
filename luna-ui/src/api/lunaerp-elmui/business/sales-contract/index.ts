import request from '@/config/axios'

// 销售合同明细 VO（作为合同聚合的一部分，随合同整体保存，无独立端点，见 ADR-0003）
export interface SalesContractItemVO {
  id?: number
  contractId?: number
  lineNo?: number
  productCode?: string
  productName?: string
  customerColor?: string
  composition?: string
  width?: string
  weight?: string
  quantity?: number
  unitPrice?: number
  otherFees?: number
  amount?: number // 行金额，服务端重算：数量 × 单价 + 其他费用
  remark?: string
}

// 销售合同 VO
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
  totalQuantity?: number // 服务端按明细权威重算，前端传入值会被忽略覆盖
  totalAmount?: number // 服务端按明细权威重算，前端传入值会被忽略覆盖
  remarks?: string
  status?: string
  items?: SalesContractItemVO[] // 商品明细列表（整体替换；为空表示零明细合同）
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

// 预览当前租户当年下一个合同编号（只读不占号，多次调用在无新建时返回相同值）
export const getNextSalesContractNo = () => {
  return request.get<string>({ url: '/lunaerp/business/sales-contract/get-next-no' })
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
