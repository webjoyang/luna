export interface PayrollDefectRefData {
  id?: number
  lineItemId?: number
  userId: number
  nickname?: string
  confirmationId: number
  confirmationNo?: string
  rollNo?: string
  grade?: string
  confirmedAt?: string
  deductPrice: number
}

export interface PayrollLineItemData {
  id?: number
  employeeRowId?: number
  itemKind: 'EARNING' | 'DEDUCTION' | string
  itemType: string
  qty?: number
  unitPrice?: number
  amount: number
  remark?: string
  defectRefs?: PayrollDefectRefData[]
}

export interface PayrollEmployeeData {
  id?: number
  userId: number
  nickname?: string
  earningTotal?: number
  deductionTotal?: number
  netAmount?: number
  remark?: string
  items: PayrollLineItemData[]
}

export interface PayrollFormData {
  id?: number
  period: string
  title?: string
  remark?: string
  employees: PayrollEmployeeData[]
}

export interface PayrollActionFlags {
  canEdit: boolean
  canSubmit: boolean
  canCancelApprove: boolean
  canDelete: boolean
  canCancel: boolean
  canPay: boolean
  canPrint: boolean
  isApproved: boolean
  isPaid: boolean
}

/**
 * 员工工资 10 位物理错误码定义（对齐 FinanceErrorCodeConstants.java）
 */
export const PAYROLL_ERROR_CODES = {
  PAYROLL_NOT_EXISTS: 1_030_025_000,
  PAYROLL_STATUS_NOT_ALLOWED: 1_030_025_001,
  PAYROLL_PERIOD_DUPLICATE: 1_030_025_002,
  PAYROLL_EMPLOYEE_DUPLICATE: 1_030_025_003,
  PAYROLL_LINE_AMOUNT_INVALID: 1_030_025_004,
  PAYROLL_DEFECT_ALREADY_REFERENCED: 1_030_025_005,
  PAYROLL_HAS_PAYMENT_NO_CANCEL: 1_030_025_006,
  PAYROLL_PAYMENT_AMOUNT_EXCEED: 1_030_025_007,
  PAYROLL_PAYMENT_NOT_EXISTS: 1_030_025_008,
  PAYROLL_EMPTY: 1_030_025_009,
  PAYROLL_DEFECT_PRICE_REQUIRED: 1_030_025_010,
  PAYROLL_NET_AMOUNT_NEGATIVE: 1_030_025_011,
  PAYROLL_USER_NOT_EXISTS: 1_030_025_012,
  ACCOUNT_BALANCE_NOT_ENOUGH: 1_030_023_004
} as const

/**
 * 格式化金额（千分位，2位小数），安全兼容脱敏星号字符串（如 '****'）与空值
 */
export function formatAmount(val?: number | string | null, currency?: string): string {
  if (val === undefined || val === null || val === '') return '0.00'
  if (typeof val === 'string' && val.includes('*')) {
    return currency ? `${currency} ${val}` : val
  }
  const num = Number(val)
  if (isNaN(num)) return typeof val === 'string' ? val : '0.00'
  const formatted = num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
  return currency ? `${currency} ${formatted}` : formatted
}

/**
 * 计算款项行金额（根据计件数量与单价），保留2位小数
 */
export function calculateLineItemAmount(qty?: number, unitPrice?: number): number {
  if (qty === undefined || unitPrice === undefined) return 0
  const q = Number(qty)
  const p = Number(unitPrice)
  if (isNaN(q) || isNaN(p) || q <= 0 || p <= 0) return 0
  return Math.round(q * p * 100) / 100
}

/**
 * 计算单个员工的应发合计、扣款合计与实发合计
 */
