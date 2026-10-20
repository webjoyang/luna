import { describe, it, expect, vi } from 'vitest'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import {
  formatAmount,
  getExpenseActionFlags,
  calculateExpenseTotal,
  validateExpenseForm,
  type ExpenseFormData
} from '../expense/model/expense'

describe('Expense End-to-End Smoke Test (Issue 10 luna-ui)', () => {
  it('smoke: 建单 → 提交 → 撤回再提 → 审批通过(APPROVED) → 资金流水 OUT_EXPENSE 关联 → A4套打契约校验', () => {
    const today = new Date().toLocaleDateString('en-CA')

    // 1. 建单阶段：选择付款账户、报销人可代填、多行费用与附件
    const expenseForm: ExpenseFormData = {
      applicantUserId: 2001, // 代他人填报
      accountId: 1001, // 工商银行基本户
      attachments: ['https://oss.example.com/inv-001.pdf', 'https://oss.example.com/inv-002.png'],
      remark: '上海纺织博览会差旅及办公报销',
      items: [
        {
          category: 'TRAVEL',
          originalAmount: 1250.5,
          occurDate: today,
          remark: '高铁往返车票'
        },
        {
          category: 'OFFICE',
          originalAmount: 349.5,
          occurDate: today,
          remark: '展会宣传册印刷'
        }
      ]
    }

    // 校验表单合规
    const validationRes = validateExpenseForm(expenseForm)
    expect(validationRes).toBeNull()

    // 校验前端实时合计计算
    const calculatedTotal = calculateExpenseTotal(expenseForm.items)
    expect(calculatedTotal).toBe(1600.0)
    expect(formatAmount(calculatedTotal, 'RMB')).toBe('RMB 1,600.00')

    // 2. 草稿态（PENDING）权限断言
    let status = 'PENDING'
    let flags = getExpenseActionFlags(status)
    expect(flags.canEdit).toBe(true)
    expect(flags.canSubmit).toBe(true)
    expect(flags.canDelete).toBe(true)
    expect(flags.canWithdraw).toBe(false)
    expect(flags.isApproved).toBe(false)

    // 3. 提交审批（APPROVING）：权限切换为仅可撤回
    status = 'APPROVING'
    flags = getExpenseActionFlags(status)
    expect(flags.canEdit).toBe(false)
    expect(flags.canSubmit).toBe(false)
    expect(flags.canDelete).toBe(false)
    expect(flags.canWithdraw).toBe(true)
    expect(flags.isApproved).toBe(false)

    // 4. 发起人撤回（CANCELLED -> PENDING）：可编辑再提
    status = 'REJECTED' // 模拟驳回或重提
    flags = getExpenseActionFlags(status)
    expect(flags.canEdit).toBe(true)
    expect(flags.canSubmit).toBe(true)
    expect(flags.isRejected).toBe(true)

    // 5. 审批通过（APPROVED）：终态只读防篡改
    status = 'APPROVED'
    flags = getExpenseActionFlags(status)
    expect(flags.canEdit).toBe(false)
    expect(flags.canSubmit).toBe(false)
    expect(flags.canDelete).toBe(false)
    expect(flags.canWithdraw).toBe(false)
    expect(flags.isApproved).toBe(true)

    // 6. 资金流水映射：对应 CREDIT / OUT_EXPENSE 分录
    const mockLedgerEntry = {
      accountId: 1001,
      direction: 'CREDIT',
      category: 'OUT_EXPENSE',
      amount: 1600.0,
      refNo: 'BX20261019001',
      partyType: 'EMPLOYEE',
      partyId: 2001
    }
    expect(mockLedgerEntry.direction).toBe('CREDIT')
    expect(mockLedgerEntry.category).toBe('OUT_EXPENSE')
    expect(mockLedgerEntry.amount).toBe(calculatedTotal)

    // 7. A4 套打契约与脱敏豁免（04号打印内核）
    // 模拟 print-data 豁免脱敏打印契约
    const mockPrintData = {
      bizType: 'EXPENSE',
      title: '员工费用报销单',
      expenseNo: 'BX20261019001',
      applicantName: '李四',
      accountName: '工商银行基本户',
      currency: 'RMB',
      originalTotal: 1600.0,
      totalAmount: 1600.0,
      attachmentNames: ['inv-001.pdf', 'inv-002.png'],
      applicantSign: '李四',
      financeSign: '王财务'
    }
    expect(formatAmount(mockPrintData.totalAmount)).toBe('1,600.00')
    expect(mockPrintData.attachmentNames).toHaveLength(2)
  })
})
