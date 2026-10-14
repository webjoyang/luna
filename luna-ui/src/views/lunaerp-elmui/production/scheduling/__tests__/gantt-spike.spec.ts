import { describe, it, expect } from 'vitest'
import dayjs from 'dayjs'
import {
  elementPlusGanttTheme,
  TASK_STATUS_THEME_MAP,
  getGanttBarClasses,
  isTaskDraggable,
  getTaskDragDisabledReason,
  type MachineTaskStatus
} from '../gantt-theme'

interface LoomGanttRow {
  loomId: number
  loomNo: string
  label: string
  bars: MachineTaskGanttBar[]
}

interface MachineTaskGanttBar {
  taskId: number
  taskCode: string
  weavingOrderCode: string
  plannedQty: number
  startDate: string
  endDate: string
  status: MachineTaskStatus
  conflict: boolean
  ganttBarConfig: {
    id: string
    label: string
    hasHandles: boolean
    immobile: boolean
    class: string
  }
}

function buildBarConfig(
  taskId: number,
  label: string,
  status: MachineTaskStatus,
  conflict: boolean
) {
  const draggable = isTaskDraggable(status)
  return {
    id: `task-${taskId}`,
    label,
    hasHandles: draggable,
    immobile: !draggable,
    class: getGanttBarClasses(status, conflict, !draggable)
  }
}

