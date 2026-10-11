import request from '@/config/axios'

/**
 * 生产管理 - 下达生产编排 API
 */

// 合同行信息 VO
export interface ReleaseContractLineVO {
  contractLineId: number
  lineNo?: number
  productCode?: string
  productName?: string
  composition?: string
  width?: string
  gsm?: string
  customerColor?: string
  contractQty?: number
  releasedQty?: number
  remainingQty?: number
  defaultLossRate?: number
}

// 已有打色单 VO
export interface ReleaseExistingColorInstructionVO {
  id: number
  instructionNo?: string
  status?: string
  productCode?: string
  productName?: string
}

// 预填响应 VO
export interface ReleaseFromContractRespVO {
  contractId: number
  contractNo?: string
  clientName?: string
  lines: ReleaseContractLineVO[]
  existingColorInstructions: ReleaseExistingColorInstructionVO[]
}

// 新建打色单颜色 VO
export interface ReleaseNewColorVO {
  contractLineId?: number
  customerColorNo?: string
  colorName?: string
  lightPrimary: string
  factoryColorNo?: string
  colorCardRef?: string
}

// 新建打色单 VO
export interface ReleaseNewColorInstructionVO {
  labFactoryId?: number
  colors: ReleaseNewColorVO[]
}

// 分组行 VO
export interface ReleaseGroupLineVO {
  contractLineId: number
  customerColor?: string
  releaseQty: number
  lossRate?: number
}

// 分组 VO
export interface ReleaseGroupVO {
  productCode?: string
  productName?: string
  productId?: number
  composition?: string
  width?: string
  gsm?: string
  colorInstructionMode?: 'EXISTING' | 'NEW' | 'NONE'
  existingColorInstructionId?: number
  newColorInstruction?: ReleaseNewColorInstructionVO
  lines: ReleaseGroupLineVO[]
}

// 下达请求 VO
export interface ReleaseProductionReqVO {
  contractId: number
  greigeFactoryId: number
  dyeingFactoryId: number
  groups: ReleaseGroupVO[]
}

// 简单单据 VO
export interface ReleaseOrderSimpleVO {
  id: number
  orderNo?: string
}

// 简单检验单 VO
export interface ReleaseInspectionSimpleVO {
  id: number
  code?: string
  type?: string
}

// 下达响应 VO
export interface ReleaseProductionRespVO {
  greigeOrders?: ReleaseOrderSimpleVO[]
  dyeingOrders?: ReleaseOrderSimpleVO[]
  inspectionOrders?: ReleaseInspectionSimpleVO[]
  colorInstructions?: ReleaseOrderSimpleVO[]
}

/**
 * 从销售合同预填下达生产信息
 */
export const getReleaseFromContract = (contractId: number) => {
  return request.get<ReleaseFromContractRespVO>({
    url: `/lunaerp/production/release-production/from-contract?contractId=${contractId}`
  })
}

/**
 * 下达生产
 */
export const releaseProduction = (data: ReleaseProductionReqVO) => {
  return request.post<ReleaseProductionRespVO>({
    url: '/lunaerp/production/release-production/release',
    data
  })
}

/**
 * 从染色单生成成品检验单
 */
export const createFinishedInspection = (dyeingOrderId: number) => {
  return request.post<number>({
    url: `/lunaerp/production/release-production/create-finished-inspection?dyeingOrderId=${dyeingOrderId}`
  })
}
