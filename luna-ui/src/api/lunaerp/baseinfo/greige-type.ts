import request from '@/config/axios'

// 坯布类型 VO
export interface GreigeTypeVO {
  id?: number
  code: string
  name: string
  matchKey?: string
  fabricType: string
  composition: string
  yarnSpec: string
  weightGsm: number
  widthCm: number
  craftType?: string
  machineType?: string
  remark?: string
  status?: string
  createTime?: Date
}

// 坯布类型精简 VO
export type GreigeTypeSimpleVO = Pick<
  GreigeTypeVO,
  'id' | 'code' | 'name' | 'fabricType' | 'composition' | 'yarnSpec' | 'weightGsm' | 'widthCm'
>

// 查询坯布类型分页
export const getGreigeTypePage = (params: PageParam) => {
  return request.get({ url: '/lunaerp/baseinfo/greige-type/page', params })
}

// 查询坯布类型精简列表
export const getSimpleGreigeTypeList = () => {
  return request.get<GreigeTypeSimpleVO[]>({ url: '/lunaerp/baseinfo/greige-type/simple-list' })
}

// 查询坯布类型详情
export const getGreigeType = (id: number) => {
  return request.get({ url: '/lunaerp/baseinfo/greige-type/get?id=' + id })
}

// 新增坯布类型
export const createGreigeType = (data: GreigeTypeVO) => {
  return request.post({ url: '/lunaerp/baseinfo/greige-type/create', data })
}

// 修改坯布类型
export const updateGreigeType = (data: GreigeTypeVO) => {
  return request.put({ url: '/lunaerp/baseinfo/greige-type/update', data })
}

// 删除坯布类型
export const deleteGreigeType = (id: number) => {
  return request.delete({ url: '/lunaerp/baseinfo/greige-type/delete?id=' + id })
}
