import request from '@/config/axios'

// ==================== 费用报销数据契约 ====================

export interface ExpenseItemRespVO {
  id?: number
  expenseId?: number
  category: string
  originalAmount: number | string
  occurDate: string
  remark: string
}

export interface ExpenseItemSaveReqVO {
  id?: number
  category: string
  originalAmount: number | string
  occurDate: string
  remark: string
}

export interface ExpenseRespVO {
  id: number
  expenseNo: string
  applicantUserId: number
  applicantName: string
  accountId: number
  accountName: string
  currency: string
  originalTotal: number | string
  exchangeRate?: number
  baseTotal?: number | string
  totalAmount: number | string
  expenseDate: string
  approvalStatus: string
  submitTime?: string
  approveTime?: string
  attachments?: string[]
  remark?: string
  creator?: string
  createTime?: string
  items?: ExpenseItemRespVO[]
  approvalProgress?: any
}

export interface ExpensePageReqVO extends PageParam {
  expenseNo?: string
  applicantUserId?: number
  applicantName?: string
  accountId?: number
  approvalStatus?: string
  category?: string
  expenseDateStart?: string
  expenseDateEnd?: string
}

export interface ExpenseCreateReqVO {
  applicantUserId?: number
  accountId: number
  attachments?: string[]
  remark?: string
  items: ExpenseItemSaveReqVO[]
}

export interface ExpenseUpdateReqVO {
  id: number
  applicantUserId?: number
  accountId: number
  attachments?: string[]
  remark?: string
  items: ExpenseItemSaveReqVO[]
}

export interface ExpensePrintItemRespVO {
  lineNo: number
  category: string
  originalAmount: number
  occurDate: string
  remark: string
}

export interface ExpensePrintRespVO {
  bizType: string
  title: string
  id: number
  expenseNo: string
  applicantUserId: number
  applicantName: string
  accountId: number
  accountName: string
  currency: string
  originalTotal: number
  exchangeRate?: number
  baseTotal?: number
  totalAmount: number
  expenseDate: string
  approvalStatus: string
  submitTime?: string
  approveTime?: string
  remark?: string
  creator?: string
  createTime?: string
  printTime?: string
  items: ExpensePrintItemRespVO[]
  attachmentNames: string[]
  applicantSign?: string
  departmentSign?: string
  financeSign?: string
  managerSign?: string
  cashierSign?: string
}

// ==================== 接口方法 ====================

export const getExpensePage = (params: ExpensePageReqVO) => {
  return request.get<PageResult<ExpenseRespVO>>({
    url: '/lunaerp/finance/expense/page',
    params
  })
}

export const getExpense = (id: number) => {
  return request.get<ExpenseRespVO>({
    url: `/lunaerp/finance/expense/get?id=${id}`
  })
}

export const createExpense = (data: ExpenseCreateReqVO) => {
  return request.post<number>({
    url: '/lunaerp/finance/expense/create',
    data
  })
}

export const updateExpense = (data: ExpenseUpdateReqVO) => {
  return request.put<boolean>({
    url: '/lunaerp/finance/expense/update',
    data
  })
}

export const deleteExpense = (id: number) => {
  return request.delete<boolean>({
    url: `/lunaerp/finance/expense/delete?id=${id}`
  })
}

export const submitExpense = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/finance/expense/submit?id=${id}`
  })
}

export const cancelApproveExpense = (id: number) => {
  return request.post<boolean>({
    url: `/lunaerp/finance/expense/cancel-approve?id=${id}`
  })
}

export const getExpensePrintData = (id: number) => {
  return request.get<ExpensePrintRespVO>({
    url: `/lunaerp/finance/expense/print-data?id=${id}`
  })
}
