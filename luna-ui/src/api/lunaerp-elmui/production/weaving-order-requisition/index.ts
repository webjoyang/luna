import request from '@/config/axios'

// 领纱单 VO
export interface YarnRequisitionVO {
  id?: number
  code?: string // 领纱单号（YS-{织造单号去符号}-{2位流水}）
  weavingOrderId?: number // 关联织造单ID
  weavingOrderCode?: string // 织造单号快照
  productId?: number // 产品ID
  productCode?: string // 产品编码快照
  productName?: string // 产品名称快照
  plannedQty?: number // 计划匹数
  plannedWeight?: number // 计划重量 KG
  status?: string // 状态：DRAFT 草稿 / ACTIVE 待发纱 / PARTIAL 部分发纱 / DISPATCHED 已发纱 / CLOSED 已关闭
  remark?: string
  itemCount?: number
  totalRequiredQty?: number
  totalDispatchedQty?: number
  items?: YarnRequisitionItemVO[]
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

// 领纱单保存请求 VO
export interface YarnRequisitionSaveReqVO {
  id?: number
  code?: string
  weavingOrderId?: number
  remark?: string
}

// 领纱单分页查询参数
export interface YarnRequisitionPageParam {
  pageNo?: number
  pageSize?: number
  weavingOrderId?: number
  code?: string
  status?: string
  createTime?: string[]
}

// 领纱明细 VO（独立子资源）
export interface YarnRequisitionItemVO {
  id?: number
  requisitionId?: number
  yarnTypeId?: number
  yarnTypeName?: string
  yarnSpecId?: number
  yarnSpecName?: string
  requiredQty?: number
  dispatchedQty?: number
  unit?: string
  remark?: string
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

// 领纱明细保存请求 VO
export interface YarnRequisitionItemSaveReqVO {
  id?: number
  requisitionId?: number
  yarnTypeId?: number
  yarnSpecId?: number
  yarnSpecName?: string
  requiredQty?: number
  unit?: string
  remark?: string
}

// 领纱明细分页参数
export interface YarnRequisitionItemPageParam {
  pageNo?: number
  pageSize?: number
  requisitionId?: number
  yarnTypeId?: number
  yarnSpecId?: number
}

// BOM 测算请求 VO
export interface YarnRequisitionCalculateReqVO {
  weavingOrderId: number
}

// BOM 测算响应明细行
export interface YarnRequisitionCalculateItemRespVO {
  yarnTypeId?: number
  yarnTypeName?: string
  yarnSpecId?: number
  yarnSpecName?: string
  ratio?: number
  requiredQty?: number
  unit?: string
  remark?: string
}

// BOM 测算响应 VO
export interface YarnRequisitionCalculateRespVO {
  weavingOrderId: number
  weavingOrderCode?: string
  productId?: number
  productCode?: string
  productName?: string
  plannedQty?: number
  plannedWeight?: number
  piecePlannedWeight?: number
  tip?: string
  items: YarnRequisitionCalculateItemRespVO[]
}

// ==================== 领纱单主表 API ====================

// 查询领纱单分页
export const getYarnRequisitionPage = (params: YarnRequisitionPageParam) => {
  return request.get({ url: '/lunaerp/production/weaving-order-requisition/page', params })
}

// 获得领纱单详情（含明细）
export const getYarnRequisition = (id: number) => {
  return request.get<YarnRequisitionVO>({ url: `/lunaerp/production/weaving-order-requisition/get?id=${id}` })
}

// 预览下一个领纱单号（只读、不占号）
export const getNextYarnRequisitionCode = (weavingOrderId: number) => {
  return request.get<string>({
    url: `/lunaerp/production/weaving-order-requisition/get-next-code?weavingOrderId=${weavingOrderId}`
  })
}

// 新建领纱单
export const createYarnRequisition = (data: YarnRequisitionSaveReqVO) => {
  return request.post<number>({ url: '/lunaerp/production/weaving-order-requisition/create', data })
}

// 修改领纱单（仅草稿态可改）
export const updateYarnRequisition = (data: YarnRequisitionSaveReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-requisition/update', data })
}

// 删除领纱单（仅草稿态可删）
export const deleteYarnRequisition = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/weaving-order-requisition/delete?id=${id}` })
}

// 提交领纱单（DRAFT → ACTIVE）
export const submitYarnRequisition = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/production/weaving-order-requisition/submit?id=${id}` })
}

