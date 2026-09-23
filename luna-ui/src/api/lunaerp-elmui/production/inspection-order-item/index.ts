import request from '@/config/axios'

// 录检明细 VO
export interface InspectionOrderItemVO {
  id?: number
  inspectionOrderId: number
  rollNo: string
  vatNo: string
  measuredWeight?: number
  lengthM?: number
  machineNo?: string
  grade?: string // A / B / C / DEFECTIVE
  colorName?: string
  colorNo?: string
  styleNo?: string
  productCode?: string
  productName?: string
  pieceCount?: number
  remark?: string
  createTime?: Date | string
  updateTime?: Date | string
}

// 录检明细分页查询参数
export interface InspectionOrderItemPageParam {
  pageNo?: number
  pageSize?: number
  inspectionOrderId: number
}

// 录检明细保存参数（创建/修改）
export interface InspectionOrderItemSaveReqVO {
  id?: number
  inspectionOrderId: number
  rollNo: string
  vatNo: string
  measuredWeight?: number
  lengthM?: number
  machineNo?: string
  grade?: string
  colorName?: string
  colorNo?: string
  styleNo?: string
  productCode?: string
  productName?: string
  pieceCount?: number
  remark?: string
}

// 查询录检明细分页（按 inspectionOrderId 过滤，按 id 倒序）
export const getInspectionOrderItemPage = (params: InspectionOrderItemPageParam) => {
  return request.get({ url: '/lunaerp/production/inspection-order-item/page', params })
}

// 查询录检明细详情
export const getInspectionOrderItem = (id: number) => {
  return request.get<InspectionOrderItemVO>({ url: `/lunaerp/production/inspection-order-item/get?id=${id}` })
}

// 创建录检明细
export const createInspectionOrderItem = (data: InspectionOrderItemSaveReqVO) => {
  return request.post<number>({ url: '/lunaerp/production/inspection-order-item/create', data })
}

// 修改录检明细
export const updateInspectionOrderItem = (data: InspectionOrderItemSaveReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/inspection-order-item/update', data })
}

// 删除录检明细
export const deleteInspectionOrderItem = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/inspection-order-item/delete?id=${id}` })
}
