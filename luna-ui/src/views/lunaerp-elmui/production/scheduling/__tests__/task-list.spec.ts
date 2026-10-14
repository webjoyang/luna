import { describe, it, expect, vi } from 'vitest'
import {
  buildTaskPageParams,
  getTaskRowActions,
  buildWeavingOrderWorkspacePath,
  formatTaskRow,
  submitTaskEdit,
  type SharedFilterState,
  type TaskListLocalFilter,
  type PaginationState
} from '../model/task-list'
import type {
  MachineTaskVO,
  MachineTaskSaveReqVO,
  MachineTaskSaveRespVO
} from '@/api/lunaerp-elmui/production/machine-task'

describe('Seam 1 & 2 & 3: Task List Model & Logic (Issue 08)', () => {
  describe('Seam 1: Query Params & Pagination Adapter', () => {
    it('weavingOrderId 不限定也能跨单出数据，分页参数正确映射', () => {
      const shared: SharedFilterState = {
        loomIds: [],
        statuses: []
      }
      const local: TaskListLocalFilter = {
        conflictOnly: false
      }
      const pagination: PaginationState = {
        pageNo: 1,
        pageSize: 20
      }

      const params = buildTaskPageParams(shared, local, pagination)

      expect(params.pageNo).toBe(1)
      expect(params.pageSize).toBe(20)
      expect(params.weavingOrderId).toBeUndefined()
      expect(params.conflictOnly).toBeUndefined()
      expect(params.code).toBeUndefined()
      expect(params.loomIds).toBeUndefined()
      expect(params.statuses).toBeUndefined()
    })

    it('conflictOnly 只看冲突红标过滤生效', () => {
      const shared: SharedFilterState = {}
      const local: TaskListLocalFilter = {
        conflictOnly: true
      }
      const pagination: PaginationState = {
        pageNo: 2,
        pageSize: 10
      }

      const params = buildTaskPageParams(shared, local, pagination)

      expect(params.conflictOnly).toBe(true)
      expect(params.pageNo).toBe(2)
      expect(params.pageSize).toBe(10)
    })

    it('与甘特区共享顶部筛选：织机与状态正确映射，跨单列表不强制限制日期窗口保证未排期任务可展现', () => {
      const shared: SharedFilterState = {
        dateWindow: ['2026-10-01', '2026-10-31'],
        loomIds: [1, 2],
        statuses: ['PENDING', 'IN_PROGRESS']
      }
      const local: TaskListLocalFilter = {
        code: '  MT-001  ',
        conflictOnly: false
      }
      const pagination: PaginationState = {
        pageNo: 1,
        pageSize: 50
      }

      const params = buildTaskPageParams(shared, local, pagination)

      expect(params.loomIds).toEqual([1, 2])
      expect(params.statuses).toEqual(['PENDING', 'IN_PROGRESS'])
      expect(params.startDate).toBeUndefined()
      expect(params.endDate).toBeUndefined()
      expect(params.code).toBe('MT-001')
    })
  })

  describe('Seam 3: Row Actions & Guard Verification (spec §2.3)', () => {
    it('列表无删除和停机入口：canDelete 和 canHalt 恒为 false', () => {
      const pendingTask: MachineTaskVO = {
        id: 1,
        code: 'MT-01',
        status: 'PENDING',
        weavingOrderId: 100
      }
      const inProgressTask: MachineTaskVO = {
        id: 2,
        code: 'MT-02',
        status: 'IN_PROGRESS',
        weavingOrderId: 100
      }
      const haltedTask: MachineTaskVO = {
        id: 3,
        code: 'MT-03',
        status: 'HALTED',
        weavingOrderId: 100
      }
      const completedTask: MachineTaskVO = {
        id: 4,
        code: 'MT-04',
        status: 'COMPLETED',
        weavingOrderId: 100
      }

      for (const t of [pendingTask, inProgressTask, haltedTask, completedTask]) {
        const actions = getTaskRowActions(t)
        expect(actions.canDelete).toBe(false)
        expect(actions.canHalt).toBe(false)
      }
    })

    it('行内仅提供编辑与跳转织造单工作台：COMPLETED 只读不可编辑', () => {
      const pendingActions = getTaskRowActions({ id: 1, status: 'PENDING', weavingOrderId: 100 })
      expect(pendingActions.canEdit).toBe(true)
      expect(pendingActions.canJump).toBe(true)

      const completedActions = getTaskRowActions({
        id: 2,
        status: 'COMPLETED',
        weavingOrderId: 200
      })
      expect(completedActions.canEdit).toBe(false)
      expect(completedActions.canJump).toBe(true)
    })

    it('跳转织造单工作台路由格式正确', () => {
      expect(buildWeavingOrderWorkspacePath(1024)).toBe('/production/weaving-order/workspace/1024')
    })

    it('行内格式化正确展示产品快照、排期起止与冲突标记', () => {
      const row: MachineTaskVO = {
        id: 1,
        code: 'MT-01',
        weavingOrderCode: 'WO2026101201',
        productCode: 'P001',
        productName: '纯棉汗布',
        plannedStart: '2026-10-08',
        plannedEnd: '2026-10-12',
        conflict: true
      }

      const formatted = formatTaskRow(row)

      expect(formatted.productDisplay).toBe('[P001] 纯棉汗布')
      expect(formatted.scheduleDisplay).toBe('2026-10-08 ~ 2026-10-12')
      expect(formatted.isConflict).toBe(true)

      const undatedRow: MachineTaskVO = {
        id: 2,
        code: 'MT-02',
        conflict: false
      }
      const formattedUndated = formatTaskRow(undatedRow)
      expect(formattedUndated.scheduleDisplay).toBe('未排期')
      expect(formattedUndated.isConflict).toBe(false)
    })
  })

  describe('Seam 2: Edit Submission & Conflict Interception (000 Error Handling)', () => {
    it('编辑保存成功并保留超量软警告', async () => {
      const mockApi = vi.fn().mockResolvedValue({
        id: 10,
        code: 'MT-01',
        warning: '机台计划匹数总和（50匹）超出织造单计划匹数（40匹）'
      } as MachineTaskSaveRespVO)

      const editData: MachineTaskSaveReqVO = {
        id: 10,
        code: 'MT-01',
        weavingOrderId: 100,
        loomId: 2,
        plannedQty: 50,
        plannedStart: '2026-10-08',
        plannedEnd: '2026-10-15'
      }

      const result = await submitTaskEdit(editData, mockApi)

      expect(result.success).toBe(true)
      expect(result.warning).toBe('机台计划匹数总和（50匹）超出织造单计划匹数（40匹）')
      expect(result.isConflict).toBe(false)
    })

    it('编辑保存制造冲突被 000 拦截并红字回显冲突任务号', async () => {
      const conflictErr = {
        code: 1030036000,
        message:
          '织机【LM-01】在 2026-10-08~2026-10-12 已排任务【MT-02】，计划时段冲突，请改期或转机'
      }
      const mockApi = vi.fn().mockRejectedValue(conflictErr)

      const editData: MachineTaskSaveReqVO = {
        id: 10,
        code: 'MT-01',
        weavingOrderId: 100,
        loomId: 1,
        plannedQty: 20,
        plannedStart: '2026-10-08',
        plannedEnd: '2026-10-10'
      }

      const result = await submitTaskEdit(editData, mockApi)

      expect(result.success).toBe(false)
      expect(result.isConflict).toBe(true)
      expect(result.conflictMessage).toContain('MT-02')
      expect(result.conflictMessage).toContain('冲突')
    })
  })
})
