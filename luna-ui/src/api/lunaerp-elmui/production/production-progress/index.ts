import request from '@/config/axios'
import type { GreigeDispatchRecordVO } from '@/api/lunaerp-elmui/production/greige-dispatch-record'

/**
 * 生产进度（只读聚合视图，ADR-0034）
 *
 * 进度不是表也不是单据，是合同下各生产单据与库存流水的实时投影：
 * page 给跨合同列表，get 给单合同全链路。所有 KG 量只聚合 KG 单位、排除 CANCELLED。
 */

// 分页结果（后端 PageResult 结构）
export interface ProductionProgressPageResult<T> {
  list: T[]
  total: number
}

/**
 * 临期天数阈值，镜像后端 ProductionProgressQueryServiceImpl.NEAR_DELIVERY_DAYS（设计文档 4.4）。
 * 前端只用它做交期着色，预警判定本身在后端；两处必须同值，否则颜色与「临期」标签会互相打脸。
 */
export const NEAR_DELIVERY_DAYS = 7

// 跨合同列表项 VO（合同头 + 各环节关键量 + 预警）
export interface ProductionProgressPageItemVO {
  contractId: number
  contractNo: string
  customerName?: string
  salespersonName?: string
  /** 交货日期，可空（无交期排最后） */
  deliveryDate?: string
  /** 合同状态：ACTIVE/COMPLETED/CANCELLED */
  status?: string
  /** 合同单位：KG/M（M 单位合同 KG 合计为 0，明细原样展示不折算） */
  unit?: string
  demandKg?: number
  greigePlannedKg?: number
  dyeingPlannedKg?: number
  sentToDyeingKg?: number
  /** 排产覆盖率百分比（0-100，2 位小数） */
  vatCoverage?: number
  greigeInKg?: number
  finishedInKg?: number
  shippedKg?: number
  unshippedKg?: number
  shipmentCount?: number
  alertCount: number
  /** 预警标签：临期/超期/延期缸 */
  alerts?: string[]
}

// 胚布环节
export interface ProductionProgressGreigeSection {
  demandKg?: number
  plannedKg?: number
  /** 胚布入库量（与 stock.greigeInKg 同源） */
  stockedInKg?: number
  sentToDyeingKg?: number
  /** 是否存在自织（SELF）胚布单 */
  selfWeaving?: boolean
  weavingPlannedPieces?: number
  weavingCompletedPieces?: number
}

// 染色环节（数据来自进度反馈覆盖层，ADR-0031）
export interface ProductionProgressDyeingSection {
  demandKg?: number
  plannedKg?: number
  vatCoverage?: number
  /** 缸状态汇总（进度反馈状态 → 缸数） */
  vatStatusSummary?: Record<string, number>
  delayedVatCount?: number
}

// 检验环节
export interface ProductionProgressInspectionSection {
  totalItems?: number
  /** 等级汇总（A/B/C/DEFECTIVE → 条数） */
  gradeSummary?: Record<string, number>
}

// 入库环节
export interface ProductionProgressStockSection {
  /** 胚布入库量（入库单 greigeOrderId 非空） */
  greigeInKg?: number
  /** 成品入库量（入库单 dyeingOrderId 非空） */
  finishedInKg?: number
}

// 出货环节
export interface ProductionProgressShipmentSection {
  shippedKg?: number
  unshippedKg?: number
  shipmentCount?: number
}

// 单合同全链路 VO
export interface ProductionProgressDetailVO {
  contractId: number
  contractNo: string
  customerName?: string
  salespersonName?: string
  deliveryDate?: string
  status?: string
  unit?: string
  greige?: ProductionProgressGreigeSection
  dyeing?: ProductionProgressDyeingSection
  inspection?: ProductionProgressInspectionSection
  stock?: ProductionProgressStockSection
  shipment?: ProductionProgressShipmentSection
  alerts?: string[]
  /** 发染记录下钻明细（复用发染记录 VO，ADR-0033） */
  greigeDispatchRecords?: GreigeDispatchRecordVO[]
}

// 分页与筛选查询参数
export interface ProductionProgressPageParam {
  pageNo?: number
  pageSize?: number
  /** 关键字（合同号/客户名模糊） */
  keyword?: string
  deliveryDateStart?: string
  deliveryDateEnd?: string
  /** 合同状态，为空时默认排除 CANCELLED */
  status?: string
  /** 仅看有预警的合同 */
  alertOnly?: boolean
  /** 是否包含已取消合同（无 status 时生效） */
  includeCancelled?: boolean
}

// 查询生产进度分页（跨合同列表，后端按交期升序、无交期排最后）
export const getProductionProgressPage = (params: ProductionProgressPageParam) => {
  return request.get<ProductionProgressPageResult<ProductionProgressPageItemVO>>({
    url: '/lunaerp/production/production-progress/page',
    params
  })
}

// 查询单合同全链路生产进度（六环节 + 发染记录/缸状态汇总下钻）
export const getProductionProgress = (id: number) => {
  return request.get<ProductionProgressDetailVO>({
    url: '/lunaerp/production/production-progress/get',
    params: { id }
  })
}
