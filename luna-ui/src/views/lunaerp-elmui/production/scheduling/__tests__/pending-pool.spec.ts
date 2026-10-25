import { describe, it, expect, vi } from 'vitest'
import {
  createEmptyDispatchForm,
  initDispatchFromPendingOrder,
  initDispatchFromPendingTask,
  validateDispatchForm,
  submitDispatch,
  type DispatchFormData
} from '../model/pending-pool'
import type { PendingOrderVO, PendingTaskVO } from '@/api/lunaerp-elmui/production/scheduling'

describe('Seam 3: Pending Pool & Dispatch Form Logic', () => {
  describe('Form Initialization', () => {
    it('从未排满织造单（组二）点击派工：自动带入织造单ID与默认匹数=remainingQty', () => {
      const pendingOrder: PendingOrderVO = {
        weavingOrderId: 8,
        weavingOrderCode: 'WO2026100801',
        productCode: 'P002',
        productName: '双面布',
        plannedQty: 50,
        scheduledQty: 30,
        remainingQty: 20
      }

      const form = initDispatchFromPendingOrder(pendingOrder)

      expect(form.weavingOrderId).toBe(8)
      expect(form.weavingOrderCode).toBe('WO2026100801')
      expect(form.plannedQty).toBe(20) // remainingQty
      expect(form.loomId).toBeUndefined()
      expect(form.plannedStart).toBeUndefined()
      expect(form.plannedEnd).toBeUndefined()
    })

    it('从无排期任务（组一）点击派工：带入织造单与任务已有织机、匹数', () => {
      const pendingTask: PendingTaskVO = {
        taskId: 11,
        taskCode: 'MT-WO20261007-02',
        weavingOrderId: 7,
        weavingOrderCode: 'WO20261007',
        loomId: 1,
        loomNo: 'L-01',
        plannedQty: 10
      }

      const form = initDispatchFromPendingTask(pendingTask)

      expect(form.id).toBe(11)
      expect(form.code).toBe('MT-WO20261007-02')
      expect(form.weavingOrderId).toBe(7)
      expect(form.weavingOrderCode).toBe('WO20261007')
      expect(form.loomId).toBe(1)
      expect(form.loomNo).toBe('L-01')
      expect(form.plannedQty).toBe(10)
    })

    it('新建空白派工单默认计划匹数为 1', () => {
      const form = createEmptyDispatchForm()
      expect(form.plannedQty).toBe(1)
      expect(form.weavingOrderId).toBeUndefined()
      expect(form.loomId).toBeUndefined()
    })
  })

  describe('Form Validation', () => {
    it('校验必填项：缺少织造单或织机报错', () => {
      const form: DispatchFormData = {
        plannedQty: 10
      }
      const v1 = validateDispatchForm(form)
      expect(v1.valid).toBe(false)
      expect(v1.errorMsg).toContain('织造单')

      form.weavingOrderId = 8
      const v2 = validateDispatchForm(form)
      expect(v2.valid).toBe(false)
      expect(v2.errorMsg).toContain('织机')

      form.loomId = 1
      const v3 = validateDispatchForm(form)
      expect(v3.valid).toBe(true)
    })

    it('计划匹数必须为正整数', () => {
      const form: DispatchFormData = {
        weavingOrderId: 8,
        loomId: 1,
        plannedQty: 0
      }
      expect(validateDispatchForm(form).valid).toBe(false)

      form.plannedQty = -5
      expect(validateDispatchForm(form).valid).toBe(false)

      form.plannedQty = 15
      expect(validateDispatchForm(form).valid).toBe(true)
    })

    it('计划结束日期不能早于开始日期', () => {
      const form: DispatchFormData = {
        weavingOrderId: 8,
        loomId: 1,
        plannedQty: 10,
        plannedStart: '2026-10-15',
        plannedEnd: '2026-10-10'
      }
      const val = validateDispatchForm(form)
      expect(val.valid).toBe(false)
      expect(val.errorMsg).toContain('结束日期')
    })
  })

  describe('submitDispatch', () => {
    it('派工保存成功且无冲突：返回成功，支持超量软警告', async () => {
      const form: DispatchFormData = {
        weavingOrderId: 8,
        loomId: 1,
        plannedQty: 60,
        plannedStart: '2026-10-10',
        plannedEnd: '2026-10-15'
      }

      const mockCreate = vi.fn().mockResolvedValue({
        id: 105,
        code: 'MT-WO2026100801-01',
        warning: '警告：已超出织造单计划匹数 10 匹'
      })

      const res = await submitDispatch(form, mockCreate)

      expect(res.success).toBe(true)
      expect(res.id).toBe(105)
      expect(res.warning).toContain('超出织造单计划匹数')
      expect(mockCreate).toHaveBeenCalledWith(
        expect.objectContaining({
          weavingOrderId: 8,
          loomId: 1,
          plannedQty: 60,
          plannedStart: '2026-10-10',
          plannedEnd: '2026-10-15'
        })
      )
    })

    it('后端拦截 000 时段冲突：捕获冲突错误、保持弹窗打开标识、输出红字冲突信息', async () => {
      const form: DispatchFormData = {
        weavingOrderId: 8,
        loomId: 1,
        plannedQty: 20,
        plannedStart: '2026-10-10',
        plannedEnd: '2026-10-15'
      }

      const conflictErr: any = new Error(
        '织机【L-01】在 2026-10-08~2026-10-12 已排任务【MT-WO2026100701-01】，计划时段冲突，请改期或转机'
      )
      conflictErr.code = 1030036000

      const mockCreate = vi.fn().mockRejectedValue(conflictErr)

      const res = await submitDispatch(form, mockCreate)

      expect(res.success).toBe(false)
      expect(res.isConflict).toBe(true)
      expect(res.conflictMessage).toContain('计划时段冲突')
      expect(res.conflictMessage).toContain('MT-WO2026100701-01')
    })
  })
})
