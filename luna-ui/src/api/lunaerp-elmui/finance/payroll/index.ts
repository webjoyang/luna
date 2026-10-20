import request from '@/config/axios'

// ==================== 员工工资单数据契约 ====================

export interface PayrollDefectRefRespVO {
  id: number
  lineItemId: number
  userId: number
  nickname: string
  confirmationId: number
  confirmationNo: string
  rollNo: string
  grade: string
  confirmedAt: string
  deductPrice: number | string
}

export interface PayrollDefectRefSaveReqVO {
  userId?: number
  nickname?: string
  confirmationId: number
  confirmationNo?: string
  rollNo?: string
  grade?: string
  confirmedAt?: string
  deductPrice: number
}

export interface PayrollLineItemRespVO {
  id?: number
  payrollId?: number
  employeeRowId?: number
  itemKind: 'EARNING' | 'DEDUCTION' | string
  itemType: string
  qty?: number | string
  unitPrice?: number | string
  amount: number | string
  remark?: string
  defectRefs?: PayrollDefectRefRespVO[]
}

export interface PayrollLineItemSaveReqVO {
  itemKind: 'EARNING' | 'DEDUCTION' | string
  itemType: string
  qty?: number
  unitPrice?: number
  amount?: number
  remark?: string
  defectRefs?: PayrollDefectRefSaveReqVO[]
}

export interface PayrollEmployeeRespVO {
  id?: number
  payrollId?: number
  userId: number
  nickname: string
  earningTotal: number | string
  deductionTotal: number | string
  netAmount: number | string
  remark?: string
  items: PayrollLineItemRespVO[]
}

export interface PayrollEmployeeSaveReqVO {
  userId: number
  remark?: string
  items: PayrollLineItemSaveReqVO[]
}

export interface PayrollPaymentRespVO {
  id: number
  paymentNo: string
  payrollId: number
  payrollNo: string
  period: string
  accountId: number
  accountName: string
  currency: string
  originalAmount: number | string
  exchangeRate?: number
  baseAmount?: number | string
  payDate: string
  remark?: string
  creator?: string
  createTime?: string
}

export interface PayrollRespVO {
  id: number
  payrollNo: string
  period: string
  title: string
  approvalStatus: string
  paymentStatus: string // UNPAID 未发放 / PARTIAL 部分发放 / PAID 已发清
  earningTotal: number | string
  deductionTotal: number | string
  netAmount: number | string
  paidAmount: number | string
  submitTime?: string
  approveTime?: string
  cancelledBy?: string
  cancelledAt?: string
  cancelReason?: string
  remark?: string
  creator?: string
  createTime?: string
  employees?: PayrollEmployeeRespVO[]
  payments?: PayrollPaymentRespVO[]
  approvalProgress?: any
}

export interface PayrollPageReqVO extends PageParam {
  payrollNo?: string
  period?: string
  approvalStatus?: string
  paymentStatus?: string
}

export interface PayrollCreateReqVO {
  period: string
  title?: string
  remark?: string
  employees?: PayrollEmployeeSaveReqVO[]
}

export interface PayrollUpdateReqVO {
  id: number
  title?: string
  remark?: string
  employees: PayrollEmployeeSaveReqVO[]
}

export interface PayrollCancelReqVO {
  id?: number
  cancelReason: string
}

export interface DefectRecord {
  confirmationId: number
  confirmationNo?: string
  rollNo: string
  vatNo?: string
  grade: string
  confirmedAt?: string
  workerUserId: number
  workerName: string
}

export interface DefectWorkerGroup {
  workerUserId: number
  workerName: string
  count: number
  records: DefectRecord[]
}

export interface PayrollDefectPreviewRespVO {
  period: string
  groups: DefectWorkerGroup[]
}

export interface PayrollPaymentRegisterReqVO {
  payrollId: number
  accountId: number
  amount: number
  payDate?: string
  remark?: string
}

export interface PayrollPaymentPageReqVO extends PageParam {
  payrollId: number
}

export interface PayrollPrintEmployeeRespVO {
  lineNo: number
  userId: number
  nickname: string
  earningTotal: number
  deductionTotal: number
  netAmount: number
  signature?: string
  remark?: string
}

export interface PayrollPrintRespVO {
  bizType: string
  title: string
  id: number
  payrollNo: string
  period: string
  approvalStatus: string
  earningTotal: number
  deductionTotal: number
  netAmount: number
  submitTime?: string
  approveTime?: string
  remark?: string
  printTime?: string
  items: PayrollPrintEmployeeRespVO[]
}

// ==================== 接口方法 ====================

export const getPayrollPage = (params: PayrollPageReqVO) => {
  return request.get<PageResult<PayrollRespVO>>({
    url: '/lunaerp/finance/payroll/page',
    params
  })
}

export const getPayroll = (id: number) => {
  return request.get<PayrollRespVO>({
    url: `/lunaerp/finance/payroll/get?id=${id}`
  })
}

export const createPayroll = (data: PayrollCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payroll/create',
    data
  })
}

export const updatePayroll = (data: PayrollUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/payroll/update',
    data
  })
}

export const deletePayroll = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/finance/payroll/delete?id=${id}`
  })
}

export const submitPayroll = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/finance/payroll/submit?id=${id}`
  })
}

export const cancelApprovePayroll = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/finance/payroll/cancel-approve?id=${id}`
  })
}

export const cancelPayroll = (id: number, cancelReason: string) => {
  return request.put<boolean>({
    url: `/lunaerp/finance/payroll/cancel?id=${id}`,
    data: { id, cancelReason }
  })
}

export const getDefectPreview = (period: string, payrollId?: number) => {
  return request.get<PayrollDefectPreviewRespVO>({
    url: '/lunaerp/finance/payroll/defect-preview',
    params: { period, payrollId }
  })
}

export const registerPayrollPayment = (data: PayrollPaymentRegisterReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/payroll/payment/register',
    data
  })
}

export const getPayrollPaymentPage = (params: PayrollPaymentPageReqVO) => {
  return request.get<PageResult<PayrollPaymentRespVO>>({
    url: '/lunaerp/finance/payroll/payment/page',
    params
  })
}

export const getPayrollPrintData = (id: number) => {
  return request.get<PayrollPrintRespVO>({
    url: `/lunaerp/finance/payroll/print-data?id=${id}`
  })
}
