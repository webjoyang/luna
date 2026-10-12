import request from '@/config/axios'

/**
 * 汇率主档 VO
 */
export interface ExchangeRateVO {
  id?: number
  currency: string
  rate: number
  effectiveDate: string
  status: string
  remark?: string
  createTime?: string
}

/**
 * 汇率分页查询参数
 */
export interface ExchangeRatePageParam {
  pageNo: number
  pageSize: number
  currency?: string
  status?: string
}

/**
 * 汇率保存/更新请求
 */
export interface ExchangeRateSaveReqVO {
  id?: number
  currency?: string
  rate?: number
  effectiveDate?: string
  status?: string
  remark?: string
}

/** 分页查询汇率主档 */
export const getExchangeRatePage = (params: ExchangeRatePageParam) => {
  return request.get<{ list: ExchangeRateVO[]; total: number }>({
    url: '/lunaerp/finance/exchange-rate/page',
    params
  })
}

/** 查询汇率详情 */
export const getExchangeRate = (id: number | string) => {
  return request.get<ExchangeRateVO>({
    url: '/lunaerp/finance/exchange-rate/get?id=' + id
  })
}

/** 创建汇率主档 */
export const createExchangeRate = (data: ExchangeRateSaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/exchange-rate/create',
    data
  })
}

/** 更新汇率主档（仅允许停用/启用及修改备注） */
export const updateExchangeRate = (data: ExchangeRateSaveReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/exchange-rate/update',
    data
  })
}
