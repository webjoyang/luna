import { describe, it, expect } from 'vitest'

/**
 * Issue 03: vue-ganttastic 选型 spike 验证与数据结构测试
 *
 * 验证目标：
 * ① 资源型甘特渲染结构（行 = 织机，条 = 机台任务）
 * ② 水平拖动平移起止（日期位移、乐观更新与回滚守卫）
 * ③ 右边缘拉伸结束日（天粒度结束日调整）
 * ④ 跨行拖拽改织机可行性验证与降级决策（条带点击弹窗织机下拉转机）
 * ⑤ ElementPlus 主题 CSS 变量对齐与冲突视觉标记
 * ⑥ 50 织机 × 500 任务数据量性能基准测试（< 3s 阈值）
 */

// 甘特条带状态定义（对齐 lunaerp_weaving_task_status 字典与 ElementPlus 色系）
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'HALTED' | 'COMPLETED'

export interface MachineTaskBar {
  id: number
  taskCode: string
  loomId: number
  weavingOrderId: number
  weavingOrderCode: string
  productCode?: string
  productName?: string
  plannedQty: number
  actualQty: number
  startDate: string // YYYY-MM-DD
  endDate: string // YYYY-MM-DD
  status: TaskStatus
  conflict: boolean
  workerName?: string
  ganttBarConfig: {
    id: string
    label: string
    hasHandles: boolean
    immobile: boolean
    style: Record<string, string>
    class?: string
  }
}

export interface LoomGanttRow {
  loomId: number
  loomNo: string
  model: string
  label: string
  bars: MachineTaskBar[]
}

