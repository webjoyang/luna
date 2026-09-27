import request from '@/config/axios'

/** 质检确认记录 VO */
export interface InspectionConfirmationVO {
  id: number
  chainKey: string
  source: string // INSPECTION / MANUAL
  status: string // PENDING / CONFIRMED / REVOKED
  inspectionOrderId?: number
  inspectionOrderCode?: string
  inspectionOrderItemId?: number
  rollNo: string
  vatNo?: string
  grade: string
  machineNo?: string
  productCode?: string
  productName?: string
  colorName?: string
  colorNo?: string
  styleNo?: string
  measuredWeight?: number
  lengthM?: number
  responsibility?: string // EQUIPMENT / OPERATOR / WAIVED
  workerUserId?: number
  workerName?: string
  confirmedByUserId?: number
  confirmedByName?: string
  confirmedAt?: string
  note?: string
  revokedByUserId?: number
  revokedByName?: string
  revokedAt?: string
  creator?: string
  createTime?: Date | string
  updater?: string
  updateTime?: Date | string
  chainRecords?: InspectionConfirmationVO[]
}

/** 质检确认分页查询参数 */
export interface InspectionConfirmationPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
  source?: string
  responsibility?: string
  workerUserId?: number
  inspectionOrderId?: number
  createTime?: string[]
}

/** 挡车工候选 VO */
export interface WorkerOptionVO {
  id: number
  nickname: string
}

/** 查询质检确认分页 */
export const getInspectionConfirmationPage = (params: InspectionConfirmationPageParam) => {
  return request.get({ url: '/lunaerp/production/inspection-confirmation/page', params })
}

/** 查询挡车工候选列表 */
export const getWorkerOptions = () => {
  return request.get<WorkerOptionVO[]>({
    url: '/lunaerp/production/inspection-confirmation/worker-options'
  })
}

/** 手工登记 Request VO */
export interface InspectionConfirmationManualCreateVO {
  rollNo: string
  grade: string
  responsibility: string
  workerUserId?: number
  vatNo?: string
  machineNo?: string
  productCode?: string
  productName?: string
  colorName?: string
  colorNo?: string
  styleNo?: string
  measuredWeight?: number
  lengthM?: number
  inspectionOrderId?: number
  confirmedAt?: string
  note?: string
}

/** 手工登记质检确认 */
export const manualCreateInspectionConfirmation = (data: InspectionConfirmationManualCreateVO) => {
  return request.post<number>({
    url: '/lunaerp/production/inspection-confirmation/manual-create',
    data
  })
}

/** 从检验单生成质检确认 Request VO */
export interface InspectionConfirmationGenerateReqVO {
  inspectionOrderId: number
}

/** 从检验单生成质检确认 Response VO */
export interface InspectionConfirmationGenerateRespVO {
  generatedCount: number
  skippedCount: number
}

/** 从检验单生成质检确认 */
export const generateFromOrder = (data: InspectionConfirmationGenerateReqVO) => {
  return request.post<InspectionConfirmationGenerateRespVO>({
    url: '/lunaerp/production/inspection-confirmation/generate-from-order',
    data
  })
}

/** 责任认定 Request VO */
export interface InspectionConfirmationConfirmReqVO {
  id: number
  responsibility: string // EQUIPMENT / OPERATOR / WAIVED
  workerUserId?: number
  note?: string
}

/** 责任认定 */
export const confirmInspectionConfirmation = (data: InspectionConfirmationConfirmReqVO) => {
  return request.post<number>({
    url: '/lunaerp/production/inspection-confirmation/confirm',
    data
  })
}

/** 撤销重录 Request VO */
export interface InspectionConfirmationRevokeReqVO {
  id: number
}

/** 撤销重录 Response VO */
export interface InspectionConfirmationRevokeRespVO {
  revokedId: number
  freshId: number
}

/** 撤销重录 */
export const revokeInspectionConfirmation = (data: InspectionConfirmationRevokeReqVO) => {
  return request.post<InspectionConfirmationRevokeRespVO>({
    url: '/lunaerp/production/inspection-confirmation/revoke',
    data
  })
}

/** 获得质检确认详情（含认定链） */
export const getInspectionConfirmation = (id: number) => {
  return request.get<InspectionConfirmationVO>({
    url: '/lunaerp/production/inspection-confirmation/get',
    params: { id }
  })
}

