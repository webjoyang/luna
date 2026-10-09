import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  createDefaultNode,
  reindexNodes,
  moveNodeUp,
  moveNodeDown,
  parseUserIds,
  serializeUserIds,
  validateNodes
} from '../helper'
import { ApprovalConfigApi } from '@/api/approval/config'
import request from '@/config/axios'
import { DICT_TYPE, getDictOptions, getStrDictOptions } from '@/utils/dict'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Approval Config Helpers & Node Operations', () => {
  it('createDefaultNode should create node with valid initial values', () => {
    const node = createDefaultNode(1)
    expect(node.sort).toBe(1)
    expect(node.name).toBe('节点 1')
    expect(node.nodeType).toBe('SEQUENTIAL')
    expect(node.approverType).toBe('USER')
    expect(node.approverIds).toBe('[]')
  })

  it('reindexNodes should update sorts to 1..N contiguous order', () => {
    const nodes = [
      { sort: 10, name: 'A', nodeType: 'SEQUENTIAL', approverType: 'USER' },
      { sort: 50, name: 'B', nodeType: 'OR_SIGN', approverType: 'POST' }
    ]
    const reindexed = reindexNodes(nodes)
    expect(reindexed[0].sort).toBe(1)
    expect(reindexed[1].sort).toBe(2)
  })

  it('moveNodeUp should swap node with previous and reindex', () => {
    const nodes = [
      createDefaultNode(1),
      { ...createDefaultNode(2), name: 'Second Node' },
      { ...createDefaultNode(3), name: 'Third Node' }
    ]

    const moved = moveNodeUp(nodes, 1)
    expect(moved[0].name).toBe('Second Node')
    expect(moved[0].sort).toBe(1)
    expect(moved[1].name).toBe('节点 1')
    expect(moved[1].sort).toBe(2)

    // 上移第一个节点不应改变
    const noop = moveNodeUp(nodes, 0)
    expect(noop).toEqual(nodes)
  })

  it('moveNodeDown should swap node with next and reindex', () => {
    const nodes = [
      createDefaultNode(1),
      { ...createDefaultNode(2), name: 'Second Node' },
      { ...createDefaultNode(3), name: 'Third Node' }
    ]

    const moved = moveNodeDown(nodes, 0)
    expect(moved[0].name).toBe('Second Node')
    expect(moved[0].sort).toBe(1)
    expect(moved[1].name).toBe('节点 1')
    expect(moved[1].sort).toBe(2)

    // 下移最后一个节点不应改变
    const noop = moveNodeDown(nodes, 2)
    expect(noop).toEqual(nodes)
  })

  it('parseUserIds and serializeUserIds should handle JSON array and fallback', () => {
    expect(parseUserIds('["101", 102]')).toEqual([101, 102])
    expect(parseUserIds('101,102')).toEqual([101, 102])
    expect(parseUserIds('')).toEqual([])
    expect(parseUserIds(undefined)).toEqual([])

    expect(serializeUserIds([101, 102])).toBe('[101,102]')
    expect(serializeUserIds([])).toBe('[]')
  })

  it('validateNodes should validate nodes thoroughly', () => {
    // 1. 空节点列表
    expect(validateNodes([])).toEqual({ valid: false, message: '请至少添加一个审批节点' })

    // 2. 节点名称为空
    expect(
      validateNodes([{ sort: 1, name: '', nodeType: 'SEQUENTIAL', approverType: 'USER' }])
    ).toEqual({ valid: false, message: '第 1 个节点的名称不能为空' })

    // 3. USER 类型未选择人员
    expect(
      validateNodes([
        { sort: 1, name: '主管审批', nodeType: 'SEQUENTIAL', approverType: 'USER', approverIds: '[]' }
      ])
    ).toEqual({
      valid: false,
      message: '第 1 个节点【主管审批】来源为指定用户，必须选择至少一位审批人'
    })

    // 4. POST 类型未选择岗位
    expect(
      validateNodes([
        { sort: 1, name: '岗位审批', nodeType: 'OR_SIGN', approverType: 'POST', approverIds: '' }
      ])
    ).toEqual({
      valid: false,
      message: '第 1 个节点【岗位审批】来源为指定岗位，必须选择审批岗位'
    })

    // 5. ROLE 类型未选择角色
    expect(
      validateNodes([
        { sort: 1, name: '角色审批', nodeType: 'AND_SIGN', approverType: 'ROLE', approverIds: '' }
      ])
    ).toEqual({
      valid: false,
      message: '第 1 个节点【角色审批】来源为指定角色，必须选择审批角色'
    })

    // 6. DEPT_LEADER / SUBMITTER_LEADER 无需额外参数，合法通过
    expect(
      validateNodes([
        { sort: 1, name: '部门负责人审批', nodeType: 'SEQUENTIAL', approverType: 'DEPT_LEADER' },
        { sort: 2, name: '直属上级审批', nodeType: 'SEQUENTIAL', approverType: 'SUBMITTER_LEADER' },
        { sort: 3, name: '用户审批', nodeType: 'OR_SIGN', approverType: 'USER', approverIds: '[1, 2]' }
      ])
    ).toEqual({ valid: true })
  })
})

