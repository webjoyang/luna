import type {
  MachineTaskVO,
  MachineTaskSaveReqVO,
  MachineTaskSaveRespVO,
  MachineTaskPageParam
} from '@/api/lunaerp-elmui/production/machine-task'
import { SCHEDULE_CONFLICT_CODE, parseErrorMessage } from '../gantt-theme'

/**
 * 顶部共享筛选状态
 */
export interface SharedFilterState {
  dateWindow?: string[]
  loomIds?: number[]
  statuses?: string[]
}

/**
 * 跨单任务列表独有筛选
 */
export interface TaskListLocalFilter {
  code?: string
  conflictOnly?: boolean
}

/**
 * 分页参数状态
 */
export interface PaginationState {
  pageNo: number
  pageSize: number
}

/**
 * 行操作入口权限守卫
 * 规约（spec §2.3）：严禁暴露删除（delete）与停机（halt）操作
 */
export interface TaskRowActions {
  canEdit: boolean
  canDelete: false
  canHalt: false
  canJump: boolean
}

/**
 * 行数据格式化结果
 */
export interface TaskRowFormatted {
  productDisplay: string
  scheduleDisplay: string
  isConflict: boolean
}

/**
 * 任务编辑提交结果
 */
export interface TaskEditSubmitResult {
  success: boolean
  id?: number
  code?: string
  warning?: string
  isConflict: boolean
  conflictMessage?: string
  error?: string
  errorCode?: number
}

/**
 * 组装跨单机台任务分页查询参数（对接 Seam 1）
 * 规则：weavingOrderId 缺省不传以获得跨单全量数据
 */
export function buildTaskPageParams(
  shared: SharedFilterState,
  local: TaskListLocalFilter,
  pagination: PaginationState
): MachineTaskPageParam {
  const params: MachineTaskPageParam = {
    pageNo: pagination.pageNo,
    pageSize: pagination.pageSize
  }

  if (local.conflictOnly) {
    params.conflictOnly = true
  }

  if (local.code && local.code.trim()) {
    params.code = local.code.trim()
  }

  if (shared.loomIds && shared.loomIds.length > 0) {
    params.loomIds = shared.loomIds
  }

  if (shared.statuses && shared.statuses.length > 0) {
    params.statuses = shared.statuses
  }

  if (
    shared.dateWindow &&
    shared.dateWindow.length === 2 &&
    shared.dateWindow[0] &&
    shared.dateWindow[1]
  ) {
    params.startDate = shared.dateWindow[0]
    params.endDate = shared.dateWindow[1]
  }

  return params
}

/**
 * 计算行操作权限守卫（对接 Seam 3）
 * 规约：
 * 1. COMPLETED 任务只读不可编辑；
 * 2. 排产中心列表坚决不放删除/停机入口（canDelete=false, canHalt=false）；
 * 3. 关联织造单存在即允许跳转织造单工作台。
 */
export function getTaskRowActions(task: MachineTaskVO): TaskRowActions {
  const isCompleted = task.status === 'COMPLETED'
  return {
    canEdit: !isCompleted,
    canDelete: false,
    canHalt: false,
    canJump: Boolean(task.weavingOrderId)
  }
}

/**
 * 生成跳转织造单工作台路径
 */
export function buildWeavingOrderWorkspacePath(weavingOrderId: number | string): string {
  return `/production/weaving-order/workspace/${weavingOrderId}`
}

/**
 * 格式化任务行数据展示
 */
export function formatTaskRow(task: MachineTaskVO): TaskRowFormatted {
  let productDisplay = '-'
  if (task.productCode && task.productName) {
    productDisplay = `[${task.productCode}] ${task.productName}`
  } else if (task.productName) {
    productDisplay = task.productName
  } else if (task.productCode) {
    productDisplay = task.productCode
  }

  let scheduleDisplay = '未排期'
  if (task.plannedStart || task.plannedEnd) {
    scheduleDisplay = `${task.plannedStart || '未定'} ~ ${task.plannedEnd || '未定'}`
  }

  return {
    productDisplay,
    scheduleDisplay,
    isConflict: Boolean(task.conflict)
  }
}

/**
 * 提交任务编辑保存（对接 Seam 2）
 * 规则：
 * 1. 000 冲突拦截（1_030_036_000 或错误消息含“冲突”）不关闭弹窗，回显红字
 * 2. 超量保存成功并保留软警告
 */
export async function submitTaskEdit(
  data: MachineTaskSaveReqVO,
  apiUpdate: (data: MachineTaskSaveReqVO) => Promise<MachineTaskSaveRespVO>
): Promise<TaskEditSubmitResult> {
  try {
    const res = await apiUpdate(data)
    return {
      success: true,
      id: res.id,
      code: res.code,
      warning: res.warning,
      isConflict: false
    }
  } catch (err: any) {
    const errorMsg = parseErrorMessage(err, '保存失败')
    const errorCode = err?.code
    const isConflict = errorCode === SCHEDULE_CONFLICT_CODE || errorMsg.includes('冲突')

    return {
      success: false,
      isConflict,
      conflictMessage: isConflict ? errorMsg : undefined,
      error: errorMsg,
      errorCode
    }
  }
}
