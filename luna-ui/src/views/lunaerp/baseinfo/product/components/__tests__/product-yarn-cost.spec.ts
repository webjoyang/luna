import { describe, expect, test } from 'vitest'
import {
  calculateProductYarnCost,
  type ProductYarnCostSettings
} from '../product-yarn-cost'

/** 构造成本设置，未指定的字段默认 0 */
function settings(overrides: Partial<ProductYarnCostSettings> = {}): ProductYarnCostSettings {
  return {
    fabricConstruction: 'KNIT',
    wovenCalculationMode: 'SIMPLE',
    greigeWeightGsm: 0,
    greigeWidthCm: 0,
    knitProcessingFeePerKg: 0,
    weavingFeePerMeter: 0,
    otherProcessingFeePerMeter: 0,
    ...overrides
  }
}

describe('calculateProductYarnCost - 针织模式（basis=KG）', () => {
  test('空数组：全部输出 0，basis=KG', () => {
    const result = calculateProductYarnCost([], settings())
    expect(result).toEqual({
      ratioTotal: 0,
      missingPriceCount: 0,
      yarnCostPerKg: 0,
      yarnCostPerMeter: 0,
      processingCost: 0,
      totalCost: 0,
      kgPerMeter: 0,
      metersPerKg: 0,
      basis: 'KG'
    })
  })

  test('全零值：单价/比例/损耗率/加工费均为 0 时成本为 0', () => {
    const result = calculateProductYarnCost(
      [{ yarnSpec: '40S棉', price: 0, ratio: 0, wasteRate: 0 }],
      settings({ knitProcessingFeePerKg: 0 })
    )
    expect(result.ratioTotal).toBe(0)
    expect(result.yarnCostPerKg).toBe(0)
    expect(result.totalCost).toBe(0)
    // 单价为 0 视为未设置价格
    expect(result.missingPriceCount).toBe(1)
  })

  test('多纱线含损耗：纱线成本=Σ(单价×比例%/(1-损耗率))，总成本=纱线成本+针织加工费', () => {
    const result = calculateProductYarnCost(
      [
        { yarnSpec: '40S棉', price: 20, ratio: 60, wasteRate: 5 },
        { yarnSpec: '100D涤', price: 10, ratio: 40, wasteRate: 0 }
      ],
      settings({ knitProcessingFeePerKg: 2.5 })
    )
    expect(result.basis).toBe('KG')
    expect(result.ratioTotal).toBe(100)
    expect(result.missingPriceCount).toBe(0)
    // 20×0.6/0.95 + 10×0.4 = 12.631578… + 4 = 16.631578… → 4 位小数 16.6316
    expect(result.yarnCostPerKg).toBe(16.6316)
    expect(result.processingCost).toBe(2.5)
    expect(result.totalCost).toBe(19.1316)
    expect(result.yarnCostPerMeter).toBe(0)
    expect(result.kgPerMeter).toBe(0)
    expect(result.metersPerKg).toBe(0)
  })

  test('无价格：missingPriceCount 统计未设置价格的纱线数量', () => {
    const result = calculateProductYarnCost(
      [
        { yarnSpec: '40S棉', price: 20, ratio: 50 },
        { yarnSpec: '100D涤', ratio: 30 },
        { yarnSpec: '32S人棉', price: 0, ratio: 20 }
      ],
      settings()
    )
    expect(result.missingPriceCount).toBe(2)
    expect(result.ratioTotal).toBe(100)
    // 仅 40S棉 有价格：20×0.5 = 10
    expect(result.yarnCostPerKg).toBe(10)
  })

  test('损耗率超 100% 钳制为 99.9%', () => {
    const result = calculateProductYarnCost(
      [{ yarnSpec: '40S棉', price: 10, ratio: 100, wasteRate: 150 }],
      settings()
    )
    // 钳制后：10 / (1 - 0.999) = 10000
    expect(result.yarnCostPerKg).toBe(10000)
  })

  test('负损耗率钳制为 0', () => {
    const result = calculateProductYarnCost(
      [{ yarnSpec: '40S棉', price: 20, ratio: 100, wasteRate: -20 }],
      settings()
    )
    expect(result.yarnCostPerKg).toBe(20)
  })

  test('字符串数值入参（JSON 行回显场景）按数字解析', () => {
    const result = calculateProductYarnCost(
      [{ yarnSpec: '40S棉', price: '25', ratio: '60', wasteRate: '0' }],
      settings()
    )
    expect(result.ratioTotal).toBe(60)
    expect(result.yarnCostPerKg).toBe(15)
    expect(result.missingPriceCount).toBe(0)
  })

  test('未填写纱线规格的空行不参与计算', () => {
    const result = calculateProductYarnCost(
      [
        { yarnSpec: '40S棉', price: 20, ratio: 100 },
        { yarnSpec: '   ', price: 99, ratio: 50 }
      ],
      settings()
    )
    expect(result.ratioTotal).toBe(100)
    expect(result.missingPriceCount).toBe(0)
    expect(result.yarnCostPerKg).toBe(20)
  })
})

