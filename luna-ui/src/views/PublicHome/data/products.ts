/**
 * 官网首页静态示例数据（迁移过渡方案）
 *
 * TODO: 后续接入真实接口后替换本文件数据源：
 *   - GET /api/public/products?featured=1&pageSize=4  首页推荐产品
 *   - GET /api/public/products?page=1&pageSize=30     面料库产品（分页）
 *   - GET /api/public/categories                      公开分类
 * 接口返回形状与下方 PublicProduct 保持一致，可直接无缝替换。
 */

export interface PublicProductImage {
  id: string
  url: string
  sortOrder: number
}

/** 原 `GET /api/public/products` 返回的单个产品形状（详情接口额外带 remark） */
export interface PublicProduct {
  id: string
  code: string
  name: string
  type: string
  composition: string
  widthCm: number | null
  weightGsm: number | null
  color: string
  craftType: string
  coverImageUrl: string
  images: PublicProductImage[]
  remark?: string
}

/** 公开接口返回的图片地址可能是相对路径，统一补全为可访问的绝对地址 */
export function resolvePublicAssetUrl(url: string | null | undefined): string {
  if (!url) return ''
  if (/^https?:\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

/** 兜底灰度面料图（保留给 CTA 黑底区做氛围 + 图片加载失败时兜底） */
export const FABRIC_FALLBACK = '/presentation/assets/fabric.png'

/**
 * 从 https://www.lunaagi.cn/ 抓取的真实面料产品图片（2026-09-11 下载）
 * 对应 lunaagi 线上公开产品的封面照片，全部存放在 public/presentation/assets/products/
 */
const PRODUCT_IMG = (n: number) => `/presentation/assets/products/product-${String(n).padStart(3, '0')}.jpg`

/** 首页叙事区展示的产品（对应原 featured=1&pageSize=4） */
export const FEATURED_PRODUCTS: PublicProduct[] = [
  {
    id: 'demo-001',
    code: 'LX-1201',
    name: '云感卫衣布',
    type: '卫衣布',
    composition: '棉 80% 聚酯纤维 20%',
    widthCm: 165,
    weightGsm: 320,
    color: '本白',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(1),
    images: [],
  },
  {
    id: 'demo-002',
    code: 'LX-1345',
    name: '轻氧毛圈布',
    type: '毛圈布',
    composition: '棉 95% 氨纶 5%',
    widthCm: 170,
    weightGsm: 260,
    color: '浅灰',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(2),
    images: [],
  },
  {
    id: 'demo-003',
    code: 'LX-2108',
    name: '弹力罗纹',
    type: '罗纹',
    composition: '棉 62% 锦纶 32% 氨纶 6%',
    widthCm: 95,
    weightGsm: 300,
    color: '藏青',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(3),
    images: [],
  },
  {
    id: 'demo-004',
    code: 'LX-2233',
    name: '珠地网眼',
    type: '珠地',
    composition: '棉 100%',
    widthCm: 160,
    weightGsm: 210,
    color: '黑色',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(4),
    images: [],
  },
]

/** 面料库全量产品（对应原 /api/public/products 分页数据，本地内存分页演示） */
export const ALL_PRODUCTS: PublicProduct[] = [
  ...FEATURED_PRODUCTS,
  {
    id: 'demo-005',
    code: 'LX-3051',
    name: '双面呢空气层',
    type: '空气层',
    composition: '锦纶 88% 氨纶 12%',
    widthCm: 150,
    weightGsm: 380,
    color: '米白',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(5),
    images: [],
  },
  {
    id: 'demo-006',
    code: 'LX-3120',
    name: '丝光棉汗布',
    type: '汗布',
    composition: '棉 100%',
    widthCm: 175,
    weightGsm: 180,
    color: '浅粉',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(6),
    images: [],
  },
  {
    id: 'demo-007',
    code: 'LX-4066',
    name: '磨毛罗马布',
    type: '罗马布',
    composition: '棉 68% 聚酯纤维 28% 氨纶 4%',
    widthCm: 168,
    weightGsm: 250,
    color: '驼色',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(7),
    images: [],
  },
  {
    id: 'demo-008',
    code: 'LX-4102',
    name: '抗菌摇粒绒',
    type: '摇粒绒',
    composition: '聚酯纤维 100%',
    widthCm: 150,
    weightGsm: 300,
    color: '灰色',
    craftType: '针织',
    coverImageUrl: PRODUCT_IMG(8),
    images: [],
  },
]

/** 公开分类（对应原 GET /api/public/categories） */
export const CATEGORIES: string[] = ['卫衣布', '毛圈布', '罗纹', '珠地']
