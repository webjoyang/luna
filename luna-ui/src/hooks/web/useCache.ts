/**
 * 配置浏览器本地存储的方式，可直接存储对象数组。
 */

import WebStorageCache from 'web-storage-cache'

type CacheType = 'localStorage' | 'sessionStorage'

export const CACHE_KEY = {
  // 用户相关
  ROLE_ROUTERS: 'roleRouters',
  USER: 'user',
  VisitTenantId: 'visitTenantId',
  // 系统设置
  IS_DARK: 'isDark',
  LANG: 'lang',
  THEME: 'theme',
  LAYOUT: 'layout',
  DICT_CACHE: 'dictCache',
  // 登录表单
  LoginForm: 'loginForm',
  TenantId: 'tenantId'
}

export const useCache = (type: CacheType = 'localStorage') => {
  if (typeof window === 'undefined') {
    const memory = new Map<string, any>()
    return {
      wsCache: {
        get: (k: string) => memory.get(k),
        set: (k: string, v: any) => memory.set(k, v),
        delete: (k: string) => memory.delete(k),
        clear: () => memory.clear()
      } as unknown as WebStorageCache
    }
  }

  const wsCache: WebStorageCache = new WebStorageCache({
    storage: type
  })

  return {
    wsCache
  }
}

export const deleteUserCache = () => {
  const { wsCache } = useCache()
  wsCache.delete(CACHE_KEY.USER)
  wsCache.delete(CACHE_KEY.ROLE_ROUTERS)
  wsCache.delete(CACHE_KEY.VisitTenantId)
  // 注意，不要清理 LoginForm 登录表单
}
