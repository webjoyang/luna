/**
 * 产品自定义标签的调色板与展示规则（对齐旧版 React product-tag-palette.ts）。
 * 色键与后端 PRODUCT_TAG_COLORS 一一对应：
 *   ink  —— 文字与圆点主色
 *   tint —— 胶囊底色（同色系约 10%）
 *   ring —— 描边色（同色系约 18%）
 */

export const PRODUCT_TAG_COLOR_KEYS = [
  'camel',
  'blue',
  'teal',
  'green',
  'orange',
  'pink',
  'purple',
  'graphite'
] as const

export type ProductTagColorKey = (typeof PRODUCT_TAG_COLOR_KEYS)[number]

export const PRODUCT_TAG_DEFAULT_COLOR: ProductTagColorKey = 'graphite'

export interface ProductTagSwatch {
  key: ProductTagColorKey
  /** 染缸说法的色名 */
  label: string
  ink: string
  tint: string
  ring: string
}

export const PRODUCT_TAG_PALETTE: Record<ProductTagColorKey, ProductTagSwatch> = {
  camel: {
    key: 'camel',
    label: '驼',
    ink: '#a2845e',
    tint: 'rgba(162,132,94,0.12)',
    ring: 'rgba(162,132,94,0.20)'
  },
  blue: {
    key: 'blue',
    label: '靛蓝',
    ink: '#0a6cff',
    tint: 'rgba(10,108,255,0.09)',
    ring: 'rgba(10,108,255,0.18)'
  },
  teal: {
    key: 'teal',
    label: '湖青',
    ink: '#2fa8bd',
    tint: 'rgba(47,168,189,0.10)',
    ring: 'rgba(47,168,189,0.20)'
  },
  green: {
    key: 'green',
    label: '草木绿',
    ink: '#2fa14f',
    tint: 'rgba(47,161,79,0.10)',
    ring: 'rgba(47,161,79,0.20)'
  },
  orange: {
    key: 'orange',
    label: '琥珀',
    ink: '#e88a00',
    tint: 'rgba(232,138,0,0.10)',
    ring: 'rgba(232,138,0,0.20)'
  },
  pink: {
    key: 'pink',
    label: '茜红',
    ink: '#e64571',
    tint: 'rgba(230,69,113,0.09)',
    ring: 'rgba(230,69,113,0.18)'
  },
  purple: {
    key: 'purple',
    label: '紫藤',
    ink: '#9a4fd6',
    tint: 'rgba(154,79,214,0.09)',
    ring: 'rgba(154,79,214,0.18)'
  },
  graphite: {
    key: 'graphite',
    label: '石墨',
    ink: '#6e6e73',
    tint: 'rgba(110,110,115,0.10)',
    ring: 'rgba(110,110,115,0.20)'
  }
}

export const PRODUCT_TAG_SWATCHES: ProductTagSwatch[] = PRODUCT_TAG_COLOR_KEYS.map(
  (key) => PRODUCT_TAG_PALETTE[key]
)

/** 后端历史脏数据或未来新增色键一律兜底石墨。 */
export const resolveTagSwatch = (color?: string): ProductTagSwatch => {
  if (color && color in PRODUCT_TAG_PALETTE) {
    return PRODUCT_TAG_PALETTE[color as ProductTagColorKey]
  }
  return PRODUCT_TAG_PALETTE[PRODUCT_TAG_DEFAULT_COLOR]
}

/** 标签文字上限 8 个字；单产品 20 个。 */
export const PRODUCT_TAG_NAME_MAX = 8
export const PRODUCT_TAG_PER_PRODUCT_MAX = 20

/** 按码点截断，避免 emoji 被截半个字符。 */
export const clampTagName = (raw: string, max: number = PRODUCT_TAG_NAME_MAX): string => {
  const chars = Array.from(raw ?? '')
  return chars.length <= max ? chars.join('') : chars.slice(0, max).join('')
}

export const normalizeTagName = (raw: string): string =>
  (raw ?? '').trim().replace(/\s+/g, ' ')
