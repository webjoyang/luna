import type { ColorScheme } from 'vue-ganttastic'

/**
 * Element Plus 对齐的主题色配置对象（传递给 GGanttChart 的 colorScheme 属性）
 */
export const elementPlusGanttTheme: ColorScheme = {
  primary: '#f5f7fa',
  secondary: '#f5f7fa',
  ternary: '#ffffff',
  quartenary: '#ebeef5',
  hoverHighlight: 'rgba(235, 238, 245, 0.65)',
  markerCurrentTime: '#409eff',
  text: '#303133',
  background: '#ffffff'
}

/**
 * 任务状态与 Element Plus 语义色映射
 * 对齐字典 2239 lunaerp_weaving_task_status 标准词汇 (GLOSSARY.md)
 */
export const TASK_STATUS_THEME_MAP = {
  PENDING: {
    label: '待开始',
    tagType: 'info' as const,
    color: '#909399',
    bgColor: '#c8c9cc',
    className: 'status-pending'
  },
  IN_PROGRESS: {
    label: '生产中',
    tagType: 'primary' as const,
    color: '#409eff',
    bgColor: '#79bbff',
    className: 'status-in-progress'
  },
  HALTED: {
    label: '暂停异常',
    tagType: 'danger' as const,
    color: '#f56c6c',
    bgColor: '#f89898',
    className: 'status-halted'
  },
  COMPLETED: {
    label: '已完工',
    tagType: 'success' as const,
    color: '#67c23a',
    bgColor: '#95d475',
    className: 'status-completed'
  }
} as const

export type MachineTaskStatus = keyof typeof TASK_STATUS_THEME_MAP

/**
 * 构建甘特条带样式类名
 */
export function getGanttBarClasses(
  status: MachineTaskStatus | string,
  conflict?: boolean,
  immobile?: boolean
): string {
  const classes: string[] = []
  const theme = TASK_STATUS_THEME_MAP[status as MachineTaskStatus]
  if (theme) {
    classes.push(theme.className)
  }
  if (conflict) {
    classes.push('is-conflict')
  }
  if (immobile) {
    classes.push('is-immobile')
  }
  return classes.join(' ')
}

/**
 * 判断任务是否允许在甘特图上拖动改期
 */
export function isTaskDraggable(status: MachineTaskStatus | string): boolean {
  return status === 'PENDING' || status === 'IN_PROGRESS'
}

/**
 * 获取任务禁止拖拽时的提示说明
 */
export function getTaskDragDisabledReason(status: MachineTaskStatus | string): string | null {
  if (status === 'COMPLETED') {
    return '已完工任务禁止改期'
  }
  if (status === 'HALTED') {
    return '任务已暂停异常，请先在织造单工作台恢复后再改期'
  }
  return null
}
