import request from '@/config/axios'
import type { ProductTagVO } from './product/index'

// 查询产品标签列表
export const getProductTagList = () => {
  return request.get({ url: '/lunaerp/baseinfo/product-tag/list' })
}

// 新增产品标签
export const createProductTag = (data: ProductTagVO) => {
  return request.post({ url: '/lunaerp/baseinfo/product-tag/create', data })
}

// 修改产品标签
export const updateProductTag = (data: ProductTagVO) => {
  return request.put({ url: '/lunaerp/baseinfo/product-tag/update', data })
}

// 删除产品标签
export const deleteProductTag = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/product-tag/delete?id=' + id })
}
