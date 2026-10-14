import { describe, it, expect, vi } from 'vitest'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn()
  }
}))

import {
  groupVatsByDate,
  formatYearMonth,
  getAdjacentMonth,
  formatDate,
  canRescheduleVat,
  getVatCalendarStatusStyle,
  buildRescheduleUpdateVO
} from '../model/vat-calendar'
import type { ProgressFeedbackCalendarVatVO } from '@/api/lunaerp-elmui/production/progress-feedback'

describe('Seam 1: Vat Calendar Model & Date Grouping (Issue 10)', () => {
  describe('groupVatsByDate', () => {
    it('按 scheduledDate 将扁平缸列表聚合成日期分组并按 vatNo 排序', () => {
      const mockVats: ProgressFeedbackCalendarVatVO[] = [
        {
          feedbackId: 1,
          dyeingOrderId: 10,
          dyeingColorId: 101,
          vatNo: 'B-02',
          scheduledDate: '2026-10-15',
          status: 'DYEING',
          quantity: 200,
          unit: 'KG'
        },
        {
          feedbackId: 2,
          dyeingOrderId: 10,
          dyeingColorId: 101,
          vatNo: 'A-01',
          scheduledDate: '2026-10-15',
          status: 'SCHEDULED',
          quantity: 300,
          unit: 'KG'
        },
        {
          feedbackId: 3,
          dyeingOrderId: 11,
          dyeingColorId: 102,
          vatNo: 'C-01',
          scheduledDate: '2026-10-16',
          status: 'COMPLETED',
          quantity: 150,
          unit: 'KG'
        }
      ]

      const grouped = groupVatsByDate(mockVats)

      expect(Object.keys(grouped)).toHaveLength(2)
      expect(grouped['2026-10-15']).toHaveLength(2)
      // 缸号字母升序排序：A-01 在前，B-02 在后
      expect(grouped['2026-10-15'][0].vatNo).toBe('A-01')
      expect(grouped['2026-10-15'][1].vatNo).toBe('B-02')

      expect(grouped['2026-10-16']).toHaveLength(1)
      expect(grouped['2026-10-16'][0].vatNo).toBe('C-01')
    })

    it('对空列表或未排日期的缸安全处理，不产生未定义键或崩溃', () => {
      expect(groupVatsByDate([])).toEqual({})

      const vatsWithoutDate: ProgressFeedbackCalendarVatVO[] = [
        {
          feedbackId: 4,
          dyeingOrderId: 12,
          dyeingColorId: 103,
          vatNo: 'D-01',
          scheduledDate: '',
          status: 'SCHEDULED',
          quantity: 100
        }
      ]
      const grouped = groupVatsByDate(vatsWithoutDate)
      expect(Object.keys(grouped)).toHaveLength(0)
    })
  })

  describe('Month Navigation & Date Formatting', () => {
    it('formatYearMonth 正确将 Date 对象格式化为 YYYY-MM', () => {
      const d = new Date(2026, 9, 15) // Month index 9 is October
      expect(formatYearMonth(d)).toBe('2026-10')
    })

    it('formatDate 正确将 Date 对象格式化为 YYYY-MM-DD', () => {
      const d = new Date(2026, 9, 5)
      expect(formatDate(d)).toBe('2026-10-05')
    })

    it('getAdjacentMonth 能够正确增减月份并处理年份进位/退位边界', () => {
      // 正常月度增减
      expect(getAdjacentMonth('2026-10', 1)).toBe('2026-11')
      expect(getAdjacentMonth('2026-10', -1)).toBe('2026-09')

      // 年份跨越进位
      expect(getAdjacentMonth('2026-12', 1)).toBe('2027-01')

      // 年份跨越退位
      expect(getAdjacentMonth('2026-01', -1)).toBe('2025-12')
    })
  })
})

