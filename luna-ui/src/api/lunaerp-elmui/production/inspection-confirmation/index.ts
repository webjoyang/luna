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
  return request.get<WorkerOptionVO[]>({ url: '/lunaerp/production/inspection-confirmation/worker-options' })
}
