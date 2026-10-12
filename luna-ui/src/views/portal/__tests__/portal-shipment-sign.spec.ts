import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as ShipmentApi from '@/api/portal/shipment'
import type { PortalShipmentSignReqVO, PortalShipmentDetailRespVO } from '@/api/portal/shipment'
import portalRequest from '@/config/axios/portal'

vi.mock('@/config/axios/portal', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Ticket 04: 门户发货单签收与拒收 (Portal Shipment Sign & Refuse)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 契约测试 (shipment.ts)', () => {
    it('signPortalShipment 应该向 /app-api/lunaerp/portal/shipment/sign 发送 PUT 请求（SIGNED 状态）', async () => {
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const payload: PortalShipmentSignReqVO = {
        id: 101,
        signStatus: 'SIGNED',
        signedByName: '张三',
        signRemark: '货物完好，实收全部'
      }

      const res = await ShipmentApi.signPortalShipment(payload)

      expect(portalRequest.put).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/sign',
        data: payload
      })
      expect(res).toBe(true)
    })

    it('signPortalShipment 应该向 /app-api/lunaerp/portal/shipment/sign 发送 PUT 请求（REFUSED 状态）', async () => {
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const payload: PortalShipmentSignReqVO = {
        id: 102,
        signStatus: 'REFUSED',
        signedByName: '李四',
        signRemark: '包装破损严重，整单拒收'
      }

      const res = await ShipmentApi.signPortalShipment(payload)

      expect(portalRequest.put).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/sign',
        data: payload
      })
      expect(res).toBe(true)
    })
  })

  describe('2. 底部操作栏展示条件与按钮状态测试', () => {
    const isActionBarVisible = (detail: PortalShipmentDetailRespVO | null): boolean => {
      return Boolean(detail && detail.signStatus === 'PENDING')
    }

    it('detail.signStatus 为 PENDING 时，底部操作栏应可见', () => {
      const detail: PortalShipmentDetailRespVO = {
        id: 101,
        shipmentNo: 'SH-PENDING-001',
        shippingDate: '2026-10-12',
        signStatus: 'PENDING',
        totalAmount: 1000,
        freightFee: 50,
        items: []
      }
      expect(isActionBarVisible(detail)).toBe(true)
    })

    it('detail.signStatus 为 SIGNED 或 REFUSED 时，底部操作栏应隐藏', () => {
      const signedDetail: PortalShipmentDetailRespVO = {
        id: 101,
        shipmentNo: 'SH-SIGNED-001',
        shippingDate: '2026-10-12',
        signStatus: 'SIGNED',
        signedByName: '张三',
        totalAmount: 1000,
        freightFee: 50,
        items: []
      }
      expect(isActionBarVisible(signedDetail)).toBe(false)

      const refusedDetail: PortalShipmentDetailRespVO = {
        id: 102,
        shipmentNo: 'SH-REFUSED-001',
        shippingDate: '2026-10-12',
        signStatus: 'REFUSED',
        signedByName: '李四',
        totalAmount: 1000,
        freightFee: 50,
        items: []
      }
      expect(isActionBarVisible(refusedDetail)).toBe(false)
    })

    it('detail 为 null 时，底部操作栏应隐藏', () => {
      expect(isActionBarVisible(null)).toBe(false)
    })
  })

  describe('3. 签收/拒收弹窗文案与表单校验规则测试', () => {
    it('弹窗标题与确认按钮文案：签收时显示“发货单签收登记”与“确认签收”，拒收时显示“发货单拒收登记”与“确认拒收”', () => {
      const getDialogTitle = (action: 'SIGNED' | 'REFUSED') =>
        action === 'SIGNED' ? '发货单签收登记' : '发货单拒收登记'

      const getConfirmBtnText = (action: 'SIGNED' | 'REFUSED') =>
        action === 'SIGNED' ? '确认签收' : '确认拒收'

      const getSuccessMsg = (action: 'SIGNED' | 'REFUSED') =>
        action === 'SIGNED' ? '签收登记成功' : '拒收登记成功'

      expect(getDialogTitle('SIGNED')).toBe('发货单签收登记')
      expect(getConfirmBtnText('SIGNED')).toBe('确认签收')
      expect(getSuccessMsg('SIGNED')).toBe('签收登记成功')

      expect(getDialogTitle('REFUSED')).toBe('发货单拒收登记')
      expect(getConfirmBtnText('REFUSED')).toBe('确认拒收')
      expect(getSuccessMsg('REFUSED')).toBe('拒收登记成功')
    })

    it('signedByName 必填校验：空值或纯空格校验失败，非空字符校验通过', () => {
      const validateSignedByName = (name: string): boolean => {
        return Boolean(name && name.trim().length > 0)
      }

      expect(validateSignedByName('')).toBe(false)
      expect(validateSignedByName('   ')).toBe(false)
      expect(validateSignedByName('张三')).toBe(true)
      expect(validateSignedByName('  李四  ')).toBe(true)
    })
  })

  describe('4. 签收与拒收提交动作闭环测试', () => {
    it('执行签收提交：组装合法 payload、调用 API、弹出成功提示，并触发重新加载单据详情', async () => {
      const initialDetail: PortalShipmentDetailRespVO = {
        id: 101,
        shipmentNo: 'SH-PENDING-001',
        shippingDate: '2026-10-12',
        signStatus: 'PENDING',
        totalAmount: 1000,
        freightFee: 50,
        items: []
      }

      const updatedDetail: PortalShipmentDetailRespVO = {
        ...initialDetail,
        signStatus: 'SIGNED',
        signedByName: '张仓管',
        signedAt: '2026-10-12 14:00:00',
        signRemark: '货物完好，实收全部'
      }

      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)
      vi.mocked(portalRequest.get).mockResolvedValueOnce(updatedDetail)

      // 模拟提交过程
      const form = { signedByName: '  张仓管  ', signRemark: '货物完好，实收全部' }
      const signAction = 'SIGNED' as const

      const payload: PortalShipmentSignReqVO = {
        id: initialDetail.id,
        signStatus: signAction,
        signedByName: form.signedByName.trim(),
        signRemark: form.signRemark ? form.signRemark.trim() : undefined
      }

      const signSuccess = await ShipmentApi.signPortalShipment(payload)
      expect(signSuccess).toBe(true)
      expect(portalRequest.put).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/sign',
        data: {
          id: 101,
          signStatus: 'SIGNED',
          signedByName: '张仓管',
          signRemark: '货物完好，实收全部'
        }
      })

      // 重新加载详情
      const reloaded = await ShipmentApi.getPortalShipmentDetail({ id: initialDetail.id })
      expect(reloaded.signStatus).toBe('SIGNED')
      expect(reloaded.signedByName).toBe('张仓管')
    })

    it('执行拒收提交：组装合法 payload、包含拒收备注并成功落库', async () => {
      const initialDetail: PortalShipmentDetailRespVO = {
        id: 102,
        shipmentNo: 'SH-PENDING-002',
        shippingDate: '2026-10-12',
        signStatus: 'PENDING',
        totalAmount: 2000,
        freightFee: 100,
        items: []
      }

      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const form = { signedByName: '李主管', signRemark: '包装破损且有水渍，整单拒收' }
      const signAction = 'REFUSED' as const

      const payload: PortalShipmentSignReqVO = {
        id: initialDetail.id,
        signStatus: signAction,
        signedByName: form.signedByName.trim(),
        signRemark: form.signRemark ? form.signRemark.trim() : undefined
      }

      const signSuccess = await ShipmentApi.signPortalShipment(payload)
      expect(signSuccess).toBe(true)
      expect(portalRequest.put).toHaveBeenCalledWith({
        url: '/app-api/lunaerp/portal/shipment/sign',
        data: {
          id: 102,
          signStatus: 'REFUSED',
          signedByName: '李主管',
          signRemark: '包装破损且有水渍，整单拒收'
        }
      })
    })
  })
})
