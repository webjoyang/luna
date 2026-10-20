import { describe, it, expect, beforeEach } from 'vitest'
import {
  PORTAL_TOKEN_STORAGE_KEY,
  PORTAL_CUSTOMER_NAME_KEY,
  getPortalToken,
  setPortalToken,
  clearPortalToken,
  getPortalCustomerName,
  setPortalCustomerName,
  handlePortalTokenGuide,
  isPortalPath,
  isPortalInvalidPath
} from '../utils/portalAuth'

describe('PortalAuth & Token Guide Tests', () => {
  beforeEach(() => {
    // 模拟纯 Node 下的 sessionStorage
    const storage = new Map<string, string>()
    globalThis.sessionStorage = {
      getItem: (key: string) => storage.get(key) || null,
      setItem: (key: string, val: string) => storage.set(key, val),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
      length: storage.size,
      key: () => null
    } as any
  })

  it('令牌保存、读取与清除正常工作', () => {
    expect(getPortalToken()).toBe('')

    setPortalToken('token_test_123')
    expect(getPortalToken()).toBe('token_test_123')
    expect(sessionStorage.getItem(PORTAL_TOKEN_STORAGE_KEY)).toBe('token_test_123')

    clearPortalToken()
    expect(getPortalToken()).toBe('')
    expect(sessionStorage.getItem(PORTAL_TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('客户名称保存、读取与清除正常工作', () => {
    expect(getPortalCustomerName()).toBe('')

    setPortalCustomerName('测试织造客户')
    expect(getPortalCustomerName()).toBe('测试织造客户')
    expect(sessionStorage.getItem(PORTAL_CUSTOMER_NAME_KEY)).toBe('测试织造客户')

    clearPortalToken()
    expect(getPortalCustomerName()).toBe('')
  })

  it('token 引导：URL query 带 ?t= 应该保存 token 并标记需要清理地址栏', () => {
    const res = handlePortalTokenGuide('url_token_999', null)
    expect(res.isValid).toBe(true)
    expect(res.token).toBe('url_token_999')
    expect(res.shouldCleanUrl).toBe(true)
    expect(getPortalToken()).toBe('url_token_999')
  })

  it('token 引导：URL query 无 ?t= 但 sessionStorage 已有时，不需要清理地址栏', () => {
    const res = handlePortalTokenGuide(null, 'stored_token_888')
    expect(res.isValid).toBe(true)
    expect(res.token).toBe('stored_token_888')
    expect(res.shouldCleanUrl).toBe(false)
  })

  it('token 引导：既无 query ?t= 也无 sessionStorage 时，判断为无效（需引导至 invalid 页）', () => {
    const res = handlePortalTokenGuide(null, null)
    expect(res.isValid).toBe(false)
    expect(res.token).toBeNull()
    expect(res.shouldCleanUrl).toBe(false)

    const resEmpty = handlePortalTokenGuide('  ', '  ')
    expect(resEmpty.isValid).toBe(false)
  })

  it('门户路径匹配与失效页判定', () => {
    expect(isPortalPath('/portal/shipments')).toBe(true)
    expect(isPortalPath('/portal/shipment/1')).toBe(true)
    expect(isPortalPath('/portal/statements')).toBe(true)
    expect(isPortalPath('/portal/invalid')).toBe(true)
    expect(isPortalPath('/admin-api/shipment')).toBe(false)

    expect(isPortalInvalidPath('/portal/invalid')).toBe(true)
    expect(isPortalInvalidPath('/portal/shipments')).toBe(false)
  })
})
