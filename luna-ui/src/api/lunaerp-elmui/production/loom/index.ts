import request from '@/config/axios'

// 织机信息 VO
export interface LoomVO {
  id?: number
  loomNo?: string
  model?: string
  status?: string
  remark?: string
  createTime?: Date | string
  updateTime?: Date | string
}

// 织机分页查询参数
export interface LoomPageParam {
  pageNo?: number
  pageSize?: number
  search?: string
  status?: string
}

// 查询织机分页
export const getLoomPage = (params: LoomPageParam) => {
  return request.get({ url: '/lunaerp/production/loom/page', params })
}

// 查询织机详情
export const getLoom = (id: number) => {
  return request.get({ url: '/lunaerp/production/loom/get?id=' + id })
}

// 预览下一个织机机台号（只读、不占号）
export const getNextLoomNo = () => {
  return request.get<string>({ url: '/lunaerp/production/loom/get-next-no' })
}

// 获得所有织机简表（供下拉选择使用，默认查 ACTIVE 启用状态）
export const getLoomListAll = (status?: string) => {
  return request.get<LoomVO[]>({
    url: '/lunaerp/production/loom/list-all',
    params: status ? { status } : undefined
  })
}

// 新增织机
export const createLoom = (data: LoomVO) => {
  return request.post<number>({ url: '/lunaerp/production/loom/create', data })
}

// 修改织机
export const updateLoom = (data: LoomVO) => {
  return request.put({ url: '/lunaerp/production/loom/update', data })
}

// 删除织机
export const deleteLoom = (id: number) => {
  return request.delete({ url: '/lunaerp/production/loom/delete?id=' + id })
}
