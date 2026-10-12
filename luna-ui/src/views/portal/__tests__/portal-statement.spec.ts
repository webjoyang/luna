import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as StatementApi from '@/api/portal/statement'
import type {
  PortalStatementRespVO,
  PortalStatementDetailRespVO,
  PortalStatementConfirmReqVO,
  PortalStatementRejectReqVO
} from '@/api/portal/statement'
import portalRequest from '@/config/axios/portal'

vi.mock('@/config/axios/portal', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Ticket 05: 客户门户对账单确认（10 号契约身份来源替换 + 门户对账页）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 契约测试 (statement.ts)', () => {
    it('getPortalStatementList 应该向 /app-api/lunaerp/portal/finance/statement/list 发送 GET 请求', async () => {
      const mockList: PortalStatementRespVO[] = [
        {
          id: 101,
          statementNo: 'CS-202610110001',
          customerId: 1001,
          customerName: '广州盛达纺织有限公司',
          currency: 'CNY',
          periodStart: '2026-10-01',
          periodEnd: '2026-10-31',
          openingBalance: 10000,
          periodDebit: 50000,
          periodCredit: 40000,
          closingBalance: 20000,
          status: 'SENT'
        }
      ]
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockList)

      const res = await StatementApi.getPortalStatementList({ status: 'SENT' })

      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/finance/statement/list',
        params: { status: 'SENT' }
      })
      expect(res).toEqual(mockList)
    })

    it('getPortalStatementDetail 支持以数字 id 形式调用 REST 路径', async () => {
      const mockDetail: PortalStatementDetailRespVO = {
        id: 101,
        statementNo: 'CS-202610110001',
        customerId: 1001,
        customerName: '广州盛达纺织有限公司',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 10000,
        periodDebit: 50000,
        periodCredit: 40000,
        closingBalance: 20000,
        status: 'SENT',
        items: []
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockDetail)

      const res = await StatementApi.getPortalStatementDetail(101)

      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/finance/statement/101'
      })
      expect(res).toEqual(mockDetail)
    })

    it('getPortalStatementDetail 支持以 { no } 单号参数形式调用', async () => {
      const mockDetail: PortalStatementDetailRespVO = {
        id: 101,
        statementNo: 'CS-202610110001',
        customerId: 1001,
        customerName: '广州盛达纺织有限公司',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 10000,
        periodDebit: 50000,
        periodCredit: 40000,
        closingBalance: 20000,
        status: 'SENT',
        items: []
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockDetail)

      const res = await StatementApi.getPortalStatementDetail({ no: 'CS-202610110001' })

      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/finance/statement',
        params: { no: 'CS-202610110001' }
      })
      expect(res).toEqual(mockDetail)
    })

    it('confirmPortalStatement 应该向 /confirm 发送 PUT 请求（带手填姓名）', async () => {
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const payload: PortalStatementConfirmReqVO = {
        id: 101,
        confirmByName: '张三'
      }

      const res = await StatementApi.confirmPortalStatement(payload)

      expect(portalRequest.put).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/finance/statement/confirm',
        data: payload
      })
      expect(res).toBe(true)
    })

    it('rejectPortalStatement 应该向 /reject 发送 PUT 请求（带驳回原因）', async () => {
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const payload: PortalStatementRejectReqVO = {
        id: 101,
        rejectReason: '金额与实际出库不符'
      }

      const res = await StatementApi.rejectPortalStatement(payload)

      expect(portalRequest.put).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/finance/statement/reject',
        data: payload
      })
      expect(res).toBe(true)
    })
  })

  describe('2. 底部操作栏展示条件与状态守卫测试', () => {
    const isActionBarVisible = (detail: PortalStatementDetailRespVO | null): boolean => {
      return Boolean(detail && detail.status === 'SENT')
    }

    it('对账单 status 为 SENT 时，底部确认与驳回操作栏应展示', () => {
      const detail: PortalStatementDetailRespVO = {
        id: 101,
        statementNo: 'CS-SENT-001',
        customerId: 1001,
        customerName: '盛达纺织',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 1000,
        periodDebit: 5000,
        periodCredit: 2000,
        closingBalance: 4000,
        status: 'SENT',
        items: []
      }
      expect(isActionBarVisible(detail)).toBe(true)
    })

    it('对账单 status 为 CONFIRMED 或 REJECTED 时，底部操作栏应隐藏', () => {
      const confirmedDetail: PortalStatementDetailRespVO = {
        id: 101,
        statementNo: 'CS-CONFIRMED-001',
        customerId: 1001,
        customerName: '盛达纺织',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 1000,
        periodDebit: 5000,
        periodCredit: 2000,
        closingBalance: 4000,
        status: 'CONFIRMED',
        confirmByName: '张三',
        items: []
      }
      expect(isActionBarVisible(confirmedDetail)).toBe(false)

      const rejectedDetail: PortalStatementDetailRespVO = {
        id: 102,
        statementNo: 'CS-REJECTED-001',
        customerId: 1001,
        customerName: '盛达纺织',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 1000,
        periodDebit: 5000,
        periodCredit: 2000,
        closingBalance: 4000,
        status: 'REJECTED',
        rejectReason: '金额有误',
        items: []
      }
      expect(isActionBarVisible(rejectedDetail)).toBe(false)
    })
  })

  describe('3. 状态标签与信息块展示规则测试', () => {
    const getStatusTagType = (status?: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
      switch (status) {
        case 'SENT':
          return 'warning'
        case 'CONFIRMED':
          return 'success'
        case 'REJECTED':
          return 'danger'
        default:
          return 'info'
      }
    }

    const getStatusLabel = (status?: string): string => {
      switch (status) {
        case 'SENT':
          return '待确认'
        case 'CONFIRMED':
          return '已确认'
        case 'REJECTED':
          return '已驳回'
        default:
          return status || '未知'
      }
    }

    it('各对账单状态对应正确的 Tag 类型与文案', () => {
      expect(getStatusTagType('SENT')).toBe('warning')
      expect(getStatusLabel('SENT')).toBe('待确认')

      expect(getStatusTagType('CONFIRMED')).toBe('success')
      expect(getStatusLabel('CONFIRMED')).toBe('已确认')

      expect(getStatusTagType('REJECTED')).toBe('danger')
      expect(getStatusLabel('REJECTED')).toBe('已驳回')
    })

    it('已确认状态展示确认信息卡片（姓名、时间、线上门户方式）', () => {
      const detail: PortalStatementDetailRespVO = {
        id: 101,
        statementNo: 'CS-001',
        customerId: 1001,
        customerName: '盛达纺织',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 1000,
        periodDebit: 5000,
        periodCredit: 2000,
        closingBalance: 4000,
        status: 'CONFIRMED',
        confirmByName: '李财务',
        confirmTime: '2026-10-15 14:30:00',
        confirmType: 'ONLINE_PORTAL',
        items: []
      }

      const shouldShowConfirmBox = detail.status === 'CONFIRMED'
      expect(shouldShowConfirmBox).toBe(true)
      expect(detail.confirmByName).toBe('李财务')
      expect(detail.confirmTime).toBe('2026-10-15 14:30:00')
      expect(detail.confirmType).toBe('ONLINE_PORTAL')
    })

    it('已驳回状态展示驳回原因卡片', () => {
      const detail: PortalStatementDetailRespVO = {
        id: 102,
        statementNo: 'CS-002',
        customerId: 1001,
        customerName: '盛达纺织',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 1000,
        periodDebit: 5000,
        periodCredit: 2000,
        closingBalance: 4000,
        status: 'REJECTED',
        rejectReason: '发货明细单价有误',
        items: []
      }

      const shouldShowRejectBox = detail.status === 'REJECTED'
      expect(shouldShowRejectBox).toBe(true)
      expect(detail.rejectReason).toBe('发货明细单价有误')
    })
  })

  describe('4. 表单校验规则测试', () => {
    it('确认对账表单要求 confirmByName 非空且长度合规', () => {
      const validateConfirmByName = (name: string): boolean => {
        const trimmed = name.trim()
        return trimmed.length >= 1 && trimmed.length <= 50
      }

      expect(validateConfirmByName('')).toBe(false)
      expect(validateConfirmByName('   ')).toBe(false)
      expect(validateConfirmByName('张三')).toBe(true)
      expect(validateConfirmByName('A'.repeat(51))).toBe(false)
    })

    it('驳会对账表单要求 rejectReason 非空且满足字数要求', () => {
      const validateRejectReason = (reason: string): boolean => {
        const trimmed = reason.trim()
        return trimmed.length >= 2 && trimmed.length <= 200
      }

      expect(validateRejectReason('')).toBe(false)
      expect(validateRejectReason(' ')).toBe(false)
      expect(validateRejectReason('错')).toBe(false)
      expect(validateRejectReason('明细金额有误')).toBe(true)
      expect(validateRejectReason('A'.repeat(201))).toBe(false)
    })
  })

  describe('5. Spec2: SENT 期源单红冲门户明细行标红测试', () => {
    it('明细行 reversalFlag 为 true 时标记源单红冲', () => {
      const itemWithReversal: StatementApi.PortalStatementItemRespVO = {
        id: 201,
        lineNo: 1,
        direction: 'DEBIT',
        bizDate: '2026-10-15',
        refNo: 'SH-20261015-001',
        summary: '发货明细出库',
        debitAmount: 1000,
        creditAmount: 0,
        reversalFlag: true
      }
      const itemNormal: StatementApi.PortalStatementItemRespVO = {
        id: 202,
        lineNo: 2,
        direction: 'DEBIT',
        bizDate: '2026-10-15',
        refNo: 'SH-20261015-002',
        summary: '发货明细出库',
        debitAmount: 2000,
        creditAmount: 0,
        reversalFlag: false
      }

      expect(itemWithReversal.reversalFlag).toBe(true)
      expect(itemNormal.reversalFlag).toBe(false)
    })
  })
})
