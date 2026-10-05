/**
 * 样品分析 - 内部成本核算与实时预览纯函数（对标后端 SampleAnalysisServiceImpl 权威计算口径）
 *
 * 核心规则：
 * 1. 纱线成本 = Σ（明细单价 × 比例 ÷ 100），明细单价 >1000 时自动 ÷1000（防吨价误填）
 * 2. 总成本 = (纱线成本 + 生产工费 + 染整费 + 其他费用) ÷ (1 − 损耗率 ÷ 100)，四舍五入保留 2 位小数
 * 3. 其他费用必须计入总成本；费用项留空按 0 计算
 * 4. 损耗率必须 0 ≤ lossRate < 100，填 100、负数或非数字属于非法
 */

export interface CostableSampleYarnSpec {
  yarnSpecId?: number | null
  unitPrice?: number | string | null
  ratio?: number | string | null
}

export interface SampleCostSettings {
  productionCost?: number | string | null
  dyeingCost?: number | string | null
  otherCost?: number | string | null
  lossRate?: number | string | null
}

export interface SampleCostResult {
  /** 用纱比例合计（%） */
  ratioTotal: number
  /** 纱线成本（元/KG，四舍五入 4 位小数供展示或累加） */
  yarnCost: number
  /** 生产工费（元/KG） */
  productionCost: number
  /** 染整费（元/KG） */
  dyeingCost: number
  /** 其他费用（元/KG） */
  otherCost: number
  /** 损耗率（%） */
  lossRate: number
  /** 损耗率是否合法（0 ≤ lossRate < 100） */
  isLossRateValid: boolean
  /** 错误信息（损耗率非法等） */
  errorMessage?: string
  /** 总成本（元/KG，四舍五入 2 位小数；若损耗率非法则为 null） */
  totalCost: number | null
}

/** 数值兜底解析：非有限数值一律按 0 处理 */
export const n = (value: unknown): number => {
  if (value === null || value === undefined || value === '') {
    return 0
  }
  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

/** 保留 2 位小数 */
export const round2 = (value: number): number => Math.round((value + Number.EPSILON) * 100) / 100

/** 保留 4 位小数 */
export const round4 = (value: number): number => Math.round((value + Number.EPSILON) * 10000) / 10000

/**
 * 校验损耗率合法性：0 ≤ lossRate < 100，空值视为 0
 */
export const validateLossRate = (lossRateRaw: unknown): { isValid: boolean; value: number; message?: string } => {
  if (lossRateRaw === null || lossRateRaw === undefined || String(lossRateRaw).trim() === '') {
    return { isValid: true, value: 0 }
  }
  const parsed = typeof lossRateRaw === 'number' ? lossRateRaw : Number(lossRateRaw)
  if (!Number.isFinite(parsed) || parsed < 0 || parsed >= 100) {
    return {
      isValid: false,
      value: parsed,
      message: '损耗率取值非法（必须 ≥ 0 且 < 100）'
    }
  }
  return { isValid: true, value: parsed }
}

/**
 * 计算样品分析成本与用纱比例汇总
 */
export function calculateSampleCost(
  yarnSpecs: CostableSampleYarnSpec[] = [],
  settings: SampleCostSettings = {}
): SampleCostResult {
  // 1. 损耗率校验
  const lossRateCheck = validateLossRate(settings.lossRate)

  // 2. 统计比例合计与纱线成本
  let ratioTotal = 0
  let yarnCost = 0

  for (const spec of yarnSpecs) {
    if (!spec) continue
    const ratio = Math.max(0, n(spec.ratio))
    ratioTotal += ratio

    let price = n(spec.unitPrice)
    if (price > 1000) {
      price = price / 1000
    }
    const lineCost = (price * ratio) / 100
    yarnCost += lineCost
  }

  const productionCost = Math.max(0, n(settings.productionCost))
  const dyeingCost = Math.max(0, n(settings.dyeingCost))
  const otherCost = Math.max(0, n(settings.otherCost))
  const yarnCostRounded = round4(yarnCost)

  if (!lossRateCheck.isValid) {
    return {
      ratioTotal: round2(ratioTotal),
      yarnCost: yarnCostRounded,
      productionCost,
      dyeingCost,
      otherCost,
      lossRate: lossRateCheck.value,
      isLossRateValid: false,
      errorMessage: lossRateCheck.message,
      totalCost: null
    }
  }

  // 3. 计算总成本：(纱线成本 + 生产工费 + 染整费 + 其他费用) ÷ (1 − 损耗率 ÷ 100)
  const lossRate = lossRateCheck.value
  const factor = 1 - lossRate / 100
  const sumCost = yarnCost + productionCost + dyeingCost + otherCost
  const totalCost = factor > 0 ? round2(sumCost / factor) : 0

  return {
    ratioTotal: round2(ratioTotal),
    yarnCost: yarnCostRounded,
    productionCost,
    dyeingCost,
    otherCost,
    lossRate,
    isLossRateValid: true,
    totalCost
  }
}
