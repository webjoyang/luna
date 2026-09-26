import request from '@/config/axios'

// ==================== 1. 厂商凭证 ====================

export interface VendorCredentialVO {
  id?: number
  vendorAccount: string
  vendorPassword?: string
  scope?: string
  status?: string
  hasPassword?: boolean
  lastSyncAt?: string | Date
  createTime?: string | Date
  updateTime?: string | Date
}

export interface VendorCredentialPageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  vendorAccount?: string
  status?: string
}

export const getVendorCredentialPage = (params: VendorCredentialPageParam) => {
  return request.get({ url: '/lunaerp/production/iot/vendor-credential/page', params })
}

export const getVendorCredential = (id: number) => {
  return request.get<VendorCredentialVO>({ url: `/lunaerp/production/iot/vendor-credential/get?id=${id}` })
}

export const createVendorCredential = (data: VendorCredentialVO) => {
  return request.post<number>({ url: '/lunaerp/production/iot/vendor-credential/create', data })
}

export const updateVendorCredential = (data: VendorCredentialVO) => {
  return request.put({ url: '/lunaerp/production/iot/vendor-credential/update', data })
}

export const deleteVendorCredential = (id: number) => {
  return request.delete({ url: `/lunaerp/production/iot/vendor-credential/delete?id=${id}` })
}

// ==================== 2. 设备绑定 ====================

export interface DeviceBindingVO {
  id?: number
  vendorDeviceId: string
  loomId: number
  loomNo?: string
  loomModel?: string
  status?: string
  lastSyncAt?: string | Date
  createTime?: string | Date
  updateTime?: string | Date
}

export interface DeviceBindingPageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  vendorDeviceId?: string
  loomId?: number
  status?: string
}

export const getDeviceBindingPage = (params: DeviceBindingPageParam) => {
  return request.get({ url: '/lunaerp/production/iot/device-binding/page', params })
}

export const getDeviceBinding = (id: number) => {
  return request.get<DeviceBindingVO>({ url: `/lunaerp/production/iot/device-binding/get?id=${id}` })
}

export const createDeviceBinding = (data: DeviceBindingVO) => {
  return request.post<number>({ url: '/lunaerp/production/iot/device-binding/create', data })
}

export const updateDeviceBinding = (data: DeviceBindingVO) => {
  return request.put({ url: '/lunaerp/production/iot/device-binding/update', data })
}

export const deleteDeviceBinding = (id: number) => {
  return request.delete({ url: `/lunaerp/production/iot/device-binding/delete?id=${id}` })
}

export const getDeviceBindingByLoomId = (loomId: number) => {
  return request.get<DeviceBindingVO>({ url: `/lunaerp/production/iot/device-binding/get-by-loom?loomId=${loomId}` })
}

// ==================== 3. 实时快照 ====================

export interface MachineRealtimeVO {
  id?: number
  vendorDeviceId: string
  deviceSn?: string
  loomId?: number
  loomNo?: string
  status?: string
  runEfficiencyPct?: number
  minuteCircleSpeed?: number
  shiftCircles?: number
  limitSpeed?: number
  runMinutes?: number
  shiftMinutes?: number
  staffOnShift?: string
  fetchedAt?: string | Date
  createTime?: string | Date
  updateTime?: string | Date
}

export interface MachineRealtimePageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  vendorDeviceId?: string
  status?: string
}

export const recordRealtimeData = (data: MachineRealtimeVO) => {
  return request.post<number>({ url: '/lunaerp/production/iot/machine-realtime/record', data })
}

export const getLatestRealtime = (params: { vendorDeviceId?: string; loomId?: number }) => {
  return request.get<MachineRealtimeVO>({ url: '/lunaerp/production/iot/machine-realtime/get-latest', params })
}

export const getMachineRealtimePage = (params: MachineRealtimePageParam) => {
  return request.get({ url: '/lunaerp/production/iot/machine-realtime/page', params })
}

export const triggerSimulatedCollection = () => {
  return request.post<number>({ url: '/lunaerp/production/iot/machine-realtime/collect-mock' })
}
