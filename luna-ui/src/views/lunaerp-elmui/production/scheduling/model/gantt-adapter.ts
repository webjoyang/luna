import type { GanttBarObject } from 'vue-ganttastic'
import type { GanttLoomVO, GanttBarVO } from '@/api/lunaerp-elmui/production/scheduling'
import type {
  MachineTaskSaveReqVO,
  MachineTaskSaveRespVO
} from '@/api/lunaerp-elmui/production/machine-task'
import {
  getGanttBarClasses,
  isTaskDraggable,
  parseErrorMessage,
  type MachineTaskStatus
} from '../gantt-theme'

/**
 * 适配 vue-ganttastic 的任务条带对象
 */
export interface MachineTaskGanttBar extends GanttBarObject {
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
  startDate: string
  endDate: string
  status: MachineTaskStatus | string
  conflict: boolean
  ganttBarConfig: {
    id: string
    label: string
    hasHandles: boolean
    immobile: boolean
    class: string
  }
}

/**
 * 适配 vue-ganttastic 的织机行对象
 */
export interface LoomGanttRow {
  loomId: number
  loomNo: string
  label: string
  bars: MachineTaskGanttBar[]
}

/**
 * 格式化甘特条带展示文本（织造单尾号 + 计划匹数）
 */
export function formatBarLabel(weavingOrderCode?: string, plannedQty: number = 0): string {
  const code = (weavingOrderCode || '').trim()
  const tail = code.length > 4 ? code.slice(-4) : code
  return tail ? `${tail} / ${plannedQty}匹` : `${plannedQty}匹`
}

/**
 * 将后端织机列表和任务条带组装为甘特图行数据
 */
export function buildGanttRows(looms: GanttLoomVO[] = [], bars: GanttBarVO[] = []): LoomGanttRow[] {
  // 构建织机行映射（保留所有活跃织机空行）
  const loomMap = new Map<number, MachineTaskGanttBar[]>()
  for (const loom of looms) {
    loomMap.set(loom.loomId, [])
  }

  // 过滤有日期的任务条带（单侧日期按 spec §3.1 coalesce 折算为单日区间，两端皆空才仅进待排池）
  for (const bar of bars) {
    const start = bar.plannedStart || bar.plannedEnd
    const end = bar.plannedEnd || bar.plannedStart
    if (!start || !end) {
      continue
    }

    const rowBars = loomMap.get(bar.loomId)
    if (!rowBars) {
      // 织机不在列表（理论上已被后端过滤）
      continue
    }

    const draggable = isTaskDraggable(bar.status)
    const label = formatBarLabel(bar.weavingOrderCode, bar.plannedQty)
    const immobile = !draggable

    rowBars.push({
      taskId: bar.taskId,
      taskCode: bar.taskCode,
      loomId: bar.loomId,
      weavingOrderId: bar.weavingOrderId,
      weavingOrderCode: bar.weavingOrderCode,
      productCode: bar.productCode,
      productName: bar.productName,
      plannedQty: bar.plannedQty,
      actualQty: bar.actualQty ?? 0,
      workerUserId: bar.workerUserId,
      workerName: bar.workerName,
      startDate: start,
      endDate: end,
      status: bar.status,
      conflict: !!bar.conflict,
      ganttBarConfig: {
        id: `task-${bar.taskId}`,
        label,
        hasHandles: draggable,
        immobile,
        class: getGanttBarClasses(bar.status, bar.conflict, immobile)
      }
    })
  }

  return looms.map((loom) => ({
    loomId: loom.loomId,
    loomNo: loom.loomNo,
    label: `${loom.loomNo}${loom.model ? ' (' + loom.model + ')' : ''}`,
    bars: loomMap.get(loom.loomId) || []
  }))
}

export interface OptimisticUpdateOptions {
  bar: MachineTaskGanttBar
  newStart: string
  newEnd: string
  oldStart: string
  oldEnd: string
  apiUpdate: (data: MachineTaskSaveReqVO) => Promise<MachineTaskSaveRespVO>
}

export interface OptimisticUpdateResult {
  success: boolean
  rolledBack?: boolean
  warning?: string
  error?: string
  code?: number
}

/**
 * 甘特条带拖拽乐观更新与回滚处理器
 */
export async function optimisticBarUpdate(
  options: OptimisticUpdateOptions
): Promise<OptimisticUpdateResult> {
  const { bar, newStart, newEnd, oldStart, oldEnd, apiUpdate } = options

  // 1. 乐观更新本地条带日期
  bar.startDate = newStart
  bar.endDate = newEnd

  try {
    const res = await apiUpdate({
      id: bar.taskId,
      loomId: bar.loomId,
      plannedStart: newStart,
      plannedEnd: newEnd,
      plannedQty: bar.plannedQty,
      workerUserId: bar.workerUserId,
      workerName: bar.workerName
    })

    return {
      success: true,
      warning: res.warning
    }
  } catch (err: any) {
    // 2. 失败时回滚本地条带日期
    bar.startDate = oldStart
    bar.endDate = oldEnd

    const errorMsg = parseErrorMessage(err, '操作失败')
    const errorCode = err?.code

    return {
      success: false,
      rolledBack: true,
      error: errorMsg,
      code: errorCode
    }
  }
}