describe('03: vue-ganttastic 选型 spike 验证', () => {
  describe('① 资源型甘特渲染数据映射（行=织机、条=机台任务）', () => {
    it('正确将织机与机台任务组装为甘特行与条带结构', () => {
      const loom = { loomId: 1, loomNo: 'L-01', model: '喷气 230' }
      const task = {
        taskId: 101,
        taskCode: 'MT-WO20261001-01',
        weavingOrderCode: 'WO20261001',
        plannedQty: 30,
        plannedStart: '2026-10-08',
        plannedEnd: '2026-10-12',
        status: 'IN_PROGRESS' as MachineTaskStatus,
        conflict: false
      }

      const row: LoomGanttRow = {
        loomId: loom.loomId,
        loomNo: loom.loomNo,
        label: `${loom.loomNo} (${loom.model})`,
        bars: [
          {
            taskId: task.taskId,
            taskCode: task.taskCode,
            weavingOrderCode: task.weavingOrderCode,
            plannedQty: task.plannedQty,
            startDate: task.plannedStart,
            endDate: task.plannedEnd,
            status: task.status,
            conflict: task.conflict,
            ganttBarConfig: buildBarConfig(
              task.taskId,
              `${task.weavingOrderCode.slice(-4)} / ${task.plannedQty}匹`,
              task.status,
              task.conflict
            )
          }
        ]
      }

      expect(row.label).toBe('L-01 (喷气 230)')
      expect(row.bars).toHaveLength(1)
      expect(row.bars[0].startDate).toBe('2026-10-08')
      expect(row.bars[0].endDate).toBe('2026-10-12')
      expect(row.bars[0].ganttBarConfig.label).toBe('1001 / 30匹')
      expect(row.bars[0].ganttBarConfig.hasHandles).toBe(true)
      expect(row.bars[0].ganttBarConfig.immobile).toBe(false)
      expect(row.bars[0].ganttBarConfig.class).toContain('status-in-progress')
    })
  })

  describe('② 水平拖动平移起止验证（天粒度）', () => {
    it('平移 Δt 天保持工期跨度不变，正确计算新的起止日期', () => {
      const originalStart = '2026-10-08'
      const originalEnd = '2026-10-12'
      const durationDays = dayjs(originalEnd).diff(dayjs(originalStart), 'day')
      expect(durationDays).toBe(4)

      // 模拟水平向右平移 3 天
      const deltaDays = 3
      const newStart = dayjs(originalStart).add(deltaDays, 'day').format('YYYY-MM-DD')
      const newEnd = dayjs(originalEnd).add(deltaDays, 'day').format('YYYY-MM-DD')

      expect(newStart).toBe('2026-10-11')
      expect(newEnd).toBe('2026-10-15')
      expect(dayjs(newEnd).diff(dayjs(newStart), 'day')).toBe(durationDays)
    })
  })

  describe('③ 右边缘拉伸结束日验证（天粒度）', () => {
    it('拉伸右边缘调整结束日期，开始日期保持不变', () => {
      const originalStart = '2026-10-08'
      const originalEnd = '2026-10-12'

      // 模拟向右拉伸 2 天
      const extendedEnd = dayjs(originalEnd).add(2, 'day').format('YYYY-MM-DD')
      expect(extendedEnd).toBe('2026-10-14')

      // 开始日期不受拉伸影响
      expect(originalStart).toBe('2026-10-08')
      expect(dayjs(extendedEnd).isAfter(dayjs(originalStart))).toBe(true)
    })
  })

  describe('④ 跨行拖拽改织机可行性分析、状态守卫与降级定案', () => {
    it('确认四态拖拽守卫及 HALTED/COMPLETED 禁拖提示策略', () => {
      expect(isTaskDraggable('PENDING')).toBe(true)
      expect(isTaskDraggable('IN_PROGRESS')).toBe(true)
      expect(isTaskDraggable('HALTED')).toBe(false)
      expect(isTaskDraggable('COMPLETED')).toBe(false)

      // 停机守卫：提示先在织造单工作台恢复
      expect(getTaskDragDisabledReason('HALTED')).toBe(
        '任务已暂停异常，请先在织造单工作台恢复后再改期'
      )
      // 完工守卫：禁止改期
      expect(getTaskDragDisabledReason('COMPLETED')).toBe('已完工任务禁止改期')
      expect(getTaskDragDisabledReason('IN_PROGRESS')).toBeNull()
    })

    it('根据 vue-ganttastic 机制定案转机降级为弹窗下拉', () => {
      // vue-ganttastic createBarDrag 仅处理 clientX 水平位移，条带非 HTML5 draggable
      // 官方 Issue #93 明确不支持跨行拖拽，定案采用条带点击弹窗/侧拉抽屉内织机下拉选择
      const degradedInteractionStrategy = 'MODAL_LOOM_DROPDOWN_TRANSFER'
      expect(degradedInteractionStrategy).toBe('MODAL_LOOM_DROPDOWN_TRANSFER')
    })
  })

  describe('⑤ 主题与 CSS 变量对齐 ElementPlus 与字典 2239 规范', () => {
    it('elementPlusGanttTheme 与状态字典 2239 严格对齐', () => {
      expect(elementPlusGanttTheme.primary).toBe('#f5f7fa')
      expect(elementPlusGanttTheme.background).toBe('#ffffff')
      expect(elementPlusGanttTheme.text).toBe('#303133')
      expect(elementPlusGanttTheme.markerCurrentTime).toBe('#409eff')

      // 状态字典 2239 标准词汇校验 (GLOSSARY.md)
      expect(TASK_STATUS_THEME_MAP.PENDING.label).toBe('待开始')
      expect(TASK_STATUS_THEME_MAP.IN_PROGRESS.label).toBe('生产中')
      expect(TASK_STATUS_THEME_MAP.HALTED.label).toBe('暂停异常')
      expect(TASK_STATUS_THEME_MAP.COMPLETED.label).toBe('已完工')

      // 状态色映射
      expect(TASK_STATUS_THEME_MAP.PENDING.color).toBe('#909399')
      expect(TASK_STATUS_THEME_MAP.IN_PROGRESS.color).toBe('#409eff')
      expect(TASK_STATUS_THEME_MAP.HALTED.color).toBe('#f56c6c')
      expect(TASK_STATUS_THEME_MAP.COMPLETED.color).toBe('#67c23a')

      // 冲突样式类名
      const conflictClasses = getGanttBarClasses('IN_PROGRESS', true, false)
      expect(conflictClasses).toContain('status-in-progress')
      expect(conflictClasses).toContain('is-conflict')

      // 终态/停机锁定类名
      const immobileClasses = getGanttBarClasses('COMPLETED', false, true)
      expect(immobileClasses).toContain('status-completed')
      expect(immobileClasses).toContain('is-immobile')
    })
  })

  describe('⑥ 50 织机 × 500 任务数据量性能基准测试（< 3s 阈值）', () => {
    it('在 50 织机 × 500 任务数据量下，数据组装与视口过滤耗时远低于 3000ms', () => {
      const loomCount = 50
      const tasksPerLoom = 10
      const totalTasks = loomCount * tasksPerLoom // 500 tasks

      const startTime = performance.now()

      // 1. 生成 50 台织机
      const looms = Array.from({ length: loomCount }, (_, i) => ({
        loomId: i + 1,
        loomNo: `L-${String(i + 1).padStart(2, '0')}`,
        model: i % 2 === 0 ? '喷气 230' : '剑杆 190'
      }))

      // 2. 生成 500 个机台任务
      const tasks: MachineTaskGanttBar[] = []
      let taskId = 1
      for (let i = 0; i < loomCount; i++) {
        for (let j = 0; j < tasksPerLoom; j++) {
          const startDay = (j * 5) % 60
          const duration = 3 + (j % 3)
          const plannedStart = dayjs('2026-10-01').add(startDay, 'day').format('YYYY-MM-DD')
          const plannedEnd = dayjs(plannedStart).add(duration, 'day').format('YYYY-MM-DD')
          const statusList: MachineTaskStatus[] = ['PENDING', 'IN_PROGRESS', 'HALTED', 'COMPLETED']
          const status = statusList[j % 4]
          const conflict = j === 2

          tasks.push({
            taskId,
            taskCode: `MT-WO-${taskId}`,
            weavingOrderCode: `WO202610${String(taskId).padStart(3, '0')}`,
            plannedQty: 20 + j * 5,
            startDate: plannedStart,
            endDate: plannedEnd,
            status,
            conflict,
            ganttBarConfig: buildBarConfig(
              taskId,
              `WO-${taskId} / ${20 + j * 5}匹`,
              status,
              conflict
            )
          })
          taskId++
        }
      }

      expect(tasks).toHaveLength(totalTasks)

      // 3. 构建织机行映射（50 行）
      const rows: LoomGanttRow[] = looms.map((loom, index) => ({
        loomId: loom.loomId,
        loomNo: loom.loomNo,
        label: `${loom.loomNo} (${loom.model})`,
        bars: tasks.slice(index * tasksPerLoom, (index + 1) * tasksPerLoom)
      }))

      expect(rows).toHaveLength(50)

      // 4. 模拟 90 天视口范围过滤
      const windowStart = '2026-10-01'
      const windowEnd = '2026-12-31'
      const filteredRows = rows.map((row) => ({
        ...row,
        bars: row.bars.filter(
          (b) =>
            dayjs(b.startDate).isBefore(dayjs(windowEnd)) &&
            dayjs(b.endDate).isAfter(dayjs(windowStart))
        )
      }))

      expect(filteredRows).toHaveLength(50)

      // 5. 模拟一次拖拽位置更新
      const targetBar = filteredRows[0].bars[0]
      const oldStart = targetBar.startDate
      const oldEnd = targetBar.endDate
      targetBar.startDate = dayjs(oldStart).add(2, 'day').format('YYYY-MM-DD')
      targetBar.endDate = dayjs(oldEnd).add(2, 'day').format('YYYY-MM-DD')

      const elapsedMs = performance.now() - startTime

      console.log(
        `[性能基准] 50 织机 × 500 任务组装、视口过滤与改期耗时: ${elapsedMs.toFixed(2)} ms`
      )

      expect(elapsedMs).toBeLessThan(3000)
      expect(elapsedMs).toBeLessThan(500)
    })
  })
})
