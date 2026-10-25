import { describe, expect, it, vi } from 'vitest'
import * as ReleaseProductionApi from '@/api/lunaerp-elmui/production/release-production'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Create Finished Inspection API & Workspace Integration', () => {
  it('createFinishedInspection should call POST /lunaerp/production/release-production/create-finished-inspection with dyeingOrderId', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(999)

    const res = await ReleaseProductionApi.createFinishedInspection(1024)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/production/release-production/create-finished-inspection',
      params: { dyeingOrderId: 1024 }
    })
    expect(res).toBe(999)
  })

  it('dyeing order workspace should configure generate finished inspection button with correct permissions and status condition', async () => {
    const fs = await import('fs')
    const path = await import('path')
    const workspaceFilePath = path.resolve(__dirname, '../workspace/index.vue')
    const content = fs.readFileSync(workspaceFilePath, 'utf-8')

    // 验证按钮仅在 currentStatus === 'COMPLETED' 态展示
    expect(content).toContain("currentStatus === 'COMPLETED'")
    // 验证按钮文案
    expect(content).toContain('生成成品检验单')
    // 验证权限受 lunaerp:production:release-production:release 控制
    expect(content).toContain("v-hasPermi=\"['lunaerp:production:release-production:release']\"")
    // 验证成功后跳转检验单工作台
    expect(content).toContain('/production/inspection-order/workspace/')
  })
})
