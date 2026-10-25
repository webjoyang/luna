import type {
  MachineTaskVO,
  MachineTaskSaveReqVO,
  MachineTaskSaveRespVO,
  MachineTaskPageParam
} from '@/api/lunaerp-elmui/production/machine-task'
import { submitDispatch } from './pending-pool'

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
 * 规则：
 * 1. weavingOrderId 缺省不传以获得跨单全量数据；
 * 2. 跨单列表不强制绑定甘特固定日期窗口，确保未排期任务与全周期任务均可按序出单（spec §3.2 / §6.1）；
 * 3. 共享织机与状态多选过滤。
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
 * 统一委托至 submitDispatch，保持错误拦截与时段冲突判定单一来源（DRY）
 */
export async function submitTaskEdit(
  data: MachineTaskSaveReqVO,
  apiUpdate: (data: MachineTaskSaveReqVO) => Promise<MachineTaskSaveRespVO>
): Promise<TaskEditSubmitResult> {
  const result = await submitDispatch(data, apiUpdate)
  return {
    success: result.success,
    id: result.id,
    code: result.code,
    warning: result.warning,
    isConflict: Boolean(result.isConflict),
    conflictMessage: result.conflictMessage,
    error: result.error,
    errorCode: result.errorCode
  }
}
