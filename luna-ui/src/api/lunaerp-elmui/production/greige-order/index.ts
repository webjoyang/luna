import request from '@/config/axios'

// 颜色投胚分配行 VO（随单聚合保存；demandQty 投胚需求由服务端权威重算，前端传值被覆盖）
export interface GreigeOrderColorVO {
  id?: number
  orderId?: number
  lineNo?: number
  color?: string
  finishedQty?: number // 成品下单量 KG，null 按 0
  lossRate?: number // 损耗率，小数（0.05 = 5%），为空时后端取主单预设损耗、再缺省 0
  demandQty?: number // 投胚需求 KG（服务端按 成品量÷(1−损耗率) 重算）
  remark?: string
}

// 胚布单自织进度 VO（自织单详情实时聚合展示，不落库）
export interface GreigeOrderWeavingProgressVO {
  plannedPieces?: number // 计划总匹数
  completedPieces?: number // 已完工总匹数
}

// 胚布单 VO（颜色投胚分配行随单整体保存；计划投胚量与颜色投胚需求合计不强制相等）
export interface GreigeOrderVO {
  id?: number
  orderNo?: string
  salesContractId?: number
  contractNo?: string
  colorInstructionId?: number
  colorInstructionNo?: string
  greigeFactoryId?: number | null // 承织坯布厂（供应商 id），外发必填，自织可空
  greigeFactoryName?: string // 坯布厂公司名快照，保存供应商档案时复制
  dyeingFactoryId?: number // 指定染厂（供应商 id），可空
  dyeingFactoryName?: string // 染厂公司名快照
  orderDate?: string // 单据日期，缺省今天
  deliveryDate?: string
  productId?: number | null // 产品档案ID（强关联，用于纱线测算查配比BOM，可空）
  productCode?: string
  productName?: string
  composition?: string
  width?: string
  gsm?: string
  shrinkage?: string
  pieceWeight?: number // 匹重 KG，缺省 25
  pieceCount?: number // 匹数，前端按 ceil(投胚量÷匹重) 自动算，可手改
  plannedQty?: number // 计划投胚量 KG
  finishedQty?: number // 下单成品数量 KG
  suggestedLoss?: number // 预设损耗，小数（0.05 = 5%）
  processingFee?: number
  greigeTypeId?: string // 所属胚布品种（档案 id 为字符串），手选
  productionRemark?: string
  remark?: string
  status?: string
  productionMode?: string // 生产模式：SELF 自织 / OUTSOURCE 外发（缺省 OUTSOURCE）
  weavingProgress?: GreigeOrderWeavingProgressVO // 织造进度（自织单聚合展示）
  weavingPlannedPieces?: number // 织造计划总匹数
  weavingCompletedPieces?: number // 织造已完工总匹数
  colors?: GreigeOrderColorVO[] // 颜色投胚分配行（详情嵌套返回，保存时整体替换）
  createTime?: Date | string
}

// 胚布单保存请求 VO（同 GreigeOrderVO，productId 可空）
export type GreigeOrderSaveReqVO = GreigeOrderVO

// 胚布单分页查询参数
export interface GreigeOrderPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
  productionMode?: string // 生产模式：SELF 自织 / OUTSOURCE 外发
}

// 查询胚布单分页
export const getGreigeOrderPage = (params: GreigeOrderPageParam) => {
  return request.get({ url: '/lunaerp/production/greige-order/page', params })
}

// 预览当天下一个胚布单号（只读不占号，多次调用在无新建时返回相同值）
export const getNextGreigeOrderNo = () => {
  return request.get<string>({ url: '/lunaerp/production/greige-order/get-next-no' })
}

// 新增胚布单（orderNo 留空时后端自动生成）
export const createGreigeOrder = (data: GreigeOrderVO) => {
  return request.post<number>({ url: '/lunaerp/production/greige-order/create', data })
}

// 修改胚布单（整体保存；orderNo 留空时后端自动生成，编号/引用校验规则与新建一致）
export const updateGreigeOrder = (data: GreigeOrderVO) => {
  return request.put<boolean>({ url: '/lunaerp/production/greige-order/update', data })
}

// 查询胚布单详情（主表全部字段 + 颜色投胚分配行按 line_no 升序嵌套返回）
export const getGreigeOrder = (id: number) => {
  return request.get<GreigeOrderVO>({ url: `/lunaerp/production/greige-order/get?id=${id}` })
}

// 删除胚布单（主表与颜色行级联逻辑删除；软删编号不释放，见 ADR-0006）
export const deleteGreigeOrder = (id: number) => {
  return request.delete<boolean>({ url: `/lunaerp/production/greige-order/delete?id=${id}` })
}