describe('Approval Config API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getApprovalConfigPage sends GET request with params', async () => {
    ;(request.get as any).mockResolvedValueOnce({ list: [], total: 0 })
    const res = await ApprovalConfigApi.getApprovalConfigPage({ pageNo: 1, pageSize: 10, bizType: 'sales_contract' })
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/config/page',
      params: { pageNo: 1, pageSize: 10, bizType: 'sales_contract' }
    })
    expect(res).toEqual({ list: [], total: 0 })
  })

  it('getApprovalConfig sends GET request with id', async () => {
    ;(request.get as any).mockResolvedValueOnce({ id: 1024, name: '流程' })
    const res = await ApprovalConfigApi.getApprovalConfig(1024)
    expect(request.get).toHaveBeenCalledWith({ url: '/lunaerp/approval/config/1024' })
    expect(res).toEqual({ id: 1024, name: '流程' })
  })

  it('createApprovalConfig sends POST request with data', async () => {
    ;(request.post as any).mockResolvedValueOnce(1024)
    const payload = {
      bizType: 'payment',
      name: '付款审批',
      status: 'ENABLED',
      nodes: [createDefaultNode(1)]
    }
    const res = await ApprovalConfigApi.createApprovalConfig(payload)
    expect(request.post).toHaveBeenCalledWith({ url: '/lunaerp/approval/config', data: payload })
    expect(res).toBe(1024)
  })

  it('updateApprovalConfig sends PUT request with id and data', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)
    const payload = {
      id: 1024,
      bizType: 'payment',
      name: '更新付款审批',
      nodes: [createDefaultNode(1)]
    }
    const res = await ApprovalConfigApi.updateApprovalConfig(payload)
    expect(request.put).toHaveBeenCalledWith({ url: '/lunaerp/approval/config/1024', data: payload })
    expect(res).toBe(true)
  })

  it('updateApprovalConfigStatus sends PUT request to status endpoint', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)
    const res = await ApprovalConfigApi.updateApprovalConfigStatus(1024, 'DISABLED')
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/config/1024/status',
      data: { status: 'DISABLED' }
    })
    expect(res).toBe(true)
  })

  it('deleteApprovalConfig sends DELETE request to endpoint', async () => {
    ;(request.delete as any).mockResolvedValueOnce(true)
    const res = await ApprovalConfigApi.deleteApprovalConfig(1024)
    expect(request.delete).toHaveBeenCalledWith({ url: '/lunaerp/approval/config/1024' })
    expect(res).toBe(true)
  })
})

describe('Approval Dictionary Integration', () => {
  it('DICT_TYPE contains all required approval engine dict keys', () => {
    expect(DICT_TYPE.LUNAERP_APPROVAL_CONFIG_STATUS).toBe('lunaerp_approval_config_status')
    expect(DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE).toBe('lunaerp_approval_biz_type')
    expect(DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE).toBe('lunaerp_approval_node_type')
    expect(DICT_TYPE.LUNAERP_APPROVAL_APPROVER_TYPE).toBe('lunaerp_approval_approver_type')
  })

  it('getDictOptions fallback returns configured approval options', () => {
    const bizTypes = getDictOptions(DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE)
    expect(bizTypes.length).toBe(5)
    expect(bizTypes.map((b) => b.value)).toContain('sales_contract')
    expect(bizTypes.map((b) => b.value)).toContain('payment')

    const nodeTypes = getDictOptions(DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE)
    expect(nodeTypes.map((n) => n.value)).toEqual(['SEQUENTIAL', 'OR_SIGN', 'AND_SIGN'])

    const statuses = getDictOptions(DICT_TYPE.LUNAERP_APPROVAL_CONFIG_STATUS)
    expect(statuses.map((s) => s.value)).toEqual(['ENABLED', 'DISABLED'])
  })
})

describe('Approval Route and Guard Integration', () => {
  it('remaining.ts includes /approval/config route with correct name and component', async () => {
    const fs = await import('fs')
    const path = await import('path')
    const remainingContent = fs.readFileSync(
      path.resolve(__dirname, '../../../../router/modules/remaining.ts'),
      'utf-8'
    )
    expect(remainingContent).toContain("path: 'config'")
    expect(remainingContent).toContain("name: 'ApprovalConfig'")
    expect(remainingContent).toContain("component: () => import('@/views/approval/config/index.vue')")
    expect(remainingContent).toContain("title: '审批链配置'")
  })

  it('deleteApprovalConfig throws and returns 1030030008 error when flow is referenced', async () => {
    const errorMsg = '流程已被引用，无法删除'
    ;(request.delete as any).mockRejectedValueOnce(new Error(errorMsg))

    await expect(ApprovalConfigApi.deleteApprovalConfig(999)).rejects.toThrow(errorMsg)
  })
})

