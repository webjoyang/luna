import request from '@/config/axios'

// 发染记录 VO
export interface GreigeDispatchRecordVO {
  id?: number
  greigeOrderId?: number
  dyeingOrderId?: number
  dyeingFactoryId?: number
  dyeingFactoryName?: string
  warehouseId?: number
  warehouseName?: string
  productId?: number
  productCode?: string
  productName?: string
  dispatchDate?: string
  quantity?: number
  pieces?: number
  stockLedgerId?: number
  remark?: string
  createTime?: string
}

// 发染记录创建参数
export interface GreigeDispatchRecordCreateVO {
  greigeOrderId: number
  dyeingOrderId?: number
  dyeingFactoryId?: number
  dyeingFactoryName?: string
  warehouseId: number
  dispatchDate: string
  quantity: number
  pieces?: number
  remark?: string
}

// 发染记录更新参数（受限编辑：仅染色单/染厂/备注）
export interface GreigeDispatchRecordUpdateVO {
  id: number
  dyeingOrderId?: number
  dyeingFactoryId?: number
  dyeingFactoryName?: string
  remark?: string
}

// 创建发染记录
export const createGreigeDispatchRecord = (data: GreigeDispatchRecordCreateVO) => {
  return request.post<number>({ url: '/lunaerp/production/greige-dispatch-record/create', data })
}

// 更新发染记录（受限编辑）
export const updateGreigeDispatchRecord = (data: GreigeDispatchRecordUpdateVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/greige-dispatch-record/update', data })
}

// 删除发染记录
export const deleteGreigeDispatchRecord = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/greige-dispatch-record/${id}` })
}

// 按胚布单ID获取发染记录列表
export const getGreigeDispatchRecordListByGreigeOrder = (greigeOrderId: number) => {
  return request.get<GreigeDispatchRecordVO[]>({
    url: `/lunaerp/production/greige-dispatch-record/list-by-greige-order?greigeOrderId=${greigeOrderId}`
  })
}

// 按染色单ID获取发染记录列表
export const getGreigeDispatchRecordListByDyeingOrder = (dyeingOrderId: number) => {
  return request.get<GreigeDispatchRecordVO[]>({
    url: `/lunaerp/production/greige-dispatch-record/list-by-dyeing-order?dyeingOrderId=${dyeingOrderId}`
  })
}
