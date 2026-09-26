import request from '@/config/axios'

// 机台任务 VO
export interface MachineTaskVO {
  id?: number
  code?: string // 任务编号（MT-{织造单号去符号}-{单内 2 位序号}）
  weavingOrderId?: number // 关联织造单ID（必填）
  loomId?: number // 关联织机ID（必填）
  loomNo?: string // 机台号快照
  workerUserId?: number // 挡车工用户ID（关联 system User，可空）
  workerName?: string // 挡车工姓名快照
  plannedQty?: number // 计划匹数（正整数）
  actualQty?: number // 完工/实织匹数（只读）
  plannedStart?: string // 计划开始日期
  plannedEnd?: string // 计划结束日期
  status?: string // 状态：PENDING 待开始 / IN_PROGRESS 生产中 / HALTED 暂停异常 / COMPLETED 已完工
  haltReason?: string // 停机原因
  haltAt?: string // 停机时间
  warning?: string // 超量软警告信息
  remark?: string
  creator?: string
  createTime?: string
  updater?: string
  updateTime?: string
}

// 机台任务新建/修改请求 VO
export interface MachineTaskSaveReqVO {
  id?: number
  code?: string
  weavingOrderId?: number
  loomId?: number
  workerUserId?: number
  workerName?: string
  plannedQty?: number
  plannedStart?: string
  plannedEnd?: string
  remark?: string
}

// 机台任务保存响应 VO
export interface MachineTaskSaveRespVO {
  id: number
  code: string
  warning?: string
}

// 机台任务停机请求 VO
export interface MachineTaskHaltReqVO {
  id: number
  haltReason: string
}

// 机台任务分页查询参数
export interface MachineTaskPageParam {
  pageNo?: number
  pageSize?: number
  weavingOrderId?: number
  loomId?: number
  workerUserId?: number
  status?: string
  code?: string
}

// 查询机台任务分页
export const getMachineTaskPage = (params: MachineTaskPageParam) => {
  return request.get({ url: '/lunaerp/production/weaving-order-task/page', params })
}

// 获得机台任务详情
export const getMachineTask = (id: number) => {
  return request.get<MachineTaskVO>({ url: `/lunaerp/production/weaving-order-task/get?id=${id}` })
}

// 预览下一个机台任务编号（只读、不占号）
export const getNextMachineTaskCode = (weavingOrderId: number) => {
  return request.get<string>({
    url: `/lunaerp/production/weaving-order-task/get-next-code?weavingOrderId=${weavingOrderId}`
  })
}

// 新建机台任务（code 留空自动派生，超量软警告）
export const createMachineTask = (data: MachineTaskSaveReqVO) => {
  return request.post<MachineTaskSaveRespVO>({ url: '/lunaerp/production/weaving-order-task/create', data })
}

// 修改机台任务（排期/挡车工/匹数/织机）
export const updateMachineTask = (data: MachineTaskSaveReqVO) => {
  return request.put<MachineTaskSaveRespVO>({ url: '/lunaerp/production/weaving-order-task/update', data })
}

// 删除机台任务（仅待开始 PENDING 态允许）
export const deleteMachineTask = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/weaving-order-task/delete?id=${id}` })
}

// 机台任务停机
export const haltMachineTask = (data: MachineTaskHaltReqVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/weaving-order-task/halt', data })
}

// 机台任务恢复
export const resumeMachineTask = (id: number) => {
  return request.put<boolean>({ url: `/lunaerp/production/weaving-order-task/resume?id=${id}` })
}
