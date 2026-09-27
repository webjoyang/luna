import request from '@/config/axios'

// 检验单 VO
export interface InspectionOrderVO {
  id?: number
  code?: string
  type: string // GREIGE 胚布检验 / FINISHED 成品检验
  status?: string // DRAFT / ACTIVE / COMPLETED / CANCELLED
  packageMode?: string // ROLL 卷装 / PIECE 匹装
  productId: number
  productCode?: string
  productName?: string
  greigeOrderId?: number
  dyeingOrderId?: number
  inspectionFactoryId?: number
  inspectionFactoryName?: string
  salesContractId?: number
  contractNo?: string
  targetWeight?: number
  remark?: string
  orderDate: string
  rollCount?: number
  creator?: string
  createTime?: Date | string
  updater?: string
  updateTime?: Date | string
}

// 检验单分页查询参数
export interface InspectionOrderPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  type?: string
  status?: string
  packageMode?: string
  inspectionFactoryId?: number
  orderDateStart?: string
  orderDateEnd?: string
}

// 查询检验单分页
export const getInspectionOrderPage = (params: InspectionOrderPageParam) => {
  return request.get({ url: '/lunaerp/production/inspection-order/page', params })
}

// 查询检验单详情
export const getInspectionOrder = (id: number) => {
  return request.get<InspectionOrderVO>({ url: `/lunaerp/production/inspection-order/get?id=${id}` })
}

// 预览下一个检验单号（只读、不占号）
export const getNextInspectionOrderNo = () => {
  return request.get<string>({ url: '/lunaerp/production/inspection-order/get-next-no' })
}

// 创建检验单
export const createInspectionOrder = (data: InspectionOrderVO) => {
  return request.post<number>({ url: '/lunaerp/production/inspection-order/create', data })
}

// 修改检验单
export const updateInspectionOrder = (data: InspectionOrderVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/inspection-order/update', data })
}

// 删除检验单
export const deleteInspectionOrder = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/inspection-order/delete?id=${id}` })
}

// 完成检验单（DRAFT/ACTIVE -> COMPLETED）
export const completeInspectionOrder = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/production/inspection-order/complete?id=${id}` })
}

// 取消检验单（任意态 -> CANCELLED）
export const cancelInspectionOrder = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/production/inspection-order/cancel?id=${id}` })
}

// 检验单生成入库单请求 VO
export interface InspectionOrderGenerateStockInReqVO {
  inspectionOrderId: number
  warehouseId: number
  remark?: string
}

// 检验单生成入库单响应 VO
export interface InspectionOrderGenerateStockInRespVO {
  stockInId: number
  stockInNo: string
  generatedCount: number
  skippedCount: number
}

// 检验单生成胚布入库单草稿
export const generateStockIn = (data: InspectionOrderGenerateStockInReqVO) => {
  return request.post<InspectionOrderGenerateStockInRespVO>({
    url: '/lunaerp/production/inspection-order/generate-stock-in',
    data
  })
}
