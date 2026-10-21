import { describe, it, expect, vi, beforeEach } from 'vitest'
import { NETWORK_ERROR_MESSAGE, isNetworkError, formatSubmitError, useH5Submit } from '../h5-submit'

describe('h5-submit', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  describe('NETWORK_ERROR_MESSAGE', () => {
    it('has exact required copy', () => {
      expect(NETWORK_ERROR_MESSAGE).toBe('网络异常，单据未提交，可点重试')
    })
  })

  describe('isNetworkError', () => {
    it('identifies axios Network Error as network error', () => {
      const err = new Error('Network Error')
      expect(isNetworkError(err)).toBe(true)
    })

    it('identifies timeout error as network error', () => {
      const err = { message: 'timeout of 30000ms exceeded', code: 'ECONNABORTED' }
      expect(isNetworkError(err)).toBe(true)
    })

    it('identifies axios error with no response as network error', () => {
      const err = {
        isAxiosError: true,
        request: {},
        response: undefined,
        message: 'Request failed'
      }
      expect(isNetworkError(err)).toBe(true)
    })

    it('identifies offline navigator as network error', () => {
      const originalOnLine = navigator.onLine
      Object.defineProperty(navigator, 'onLine', {
        value: false,
        configurable: true
      })

      const err = new Error('Something failed')
      expect(isNetworkError(err)).toBe(true)

      Object.defineProperty(navigator, 'onLine', {
        value: originalOnLine,
        configurable: true
      })
    })

    it('does NOT treat server business error (e.g. 015-111, 500) as network error', () => {
      const businessErr = {
        code: '015-111',
        message: '布票状态不正确，无法完工',
        response: { status: 500, data: { code: '015-111', msg: '布票状态不正确，无法完工' } }
      }
      expect(isNetworkError(businessErr)).toBe(false)
    })
  })

  describe('formatSubmitError', () => {
    it('formats network error with warning color and exact copy', () => {
      const err = new Error('Network Error')
      const formatted = formatSubmitError(err)

      expect(formatted.isNetwork).toBe(true)
      expect(formatted.type).toBe('warning')
      expect(formatted.message).toBe(NETWORK_ERROR_MESSAGE)
    })

    it('formats business error with danger color and original error message', () => {
      const err = { code: 400, message: '数量必须大于0' }
      const formatted = formatSubmitError(err)

      expect(formatted.isNetwork).toBe(false)
      expect(formatted.type).toBe('danger')
      expect(formatted.message).toBe('数量必须大于0')
    })

    it('extracts msg field if available on error object', () => {
      const err = { code: '039-008', msg: '照片最多只能上传6张' }
      const formatted = formatSubmitError(err)

      expect(formatted.message).toBe('照片最多只能上传6张')
      expect(formatted.type).toBe('danger')
    })
  })

  describe('useH5Submit anti-duplicate & execution', () => {
    it('prevents concurrent double clicks while submitting is in progress', async () => {
      let resolveSubmit: (val: any) => void
      const asyncWork = vi.fn().mockImplementation(() => {
        return new Promise((resolve) => {
          resolveSubmit = resolve
        })
      })

      const { submitting, submit } = useH5Submit(asyncWork, { showFeedback: false })

      expect(submitting.value).toBe(false)

      // First click
      const promise1 = submit('payload1')
      expect(submitting.value).toBe(true)
      expect(asyncWork).toHaveBeenCalledTimes(1)

      // Second immediate click while first is pending (防连点)
      const promise2 = submit('payload2')
      expect(asyncWork).toHaveBeenCalledTimes(1) // Still 1, blocked!
      expect(await promise2).toBeUndefined()

      // Resolve first
      resolveSubmit!({ success: true })
      const result1 = await promise1
      expect(result1).toEqual({ success: true })
      expect(submitting.value).toBe(false)

      // Third click after completion should work again
      asyncWork.mockResolvedValueOnce({ success: true, third: true })
      const result3 = await submit('payload3')
      expect(asyncWork).toHaveBeenCalledTimes(2)
      expect(result3).toEqual({ success: true, third: true })
    })

    it('calls clearDraft callback upon successful submit', async () => {
      const clearDraftMock = vi.fn()
      const submitFn = vi.fn().mockResolvedValue({ id: 1 })

      const { submit } = useH5Submit(submitFn, {
        showFeedback: false,
        onClearDraft: clearDraftMock
      })

      await submit()
      expect(clearDraftMock).toHaveBeenCalledTimes(1)
    })

    it('does NOT call clearDraft on submit failure', async () => {
      const clearDraftMock = vi.fn()
      const submitFn = vi.fn().mockRejectedValue(new Error('Network Error'))

      const { submit } = useH5Submit(submitFn, {
        showFeedback: false,
        onClearDraft: clearDraftMock
      })

      try {
        await submit()
      } catch {
        // expected
      }

      expect(clearDraftMock).not.toHaveBeenCalled()
    })
  })
})
