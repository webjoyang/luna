import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  getDraftKey,
  saveDraft,
  getDraft,
  clearDraft,
  hasDraft,
  getDraftPayload,
  useH5Draft
} from '../h5-draft'
import { ref } from 'vue'

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

const mockStorage = new MemoryStorage()
;(globalThis as any).localStorage = mockStorage
;(globalThis as any).window = { localStorage: mockStorage }

describe('h5-draft', () => {
  beforeEach(() => {
    mockStorage.clear()
    vi.restoreAllMocks()
  })

  describe('getDraftKey', () => {
    it('generates consistent key with namespace and code/id', () => {
      expect(getDraftKey('ticket-exception', 'WT20261021-001')).toBe(
        'luna:h5:draft:ticket-exception:WT20261021-001'
      )
      expect(getDraftKey('requisition', 12345)).toBe('luna:h5:draft:requisition:12345')
    })

    it('trims whitespace in namespace and key', () => {
      expect(getDraftKey(' report ', ' T001 ')).toBe('luna:h5:draft:report:T001')
    })
  })

  describe('saveDraft & getDraft', () => {
    it('saves form data to localStorage and retrieves it correctly', () => {
      const formData = {
        exceptionType: 'DEFECT_WEFT',
        note: '纬向断纱严重',
        photos: ['https://oss.example.com/p1.jpg', 'https://oss.example.com/p2.jpg']
      }

      const success = saveDraft('ticket-exception', 'WT001', formData)
      expect(success).toBe(true)

      const retrieved = getDraft('ticket-exception', 'WT001')
      expect(retrieved).toEqual(formData)
    })

    it('isolates drafts by different code/id under same namespace', () => {
      saveDraft('ticket-exception', 'WT001', { note: '单据一' })
      saveDraft('ticket-exception', 'WT002', { note: '单据二' })

      expect(getDraft('ticket-exception', 'WT001')).toEqual({ note: '单据一' })
      expect(getDraft('ticket-exception', 'WT002')).toEqual({ note: '单据二' })
    })

    it('isolates drafts by different namespace with same id', () => {
      saveDraft('report', '100', { status: 'REPORT' })
      saveDraft('inspection', '100', { status: 'INSPECTION' })

      expect(getDraft('report', '100')).toEqual({ status: 'REPORT' })
      expect(getDraft('inspection', '100')).toEqual({ status: 'INSPECTION' })
    })

    it('returns null when draft does not exist', () => {
      expect(getDraft('ticket-exception', 'NON_EXISTENT')).toBeNull()
    })

    it('returns null and removes key when storage contains corrupted JSON', () => {
      const key = getDraftKey('ticket-exception', 'CORRUPT')
      localStorage.setItem(key, 'invalid-json{}}')

      const result = getDraft('ticket-exception', 'CORRUPT')
      expect(result).toBeNull()
      expect(localStorage.getItem(key)).toBeNull()
    })

    it('returns false when namespace or key is empty', () => {
      expect(saveDraft('', 'WT001', { a: 1 })).toBe(false)
      expect(saveDraft('ticket-exception', '', { a: 1 })).toBe(false)
    })

    it('handles localStorage quota exceeded without throwing', () => {
      vi.spyOn(mockStorage, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError')
      })

      const success = saveDraft('ticket-exception', 'WT001', { data: 'test' })
      expect(success).toBe(false)
    })
  })

  describe('getDraftPayload', () => {
    it('returns payload including timestamp and original data', () => {
      const now = Date.now()
      saveDraft('requisition', 'REQ-99', { yarnCount: 30 })

      const payload = getDraftPayload('requisition', 'REQ-99')
      expect(payload).not.toBeNull()
      expect(payload?.namespace).toBe('requisition')
      expect(payload?.key).toBe('REQ-99')
      expect(payload?.data).toEqual({ yarnCount: 30 })
      expect(payload?.updatedAt).toBeGreaterThanOrEqual(now)
    })
  })

  describe('hasDraft & clearDraft', () => {
    it('correctly reports existence with hasDraft', () => {
      expect(hasDraft('inspection', 'INSP-1')).toBe(false)
      saveDraft('inspection', 'INSP-1', { score: 98 })
      expect(hasDraft('inspection', 'INSP-1')).toBe(true)
    })

    it('clears specific draft after submission', () => {
      saveDraft('inspection', 'INSP-1', { score: 98 })
      saveDraft('inspection', 'INSP-2', { score: 95 })

      const cleared = clearDraft('inspection', 'INSP-1')
      expect(cleared).toBe(true)
      expect(hasDraft('inspection', 'INSP-1')).toBe(false)
      expect(hasDraft('inspection', 'INSP-2')).toBe(true)
    })
  })

  describe('useH5Draft', () => {
    it('supports getter function as key and correctly resolves dynamic code/id', () => {
      const code = ref('WT-001')
      const form = ref({ note: '测试1' })

      const { saveDraft: save, loadDraft: load } = useH5Draft({
        namespace: 'ticket',
        key: () => code.value,
        formData: form,
        autoSave: false
      })

      save()
      expect(getDraft('ticket', 'WT-001')).toEqual({ note: '测试1' })

      // Change code
      code.value = 'WT-002'
      form.value = { note: '测试2' }
      save()

      expect(getDraft('ticket', 'WT-002')).toEqual({ note: '测试2' })
      expect(getDraft('ticket', 'WT-001')).toEqual({ note: '测试1' })

      // Reload draft for WT-001
      code.value = 'WT-001'
      load()
      expect(form.value.note).toBe('测试1')
    })
  })
})
