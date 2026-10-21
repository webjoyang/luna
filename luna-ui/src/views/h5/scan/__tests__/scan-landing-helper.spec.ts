import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import {
  resolveScanLanding,
  getSavedScanLandingChoice,
  setSavedScanLandingChoice,
  resetSavedScanLandingChoice,
  STORAGE_KEY_SCAN_LANDING
} from '../scan-landing-helper'

class MemoryStorage implements Storage {
  private store = new Map<string, string>()

  get length(): number {
    return this.store.size
  }

  clear(): void {
    this.store.clear()
  }

  getItem(key: string): string | null {
    return this.store.get(key) ?? null
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null
  }

  removeItem(key: string): void {
    this.store.delete(key)
  }

  setItem(key: string, value: string): void {
    this.store.set(key, String(value))
  }
}

describe('scan-landing-helper', () => {
  let memoryStorage: MemoryStorage

  beforeEach(() => {
    memoryStorage = new MemoryStorage()
    ;(globalThis as any).localStorage = memoryStorage
  })

  afterEach(() => {
    delete (globalThis as any).localStorage
  })

  describe('resolveScanLanding', () => {
    it('当用户无报工且无质检权限时，应分流至 /h5/403', () => {
      const decision = resolveScanLanding({
        code: 'BT-001',
        canReport: false,
        canInspect: false
      })
      expect(decision).toEqual({
        action: 'forbidden',
        target: '/h5/403'
      })
    })

    it('当用户仅拥有报工权限时，应直接分流至 /h5/ticket', () => {
      const decision = resolveScanLanding({
        code: 'BT-001',
        canReport: true,
        canInspect: false
      })
      expect(decision).toEqual({
        action: 'redirect',
        target: '/h5/ticket',
        query: { code: 'BT-001' }
      })
    })

    it('当用户仅拥有质检权限时，应直接分流至 /h5/inspection', () => {
      const decision = resolveScanLanding({
        code: 'BT-002',
        canReport: false,
        canInspect: true
      })
      expect(decision).toEqual({
        action: 'redirect',
        target: '/h5/inspection',
        query: { code: 'BT-002' }
      })
    })

    it('双角色且未曾选择时，应触发 choose ActionSheet 交互', () => {
      const decision = resolveScanLanding({
        code: 'BT-003',
        canReport: true,
        canInspect: true,
        savedChoice: null
      })
      expect(decision).toEqual({
        action: 'choose'
      })
    })

    it('双角色且上次选择报工(report)时，应自动分流至 /h5/ticket', () => {
      const decision = resolveScanLanding({
        code: 'BT-004',
        canReport: true,
        canInspect: true,
        savedChoice: 'report'
      })
      expect(decision).toEqual({
        action: 'redirect',
        target: '/h5/ticket',
        query: { code: 'BT-004' }
      })
    })

    it('双角色且上次选择质检(inspection)时，应自动分流至 /h5/inspection', () => {
      const decision = resolveScanLanding({
        code: 'BT-005',
        canReport: true,
        canInspect: true,
        savedChoice: 'inspection'
      })
      expect(decision).toEqual({
        action: 'redirect',
        target: '/h5/inspection',
        query: { code: 'BT-005' }
      })
    })

    it('应自动去除 code 首尾空白', () => {
      const decision = resolveScanLanding({
        code: '  BT-TRIM-001  ',
        canReport: true,
        canInspect: false
      })
      expect(decision).toEqual({
        action: 'redirect',
        target: '/h5/ticket',
        query: { code: 'BT-TRIM-001' }
      })
    })
  })

  describe('localStorage 偏好记忆与重置', () => {
    it('初次读取应返回 null', () => {
      expect(getSavedScanLandingChoice()).toBeNull()
    })

    it('保存报工偏好后读取应返回 report', () => {
      setSavedScanLandingChoice('report')
      expect(localStorage.getItem(STORAGE_KEY_SCAN_LANDING)).toBe('report')
      expect(getSavedScanLandingChoice()).toBe('report')
    })

    it('保存质检偏好后读取应返回 inspection', () => {
      setSavedScanLandingChoice('inspection')
      expect(localStorage.getItem(STORAGE_KEY_SCAN_LANDING)).toBe('inspection')
      expect(getSavedScanLandingChoice()).toBe('inspection')
    })

    it('调用 resetSavedScanLandingChoice 应成功清除缓存', () => {
      setSavedScanLandingChoice('report')
      expect(getSavedScanLandingChoice()).toBe('report')

      resetSavedScanLandingChoice()
      expect(getSavedScanLandingChoice()).toBeNull()
      expect(localStorage.getItem(STORAGE_KEY_SCAN_LANDING)).toBeNull()
    })
  })
})
