import request from '@/config/axios'

/** 回收站项 VO */
export interface RecycleItemRespVO {
  bizType: string
  bizId: number
  displayName: string
  deletedBy?: string
  deletedTime?: string | Date
}

/** 回收站支持的业务类型字典定义 */
export const RECYCLE_BIZ_TYPES = [
  { label: '客户档案', value: 'CUSTOMER' },
  { label: '供应商档案', value: 'SUPPLIER' },
  { label: '产品档案', value: 'PRODUCT' },
  { label: '纱线材质', value: 'YARN_TYPE' },
  { label: '纱线规格', value: 'YARN_SPEC' },
  { label: '销售合同', value: 'SALES_CONTRACT' },
  { label: '打色指示单', value: 'COLOR_INSTRUCTION' },
  { label: '胚布单', value: 'GREIGE_ORDER' },
  { label: '染色单', value: 'DYEING_ORDER' },
  { label: '纱线采购单', value: 'YARN_PURCHASE_ORDER' },
  { label: '入库单', value: 'STOCK_IN' },
  { label: '织造单', value: 'WEAVING_ORDER' },
  { label: '发货单', value: 'SHIPMENT' }
] as const

/** 回收站分页 Request VO */
export interface RecyclePageReqVO extends PageParam {
  bizType: string
}

/** 回收站恢复 Request VO */
export interface RecycleRestoreReqVO {
  bizType: string
  bizId: number
}

/** 分页查询回收站已软删列表 */
export const getRecyclePage = (params: RecyclePageReqVO) => {
  return request.get({ url: '/lunaerp/common/recycle/page', params })
}

/** 恢复回收站项 */
export const restoreRecycleItem = (data: RecycleRestoreReqVO) => {
  return request.put({ url: '/lunaerp/common/recycle/restore', data })
}

/** 彻底删除回收站项（物理删除） */
export const purgeRecycleItem = (bizType: string, bizId: number) => {
  return request.delete({ url: '/lunaerp/common/recycle/purge', params: { bizType, bizId } })
}
