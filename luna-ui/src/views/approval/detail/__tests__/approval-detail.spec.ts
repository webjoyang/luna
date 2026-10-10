import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  resolveDetailStepItems,
  getActiveStepIndex,
  getBizJumpUrl
} from '../helper'
import { ApprovalInstanceApi, ApprovalInstanceRespVO } from '@/api/approval/instance'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Approval Detail Helpers & Progress Calculation', () => {
  it('resolveDetailStepItems should prioritize backend nodeProgress if provided', () => {
    const instance: ApprovalInstanceRespVO = {
      id: 501,
      bizType: 'sales_contract',
      bizId: 1001,
      processSnapshot: '',
      status: 'APPROVING',
      submitterId: 100,
      currentNodeSort: 2,
      nodeProgress: [
        {
          sort: 1,
          name: '主管审批',
          nodeType: 'SEQUENTIAL',
          approverType: 'USER',
          approverNames: ['张三'],
          status: 'PASSED',
          statusDesc: '已通过'
        },
        {
          sort: 2,
          name: '财务审核',
          nodeType: 'OR_SIGN',
          approverType: 'ROLE',
          approverNames: ['李四', '王五'],
          status: 'CURRENT',
          statusDesc: '审批中'
        },
        {
          sort: 3,
          name: '总经理审批',
          nodeType: 'AND_SIGN',
          approverType: 'USER',
          approverNames: ['赵六'],
          status: 'PENDING',
          statusDesc: '待审批'
        }
      ]
    }

    const steps = resolveDetailStepItems(instance)
    expect(steps.length).toBe(3)

    expect(steps[0].name).toBe('主管审批')
    expect(steps[0].stepStatus).toBe('finish')
    expect(steps[0].statusText).toBe('已通过')
    expect(steps[0].approverNames).toEqual(['张三'])

    expect(steps[1].name).toBe('财务审核')
    expect(steps[1].stepStatus).toBe('process')
    expect(steps[1].statusText).toBe('审批中')
    expect(steps[1].approverNames).toEqual(['李四', '王五'])

    expect(steps[2].name).toBe('总经理审批')
    expect(steps[2].stepStatus).toBe('wait')
    expect(steps[2].statusText).toBe('待审批')
  })

  it('resolveDetailStepItems should handle REJECTED status in nodeProgress', () => {
    const instance: ApprovalInstanceRespVO = {
      id: 502,
      bizType: 'sales_contract',
      bizId: 1002,
      processSnapshot: '',
      status: 'REJECTED',
      submitterId: 100,
      currentNodeSort: 1,
      nodeProgress: [
        {
          sort: 1,
          name: '主管审批',
          nodeType: 'SEQUENTIAL',
          approverType: 'USER',
          status: 'REJECTED',
          statusDesc: '已驳回'
        }
      ]
    }

    const steps = resolveDetailStepItems(instance)
    expect(steps.length).toBe(1)
    expect(steps[0].stepStatus).toBe('error')
    expect(steps[0].statusText).toBe('已驳回')
  })

  it('resolveDetailStepItems should fallback to snapshot parsing if nodeProgress is absent', () => {
    const snapshot = JSON.stringify({
      nodes: [
        { sort: 1, name: '部门主管', nodeType: 'SEQUENTIAL' },
        { sort: 2, name: '财务审核', nodeType: 'OR_SIGN' }
      ]
    })
    const instance: ApprovalInstanceRespVO = {
      id: 503,
      bizType: 'sales_contract',
      bizId: 1003,
      processSnapshot: snapshot,
      status: 'APPROVING',
      submitterId: 100,
      currentNodeSort: 1
    }

    const steps = resolveDetailStepItems(instance)
    expect(steps.length).toBe(2)
    expect(steps[0].stepStatus).toBe('process')
    expect(steps[1].stepStatus).toBe('wait')
  })

  it('getActiveStepIndex should return correct index', () => {
    const steps = [
      { sort: 1, name: 'N1', nodeType: 'S', approverNames: [], stepStatus: 'finish' as const, statusText: '已通过' },
      { sort: 2, name: 'N2', nodeType: 'S', approverNames: [], stepStatus: 'process' as const, statusText: '审批中' },
      { sort: 3, name: 'N3', nodeType: 'S', approverNames: [], stepStatus: 'wait' as const, statusText: '待审批' }
    ]
    expect(getActiveStepIndex(steps, 'APPROVING')).toBe(1)
    expect(getActiveStepIndex(steps, 'APPROVED')).toBe(3)
  })

  it('getBizJumpUrl should generate sales contract url', () => {
    expect(getBizJumpUrl('sales_contract', 2001)).toBe('/lunaerp/business/sales-contract?id=2001')
    expect(getBizJumpUrl('unknown', 2001)).toBe('')
    expect(getBizJumpUrl(undefined, 2001)).toBe('')
  })
})

describe('ApprovalInstanceApi Integration in Vitest', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('ApprovalInstanceApi.getInstance should request /lunaerp/approval/instance/{id}', async () => {
    const mockData = { id: 5001, bizType: 'sales_contract', status: 'APPROVING' }
    ;(request.get as any).mockResolvedValueOnce(mockData)

    const res = await ApprovalInstanceApi.getInstance(5001)
    expect(request.get).toHaveBeenCalledWith({ url: '/lunaerp/approval/instance/5001' })
    expect(res).toEqual(mockData)
  })

  it('ApprovalInstanceApi.getMyPage should request /lunaerp/approval/instance/my-page', async () => {
    const mockPage = { list: [], total: 0 }
    ;(request.get as any).mockResolvedValueOnce(mockPage)

    const params = { pageNo: 1, pageSize: 10, status: 'APPROVING' }
    const res = await ApprovalInstanceApi.getMyPage(params)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/my-page',
      params
    })
    expect(res).toEqual(mockPage)
  })

  it('ApprovalInstanceApi.withdrawInstance should request PUT withdraw', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)

    const res = await ApprovalInstanceApi.withdrawInstance(5001, { reason: '撤回重新提交' })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/5001/withdraw',
      data: { reason: '撤回重新提交' }
    })
    expect(res).toBe(true)
  })
})
