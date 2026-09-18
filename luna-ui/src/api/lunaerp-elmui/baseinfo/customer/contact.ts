import request from '@/config/axios'

// 客户联系人 VO
export interface CustomerContactVO {
  id?: number
  customerId: number
  name: string
  position?: string
  phone?: string
  email?: string
  remark?: string
  isPrimary?: number
  createTime?: Date | string
}

// 客户联系人分页查询参数（customerId 必填）
export interface CustomerContactPageParam {
  pageNo?: number
  pageSize?: number
  customerId: number
}

// 查询客户联系人分页（按 customerId 过滤）
export const getCustomerContactPage = (params: CustomerContactPageParam) => {
  return request.get({ url: '/lunaerp/baseinfo/customer-contact/page', params })
}

// 查询客户联系人详情
export const getCustomerContact = (id: number) => {
  return request.get({ url: '/lunaerp/baseinfo/customer-contact/get?id=' + id })
}

// 新增客户联系人
export const createCustomerContact = (data: CustomerContactVO) => {
  return request.post<number>({ url: '/lunaerp/baseinfo/customer-contact/create', data })
}

// 修改客户联系人
export const updateCustomerContact = (data: CustomerContactVO) => {
  return request.put({ url: '/lunaerp/baseinfo/customer-contact/update', data })
}

// 删除客户联系人
export const deleteCustomerContact = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/customer-contact/delete?id=' + id })
}
