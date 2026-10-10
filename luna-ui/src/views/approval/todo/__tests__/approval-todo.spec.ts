import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  parseSnapshotNodes,
  getBizJumpUrl,
  getTimeoutTagType,
  validateRejectComment,
  validateTransfer
} from '../helper'
import { ApprovalTaskApi } from '@/api/approval/task'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Approval Todo Helpers & Step Calculation', () => {
  const sampleSnapshot = JSON.stringify({
    configId: 1,
    nodes: [
      { sort: 1, name: '初审', nodeType: 'SEQUENTIAL', approverType: 'USER', approverUserIds: [101] },
      { sort: 2, name: '复审', nodeType: 'OR_SIGN', approverType: 'POST', approverUserIds: [201, 202] },
      { sort: 3, name: '终审', nodeType: 'AND_SIGN', approverType: 'ROLE', approverUserIds: [301] }
    ]
  })

  it('parseSnapshotNodes should correctly assign process/finish/wait when APPROVING', () => {
    // 当前在第 2 级
    const steps = parseSnapshotNodes(sampleSnapshot, 2, 'APPROVING')
    expect(steps.length).toBe(3)

    expect(steps[0].sort).toBe(1)
    expect(steps[0].stepStatus).toBe('finish')
    expect(steps[0].statusText).toBe('已通过')

    expect(steps[1].sort).toBe(2)
    expect(steps[1].stepStatus).toBe('process')
    expect(steps[1].statusText).toBe('审批中')

    expect(steps[2].sort).toBe(3)
    expect(steps[2].stepStatus).toBe('wait')
    expect(steps[2].statusText).toBe('待审批')
  })

  it('parseSnapshotNodes should set all nodes to finish when APPROVED', () => {
    const steps = parseSnapshotNodes(sampleSnapshot, -1, 'APPROVED')
    expect(steps.length).toBe(3)
    expect(steps.every((s) => s.stepStatus === 'finish')).toBe(true)
    expect(steps.every((s) => s.statusText === '已通过')).toBe(true)
  })

  it('parseSnapshotNodes should mark current node as error when REJECTED', () => {
    const steps = parseSnapshotNodes(sampleSnapshot, 2, 'REJECTED')
    expect(steps.length).toBe(3)
    expect(steps[0].stepStatus).toBe('finish')
    expect(steps[1].stepStatus).toBe('error')
    expect(steps[1].statusText).toBe('已驳回')
    expect(steps[2].stepStatus).toBe('wait')
    expect(steps[2].statusText).toBe('未到达')
  })

  it('parseSnapshotNodes should gracefully handle empty or invalid JSON', () => {
    expect(parseSnapshotNodes('')).toEqual([])
    expect(parseSnapshotNodes('invalid-json')).toEqual([])
    expect(parseSnapshotNodes('{}')).toEqual([])
  })

  it('getBizJumpUrl resolves sales contract detail url correctly', () => {
    expect(getBizJumpUrl('sales_contract', 1024)).toBe('/lunaerp/business/sales-contract?id=1024')
    expect(getBizJumpUrl('', 1024)).toBe('')
    expect(getBizJumpUrl('unknown', 1024)).toBe('')
  })

  it('getTimeoutTagType gives warning and danger styles on delays', () => {
    expect(getTimeoutTagType(0)).toBe('info')
    expect(getTimeoutTagType(1000 * 60 * 60)).toBe('info') // 1 hour
    expect(getTimeoutTagType(1000 * 60 * 60 * 5)).toBe('warning') // 5 hours
    expect(getTimeoutTagType(1000 * 60 * 60 * 25)).toBe('danger') // 25 hours
  })

  it('validateRejectComment enforces non-empty comment', () => {
    expect(validateRejectComment('')).toEqual({
      valid: false,
      message: '驳回意见不能为空，请填写驳回原因打回发起人'
    })
    expect(validateRejectComment('   ')).toEqual({
      valid: false,
      message: '驳回意见不能为空，请填写驳回原因打回发起人'
    })
    expect(validateRejectComment('单价不合规')).toEqual({ valid: true })
  })

  it('validateTransfer ensures receiver is selected and not self', () => {
    expect(validateTransfer(undefined, 100)).toEqual({
      valid: false,
      message: '请选择转交接收人'
    })
    expect(validateTransfer(100, 100)).toEqual({
      valid: false,
      message: '转交接收人不能是自己'
    })
    expect(validateTransfer(200, 100)).toEqual({ valid: true })
  })
})

describe('Approval Task API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getTodoPage sends GET request with params', async () => {
    ;(request.get as any).mockResolvedValueOnce({ list: [], total: 0 })
    const res = await ApprovalTaskApi.getTodoPage({ pageNo: 1, pageSize: 10, bizType: 'sales_contract' })
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/task/todo-page',
      params: { pageNo: 1, pageSize: 10, bizType: 'sales_contract' }
    })
    expect(res).toEqual({ list: [], total: 0 })
  })

  it('approveTask sends PUT request with comment', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)
    const res = await ApprovalTaskApi.approveTask(101, { comment: '同意' })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/task/101/approve',
      data: { comment: '同意' }
    })
    expect(res).toBe(true)
  })

  it('rejectTask sends PUT request with comment', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)
    const res = await ApprovalTaskApi.rejectTask(101, { comment: '打回修改' })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/task/101/reject',
      data: { comment: '打回修改' }
    })
    expect(res).toBe(true)
  })

  it('transferTask sends PUT request with toUserId and comment', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)
    const res = await ApprovalTaskApi.transferTask(101, { toUserId: 200, comment: '转交处理' })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/task/101/transfer',
      data: { toUserId: 200, comment: '转交处理' }
    })
    expect(res).toBe(true)
  })
})