// 关闭领纱单（转 CLOSED 手动终态）
export const closeYarnRequisition = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/production/weaving-order-requisition/close?id=${id}` })
}

// 领纱用量 BOM 权威测算接口
export const calculateYarnRequisition = (data: YarnRequisitionCalculateReqVO) => {
  return request.post<YarnRequisitionCalculateRespVO>({
    url: '/lunaerp/production/weaving-order-requisition/calculate',
    data
  })
}

// ==================== 领纱明细（独立子资源）API ====================

// 查询领纱明细分页
export const getYarnRequisitionItemPage = (params: YarnRequisitionItemPageParam) => {
  return request.get({ url: '/lunaerp/production/weaving-order-requisition-item/page', params })
}

// 获得领纱明细详情
export const getYarnRequisitionItem = (id: number) => {
  return request.get<YarnRequisitionItemVO>({ url: `/lunaerp/production/weaving-order-requisition-item/get?id=${id}` })
}

// 新建领纱明细
export const createYarnRequisitionItem = (data: YarnRequisitionItemSaveReqVO) => {
  return request.post<number>({ url: '/lunaerp/production/weaving-order-requisition-item/create', data })
}

// 修改领纱明细
export const updateYarnRequisitionItem = (data: YarnRequisitionItemSaveReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-requisition-item/update', data })
}

// 删除领纱明细
export const deleteYarnRequisitionItem = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/weaving-order-requisition-item/delete?id=${id}` })
}

// ==================== 发纱与撤销红冲（Issue 10）API ====================

// 发纱请求 VO
export interface YarnRequisitionDispatchReqVO {
  itemId: number
  warehouseId: number
  locationId?: number
  dispatchQty: number
  remark?: string
}

// 撤销发纱（红冲）请求 VO
export interface YarnRequisitionDispatchReversalReqVO {
  stockLedgerId: number
  remark?: string
}

// 发纱记录 VO
export interface YarnDispatchRecordRespVO {
  id: number
  itemId?: number
  yarnSpecId?: number
  yarnSpecName?: string
  warehouseId?: number
  warehouseName?: string
  locationId?: number
  locationCode?: string
  quantity?: number
  unit?: string
  txnType?: string
  reversed?: boolean
  reversalTime?: string
  createTime?: string
  creator?: string
  remark?: string
  relatedDocNo?: string
}

// 领纱明细发纱（同事务 YARN_OUT + dispatchedQty + 状态推导）
export const dispatchYarn = (data: YarnRequisitionDispatchReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/production/weaving-order-requisition-item/dispatch',
    data
  })
}

// 撤销发纱（按发纱记录红冲 YARN_OUT_REVERSAL + 回减 dispatchedQty + 状态回退）
export const reverseDispatch = (data: YarnRequisitionDispatchReversalReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/production/weaving-order-requisition-item/reverse-dispatch',
    data
  })
}

// 获取领纱明细发纱记录列表
export const getDispatchRecordList = (itemId: number) => {
  return request.get<YarnDispatchRecordRespVO[]>({
    url: `/lunaerp/production/weaving-order-requisition-item/list-dispatch-records?itemId=${itemId}`
  })
}

// 获取某仓库下某纱线规格的可用库存余额
export const getYarnBalance = (warehouseId: number, yarnSpecId: number) => {
  return request.get<number>({
    url: `/lunaerp/production/weaving-order-requisition-item/get-yarn-balance?warehouseId=${warehouseId}&yarnSpecId=${yarnSpecId}`
  })
}

