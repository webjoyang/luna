import request from '@/config/axios'

// 产品标签 VO
export interface ProductTagVO {
  id?: number
  name: string
  color: string
  sortOrder?: number
}

// 产品图片 VO
export interface ProductImageVO {
  id?: number
  url: string
  sortOrder: number
}

// 产品信息 VO（对齐 ADR-0035：删 output/patternNo/tempCode/conversionRate，增布种/类别标签/梭织参数/成本设置 17 列）
// 可空字段声明为 `| null`：编辑时提交 null 表示「清空」，后端 ProductDO 对应列已加 FieldStrategy.ALWAYS
export interface ProductVO {
  id?: number
  code: string
  normalizedCode?: string
  name: string
  /** 产品类别（字典 lunaerp_product_category 的 value） */
  type: string
  /** 产品类别字典标签（名称冗余，供列表直接展示） */
  typeLabel?: string
  composition: string
  widthCm: number
  weightGsm: number
  coverImageUrl?: string
  supplierId?: string
  supplierCompany?: string
  status?: string
  isPublic?: boolean
  isFeatured?: boolean
  featuredOrder?: number
  yarnSpecs?: string | null
  machineType?: string
  remark?: string
  /** 供应商样号（ADR-0035 更正标签，原「原厂编号」） */
  supplierCode?: string
  craftType?: string
  color?: string
  greigeTypeId?: string | null
  standardLength?: string
  /** 布种（字典 lunaerp_fabric_type 的 value，选填，提交 null 表示清空） */
  fabricType?: string | null
  /** 布种字典标签（名称冗余，与 fabricType 成对清空） */
  fabricTypeLabel?: string | null
  /** 梭织-总经根数 */
  wovenTotalEnds?: number | null
  /** 梭织-筘号 */
  wovenReedNo?: number | null
  /** 梭织-经密（根/CM） */
  wovenWarpDensity?: number | null
  /** 梭织-纬密（根/CM） */
  wovenWeftDensity?: number | null
  /** 梭织-上机纬密（根/CM） */
  wovenLoomWeftDensity?: number | null
  /** 织物结构（KNIT 针织 / WOVEN 梭织） */
  fabricConstruction?: string
  /** 梭织估算方式（SIMPLE 简易估算 / PROFESSIONAL 经纬精算） */
  wovenCalculationMode?: string
  /** 坯布克重（GSM） */
  greigeWeightGsm?: number | null
  /** 坯布门幅（CM） */
  greigeWidthCm?: number | null
  /** 针织加工费（元/KG） */
  knitProcessingFeePerKg?: number | null
  /** 织造费（元/M） */
  weavingFeePerMeter?: number | null
  /** 其他加工费（元/M） */
  otherProcessingFeePerMeter?: number | null
  /** 包装费 */
  packagingFee?: number | null
  /** 税率 */
  taxRate?: number | null
  costItems?: string | null
  costPrice?: number
  costStatus?: string
  costUpdatedAt?: Date | string
  aiDesignUrl?: string
  aiDesignCategory?: string
  aiDesignDescription?: string
  businessOwnership?: string
  tags?: ProductTagVO[]
  images?: ProductImageVO[]
  createTime?: Date
}

// 产品信息分页查询参数
export interface ProductPageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  code?: string
  name?: string
  composition?: string
  type?: string
  weightGsm?: number
  minWeight?: number
  maxWeight?: number
  status?: string
}

// 查询产品分页
export const getProductPage = (params: ProductPageParam) => {
  return request.get({ url: '/lunaerp/baseinfo/product/page', params })
}

// 查询产品详情
export const getProduct = (id: number) => {
  return request.get({ url: '/lunaerp/baseinfo/product/get?id=' + id })
}

// 根据编码查询产品
export const getProductByCode = (code: string) => {
  return request.get({ url: '/lunaerp/baseinfo/product/get-by-code?code=' + code })
}

// 获取下一个产品编码
export const getNextCode = (prefix?: string) => {
  return request.get<string>({ url: '/lunaerp/baseinfo/product/next-code', params: { prefix } })
}

// 新增产品
export const createProduct = (data: ProductVO) => {
  return request.post<number>({ url: '/lunaerp/baseinfo/product/create', data })
}

// 修改产品
export const updateProduct = (data: ProductVO) => {
  return request.put({ url: '/lunaerp/baseinfo/product/update', data })
}

// 删除产品
export const deleteProduct = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/product/delete?id=' + id })
}

// 批量删除产品
export const deleteProductList = (ids: number[]) => {
  return request.delete({
    url: '/lunaerp/baseinfo/product/delete-list',
    params: { ids: ids.join(',') }
  })
}

// 修改产品标签
export const updateProductTags = (id: number, tags: ProductTagVO[]) => {
  return request.put({ url: '/lunaerp/baseinfo/product/update-tags?id=' + id, data: tags })
}

// 下载产品导入模板
export const downloadProductTemplate = () => {
  return request.download({ url: '/lunaerp/baseinfo/product/import-template' })
}

// 导入产品（二进制响应以捕获错误 Excel 流）
export const importProduct = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.postOriginal({
    url: '/lunaerp/baseinfo/product/import',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob'
  })
}

