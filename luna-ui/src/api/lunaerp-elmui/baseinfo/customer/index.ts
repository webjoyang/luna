import request from '@/config/axios'

// 客户信息 VO
export interface CustomerVO {
  id?: number
  companyName: string
  shortName?: string
  phone?: string
  address?: string
  clientType: string
  industry?: string
  scale?: string
  source?: string
  cooperationStatus?: string
  unifiedSocialCreditCode?: string
  remark?: string
  status?: string
  createTime?: Date | string
}

// 客户分页查询参数
export interface CustomerPageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  status?: string
}

// 查询客户分页
export const getCustomerPage = (params: CustomerPageParam) => {
  return request.get({ url: '/lunaerp/baseinfo/customer/page', params })
}

// 查询客户详情
export const getCustomer = (id: number) => {
  return request.get({ url: '/lunaerp/baseinfo/customer/get?id=' + id })
}

// 新增客户
export const createCustomer = (data: CustomerVO) => {
  return request.post<number>({ url: '/lunaerp/baseinfo/customer/create', data })
}

// 修改客户
export const updateCustomer = (data: CustomerVO) => {
  return request.put({ url: '/lunaerp/baseinfo/customer/update', data })
}

// 删除客户
export const deleteCustomer = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/customer/delete?id=' + id })
}

// 批量删除客户
export const deleteCustomerList = (ids: number[]) => {
  return request.delete({ url: '/lunaerp/baseinfo/customer/delete-list', params: { ids: ids.join(',') } })
}
