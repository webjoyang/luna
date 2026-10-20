/**
 * 客户门户令牌与会话工具
 */

export const PORTAL_TOKEN_STORAGE_KEY = 'luna_portal_token'
export const PORTAL_CUSTOMER_NAME_KEY = 'luna_portal_customer_name'

/**
 * 获取当前存储的客户门户令牌
 */
export function getPortalToken(): string {
  if (typeof sessionStorage === 'undefined') {
    return ''
  }
  return sessionStorage.getItem(PORTAL_TOKEN_STORAGE_KEY) || ''
}

/**
 * 保存客户门户令牌到 sessionStorage
 */
export function setPortalToken(token: string): void {
  if (typeof sessionStorage !== 'undefined' && token) {
    sessionStorage.setItem(PORTAL_TOKEN_STORAGE_KEY, token.trim())
  }
}

/**
 * 清除客户门户令牌与相关信息
 */
export function clearPortalToken(): void {
  if (typeof sessionStorage !== 'undefined') {
    sessionStorage.removeItem(PORTAL_TOKEN_STORAGE_KEY)
    sessionStorage.removeItem(PORTAL_CUSTOMER_NAME_KEY)
  }
}

/**
 * 获取客户名称
 */
export function getPortalCustomerName(): string {
  if (typeof sessionStorage === 'undefined') {
    return ''
  }
  return sessionStorage.getItem(PORTAL_CUSTOMER_NAME_KEY) || ''
}

/**
 * 保存客户名称
 */
export function setPortalCustomerName(name: string): void {
  if (typeof sessionStorage !== 'undefined' && name) {
    sessionStorage.setItem(PORTAL_CUSTOMER_NAME_KEY, name.trim())
  }
}

/**
 * 门户令牌引导决策
 *
 * @param queryToken URL query 中的 ?t= 令牌
 * @param currentStoredToken 当前已存在的 sessionStorage 令牌
 * @returns 判定结果：token 明文、是否应清理 URL 地址栏、是否有效
 */
export function handlePortalTokenGuide(
  queryToken?: string | null,
  currentStoredToken?: string | null
): { token: string | null; shouldCleanUrl: boolean; isValid: boolean } {
  if (queryToken && queryToken.trim().length > 0) {
    const cleanToken = queryToken.trim()
    setPortalToken(cleanToken)
    return {
      token: cleanToken,
      shouldCleanUrl: true,
      isValid: true
    }
  }

  if (currentStoredToken && currentStoredToken.trim().length > 0) {
    return {
      token: currentStoredToken.trim(),
      shouldCleanUrl: false,
      isValid: true
    }
  }

  return {
    token: null,
    shouldCleanUrl: false,
    isValid: false
  }
}

/**
 * 判断是否为门户相关路由
 */
export function isPortalPath(path: string): boolean {
  return typeof path === 'string' && path.startsWith('/portal')
}

/**
 * 判断是否为失效页面
 */
export function isPortalInvalidPath(path: string): boolean {
  return path === '/portal/invalid'
}
