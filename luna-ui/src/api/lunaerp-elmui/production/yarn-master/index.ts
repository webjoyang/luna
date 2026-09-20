import request from '@/config/axios'

// ==================== 纱线材质 VO & API ====================

export interface YarnTypeVO {
  id?: number
  name: string
  sort?: number
  status?: string
  createTime?: Date | string
}

export interface YarnTypePageParam {
  pageNo?: number
  pageSize?: number
  name?: string
  status?: string
}

// 查询纱线材质分页
export const getYarnTypePage = (params: YarnTypePageParam) => {
  return request.get({ url: '/lunaerp/production/yarn-type/page', params })
}

// 查询纱线材质详情
export const getYarnType = (id: number) => {
  return request.get<YarnTypeVO>({ url: `/lunaerp/production/yarn-type/get?id=${id}` })
}

// 新增纱线材质
export const createYarnType = (data: YarnTypeVO) => {
  return request.post<number>({ url: '/lunaerp/production/yarn-type/create', data })
}

// 修改纱线材质
export const updateYarnType = (data: YarnTypeVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/yarn-type/update', data })
}

// 删除纱线材质
export const deleteYarnType = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/yarn-type/delete?id=${id}` })
}

// ==================== 纱线规格 VO & API ====================

export interface YarnSpecVO {
  id?: number
  yarnTypeId: number
  yarnTypeName?: string
  specName: string
  standardName: string
  spinMethod?: string
  luster?: string
  refPrice?: number
  remark?: string
  status?: string
  createTime?: Date | string
}

export interface YarnSpecPageParam {
  pageNo?: number
  pageSize?: number
  yarnTypeId?: number
  specName?: string
  standardName?: string
  status?: string
}

export interface YarnSpecOptionVO {
  id: number
  specName: string
  standardName: string
  refPrice?: number
  spinMethod?: string
  luster?: string
}

export interface YarnSpecCascadeOptionVO {
  yarnTypeId: number
  yarnTypeName: string
  specs: YarnSpecOptionVO[]
}

// 查询纱线规格分页
export const getYarnSpecPage = (params: YarnSpecPageParam) => {
  return request.get({ url: '/lunaerp/production/yarn-spec/page', params })
}

// 查询纱线规格详情
export const getYarnSpec = (id: number) => {
  return request.get<YarnSpecVO>({ url: `/lunaerp/production/yarn-spec/get?id=${id}` })
}

// 新增纱线规格
export const createYarnSpec = (data: YarnSpecVO) => {
  return request.post<number>({ url: '/lunaerp/production/yarn-spec/create', data })
}

// 修改纱线规格
export const updateYarnSpec = (data: YarnSpecVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/yarn-spec/update', data })
}

// 删除纱线规格
export const deleteYarnSpec = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/yarn-spec/delete?id=${id}` })
}

// 获得两级级联规格数据源
export const getYarnSpecCascadeOptions = (params?: { keyword?: string }) => {
  return request.get<YarnSpecCascadeOptionVO[]>({
    url: '/lunaerp/production/yarn-spec/cascade-options',
    params
  })
}
