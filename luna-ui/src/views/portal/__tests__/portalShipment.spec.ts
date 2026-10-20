import { describe, it, expect } from 'vitest'
import {
  getSignStatusText,
  getSignStatusTagType,
  formatAmount,
  resolveShipmentDetailParams
} from '../model/portalShipment'

describe('portalShipment model & utils', () => {
  describe('getSignStatusText', () => {
    it('should return correct text for status', () => {
      expect(getSignStatusText('PENDING')).toBe('待签收')
      expect(getSignStatusText('SIGNED')).toBe('已签收')
      expect(getSignStatusText('REFUSED')).toBe('已拒收')
      expect(getSignStatusText('UNKNOWN')).toBe('UNKNOWN')
      expect(getSignStatusText(undefined)).toBe('未知')
    })
  })

  describe('getSignStatusTagType', () => {
    it('should return correct tag type for status', () => {
      expect(getSignStatusTagType('PENDING')).toBe('warning')
      expect(getSignStatusTagType('SIGNED')).toBe('success')
      expect(getSignStatusTagType('REFUSED')).toBe('danger')
      expect(getSignStatusTagType(undefined)).toBe('info')
    })
  })

  describe('formatAmount', () => {
    it('should format amounts correctly without masking', () => {
      expect(formatAmount(5000)).toBe('¥5000.00')
      expect(formatAmount(150.5)).toBe('¥150.50')
      expect(formatAmount('25.50')).toBe('¥25.50')
      expect(formatAmount(0)).toBe('¥0.00')
      expect(formatAmount(null)).toBe('¥0.00')
      expect(formatAmount(undefined)).toBe('¥0.00')
    })
  })

  describe('resolveShipmentDetailParams', () => {
    it('should resolve :id when present', () => {
      const res = resolveShipmentDetailParams({ id: '1024' }, {})
      expect(res.isValid).toBe(true)
      expect(res.id).toBe(1024)
      expect(res.no).toBeUndefined()
    })

    it('should resolve ?no= when :id is absent', () => {
      const res = resolveShipmentDetailParams({}, { no: 'SH-20260927-001' })
      expect(res.isValid).toBe(true)
      expect(res.no).toBe('SH-20260927-001')
      expect(res.id).toBeUndefined()
    })

    it('should prefer :id if both :id and ?no= are present', () => {
      const res = resolveShipmentDetailParams({ id: '1024' }, { no: 'SH-20260927-001' })
      expect(res.isValid).toBe(true)
      expect(res.id).toBe(1024)
    })

    it('should return isValid: false if neither is provided', () => {
      const res = resolveShipmentDetailParams({}, {})
      expect(res.isValid).toBe(false)
    })

    it('should handle whitespace in ?no=', () => {
      const res = resolveShipmentDetailParams({}, { no: '  SH-20260927-002  ' })
      expect(res.isValid).toBe(true)
      expect(res.no).toBe('SH-20260927-002')
    })
  })
})
