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
