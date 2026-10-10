export interface ParsedStepNode {
  sort: number
  name: string
  nodeType: string
  approverType: string
  approverUserIds: number[]
  stepStatus: 'finish' | 'process' | 'wait' | 'error'
  statusText: string
}

/**
 * 解析流程快照 JSON 并计算各节点的执行状态
 */
export function parseSnapshotNodes(
  snapshotJson?: string,
  currentNodeSort: number = 1,
  instanceStatus: string = 'APPROVING'
): ParsedStepNode[] {
  if (!snapshotJson || !snapshotJson.trim()) {
    return []
  }

  try {
    const snapshot = JSON.parse(snapshotJson)
    const nodes = snapshot.nodes || []
    if (!Array.isArray(nodes)) {
      return []
    }

    return nodes.map((node: any) => {
      const sort = node.sort || 0
      let stepStatus: 'finish' | 'process' | 'wait' | 'error' = 'wait'
      let statusText = '待审批'

      if (instanceStatus === 'APPROVED') {
        stepStatus = 'finish'
        statusText = '已通过'
      } else if (instanceStatus === 'REJECTED') {
        if (sort < currentNodeSort || (currentNodeSort === -1 && sort < nodes.length)) {
          stepStatus = 'finish'
          statusText = '已通过'
        } else if (sort === currentNodeSort) {
          stepStatus = 'error'
          statusText = '已驳回'
        } else {
          stepStatus = 'wait'
          statusText = '未到达'
        }
      } else if (instanceStatus === 'CANCELLED') {
        if (sort < currentNodeSort) {
          stepStatus = 'finish'
          statusText = '已通过'
        } else {
          stepStatus = 'wait'
          statusText = '已撤回'
        }
      } else {
        // APPROVING
        if (sort < currentNodeSort) {
          stepStatus = 'finish'
          statusText = '已通过'
        } else if (sort === currentNodeSort) {
          stepStatus = 'process'
          statusText = '审批中'
        } else {
          stepStatus = 'wait'
          statusText = '待审批'
        }
      }

      return {
        sort,
        name: node.name || `节点 ${sort}`,
        nodeType: node.nodeType || 'SEQUENTIAL',
        approverType: node.approverType || 'USER',
        approverUserIds: node.approverUserIds || [],
        stepStatus,
        statusText
      }
    })
  } catch {
    return []
  }
}

/**
 * 获取原业务单据的跳转路径
 */
export function getBizJumpUrl(bizType?: string, bizId?: number): string {
  if (!bizType || !bizId) {
    return ''
  }
  if (bizType === 'sales_contract') {
    return `/lunaerp/business/sales-contract?id=${bizId}`
  }
  // 未来扩展其他业务单据：payment, expense, payroll, sales_return
  return ''
}

/**
 * 根据超时/等待耗时毫秒数返回 Tag 类型
 */
export function getTimeoutTagType(timeoutMillis?: number): '' | 'warning' | 'danger' | 'info' {
  if (!timeoutMillis || timeoutMillis <= 0) {
    return 'info'
  }
  // 超过24小时报红
  if (timeoutMillis >= 24 * 60 * 60 * 1000) {
    return 'danger'
  }
  // 超过4小时报橙
  if (timeoutMillis >= 4 * 60 * 60 * 1000) {
    return 'warning'
  }
  return 'info'
}

/**
 * 校验驳回意见必填
 */
export function validateRejectComment(comment?: string): { valid: boolean; message?: string } {
  if (!comment || !comment.trim()) {
    return { valid: false, message: '驳回意见不能为空，请填写驳回原因打回发起人' }
  }
  return { valid: true }
}

/**
 * 校验转交参数
 */
export function validateTransfer(
  toUserId?: number,
  currentUserId?: number
): { valid: boolean; message?: string } {
  if (!toUserId) {
    return { valid: false, message: '请选择转交接收人' }
  }
  if (currentUserId && Number(toUserId) === Number(currentUserId)) {
    return { valid: false, message: '转交接收人不能是自己' }
  }
  return { valid: true }
}
