import request from '@/config/axios'

// 反馈缸 VO
export interface ProgressFeedbackVatVO {
  id: number
  dyeingOrderId: number
  dyeingColorId: number
  vatNo: string
  quantity: number
  pieces?: number
  status: string
  scheduledDate?: string
  dyeingStartedDate?: string
  fixingStartedDate?: string
  qualityCheckDate?: string
  completedDate?: string
  delayReason?: string
  remark?: string
  delayed: boolean
  unresolvedIssueCount: number
  createTime?: string
}

// 颜色明细行骨架节 VO（零快照实时取出）
export interface ProgressFeedbackColorSectionVO {
  dyeingOrderId: number
  salesContractId?: number
  contractNo?: string
  customerId?: number
  clientName?: string
  dyeingFactoryId: number
  dyeingFactoryName?: string
  deliveryDate?: string
  productCode?: string
  productName?: string
  dyeingOrderStatus?: string
  dyeingColorId: number
  lineNo?: number
  customerColorNo?: string
  colorName?: string
  colorQuantity: number
  colorPieces?: number
  unit?: string
  totalVatQuantity: number
  totalVatPieces?: number
  coveragePercent: number
  overScheduled: boolean
  vats: ProgressFeedbackVatVO[]
}

// 统计条响应 VO
export interface ProgressFeedbackStatisticsVO {
  unscheduledColorCount: number
  inProgressVatCount: number
  completedVatCount: number
  delayedVatCount: number
  unresolvedVatCount?: number
}

// 分页与筛选查询参数
export interface ProgressFeedbackPageParam {
  pageNo?: number
  pageSize?: number
  dyeingOrderId?: number
  contractNo?: string
  dyeingFactoryId?: number
  status?: string
  keyword?: string
  delayedOnly?: boolean
  unresolvedOnly?: boolean
  includeCompleted?: boolean
  includeDeleted?: boolean
}

// 查询进度反馈覆盖层分页（以染色颜色明细行为骨架）
export const getProgressFeedbackPage = (params: ProgressFeedbackPageParam) => {
  return request.get({ url: '/lunaerp/production/progress-feedback/page', params })
}

// 查询染色单下的全部颜色行节（供染色单工作台页签使用）
export const getProgressFeedbackListByOrder = (dyeingOrderId: number) => {
  return request.get<ProgressFeedbackColorSectionVO[]>({
    url: '/lunaerp/production/progress-feedback/list-by-order',
    params: { dyeingOrderId }
  })
}

// 查询统计条数据（与当前筛选同口径）
export const getProgressFeedbackStatistics = (params: ProgressFeedbackPageParam) => {
  return request.get<ProgressFeedbackStatisticsVO>({
    url: '/lunaerp/production/progress-feedback/statistics',
    params
  })
}

// 建缸 VO
export interface ProgressFeedbackCreateVO {
  dyeingOrderId: number
  dyeingColorId: number
  vatNo: string
  quantity: number
  pieces?: number
  scheduledDate?: string
  remark?: string
}

// 新建反馈缸
export const createProgressFeedback = (data: ProgressFeedbackCreateVO) => {
  return request.post<number>({
    url: '/lunaerp/production/progress-feedback/create',
    data
  })
}

// 反馈缸详情 VO
export interface ProgressFeedbackDetailVO {
  id: number
  dyeingOrderId: number
  dyeingColorId: number
  vatNo: string
  quantity: number
  pieces?: number
  status: string
  scheduledDate?: string
  dyeingStartedDate?: string
  fixingStartedDate?: string
  qualityCheckDate?: string
  completedDate?: string
  delayReason?: string
  remark?: string
  createTime?: string
  creator?: string
  updateTime?: string
  updater?: string

  // 染色单与颜色行实时信息（零快照）
  salesContractId?: number
  contractNo?: string
  customerId?: number
  clientName?: string
  dyeingFactoryId?: number
  dyeingFactoryName?: string
  deliveryDate?: string
  productCode?: string
  productName?: string
  dyeingOrderStatus?: string
  lineNo?: number
  customerColorNo?: string
  colorName?: string
  colorQuantity?: number
  colorPieces?: number
  unit?: string

  // 派生计算指标
  delayed: boolean
  unresolvedIssueCount: number
  totalVatQuantity: number
  totalVatPieces?: number
  coveragePercent: number
  overScheduled: boolean
}

// 编辑缸 VO
export interface ProgressFeedbackUpdateVO {
  id: number
  vatNo: string
  quantity: number
  pieces?: number
  status?: string
  scheduledDate?: string
  dyeingStartedDate?: string
  fixingStartedDate?: string
  qualityCheckDate?: string
  completedDate?: string
  delayReason?: string
  remark?: string
}

// 推进工序 VO
export interface ProgressFeedbackAdvanceVO {
  id: number
  targetStatus?: string
}

// 查询反馈缸详情
export const getProgressFeedback = (id: number) => {
  return request.get<ProgressFeedbackDetailVO>({
    url: '/lunaerp/production/progress-feedback/get',
    params: { id }
  })
}

// 编辑反馈缸信息与阶段日期、状态回退
export const updateProgressFeedback = (data: ProgressFeedbackUpdateVO) => {
  return request.put<boolean>({
    url: '/lunaerp/production/progress-feedback/update',
    data
  })
}

// 推进反馈缸工序阶段
export const advanceProgressFeedback = (data: ProgressFeedbackAdvanceVO) => {
  return request.put<boolean>({
    url: '/lunaerp/production/progress-feedback/advance',
    data
  })
}

// 删除反馈缸（级联删除沟通记录）
export const deleteProgressFeedback = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/production/progress-feedback/${id}`
  })
}
