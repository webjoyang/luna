import request from '@/config/axios'

// 合同明细行预填数据
export interface ReleaseContractLineVO {
  contractLineId: number
  lineNo: number
  productCode: string
  productName: string
  composition?: string
  width?: string
  gsm?: string
  customerColor?: string
  contractQty: number
  releasedQty: number
  remainingQty: number
  defaultLossRate?: number
}

// 已有打色单简要信息
export interface ExistingColorInstructionVO {
  id: number
  instructionNo: string
  status: string
  productCode: string
  productName: string
}

// 预填 Response VO
export interface ReleaseFromContractRespVO {
  contractId: number
  contractNo: string
  clientName: string
  customerId: number
  lines: ReleaseContractLineVO[]
  existingColorInstructions: ExistingColorInstructionVO[]
}

// 新建打色单颜色行
export interface ColorInstructionLineVO {
  contractLineId?: number
  customerColorNo?: string
  colorName?: string
  lightPrimary: string
  lightSecondary?: string
  factoryColorNo?: string
  colorCardRef?: string
}

// 新建打色单配置
export interface NewColorInstructionVO {
  labFactoryId?: number | null
  colors: ColorInstructionLineVO[]
}

// 下达明细行
export interface ReleaseLineVO {
  contractLineId: number
  customerColor?: string
  releaseQty: number
  lossRate?: number
}

export type ColorInstructionMode = 'EXISTING' | 'NEW' | 'NONE'

// 下达产品分组
export interface ReleaseGroupVO {
  productId?: number
  productCode: string
  productName: string
  composition?: string
  width?: string
  gsm?: string
  colorInstructionMode: ColorInstructionMode
  existingColorInstructionId?: number
  newColorInstruction?: NewColorInstructionVO
  lines: ReleaseLineVO[]
}

// 下达生产 Request VO
export interface ReleaseProductionReqVO {
  contractId: number
  greigeFactoryId: number
  dyeingFactoryId: number
  groups: ReleaseGroupVO[]
}

// 胚布单简要信息
export interface GreigeOrderSimpleVO {
  id: number
  orderNo?: string
}

// 染色单简要信息
export interface DyeingOrderSimpleVO {
  id: number
}

// 检验单简要信息
export interface InspectionOrderSimpleVO {
  id: number
  code?: string
  type?: string
}

// 打色指示单简要信息
export interface ColorInstructionSimpleVO {
  id: number
  instructionNo?: string
}

// 下达生产 Response VO
export interface ReleaseProductionRespVO {
  greigeOrders: GreigeOrderSimpleVO[]
  dyeingOrders: DyeingOrderSimpleVO[]
  inspectionOrders: InspectionOrderSimpleVO[]
  colorInstructions: ColorInstructionSimpleVO[]
}

/**
 * 从销售合同带入下达生产预填数据（剩余可下达量与已有打色单）
 */
export const getReleaseFromContract = (contractId: number): Promise<ReleaseFromContractRespVO> => {
  return request.get({
    url: '/lunaerp/production/release-production/from-contract',
    params: { contractId }
  })
}

/**
 * 下达生产（按产品分组创建胚布单、染色单与检验单）
 */
export const releaseProduction = (data: ReleaseProductionReqVO): Promise<ReleaseProductionRespVO> => {
  return request.post({
    url: '/lunaerp/production/release-production/release',
    data
  })
}

/**
 * 从染色单便利生成成品检验单
 */
export const createFinishedInspection = (dyeingOrderId: number): Promise<number> => {
  return request.post({
    url: '/lunaerp/production/release-production/create-finished-inspection',
    params: { dyeingOrderId }
  })
}

