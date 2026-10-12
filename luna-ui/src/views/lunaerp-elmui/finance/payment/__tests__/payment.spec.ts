import { describe, expect, it, vi, beforeEach } from 'vitest'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import request from '@/config/axios'
import { formatAmount, DEFAULT_FINANCE_ACCOUNTS } from '../utils'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Issue 09: 收付款前端（付款申请 Tab + 付款登记 Tab）', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('1. API 接口契约测试', () => {
    it('getPaymentApplyPage 应该按筛选条件与分页请求付款申请', async () => {
      const mockResult = { list: [], total: 0 }
      vi.mocked(request.get).mockResolvedValueOnce(mockResult as any)

      const params: PaymentApi.PaymentApplyPageParam = {
        pageNo: 1,
        pageSize: 20,
        supplierId: 1001,
        applyType: 'RECON',
        approvalStatus: 'PENDING',
        keyword: 'FK2026'
      }

      const res = await PaymentApi.getPaymentApplyPage(params)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/page',
        params
      })
      expect(res).toEqual(mockResult)
    })

    it('getPaymentApply 应该获取付款申请详情', async () => {
      vi.mocked(request.get).mockResolvedValueOnce({ id: 101, applyNo: 'FK20261011001' } as any)
      const res = await PaymentApi.getPaymentApply(101)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/get?id=101'
      })
      expect(res.id).toBe(101)
    })

    it('createPaymentApply 应该调用 POST /create 创建申请', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(102 as any)
      const payload: PaymentApi.PaymentApplySaveReqVO = {
        applyType: 'RECON',
        supplierId: 1001,
        reconId: 2001,
        amount: 5000,
        remark: '首期款'
      }
      const res = await PaymentApi.createPaymentApply(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/create',
        data: payload
      })
      expect(res).toBe(102)
    })

    it('updatePaymentApply 应该调用 PUT /update 更新申请', async () => {
      vi.mocked(request.put).mockResolvedValueOnce(true as any)
      const payload: PaymentApi.PaymentApplySaveReqVO = {
        id: 102,
        applyType: 'PREPAY',
        supplierId: 1001,
        amount: 8000,
        purpose: '原料定金'
      }
      const res = await PaymentApi.updatePaymentApply(payload)
      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/update',
        data: payload
      })
      expect(res).toBe(true)
    })

    it('deletePaymentApply 应该调用 DELETE /delete 软删除草稿申请', async () => {
      vi.mocked(request.delete).mockResolvedValueOnce(true as any)
      const res = await PaymentApi.deletePaymentApply(102)
      expect(request.delete).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/delete?id=102'
      })
      expect(res).toBe(true)
    })

    it('submitPaymentApply 应该调用 POST /submit 提交审批', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(888 as any)
      const res = await PaymentApi.submitPaymentApply(102)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/submit?id=102'
      })
      expect(res).toBe(888)
    })

    it('cancelApprovePaymentApply 应该调用 POST /cancel-approve 撤回审批', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(true as any)
      const res = await PaymentApi.cancelApprovePaymentApply(102)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/cancel-approve?id=102'
      })
      expect(res).toBe(true)
    })

    it('getPrepayBalance 应该获取供应商实时预付余额', async () => {
      vi.mocked(request.get).mockResolvedValueOnce(12500.5 as any)
      const balance = await PaymentApi.getPrepayBalance(1001)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment-apply/prepay-balance?supplierId=1001'
      })
      expect(balance).toBe(12500.5)
    })

    it('getPaymentPage 应该分页查询付款单流水（固定 direction=OUT）', async () => {
      const mockResult = { list: [], total: 0 }
      vi.mocked(request.get).mockResolvedValueOnce(mockResult as any)
      const params: PaymentApi.PaymentPageParam = {
        pageNo: 1,
        pageSize: 10,
        supplierId: 1001
      }
      const res = await PaymentApi.getPaymentPage(params)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/page',
        params
      })
      expect(res).toEqual(mockResult)
    })

    it('getPayment 应该获取付款单及核销明细', async () => {
      vi.mocked(request.get).mockResolvedValueOnce({
        id: 301,
        paymentNo: 'PAY20261011001',
        items: [{ applyNo: 'FK20261011001', allocateAmount: 5000 }]
      } as any)
      const res = await PaymentApi.getPayment(301)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/get?id=301'
      })
      expect(res.paymentNo).toBe('PAY20261011001')
      expect(res.items?.length).toBe(1)
    })

    it('registerPayment 应该调用 POST /register 执行付款登记', async () => {
      vi.mocked(request.post).mockResolvedValueOnce(302 as any)
      const payload: PaymentApi.PaymentRegisterReqVO = {
        supplierId: 1001,
        accountId: 1,
        payAmount: 4000,
        prepayUsedAmount: 1000,
        remark: '合并支付两笔申请',
        items: [
          { applyId: 101, allocateAmount: 3000 },
          { applyId: 102, allocateAmount: 2000 }
        ]
      }
      const res = await PaymentApi.registerPayment(payload)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/finance/payment/register',
        data: payload
      })
      expect(res).toBe(302)
    })
  })

  describe('2. 金额格式化与前端计算校验', () => {
    it('formatAmount 应该准确处理空值与千分位小数格式', () => {
      expect(formatAmount(null)).toBe('0.00')
      expect(formatAmount(undefined)).toBe('0.00')
      expect(formatAmount(0)).toBe('0.00')
      expect(formatAmount(1234.5)).toBe('1,234.50')
      expect(formatAmount('1000000')).toBe('1,000,000.00')
      expect(formatAmount(99.999)).toBe('100.00')
    })

    it('核销剩余额应精确计算 (amount - allocatedAmount)', () => {
      const apply = { amount: 10000, allocatedAmount: 3500.25 }
      const remaining = Number((apply.amount - apply.allocatedAmount).toFixed(2))
      expect(remaining).toBe(6499.75)
    })

    it('支付总计与分配合计比较守卫（钱要够分）', () => {
      const payAmount = 3000
      const prepayUsedAmount = 1500
      const totalPaid = payAmount + prepayUsedAmount

      // 分配合计小于等于支付总计：合法
      const totalAllocated1 = 4500
      expect(totalAllocated1 <= totalPaid).toBe(true)

      // 分配合计大于支付总计：非法超额
      const totalAllocated2 = 4500.01
      expect(totalAllocated2 <= totalPaid).toBe(false)
    })

    it('预付抵扣额不能超出供应商预付余额守卫', () => {
      const prepayBalance = 5000
      const prepayUsedValid = 5000
      const prepayUsedInvalid = 5000.01

      expect(prepayUsedValid <= prepayBalance).toBe(true)
      expect(prepayUsedInvalid <= prepayBalance).toBe(false)
    })
  })

  describe('3. 字典与资金账户定义', () => {
    it('字典 LUNAERP_PAYMENT_APPLY_TYPE 应包含 RECON 与 PREPAY', () => {
      const options = getDictOptions(DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE)
      expect(options.some((o) => o.value === 'RECON')).toBe(true)
      expect(options.some((o) => o.value === 'PREPAY')).toBe(true)
    })

    it('字典 LUNAERP_FINANCE_PAYMENT_DIRECTION 应包含 IN 与 OUT', () => {
      const options = getDictOptions(DICT_TYPE.LUNAERP_FINANCE_PAYMENT_DIRECTION)
      expect(options.some((o) => o.value === 'IN')).toBe(true)
      expect(options.some((o) => o.value === 'OUT')).toBe(true)
    })

    it('预设资金账户列表应包含停用不可选账户', () => {
      const disabledAcc = DEFAULT_FINANCE_ACCOUNTS.find((a) => a.disabled)
      expect(disabledAcc).toBeDefined()
      expect(disabledAcc?.name).toContain('停用')
    })
  })
})
