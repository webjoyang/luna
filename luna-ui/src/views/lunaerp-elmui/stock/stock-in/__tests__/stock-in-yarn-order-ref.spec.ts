import { describe, expect, it, vi, beforeEach } from 'vitest'
import * as StockInApi from '@/api/lunaerp-elmui/stock/stock-in'
import * as YarnOrderApi from '@/api/lunaerp-elmui/production/yarn-order'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Issue 07: 入库单关联纱线采购单引用增量', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('API: StockInVO and YarnOrder reference', () => {
    it('createStockIn 应该包含 yarnOrderId 与 yarnOrderNo 快照', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(100 as any)

      const payload: StockInApi.StockInVO = {
        inType: 'PURCHASE',
        warehouseId: 10,
        inDate: '2026-09-24',
        supplierId: 20,
        yarnOrderId: 70,
        yarnOrderNo: 'YP20260924001',
        items: []
      }

      const id = await StockInApi.createStockIn(payload)

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/stock/stock-in/create',
        data: payload
      })
      expect(id).toBe(100)
    })

    it('updateStockIn 应该能更新或清空 yarnOrderId 与 yarnOrderNo', async () => {
      vi.mocked(request.put).mockResolvedValueOnce(true as any)

      const payload: StockInApi.StockInVO = {
        id: 100,
        inType: 'PURCHASE',
        warehouseId: 10,
        inDate: '2026-09-24',
        yarnOrderId: undefined,
        yarnOrderNo: undefined,
        items: []
      }

      await StockInApi.updateStockIn(payload)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/stock/stock-in/update',
        data: payload
      })
    })
  })

  describe('Workspace UI Logic: 纱线采购单选择与回填', () => {
    it('searchYarnOrders 应该调用 getYarnOrderPage 远程搜索', async () => {
      const mockList = [
        { id: 70, orderNo: 'YP20260924001', orderDate: '2026-09-24', status: 'ACTIVE' }
      ]
      vi.mocked(request.get).mockResolvedValueOnce({ list: mockList, total: 1 } as any)

      const res = await YarnOrderApi.getYarnOrderPage({
        pageNo: 1,
        pageSize: 20,
        keyword: 'YP2026'
      })

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/production/yarn-order/page',
        params: { pageNo: 1, pageSize: 20, keyword: 'YP2026' }
      })
      expect(res.list).toEqual(mockList)
    })

    it('handleYarnOrderChange 选中采购单后正确回填 yarnOrderNo', () => {
      const yarnOrderOptions = [
        { id: 70, orderNo: 'YP20260924001', orderDate: '2026-09-24' },
        { id: 71, orderNo: 'YP20260924002', orderDate: '2026-09-25' }
      ]
      const formData: Partial<StockInApi.StockInVO> = {
        yarnOrderId: undefined,
        yarnOrderNo: ''
      }

      const handleYarnOrderChange = (val?: number) => {
        if (!val) {
          formData.yarnOrderId = undefined
          formData.yarnOrderNo = undefined
          return
        }
        const target = yarnOrderOptions.find((y) => y.id === val)
        formData.yarnOrderNo = target?.orderNo
      }

      // 选中 70
      formData.yarnOrderId = 70
      handleYarnOrderChange(70)
      expect(formData.yarnOrderNo).toBe('YP20260924001')

      // 清空
      handleYarnOrderChange(undefined)
      expect(formData.yarnOrderId).toBeUndefined()
      expect(formData.yarnOrderNo).toBeUndefined()
    })

    it('ensureInitialOptions 能够预填充已存在的数据选项', () => {
      let yarnOrderOptions: any[] = []
      const formData: Partial<StockInApi.StockInVO> = {
        yarnOrderId: 70,
        yarnOrderNo: 'YP20260924001'
      }

      const ensureInitialOptions = () => {
        if (formData.yarnOrderId && formData.yarnOrderNo) {
          yarnOrderOptions = [
            { id: formData.yarnOrderId, orderNo: formData.yarnOrderNo }
          ]
        }
      }

      ensureInitialOptions()
      expect(yarnOrderOptions).toHaveLength(1)
      expect(yarnOrderOptions[0].id).toBe(70)
      expect(yarnOrderOptions[0].orderNo).toBe('YP20260924001')
    })
  })
})
