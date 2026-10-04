import request from '@/config/axios'

// 样品分析 VO
export interface SampleAnalysisVO {
  id?: number
  sampleAnalysisNo?: string
  customerId?: number
  clientName?: string
  clientContact?: string
  clientStyleNo?: string
  productName: string
  composition?: string
  width?: number
  weight?: number
  meterLength?: number
  process?: string
  dyeingProcess?: string
  machineType?: string
  output?: string
  flowerNo?: string
  whiteWidth?: string
  whiteWeight?: string
  flowerWidth?: string
  flowerHeight?: string
  gear?: string
  clothRack?: string
  startSpeed?: string
  feederDisk?: string
  theoreticalOutput?: string
  stitchLength?: string
  photos?: string[]
  firstPhotoUrl?: string
  stitchArrange?: any
  productionCost?: number
  dyeingCost?: number
  otherCost?: number
  lossRate?: number
  totalCost?: number
  quotedPrice?: number
  quotedRemark?: string
  isPaid?: boolean
  unitPrice?: number
  creatorId?: number
  creatorName?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

// 样品分析保存 Req VO
export interface SampleAnalysisSaveReqVO {
  id?: number
  sampleAnalysisNo?: string
  customerId?: number
  clientName?: string
  clientContact?: string
  clientStyleNo?: string
  productName: string
  composition?: string
  width?: number
  weight?: number
  process?: string
  dyeingProcess?: string
  machineType?: string
  output?: string
  flowerNo?: string
  whiteWidth?: string
  whiteWeight?: string
  flowerWidth?: string
  flowerHeight?: string
  gear?: string
  clothRack?: string
  startSpeed?: string
  feederDisk?: string
  theoreticalOutput?: string
  stitchLength?: string
  photos?: string[]
  stitchArrange?: any
  productionCost?: number
  dyeingCost?: number
  otherCost?: number
  lossRate?: number
  quotedPrice?: number
  quotedRemark?: string
  unitPrice?: number
  remark?: string
}

// 样品分析分页 Req VO
export interface SampleAnalysisPageReqVO extends PageParam {
  keyword?: string
  isPaid?: boolean
}

// 创建样品分析
export const createSampleAnalysis = (data: SampleAnalysisSaveReqVO): Promise<number> => {
  return request.post({ url: '/lunaerp/sample/sample-analysis/create', data })
}

// 修改样品分析
export const updateSampleAnalysis = (data: SampleAnalysisSaveReqVO): Promise<boolean> => {
  return request.put({ url: '/lunaerp/sample/sample-analysis/update', data })
}

// 删除样品分析
export const deleteSampleAnalysis = (id: number): Promise<boolean> => {
  return request.delete({ url: `/lunaerp/sample/sample-analysis/delete?id=${id}` })
}

// 获得样品分析详情
export const getSampleAnalysis = (id: number): Promise<SampleAnalysisVO> => {
  return request.get({ url: `/lunaerp/sample/sample-analysis/get?id=${id}` })
}

// 获得样品分析分页
export const getSampleAnalysisPage = (params: SampleAnalysisPageReqVO): Promise<PageResult<SampleAnalysisVO[]>> => {
  return request.get({ url: '/lunaerp/sample/sample-analysis/page', params })
}

// 预览下一个样品编号
export const getNextSampleAnalysisNo = (): Promise<string> => {
  return request.get({ url: '/lunaerp/sample/sample-analysis/next-no' })
}
