import request from '@/config/axios'

// 打色颜色行摘要 VO（列表色号 chips）
export interface ColorSummaryVO {
  customerColorNo?: string
  colorName?: string
  factoryColorNo?: string
  colorHex?: string
  round?: number
  status?: string
}

// 打色颜色行明细 VO
export interface ColorInstructionColorVO {
  id?: number
  instructionId?: number
  lineNo?: number
  customerColorNo?: string
  colorName?: string
  factoryColorNo?: string
  colorHex?: string
  lightPrimary?: string
  lightSecondary?: string
  colorCardRef?: string
  samplePhotos?: string[]
  copies?: number
  round?: number
  status?: string
  rejectReason?: string
  remark?: string
}

// 打色指示单 VO
export interface ColorInstructionVO {
  id?: number
  instructionNo?: string
  customerId?: number
  clientName?: string
  contactPerson?: string
  productId?: number
  productCode?: string
  productName?: string
  labFactoryId?: number | null
  labFactoryName?: string
  composition?: string
  width?: string
  gsm?: string
  instructionDate?: string
  deliveryDate?: string
  status?: string
  confirmType?: string
  customerFeedback?: string
  confirmedBy?: number
  confirmedAt?: string
  voidReason?: string
  remark?: string
  createTime?: string
  colorSummaries?: ColorSummaryVO[]
  colors?: ColorInstructionColorVO[]
}

// 打色指示单分页查询参数
export interface ColorInstructionPageReqVO {
  pageNo?: number
  pageSize?: number
  instructionNo?: string
  customerId?: number
  productCodeOrName?: string
  customerColorNo?: string
  status?: string
  statusList?: string[]
  keyword?: string
  instructionDateRange?: string[]
}

// 格式化打色指示单下拉选项文本（如：单号 | 客户 | 品名 | [色号 颜色名]）
export function formatColorInstructionOptionLabel(item: ColorInstructionVO): string {
  const parts: string[] = []
  if (item.instructionNo) parts.push(item.instructionNo)
  if (item.clientName) parts.push(item.clientName)
  if (item.productName || item.productCode) {
    parts.push(item.productName || item.productCode || '')
  }
  const colorList =
    item.colorSummaries && item.colorSummaries.length > 0
      ? item.colorSummaries
      : (item.colors || [])
  if (colorList && colorList.length > 0) {
    const summaryStr = colorList
      .map((c) => [c.customerColorNo, c.colorName].filter(Boolean).join(' '))
      .filter(Boolean)
      .join(', ')
    if (summaryStr) {
      parts.push(`[${summaryStr}]`)
    }
  }
  return parts.join(' | ')
}

// 查询打色指示单分页
export const getColorInstructionPage = (params: ColorInstructionPageReqVO) => {
  return request.get({ url: '/lunaerp/production/color-instruction/page', params })
}

// 查询打色指示单详情
export const getColorInstruction = (id: number) => {
  return request.get({ url: `/lunaerp/production/color-instruction/get?id=${id}` })
}

// ========== 后续票据端点占位 ==========

// 预览下一个打色单号（02 票）
export const getNextInstructionNo = () => {
  return request.get({ url: '/lunaerp/production/color-instruction/get-next-no' })
}

// 新建打色指示单（02 票）
export const createColorInstruction = (data: Partial<ColorInstructionVO>) => {
  return request.post({ url: '/lunaerp/production/color-instruction/create', data })
}

// 差异保存更新打色指示单（03 票）
export const updateColorInstruction = (data: Partial<ColorInstructionVO>) => {
  return request.put({ url: '/lunaerp/production/color-instruction/update', data })
}

// 软删打色指示单（02 票）
export const deleteColorInstruction = (id: number) => {
  return request.delete({ url: `/lunaerp/production/color-instruction/delete?id=${id}` })
}

// 开始打色 PENDING -> IN_PROGRESS（04 票）
export const startColorInstruction = (data: { id: number }) => {
  return request.put({ url: '/lunaerp/production/color-instruction/start', data })
}

// 打色指示单确认类型常量（内部枚举，不入字典）
export const COLOR_INSTRUCTION_CONFIRM_TYPES = [
  { value: 'CUSTOMER', label: '客户确认' },
  { value: 'INTERNAL', label: '内部确认' }
] as const

export type ColorInstructionConfirmType = (typeof COLOR_INSTRUCTION_CONFIRM_TYPES)[number]['value']

// 确认打色指示单 IN_PROGRESS -> CONFIRMED（05 票）
export const confirmColorInstruction = (data: {
  id: number
  confirmType: ColorInstructionConfirmType
  customerFeedback?: string
}) => {
  return request.put({ url: '/lunaerp/production/color-instruction/confirm', data })
}

// 回退打色单 CONFIRMED -> IN_PROGRESS（08 票）
export const rollbackColorInstruction = (data: { id: number }) => {
  return request.put({ url: '/lunaerp/production/color-instruction/rollback', data })
}

// 作废打色单（08 票）
export const voidColorInstruction = (data: { id: number; voidReason: string }) => {
  return request.put({ url: '/lunaerp/production/color-instruction/void', data })
}

// 颜色行通过（04 票）
export const approveColorLine = (data: { id: number }) => {
  return request.put({ url: '/lunaerp/production/color-instruction/color/approve', data })
}

// 颜色行驳回重打（04 票）
export const rejectRestrikeColorLine = (data: { id: number; rejectReason: string }) => {
  return request.put({ url: '/lunaerp/production/color-instruction/color/reject-restrike', data })
}

// 打印数据（10 票）
export const getColorInstructionPrintData = (id: number) => {
  return request.get({ url: `/lunaerp/production/color-instruction/${id}/print-data` })
}

// 打色指示单可操作状态列表（非终态，可进入编辑页进行编辑、回退或作废操作）
export const COLOR_INSTRUCTION_OPERABLE_STATUSES = ['PENDING', 'IN_PROGRESS', 'CONFIRMED'] as const

// 是否处于可操作状态（PENDING / IN_PROGRESS / CONFIRMED）
export const isColorInstructionOperable = (status?: string): boolean => {
  return !!status && (COLOR_INSTRUCTION_OPERABLE_STATUSES as readonly string[]).includes(status)
}

// 是否处于终态（IN_PRODUCTION / VOID）
export const isColorInstructionTerminal = (status?: string): boolean => {
  return status === 'IN_PRODUCTION' || status === 'VOID'
}
