import portalRequest from '@/config/axios/portal'

export interface PortalAuthInfoVO {
  customerId: number
  customerName: string
}

/**
 * 获取当前门户客户身份信息
 */
export const getPortalAuthInfoApi = () => {
  return portalRequest.get<PortalAuthInfoVO>({
    url: '/app-api/lunaerp/portal/auth/info'
  })
}