describe('calculateProductYarnCost - 梭织简易估算（basis=METER）', () => {
  test('每米重量=克重×门幅/100000，纱线成本=每米重量×混纺单价', () => {
    const result = calculateProductYarnCost(
      [
        { yarnSpec: '40S棉', price: 20, ratio: 60 },
        { yarnSpec: '100D涤', price: 10, ratio: 40 }
      ],
      settings({
        fabricConstruction: 'WOVEN',
        wovenCalculationMode: 'SIMPLE',
        greigeWeightGsm: 150,
        greigeWidthCm: 160,
        weavingFeePerMeter: 1.5,
        otherProcessingFeePerMeter: 0.3
      })
    )
    expect(result.basis).toBe('METER')
    // 150×160/100000 = 0.24 kg/m
    expect(result.kgPerMeter).toBe(0.24)
    expect(result.metersPerKg).toBe(4.1667)
    // 混纺单价 20×0.6+10×0.4=16，纱线成本/M = 0.24×16 = 3.84
    expect(result.yarnCostPerMeter).toBe(3.84)
    // 等效成本/KG = 3.84/0.24 = 16
    expect(result.yarnCostPerKg).toBe(16)
    expect(result.processingCost).toBe(1.8)
    expect(result.totalCost).toBe(5.64)
  })

  test('简易估算含损耗：混纺单价按投入损耗放大', () => {
    const result = calculateProductYarnCost(
      [{ yarnSpec: '40S棉', price: 20, ratio: 100, wasteRate: 5 }],
      settings({
        fabricConstruction: 'WOVEN',
        wovenCalculationMode: 'SIMPLE',
        greigeWeightGsm: 100,
        greigeWidthCm: 150
      })
    )
    // 每米重量 0.15，混纺单价 20/0.95=21.0526…，纱线成本/M = 3.157894… → 3.1579
    expect(result.kgPerMeter).toBe(0.15)
    expect(result.yarnCostPerMeter).toBe(3.1579)
  })

  test('克重/门幅为 0 时 kgPerMeter=0，metersPerKg 不做除零', () => {
    const result = calculateProductYarnCost(
      [{ yarnSpec: '40S棉', price: 20, ratio: 100 }],
      settings({ fabricConstruction: 'WOVEN', wovenCalculationMode: 'SIMPLE' })
    )
    expect(result.kgPerMeter).toBe(0)
    expect(result.metersPerKg).toBe(0)
    expect(result.yarnCostPerKg).toBe(0)
    expect(result.yarnCostPerMeter).toBe(0)
  })
})

describe('calculateProductYarnCost - 梭织经纬精算（basis=METER）', () => {
  test('经纱/纬纱分组按每米用量计算，不使用配比重复计算', () => {
    const result = calculateProductYarnCost(
      [
        { yarnSpec: '经纱', direction: 'WARP', price: 20, ratio: 60, consumptionKgPerMeter: 0.12 },
        { yarnSpec: '纬纱', direction: 'WEFT', price: 10, ratio: 40, consumptionKgPerMeter: 0.06 }
      ],
      settings({
        fabricConstruction: 'WOVEN',
        wovenCalculationMode: 'PROFESSIONAL',
        greigeWeightGsm: 150,
        greigeWidthCm: 160,
        weavingFeePerMeter: 1.5,
        otherProcessingFeePerMeter: 0.3
      })
    )
    expect(result.basis).toBe('METER')
    // 0.12×20 + 0.06×10 = 3（ratio 存在也不参与精算）
    expect(result.yarnCostPerMeter).toBe(3)
    expect(result.processingCost).toBe(1.8)
    expect(result.totalCost).toBe(4.8)
    expect(result.kgPerMeter).toBe(0.24)
  })

  test('经纬精算含损耗：每米用量×单价/(1-损耗率)', () => {
    const result = calculateProductYarnCost(
      [
        { yarnSpec: '经纱', direction: 'WARP', price: 20, consumptionKgPerMeter: 0.1, wasteRate: 10 }
      ],
      settings({ fabricConstruction: 'WOVEN', wovenCalculationMode: 'PROFESSIONAL' })
    )
    // 0.1×20/0.9 = 2.2222… → 2.2222
    expect(result.yarnCostPerMeter).toBe(2.2222)
  })

  test('经纬精算损耗率超 100% 钳制为 99.9%', () => {
    const result = calculateProductYarnCost(
      [
        {
          yarnSpec: '经纱',
          direction: 'WARP',
          price: 1,
          consumptionKgPerMeter: 1,
          wasteRate: 999
        }
      ],
      settings({ fabricConstruction: 'WOVEN', wovenCalculationMode: 'PROFESSIONAL' })
    )
    // 钳制后：1×1/(1-0.999) = 1000
    expect(result.yarnCostPerMeter).toBe(1000)
  })
})
