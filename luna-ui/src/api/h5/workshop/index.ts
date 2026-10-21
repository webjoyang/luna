import request from '@/config/axios'

/**
 * 车间移动 H5 - 布票状态统计 VO
 */
export interface WorkshopTicketStatusCountsVO {
  total: number
  generated: number
  started: number
  completed: number
  inspected: number
  stocked: number
  exception: number
  voided: number
}

/**
 * 车间移动 H5 - 我的机台任务精简出参 VO
 */
export interface WorkshopMyTaskVO {
  id: number
  code: string
  weavingOrderId: number
  weavingOrderCode?: string
  productName?: string
  productCode?: string
  loomId?: number
  loomNo?: string
  workerUserId?: number
  workerName?: string
  plannedQty: number
  actualQty: number
  status: 'PENDING' | 'IN_PROGRESS' | 'HALTED' | 'COMPLETED' | string
  plannedStart?: string
  plannedEnd?: string
  ticketCounts?: WorkshopTicketStatusCountsVO
}

/**
 * 车间移动 H5 - 我的机台任务分页请求 VO
 */
export interface WorkshopMyTasksPageReqVO {
  pageNo?: number
  pageSize?: number
  status?: string
}

/**
 * 查询当前登录挡车工的机台任务列表（分页）
 *
 * 后端强制硬过滤 worker_user_id = 登录人，不可越权查询他人任务
 */
export const getMyTasks = (params?: WorkshopMyTasksPageReqVO) => {
  return request.get<{ list: WorkshopMyTaskVO[]; total: number }>({
    url: '/lunaerp/h5/workshop/my-tasks',
    params
  })
}

/**
 * 车间移动 H5 - 布票精简卡片出参 VO
 */
export interface WorkshopTicketVO {
  id: number
  code: string
  weavingOrderId?: number
  weavingOrderCode?: string
  machineTaskId?: number
  machineTaskCode?: string
  loomId?: number
  loomNo?: string
  plannedMachine?: string
  actualMachine?: string
  productName?: string
  productCode?: string
  composition?: string
  gsm?: string
  width?: string
  plannedWeight?: number
  workerUserId?: number
  workerName?: string
  materialBatchNo?: string
  status:
    | 'GENERATED'
    | 'STARTED'
    | 'COMPLETED'
    | 'INSPECTED'
    | 'STOCKED'
    | 'EXCEPTION'
    | 'VOID'
    | string
  exceptionType?: string
  exceptionNote?: string
  exceptionPhotos?: string[]
  startedAt?: string
  completedAt?: string
  claimable?: boolean
}

/**
 * 按布票编号查询布票卡片详情（扫码/手输定位）
 *
 * @param code 布票编号
 */
export const getTicketByCode = (code: string) => {
  return request.get<WorkshopTicketVO>({
    url: '/lunaerp/h5/workshop/ticket/get-by-code',
    params: { code }
  })
}

/**
 * 车间移动 H5 - 布票开工 Request VO
 */
export interface WorkshopTicketStartReqVO {
  /**
   * 布票编号
   */
  code: string
  /**
   * 实际上机机台号（可选，缺省继承任务机台号）
   */
  actualMachine?: string
}

/**
 * 布票开工（含自助认领）
 *
 * @param data 开工入参
 */
export const startTicket = (data: WorkshopTicketStartReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/h5/workshop/ticket/start',
    data
  })
}
