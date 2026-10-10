import request from '@/config/axios'

export interface CostCardYarnItemVO {
  id?: number
  lineNo?: number
  yarnTypeId?: number
  yarnSpecId: number
  yarnSpecName: string
  direction?: string
  ratio: number
  density?: number
  crimpRate?: number
  wasteRate?: number
  consumptionKgPerMeter?: number
  price?: number
  priceType?: string
  priceSource?: string
  priceEffectiveDate?: string
  contributionPerKg?: number
  contributionPerMeter?: number
}

export interface CostCardFeeItemVO {
  id?: number
  lineNo?: number
  feeType: string
  feeUnit: string
  price: number
  lossRate: number
  contributionPerKg?: number
  contributionPerMeter?: number
  remark?: string
}

export interface CostCardVO {
  id?: number
  cardNo?: string
  productId: number
  productCode?: string
  productName?: string
  composition?: string
  widthSnapshot?: string
  weightSnapshot?: string
  fabricConstruction: 'KNIT' | 'WOVEN'
  wovenCalculationMode?: 'SIMPLE' | 'PROFESSIONAL'
  yarnPriceType: 'PURCHASE' | 'MARKET'
  greigeWeightGsm?: number
  greigeWidthCm?: number
  kgPerMeter?: number
  taxRate?: number
  basis: 'KG' | 'METER'
  yarnCostPerKg?: number
  yarnCostPerMeter?: number
  totalFeePerKg?: number
  totalFeePerMeter?: number
  unitCostPerKg?: number
  unitCostPerKgWithTax?: number
  unitCostPerMeter?: number
  unitCostPerMeterWithTax?: number
  missingPriceCount?: number
  effectiveDate?: string
  status?: 'DRAFT' | 'EFFECTIVE' | 'EXPIRED'
  remarks?: string
  yarnItems?: CostCardYarnItemVO[]
  feeItems?: CostCardFeeItemVO[]
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

export interface CostCardPageReqVO extends PageParam {
  search?: string
  status?: string
  productId?: number
  fabricConstruction?: string
}

export interface CostCardTrialRespVO {
  basis: string
  kgPerMeter?: number
  yarnCostPerKg?: number
  yarnCostPerMeter?: number
  totalFeePerKg?: number
  totalFeePerMeter?: number
  unitCostPerKg?: number
  unitCostPerKgWithTax?: number
  unitCostPerMeter?: number
  unitCostPerMeterWithTax?: number
  missingPriceCount: number
  yarnItems?: CostCardYarnItemVO[]
  feeItems?: CostCardFeeItemVO[]
}

// 创建成本卡
export const createCostCard = (data: CostCardVO) => {
  return request.post<number>({ url: '/lunaerp/business/cost-card/create', data })
}

// 更新成本卡
export const updateCostCard = (data: CostCardVO) => {
  return request.put<boolean>({ url: '/lunaerp/business/cost-card/update', data })
}

// 删除成本卡
export const deleteCostCard = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/business/cost-card/delete?id=${id}` })
}

// 查询成本卡详情
export const getCostCard = (id: number) => {
  return request.get<CostCardVO>({ url: `/lunaerp/business/cost-card/get?id=${id}` })
}

// 查询指定产品的生效成本卡
export const getEffectiveCostCardByProductId = (productId: number) => {
  return request.get<CostCardVO | null>({ url: `/lunaerp/business/cost-card/effective-by-product?productId=${productId}` })
}

// 分页查询成本卡
export const getCostCardPage = (params: CostCardPageReqVO) => {
  return request.get<PageResult<CostCardVO>>({ url: '/lunaerp/business/cost-card/page', params })
}

// 生效成本卡
export const effectCostCard = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/business/cost-card/effect?id=${id}` })
}

// 失效成本卡
export const expireCostCard = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/business/cost-card/expire?id=${id}` })
}

// 复制成本卡
export const copyCostCard = (id: number) => {
  return request.post<number>({ url: `/lunaerp/business/cost-card/copy?id=${id}` })
}

// 重新拉取当日价格并重算
export const refreshPrices = (data: Partial<CostCardVO>) => {
  return request.post<CostCardTrialRespVO>({ url: '/lunaerp/business/cost-card/refresh-prices', data })
}

// 成本卡纯内存试算
export const trialCostCard = (data: Partial<CostCardVO>) => {
  return request.post<CostCardTrialRespVO>({ url: '/lunaerp/business/cost-card/trial', data })
}

// 获取下一个卡号
export const getNextCardNo = () => {
  return request.get<string>({ url: '/lunaerp/business/cost-card/get-next-no' })
}
