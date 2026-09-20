import request from '@/config/axios'

// ==================== 纱线库存 VO & API ====================

export interface YarnStockBalanceVO {
  yarnSpecId: number
  stockQty: number
}

// 查询单规格纱线库存余额
export const getBalance = (yarnSpecId: number) => {
  return request.get<YarnStockBalanceVO>({
    url: `/lunaerp/production/yarn-stock/balance?yarnSpecId=${yarnSpecId}`
  })
}

// 批量查询纱线库存余额
export const getBalances = (yarnSpecIds: number[] | string) => {
  const ids = Array.isArray(yarnSpecIds) ? yarnSpecIds.join(',') : yarnSpecIds
  return request.get<YarnStockBalanceVO[]>({
    url: `/lunaerp/production/yarn-stock/balances?yarnSpecIds=${ids}`
  })
}
