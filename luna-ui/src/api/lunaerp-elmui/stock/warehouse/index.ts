import request from '@/config/axios'

// 仓库信息 VO
export interface WarehouseVO {
  id?: number
  warehouseNo?: string
  name: string
  address?: string
  status?: string
  remark?: string
  createTime?: Date | string
  updateTime?: Date | string
}

// 仓库分页查询参数
export interface WarehousePageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  status?: string
}

// 查询仓库分页
export const getWarehousePage = (params: WarehousePageParam) => {
  return request.get({ url: '/lunaerp/stock/warehouse/page', params })
}

// 查询仓库详情
export const getWarehouse = (id: number) => {
  return request.get({ url: '/lunaerp/stock/warehouse/get?id=' + id })
}

// 预览下一个仓库编号（只读、不占号）
export const getNextWarehouseNo = () => {
  return request.get<string>({ url: '/lunaerp/stock/warehouse/get-next-no' })
}

// 获得所有仓库简表（供下拉选择使用，默认仅查询 ACTIVE 启用状态）
export const getWarehouseListAll = (status?: string) => {
  return request.get<WarehouseVO[]>({
    url: '/lunaerp/stock/warehouse/list-all',
    params: status ? { status } : undefined
  })
}

// 新增仓库
export const createWarehouse = (data: WarehouseVO) => {
  return request.post<number>({ url: '/lunaerp/stock/warehouse/create', data })
}

// 修改仓库
export const updateWarehouse = (data: WarehouseVO) => {
  return request.put({ url: '/lunaerp/stock/warehouse/update', data })
}

// 删除仓库
export const deleteWarehouse = (id: number) => {
  return request.delete({ url: '/lunaerp/stock/warehouse/delete?id=' + id })
}
