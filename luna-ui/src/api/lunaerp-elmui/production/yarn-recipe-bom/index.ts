import request from '@/config/axios'

// ==================== 纱线配比BOM VO & API ====================

export interface YarnRecipeBomItemVO {
  id?: number
  recipeBomId?: number
  yarnTypeId: number
  yarnTypeName?: string
  yarnSpecId: number
  yarnSpecName?: string
  ratio: number
  spinMethod?: string
  remark?: string
  refPrice?: number
  wasteRate?: number
  direction?: string
  density?: number
  crimpRate?: number
  consumptionKgPerMeter?: number
}

export interface YarnRecipeBomVO {
  id?: number
  productId: number
  productCode?: string
  productName?: string
  remark?: string
  status?: string
  itemCount?: number
  items?: YarnRecipeBomItemVO[]
  creator?: string
  createTime?: Date | string
  updater?: string
  updateTime?: Date | string
}

export interface YarnRecipeBomSaveReqVO {
  id?: number
  productId: number
  productCode?: string
  productName?: string
  remark?: string
  status?: string
  items?: YarnRecipeBomItemVO[]
}

export interface YarnRecipeBomPageParam {
  pageNo?: number
  pageSize?: number
  productId?: number
  productCode?: string
  productName?: string
  status?: string
}

// 查询配比BOM分页
export const getYarnRecipeBomPage = (params: YarnRecipeBomPageParam) => {
  return request.get({ url: '/lunaerp/production/yarn-recipe-bom/page', params })
}

// 查询配比BOM详情
export const getYarnRecipeBom = (id: number) => {
  return request.get<YarnRecipeBomVO>({ url: `/lunaerp/production/yarn-recipe-bom/get?id=${id}` })
}

// 新增配比BOM
export const createYarnRecipeBom = (data: YarnRecipeBomSaveReqVO) => {
  return request.post<number>({ url: '/lunaerp/production/yarn-recipe-bom/create', data })
}

// 修改配比BOM
export const updateYarnRecipeBom = (data: YarnRecipeBomSaveReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/yarn-recipe-bom/update', data })
}

// 删除配比BOM
export const deleteYarnRecipeBom = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/yarn-recipe-bom/delete?id=${id}` })
}

// 按产品档案ID查询配比BOM
export const getYarnRecipeBomByProductId = (productId: number) => {
  return request.get<YarnRecipeBomVO>({
    url: `/lunaerp/production/yarn-recipe-bom/get-by-product-id?productId=${productId}`
  })
}
