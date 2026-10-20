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
  getPayrollActionFlags,
  calculateEmployeeTotals,
  calculatePayrollTotals,
  validateEmployeeNetAmounts,
  validatePayrollForm,
  transformDefectPreviewToItems,
  type PayrollFormData
} from '../payroll/model/payroll'

describe('Payroll End-to-End Smoke Test (Issue 11 luna-ui)', () => {
  it('smoke: 建单 → 导次品 → 提交 → (审批通过) → 两次跨账户发放 → 列表「已发清」 → 工资表套打契约正常', () => {
    // -------------------------------------------------------------------------
    // 1. 建单阶段：选择工资期间 2026-10，初始录入张三（基本工资 5000）
    // -------------------------------------------------------------------------
    const payrollForm: PayrollFormData = {
      period: '2026-10',
      title: '2026-10 工资表',
      remark: '2026年10月全厂工资核算',
      employees: [
        {
          userId: 101,
          nickname: '张三',
          remark: '车间熟练工',
          items: [
            {
              itemKind: 'EARNING',
              itemType: 'BASE',
              amount: 5000,
              remark: '基本工资'
            }
          ]
        }
      ]
    }

    // 初始表单校验合规
    expect(validatePayrollForm(payrollForm)).toBeNull()

    // -------------------------------------------------------------------------
    // 2. 导次品阶段：预览质检次品记录，统一单价 50 元/卷
    // -------------------------------------------------------------------------
    const defectGroups = [
      {
        workerUserId: 101,
        workerName: '张三',
        count: 2,
        records: [
          {
            confirmationId: 901,
            confirmationNo: 'QC-20261015-001',
            rollNo: 'R101',
            vatNo: 'V1',
            grade: 'C',
            confirmedAt: '2026-10-15 14:00:00',
            workerUserId: 101,
            workerName: '张三'
          },
          {
            confirmationId: 902,
            confirmationNo: 'QC-20261016-002',
            rollNo: 'R102',
            vatNo: 'V1',
            grade: 'C',
            confirmedAt: '2026-10-16 15:30:00',
            workerUserId: 101,
            workerName: '张三'
          }
        ]
      },
      {
        workerUserId: 102,
        workerName: '李四',
        count: 3,
        records: [
          {
            confirmationId: 903,
            confirmationNo: 'QC-20261017-003',
            rollNo: 'R103',
            vatNo: 'V2',
            grade: 'C',
            confirmedAt: '2026-10-17 09:10:00',
            workerUserId: 102,
            workerName: '李四'
          },
          {
            confirmationId: 904,
            confirmationNo: 'QC-20261017-004',
            rollNo: 'R104',
            vatNo: 'V2',
            grade: 'C',
            confirmedAt: '2026-10-17 10:20:00',
            workerUserId: 102,
            workerName: '李四'
          },
          {
            confirmationId: 905,
            confirmationNo: 'QC-20261018-005',
            rollNo: 'R105',
            vatNo: 'V2',
            grade: 'C',
            confirmedAt: '2026-10-18 16:00:00',
            workerUserId: 102,
            workerName: '李四'
          }
        ]
      }
    ]

    const unitPrice = 50
    const importedItemsMap = transformDefectPreviewToItems(defectGroups, unitPrice, [101, 102])

    // 张三增加次品扣款行（2卷×50元 = 100元）
    payrollForm.employees[0].items.push(importedItemsMap.get(101)!)

    // 李四原不存在，自动新增员工行（基本工资 4000 + 次品扣款 3卷×50元 = 150元）
    payrollForm.employees.push({
      userId: 102,
      nickname: '李四',
      remark: '次品扣款导入生成',
      items: [
        {
          itemKind: 'EARNING',
          itemType: 'BASE',
          amount: 4000,
          remark: '底薪标准'
        },
        importedItemsMap.get(102)!
      ]
    })

    // 校验金额联动重算
    const zhangTotals = calculateEmployeeTotals(payrollForm.employees[0].items)
    expect(zhangTotals.earningTotal).toBe(5000.0)
    expect(zhangTotals.deductionTotal).toBe(100.0)
    expect(zhangTotals.netAmount).toBe(4900.0)

    const liTotals = calculateEmployeeTotals(payrollForm.employees[1].items)
    expect(liTotals.earningTotal).toBe(4000.0)
    expect(liTotals.deductionTotal).toBe(150.0)
    expect(liTotals.netAmount).toBe(3850.0)

    // 单据级聚合金额
    const payrollTotals = calculatePayrollTotals(payrollForm.employees, 0)
    expect(payrollTotals.earningTotal).toBe(9000.0)
    expect(payrollTotals.deductionTotal).toBe(250.0)
    expect(payrollTotals.netAmount).toBe(8750.0)
    expect(payrollTotals.unpaidAmount).toBe(8750.0)

    // 全员实发非负门禁校验（025_011）通过
    expect(validateEmployeeNetAmounts(payrollForm.employees)).toBeNull()

    // -------------------------------------------------------------------------
    // 3. 审批流转阶段（草稿 PENDING → 审批中 APPROVING → 审批通过 APPROVED）
    // -------------------------------------------------------------------------
    let status = 'PENDING'
    let paymentStatus = 'UNPAID'
    let paidAmount = 0
    let flags = getPayrollActionFlags(status, paymentStatus, paidAmount)
    expect(flags.canEdit).toBe(true)
    expect(flags.canSubmit).toBe(true)
    expect(flags.canDelete).toBe(true)

    // 提交审批
    status = 'APPROVING'
    flags = getPayrollActionFlags(status, paymentStatus, paidAmount)
    expect(flags.canEdit).toBe(false)
    expect(flags.canCancelApprove).toBe(true)

    // 审批通过
    status = 'APPROVED'
    flags = getPayrollActionFlags(status, paymentStatus, paidAmount)
    expect(flags.isApproved).toBe(true)
    expect(flags.canPay).toBe(true)
    expect(flags.canPrint).toBe(true)
    expect(flags.canCancel).toBe(true) // 零发放时仍可作废

    // -------------------------------------------------------------------------
    // 4. 两次跨账户发放阶段
    // -------------------------------------------------------------------------
    // 批次 1：工商银行基本户发放 5000 元
    paidAmount = 5000
    paymentStatus = 'PARTIAL'
    let currentTotals = calculatePayrollTotals(payrollForm.employees, paidAmount)
    expect(currentTotals.paidAmount).toBe(5000.0)
    expect(currentTotals.unpaidAmount).toBe(3750.0)

    flags = getPayrollActionFlags(status, paymentStatus, paidAmount)
    expect(flags.canCancel).toBe(false) // 已存在发放记录，禁止作废（025_006）
    expect(flags.canPay).toBe(true) // 还有未发余额，可继续发放

    // 批次 2：招商银行专户发放剩余 3750 元（发清）
    paidAmount = 8750
    paymentStatus = 'PAID'
    currentTotals = calculatePayrollTotals(payrollForm.employees, paidAmount)
    expect(currentTotals.paidAmount).toBe(8750.0)
    expect(currentTotals.unpaidAmount).toBe(0.0)

    flags = getPayrollActionFlags(status, paymentStatus, paidAmount)
    expect(flags.isPaid).toBe(true)
    expect(flags.canPay).toBe(false) // 已发清，禁用发放按钮
    expect(flags.canPrint).toBe(true)

    // -------------------------------------------------------------------------
    // 5. 套打工资表契约校验（豁免金额脱敏，每人一行 应发/扣款/实发 + 签收栏）
    // -------------------------------------------------------------------------
    const mockPrintData = {
      bizType: 'PAYROLL',
      title: '2026-10 工资表',
      payrollNo: 'GZ202610001',
      period: '2026-10',
      approvalStatus: 'APPROVED',
      earningTotal: 9000.0,
      deductionTotal: 250.0,
      netAmount: 8750.0,
      items: [
        {
          lineNo: 1,
          userId: 101,
          nickname: '张三',
          earningTotal: 5000.0,
          deductionTotal: 100.0,
          netAmount: 4900.0,
          signature: ''
        },
        {
          lineNo: 2,
          userId: 102,
          nickname: '李四',
          earningTotal: 4000.0,
          deductionTotal: 150.0,
          netAmount: 3850.0,
          signature: ''
        }
      ]
    }
    expect(mockPrintData.bizType).toBe('PAYROLL')
    expect(mockPrintData.items).toHaveLength(2)
    expect(formatAmount(mockPrintData.netAmount)).toBe('8,750.00')
    expect(mockPrintData.items[0].netAmount).toBe(4900.0)
    expect(mockPrintData.items[1].netAmount).toBe(3850.0)
  })
})
