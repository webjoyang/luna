import request from '@/config/axios'

// 胚布单 VO（颜色投胚分配行随 04 票接入；投胚需求等服务端重算字段以 04 票为准）
export interface GreigeOrderVO {
  id?: number
  orderNo?: string
  salesContractId?: number
  contractNo?: string
  greigeFactoryId: number // 承织坯布厂（供应商 id），必填
  greigeFactoryName?: string // 坯布厂公司名快照，保存供应商档案时复制
  dyeingFactoryId?: number // 指定染厂（供应商 id），可空
  dyeingFactoryName?: string // 染厂公司名快照
  orderDate?: string // 单据日期，缺省今天
  deliveryDate?: string
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
  createTime?: Date | string
}

// 胚布单分页查询参数
export interface GreigeOrderPageParam {
  pageNo?: number
  pageSize?: number
  keyword?: string
  status?: string
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
