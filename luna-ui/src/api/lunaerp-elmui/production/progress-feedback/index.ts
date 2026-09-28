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
