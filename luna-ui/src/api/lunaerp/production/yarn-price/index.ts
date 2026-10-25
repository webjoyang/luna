import request from '@/config/axios'

// ==================== 纱价主档 VO & API ====================

export interface YarnPriceVO {
  id?: number
  yarnSpecId: number
  yarnTypeId?: number
  specName?: string
  priceType: string // PURCHASE | MARKET
  price: number
  effectiveDate: string
  source?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface YarnPriceSaveVO {
  id?: number
  yarnSpecId: number
  priceType: string
  price: number
  effectiveDate: string
  source?: string
  remark?: string
}

export interface YarnPricePageParam {
  pageNo?: number
  pageSize?: number
  yarnSpecId?: number
  priceType?: string
  startDate?: string
  endDate?: string
}

export interface YarnLatestPurchasePriceVO {
  hasOrder: boolean
  price?: number
  orderNo?: string
  orderDate?: string
  supplierName?: string
}

// 分页查询纱价
export const getYarnPricePage = (params: YarnPricePageParam) => {
  return request.get({ url: '/lunaerp/production/yarn-price/page', params })
}

// 按规格查询纱价历史（日期倒序）
export const getYarnPriceListBySpec = (yarnSpecId: number, priceType?: string) => {
  return request.get<YarnPriceVO[]>({
    url: '/lunaerp/production/yarn-price/list-by-spec',
    params: { yarnSpecId, priceType }
  })
}

// 查询某规格最近一条已审核采购单价（只读聚合）
export const getLatestPurchasePrice = (yarnSpecId: number) => {
  return request.get<YarnLatestPurchasePriceVO>({
    url: '/lunaerp/production/yarn-price/latest-purchase-price',
    params: { yarnSpecId }
  })
}

// 新增价格记录
export const createYarnPrice = (data: YarnPriceSaveVO) => {
  return request.post<number>({ url: '/lunaerp/production/yarn-price/create', data })
}

// 修改价格记录
export const updateYarnPrice = (data: YarnPriceSaveVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/yarn-price/update', data })
}

// 删除价格记录
export const deleteYarnPrice = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/yarn-price/delete?id=${id}` })
}

// 下载导入模板
export const downloadYarnPriceTemplate = () => {
  return request.download({ url: '/lunaerp/production/yarn-price/import-template' })
}

// Excel 导入
export const importYarnPrice = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.postOriginal({
    url: '/lunaerp/production/yarn-price/import',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob'
  })
}
