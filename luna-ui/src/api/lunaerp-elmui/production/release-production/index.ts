import request from '@/config/axios'

/** 销售合同下达生产合同行 VO */
export interface ReleaseContractLineVO {
  contractLineId: number
  lineNo?: number
  productId?: number
  productCode?: string
  productName?: string
  composition?: string
  width?: string
  gsm?: string
  customerColor?: string
  contractQty: number
  releasedQty: number
  remainingQty: number
  defaultLossRate?: number
}

/** 销售合同下达生产已有打色单 VO */
export interface ReleaseExistingColorInstructionVO {
  id: number
  instructionNo: string
  status: string
  productCode?: string
  productName?: string
}

/** 销售合同下达生产预填 Response VO */
export interface ReleaseFromContractRespVO {
  contractId: number
  contractNo: string
  clientName: string
  deliveryDate?: string
  status?: string
  lines: ReleaseContractLineVO[]
  existingColorInstructions: ReleaseExistingColorInstructionVO[]
}

/** 新建打色单颜色行 VO */
export interface ReleaseNewColorVO {
  contractLineId?: number
  customerColorNo?: string
  colorName?: string
  lightPrimary: string
  factoryColorNo?: string
  colorCardRef?: string
}

/** 新建打色单 VO */
export interface ReleaseNewColorInstructionVO {
  labFactoryId?: number | null
  colors: ReleaseNewColorVO[]
}

/** 下达生产分组明细行 VO */
export interface ReleaseGroupLineVO {
  contractLineId: number
  customerColor?: string
  releaseQty: number
  lossRate?: number
}

/** 下达生产产品分组 VO */
export interface ReleaseGroupVO {
  productId?: number
  productCode?: string
  productName?: string
  composition?: string
  width?: string
  gsm?: string
  colorInstructionMode: 'EXISTING' | 'NEW' | 'NONE' | string
  existingColorInstructionId?: number | null
  newColorInstruction?: ReleaseNewColorInstructionVO | null
  lines: ReleaseGroupLineVO[]
}

/** 下达生产 Request VO */
export interface ReleaseProductionReqVO {
  contractId: number
  greigeFactoryId: number
  dyeingFactoryId: number
  groups: ReleaseGroupVO[]
}

/** 下达生产生成的单据精简 VO */
export interface ReleaseOrderSimpleVO {
  id: number
  orderNo?: string
}

/** 下达生产生成的检验单精简 VO */
export interface ReleaseInspectionSimpleVO {
  id: number
  code?: string
  type?: string
}

/** 下达生产 Response VO */
export interface ReleaseProductionRespVO {
  greigeOrders: ReleaseOrderSimpleVO[]
  dyeingOrders: ReleaseOrderSimpleVO[]
  inspectionOrders: ReleaseInspectionSimpleVO[]
  colorInstructions: ReleaseOrderSimpleVO[]
}

/** 从销售合同预填下达生产信息 */
export const getReleaseFromContract = (contractId: number) => {
  return request.get<ReleaseFromContractRespVO>({
    url: '/lunaerp/production/release-production/from-contract',
    params: { contractId }
  })
}

/** 下达生产（创建胚布单、染色单、检验单并更新合同行已下达量） */
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
