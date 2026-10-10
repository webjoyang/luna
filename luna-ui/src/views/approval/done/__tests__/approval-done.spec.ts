import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ApprovalTaskApi, ApprovalTaskDonePageReqVO, ApprovalTaskDoneRespVO } from '@/api/approval/task'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Approval Done API & View Logic', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('ApprovalTaskApi.getDonePage should call /lunaerp/approval/task/done-page with params', async () => {
    const mockList: ApprovalTaskDoneRespVO[] = [
      {
        id: 301,
        instanceId: 5001,
        taskId: 101,
        bizType: 'sales_contract',
        bizTypeName: '销售合同',
        bizId: 2001,
        bizNo: 'HT20261010001',
        instanceStatus: 'APPROVED',
        submitterId: 100,
        submitterName: '销售员小李',
        operatorId: 200,
        operatorName: '财务审批人',
        action: 'APPROVE',
        actionName: '通过',
        comment: '合同条款核对无误，予以通过',
        operateTime: '2026-10-10 11:00:00',
        nodeName: '财务复核'
      }
    ]
    const mockResponse = { list: mockList, total: 1 }
    ;(request.get as any).mockResolvedValueOnce(mockResponse)

    const params: ApprovalTaskDonePageReqVO = {
      pageNo: 1,
      pageSize: 10,
      bizType: 'sales_contract',
      bizNo: 'HT2026',
      action: 'APPROVE'
    }

    const result = await ApprovalTaskApi.getDonePage(params)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/task/done-page',
      params
    })
    expect(result.list.length).toBe(1)
    expect(result.list[0].id).toBe(301)
    expect(result.list[0].action).toBe('APPROVE')
    expect(result.list[0].actionName).toBe('通过')
    expect(result.list[0].instanceStatus).toBe('APPROVED')
    expect(result.list[0].nodeName).toBe('财务复核')
  })

  it('ApprovalTaskApi.getDonePage should support empty filters', async () => {
    const mockResponse = { list: [], total: 0 }
    ;(request.get as any).mockResolvedValueOnce(mockResponse)

    const params: ApprovalTaskDonePageReqVO = {
      pageNo: 1,
      pageSize: 20
    }

    const result = await ApprovalTaskApi.getDonePage(params)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/task/done-page',
      params
    })
    expect(result.total).toBe(0)
    expect(result.list).toEqual([])
  })
})
