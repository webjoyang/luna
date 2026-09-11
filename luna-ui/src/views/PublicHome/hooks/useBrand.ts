/**
 * 多租户品牌配置 composable（迁自 oldweb/hooks/useBrand.ts）
 *
 * - 模块级全局 ref，跨组件共享响应式品牌状态（React 版的 listeners 订阅
 *   机制在 Vue 中由全局 ref 的响应式天然替代）
 * - localStorage 缓存（按 host + 租户 slug 区分）实现秒开
 * - 异步拉取 /api/tenant/brand（失败静默回退默认预设，luna-ui 后端暂无该接口时页面以预设渲染）
 * - 监听 brand-config-updated 事件响应外部更新
 *
 * 简化项：不迁移 oldweb 的 scrubCachedBrandConfig（上传缓存清理）；
 * normalizeBrandLogoUrl 简化为透传（Vue 版无需 dark/light tone 规范化）。
 */
import { ref } from 'vue'
import { defaultBrand, type BrandConfig } from '@/views/PublicHome/config/brand'
import { getAccessToken } from '@/utils/auth'

// 模块级全局状态：所有组件共享同一份响应式品牌数据
const globalBrandState = ref<BrandConfig>({ ...defaultBrand })

// 按缓存 key 去重，避免重复初始化
let initializedKey: string | null = null

const LEGACY_BRAND_CACHE_KEY = 'tenant_brand_config'

function normalizeTenantPreviewSlug(value: string | null): string {
  const normalized = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/^https?:\/\//, '')
    .split('/')[0]
    .split('?')[0]
    .split('#')[0]
    .replace(/^www\./, '')
    .replace(/:\d+$/, '')
  if (!normalized) return ''

  const slug = normalized.split('.')[0]
  return /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(slug) ? slug : ''
}

/** 从 URL 读取租户预览参数（?tenant= / ?tenantPreview= / ?_tenant=） */
export function getTenantPreviewSlug(): string {
  if (typeof window === 'undefined') return ''
  const params = new URLSearchParams(window.location.search)
  return normalizeTenantPreviewSlug(params.get('tenant') || params.get('tenantPreview') || params.get('_tenant'))
}

function getBrandCacheKey(): string {
  const host = window.location.host || 'default'
  const previewTenant = getTenantPreviewSlug()
  return `tenant_brand_config:${host}${previewTenant ? `:${previewTenant}` : ''}`
}

/** 官网后端 API 地址（VITE_PUBLIC_API_BASE 可配置，默认同源） */
export function getPublicApiBase(): string {
  const configuredBase = import.meta.env.VITE_PUBLIC_API_BASE as string | undefined
  return configuredBase ? configuredBase.replace(/\/$/, '') : ''
}

function getBrandFetchHeaders(): HeadersInit {
  const token = getAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function readBrandField(data: Record<string, any>, keys: string[], fallback = '') {
  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(data, key)) {
      return data[key] ?? ''
    }
  }
  return fallback
}

/** 从接口数据合并出品牌配置（logo 字段做兼容映射后透传） */
function normalizeBrand(data: Record<string, any>): BrandConfig {
  return {
    ...data,
    clientId: readBrandField(data, ['clientId', 'code'], globalBrandState.value.clientId),
    logoUrl: readBrandField(data, ['logoUrl', 'logo'], globalBrandState.value.logoUrl),
    logoWhiteUrl: readBrandField(data, ['logoWhiteUrl', 'logoWhite'], globalBrandState.value.logoWhiteUrl),
  } as BrandConfig
}

const initGlobalBrand = async () => {
  const cacheKey = getBrandCacheKey()
  if (initializedKey === cacheKey) return
  initializedKey = cacheKey
  globalBrandState.value = { ...defaultBrand }

  try {
    // 1. 先读 localStorage 缓存，保证秒开
    const cached = localStorage.getItem(cacheKey) || localStorage.getItem(LEGACY_BRAND_CACHE_KEY)
    if (cached) {
      try {
        globalBrandState.value = { ...defaultBrand, ...JSON.parse(cached) }
      } catch (e) {
        console.error('Failed to parse cached brand config', e)
      }
    }

    // 2. 拉取当前域名/租户的公开品牌配置
    const previewTenant = getTenantPreviewSlug()
    const brandUrl = previewTenant
      ? `${getPublicApiBase()}/api/tenant/brand?tenant=${encodeURIComponent(previewTenant)}`
      : `${getPublicApiBase()}/api/tenant/brand`
    const res = await fetch(brandUrl, { headers: getBrandFetchHeaders() })

    if (res.ok) {
      const data = await res.json()
      if (data) {
        const normalizedBrand = normalizeBrand(data)
        globalBrandState.value = { ...globalBrandState.value, ...normalizedBrand }
        localStorage.setItem(getBrandCacheKey(), JSON.stringify(normalizedBrand))
      }
    }
  } catch (error) {
    // luna-ui 后端暂无 /api/tenant/brand 时静默回退默认预设
    console.error('Failed to fetch tenant brand config', error)
  }
}

// 外部更新（brand-config-updated 事件）时重读缓存
window.addEventListener('brand-config-updated', () => {
  const cached = localStorage.getItem(getBrandCacheKey()) || localStorage.getItem(LEGACY_BRAND_CACHE_KEY)
  if (cached) {
    try {
      globalBrandState.value = { ...defaultBrand, ...JSON.parse(cached) }
    } catch (e) {}
  }
})

/** 多租户品牌配置 hook：返回全局响应式品牌状态（幂等初始化） */
export function useBrand() {
  initGlobalBrand()
  return { brand: globalBrandState }
}

export const getGlobalBrand = () => globalBrandState.value
