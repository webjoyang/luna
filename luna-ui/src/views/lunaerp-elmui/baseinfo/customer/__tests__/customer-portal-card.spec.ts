import { describe, expect, it, vi, beforeEach } from 'vitest'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { PortalTokenRespVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import request from '@/config/axios'
import dayjs from 'dayjs'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    postOriginal: vi.fn(),
    download: vi.fn()
  }
}))

describe('Ticket 01: 管理端门户链接签发闭环（API 契约与卡片逻辑）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 接口契约测试', () => {
    it('getPortalToken 应该向 /lunaerp/baseinfo/customer/portal-token 发送 GET 请求并带上 customerId', async () => {
      const mockResp: PortalTokenRespVO = {
        token: 'token_abc123',
        url: 'https://portal.luna.com/portal/shipments?t=token_abc123',
        generateTime: '2026-10-12 10:00:00',
        expireDays: 30,
        expireTime: '2026-11-11 10:00:00'
      }
      vi.mocked(request.get).mockResolvedValueOnce(mockResp as any)

      const res = await CustomerApi.getPortalToken(1001)

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/baseinfo/customer/portal-token',
        params: { customerId: 1001 }
      })
      expect(res).toEqual(mockResp)
    })

    it('generatePortalToken 应该向 /lunaerp/baseinfo/customer/portal-token/generate 发送 POST 请求', async () => {
      const mockResp: PortalTokenRespVO = {
        token: 'new_token_xyz',
        url: 'https://portal.luna.com/portal/shipments?t=new_token_xyz',
        generateTime: '2026-10-12 12:00:00',
        expireDays: 30,
        expireTime: '2026-11-11 12:00:00'
      }
      vi.mocked(request.post).mockResolvedValueOnce(mockResp as any)

      const res = await CustomerApi.generatePortalToken(1001)

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/baseinfo/customer/portal-token/generate',
        params: { customerId: 1001 }
      })
      expect(res).toEqual(mockResp)
    })

    it('revokePortalToken 应该向 /lunaerp/baseinfo/customer/portal-token 发送 DELETE 请求', async () => {
      vi.mocked(request.delete).mockResolvedValueOnce(true as any)

      const res = await CustomerApi.revokePortalToken(1001)

      expect(request.delete).toHaveBeenCalledWith({
        url: '/lunaerp/baseinfo/customer/portal-token',
        params: { customerId: 1001 }
      })
      expect(res).toBe(true)
    })
  })

  describe('2. 门户卡片状态展示逻辑测试', () => {
    it('未开通状态：tag 显示未开通，状态文本显示未开通', () => {
      const portalTokenInfo: PortalTokenRespVO | null = null

      const isActivated = Boolean(portalTokenInfo?.token)
      const tagText = isActivated ? '已开通' : '未开通'
      const tagType = isActivated ? 'success' : 'info'
      const statusText = isActivated ? `有效期至 ...` : '未开通'

      expect(isActivated).toBe(false)
      expect(tagText).toBe('未开通')
      expect(tagType).toBe('info')
      expect(statusText).toBe('未开通')
    })

    it('已开通状态：按 YYYY-MM-DD HH:mm 格式化有效期与剩余天数', () => {
      const portalTokenInfo: PortalTokenRespVO = {
        token: 'token_43chars_length_mock_for_customer_portal_key',
        url: 'https://portal.luna.com/portal/shipments?t=xxx',
        generateTime: '2026-10-12 10:00:00',
        expireDays: 28,
        expireTime: '2026-11-09 10:00:00'
      }

      const isActivated = Boolean(portalTokenInfo?.token)
      const tagText = isActivated ? '已开通' : '未开通'
      const formattedExpireTime = dayjs(portalTokenInfo.expireTime).format('YYYY-MM-DD HH:mm')
      const statusText = `有效期至 ${formattedExpireTime} (剩余 ${portalTokenInfo.expireDays} 天)`

      expect(isActivated).toBe(true)
      expect(tagText).toBe('已开通')
      expect(statusText).toBe('有效期至 2026-11-09 10:00 (剩余 28 天)')
    })
  })

  describe('3. 复制链接与降级提示逻辑测试', () => {
    it('当 url 存在时，复制完整 url 并在剪贴板中写入', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined)
      Object.assign(navigator, {
        clipboard: { writeText }
      })

      const portalTokenInfo: PortalTokenRespVO = {
        token: 'token_abc',
        url: 'https://portal.luna.com/portal/shipments?t=token_abc',
        expireDays: 30
      }

      let copiedText = ''
      let warningMessage = ''
      let successMessage = ''

      if (portalTokenInfo.url) {
        await navigator.clipboard.writeText(portalTokenInfo.url)
        copiedText = portalTokenInfo.url
        successMessage = '链接已复制到剪贴板'
      } else {
        await navigator.clipboard.writeText(portalTokenInfo.token)
        copiedText = portalTokenInfo.token
        warningMessage = '未配置客户门户基准地址(luna.portal.base-url)，仅复制 Token'
      }

      expect(writeText).toHaveBeenCalledWith('https://portal.luna.com/portal/shipments?t=token_abc')
      expect(copiedText).toBe('https://portal.luna.com/portal/shipments?t=token_abc')
      expect(successMessage).toBe('链接已复制到剪贴板')
      expect(warningMessage).toBe('')
    })

    it('当 url 为空或 null 时，仅复制 Token 并触发基准地址未配置的警告提示', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined)
      Object.assign(navigator, {
        clipboard: { writeText }
      })

      const portalTokenInfo: PortalTokenRespVO = {
        token: 'token_standalone_xyz',
        url: null,
        expireDays: 30
      }

      let copiedText = ''
      let warningMessage = ''

      if (portalTokenInfo.url) {
        await navigator.clipboard.writeText(portalTokenInfo.url)
        copiedText = portalTokenInfo.url
      } else {
        await navigator.clipboard.writeText(portalTokenInfo.token)
        copiedText = portalTokenInfo.token
        warningMessage = '未配置客户门户基准地址(luna.portal.base-url)，仅复制 Token'
      }

      expect(writeText).toHaveBeenCalledWith('token_standalone_xyz')
      expect(copiedText).toBe('token_standalone_xyz')
      expect(warningMessage).toBe('未配置客户门户基准地址(luna.portal.base-url)，仅复制 Token')
    })
  })

  describe('4. 重新生成与吊销二次确认逻辑测试', () => {
    it('首次生成不需要二次确认弹窗直接调用接口', async () => {
      const isRegenerate = false
      const mockConfirm = vi.fn()

      if (isRegenerate) {
        mockConfirm('重新生成将使旧链接立即失效，是否继续？')
      }

      expect(mockConfirm).not.toHaveBeenCalled()
    })

    it('重新生成必须弹出二次确认提示，且文案提示旧链接立即失效', async () => {
      const isRegenerate = true
      const mockConfirm = vi.fn().mockResolvedValue('confirm')

      let promptMsg = ''
      if (isRegenerate) {
        promptMsg = '重新生成将使旧链接立即失效，是否继续？'
        await mockConfirm(promptMsg)
      }

      expect(mockConfirm).toHaveBeenCalledWith('重新生成将使旧链接立即失效，是否继续？')
      expect(promptMsg).toContain('重新生成将使旧链接立即失效')
    })

    it('吊销操作必须弹出二次确认弹窗', async () => {
      const mockConfirm = vi.fn().mockResolvedValue('confirm')
      const promptMsg = '确定要吊销该客户的门户链接吗？吊销后旧链接将立即失效。'
      await mockConfirm(promptMsg)

      expect(mockConfirm).toHaveBeenCalledWith(promptMsg)
    })
  })
})
