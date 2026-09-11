/**
 * 品牌配置中心（迁自 oldweb/lib/brand-config.ts）
 *
 * 官网首页（PublicHome）的品牌差异通过此文件 + 环境变量控制。
 * 使用方式：
 *   import { defaultBrand } from '@/views/PublicHome/config/brand'
 *   <span>{{ brand.name }}</span>
 *
 * 配置方式：
 *   在 .env.local 中设置 VITE_BRAND_CODE=lunaagi 等变量
 */

export interface BrandConfig {
  clientId: string
  /** 租户配置的域名或共享站点标识 */
  domain?: string | null
  /** 后端解析后的租户官网公开地址 */
  publicSiteUrl?: string | null
  /** 品牌中文名，如 "露娜主系统" */
  name: string
  /** 品牌英文代码，如 "LUNAAGI" */
  nameEn: string
  /** 品牌全称 */
  fullName: string
  /** 品牌标语 */
  slogan: string
  /** 品牌介绍文案 */
  description: string
  /** Logo 路径 */
  logoUrl: string
  /** 白色 Logo 路径（深色背景用） */
  logoWhiteUrl: string
  /** 品牌主色调 */
  primaryColor: string
  /** 品牌副色调 */
  secondaryColor: string
  /** AI 助手问候语 */
  aiGreeting: string
  /** 页脚版权声明 */
  footer: string
  /** 是否启用管理后台 */
  enableAdminModule?: boolean
  /** 官网首屏大标题（留空用默认文案） */
  heroTitle?: string
  /** 官网首屏副文案（留空用默认文案） */
  heroSubtitle?: string
  /** 官网滚动放大屏标题（留空用默认文案） */
  zoomTitle?: string
  /** 官网滚动放大屏副文案（留空用默认文案） */
  zoomSubtitle?: string
  /** 官网黑底收尾屏标题（留空用默认文案） */
  ctaTitle?: string
  /** 官网黑底收尾屏副文案（留空用默认文案） */
  ctaSubtitle?: string
}

// ========== 品牌预设表 ==========
// 后续新增客户只需在此添加一个对象即可

const BRAND_PRESETS: Record<string, BrandConfig> = {
  lunaagi: {
    clientId: 'lunaagi',
    name: '露娜主系统',
    nameEn: 'LUNAAGI',
    fullName: 'LUNAAGI 科技有限公司',
    slogan: '露娜智能',
    description: 'LUNAAGI 致力于为纺织行业提供最前沿的 AI 驱动 ERP 解决方案。',
    logoUrl: '/logo.svg',
    logoWhiteUrl: '/logo_white.svg',
    primaryColor: '#0052D4',
    secondaryColor: '#6A11CB',
    aiGreeting: '您好，我是 LUNAAGI 智能助手。',
    footer: `© ${new Date().getFullYear()} LUNAAGI. ALL RIGHTS RESERVED.`,
    enableAdminModule: true,
  },
  customer: {
    clientId: 'customer',
    name: '智能管理系统',
    nameEn: 'ERP',
    fullName: '智能管理系统',
    slogan: '智能管理',
    description: '面向纺织行业的智能管理系统。',
    logoUrl: '/logo.svg',
    logoWhiteUrl: '/logo_white.svg',
    primaryColor: '#0052D4',
    secondaryColor: '#6A11CB',
    aiGreeting: '您好，我是智能助手。',
    footer: `© ${new Date().getFullYear()}`,
    enableAdminModule: false,
  },
}

/**
 * 解析品牌配置：优先读取环境变量，回退到预设值
 */
function resolveBrand(): BrandConfig {
  const code = (import.meta.env.VITE_BRAND_CODE || 'lunaagi').toLowerCase()
  const preset = BRAND_PRESETS[code] || BRAND_PRESETS['lunaagi']

  return {
    ...preset,
    name: import.meta.env.VITE_BRAND_NAME || preset.name,
    nameEn: import.meta.env.VITE_BRAND_NAME_EN || preset.nameEn,
    slogan: import.meta.env.VITE_BRAND_SLOGAN || preset.slogan,
    description: import.meta.env.VITE_BRAND_DESCRIPTION || preset.description,
    logoUrl: import.meta.env.VITE_BRAND_LOGO || preset.logoUrl,
    logoWhiteUrl: import.meta.env.VITE_BRAND_LOGO_WHITE || preset.logoWhiteUrl,
    aiGreeting: import.meta.env.VITE_BRAND_AI_GREETING || preset.aiGreeting,
    footer: import.meta.env.VITE_BRAND_FOOTER || preset.footer,
    primaryColor: import.meta.env.VITE_BRAND_PRIMARY_COLOR || preset.primaryColor,
  }
}

/** 全局默认品牌配置实例 */
export const defaultBrand: BrandConfig = resolveBrand()
