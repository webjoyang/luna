import type { ApprovalConfigNodeVO } from '@/api/approval/config'

/** 创建默认节点 */
export function createDefaultNode(sort: number): ApprovalConfigNodeVO {
  return {
    sort,
    name: `节点 ${sort}`,
    nodeType: 'SEQUENTIAL',
    approverType: 'USER',
    approverIds: '[]'
  }
}

/** 重新按 1..N 连续编号 */
export function reindexNodes(nodes: ApprovalConfigNodeVO[]): ApprovalConfigNodeVO[] {
  return nodes.map((node, index) => ({
    ...node,
    sort: index + 1
  }))
}

/** 节点上移 */
export function moveNodeUp(nodes: ApprovalConfigNodeVO[], index: number): ApprovalConfigNodeVO[] {
  if (index <= 0 || index >= nodes.length) {
    return nodes
  }
  const cloned = [...nodes]
  const temp = cloned[index - 1]
  cloned[index - 1] = cloned[index]
  cloned[index] = temp
  return reindexNodes(cloned)
}

/** 节点下移 */
export function moveNodeDown(nodes: ApprovalConfigNodeVO[], index: number): ApprovalConfigNodeVO[] {
  if (index < 0 || index >= nodes.length - 1) {
    return nodes
  }
  const cloned = [...nodes]
  const temp = cloned[index + 1]
  cloned[index + 1] = cloned[index]
  cloned[index] = temp
  return reindexNodes(cloned)
}

/** 解析 USER 类型的用户 ID 列表 */
export function parseUserIds(approverIds?: string): number[] {
  if (!approverIds || !approverIds.trim()) {
    return []
  }
  try {
    const parsed = JSON.parse(approverIds)
    if (Array.isArray(parsed)) {
      return parsed.map((id) => Number(id)).filter((id) => !Number.isNaN(id))
    }
  } catch {
    // 兼容逗号分隔 "1,2"
    return approverIds
      .split(',')
      .map((s) => Number(s.trim()))
      .filter((id) => !Number.isNaN(id))
  }
  return []
}

/** 序列化 USER 类型的用户 ID 列表为 JSON */
export function serializeUserIds(userIds: number[]): string {
  if (!userIds || userIds.length === 0) {
    return '[]'
  }
  return JSON.stringify(userIds)
}

/** 校验节点列表完整性 */
export function validateNodes(nodes?: ApprovalConfigNodeVO[]): { valid: boolean; message?: string } {
  if (!nodes || nodes.length === 0) {
    return { valid: false, message: '请至少添加一个审批节点' }
  }

  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[i]
    const seq = i + 1

    if (!node.name || !node.name.trim()) {
      return { valid: false, message: `第 ${seq} 个节点的名称不能为空` }
    }

    if (!node.nodeType) {
      return { valid: false, message: `第 ${seq} 个节点未选择节点类型` }
    }

    if (!node.approverType) {
      return { valid: false, message: `第 ${seq} 个节点未选择审批人来源` }
    }

    if (node.approverType === 'USER') {
      const userIds = parseUserIds(node.approverIds)
      if (userIds.length === 0) {
        return { valid: false, message: `第 ${seq} 个节点【${node.name}】来源为指定用户，必须选择至少一位审批人` }
      }
    } else if (node.approverType === 'POST') {
      if (!node.approverIds || !node.approverIds.trim()) {
        return { valid: false, message: `第 ${seq} 个节点【${node.name}】来源为指定岗位，必须选择审批岗位` }
      }
    } else if (node.approverType === 'ROLE') {
      if (!node.approverIds || !node.approverIds.trim()) {
        return { valid: false, message: `第 ${seq} 个节点【${node.name}】来源为指定角色，必须选择审批角色` }
      }
    }
  }

  return { valid: true }
}
