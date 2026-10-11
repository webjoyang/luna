import request from '@/config/axios'

/**
 * 颜色摘要 VO（色号 chips + 轮次数，防 N+1 预聚合）
 */
export interface ColorInstructionSummaryVO {
  customerColorNo: string
  colorName?: string
  colorHex?: string
  round: number
  status: string
}

/**
 * 打色指示单颜色行 VO
 */
export interface ColorInstructionColorVO {
  id?: number
  _tempId?: string
  instructionId?: number
  lineNo?: number
  customerColorNo: string
  colorName: string
  factoryColorNo?: string
  colorHex?: string
  lightPrimary: string
  lightSecondary?: string
  colorCardRef?: string
  samplePhotos?: string
  copies?: number
  round: number
  status: string
  rejectReason?: string
  remark?: string
}

/**
 * 打色指示单主档 VO
 */
export interface ColorInstructionVO {
  id?: number
  instructionNo?: string
  customerId: number
  clientName: string
  contactPerson?: string
  productId: number
  productCode: string
  productName: string
  labFactoryId?: number | null
  labFactoryName?: string
  composition?: string
  width?: string
  gsm?: string
  instructionDate: string
  deliveryDate?: string
  status: string
  confirmType?: string
  customerFeedback?: string
  confirmedBy?: number
  confirmedAt?: string
  voidReason?: string
  remark?: string
  createTime?: string
  colorSummary?: ColorInstructionSummaryVO[]
  colors?: ColorInstructionColorVO[]
}

/**
 * 打色指示单分页查询请求 VO
 */
export interface ColorInstructionPageReqVO {
  pageNo: number
  pageSize: number
  instructionNo?: string
  customerId?: number
  productCodeOrName?: string
  customerColorNo?: string
  status?: string
  instructionDateRange?: string[]
}

/**
 * 查询打色指示单分页
 */
export const getColorInstructionPage = (params: ColorInstructionPageReqVO) => {
  return request.get({ url: '/lunaerp/production/color-instruction/page', params })
}

/**
 * 查询打色指示单详情
 */
export const getColorInstruction = (id: number) => {
  return request.get({ url: '/lunaerp/production/color-instruction/get', params: { id } })
}

/**
 * 预览下一个打色单号
 */
export const getNextNo = () => {
  return request.get({ url: '/lunaerp/production/color-instruction/get-next-no' })
}

/**
 * 创建打色指示单（预留骨架，后续票据落地）
 */
export const createColorInstruction = (data: Partial<ColorInstructionVO>) => {
  return request.post({ url: '/lunaerp/production/color-instruction/create', data })
}

/**
 * 更新打色指示单（预留骨架，后续票据落地）
 */
export const updateColorInstruction = (data: Partial<ColorInstructionVO>) => {
  return request.put({ url: '/lunaerp/production/color-instruction/update', data })
}

/**
 * 删除打色指示单（预留骨架，后续票据落地）
 */
export const deleteColorInstruction = (id: number) => {
  return request.delete({ url: '/lunaerp/production/color-instruction/delete', params: { id } })
}

/**
 * 开始打色（PENDING -> IN_PROGRESS）
 */
export const startColorInstruction = (id: number) => {
  return request.put({ url: '/lunaerp/production/color-instruction/start', data: { id } })
}

/**
 * 通过打色颜色行（PENDING -> APPROVED）
 */
export const approveColorLine = (id: number) => {
  return request.put({ url: '/lunaerp/production/color-instruction/color/approve', data: { id } })
}

/**
 * 驳回重打打色颜色行（PENDING/APPROVED -> REJECTED 并克隆 round+1 新行）
 */
export const rejectRestrikeColorLine = (data: { id: number; rejectReason?: string }) => {
  return request.put({ url: '/lunaerp/production/color-instruction/color/reject-restrike', data })
}

/**
 * 打色指示单套打颜色行 VO
 */
export interface ColorInstructionPrintColorVO {
  id?: number
  instructionId?: number
  lineNo?: number
  customerColorNo: string
  colorName: string
  factoryColorNo?: string
  colorHex?: string
  lightPrimary: string
  lightSecondary?: string
  colorCardRef?: string
  samplePhotos?: string
  copies?: number
  round: number
  status: string
  rejectReason?: string
  remark?: string
}

/**
 * 打色指示单套打 VO（独立 VO，无金额字段，豁免脱敏）
 */
export interface ColorInstructionPrintVO {
  id: number
  instructionNo: string
  customerId: number
  clientName: string
  contactPerson?: string
  productId: number
  productCode: string
  productName: string
  labFactoryId?: number | null
  labFactoryName?: string
  composition?: string
  width?: string
  gsm?: string
  instructionDate: string
  deliveryDate?: string
  status: string
  confirmType?: string
  customerFeedback?: string
  confirmedBy?: number
  confirmedAt?: string
  voidReason?: string
  remark?: string
  creator?: string
  createTime?: string
  printTime?: string
  colors?: ColorInstructionPrintColorVO[]
}

/**
 * 获取打色指示单套打数据（独立 VO，无金额字段，豁免脱敏）
 */
export const getPrintData = (id: number): Promise<ColorInstructionPrintVO> => {
  return request.get({ url: `/lunaerp/production/color-instruction/${id}/print-data` })
}


