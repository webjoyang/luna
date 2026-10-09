import request from '@/config/axios'

/** 审批节点 VO */
export interface ApprovalConfigNodeVO {
  id?: number
  configId?: number
  sort: number
  name: string
  nodeType: string // SEQUENTIAL 逐级 / OR_SIGN 或签 / AND_SIGN 会签
  approverType: string // USER 指定用户 / POST 指定岗位 / ROLE 指定角色 / DEPT_LEADER 部门负责人 / SUBMITTER_LEADER 提交人直属上级
  approverIds?: string
}

/** 审批流程配置 VO */
export interface ApprovalConfigVO {
  id?: number
  bizType: string
  name: string
  status?: string // ENABLED 启用 / DISABLED 停用
  remark?: string
  createTime?: string
  updateTime?: string
  nodes?: ApprovalConfigNodeVO[]
}

/** 分页查询参数 */
export interface ApprovalConfigPageReqVO {
  pageNo: number
  pageSize: number
  bizType?: string
}

export const ApprovalConfigApi = {
  // 分页查询流程配置
  getApprovalConfigPage: async (params: ApprovalConfigPageReqVO) => {
    return await request.get({ url: '/lunaerp/approval/config/page', params })
  },

  // 获得流程配置详情（含节点列表按 sort 升序）
  getApprovalConfig: async (id: number) => {
    return await request.get({ url: `/lunaerp/approval/config/${id}` })
  },

  // 新建流程配置（含节点）
  createApprovalConfig: async (data: ApprovalConfigVO) => {
    return await request.post({ url: '/lunaerp/approval/config', data })
  },

  // 更新流程配置（节点整体替换）
  updateApprovalConfig: async (data: ApprovalConfigVO) => {
    return await request.put({ url: `/lunaerp/approval/config/${data.id}`, data })
  },

  // 启用/停用流程配置
  updateApprovalConfigStatus: async (id: number, status: string) => {
    return await request.put({
      url: `/lunaerp/approval/config/${id}/status`,
      data: { status }
    })
  },

  // 删除流程配置（仅未被实例引用）
  deleteApprovalConfig: async (id: number) => {
    return await request.delete({ url: `/lunaerp/approval/config/${id}` })
  }
}
