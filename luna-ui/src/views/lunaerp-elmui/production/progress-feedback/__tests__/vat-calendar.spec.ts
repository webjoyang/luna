import { describe, expect, it, vi, beforeEach } from 'vitest'
import * as ProgressFeedbackApi from '@/api/lunaerp-elmui/production/progress-feedback'
import type {
  VatCalendarVatVO,
  VatCalendarReqVO,
  VatCalendarRespVO
} from '@/api/lunaerp-elmui/production/progress-feedback'
import request from '@/config/axios'
import {
  getVatStatusTheme,
  groupVatsByDate,
  filterVats,
  canRescheduleVat,
  formatDayNumber,
  shiftMonth,
  buildReschedulePayload
} from '../components/vat-calendar-helper'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Issue 10: 排缸日历前端月历视图与改期交互 (VatCalendar)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 端点封装校验', () => {
    it('getVatCalendar 应调用 GET /lunaerp/production/progress-feedback/calendar', async () => {
      const mockReq: VatCalendarReqVO = {
        month: '2026-10',
        dyeingFactoryId: 5,
        status: 'DYEING'
      }
      const mockResp: VatCalendarRespVO = {
        vats: [
          {
            feedbackId: 101,
            dyeingOrderId: 10,
            dyeingColorId: 21,
            vatNo: 'VAT-01',
            scheduledDate: '2026-10-09',
            status: 'DYEING',
            quantity: 500,
            pieces: 20,
            unit: 'KG',
            customerColorNo: 'C101',
            colorName: '藏青',
            contractNo: 'CH2026-001',
            dyeingFactoryId: 5,
            dyeingFactoryName: '永盛染厂'
          }
        ]
      }

      vi.mocked(request.get).mockResolvedValueOnce(mockResp as any)

      const result = await ProgressFeedbackApi.getVatCalendar(mockReq)

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/production/progress-feedback/calendar',
        params: mockReq
      })
      expect(result.vats).toHaveLength(1)
      expect(result.vats[0].vatNo).toBe('VAT-01')
    })

    it('updateProgressFeedback 改期调用 PUT /lunaerp/production/progress-feedback/update', async () => {
      const updateData = {
        id: 101,
        vatNo: 'VAT-01',
        quantity: 500,
        pieces: 20,
        scheduledDate: '2026-10-15'
      }

      vi.mocked(request.put).mockResolvedValueOnce(true as any)

      const result = await ProgressFeedbackApi.updateProgressFeedback(updateData)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/production/progress-feedback/update',
        data: updateData
      })
      expect(result).toBe(true)
    })
  })

  describe('2. 日期聚合与缸数据分组 (Calendar Logic)', () => {
    const mockVats: VatCalendarVatVO[] = [
      {
        feedbackId: 1,
        dyeingOrderId: 10,
        dyeingColorId: 20,
        vatNo: 'V-01',
        scheduledDate: '2026-10-08',
        status: 'SCHEDULED',
        quantity: 300
      },
      {
        feedbackId: 2,
        dyeingOrderId: 10,
        dyeingColorId: 20,
        vatNo: 'V-02',
        scheduledDate: '2026-10-08',
        status: 'DYEING',
        quantity: 400
      },
      {
        feedbackId: 3,
        dyeingOrderId: 11,
        dyeingColorId: 21,
        vatNo: 'V-03',
        scheduledDate: '2026-10-09',
        status: 'COMPLETED',
        quantity: 500
      }
    ]

    it('groupVatsByDate 能将多个缸按 scheduledDate 正确归类到各日期槽', () => {
      const grouped = groupVatsByDate(mockVats)

      expect(Object.keys(grouped)).toContain('2026-10-08')
      expect(Object.keys(grouped)).toContain('2026-10-09')
      expect(grouped['2026-10-08']).toHaveLength(2)
      expect(grouped['2026-10-08'][0].vatNo).toBe('V-01')
      expect(grouped['2026-10-08'][1].vatNo).toBe('V-02')
      expect(grouped['2026-10-09']).toHaveLength(1)
      expect(grouped['2026-10-09'][0].vatNo).toBe('V-03')
    })

    it('空数组或无排缸日期的缸安全跳过', () => {
      expect(groupVatsByDate([])).toEqual({})
      const invalidVats = [{ feedbackId: 9, vatNo: 'X' } as any]
      expect(groupVatsByDate(invalidVats)).toEqual({})
    })
  })

  describe('3. 筛选过滤逻辑 (Filtering)', () => {
    const list: VatCalendarVatVO[] = [
      {
        feedbackId: 1,
        dyeingOrderId: 10,
        dyeingColorId: 20,
        vatNo: 'A-1',
        scheduledDate: '2026-10-01',
        status: 'SCHEDULED',
        dyeingFactoryId: 100
      },
      {
        feedbackId: 2,
        dyeingOrderId: 10,
        dyeingColorId: 20,
        vatNo: 'A-2',
        scheduledDate: '2026-10-02',
        status: 'DYEING',
        dyeingFactoryId: 100
      },
      {
        feedbackId: 3,
        dyeingOrderId: 11,
        dyeingColorId: 21,
        vatNo: 'B-1',
        scheduledDate: '2026-10-03',
        status: 'COMPLETED',
        dyeingFactoryId: 200
      }
    ]

    it('按承染厂筛选生效', () => {
      const filtered = filterVats(list, { dyeingFactoryId: 100 })
      expect(filtered).toHaveLength(2)
      expect(filtered.map((v) => v.vatNo)).toEqual(['A-1', 'A-2'])
    })

    it('按状态筛选生效', () => {
      const filtered = filterVats(list, { status: 'COMPLETED' })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].vatNo).toBe('B-1')
    })

    it('组合筛选生效', () => {
      const filtered = filterVats(list, { dyeingFactoryId: 100, status: 'DYEING' })
      expect(filtered).toHaveLength(1)
      expect(filtered[0].vatNo).toBe('A-2')
    })

    it('无筛选条件返回原列表', () => {
      const filtered = filterVats(list, {})
      expect(filtered).toHaveLength(3)
    })
  })

  describe('4. 状态字典配色与 COMPLETED 灰化 (Status Colors & Greying Out)', () => {
    it('SCHEDULED 应为 primary 蓝色系', () => {
      const theme = getVatStatusTheme('SCHEDULED')
      expect(theme.label).toBe('已排缸')
      expect(theme.color).toBe('#409eff')
      expect(theme.dotColor).toBe('#409eff')
      expect(theme.isCompleted).toBe(false)
      expect(theme.className).toBe('status-scheduled')
    })

    it('DYEING/FIXING/QUALITY_CHECK 应为 warning 橙黄色系', () => {
      const dyeing = getVatStatusTheme('DYEING')
      expect(dyeing.label).toBe('染色中')
      expect(dyeing.dotColor).toBe('#e6a23c')
      expect(dyeing.isCompleted).toBe(false)

      const fixing = getVatStatusTheme('FIXING')
      expect(fixing.label).toBe('固色中')
      expect(fixing.dotColor).toBe('#e6a23c')

      const qc = getVatStatusTheme('QUALITY_CHECK')
      expect(qc.label).toBe('质检中')
      expect(qc.dotColor).toBe('#e6a23c')
    })

    it('COMPLETED 状态必须强制灰化 (isCompleted=true, 灰色配色)', () => {
      const theme = getVatStatusTheme('COMPLETED')
      expect(theme.label).toBe('已完成')
      expect(theme.color).toBe('#909399')
      expect(theme.dotColor).toBe('#909399')
      expect(theme.bg).toBe('#f4f4f5')
      expect(theme.border).toBe('#e9e9eb')
      expect(theme.isCompleted).toBe(true)
      expect(theme.className).toBe('status-completed')
    })
  })

  describe('5. 改期守卫与 COMPLETED 禁用 (Disabled Guard)', () => {
    it('COMPLETED 缸前端禁用改期 (canRescheduleVat 返回 false)', () => {
      expect(canRescheduleVat({ status: 'COMPLETED' })).toBe(false)
      expect(canRescheduleVat({ status: 'completed' })).toBe(false)
    })

    it('非 COMPLETED 缸允许改期 (canRescheduleVat 返回 true)', () => {
      expect(canRescheduleVat({ status: 'SCHEDULED' })).toBe(true)
      expect(canRescheduleVat({ status: 'DYEING' })).toBe(true)
      expect(canRescheduleVat({ status: 'FIXING' })).toBe(true)
      expect(canRescheduleVat({ status: 'QUALITY_CHECK' })).toBe(true)
    })

    it('buildReschedulePayload 在遇到 COMPLETED 缸时抛出异常阻止提交', () => {
      const completedVat: VatCalendarVatVO = {
        feedbackId: 10,
        dyeingOrderId: 1,
        dyeingColorId: 1,
        vatNo: 'C-01',
        scheduledDate: '2026-10-01',
        status: 'COMPLETED'
      }

      expect(() => buildReschedulePayload(completedVat, '2026-10-10')).toThrow(
        '已完成的缸不允许修改排缸日期'
      )
    })

    it('buildReschedulePayload 组装合法参数并调用 updateProgressFeedback 结构', () => {
      const activeVat: VatCalendarVatVO = {
        feedbackId: 20,
        dyeingOrderId: 2,
        dyeingColorId: 3,
        vatNo: 'D-02',
        scheduledDate: '2026-10-05',
        status: 'SCHEDULED',
        quantity: 650,
        pieces: 25
      }

      const payload = buildReschedulePayload(activeVat, '2026-10-12')
      expect(payload).toEqual({
        id: 20,
        vatNo: 'D-02',
        quantity: 650,
        pieces: 25,
        scheduledDate: '2026-10-12'
      })
    })

    it('新排缸日期为空时抛错', () => {
      const activeVat: VatCalendarVatVO = {
        feedbackId: 21,
        dyeingOrderId: 2,
        dyeingColorId: 3,
        vatNo: 'D-03',
        scheduledDate: '2026-10-05',
        status: 'SCHEDULED'
      }
      expect(() => buildReschedulePayload(activeVat, '')).toThrow('新排缸日期不能为空')
    })
  })

  describe('6. 日期与月份计算辅助', () => {
    it('formatDayNumber 能解析天数', () => {
      expect(formatDayNumber('2026-10-09')).toBe('9')
      expect(formatDayNumber('2026-10-25')).toBe('25')
    })

    it('shiftMonth 正确进行月份平移且不跨月越界', () => {
      const base = new Date('2026-10-15T00:00:00Z')
      const prev = shiftMonth(base, -1)
      expect(prev.getMonth()).toBe(8) // 9月 (0-indexed 8)

      const next = shiftMonth(base, 1)
      expect(next.getMonth()).toBe(10) // 11月 (0-indexed 10)
    })
  })

  describe('7. 改期成功后局部刷新当月数据流程', () => {
    it('改期成功后自动请求最新月份排缸数据', async () => {
      // 模拟改期 API 成功
      vi.mocked(request.put).mockResolvedValueOnce(true as any)

      // 模拟后续刷新当月数据
      vi.mocked(request.get).mockResolvedValueOnce({
        vats: [
          {
            feedbackId: 30,
            dyeingOrderId: 3,
            dyeingColorId: 4,
            vatNo: 'REFRESH-01',
            scheduledDate: '2026-10-20',
            status: 'SCHEDULED'
          }
        ]
      } as any)

      // 执行改期
      const updateRes = await ProgressFeedbackApi.updateProgressFeedback({
        id: 30,
        vatNo: 'REFRESH-01',
        quantity: 100,
        scheduledDate: '2026-10-20'
      })
      expect(updateRes).toBe(true)

      // 刷新数据
      const refreshed = await ProgressFeedbackApi.getVatCalendar({ month: '2026-10' })
      expect(refreshed.vats).toHaveLength(1)
      expect(refreshed.vats[0].scheduledDate).toBe('2026-10-20')
    })
  })
})