export function calculateEmployeeTotals(items?: PayrollLineItemData[]): {
  earningTotal: number
  deductionTotal: number
  netAmount: number
} {
  if (!items || !Array.isArray(items)) {
    return { earningTotal: 0, deductionTotal: 0, netAmount: 0 }
  }

  let earning = 0
  let deduction = 0

  for (const item of items) {
    const amt = Number(item.amount) || 0
    if (item.itemKind === 'EARNING') {
      earning += amt
    } else if (item.itemKind === 'DEDUCTION') {
      deduction += amt
    }
  }

  earning = Math.round(earning * 100) / 100
  deduction = Math.round(deduction * 100) / 100
  const net = Math.round((earning - deduction) * 100) / 100

  return {
    earningTotal: earning,
    deductionTotal: deduction,
    netAmount: net
  }
}

/**
 * 汇总整张工资单的应发总计、扣款总计、实发总计、已发金额与未发金额
 */
export function calculatePayrollTotals(
  employees?: PayrollEmployeeData[],
  paidAmount = 0
): {
  earningTotal: number
  deductionTotal: number
  netAmount: number
  paidAmount: number
  unpaidAmount: number
} {
  if (!employees || !Array.isArray(employees)) {
    return {
      earningTotal: 0,
      deductionTotal: 0,
      netAmount: 0,
      paidAmount: paidAmount || 0,
      unpaidAmount: 0
    }
  }

  let earningSum = 0
  let deductionSum = 0

  for (const emp of employees) {
    const empTotals = calculateEmployeeTotals(emp.items)
    earningSum += empTotals.earningTotal
    deductionSum += empTotals.deductionTotal
  }

  earningSum = Math.round(earningSum * 100) / 100
  deductionSum = Math.round(deductionSum * 100) / 100
  const netSum = Math.round((earningSum - deductionSum) * 100) / 100
  const paid = Math.round((Number(paidAmount) || 0) * 100) / 100
  const unpaid = Math.round(Math.max(0, netSum - paid) * 100) / 100

  return {
    earningTotal: earningSum,
    deductionTotal: deductionSum,
    netAmount: netSum,
    paidAmount: paid,
    unpaidAmount: unpaid
  }
}

/**
 * 校验全员实发非负守卫（025_011）
 * 若有员工实发工资 < 0，返回明确错误提示，阻断提交
 */
export function validateEmployeeNetAmounts(employees?: PayrollEmployeeData[]): string | null {
  if (!employees || !Array.isArray(employees)) return null

  const negativeList: string[] = []
  for (const emp of employees) {
    const totals = calculateEmployeeTotals(emp.items)
    if (totals.netAmount < 0) {
      negativeList.push(`${emp.nickname || `用户${emp.userId}`}（实发: ${totals.netAmount}元）`)
    }
  }

  if (negativeList.length > 0) {
    return `员工实发工资不能为负数（025_011）：${negativeList.join('、')}`
  }
  return null
}

/**
 * 前端校验工资单完整性
 */
export function validatePayrollForm(formData: PayrollFormData): string | null {
  if (!formData.period || !formData.period.trim()) {
    return '工资期间不能为空'
  }
  if (!/^\d{4}-\d{2}$/.test(formData.period.trim())) {
    return '工资期间格式必须为 YYYY-MM'
  }
  if (!formData.employees || formData.employees.length === 0) {
    return '工资单员工明细至少需要一人（025_009）'
  }

  const userIds = new Set<number>()
  for (let i = 0; i < formData.employees.length; i++) {
    const emp = formData.employees[i]
    const rowIdx = i + 1
    if (!emp.userId) {
      return `第 ${rowIdx} 行员工未选择`
    }
    if (userIds.has(emp.userId)) {
      return `工资单中员工不能重复（025_003）：${emp.nickname || emp.userId}`
    }
    userIds.add(emp.userId)

    if (!emp.items || emp.items.length === 0) {
      return `员工【${emp.nickname || emp.userId}】至少需要一条款项明细`
    }

    for (let j = 0; j < emp.items.length; j++) {
      const item = emp.items[j]
      const itemIdx = j + 1
      if (!item.itemKind) {
        return `员工【${emp.nickname}】第 ${itemIdx} 项款项大类不能为空`
      }
      if (!item.itemType) {
        return `员工【${emp.nickname}】第 ${itemIdx} 项款项类型不能为空`
      }
      const amount = Number(item.amount)
      if (isNaN(amount) || amount <= 0) {
        return `员工【${emp.nickname}】第 ${itemIdx} 项款项金额必须大于0（025_004）`
      }
    }
  }

  // 负值拦截
  const netNegativeErr = validateEmployeeNetAmounts(formData.employees)
  if (netNegativeErr) {
    return netNegativeErr
  }

  return null
}

