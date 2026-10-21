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

/**
 * 车间移动 H5 - 布票完工 Request VO
 */
export interface WorkshopTicketCompleteReqVO {
  /**
   * 布票编号
   */
  code: string
}

/**
 * 车间移动 H5 - 布票完工 Response VO（幂等友好）
 */
export interface WorkshopTicketCompleteRespVO {
  /**
   * 是否已完工（若原先已处于完工/检验/入库态，返回 true；若本次从 STARTED 成功完工，返回 false）
   */
  alreadyCompleted: boolean
}

/**
 * 布票完工（幂等友好）
 *
 * @param data 完工入参
 */
export const completeTicket = (data: WorkshopTicketCompleteReqVO) => {
  return request.post<WorkshopTicketCompleteRespVO>({
    url: '/lunaerp/h5/workshop/ticket/complete',
    data
  })
}

/**
 * 车间移动 H5 - 布票异常登记 Request VO
 */
export interface WorkshopTicketExceptionReqVO {
  /**
   * 布票编号
   */
  code: string
  /**
   * 异常类型（字典 lunaerp_weaving_exception_type：YARN_BREAK/CLOTH_DAMAGE/YARN_SHORTAGE/MACHINE_CHANGE/WORKER_CHANGE）
   */
  exceptionType: string
  /**
   * 异常说明
   */
  exceptionNote?: string
  /**
   * 异常现场照片 URL 列表（≤6 张，仅允许图片 URL）
   */
  exceptionPhotos?: string[]
}

/**
 * 布票异常登记
 *
 * @param data 异常登记入参
 */
export const exceptionTicket = (data: WorkshopTicketExceptionReqVO) => {
  return request.post<boolean>({
    url: '/lunaerp/h5/workshop/ticket/exception',
    data
  })
}

/**
 * 车间移动 H5 - 检验单分页 Request VO
 */
export interface WorkshopInspectionPageReqVO {
  pageNo?: number
  pageSize?: number
  status?: string
  type?: string
}

/**
 * 车间移动 H5 - 检验单精简出参 VO
 */
export interface WorkshopInspectionVO {
  id: number
  code: string
  type: 'GREIGE' | 'FINISHED' | string
  status: 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | string
  packageMode?: 'ROLL' | 'PIECE' | string
  productId?: number
  productCode?: string
  productName?: string
  inspectionFactoryId?: number
  inspectionFactoryName?: string
  salesContractId?: number
  contractNo?: string
  targetWeight?: number
  orderDate?: string
  remark?: string
  rollCount?: number
  createTime?: string
}

/**
 * 车间移动 H5 - 录检明细精简出参 VO
 */
export interface WorkshopInspectionItemVO {
  id: number
  inspectionOrderId: number
  rollNo?: string
  pieceNo?: string
  weavingTicketId?: number
  vatNo?: string
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
  photos?: string[]
  createTime?: string
}

/**
 * 车间移动 H5 - 检验单详情出参 VO（表头 + 明细列表 + 只读状态）
 */
export interface WorkshopInspectionDetailVO extends WorkshopInspectionVO {
  readonly: boolean
  items: WorkshopInspectionItemVO[]
}

/**
 * 查询检验单列表（分页）
 *
 * 状态默认 DRAFT/ACTIVE、type 可选、分页；检验员可看全部租户内可录检单不按机台过滤
 *
 * @param params 分页与过滤参数
 */
export const getInspectionPage = (params?: WorkshopInspectionPageReqVO) => {
  return request.get<{ list: WorkshopInspectionVO[]; total: number }>({
    url: '/lunaerp/h5/workshop/inspection/page',
    params
  })
}

/**
 * 查询检验单详情（表头+逐行含 photos；仅 DRAFT/ACTIVE 可进，COMPLETED 只读）
 *
 * @param id 检验单编号
 */
export const getInspection = (id: number) => {
  return request.get<WorkshopInspectionDetailVO>({
    url: '/lunaerp/h5/workshop/inspection/get',
    params: { id }
  })
}
