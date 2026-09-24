import request from '@/config/axios'

// 库位信息 VO
export interface WarehouseLocationVO {
  id?: number
  warehouseId: number
  code: string
  name?: string
  sort?: number
  status?: string
  remark?: string
  createTime?: Date | string
  updateTime?: Date | string
}

// 库位分页查询参数
export interface WarehouseLocationPageParam {
  pageNo?: number
  pageSize?: number
  warehouseId?: number
  search?: string
  status?: string
}

// 查询库位分页
export const getWarehouseLocationPage = (params: WarehouseLocationPageParam) => {
  return request.get({ url: '/lunaerp/stock/warehouse-location/page', params })
}

// 查询库位详情
export const getWarehouseLocation = (id: number) => {
  return request.get({ url: '/lunaerp/stock/warehouse-location/get?id=' + id })
}

// 获得指定仓库下的启用库位列表（供下拉选择使用）
export const getWarehouseLocationListByWarehouse = (warehouseId: number, status?: string) => {
  return request.get<WarehouseLocationVO[]>({
    url: '/lunaerp/stock/warehouse-location/list-by-warehouse',
    params: { warehouseId, ...(status ? { status } : {}) }
  })
}

// 新增库位
export const createWarehouseLocation = (data: WarehouseLocationVO) => {
  return request.post<number>({ url: '/lunaerp/stock/warehouse-location/create', data })
}

// 修改库位
export const updateWarehouseLocation = (data: WarehouseLocationVO) => {
  return request.put({ url: '/lunaerp/stock/warehouse-location/update', data })
}

// 删除库位
export const deleteWarehouseLocation = (id: number) => {
  return request.delete({ url: '/lunaerp/stock/warehouse-location/delete?id=' + id })
}
