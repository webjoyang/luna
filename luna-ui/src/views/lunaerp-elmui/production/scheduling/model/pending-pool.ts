import type { PendingOrderVO, PendingTaskVO } from '@/api/lunaerp-elmui/production/scheduling'
import type {
  MachineTaskSaveReqVO,
  MachineTaskSaveRespVO
} from '@/api/lunaerp-elmui/production/machine-task'

/**
 * 派工表单数据结构
 */
export interface DispatchFormData {
  id?: number
  code?: string
  weavingOrderId?: number
  weavingOrderCode?: string
  loomId?: number
  loomNo?: string
  workerUserId?: number
  workerName?: string
  plannedQty: number
  plannedStart?: string
  plannedEnd?: string
  remark?: string
}

/**
 * 创建空白派工表单
 */
export function createEmptyDispatchForm(): DispatchFormData {
  return {
    plannedQty: 1
  }
}

/**
 * 从待排池未排满织造单（组二）初始化派工表单
 * 规则：带入单 ID 与默认匹数=remainingQty
 */
export function initDispatchFromPendingOrder(order: PendingOrderVO): DispatchFormData {
  return {
    weavingOrderId: order.weavingOrderId,
    weavingOrderCode: order.weavingOrderCode,
    plannedQty: order.remainingQty ?? 1,
    loomId: undefined,
    plannedStart: undefined,
    plannedEnd: undefined
  }
}

/**
 * 从待排池无日期任务（组一）初始化派工表单
 * 规则：带入织造单与任务已有织机、匹数快照
 */
export function initDispatchFromPendingTask(task: PendingTaskVO): DispatchFormData {
  return {
    weavingOrderId: task.weavingOrderId,
    weavingOrderCode: task.weavingOrderCode,
    loomId: task.loomId,
    loomNo: task.loomNo,
    plannedQty: task.plannedQty ?? 1,
    plannedStart: undefined,
    plannedEnd: undefined
  }
}

/**
 * 派工表单校验结果
 */
export interface DispatchValidationResult {
  valid: boolean
  errorMsg?: string
}

/**
 * 校验派工表单
 */
export function validateDispatchForm(form: DispatchFormData): DispatchValidationResult {
  if (!form.weavingOrderId) {
    return { valid: false, errorMsg: '请选择关联织造单' }
  }
  if (!form.loomId) {
    return { valid: false, errorMsg: '请选择目标织机' }
  }
  if (!form.plannedQty || form.plannedQty <= 0) {
    return { valid: false, errorMsg: '计划匹数必须为大于0的正整数' }
  }
  if (form.plannedStart && form.plannedEnd && form.plannedEnd < form.plannedStart) {
    return { valid: false, errorMsg: '计划结束日期不能早于开始日期' }
  }
  return { valid: true }
}

/**
 * 派工提交响应结果
 */
export interface DispatchSubmitResult {
  success: boolean
  id?: number
  code?: string
  warning?: string
  isConflict?: boolean
  conflictMessage?: string
  error?: string
  errorCode?: number
}

/**
 * 提交派工请求，硬校验拦截(000)时保持弹窗打开并回显红字
 */
export async function submitDispatch(
  form: DispatchFormData,
  apiCreate: (data: MachineTaskSaveReqVO) => Promise<MachineTaskSaveRespVO>
): Promise<DispatchSubmitResult> {
  const reqData: MachineTaskSaveReqVO = {
    id: form.id,
    code: form.code,
    weavingOrderId: form.weavingOrderId,
    loomId: form.loomId,
    workerUserId: form.workerUserId,
    workerName: form.workerName,
    plannedQty: form.plannedQty,
    plannedStart: form.plannedStart,
    plannedEnd: form.plannedEnd,
    remark: form.remark
  }

  try {
    const res = await apiCreate(reqData)
    return {
      success: true,
      id: res.id,
      code: res.code,
      warning: res.warning
    }
  } catch (err: any) {
    const errorMsg = err?.message || err?.msg || (typeof err === 'string' ? err : '派工保存失败')
    const errorCode = err?.code
    const isConflict = errorCode === 1030036000 || errorMsg.includes('冲突')

    return {
      success: false,
      isConflict,
      conflictMessage: isConflict ? errorMsg : undefined,
      error: errorMsg,
      errorCode
    }
  }
}
