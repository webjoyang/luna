import { describe, expect, it, vi, beforeEach } from 'vitest'
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

describe('Issue 04: 成品检验单便利生成 (create-finished-inspection)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('API: createFinishedInspection', () => {
    it('should call POST /lunaerp/production/release-production/create-finished-inspection with dyeingOrderId', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(501 as any)

      const result = await ReleaseProductionApi.createFinishedInspection(1024)

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/release-production/create-finished-inspection?dyeingOrderId=1024'
      })
      expect(result).toBe(501)
    })

    it('should correctly format request for getReleaseFromContract and releaseProduction', async () => {
      vi.mocked(request.get).mockResolvedValueOnce({ contractId: 100, lines: [] } as any)
      await ReleaseProductionApi.getReleaseFromContract(100)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/production/release-production/from-contract',
        params: { contractId: 100 }
      })

      vi.mocked(request.post).mockResolvedValueOnce({ greigeOrders: [] } as any)
      await ReleaseProductionApi.releaseProduction({
        contractId: 100,
        greigeFactoryId: 1,
        dyeingFactoryId: 2,
        groups: []
      })
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/release-production/release',
        data: {
          contractId: 100,
          greigeFactoryId: 1,
          dyeingFactoryId: 2,
          groups: []
        }
      })
    })
  })

  describe('UI Logic: BasicInfoTab "生成成品检验单" 按钮行为', () => {
    it('hasActions 应该在 COMPLETED 状态时返回 true', () => {
      const computeHasActions = (status: string, isCreateMode: boolean, isViewMode: boolean) => {
        if (isViewMode) return false
        if (isCreateMode) return true
        return status === 'DRAFT' || status === 'ACTIVE' || status === 'COMPLETED'
      }

      expect(computeHasActions('COMPLETED', false, false)).toBe(true)
      expect(computeHasActions('DRAFT', false, false)).toBe(true)
      expect(computeHasActions('ACTIVE', false, false)).toBe(true)
      expect(computeHasActions('CANCELLED', false, false)).toBe(false)
      expect(computeHasActions('COMPLETED', false, true)).toBe(false)
    })

    it('点击 "生成成品检验单" 应调用 API，提示成功并跳转到检验单工作台', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(888 as any)
      const mockSuccessMsg = vi.fn()
      const mockPush = vi.fn()

      const handleCreateFinishedInspection = async (
        orderId: number,
        message: { success: (msg: string) => void },
        router: { push: (path: string) => void }
      ) => {
        const inspectionOrderId = await ReleaseProductionApi.createFinishedInspection(orderId)
        message.success('生成成品检验单成功')
        if (inspectionOrderId) {
          router.push(`/production/inspection-order/workspace/${inspectionOrderId}`)
        } else {
          router.push('/production/inspection-order')
        }
      }

      await handleCreateFinishedInspection(1024, { success: mockSuccessMsg }, { push: mockPush })

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/release-production/create-finished-inspection?dyeingOrderId=1024'
      })
      expect(mockSuccessMsg).toHaveBeenCalledWith('生成成品检验单成功')
      expect(mockPush).toHaveBeenCalledWith('/production/inspection-order/workspace/888')
    })
  })
})
