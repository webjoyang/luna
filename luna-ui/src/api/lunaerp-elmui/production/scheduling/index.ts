import request from '@/config/axios'

/**
 * 甘特图请求参数
 */
export interface GanttReqVO {
  start: string
  end: string
  loomIds?: number[]
  statuses?: string[]
}

/**
 * 甘特图织机行 VO
 */
export interface GanttLoomVO {
  loomId: number
  loomNo: string
  model?: string
}

/**
 * 甘特图任务条带 VO
 */
export interface GanttBarVO {
  taskId: number
  taskCode: string
  loomId: number
  weavingOrderId: number
  weavingOrderCode: string
  productCode?: string
  productName?: string
  plannedQty: number
  actualQty: number
  workerUserId?: number
  workerName?: string
  plannedStart?: string
  plannedEnd?: string
  status: string
  conflict: boolean
}

/**
 * 甘特图响应 VO
 */
export interface GanttRespVO {
  looms: GanttLoomVO[]
  bars: GanttBarVO[]
}

/**
 * 待排池 未排期任务 VO
 */
export interface PendingTaskVO {
  taskId: number
  taskCode: string
  weavingOrderId: number
  weavingOrderCode: string
  loomId?: number
  loomNo?: string
  plannedQty: number
}

/**
 * 待排池 未排满织造单 VO
 */
export interface PendingOrderVO {
  weavingOrderId: number
  weavingOrderCode: string
  productCode?: string
  productName?: string
  plannedQty: number
  scheduledQty: number
  remainingQty: number
}

/**
 * 待排池响应 VO
 */
export interface PendingPoolRespVO {
  unDatedTasks: PendingTaskVO[]
  unScheduledOrders: PendingOrderVO[]
}

/**
 * 查询排产甘特数据（织机行 + 任务条带 + 冲突标记）
 */
export const getGantt = (params: GanttReqVO) => {
  return request.get<GanttRespVO>({
    url: '/lunaerp/production/weaving-order-task/gantt',
    params
  })
}

/**
 * 查询待排池数据（未排期任务 + 未排满织造单）
 */
export const getPendingPool = () => {
  return request.get<PendingPoolRespVO>({
    url: '/lunaerp/production/weaving-order-task/pending-pool'
  })
}
