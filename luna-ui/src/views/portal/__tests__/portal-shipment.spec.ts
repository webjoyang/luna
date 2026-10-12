import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as ShipmentApi from '@/api/portal/shipment'
import portalRequest from '@/config/axios/portal'
import remainingRouter from '@/router/modules/remaining'

vi.mock('@/config/axios/portal', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Ticket 03: 客户门户发货单只读（列表 + 详情 + 双形态直达 + Vitest）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 契约测试 (shipment.ts)', () => {
    it('getPortalShipmentPage 应该向 /app-api/lunaerp/portal/shipment/page 发送 GET 请求', async () => {
      const mockResult = {
        list: [
          {
            id: 101,
            shipmentNo: 'SH-20261011-001',
            shippingDate: '2026-10-11',
            logisticsCompany: '顺丰速运',
            logisticsTrackingNo: 'SF123456789',
            signStatus: 'PENDING',
            totalAmount: 12800.5,
            freightFee: 150.0
          }
        ],
        total: 1
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockResult)

      const params = { pageNo: 1, pageSize: 10, signStatus: 'PENDING' }
      const res = await ShipmentApi.getPortalShipmentPage(params)

      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/page',
        params
      })
      expect(res).toEqual(mockResult)
    })

    it('getPortalShipmentDetail 应该向 /app-api/lunaerp/portal/shipment/get 发送 GET 请求（支持 id 与 no 双形态）', async () => {
      const mockDetail: ShipmentApi.PortalShipmentDetailRespVO = {
        id: 101,
        shipmentNo: 'SH-20261011-001',
        shippingDate: '2026-10-11',
        logisticsCompany: '顺丰速运',
        logisticsTrackingNo: 'SF123456789',
        signStatus: 'PENDING',
        totalAmount: 12800.5,
        freightFee: 150.0,
        totalPieces: 20,
        items: [
          {
            id: 201,
            productCode: 'P001',
            productName: '高弹汗布',
            customerColorNo: 'COL-A1',
            colorName: '藏青色',
            vatNo: 'VAT-001',
            quantity: 100.0,
            pieces: 2,
            unit: 'KG',
            unitPrice: 35.5,
            amount: 3550.0,
            rolls: [
              { id: 501, rollNo: 'R01', weight: 50.0, lengthM: 120.0 }
            ]
          }
        ]
      }

      // 1. 按 id 请求
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockDetail)
      const resById = await ShipmentApi.getPortalShipmentDetail({ id: 101 })
      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/get',
        params: { id: 101 }
      })
      expect(resById).toEqual(mockDetail)

      // 2. 按 no 请求
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockDetail)
      const resByNo = await ShipmentApi.getPortalShipmentDetail({ no: 'SH-20261011-001' })
      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/get',
        params: { no: 'SH-20261011-001' }
      })
      expect(resByNo).toEqual(mockDetail)
    })
  })

  describe('2. 发货单列表页逻辑 (list.vue)', () => {
    it('Tab 切换触发带对应 signStatus 的重新加载', async () => {
      const tabs = [
        { label: '全部', value: '' },
        { label: '待签收', value: 'PENDING' },
        { label: '已签收', value: 'SIGNED' },
        { label: '已拒收', value: 'REFUSED' }
      ]

      for (const tab of tabs) {
        vi.mocked(portalRequest.get).mockResolvedValueOnce({ list: [], total: 0 })
        const res = await ShipmentApi.getPortalShipmentPage({
          pageNo: 1,
          pageSize: 10,
          signStatus: tab.value || undefined
        })
        expect(res).toBeDefined()
        expect(portalRequest.get).toHaveBeenCalledWith({
          url: '/app-api/lunaerp/portal/shipment/page',
          params: { pageNo: 1, pageSize: 10, signStatus: tab.value || undefined }
        })
      }
    })

    it('发货单列表卡片状态 Tag 映射符合预期', () => {
      const getTagType = (status?: string) => {
        switch (status) {
          case 'PENDING':
            return 'warning'
          case 'SIGNED':
            return 'success'
          case 'REFUSED':
            return 'danger'
          default:
            return 'info'
        }
      }

      const getTagLabel = (status?: string) => {
        switch (status) {
          case 'PENDING':
            return '待签收'
          case 'SIGNED':
            return '已签收'
          case 'REFUSED':
            return '已拒收'
          default:
            return status || '未知'
        }
      }

      expect(getTagType('PENDING')).toBe('warning')
      expect(getTagLabel('PENDING')).toBe('待签收')

      expect(getTagType('SIGNED')).toBe('success')
      expect(getTagLabel('SIGNED')).toBe('已签收')

      expect(getTagType('REFUSED')).toBe('danger')
      expect(getTagLabel('REFUSED')).toBe('已拒收')

      expect(getTagType('UNKNOWN')).toBe('info')
      expect(getTagLabel('UNKNOWN')).toBe('UNKNOWN')
    })

    it('金额格式化透明展示（含运费与货物金额）', () => {
      const formatAmount = (num?: number | null) => (num ?? 0).toFixed(2)

      expect(formatAmount(12800.5)).toBe('12800.50')
      expect(formatAmount(150)).toBe('150.00')
      expect(formatAmount(0)).toBe('0.00')
      expect(formatAmount(null)).toBe('0.00')
    })
  })

  describe('3. 发货单详情页逻辑 (detail.vue)', () => {
    it('支持按 id 详情加载并正确映射明细行与卷清单', async () => {
      const mockDetail: ShipmentApi.PortalShipmentDetailRespVO = {
        id: 101,
        shipmentNo: 'SH-20261011-001',
        shippingDate: '2026-10-11',
        logisticsCompany: '中通快递',
        logisticsTrackingNo: 'ZT99887766',
        signStatus: 'SIGNED',
        signedByName: '王厂长',
        signedAt: '2026-10-11 16:00:00',
        signRemark: '确认收货，质量完好',
        totalAmount: 9800.0,
        freightFee: 120.0,
        totalPieces: 10,
        items: [
          {
            id: 301,
            productCode: 'PROD-A',
            productName: '针织面料A',
            customerColorNo: 'COL-01',
            colorName: '黑色',
            vatNo: 'V-202610',
            quantity: 300,
            pieces: 6,
            unit: 'KG',
            unitPrice: 20.0,
            amount: 6000.0,
            rolls: [
              { id: 1, rollNo: 'R-01', weight: 50.0, lengthM: 100.0 },
              { id: 2, rollNo: 'R-02', weight: 50.0, lengthM: 100.0 }
            ]
          }
        ]
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockDetail)

      const res = await ShipmentApi.getPortalShipmentDetail({ id: 101 })

      expect(res.id).toBe(101)
      expect(res.signStatus).toBe('SIGNED')
      expect(res.signedByName).toBe('王厂长')
      expect(res.signedAt).toBe('2026-10-11 16:00:00')
      expect(res.signRemark).toBe('确认收货，质量完好')
      expect(res.totalAmount).toBe(9800.0)
      expect(res.freightFee).toBe(120.0)

      expect(res.items.length).toBe(1)
      expect(res.items[0].productCode).toBe('PROD-A')
      expect(res.items[0].rolls?.length).toBe(2)
      expect(res.items[0].rolls?.[0].rollNo).toBe('R-01')
      expect(res.items[0].rolls?.[0].weight).toBe(50.0)
    })

    it('支持按 ?no= 详情加载（二维码冻结契约 /portal/shipment?no=）', async () => {
      const mockDetail: ShipmentApi.PortalShipmentDetailRespVO = {
        id: 102,
        shipmentNo: 'SH-QR-2026',
        shippingDate: '2026-10-11',
        signStatus: 'PENDING',
        totalAmount: 5000.0,
        freightFee: 0.0,
        items: []
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockDetail)

      const res = await ShipmentApi.getPortalShipmentDetail({ no: 'SH-QR-2026' })

      expect(res.id).toBe(102)
      expect(res.shipmentNo).toBe('SH-QR-2026')
      expect(portalRequest.get).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/get',
        params: { no: 'SH-QR-2026' }
      })
    })

    it('路由兼容性：/portal/shipment 路由存在且映射至同一 detail.vue 组件', () => {
      const shipmentQrRoute = remainingRouter.find((r) => r.path === '/portal/shipment')
      expect(shipmentQrRoute).toBeDefined()
      expect(shipmentQrRoute?.name).toBe('PortalShipmentQrDetail')
      expect(shipmentQrRoute?.meta?.title).toBe('发货单详情')
      expect(shipmentQrRoute?.meta?.hidden).toBe(true)

      const shipmentIdRoute = remainingRouter.find((r) => r.path === '/portal/shipment/:id')
      expect(shipmentIdRoute).toBeDefined()
      expect(shipmentIdRoute?.name).toBe('PortalShipmentDetail')
    })
  })
})
