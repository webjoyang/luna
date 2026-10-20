/**
 * 客户门户令牌与状态领域模型
 */

export interface PortalTokenInfo {
  token: string
  url?: string
  generateTime?: string | Date
  remainingDays?: number
  customerName?: string
}

export type PortalTabType = 'shipment' | 'statement'

export interface PortalRouteMeta {
  title: string
  tab?: PortalTabType
}
