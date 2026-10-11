import { describe, it, expect } from 'vitest'
import {
  resolveBarTheme,
  transformToGanttBar,
  buildGanttRows,
  rollbackBar,
  getDefaultWindow,
  type FormattedGanttBar
} from '../helper'
import type { GanttBarVO, GanttLoomVO } from '@/api/lunaerp-elmui/production/scheduling'

describe('排产中心甘特与待排池逻辑 (Issue 07)', () => {
  const sampleLooms: GanttLoomVO[] = [
    { loomId: 1, loomNo: 'L-01', model: '喷气 230' },
    { loomId: 2, loomNo: 'L-02', model: '剑杆 200' },
    { loomId: 3, loomNo: 'L-03', model: '喷水 190' }
  ]

  const sampleBars: GanttBarVO[] = [
    {
      taskId: 101,
      taskCode: 'MT-WO01-01',
      loomId: 1,
      weavingOrderId: 11,
      weavingOrderCode: 'WO20261001',
      productCode: 'P01',
      productName: '汗布',
      plannedQty: 30,
      actualQty: 10,
      workerName: '张三',
      plannedStart: '2026-10-05',
      plannedEnd: '2026-10-10',
      status: 'IN_PROGRESS',
      conflict: false
    },
    {
      taskId: 102,
      taskCode: 'MT-WO02-01',
      loomId: 1,
      weavingOrderId: 12,
      weavingOrderCode: 'WO20261002',
      productCode: 'P02',
      productName: '斜纹',
      plannedQty: 20,
      actualQty: 0,
      workerName: '李四',
      plannedStart: '2026-10-08',
      plannedEnd: '2026-10-12',
      status: 'PENDING',
      conflict: true // 冲突
    },
    {
      taskId: 103,
      taskCode: 'MT-WO03-01',
      loomId: 2,
      weavingOrderId: 13,
      weavingOrderCode: 'WO20261003',
      productCode: 'P03',
      productName: '提花',
      plannedQty: 50,
      actualQty: 50,
      workerName: '王五',
      plannedStart: '2026-10-01',
      plannedEnd: '2026-10-04',
      status: 'COMPLETED',
      conflict: false
    },
    {
      taskId: 104,
      taskCode: 'MT-WO04-01',
      loomId: 2,
      weavingOrderId: 14,
      weavingOrderCode: 'WO20261004',
      productCode: 'P04',
      productName: '罗纹',
      plannedQty: 15,
      actualQty: 5,
      workerName: '赵六',
      plannedStart: '2026-10-05',
      plannedEnd: '2026-10-08',
      status: 'HALTED',
      conflict: false
    },
    {
      taskId: 105,
      taskCode: 'MT-WO05-01',
      loomId: 1,
      weavingOrderId: 15,
      weavingOrderCode: 'WO20261005',
      productCode: 'P05',
      productName: '毛圈',
      plannedQty: 40,
      actualQty: 0,
      workerName: '',
      plannedStart: '',
      plannedEnd: '',
      status: 'PENDING',
      conflict: false
    }
  ]

  it('① 状态样式与冲突标记映射正确', () => {
    const normalTheme = resolveBarTheme('IN_PROGRESS', false)
    expect(normalTheme.style.backgroundColor).toContain('--el-color-primary')
    expect(normalTheme.className).toBe('gantt-bar-in_progress')

    const conflictTheme = resolveBarTheme('PENDING', true)
    expect(conflictTheme.className).toContain('is-conflict')
    expect(conflictTheme.style.border).toContain('dashed')
    expect(conflictTheme.style.border).toContain('--el-color-error')
  })

  it('② 甘特条带转换：COMPLETED 与 HALTED 禁用拖拽', () => {
    const inProgress = transformToGanttBar(sampleBars[0])
    expect(inProgress.ganttBarConfig.immobile).toBe(false)
    expect(inProgress.ganttBarConfig.hasHandles).toBe(true)
    expect(inProgress.ganttBarConfig.label).toBe('1001 (30匹)')

    const completed = transformToGanttBar(sampleBars[2])
    expect(completed.ganttBarConfig.immobile).toBe(true)
    expect(completed.ganttBarConfig.hasHandles).toBe(false)

    const halted = transformToGanttBar(sampleBars[3])
    expect(halted.ganttBarConfig.immobile).toBe(true)
    expect(halted.ganttBarConfig.hasHandles).toBe(false)
  })

  it('③ buildGanttRows：包含空行织机，过滤无日期任务', () => {
    const rows = buildGanttRows(sampleLooms, sampleBars)
    expect(rows.length).toBe(3)

    // Loom 1 有 2 个有日期条带（无日期的 105 排除）
    const loom1 = rows.find((r) => r.loomId === 1)!
    expect(loom1.bars.length).toBe(2)
    expect(loom1.bars.map((b) => b.id)).toEqual([101, 102])

    // Loom 2 有 2 个条带（COMPLETED, HALTED）
    const loom2 = rows.find((r) => r.loomId === 2)!
    expect(loom2.bars.length).toBe(2)

    // Loom 3 无任务（空行）仍展示
    const loom3 = rows.find((r) => r.loomId === 3)!
    expect(loom3.bars.length).toBe(0)
    expect(loom3.label).toBe('L-03 (喷水 190)')
  })

  it('④ 乐观更新回滚：恢复原起止日期', () => {
    const bar: FormattedGanttBar = transformToGanttBar(sampleBars[0])
    bar.startDate = '2026-10-15'
    bar.endDate = '2026-10-20'

    rollbackBar(bar, '2026-10-05', '2026-10-10')
    expect(bar.startDate).toBe('2026-10-05')
    expect(bar.endDate).toBe('2026-10-10')
  })

  it('⑤ 默认时间窗口生成 <= 92 天跨度', () => {
    const { start, end } = getDefaultWindow(30)
    expect(start).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(end).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(end >= start).toBe(true)
  })
})