/**
 * 根据工资单审批状态与发放状态推导前端操作标识
 */
export function getPayrollActionFlags(
  approvalStatus?: string,
  paymentStatus?: string,
  paidAmount = 0
): PayrollActionFlags {
  const status = (approvalStatus || 'PENDING').toUpperCase()
  const payStatus = (paymentStatus || 'UNPAID').toUpperCase()
  const paid = Number(paidAmount) || 0

  switch (status) {
    case 'PENDING':
      return {
        canEdit: true,
        canSubmit: true,
        canCancelApprove: false,
        canDelete: true,
        canCancel: false,
        canPay: false,
        canPrint: false,
        isApproved: false,
        isPaid: false
      }
    case 'APPROVING':
      return {
        canEdit: false,
        canSubmit: false,
        canCancelApprove: true,
        canDelete: false,
        canCancel: false,
        canPay: false,
        canPrint: false,
        isApproved: false,
        isPaid: false
      }
    case 'REJECTED':
      return {
        canEdit: true,
        canSubmit: true,
        canCancelApprove: false,
        canDelete: true,
        canCancel: false,
        canPay: false,
        canPrint: false,
        isApproved: false,
        isPaid: false
      }
    case 'APPROVED': {
      const isPaid = payStatus === 'PAID'
      const canCancel = paid <= 0 // 零发放方可作废（025_006）
      const canPay = !isPaid
      return {
        canEdit: false,
        canSubmit: false,
        canCancelApprove: false,
        canDelete: false,
        canCancel,
        canPay,
        canPrint: true,
        isApproved: true,
        isPaid
      }
    }
    case 'CANCELLED':
    default:
      return {
        canEdit: false,
        canSubmit: false,
        canCancelApprove: false,
        canDelete: false,
        canCancel: false,
        canPay: false,
        canPrint: false,
        isApproved: false,
        isPaid: false
      }
  }
}

/**
 * 将次品扣款预览分组数据转换为款项明细行及 refs
 */
export function transformDefectPreviewToItems(
  groups: any[],
  unitPrice: number,
  selectedWorkerIds: number[]
): Map<number, PayrollLineItemData> {
  const result = new Map<number, PayrollLineItemData>()
  if (!groups || !Array.isArray(groups) || !unitPrice || unitPrice <= 0) return result

  const selectedSet = new Set(selectedWorkerIds)

  for (const group of groups) {
    if (!selectedSet.has(group.workerUserId)) continue

    const count = Number(group.count) || 0
    if (count <= 0) continue

    const amount = Math.round(count * unitPrice * 100) / 100
    const defectRefs: PayrollDefectRefData[] = (group.records || []).map((r: any) => ({
      confirmationId: r.confirmationId || r.id,
      confirmationNo: r.confirmationNo,
      rollNo: r.rollNo,
      grade: r.grade,
      confirmedAt: r.confirmedAt,
      deductPrice: unitPrice,
      userId: group.workerUserId,
      nickname: group.workerName
    }))

    result.set(group.workerUserId, {
      itemKind: 'DEDUCTION',
      itemType: 'DEFECT',
      qty: count,
      unitPrice,
      amount,
      remark: `次品扣款(${count}卷×${unitPrice}元)`,
      defectRefs
    })
  }

  return result
}

