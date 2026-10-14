import { describe, it, expect, vi } from 'vitest'
import {
  buildGanttRows,
  optimisticBarUpdate,
  formatBarLabel,
  type MachineTaskGanttBar
} from '../model/gantt-adapter'
import type { GanttLoomVO, GanttBarVO } from '@/api/lunaerp-elmui/production/scheduling'

describe('Seam 2: Gantt Adapter & Optimistic Drag-and-Drop Model', () => {
  const mockLooms: GanttLoomVO[] = [
    { loomId: 1, loomNo: 'L-01', model: '喷气 230' },
    { loomId: 2, loomNo: 'L-02', model: '剑杆 190' },
    { loomId: 3, loomNo: 'L-03', model: undefined }
  ]

  const mockBars: GanttBarVO[] = [
    {
      taskId: 101,
      taskCode: 'MT-WO20261001-01',
      loomId: 1,
      weavingOrderId: 10,
      weavingOrderCode: 'WO20261001',
      productCode: 'P01',
      productName: '棉汗布',
      plannedQty: 30,
      actualQty: 10,
      workerUserId: 99,
      workerName: '张三',
      plannedStart: '2026-10-05',
      plannedEnd: '2026-10-10',
      status: 'IN_PROGRESS',
      conflict: false
    },
    {
      taskId: 102,
      taskCode: 'MT-WO20261002-01',
      loomId: 1,
      weavingOrderId: 11,
      weavingOrderCode: 'WO20261002',
      productCode: 'P02',
      productName: '卫衣布',
      plannedQty: 25,
      actualQty: 0,
      workerName: '李四',
      plannedStart: '2026-10-08',
      plannedEnd: '2026-10-14',
      status: 'PENDING',
      conflict: true
    },
    {
      taskId: 103,
      taskCode: 'MT-WO20261003-01',
      loomId: 2,
      weavingOrderId: 12,
      weavingOrderCode: 'WO20261003',
      productCode: 'P03',
      productName: '双面布',
      plannedQty: 40,
      actualQty: 0,
      workerName: undefined,
      plannedStart: '2026-10-12',
      plannedEnd: '2026-10-16',
      status: 'HALTED',
      conflict: false
    },
    {
      taskId: 104,
      taskCode: 'MT-WO20261004-01',
      loomId: 2,
      weavingOrderId: 13,
      weavingOrderCode: 'WO20261004',
      productCode: 'P04',
      productName: '罗纹布',
      plannedQty: 50,
      actualQty: 50,
      workerName: '王五',
      plannedStart: '2026-10-01',
      plannedEnd: '2026-10-04',
      status: 'COMPLETED',
      conflict: false
    },
    {
      // 无排期任务，必须排除出甘特图（仅入待排池）
      taskId: 105,
      taskCode: 'MT-WO20261005-01',
      loomId: 1,
      weavingOrderId: 14,
      weavingOrderCode: 'WO20261005',
      productCode: 'P05',
      productName: '氨纶布',
      plannedQty: 15,
      actualQty: 0,
      workerName: undefined,
      plannedStart: undefined,
      plannedEnd: undefined,
      status: 'PENDING',
      conflict: false
    }
  ]

  describe('buildGanttRows', () => {
    it('正确生成织机行，保留空行（无任务的织机）', () => {
      const rows = buildGanttRows(mockLooms, mockBars)

      expect(rows).toHaveLength(3)
      expect(rows[0].label).toBe('L-01 (喷气 230)')
      expect(rows[1].label).toBe('L-02 (剑杆 190)')
      expect(rows[2].label).toBe('L-03')
      expect(rows[2].bars).toHaveLength(0) // 空行
    })

    it('无日期任务不渲染在甘特图中', () => {
      const rows = buildGanttRows(mockLooms, mockBars)
      const loom1Bars = rows[0].bars

      // mockBars 中 loomId=1 有 3 条，但 105 号任务无日期，甘特行仅应有 2 条
      expect(loom1Bars).toHaveLength(2)
      expect(loom1Bars.find((b) => b.taskId === 105)).toBeUndefined()
    })

    it('单边日期任务自动归一为起止同天的单日甘特条', () => {
      const singleDateBars: GanttBarVO[] = [
        {
          taskId: 106,
          taskCode: 'MT-WO20261006-01',
          loomId: 1,
          weavingOrderId: 15,
          weavingOrderCode: 'WO20261006',
          productCode: 'P06',
          productName: '单面布',
          plannedQty: 10,
          actualQty: 0,
          plannedStart: '2026-10-09',
          plannedEnd: undefined,
          status: 'PENDING',
          conflict: false
        }
      ]
      const rows = buildGanttRows(mockLooms, singleDateBars)
      const loom1Bars = rows[0].bars
      expect(loom1Bars).toHaveLength(1)
      expect(loom1Bars[0].startDate).toBe('2026-10-09')
      expect(loom1Bars[0].endDate).toBe('2026-10-09')
    })

    it('条带文字包含织造单尾号与计划匹数，状态色与冲突样式正确', () => {
      const rows = buildGanttRows(mockLooms, mockBars)
      const barInProgress = rows[0].bars.find((b) => b.taskId === 101)!
      const barConflict = rows[0].bars.find((b) => b.taskId === 102)!

      expect(barInProgress.ganttBarConfig.label).toBe('1001 / 30匹')
      expect(barInProgress.ganttBarConfig.hasHandles).toBe(true)
      expect(barInProgress.ganttBarConfig.immobile).toBe(false)
      expect(barInProgress.ganttBarConfig.class).toContain('status-in-progress')
      expect(barInProgress.ganttBarConfig.class).not.toContain('is-conflict')

      expect(barConflict.ganttBarConfig.label).toBe('1002 / 25匹')
      expect(barConflict.ganttBarConfig.class).toContain('status-pending')
      expect(barConflict.ganttBarConfig.class).toContain('is-conflict')
    })

    it('HALTED 和 COMPLETED 条带禁用拖拽且无手柄', () => {
      const rows = buildGanttRows(mockLooms, mockBars)
      const barHalted = rows[1].bars.find((b) => b.taskId === 103)!
      const barCompleted = rows[1].bars.find((b) => b.taskId === 104)!

      expect(barHalted.ganttBarConfig.immobile).toBe(true)
      expect(barHalted.ganttBarConfig.hasHandles).toBe(false)
      expect(barHalted.ganttBarConfig.class).toContain('status-halted')
      expect(barHalted.ganttBarConfig.class).toContain('is-immobile')

      expect(barCompleted.ganttBarConfig.immobile).toBe(true)
      expect(barCompleted.ganttBarConfig.hasHandles).toBe(false)
      expect(barCompleted.ganttBarConfig.class).toContain('status-completed')
      expect(barCompleted.ganttBarConfig.class).toContain('is-immobile')
    })
  })

  describe('formatBarLabel', () => {
    it('正确截取织造单后4位并拼接匹数', () => {
      expect(formatBarLabel('WO20261001', 30)).toBe('1001 / 30匹')
      expect(formatBarLabel('ZZ-PB0919001-01', 12)).toBe('1-01 / 12匹')
      expect(formatBarLabel('', 10)).toBe('10匹')
    })
  })

  describe('optimisticBarUpdate', () => {
    it('拖拽改期成功时，保持新日期并返回成功与警告信息', async () => {
      const rows = buildGanttRows(mockLooms, mockBars)
      const bar = rows[0].bars[0]
      const oldStart = bar.startDate
      const oldEnd = bar.endDate

      const mockApiUpdate = vi.fn().mockResolvedValue({
        id: bar.taskId,
        code: bar.taskCode,
        warning: '注意：已接近织机最大负荷'
      })

      const result = await optimisticBarUpdate({
        bar,
        newStart: '2026-10-07',
        newEnd: '2026-10-12',
        oldStart,
        oldEnd,
        apiUpdate: mockApiUpdate
      })

      expect(result.success).toBe(true)
      expect(result.warning).toBe('注意：已接近织机最大负荷')
      expect(bar.startDate).toBe('2026-10-07')
      expect(bar.endDate).toBe('2026-10-12')
      expect(mockApiUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          id: bar.taskId,
          plannedStart: '2026-10-07',
          plannedEnd: '2026-10-12',
          workerUserId: 99,
          workerName: '张三'
        })
      )
    })

    it('制造冲突或后端拦截(000)失败时，自动回滚条带位置并返回错误消息', async () => {
      const rows = buildGanttRows(mockLooms, mockBars)
      const bar = rows[0].bars[0]
      const originalStart = '2026-10-05'
      const originalEnd = '2026-10-10'

      const conflictError: any = new Error(
        '织机【L-01】在 2026-10-08~2026-10-14 已排任务【MT-WO20261002-01】，计划时段冲突，请改期或转机'
      )
      conflictError.code = 1030036000

      const mockApiUpdate = vi.fn().mockRejectedValue(conflictError)

      const result = await optimisticBarUpdate({
        bar,
        newStart: '2026-10-08',
        newEnd: '2026-10-13',
        oldStart: originalStart,
        oldEnd: originalEnd,
        apiUpdate: mockApiUpdate
      })

      // 验证回滚
      expect(result.success).toBe(false)
      expect(result.rolledBack).toBe(true)
      expect(result.error).toContain('计划时段冲突')
      expect(result.code).toBe(1030036000)
      expect(bar.startDate).toBe(originalStart)
      expect(bar.endDate).toBe(originalEnd)
    })
  })
})
