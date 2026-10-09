import request from '@/config/axios'

// 供应商信息 VO
export interface SupplierVO {
  id?: number
  supplierNo?: string
  companyName: string
  shortName?: string
  phone?: string
  address?: string
  supplierType?: string
  machineType?: string
  remark?: string
  status?: string
  createTime?: Date | string
}

// 供应商分页查询参数
export interface SupplierPageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  supplierType?: string
  status?: string
}

// 查询供应商分页
export const getSupplierPage = (params: SupplierPageParam) => {
  return request.get({ url: '/lunaerp/baseinfo/supplier/page', params })
}

// 查询供应商详情
export const getSupplier = (id: number) => {
  return request.get({ url: '/lunaerp/baseinfo/supplier/get?id=' + id })
}

// 预览下一个供应商编号（只读、不占号）
export const getNextSupplierNo = () => {
  return request.get<string>({ url: '/lunaerp/baseinfo/supplier/get-next-no' })
}

// 新增供应商
export const createSupplier = (data: SupplierVO) => {
  return request.post<number>({ url: '/lunaerp/baseinfo/supplier/create', data })
}

// 修改供应商
export const updateSupplier = (data: SupplierVO) => {
  return request.put({ url: '/lunaerp/baseinfo/supplier/update', data })
}

// 删除供应商
export const deleteSupplier = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/supplier/delete?id=' + id })
}

// 批量删除供应商
export const deleteSupplierList = (ids: number[]) => {
  return request.delete({ url: '/lunaerp/baseinfo/supplier/delete-list', params: { ids: ids.join(',') } })
}

// 下载供应商导入模板
export const downloadSupplierTemplate = () => {
  return request.download({ url: '/lunaerp/baseinfo/supplier/import-template' })
}

// 导入供应商（二进制响应以捕获错误 Excel 流）
export const importSupplier = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.postOriginal({
    url: '/lunaerp/baseinfo/supplier/import',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob'
  })
}
