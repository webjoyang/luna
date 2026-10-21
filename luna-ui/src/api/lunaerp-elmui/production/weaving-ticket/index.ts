import request from '@/config/axios'

// 布票 VO
export interface WeavingTicketVO {
  id?: number
  code?: string // 布票编号（BT-{机台任务号去符号}-{4 位流水}）
  weavingOrderId?: number // 关联织造单ID
  machineTaskId?: number // 关联机台任务ID
  plannedMachine?: string // 预排机台号快照
  actualMachine?: string // 实际上机机台号
  plannedWeight?: number // 单匹计划重量 KG
  workerUserId?: number // 挡车工用户ID
  workerName?: string // 挡车工姓名快照
  materialBatchNo?: string // 原料批号
  status?: string // 状态：GENERATED / STARTED / COMPLETED / INSPECTED / STOCKED / EXCEPTION / VOID
  exceptionType?: string // 异常类型：YARN_BREAK / CLOTH_DAMAGE / YARN_SHORTAGE / MACHINE_CHANGE / WORKER_CHANGE
  exceptionNote?: string // 异常说明
  exceptionPhotos?: string[] // 异常现场照片 URL 数组（≤6 张）
  startedAt?: string // 开工时间
  completedAt?: string // 完工时间
  voidReason?: string // 作废原因
  voidedAt?: string // 作废时间
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

// 布票批量生成请求 VO
export interface WeavingTicketCreateReqVO {
  machineTaskId: number
  count: number
  materialBatchNo?: string
  workerUserId?: number
  workerName?: string
}

// 布票开工请求 VO
export interface WeavingTicketStartReqVO {
  id?: number
  ids?: number[]
  actualMachine?: string
  workerUserId?: number
  workerName?: string
  startedAt?: string
}

// 布票完工请求 VO
export interface WeavingTicketCompleteReqVO {
  id?: number
  ids?: number[]
  completedAt?: string
}

// 布票异常请求 VO
export interface WeavingTicketExceptionReqVO {
  id?: number
  ids?: number[]
  exceptionType: string
  exceptionNote?: string
  exceptionPhotos?: string[]
}

// 布票作废请求 VO
export interface WeavingTicketVoidReqVO {
  id?: number
  ids?: number[]
  voidReason: string
}

// 布票分页查询参数
export interface WeavingTicketPageParam {
  pageNo?: number
  pageSize?: number
  weavingOrderId?: number
  machineTaskId?: number
  status?: string
  code?: string
  plannedMachine?: string
  actualMachine?: string
  workerName?: string
}

// 按机台任务批量生成布票
export const createWeavingTickets = (data: WeavingTicketCreateReqVO) => {
  return request.post<WeavingTicketVO[]>({
    url: '/lunaerp/production/weaving-order-ticket/create',
    data
  })
}

// 布票开工（单张/批量）
export const startWeavingTicket = (data: WeavingTicketStartReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-ticket/start', data })
}

// 布票完工（单张/批量）
export const completeWeavingTicket = (data: WeavingTicketCompleteReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-ticket/complete', data })
}

// 布票异常登记（单张/批量）
export const exceptionWeavingTicket = (data: WeavingTicketExceptionReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-ticket/exception', data })
}

// 布票作废（单张/批量）
export const voidWeavingTicket = (data: WeavingTicketVoidReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-ticket/void', data })
}

// 获得布票详情
export const getWeavingTicket = (id: number) => {
  return request.get<WeavingTicketVO>({
    url: `/lunaerp/production/weaving-order-ticket/get?id=${id}`
  })
}

// 按布票编号获得布票详情（扫码枪录入）
export const getWeavingTicketByCode = (code: string) => {
  return request.get<WeavingTicketVO>({
    url: `/lunaerp/production/weaving-order-ticket/get-by-code?code=${encodeURIComponent(code)}`
  })
}

// 查询布票分页
export const getWeavingTicketPage = (params: WeavingTicketPageParam) => {
  return request.get({ url: '/lunaerp/production/weaving-order-ticket/page', params })
}

// 布票打印标签 VO（独立 VO，豁免脱敏）
export interface WeavingTicketPrintVO {
  id: number
  code: string // 布票编号
  weavingOrderId?: number
  weavingOrderCode?: string // 织造单号
  machineTaskId?: number
  machineTaskCode?: string // 机台任务号
  loomNo?: string // 机台号
  productName?: string // 品名
  color?: string // 颜色
  pieceIndex?: number // 匹序
  quantity?: number // 数量/重量 KG
  unit?: string // 单位
  workerName?: string // 挡车工姓名
  materialBatchNo?: string // 原料批号
  qrUrl: string // 完整二维码 URL
  printTime?: string
}

// 获得布票打印数据（独立 VO，豁免脱敏，含拼好的 qrUrl）
export const getWeavingTicketPrintData = (id: number | string) => {
  return request.get<WeavingTicketPrintVO>({
    url: `/lunaerp/production/weaving-ticket/${id}/print-data`
  })
}