const NUMERIC_ERROR_MAP: Record<number, string> = {
  [PAYROLL_ERROR_CODES.PAYROLL_NOT_EXISTS]: '工资单不存在',
  [PAYROLL_ERROR_CODES.PAYROLL_STATUS_NOT_ALLOWED]: '工资单当前状态不允许此操作',
  [PAYROLL_ERROR_CODES.PAYROLL_PERIOD_DUPLICATE]: '同期间有效工资单已存在',
  [PAYROLL_ERROR_CODES.PAYROLL_EMPLOYEE_DUPLICATE]: '工资单中员工不能重复',
  [PAYROLL_ERROR_CODES.PAYROLL_LINE_AMOUNT_INVALID]: '工资款项金额必须大于0',
  [PAYROLL_ERROR_CODES.PAYROLL_DEFECT_ALREADY_REFERENCED]:
    '次品记录已被其他工资单引用或已失效，请重新导入',
  [PAYROLL_ERROR_CODES.PAYROLL_HAS_PAYMENT_NO_CANCEL]: '工资单已存在发放记录，禁止作废',
  [PAYROLL_ERROR_CODES.PAYROLL_PAYMENT_AMOUNT_EXCEED]: '本次发放金额超出剩余应发金额',
  [PAYROLL_ERROR_CODES.PAYROLL_PAYMENT_NOT_EXISTS]: '工资发放批次不存在',
  [PAYROLL_ERROR_CODES.PAYROLL_EMPTY]: '工资单员工明细至少需要一人',
  [PAYROLL_ERROR_CODES.PAYROLL_DEFECT_PRICE_REQUIRED]: '次品扣款单价必填且必须大于0',
  [PAYROLL_ERROR_CODES.PAYROLL_NET_AMOUNT_NEGATIVE]: '员工实发工资不能为负数',
  [PAYROLL_ERROR_CODES.PAYROLL_USER_NOT_EXISTS]: '员工用户不存在',
  [PAYROLL_ERROR_CODES.ACCOUNT_BALANCE_NOT_ENOUGH]: '资金账户可用余额不足'
}

const STRING_CODE_MAP: Record<string, string> = {
  '025_000': '工资单不存在',
  '025_001': '工资单当前状态不允许此操作',
  '025_002': '同期间有效工资单已存在',
  '025_003': '工资单中员工不能重复',
  '025_004': '工资款项金额必须大于0',
  '025_005': '次品记录已被其他工资单引用或已失效，请重新导入',
  '025_006': '工资单已存在发放记录，禁止作废',
  '025_007': '本次发放金额超出剩余应发金额',
  '025_008': '工资发放批次不存在',
  '025_009': '工资单员工明细至少需要一人',
  '025_010': '次品扣款单价必填且必须大于0',
  '025_011': '员工实发工资不能为负数',
  '025_012': '员工用户不存在',
  '023_004': '资金账户可用余额不足'
}

/**
 * 解析后端异常，转换为友好中文提示
 */
export function parsePayrollErrorMessage(err: any): string {
  if (!err) return '操作失败，请重试'

  const rawMsg = typeof err === 'string' ? err : err?.message || err?.msg || ''

  // 1. 若原始 message 已经包含动态占位说明（如"已被工资单【GZ202610001】引用"），优先保留原意
  if (rawMsg && (rawMsg.includes('已被工资单') || rawMsg.includes('【'))) {
    return rawMsg
  }

  // 2. 按数字 code 精确匹配
  const codeNum = Number(err?.code ?? (typeof err === 'number' ? err : NaN))
  if (!isNaN(codeNum) && NUMERIC_ERROR_MAP[codeNum]) {
    return NUMERIC_ERROR_MAP[codeNum]
  }

  // 3. 按 025_xxx / 023_xxx 字符串匹配
  for (const [code, desc] of Object.entries(STRING_CODE_MAP)) {
    if (rawMsg.includes(code)) {
      return desc
    }
  }

  return rawMsg || '操作失败，请重试'
}
