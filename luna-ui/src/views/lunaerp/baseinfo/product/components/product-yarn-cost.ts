/**
 * 纱线成本计算纯函数（从旧系统 product-yarn-cost.ts 迁移，见 ADR-0035）
 *
 * 不依赖 Vue 响应式，供 ProductYarnCostSection 组件与单元测试使用。
 * 损耗口径统一为投入损耗：采购投入 = 净耗用 ÷ (1 - 损耗率)。
 * 金额精度 4 位小数（Math.round(value * 10000) / 10000）。
 */

/** 织物结构：KNIT 针织 / WOVEN 梭织 */
export type FabricConstruction = 'KNIT' | 'WOVEN'
/** 梭织估算方式：SIMPLE 简易估算 / PROFESSIONAL 经纬精算 */
export type WovenCalculationMode = 'SIMPLE' | 'PROFESSIONAL'
/** 纱线方向：GENERAL 通用 / WARP 经纱 / WEFT 纬纱（经纬精算模式用） */
export type YarnDirection = 'GENERAL' | 'WARP' | 'WEFT'

/** 纱线规格行（yarnSpecs JSON 行的成本计算子集，字段为超集兼容针织/梭织各模式） */
export interface CostableYarnSpec {
  yarnSpecId?: number
  yarnSpecName?: string
  yarnSpec?: string
  price?: number | string
  ratio?: number | string
  direction?: YarnDirection
  density?: number | string
  crimpRate?: number | string
  wasteRate?: number | string
  consumptionKgPerMeter?: number | string
}

/** 成本设置（对应产品表独立列：织物结构/估算方式/坯布克重门幅/三项加工费） */
export interface ProductYarnCostSettings {
  fabricConstruction: FabricConstruction
  wovenCalculationMode: WovenCalculationMode
  /** 坯布克重（GSM） */
  greigeWeightGsm: number
  /** 坯布门幅（CM） */
  greigeWidthCm: number
  /** 针织加工费（元/KG） */
  knitProcessingFeePerKg: number
  /** 织造费（元/M） */
  weavingFeePerMeter: number
  /** 其他加工费（元/M） */
  otherProcessingFeePerMeter: number
}

/** 成本汇总结果 */
export interface ProductYarnCostResult {
  /** 配比合计（%） */
  ratioTotal: number
  /** 未设置价格的纱线数量 */
  missingPriceCount: number
  /** 纱线成本（元/KG） */
  yarnCostPerKg: number
  /** 纱线成本（元/M，仅梭织） */
  yarnCostPerMeter: number
  /** 加工费（针织：元/KG；梭织：元/M） */
  processingCost: number
  /** 总成本（纱线成本 + 加工费，口径随 basis） */
  totalCost: number
  /** 每米重量（KG/M，仅梭织） */
  kgPerMeter: number
  /** 每公斤米长（M/KG，仅梭织） */
  metersPerKg: number
  /** 成本口径：KG 针织按公斤 / METER 梭织按米 */
  basis: 'KG' | 'METER'
}

/** 数值兜底解析：非有限数值一律按 0 处理（JSON 行可能存字符串） */
const n = (value: unknown): number => {
  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

/** 金额精度：4 位小数 */
const money = (value: number): number => Math.round(value * 10000) / 10000

/**
 * 投入损耗放大：净耗用 ÷ (1 - 损耗率)。
 * 损耗率负值钳制为 0，超 100% 属无效输入，计算时钳制为 99.9%，保存前由 UI 阻止。
 */
export function applyInputWaste(netCost: number, wasteRate: unknown): number {
  const rate = Math.min(99.9, Math.max(0, n(wasteRate))) / 100
  return rate > 0 ? netCost / (1 - rate) : netCost
}

/**
 * 计算产品纱线成本汇总。
 *
 * - 针织：basis=KG，纱线成本/KG = Σ(单价 × 比例% / (1 - 损耗率))，总成本 = 纱线成本 + 针织加工费
 * - 梭织简易估算：basis=METER，每米重量 = 克重 × 门幅 / 100000，
 *   纱线成本/M = 每米重量 × 混纺单价(Σ 单价 × 比例% / (1 - 损耗率))
 * - 梭织经纬精算：basis=METER，纱线成本/M = Σ(每米用量 × 单价 / (1 - 损耗率))，按经纱/纬纱行独立累加
 * - 梭织总成本 = 纱线成本/M + 织造费 + 其他加工费
 */
export function calculateProductYarnCost(
  specs: CostableYarnSpec[],
  settings: ProductYarnCostSettings
): ProductYarnCostResult {
  // 未填写纱线规格的空行不参与计算
  const valid = specs.filter((spec) => String(spec.yarnSpec || '').trim())
  const ratioTotal = valid.reduce((sum, spec) => sum + Math.max(0, n(spec.ratio)), 0)
  const missingPriceCount = valid.filter((spec) => n(spec.price) <= 0).length

  if (settings.fabricConstruction === 'KNIT') {
    const yarnCostPerKg = valid.reduce((sum, spec) => {
      const contribution = (n(spec.price) * Math.max(0, n(spec.ratio))) / 100
      return sum + applyInputWaste(contribution, spec.wasteRate)
    }, 0)
    const processingCost = Math.max(0, n(settings.knitProcessingFeePerKg))
    return {
      ratioTotal: money(ratioTotal),
      missingPriceCount,
      yarnCostPerKg: money(yarnCostPerKg),
      yarnCostPerMeter: 0,
      processingCost: money(processingCost),
      totalCost: money(yarnCostPerKg + processingCost),
      kgPerMeter: 0,
      metersPerKg: 0,
      basis: 'KG'
    }
  }

  // 梭织：每米重量（KG/M）= 克重(GSM) × 门幅(CM) / 100000
  const kgPerMeter =
    (Math.max(0, n(settings.greigeWeightGsm)) * Math.max(0, n(settings.greigeWidthCm))) / 100000
  let yarnCostPerMeter = 0
  if (settings.wovenCalculationMode === 'PROFESSIONAL') {
    // 经纬精算：按每米实际耗用计算，不使用配比
    yarnCostPerMeter = valid.reduce((sum, spec) => {
      const netKgPerMeter = Math.max(0, n(spec.consumptionKgPerMeter))
      return sum + applyInputWaste(netKgPerMeter * n(spec.price), spec.wasteRate)
    }, 0)
  } else {
    // 简易估算：混纺单价（元/KG）× 每米重量
    const blendedPricePerKg = valid.reduce(
      (sum, spec) =>
        sum + applyInputWaste((n(spec.price) * Math.max(0, n(spec.ratio))) / 100, spec.wasteRate),
      0
    )
    yarnCostPerMeter = kgPerMeter * blendedPricePerKg
  }
  const processingCost =
    Math.max(0, n(settings.weavingFeePerMeter)) + Math.max(0, n(settings.otherProcessingFeePerMeter))
  const totalCost = yarnCostPerMeter + processingCost
  return {
    ratioTotal: money(ratioTotal),
    missingPriceCount,
    yarnCostPerKg: kgPerMeter > 0 ? money(yarnCostPerMeter / kgPerMeter) : 0,
    yarnCostPerMeter: money(yarnCostPerMeter),
    processingCost: money(processingCost),
    totalCost: money(totalCost),
    kgPerMeter: money(kgPerMeter),
    metersPerKg: kgPerMeter > 0 ? money(1 / kgPerMeter) : 0,
    basis: 'METER'
  }
}
