import { describe, expect, it, vi, beforeEach } from 'vitest'
import {
  ApprovalInstanceApi,
  ApprovalInstanceMyPageReqVO,
  ApprovalInstanceMyRespVO
} from '@/api/approval/instance'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Approval Mine API & View Logic', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('ApprovalInstanceApi.getMyPage should call /lunaerp/approval/instance/my-page with params', async () => {
    const mockList: ApprovalInstanceMyRespVO[] = [
      {
        id: 5001,
        bizType: 'sales_contract',
        bizTypeName: '销售合同',
        bizId: 2001,
        bizNo: 'HT20261010001',
        status: 'APPROVING',
        submitterId: 100,
        submitterName: '当前用户',
        currentNodeSort: 1,
        currentNodeName: '部门主管审批',
        startTime: '2026-10-10 10:00:00'
      }
    ]
    const mockResponse = { list: mockList, total: 1 }
    ;(request.get as any).mockResolvedValueOnce(mockResponse)

    const params: ApprovalInstanceMyPageReqVO = {
      pageNo: 1,
      pageSize: 10,
      bizType: 'sales_contract',
      status: 'APPROVING'
    }

    const result = await ApprovalInstanceApi.getMyPage(params)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/my-page',
      params
    })
    expect(result.list.length).toBe(1)
    expect(result.list[0].id).toBe(5001)
    expect(result.list[0].status).toBe('APPROVING')
    expect(result.list[0].currentNodeName).toBe('部门主管审批')
  })

  it('ApprovalInstanceApi.withdrawInstance should call withdraw endpoint', async () => {
    ;(request.put as any).mockResolvedValueOnce(true)

    const res = await ApprovalInstanceApi.withdrawInstance(5001, { reason: '价格输入有误' })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/5001/withdraw',
      data: { reason: '价格输入有误' }
    })
    expect(res).toBe(true)
  })
})
