import request from '@/config/axios'

export interface ApprovalTaskTodoPageReqVO extends PageParam {
  bizType?: string
  bizNo?: string
}

export interface ApprovalTaskTodoRespVO {
  id: number
  instanceId: number
  nodeSort: number
  nodeName: string
  nodeType: string
  approverId: number
  approverName?: string
  bizType: string
  bizTypeName?: string
  bizId: number
  bizNo?: string
  submitterId: number
  submitterName?: string
  startTime: string
  instanceStartTime?: string
  timeoutDuration?: number
  timeoutDurationDesc?: string
  status: string
}

export interface ApprovalTaskApproveReqVO {
  comment?: string
}

export interface ApprovalTaskRejectReqVO {
  comment: string
}

export interface ApprovalTaskTransferReqVO {
  toUserId: number
  comment?: string
}

export const ApprovalTaskApi = {
  // 待我审批分页
  getTodoPage: (params: ApprovalTaskTodoPageReqVO) => {
    return request.get<{ list: ApprovalTaskTodoRespVO[]; total: number }>({
      url: '/lunaerp/approval/task/todo-page',
      params
    })
  },

  // 审批通过
  approveTask: (id: number, data?: ApprovalTaskApproveReqVO) => {
    return request.put<boolean>({
      url: `/lunaerp/approval/task/${id}/approve`,
      data
    })
  },

  // 审批驳回
  rejectTask: (id: number, data: ApprovalTaskRejectReqVO) => {
    return request.put<boolean>({
      url: `/lunaerp/approval/task/${id}/reject`,
      data
    })
  },

  // 审批转交
  transferTask: (id: number, data: ApprovalTaskTransferReqVO) => {
    return request.put<boolean>({
      url: `/lunaerp/approval/task/${id}/transfer`,
      data
    })
  }
}
