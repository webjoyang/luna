import { describe, it, expect, vi } from 'vitest'

// AGENTS.md 纯 Node 环境守卫：若间接引到 axios 需顶层 mock
vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

import {
  getPortalStatusBadge,
  formatPortalRemainingDays,
  getCopyLinkPayload
} from '../model/portalToken'

describe('客户门户令牌前端领域模型测试 (portalToken.spec.ts)', () => {
  describe('状态标签映射 (getPortalStatusBadge)', () => {
    it('已开通状态返回 success 与 已开通', () => {
      const badge = getPortalStatusBadge(true)
      expect(badge.type).toBe('success')
      expect(badge.text).toBe('已开通')
    })

    it('未开通状态返回 info 与 未开通', () => {
      const badge = getPortalStatusBadge(false)
      expect(badge.type).toBe('info')
      expect(badge.text).toBe('未开通')
    })
  })

  describe('剩余天数格式化 (formatPortalRemainingDays)', () => {
    it('剩余正数天正常展示', () => {
      expect(formatPortalRemainingDays(30)).toBe('剩余 30 天')
      expect(formatPortalRemainingDays(1)).toBe('剩余 1 天')
    })

    it('0 或负数展示已过期', () => {
      expect(formatPortalRemainingDays(0)).toBe('已过期')
      expect(formatPortalRemainingDays(-1)).toBe('已过期')
    })

    it('未定义或空展示破折号', () => {
      expect(formatPortalRemainingDays(undefined)).toBe('-')
    })
  })

  describe('复制链接与基址未配置降级逻辑 (getCopyLinkPayload)', () => {
    it('未开通或无数据时不可复制', () => {
      const res1 = getCopyLinkPayload(null)
      expect(res1.canCopy).toBe(false)
      expect(res1.message).toBe('当前未生成有效门户链接')

      const res2 = getCopyLinkPayload({ exists: false })
      expect(res2.canCopy).toBe(false)
      expect(res2.message).toBe('当前未生成有效门户链接')
    })

    it('正常配置基址时返回完整 URL 且非降级', () => {
      const res = getCopyLinkPayload({
        exists: true,
        url: 'https://portal.lunaerp.com/portal/shipments?t=token123',
        token: 'token123'
      })
      expect(res.canCopy).toBe(true)
      expect(res.text).toBe('https://portal.lunaerp.com/portal/shipments?t=token123')
      expect(res.isDegraded).toBe(false)
      expect(res.message).toBe('门户链接已复制到剪贴板')
    })

    it('基址未配置时降级仅复制 token 并给出提示先配置基址', () => {
      const res = getCopyLinkPayload({
        exists: true,
        url: '', // 未配置基址
        token: 'plainToken456'
      })
      expect(res.canCopy).toBe(true)
      expect(res.text).toBe('plainToken456')
      expect(res.isDegraded).toBe(true)
      expect(res.message).toContain('luna.portal.base-url')
      expect(res.message).toContain('已复制令牌明文')
    })
  })
})
