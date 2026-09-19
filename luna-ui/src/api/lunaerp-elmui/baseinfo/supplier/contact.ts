import request from '@/config/axios'

// 供应商联系人 VO
export interface SupplierContactVO {
  id?: number
  supplierId: number
  name: string
  position?: string
  phone?: string
  email?: string
  remark?: string
  isPrimary?: number
  createTime?: Date | string
}

// 供应商联系人分页查询参数（supplierId 必填）
export interface SupplierContactPageParam {
  pageNo?: number
  pageSize?: number
  supplierId: number
}

// 查询供应商联系人分页（按 supplierId 过滤）
export const getSupplierContactPage = (params: SupplierContactPageParam) => {
  return request.get({ url: '/lunaerp/baseinfo/supplier-contact/page', params })
}

// 查询供应商联系人详情
export const getSupplierContact = (id: number) => {
  return request.get({ url: '/lunaerp/baseinfo/supplier-contact/get?id=' + id })
}

// 新增供应商联系人
export const createSupplierContact = (data: SupplierContactVO) => {
  return request.post<number>({ url: '/lunaerp/baseinfo/supplier-contact/create', data })
}

// 修改供应商联系人
export const updateSupplierContact = (data: SupplierContactVO) => {
  return request.put({ url: '/lunaerp/baseinfo/supplier-contact/update', data })
}

// 删除供应商联系人
export const deleteSupplierContact = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/supplier-contact/delete?id=' + id })
}
