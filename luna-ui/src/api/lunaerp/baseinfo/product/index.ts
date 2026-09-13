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

// 产品信息 VO
export interface ProductVO {
  id?: number
  code: string
  normalizedCode?: string
  name: string
  type: string
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
  yarnSpecs?: string
  machineType?: string
  output?: string
  remark?: string
  supplierCode?: string
  tempCode?: string
  craftType?: string
  color?: string
  greigeTypeId?: string
  patternNo?: string
  standardLength?: string
  conversionRate?: number
  costItems?: string
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
