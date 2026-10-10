import { describe, expect, it, vi } from 'vitest'
import * as ApprovalInstanceApi from '@/api/approval/instance'
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

describe('Approval Instance API & Sales Contract Approval States', () => {
  it('submitInstance should post to /lunaerp/approval/instance/submit', async () => {
    vi.mocked(request.post).mockResolvedValueOnce({ data: 5001 } as any)
    const res = await ApprovalInstanceApi.submitInstance({
      bizType: 'sales_contract',
      bizId: 1024,
      comment: '请领导审批'
    })
    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/submit',
      data: {
        bizType: 'sales_contract',
        bizId: 1024,
        comment: '请领导审批'
      }
    })
  })

  it('withdrawInstance should put to /lunaerp/approval/instance/{id}/withdraw', async () => {
    vi.mocked(request.put).mockResolvedValueOnce({ data: true } as any)
    await ApprovalInstanceApi.withdrawInstance(5001, { reason: '手误撤回' })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/5001/withdraw',
      data: { reason: '手误撤回' }
    })
  })

  it('getInstance should get /lunaerp/approval/instance/{id}', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({ data: { id: 5001, status: 'APPROVING' } } as any)
    await ApprovalInstanceApi.getInstance(5001)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/5001'
    })
  })

  it('getLatestInstanceByBiz should get /lunaerp/approval/instance/get-by-biz', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({ data: { id: 5001, status: 'APPROVING' } } as any)
    await ApprovalInstanceApi.getLatestInstanceByBiz({
      bizType: 'sales_contract',
      bizId: 1024
    })
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/approval/instance/get-by-biz',
      params: { bizType: 'sales_contract', bizId: 1024 }
    })
  })

  it('dict lunaerp_approval_instance_status should contain DRAFT, APPROVING, APPROVED, REJECTED, CANCELLED', () => {
    const options = getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS)
    const values = options.map((opt) => opt.value)
    expect(values).toContain('DRAFT')
    expect(values).toContain('APPROVING')
    expect(values).toContain('APPROVED')
    expect(values).toContain('REJECTED')
    expect(values).toContain('CANCELLED')

    const draftOpt = options.find((opt) => opt.value === 'DRAFT')
    expect(draftOpt?.label).toBe('草稿')
    const approvingOpt = options.find((opt) => opt.value === 'APPROVING')
    expect(approvingOpt?.label).toBe('审批中')
  })

  describe('Contract Approval State Guard Computation', () => {
    const computeFlags = (approvalStatus: string | undefined, isViewMode: boolean, isCreateMode: boolean) => {
      const isApproving = approvalStatus === 'APPROVING'
      const isReadOnly = isViewMode || isApproving
      const canSave = !isReadOnly
      const canSubmitApproval = !isCreateMode && !isApproving
      const canWithdraw = !isCreateMode && isApproving
      const listEditDeleteDisabled = isApproving
      return {
        isApproving,
        isReadOnly,
        canSave,
        canSubmitApproval,
        canWithdraw,
        listEditDeleteDisabled
      }
    }

    it('DRAFT status in edit mode: can edit and submit approval, cannot withdraw', () => {
      const flags = computeFlags('DRAFT', false, false)
      expect(flags.isApproving).toBe(false)
      expect(flags.isReadOnly).toBe(false)
      expect(flags.canSave).toBe(true)
      expect(flags.canSubmitApproval).toBe(true)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.listEditDeleteDisabled).toBe(false)
    })

    it('APPROVING status: locked to readonly, can withdraw, cannot save/submit, list edit/delete disabled', () => {
      const flags = computeFlags('APPROVING', false, false)
      expect(flags.isApproving).toBe(true)
      expect(flags.isReadOnly).toBe(true)
      expect(flags.canSave).toBe(false)
      expect(flags.canSubmitApproval).toBe(false)
      expect(flags.canWithdraw).toBe(true)
      expect(flags.listEditDeleteDisabled).toBe(true)
    })

    it('REJECTED status in edit mode: editable, can submit approval again, cannot withdraw', () => {
      const flags = computeFlags('REJECTED', false, false)
      expect(flags.isApproving).toBe(false)
      expect(flags.isReadOnly).toBe(false)
      expect(flags.canSave).toBe(true)
      expect(flags.canSubmitApproval).toBe(true)
      expect(flags.canWithdraw).toBe(false)
    })

    it('CANCELLED status in edit mode: editable, can submit approval again, cannot withdraw', () => {
      const flags = computeFlags('CANCELLED', false, false)
      expect(flags.isApproving).toBe(false)
      expect(flags.isReadOnly).toBe(false)
      expect(flags.canSave).toBe(true)
      expect(flags.canSubmitApproval).toBe(true)
      expect(flags.canWithdraw).toBe(false)
    })

    it('Create mode: cannot submit approval or withdraw until created', () => {
      const flags = computeFlags(undefined, false, true)
      expect(flags.canSave).toBe(true)
      expect(flags.canSubmitApproval).toBe(false)
      expect(flags.canWithdraw).toBe(false)
    })
  })
})
