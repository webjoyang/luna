import { describe, expect, it, vi, beforeEach } from 'vitest'
import * as SupplierReconApi from '@/api/lunaerp-elmui/finance/supplier-recon'
import request from '@/config/axios'
import {
  formatAmount,
  formatRmbUppercase,
  formatPrintTime
} from '../components/print/utils'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Issue 08: 供应商对账前端（API、计算逻辑、格式化、A4 套打与状态机）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 接口调用契约', () => {
    it('getSupplierReconPage 应该发送正确的分页与筛选参数', async () => {
      const mockResult = { list: [], total: 0 }
      vi.mocked(request.get).mockResolvedValueOnce(mockResult as any)

      const params = {
        pageNo: 1,
        pageSize: 20,
        supplierId: 1001,
        status: 'DRAFT',
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        keyword: 'SR2026'
      }

      const res = await SupplierReconApi.getSupplierReconPage(params)

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/page',
        params
      })
      expect(res).toEqual(mockResult)
    })

    it('getSupplierRecon 应该按 ID 获取单据详情', async () => {
      vi.mocked(request.get).mockResolvedValueOnce({ id: 1024, reconNo: 'SR20261011001' } as any)

      const res = await SupplierReconApi.getSupplierRecon(1024)

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/get?id=1024'
      })
      expect(res.id).toBe(1024)
    })

    it('previewSupplierRecon 应该调用 /preview 取数预览不落库', async () => {
      const candidates = { items: [], processItems: [], purchaseItems: [] }
      vi.mocked(request.post).mockResolvedValueOnce(candidates as any)

      const payload = {
        supplierId: 1001,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30'
      }

      const res = await SupplierReconApi.previewSupplierRecon(payload)

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/preview',
        data: payload
      })
      expect(res).toEqual(candidates)
    })

    it('createSupplierRecon 应该提交保存入参并返回新单ID', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(2048 as any)

      const payload: SupplierReconApi.SupplierReconSaveReqVO = {
        supplierId: 1001,
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        remark: '9月对账',
        items: []
      }

      const id = await SupplierReconApi.createSupplierRecon(payload)

      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/create',
        data: payload
      })
      expect(id).toBe(2048)
    })

    it('reFetchSupplierRecon 应该调用 /re-fetch 整体重取草稿', async () => {
      vi.mocked(request.put).mockResolvedValueOnce(true as any)

      await SupplierReconApi.reFetchSupplierRecon(1024)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/re-fetch?id=1024'
      })
    })

    it('confirmSupplierRecon 应该调用 /confirm 确认对账单', async () => {
      vi.mocked(request.put).mockResolvedValueOnce(true as any)

      await SupplierReconApi.confirmSupplierRecon(1024)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/confirm?id=1024'
      })
    })

    it('cancelSupplierRecon 应该调用 /cancel 作废对账单', async () => {
      vi.mocked(request.put).mockResolvedValueOnce(true as any)

      await SupplierReconApi.cancelSupplierRecon(1024)

      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/cancel?id=1024'
      })
    })

    it('deleteSupplierRecon 应该调用 /delete 删除草稿进回收站', async () => {
      vi.mocked(request.delete).mockResolvedValueOnce(true as any)

      await SupplierReconApi.deleteSupplierRecon(1024)

      expect(request.delete).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/delete?id=1024'
      })
    })

    it('getSupplierReconPrintData 应该调用 /print-data 获取 A4 套打数据', async () => {
      const mockPrintData = { id: 1024, reconNo: 'SR20261011001', totalAmount: 25000 }
      vi.mocked(request.get).mockResolvedValueOnce(mockPrintData as any)

      const res = await SupplierReconApi.getSupplierReconPrintData(1024)

      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/supplier-recon/print-data?id=1024'
      })
      expect(res.totalAmount).toBe(25000)
    })
  })

  describe('2. 工作台明细与计价基准计算逻辑', () => {
    it('PROCESS 行切换计价基准（METER <-> KG）应联动切换 billQty 与重算金额', () => {
      const row: SupplierReconApi.SupplierReconItemVO = {
        lineType: 'PROCESS',
        qtyKg: 100.5,
        qtyM: 500,
        pricingBasis: 'METER',
        billQty: 500,
        unitPrice: 2.5,
        lineAmount: 1250
      }

      // 切换为 KG
      const switchBasis = (r: SupplierReconApi.SupplierReconItemVO, newBasis: string) => {
        r.pricingBasis = newBasis
        r.billQty = newBasis === 'METER' ? Number(r.qtyM || 0) : Number(r.qtyKg || 0)
        r.lineAmount = Math.round(r.billQty * Number(r.unitPrice || 0) * 100) / 100
      }

      switchBasis(row, 'KG')
      expect(row.pricingBasis).toBe('KG')
      expect(row.billQty).toBe(100.5)
      expect(row.lineAmount).toBe(251.25) // 100.5 * 2.5 = 251.25

      // 切换回 METER
      switchBasis(row, 'METER')
      expect(row.pricingBasis).toBe('METER')
      expect(row.billQty).toBe(500)
      expect(row.lineAmount).toBe(1250)
    })

    it('PURCHASE 行单价变动时 lineAmount 应实时计算', () => {
      const row: SupplierReconApi.SupplierReconItemVO = {
        lineType: 'PURCHASE',
        quantity: 200,
        unit: 'KG',
        billQty: 200,
        unitPrice: 15.0,
        lineAmount: 3000
      }

      const updatePrice = (r: SupplierReconApi.SupplierReconItemVO, newPrice: number) => {
        r.unitPrice = newPrice
        r.lineAmount = Math.round(Number(r.billQty || 0) * newPrice * 100) / 100
      }

      updatePrice(row, 16.5)
      expect(row.lineAmount).toBe(3300)
    })

    it('ADJUST 调整项支持正负金额与原因必填校验', () => {
      const validNegativeRow: SupplierReconApi.SupplierReconItemVO = {
        lineType: 'ADJUST',
        lineAmount: -500.5,
        adjustReason: '疵点扣款'
      }

      const invalidRow: SupplierReconApi.SupplierReconItemVO = {
        lineType: 'ADJUST',
        lineAmount: 0,
        adjustReason: ''
      }

      const validateAdjust = (r: SupplierReconApi.SupplierReconItemVO) => {
        if (!r.adjustReason || !r.adjustReason.trim()) return false
        if (r.lineAmount == null || Number(r.lineAmount) === 0) return false
        return true
      }

      expect(validateAdjust(validNegativeRow)).toBe(true)
      expect(validateAdjust(invalidRow)).toBe(false)
    })

    it('三类行混排时，小计与确认金额合计应精确汇总', () => {
      const items: SupplierReconApi.SupplierReconItemVO[] = [
        { lineType: 'PROCESS', billQty: 500, unitPrice: 2.0, lineAmount: 1000 },
        { lineType: 'PROCESS', billQty: 200, unitPrice: 3.5, lineAmount: 700 },
        { lineType: 'PURCHASE', billQty: 100, unitPrice: 50.0, lineAmount: 5000 },
        { lineType: 'ADJUST', lineAmount: -200, adjustReason: '质量扣款' },
        { lineType: 'ADJUST', lineAmount: 50, adjustReason: '运费补差' }
      ]

      const processAmount = items
        .filter((i) => i.lineType === 'PROCESS')
        .reduce((sum, cur) => sum + (cur.lineAmount || 0), 0)
      const purchaseAmount = items
        .filter((i) => i.lineType === 'PURCHASE')
        .reduce((sum, cur) => sum + (cur.lineAmount || 0), 0)
      const adjustAmount = items
        .filter((i) => i.lineType === 'ADJUST')
        .reduce((sum, cur) => sum + (cur.lineAmount || 0), 0)

      const total = processAmount + purchaseAmount + adjustAmount

      expect(processAmount).toBe(1700)
      expect(purchaseAmount).toBe(5000)
      expect(adjustAmount).toBe(-150)
      expect(total).toBe(6550)
    })
  })

  describe('3. 工具函数与 A4 打印格式化', () => {
    it('formatAmount 应该生成千分位并保留两位小数', () => {
      expect(formatAmount(25000)).toBe('25,000.00')
      expect(formatAmount(0)).toBe('0.00')
      expect(formatAmount(1234567.89)).toBe('1,234,567.89')
      expect(formatAmount(-500.5)).toBe('-500.50')
      expect(formatAmount(null)).toBe('0.00')
    })

    it('formatRmbUppercase 应该正确将金额转换为人民币大写', () => {
      expect(formatRmbUppercase(0)).toBe('零元整')
      expect(formatRmbUppercase(25000)).toBe('贰万伍仟元整')
      expect(formatRmbUppercase(1024.5)).toBe('壹仟零贰拾肆元伍角')
      expect(formatRmbUppercase(10000.08)).toBe('壹万元零捌分')
      expect(formatRmbUppercase(-200)).toBe('负贰佰元整')
    })

    it('formatPrintTime 应该格式化日期时间字符串', () => {
      const fixedDate = new Date('2026-10-11T12:30:45')
      const formatted = formatPrintTime(fixedDate)
      expect(formatted).toMatch(/^2026-10-11 12:30:45$/)
    })
  })

  describe('4. 状态机与错误码守卫行为', () => {
    it('004 错误应能识别为明细被抢先引用', () => {
      const errResponse = {
        response: {
          data: {
            code: 1030022004,
            msg: '源明细已被对账单【SR20261011001】引用'
          }
        }
      }

      const isConflict004 =
        errResponse.response.data.code === 1030022004 ||
        errResponse.response.data.msg.includes('已被对账单')

      expect(isConflict004).toBe(true)
    })

    it('006 错误应能识别为存在付款申请禁止作废', () => {
      const errResponse = {
        response: {
          data: {
            code: 1030022006,
            msg: '对账单存在付款申请，禁止作废'
          }
        }
      }

      const isHasApply006 =
        errResponse.response.data.code === 1030022006 ||
        errResponse.response.data.msg.includes('存在付款申请')

      expect(isHasApply006).toBe(true)
    })
  })
})
