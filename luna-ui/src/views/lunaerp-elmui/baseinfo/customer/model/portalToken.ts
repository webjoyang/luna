/**
 * 客户门户令牌 VO
 */
export interface CustomerPortalTokenVO {
  exists: boolean
  url?: string
  token?: string
  tokenFingerprint?: string
  generateTime?: string | Date
  remainingDays?: number
}

/**
 * 获得门户状态标签属性
 */
export function getPortalStatusBadge(exists: boolean): {
  type: '' | 'success' | 'warning' | 'info' | 'danger'
  text: string
} {
  if (exists) {
    return { type: 'success', text: '已开通' }
  }
  return { type: 'info', text: '未开通' }
}

/**
 * 格式化剩余有效期文案
 */
export function formatPortalRemainingDays(remainingDays?: number): string {
  if (remainingDays === undefined || remainingDays === null) {
    return '-'
  }
  if (remainingDays <= 0) {
    return '已过期'
  }
  return `剩余 ${remainingDays} 天`
}

/**
 * 复制链接或降级复制 Token 计算
 */
export function getCopyLinkPayload(tokenData?: CustomerPortalTokenVO | null): {
  canCopy: boolean
  text: string
  isDegraded: boolean
  message: string
} {
  if (!tokenData || !tokenData.exists) {
    return {
      canCopy: false,
      text: '',
      isDegraded: false,
      message: '当前未生成有效门户链接'
    }
  }

  // 正常模式：已配置 base-url 并拼装完整 url
  if (tokenData.url && tokenData.url.trim().length > 0) {
    return {
      canCopy: true,
      text: tokenData.url.trim(),
      isDegraded: false,
      message: '门户链接已复制到剪贴板'
    }
  }

  // 降级模式：未配置 base-url，仅复制 token 明文并提示配置基址
  if (tokenData.token && tokenData.token.trim().length > 0) {
    return {
      canCopy: true,
      text: tokenData.token.trim(),
      isDegraded: true,
      message:
        '系统尚未配置客户门户基准地址(luna.portal.base-url)，已复制令牌明文。请先联系管理员配置基准地址后获取完整访问链接。'
    }
  }

  return {
    canCopy: false,
    text: '',
    isDegraded: false,
    message: '未能获取有效访问链接或令牌'
  }
}
