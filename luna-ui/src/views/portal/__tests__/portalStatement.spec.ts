import { describe, it, expect, vi } from 'vitest'

// Vitest 纯 Node 环境守卫：mock @/config/axios 防止路由依赖级联报错
vi.mock('@/config/axios', () => ({}))
vi.mock('@/config/axios/portal', () => ({}))

import {
  getStatementStatusText,
  getStatementStatusTagType,
  formatAmount,
  formatDateTime,
  resolveStatementDetailParams,
  filterStatementListByTab,
  canConfirmOrReject,
  validateConfirmForm,
  validateRejectForm
} from '../model/portalStatement'

describe('portalStatement model & utils', () => {
  describe('getStatementStatusText', () => {
    it('should return correct text for status', () => {
      expect(getStatementStatusText('SENT')).toBe('待确认')
      expect(getStatementStatusText('CONFIRMED')).toBe('已确认')
      expect(getStatementStatusText('REJECTED')).toBe('已驳回')
      expect(getStatementStatusText('DRAFT')).toBe('DRAFT')
      expect(getStatementStatusText(undefined)).toBe('未知')
    })
  })

  describe('getStatementStatusTagType', () => {
    it('should return correct tag type for status', () => {
      expect(getStatementStatusTagType('SENT')).toBe('warning')
      expect(getStatementStatusTagType('CONFIRMED')).toBe('success')
      expect(getStatementStatusTagType('REJECTED')).toBe('danger')
      expect(getStatementStatusTagType(undefined)).toBe('info')
    })
  })

  describe('formatAmount', () => {
    it('should format amount with currency symbol', () => {
      expect(formatAmount(1000)).toBe('¥1000.00')
      expect(formatAmount(50.5, 'USD')).toBe('$50.50')
      expect(formatAmount(0)).toBe('¥0.00')
      expect(formatAmount(null)).toBe('¥0.00')
      expect(formatAmount(undefined)).toBe('¥0.00')
      expect(formatAmount('200.75')).toBe('¥200.75')
    })
  })

  describe('formatDateTime', () => {
    it('should format date correctly', () => {
      expect(formatDateTime('2026-10-20T15:30:00')).toContain('2026-10-20')
      expect(formatDateTime(null)).toBe('-')
      expect(formatDateTime(undefined)).toBe('-')
    })
  })

  describe('resolveStatementDetailParams', () => {
    it('should resolve :id when routeParams.id is provided', () => {
      const res = resolveStatementDetailParams({ id: '1001' }, {})
      expect(res.isValid).toBe(true)
      expect(res.id).toBe(1001)
      expect(res.no).toBeUndefined()
    })

    it('should resolve ?no= when routeQuery.no is provided', () => {
      const res = resolveStatementDetailParams({}, { no: 'CS-20261020-001' })
      expect(res.isValid).toBe(true)
      expect(res.no).toBe('CS-20261020-001')
      expect(res.id).toBeUndefined()
    })

    it('should resolve ?statementNo= as alias for no', () => {
      const res = resolveStatementDetailParams({}, { statementNo: 'CS-20261020-002' })
      expect(res.isValid).toBe(true)
      expect(res.no).toBe('CS-20261020-002')
    })

    it('should prefer :id if both :id and ?no= are present', () => {
      const res = resolveStatementDetailParams({ id: '1001' }, { no: 'CS-20261020-001' })
      expect(res.isValid).toBe(true)
      expect(res.id).toBe(1001)
      expect(res.no).toBeUndefined()
    })

    it('should return isValid false when neither id nor no is present', () => {
      const res = resolveStatementDetailParams({}, {})
      expect(res.isValid).toBe(false)
    })
  })

  describe('filterStatementListByTab', () => {
    const list: PortalStatementRespVO[] = [
      { id: 1, statementNo: 'CS1', statementDate: '2026-10-01', status: 'SENT' },
      { id: 2, statementNo: 'CS2', statementDate: '2026-10-02', status: 'CONFIRMED' },
      { id: 3, statementNo: 'CS3', statementDate: '2026-10-03', status: 'REJECTED' }
    ]

    it('should filter by SENT tab', () => {
      const filtered = filterStatementListByTab(list, 'SENT')
      expect(filtered.length).toBe(1)
      expect(filtered[0].id).toBe(1)
    })

    it('should filter by CONFIRMED tab', () => {
      const filtered = filterStatementListByTab(list, 'CONFIRMED')
      expect(filtered.length).toBe(1)
      expect(filtered[0].id).toBe(2)
    })

    it('should filter by REJECTED tab', () => {
      const filtered = filterStatementListByTab(list, 'REJECTED')
      expect(filtered.length).toBe(1)
      expect(filtered[0].id).toBe(3)
    })
  })

  describe('canConfirmOrReject', () => {
    it('should return true only for SENT status', () => {
      expect(canConfirmOrReject('SENT')).toBe(true)
      expect(canConfirmOrReject('CONFIRMED')).toBe(false)
      expect(canConfirmOrReject('REJECTED')).toBe(false)
      expect(canConfirmOrReject(undefined)).toBe(false)
    })
  })

  describe('validateConfirmForm', () => {
    it('should require confirmByName', () => {
      expect(validateConfirmForm({ confirmByName: '' }).valid).toBe(false)
      expect(validateConfirmForm({ confirmByName: '  ' }).valid).toBe(false)
      expect(validateConfirmForm({ confirmByName: '张财务' }).valid).toBe(true)
    })
  })

  describe('validateRejectForm', () => {
    it('should require rejectReason', () => {
      expect(validateRejectForm({ rejectReason: '' }).valid).toBe(false)
      expect(validateRejectForm({ rejectReason: '  ' }).valid).toBe(false)
      expect(validateRejectForm({ rejectReason: '金额与订单不符' }).valid).toBe(true)
    })
  })
})
