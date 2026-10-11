import type { GanttBarVO, GanttLoomVO } from '@/api/lunaerp-elmui/production/scheduling'

export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'HALTED' | 'COMPLETED'

export interface FormattedGanttBar {
  id: number
  taskId: number
  taskCode: string
  loomId: number
  weavingOrderId: number
  weavingOrderCode?: string
  productCode?: string
  productName?: string
  plannedQty: number
  actualQty?: number
  workerName?: string
  startDate: string // YYYY-MM-DD
  endDate: string // YYYY-MM-DD
  status: string
  conflict: boolean
  ganttBarConfig: {
    id: string
    label: string
    hasHandles: boolean
    immobile: boolean
    style: Record<string, string>
    class: string
  }
}

export interface LoomGanttRow {
  loomId: number
  loomNo: string
  model?: string
  label: string
  bars: FormattedGanttBar[]
}

/**
 * 样式映射函数：将任务状态与冲突标记映射为 ElementPlus CSS 变量 (ADR-0041)
 */
export function resolveBarTheme(status: string, conflict: boolean) {
  const colorMap: Record<string, { bg: string; text: string; border: string }> = {
    PENDING: {
      bg: 'var(--el-color-info-light-8, #f4f4f5)',
      text: 'var(--el-color-info-dark-2, #303133)',
      border: 'var(--el-color-info-light-5, #d3d4d6)'
    },
    IN_PROGRESS: {
      bg: 'var(--el-color-primary, #409eff)',
      text: '#ffffff',
      border: 'var(--el-color-primary-dark-2, #337ecc)'
    },
    HALTED: {
      bg: 'var(--el-color-danger, #f56c6c)',
      text: '#ffffff',
      border: 'var(--el-color-danger-dark-2, #b88230)'
    },
    COMPLETED: {
      bg: 'var(--el-color-success, #67c23a)',
      text: '#ffffff',
      border: 'var(--el-color-success-dark-2, #529b2e)'
    }
  }

  const theme = colorMap[status] || colorMap.PENDING
  const style: Record<string, string> = {
    backgroundColor: theme.bg,
    color: theme.text,
    border: `1px solid ${theme.border}`,
    borderRadius: 'var(--el-border-radius-base, 4px)',
    fontSize: 'var(--el-font-size-extra-small, 12px)'
  }

  let className = `gantt-bar-${status.toLowerCase()}`
  if (conflict) {
    style.border = '2px dashed var(--el-color-error, #f56c6c)'
    style.boxShadow = '0 0 6px var(--el-color-error-light-5, rgba(245, 108, 108, 0.4))'
    className += ' is-conflict'
  }

  return { style, className }
}

/**
 * 转换后端甘特条带为 vue-ganttastic 条带对象
 */
export function transformToGanttBar(task: GanttBarVO): FormattedGanttBar {
  const isConflict = Boolean(task.conflict)
  const isImmobile = task.status === 'COMPLETED' || task.status === 'HALTED'
  const canResize = !isImmobile // 仅非完工且非停机的任务支持拖拽边缘拉伸
  const { style, className } = resolveBarTheme(task.status, isConflict)

  const orderSuffix = task.weavingOrderCode
    ? task.weavingOrderCode.slice(-4)
    : task.taskCode.slice(-4)
  const label = `${orderSuffix} (${task.plannedQty}匹)`

  return {
    id: task.taskId,
    taskId: task.taskId,
    taskCode: task.taskCode,
    loomId: task.loomId,
    weavingOrderId: task.weavingOrderId,
    weavingOrderCode: task.weavingOrderCode,
    productCode: task.productCode,
    productName: task.productName,
    plannedQty: task.plannedQty,
    actualQty: task.actualQty,
    workerName: task.workerName,
    startDate: task.plannedStart || '',
    endDate: task.plannedEnd || '',
    status: task.status,
    conflict: isConflict,
    ganttBarConfig: {
      id: String(task.taskId),
      label,
      hasHandles: canResize,
      immobile: isImmobile,
      style,
      class: className
    }
  }
}

/**
 * 装配甘特织机行与条带
 */
export function buildGanttRows(looms: GanttLoomVO[], bars: GanttBarVO[]): LoomGanttRow[] {
  const barsByLoomId = new Map<number, FormattedGanttBar[]>()
  for (const bar of bars) {
    // 过滤无日期条带（无日期不渲染于甘特，进待排池）
    if (!bar.plannedStart || !bar.plannedEnd) {
      continue
    }
    const formatted = transformToGanttBar(bar)
    const list = barsByLoomId.get(bar.loomId) || []
    list.push(formatted)
    barsByLoomId.set(bar.loomId, list)
  }

  return looms.map((loom) => {
    const label = loom.model ? `${loom.loomNo} (${loom.model})` : loom.loomNo
    return {
      loomId: loom.loomId,
      loomNo: loom.loomNo,
      model: loom.model,
      label,
      bars: barsByLoomId.get(loom.loomId) || []
    }
  })
}

/**
 * 格式化日期为 YYYY-MM-DD
 */
export function formatDateStr(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * 获取默认查询窗口（今天起 30 天）
 */
export function getDefaultWindow(days = 30): { start: string; end: string } {
  const now = new Date()
  const start = formatDateStr(now)
  const endDate = new Date(now)
  endDate.setDate(endDate.getDate() + days)
  const end = formatDateStr(endDate)
  return { start, end }
}

/**
 * 乐观更新失败回滚条带
 */
export function rollbackBar(
  bar: FormattedGanttBar,
  oldStart: string,
  oldEnd: string
): void {
  bar.startDate = oldStart
  bar.endDate = oldEnd
}

/**
 * 组装跨单机台任务分页查询参数 (Issue 08)
 * 保证不强加 weavingOrderId 限制，天然支持跨单聚合分页
 */
export function buildTaskPageQuery(params: {
  pageNo: number
  pageSize: number
  loomId?: number
  status?: string
  conflictOnly?: boolean
  code?: string
}) {
  return {
    pageNo: params.pageNo,
    pageSize: params.pageSize,
    loomId: params.loomId || undefined,
    status: params.status || undefined,
    conflictOnly: params.conflictOnly ? true : undefined,
    code: params.code?.trim() || undefined
  }
}

