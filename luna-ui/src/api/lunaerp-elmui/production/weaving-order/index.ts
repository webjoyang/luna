import request from '@/config/axios'

// 织造单 VO
export interface WeavingOrderVO {
  id?: number
  code?: string // 织造单号（ZZ-{胚布单号去符号}-{2位序号}）
  greigeOrderId?: number // 关联自织胚布单ID（必填）
  greigeOrderNo?: string // 胚布单号快照
  contractNo?: string // 销售合同号快照
  productId?: number // 关联产品档案ID
  productCode?: string // 产品编号快照
  productName?: string // 产品名称快照
  composition?: string // 成分
  gsm?: string // 克重
  width?: string // 门幅
  plannedQty?: number // 计划匹数（正整数）
  plannedWeight?: number // 计划总重量 KG
  completedQty?: number // 完工匹数（只读）
  status?: string // 状态：DRAFT 待排产 / ACTIVE 生产中 / COMPLETED 已完工 / CANCELLED 已取消
  warning?: string // 超量软警告信息
  remark?: string
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

// 织造单新建/修改请求 VO
export interface WeavingOrderSaveReqVO {
  id?: number
  code?: string
  greigeOrderId?: number
  plannedQty?: number
  plannedWeight?: number
  composition?: string
  gsm?: string
  width?: string
  remark?: string
}

// 织造单保存响应 VO
export interface WeavingOrderSaveRespVO {
  id: number
  code: string
  warning?: string
}

// 织造单分页查询参数
export interface WeavingOrderPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  greigeOrderId?: number
  status?: string
  createTime?: string[]
}

// 查询织造单分页
export const getWeavingOrderPage = (params: WeavingOrderPageParam) => {
  return request.get({ url: '/lunaerp/production/weaving-order/page', params })
}

// 获得织造单详情
export const getWeavingOrder = (id: number) => {
  return request.get<WeavingOrderVO>({ url: `/lunaerp/production/weaving-order/get?id=${id}` })
}

// 预览下一个织造单号（只读、不占号）
export const getNextWeavingOrderCode = (greigeOrderId: number) => {
  return request.get<string>({
    url: `/lunaerp/production/weaving-order/get-next-code?greigeOrderId=${greigeOrderId}`
  })
}

// 新建织造单（code 留空自动派生，超量软警告）
export const createWeavingOrder = (data: WeavingOrderSaveReqVO) => {
  return request.post<WeavingOrderSaveRespVO>({ url: '/lunaerp/production/weaving-order/create', data })
}

// 修改织造单（仅待排产 DRAFT 态允许修改）
export const updateWeavingOrder = (data: WeavingOrderSaveReqVO) => {
  return request.put<WeavingOrderSaveRespVO>({ url: '/lunaerp/production/weaving-order/update', data })
}

// 删除织造单（仅待排产 DRAFT 态允许删除，级联逻辑删除下属任务与布票）
export const deleteWeavingOrder = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/weaving-order/delete?id=${id}` })
}

// 取消织造单（手动终态）
export const cancelWeavingOrder = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/production/weaving-order/cancel?id=${id}` })
}
