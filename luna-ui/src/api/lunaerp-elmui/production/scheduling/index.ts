import request from '@/config/axios'

// 甘特图织机行 VO
export interface GanttLoomVO {
  loomId: number
  loomNo: string
  model?: string
}

// 甘特图任务条带 VO
export interface GanttBarVO {
  taskId: number
  taskCode: string
  loomId: number
  weavingOrderId: number
  weavingOrderCode?: string
  productCode?: string
  productName?: string
  plannedQty: number
  actualQty?: number
  workerName?: string
  plannedStart?: string
  plannedEnd?: string
  status: string
  conflict: boolean
}

// 甘特图查询响应 VO
export interface GanttRespVO {
  looms: GanttLoomVO[]
  bars: GanttBarVO[]
}

// 甘特图查询入参
export interface GanttQueryParam {
  start: string
  end: string
  loomIds?: number[]
  statuses?: string[]
}

// 待排池无日期任务 VO
export interface PendingTaskVO {
  taskId: number
  taskCode: string
  weavingOrderId: number
  weavingOrderCode?: string
  loomId: number
  loomNo?: string
  plannedQty: number
}

// 待排池未排满织造单 VO
export interface PendingOrderVO {
  weavingOrderId: number
  weavingOrderCode: string
  productCode?: string
  productName?: string
  plannedQty: number
  scheduledQty: number
  remainingQty: number
}

// 待排池响应 VO
export interface PendingPoolRespVO {
  unDatedTasks: PendingTaskVO[]
  unScheduledOrders: PendingOrderVO[]
}

// 获得排产甘特图数据（织机行与任务条带）
export const getGanttData = (params: GanttQueryParam) => {
  return request.get<GanttRespVO>({ url: '/lunaerp/production/weaving-order-task/gantt', params })
}

// 获得排产待排池数据（无日期任务与未排满织造单）
export const getPendingPool = () => {
  return request.get<PendingPoolRespVO>({ url: '/lunaerp/production/weaving-order-task/pending-pool' })
}
