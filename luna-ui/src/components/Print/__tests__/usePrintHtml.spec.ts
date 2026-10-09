import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { usePrintHtml } from '../composables/usePrintHtml'

describe('usePrintHtml Composable', () => {
  let originalDocument: any

  beforeEach(() => {
    originalDocument = (globalThis as any).document
  })

  afterEach(() => {
    if (originalDocument !== undefined) {
      ;(globalThis as any).document = originalDocument
    } else {
      delete (globalThis as any).document
    }
  })

  it('should create hidden iframe and trigger print', async () => {
    const mockPrint = vi.fn()
    const mockFocus = vi.fn()
    const mockOpen = vi.fn()
    const mockWrite = vi.fn()
    const mockClose = vi.fn()

    const fakeDoc = {
      open: mockOpen,
      write: mockWrite,
      close: mockClose,
      fonts: { ready: Promise.resolve() }
    }

    let appendedIframe: any = null
    const fakeBody = {
      contains: vi.fn((el) => el === appendedIframe),
      appendChild: vi.fn((el) => {
        appendedIframe = el
        return el
      })
    }

    const mockDocument = {
      body: fakeBody,
      createElement: vi.fn((tagName: string) => {
        if (tagName === 'iframe') {
          const el = {
            style: {},
            contentDocument: fakeDoc,
            contentWindow: {
              focus: mockFocus,
              print: mockPrint,
              document: fakeDoc
            }
          }
          return el
        }
        return {}
      })
    }

    ;(globalThis as any).document = mockDocument

    const { printHtml } = usePrintHtml()
    await printHtml('<div>测试打印内容</div>', {
      title: '测试布票标签',
      pageStyle: '@page { size: 100mm 60mm; margin: 0; }'
    })

    expect(mockDocument.createElement).toHaveBeenCalledWith('iframe')
    expect(fakeBody.appendChild).toHaveBeenCalled()
    expect(mockOpen).toHaveBeenCalled()
    expect(mockWrite).toHaveBeenCalled()
    expect(mockClose).toHaveBeenCalled()
    expect(mockFocus).toHaveBeenCalled()
    expect(mockPrint).toHaveBeenCalled()
  })

  it('printElement should extract innerHTML and call printHtml', async () => {
    const mockPrint = vi.fn()
    const mockFocus = vi.fn()
    const mockOpen = vi.fn()
    const mockWrite = vi.fn()
    const mockClose = vi.fn()

    const fakeDoc = {
      open: mockOpen,
      write: mockWrite,
      close: mockClose,
      fonts: { ready: Promise.resolve() }
    }

    let appendedIframe: any = null
    const fakeBody = {
      contains: vi.fn((el) => el === appendedIframe),
      appendChild: vi.fn((el) => {
        appendedIframe = el
        return el
      })
    }

    const mockDocument = {
      body: fakeBody,
      createElement: vi.fn((tagName: string) => {
        if (tagName === 'iframe') {
          return {
            style: {},
            contentDocument: fakeDoc,
            contentWindow: {
              focus: mockFocus,
              print: mockPrint,
              document: fakeDoc
            }
          }
        }
        return {}
      })
    }

    ;(globalThis as any).document = mockDocument

    const fakeEl = {
      innerHTML: '<span>明细行内容</span>'
    } as any

    const { printElement } = usePrintHtml()
    await printElement(fakeEl, { title: '元素测试' })

    expect(mockWrite).toHaveBeenCalledWith(expect.stringContaining('<span>明细行内容</span>'))
    expect(mockPrint).toHaveBeenCalled()
  })
})
