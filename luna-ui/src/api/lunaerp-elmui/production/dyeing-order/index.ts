import request from '@/config/axios'

// 染色颜色明细行 VO（随染色单整体保存）
export interface DyeingColorVO {
  id?: number
  dyeingOrderId?: number
  lineNo?: number
  customerColorNo?: string // 客户色号（从合同明细带出可改）
  colorName?: string // 颜色名称
  quantity?: number // 本单染色数量（成品口径）
  pieces?: number // 匹数
  dyeingInputQty?: number // 投染数（含损耗的实际投入量）
  dyeingInputUnit?: string // 投染数单位（字典 lunaerp_sales_contract_unit：KG/M）
  contractQty?: number // 合同数量（从合同明细带出，只读对比）
}

// 染色单 VO
export interface DyeingOrderVO {
  id?: number
  salesContractId?: number
  contractNo?: string
  customerId?: number
  clientName?: string
  dyeingFactoryId: number // 承染方（供应商 id），必填
  dyeingFactoryName?: string // 承染方公司名快照
  orderDate?: string // 单据日期，缺省今天
  deliveryDate?: string
  productCode?: string
  productName?: string
  composition?: string
  width?: string
  gsm?: string
  shrinkage?: string
  loss?: string
  ph?: string
  fastnessWash?: string
  fastnessSoap?: string
  fastnessDryRub?: string
  fastnessWetRub?: string
  fastnessLight?: string
  fastnessPerspiration?: string
  fastnessDryClean?: string
  fastnessSixFiber?: string
  otherReq?: string
  remark?: string
  status?: string // DRAFT / ACTIVE / COMPLETED / CANCELLED
  colors?: DyeingColorVO[]
  creator?: string
  createTime?: Date | string
}

// 染色单分页查询参数
export interface DyeingOrderPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
  customerId?: number
  dyeingFactoryId?: number
  orderDateStart?: string
  orderDateEnd?: string
  deliveryDateStart?: string
  deliveryDateEnd?: string
}

// 查询染色单分页
export const getDyeingOrderPage = (params: DyeingOrderPageParam) => {
  return request.get({ url: '/lunaerp/production/dyeing-order/page', params })
}

// 查询染色单详情
export const getDyeingOrder = (id: number) => {
  return request.get<DyeingOrderVO>({ url: `/lunaerp/production/dyeing-order/get?id=${id}` })
}

// 删除染色单
export const deleteDyeingOrder = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/dyeing-order/delete?id=${id}` })
}