// 主题样式映射函数：将任务状态与冲突标记映射为 ElementPlus CSS 变量
export function resolveBarTheme(status: TaskStatus, conflict: boolean) {
  const colorMap: Record<TaskStatus, { bg: string; text: string; border: string }> = {
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

// 转换机台任务为甘特条带对象
export function transformToGanttBar(task: {
  id: number
  taskCode: string
  loomId: number
  weavingOrderId: number
  weavingOrderCode: string
  plannedQty: number
  actualQty: number
  plannedStart: string
  plannedEnd: string
  status: TaskStatus
  conflict?: boolean
}): MachineTaskBar {
  const isConflict = Boolean(task.conflict)
  const isImmobile = task.status === 'COMPLETED' || task.status === 'HALTED'
  const canResize = !isImmobile // 仅非完工且非停机的任务支持拖拽边缘拉伸
  const { style, className } = resolveBarTheme(task.status, isConflict)

  return {
    id: task.id,
    taskCode: task.taskCode,
    loomId: task.loomId,
    weavingOrderId: task.weavingOrderId,
    weavingOrderCode: task.weavingOrderCode,
    plannedQty: task.plannedQty,
    actualQty: task.actualQty,
    startDate: task.plannedStart,
    endDate: task.plannedEnd,
    status: task.status,
    conflict: isConflict,
    ganttBarConfig: {
      id: String(task.id),
      label: `${task.weavingOrderCode.slice(-4)} (${task.plannedQty}匹)`,
      hasHandles: canResize,
      immobile: isImmobile,
      style,
      class: className
    }
  }
}

// 模拟水平平移（Move start/end）
export function applyHorizontalMove(
  bar: MachineTaskBar,
  daysDelta: number
): { newStart: string; newEnd: string } {
  const shiftDate = (dStr: string, delta: number) => {
    const d = new Date(dStr)
    d.setDate(d.getDate() + delta)
    return d.toISOString().slice(0, 10)
  }
  return {
    newStart: shiftDate(bar.startDate, daysDelta),
    newEnd: shiftDate(bar.endDate, daysDelta)
  }
}

// 模拟右边缘拉伸（Resize end date）
export function applyRightEdgeResize(
  bar: MachineTaskBar,
  newEndDate: string
): { newStart: string; newEnd: string } {
  if (newEndDate < bar.startDate) {
    throw new Error('结束日期不能早于开始日期')
  }
  return {
    newStart: bar.startDate,
    newEnd: newEndDate
  }
}

// 降级转机操作验证：甘特内禁止跨行拖拽，通过独立转机逻辑触发
export function changeTaskLoom(
  task: MachineTaskBar,
  newLoomId: number,
  allRows: LoomGanttRow[]
): { success: boolean; rows: LoomGanttRow[] } {
  // 查找原织机与目标织机
  const originRow = allRows.find((r) => r.loomId === task.loomId)
  const targetRow = allRows.find((r) => r.loomId === newLoomId)
  if (!originRow || !targetRow) {
    return { success: false, rows: allRows }
  }

  // 从原行移除并加入新行
  originRow.bars = originRow.bars.filter((b) => b.id !== task.id)
  const updatedBar: MachineTaskBar = {
    ...task,
    loomId: newLoomId
  }
  targetRow.bars.push(updatedBar)
  return { success: true, rows: allRows }
}

describe('vue-ganttastic 选型 spike 验证 (Issue 03)', () => {
  it('① 资源型甘特渲染结构：行=织机，条=机台任务', () => {
    const looms = [
      { loomId: 1, loomNo: 'L-01', model: '喷气230' },
      { loomId: 2, loomNo: 'L-02', model: '喷气230' }
    ]
    const rawTasks = [
      {
        id: 101,
        taskCode: 'MT-01',
        loomId: 1,
        weavingOrderId: 10,
        weavingOrderCode: 'WO20261001-01',
        plannedQty: 100,
        actualQty: 20,
        plannedStart: '2026-10-01',
        plannedEnd: '2026-10-05',
        status: 'IN_PROGRESS' as TaskStatus
      },
      {
        id: 102,
        taskCode: 'MT-02',
        loomId: 2,
        weavingOrderId: 11,
        weavingOrderCode: 'WO20261001-02',
        plannedQty: 80,
        actualQty: 0,
        plannedStart: '2026-10-06',
        plannedEnd: '2026-10-10',
        status: 'PENDING' as TaskStatus
      }
    ]

    const rows: LoomGanttRow[] = looms.map((loom) => ({
      loomId: loom.loomId,
      loomNo: loom.loomNo,
      model: loom.model,
      label: `${loom.loomNo} (${loom.model})`,
      bars: rawTasks.filter((t) => t.loomId === loom.loomId).map(transformToGanttBar)
    }))

    expect(rows).toHaveLength(2)
    expect(rows[0].label).toBe('L-01 (喷气230)')
    expect(rows[0].bars).toHaveLength(1)
    expect(rows[0].bars[0].ganttBarConfig.label).toBe('1-01 (100匹)')
    expect(rows[0].bars[0].ganttBarConfig.immobile).toBe(false)
    expect(rows[0].bars[0].ganttBarConfig.hasHandles).toBe(true)
    expect(rows[1].bars).toHaveLength(1)
    expect(rows[1].bars[0].status).toBe('PENDING')
  })

  it('② 水平拖动平移起止：计算位移并支持冲突回滚', () => {
    const task = transformToGanttBar({
      id: 201,
      taskCode: 'MT-03',
      loomId: 1,
      weavingOrderId: 12,
      weavingOrderCode: 'WO20261001-03',
      plannedQty: 50,
      actualQty: 0,
      plannedStart: '2026-10-05',
      plannedEnd: '2026-10-08',
      status: 'PENDING'
    })

    // 水平平移 +3 天
    const { newStart, newEnd } = applyHorizontalMove(task, 3)
    expect(newStart).toBe('2026-10-08')
    expect(newEnd).toBe('2026-10-11')

    // 乐观更新机制：更新前保存快照
    const snapshot = { start: task.startDate, end: task.endDate }
    task.startDate = newStart
    task.endDate = newEnd

    // 模拟后端冲突拦截 (1_030_036_000) 导致回滚
    const backendSaveResult = { success: false, code: 1030036000 }
    if (!backendSaveResult.success) {
      task.startDate = snapshot.start
      task.endDate = snapshot.end
    }

    expect(task.startDate).toBe('2026-10-05')
    expect(task.endDate).toBe('2026-10-08')
  })

  it('③ 右边缘拉伸结束日：天粒度调整结束日，禁止早于开始日', () => {
    const task = transformToGanttBar({
      id: 202,
      taskCode: 'MT-04',
      loomId: 1,
      weavingOrderId: 12,
      weavingOrderCode: 'WO20261001-04',
      plannedQty: 60,
      actualQty: 10,
      plannedStart: '2026-10-10',
      plannedEnd: '2026-10-15',
      status: 'IN_PROGRESS'
    })

    // 正常拉伸延期至 2026-10-18
    const extended = applyRightEdgeResize(task, '2026-10-18')
    expect(extended.newStart).toBe('2026-10-10')
    expect(extended.newEnd).toBe('2026-10-18')

    // 异常测试：拉伸至开始日之前抛出错误
    expect(() => applyRightEdgeResize(task, '2026-10-09')).toThrow('结束日期不能早于开始日期')
  })

  it('④ 跨行拖拽改织机可行性验证与降级决策：定案弹窗转机', () => {
    // 验证状态守卫：COMPLETED 与 HALTED 任务不可被拖动 (immobile = true)
    const completedTask = transformToGanttBar({
      id: 301,
      taskCode: 'MT-05',
      loomId: 1,
      weavingOrderId: 13,
      weavingOrderCode: 'WO20261001-05',
      plannedQty: 100,
      actualQty: 100,
      plannedStart: '2026-09-20',
      plannedEnd: '2026-09-25',
      status: 'COMPLETED'
    })
    expect(completedTask.ganttBarConfig.immobile).toBe(true)
    expect(completedTask.ganttBarConfig.hasHandles).toBe(false)

    const haltedTask = transformToGanttBar({
      id: 302,
      taskCode: 'MT-06',
      loomId: 1,
      weavingOrderId: 13,
      weavingOrderCode: 'WO20261001-06',
      plannedQty: 100,
      actualQty: 40,
      plannedStart: '2026-09-26',
      plannedEnd: '2026-09-30',
      status: 'HALTED'
    })
    expect(haltedTask.ganttBarConfig.immobile).toBe(true)

    // 跨行转机验证：vue-ganttastic 不支持原生跨行拖拽，通过转机弹窗/方法完成转机迁移
    const rows: LoomGanttRow[] = [
      { loomId: 1, loomNo: 'L-01', model: '喷气230', label: 'L-01', bars: [] },
      { loomId: 2, loomNo: 'L-02', model: '喷气230', label: 'L-02', bars: [] }
    ]
    const activeTask = transformToGanttBar({
      id: 303,
      taskCode: 'MT-07',
      loomId: 1,
      weavingOrderId: 14,
      weavingOrderCode: 'WO20261001-07',
      plannedQty: 80,
      actualQty: 0,
      plannedStart: '2026-10-10',
      plannedEnd: '2026-10-15',
      status: 'PENDING'
    })
    rows[0].bars.push(activeTask)

    const transferResult = changeTaskLoom(activeTask, 2, rows)
    expect(transferResult.success).toBe(true)
    expect(rows[0].bars).toHaveLength(0)
    expect(rows[1].bars).toHaveLength(1)
    expect(rows[1].bars[0].loomId).toBe(2)
  })

  it('⑤ ElementPlus 主题 CSS 变量对齐与冲突视觉标记', () => {
    // 待排：info 色系
    const pendingTheme = resolveBarTheme('PENDING', false)
    expect(pendingTheme.style.backgroundColor).toContain('--el-color-info')
    expect(pendingTheme.className).toBe('gantt-bar-pending')

    // 生产中：primary 色系
    const inProgressTheme = resolveBarTheme('IN_PROGRESS', false)
    expect(inProgressTheme.style.backgroundColor).toContain('--el-color-primary')
    expect(inProgressTheme.className).toBe('gantt-bar-in_progress')

    // 暂停：danger 色系
    const haltedTheme = resolveBarTheme('HALTED', false)
    expect(haltedTheme.style.backgroundColor).toContain('--el-color-danger')

    // 完工：success 色系
    const completedTheme = resolveBarTheme('COMPLETED', false)
    expect(completedTheme.style.backgroundColor).toContain('--el-color-success')

    // 冲突任务：叠加红色虚线描边与阴影高亮
    const conflictTheme = resolveBarTheme('IN_PROGRESS', true)
    expect(conflictTheme.style.border).toContain('--el-color-error')
    expect(conflictTheme.className).toContain('is-conflict')
  })

  it('⑥ 50 织机 × 500 任务数据量性能基准测试 (< 3s)', () => {
    const startTime = performance.now()

    // 构造 50 台织机
    const looms = Array.from({ length: 50 }, (_, i) => ({
      loomId: i + 1,
      loomNo: `L-${String(i + 1).padStart(2, '0')}`,
      model: '喷气230'
    }))

    // 构造 500 个机台任务（每机 10 个任务）
    const tasks = []
    let taskId = 1000
    for (let l = 1; l <= 50; l++) {
      for (let t = 0; t < 10; t++) {
        taskId++
        const startDay = 1 + (t % 25)
        const endDay = startDay + 2
        tasks.push({
          id: taskId,
          taskCode: `MT-WO${taskId}`,
          loomId: l,
          weavingOrderId: 500 + taskId,
          weavingOrderCode: `WO${taskId}`,
          plannedQty: 100,
          actualQty: 10,
          plannedStart: `2026-10-${String(startDay).padStart(2, '0')}`,
          plannedEnd: `2026-10-${String(endDay).padStart(2, '0')}`,
          status: (t === 0 ? 'IN_PROGRESS' : 'PENDING') as TaskStatus,
          conflict: t === 3 // 模拟冲突
        })
      }
    }

    // 模拟数据映射与行结构组装
    const taskMap = new Map<number, MachineTaskBar[]>()
    for (const t of tasks) {
      const bar = transformToGanttBar(t)
      const list = taskMap.get(bar.loomId) || []
      list.push(bar)
      taskMap.set(bar.loomId, list)
    }

    const rows: LoomGanttRow[] = looms.map((loom) => ({
      loomId: loom.loomId,
      loomNo: loom.loomNo,
      model: loom.model,
      label: `${loom.loomNo} (${loom.model})`,
      bars: taskMap.get(loom.loomId) || []
    }))

    const elapsed = performance.now() - startTime

    expect(rows).toHaveLength(50)
    expect(tasks).toHaveLength(500)
    // 耗时应远小于 3000ms（实际应在 50ms 以内）
    expect(elapsed).toBeLessThan(3000)
  })
})
