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
  actionName?: string
  comment?: string
  transferToId?: number
  transferToName?: string
  operateTime: string
}

export interface ApprovalNodeProgressRespVO {
  sort: number
  name: string
  nodeType: string
  approverType: string
  approverUserIds?: number[]
  approverNames?: string[]
  status: string
  statusDesc: string
}

export interface ApprovalInstanceRespVO {
  id: number
  bizType: string
  bizTypeName?: string
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
  nodeProgress?: ApprovalNodeProgressRespVO[]
  tasks?: ApprovalTaskRespVO[]
  records?: ApprovalRecordRespVO[]
}

export interface ApprovalInstanceMyPageReqVO extends PageParam {
  bizType?: string
  bizNo?: string
  status?: string
}

export interface ApprovalInstanceMyRespVO {
  id: number
  bizType: string
  bizTypeName?: string
  bizId: number
  bizNo?: string
  status: string
  submitterId: number
  submitterName?: string
  currentNodeSort: number
  currentNodeName?: string
  startTime: string
  endTime?: string
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

// 我发起的审批分页
export const getMyPage = (params: ApprovalInstanceMyPageReqVO) => {
  return request.get<{ list: ApprovalInstanceMyRespVO[]; total: number }>({
    url: '/lunaerp/approval/instance/my-page',
    params
  })
}

// 获取审批实例详情
export const getInstance = (id: number) => {
  return request.get<ApprovalInstanceRespVO>({ url: `/lunaerp/approval/instance/${id}` })
}

// 根据业务单据获取最新审批实例
export const getLatestInstanceByBiz = (params: { bizType: string; bizId: number }) => {
  return request.get<ApprovalInstanceRespVO>({ url: '/lunaerp/approval/instance/get-by-biz', params })
}

export const ApprovalInstanceApi = {
  submitInstance,
  withdrawInstance,
  getMyPage,
  getInstance,
  getLatestInstanceByBiz
}
