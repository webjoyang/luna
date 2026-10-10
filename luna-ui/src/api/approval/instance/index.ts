import request from '@/config/axios'

export interface ApprovalTaskRespVO {
  id: number
  instanceId: number
  nodeSort: number
  nodeName: string
  nodeType: string
  approverId: number
  approverName?: string
  status: string
  action?: string
  startTime: string
  endTime?: string
}

export interface ApprovalRecordRespVO {
  id: number
  instanceId: number
  taskId?: number
  operatorId: number
  operatorName?: string
  action: string
  comment?: string
  transferToId?: number
  transferToName?: string
  operateTime: string
}

export interface ApprovalInstanceRespVO {
  id: number
  bizType: string
  bizId: number
  bizNo?: string
  processSnapshot: string
  status: string
  submitterId: number
  submitterName?: string
  currentNodeSort: number
  flowableProcInstId?: string
  startTime: string
  endTime?: string
  tasks?: ApprovalTaskRespVO[]
  records?: ApprovalRecordRespVO[]
}

export interface ApprovalInstanceSubmitReqVO {
  bizType: string
  bizId: number
  comment?: string
}

export interface ApprovalInstanceWithdrawReqVO {
  reason?: string
}

// 发起审批
export const submitInstance = (data: ApprovalInstanceSubmitReqVO) => {
  return request.post<number>({ url: '/lunaerp/approval/instance/submit', data })
}

// 撤回审批
export const withdrawInstance = (id: number, data?: ApprovalInstanceWithdrawReqVO) => {
  return request.put<boolean>({ url: `/lunaerp/approval/instance/${id}/withdraw`, data })
}

// 获取审批实例详情
export const getInstance = (id: number) => {
  return request.get<ApprovalInstanceRespVO>({ url: `/lunaerp/approval/instance/${id}` })
}

// 根据业务单据获取最新审批实例
export const getLatestInstanceByBiz = (params: { bizType: string; bizId: number }) => {
  return request.get<ApprovalInstanceRespVO>({ url: '/lunaerp/approval/instance/get-by-biz', params })
}
