import request from '@/config/axios'

/**
 * 汇率主档 Response VO
 */
export interface ExchangeRateRespVO {
  id: number
  currency: string
  rate: number
  effectiveDate: string
  status: 'ENABLE' | 'DISABLE' | string
  remark?: string
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

/**
 * 汇率主档分页查询 Request VO
 */
export interface ExchangeRatePageReqVO extends PageParam {
  currency?: string
  status?: string
  effectiveDate?: string
}

/**
 * 汇率主档创建 Request VO
 */
export interface ExchangeRateSaveReqVO {
  currency: string
  rate: number | string
  effectiveDate: string
  remark?: string
}

/**
 * 汇率主档更新 Request VO（仅用于停用与维护备注）
 */
export interface ExchangeRateUpdateReqVO {
  id: number
  status: 'ENABLE' | 'DISABLE' | string
  remark?: string
}

/**
 * 分页查询汇率主档
 */
export const getExchangeRatePage = (params: ExchangeRatePageReqVO) => {
  return request.get<PageResult<ExchangeRateRespVO>>({
    url: '/lunaerp/finance/exchange-rate/page',
    params
  })
}

/**
 * 获得汇率记录详情
 */
export const getExchangeRate = (id: number) => {
  return request.get<ExchangeRateRespVO>({
    url: `/lunaerp/finance/exchange-rate/get?id=${id}`
  })
}

/**
 * 创建汇率主档（本位币禁止配置，同币种同生效日唯一）
 */
export const createExchangeRate = (data: ExchangeRateSaveReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/exchange-rate/create',
    data
  })
}

/**
 * 更新汇率主档（主要用于停用与维护备注，不可物理删除）
 */
export const updateExchangeRate = (data: ExchangeRateUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/exchange-rate/update',
    data
  })
}
