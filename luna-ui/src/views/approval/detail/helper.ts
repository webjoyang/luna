import { ApprovalInstanceRespVO, ApprovalNodeProgressRespVO } from '@/api/approval/instance'
import { parseSnapshotNodes, ParsedStepNode } from '@/views/approval/todo/helper'

export interface DetailStepItem {
  sort: number
  name: string
  nodeType: string
  approverNames: string[]
  stepStatus: 'finish' | 'process' | 'wait' | 'error'
  statusText: string
}

/**
 * 根据审批实例详情对象推导节点进度条渲染项
 */
export function resolveDetailStepItems(instance?: ApprovalInstanceRespVO): DetailStepItem[] {
  if (!instance) {
    return []
  }

  // 1. 优先使用后端计算好的 nodeProgress
  if (instance.nodeProgress && instance.nodeProgress.length > 0) {
    return instance.nodeProgress.map((node: ApprovalNodeProgressRespVO) => {
      let stepStatus: 'finish' | 'process' | 'wait' | 'error' = 'wait'
      let statusText = node.statusDesc || '待审批'

      if (node.status === 'PASSED') {
        stepStatus = 'finish'
        statusText = '已通过'
      } else if (node.status === 'CURRENT') {
        stepStatus = 'process'
        statusText = '审批中'
      } else if (node.status === 'REJECTED') {
        stepStatus = 'error'
        statusText = '已驳回'
      } else if (node.status === 'CANCELLED') {
        stepStatus = 'wait'
        statusText = '已撤回'
      } else {
        stepStatus = 'wait'
        statusText = '待审批'
      }

      return {
        sort: node.sort,
        name: node.name,
        nodeType: node.nodeType,
        approverNames: node.approverNames || [],
        stepStatus,
        statusText
      }
    })
  }

  // 2. 降级从流程快照 JSON 解析
  const parsedNodes: ParsedStepNode[] = parseSnapshotNodes(
    instance.processSnapshot,
    instance.currentNodeSort,
    instance.status
  )

  return parsedNodes.map((n) => ({
    sort: n.sort,
    name: n.name,
    nodeType: n.nodeType,
    approverNames: [],
    stepStatus: n.stepStatus,
    statusText: n.statusText
  }))
}

/**
 * 计算 el-steps active 属性
 */
export function getActiveStepIndex(steps: DetailStepItem[], instanceStatus?: string): number {
  if (!steps || steps.length === 0) {
    return 0
  }
  if (instanceStatus === 'APPROVED') {
    return steps.length
  }
  const currentIndex = steps.findIndex((s) => s.stepStatus === 'process' || s.stepStatus === 'error')
  return currentIndex !== -1 ? currentIndex : 0
}

/**
 * 原业务单据跳转路径
 */
export function getBizJumpUrl(bizType?: string, bizId?: number): string {
  if (!bizType || !bizId) {
    return ''
  }
  if (bizType === 'sales_contract') {
    return `/lunaerp/business/sales-contract?id=${bizId}`
  }
  return ''
}