describe('Seam 2: Status Styling & Reschedule Guard (Issue 10)', () => {
  describe('canRescheduleVat', () => {
    it('COMPLETED 状态缸禁止前端改期', () => {
      expect(canRescheduleVat('COMPLETED')).toBe(false)
    })

    it('未完成状态（SCHEDULED/DYEING/FIXING/QUALITY_CHECK）允许改期', () => {
      expect(canRescheduleVat('SCHEDULED')).toBe(true)
      expect(canRescheduleVat('DYEING')).toBe(true)
      expect(canRescheduleVat('FIXING')).toBe(true)
      expect(canRescheduleVat('QUALITY_CHECK')).toBe(true)
    })
  })

  describe('getVatCalendarStatusStyle', () => {
    it('COMPLETED 状态返回灰化样式与 isCompleted 标记', () => {
      const style = getVatCalendarStatusStyle('COMPLETED')
      expect(style.isCompleted).toBe(true)
      expect(style.tagType).toBe('info')
      expect(style.color).toBe('#909399')
      expect(style.bg).toBe('#f4f4f5')
    })

    it('SCHEDULED 状态返回 primary 字典色', () => {
      const style = getVatCalendarStatusStyle('SCHEDULED')
      expect(style.isCompleted).toBe(false)
      expect(style.tagType).toBe('primary')
      expect(style.color).toBe('#409eff')
    })

    it('DYEING/FIXING/QUALITY_CHECK 状态返回 warning 字典色', () => {
      const dyeingStyle = getVatCalendarStatusStyle('DYEING')
      expect(dyeingStyle.isCompleted).toBe(false)
      expect(dyeingStyle.tagType).toBe('warning')

      const fixingStyle = getVatCalendarStatusStyle('FIXING')
      expect(fixingStyle.isCompleted).toBe(false)
      expect(fixingStyle.tagType).toBe('warning')

      const qcStyle = getVatCalendarStatusStyle('QUALITY_CHECK')
      expect(qcStyle.isCompleted).toBe(false)
      expect(qcStyle.tagType).toBe('warning')
    })
  })

  describe('buildRescheduleUpdateVO', () => {
    const baseDetail = {
      id: 88,
      dyeingOrderId: 10,
      dyeingColorId: 101,
      vatNo: 'VAT-2026-01',
      quantity: 500,
      pieces: 20,
      status: 'SCHEDULED',
      scheduledDate: '2026-10-10',
      dyeingStartedDate: undefined,
      fixingStartedDate: undefined,
      qualityCheckDate: undefined,
      completedDate: undefined,
      delayReason: '染化料在途',
      remark: '急单优先'
    }

    it('正确生成改期 payload 并保留既有工序日期与备注，同时更新 scheduledDate', () => {
      const payload = buildRescheduleUpdateVO(baseDetail, '2026-10-18')
      expect(payload.id).toBe(88)
      expect(payload.vatNo).toBe('VAT-2026-01')
      expect(payload.quantity).toBe(500)
      expect(payload.pieces).toBe(20)
      expect(payload.scheduledDate).toBe('2026-10-18')
      expect(payload.delayReason).toBe('染化料在途')
      expect(payload.remark).toBe('急单优先')
    })

    it('对 COMPLETED 缸调用改期构造抛出错误阻止非法提交', () => {
      const completedDetail = { ...baseDetail, status: 'COMPLETED' }
      expect(() => buildRescheduleUpdateVO(completedDetail, '2026-10-20')).toThrowError(
        '已完成缸禁止改期'
      )
    })
  })
})

describe('Seam 3: Calendar API Layer Integration (spec §5.1, §7)', () => {
  it('getProgressFeedbackCalendar 发起 GET 请求至 /lunaerp/production/progress-feedback/calendar', async () => {
    const request = (await import('@/config/axios')).default
    const { getProgressFeedbackCalendar } = await import(
      '@/api/lunaerp-elmui/production/progress-feedback'
    )

    const params = {
      month: '2026-10',
      dyeingFactoryId: 5,
      status: 'DYEING'
    }
    await getProgressFeedbackCalendar(params)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/progress-feedback/calendar',
      params
    })
  })
})

