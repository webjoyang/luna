import { describe, expect, test } from 'vitest'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import {
  FABRIC_TYPE_DICT_TYPE,
  PRODUCT_CATEGORY_DICT_TYPE,
  createDefaultProductFormData,
  toProductFormData,
  toProductSavePayload,
  type DictLabelResolver,
  type ProductFormData
} from '../product-form'

/** ADR-0035 已删除的 4 个字段：表单与载荷都不应再出现 */
const REMOVED_FIELDS = ['output', 'patternNo', 'tempCode', 'conversionRate']

/** 字典桩：命中返回 label，未命中返回空串（与 getDictLabel 的行为一致） */
const DICT_FIXTURE: Record<string, Record<string, string>> = {
  [PRODUCT_CATEGORY_DICT_TYPE]: { FINISHED: '成品', GREIGE: '胚布', SEMI_FINISHED: '半成品' },
  [FABRIC_TYPE_DICT_TYPE]: { KNIT: '针织', WOVEN: '梭织', WARP_KNIT: '经编', WEFT_KNIT: '纬编' }
}
const resolveDictLabel: DictLabelResolver = (dictType, value) =>
  DICT_FIXTURE[dictType]?.[value] ?? ''

/** 空字典：模拟字典未加载或字典值已停用 */
const resolveEmptyDictLabel: DictLabelResolver = () => ''

function form(overrides: Partial<ProductFormData> = {}): ProductFormData {
  return { ...createDefaultProductFormData(), ...overrides }
}

/** 含 ADR-0035 全部新增字段的详情 VO（梭织经纬精算口径） */
function productVO(overrides: Partial<ProductVO> = {}): ProductVO {
  return {
    id: 1024,
    code: 'P0001',
    name: '莫代尔汗布',
    type: 'FINISHED',
    typeLabel: '成品',
    composition: 'NR',
    widthCm: 160,
    weightGsm: 260,
    supplierCode: 'SUP-001',
    standardLength: '100',
    color: '本白',
    craftType: '染色',
    machineType: '大圆机',
    greigeTypeId: 'gt-001',
    fabricType: 'WOVEN',
    fabricTypeLabel: '梭织',
    wovenTotalEnds: 8880,
    wovenReedNo: 144.5,
    wovenWarpDensity: 52,
    wovenWeftDensity: 28,
    wovenLoomWeftDensity: 30,
    status: 'ACTIVE',
    isPublic: true,
    isFeatured: false,
    remark: '备注',
    coverImageUrl: 'https://cdn/cover.png',
    ...overrides
  }
}

describe('createDefaultProductFormData - 新建缺省值', () => {
  test('缺省 status 为 ACTIVE，可透传分段状态', () => {
    expect(createDefaultProductFormData().status).toBe('ACTIVE')
    expect(createDefaultProductFormData('DRAFT').status).toBe('DRAFT')
  })

  test('不含 ADR-0035 已删除的 4 个字段', () => {
    const keys = Object.keys(createDefaultProductFormData())
    for (const removed of REMOVED_FIELDS) {
      expect(keys).not.toContain(removed)
    }
  })

  test('数值字段缺省 undefined（未填），文本字段缺省空串', () => {
    const data = createDefaultProductFormData()
    expect(data.widthCm).toBeUndefined()
    expect(data.weightGsm).toBeUndefined()
    expect(data.wovenTotalEnds).toBeUndefined()
    expect(data.wovenReedNo).toBeUndefined()
    expect(data.wovenWarpDensity).toBeUndefined()
    expect(data.wovenWeftDensity).toBeUndefined()
    expect(data.wovenLoomWeftDensity).toBeUndefined()
    expect(data.greigeTypeId).toBeUndefined()
    expect(data.code).toBe('')
    expect(data.type).toBe('')
    expect(data.typeLabel).toBe('')
    expect(data.fabricType).toBe('')
    expect(data.fabricTypeLabel).toBe('')
    expect(data.isPublic).toBe(false)
    expect(data.isFeatured).toBe(false)
  })
})

describe('toProductFormData - 详情回显', () => {
  test('ADR-0035 新增字段全部回显（类别/布种 value+label、梭织参数 5 列）', () => {
    const data = toProductFormData(productVO())
    expect(data.id).toBe(1024)
    expect(data.type).toBe('FINISHED')
    expect(data.typeLabel).toBe('成品')
    expect(data.fabricType).toBe('WOVEN')
    expect(data.fabricTypeLabel).toBe('梭织')
    expect(data.wovenTotalEnds).toBe(8880)
    expect(data.wovenReedNo).toBe(144.5)
    expect(data.wovenWarpDensity).toBe(52)
    expect(data.wovenWeftDensity).toBe(28)
    expect(data.wovenLoomWeftDensity).toBe(30)
    expect(data.supplierCode).toBe('SUP-001')
    expect(data.standardLength).toBe('100')
    expect(data.greigeTypeId).toBe('gt-001')
    expect(data.isPublic).toBe(true)
    expect(data.isFeatured).toBe(false)
  })

  test('数值以字符串回传（BigDecimal 序列化场景）时转 number', () => {
    const data = toProductFormData(
      productVO({
        widthCm: '160.50' as unknown as number,
        weightGsm: '260' as unknown as number,
        wovenReedNo: '144.50' as unknown as number,
        wovenLoomWeftDensity: '30.00' as unknown as number
      })
    )
    expect(data.widthCm).toBe(160.5)
    expect(data.weightGsm).toBe(260)
    expect(data.wovenReedNo).toBe(144.5)
    expect(data.wovenLoomWeftDensity).toBe(30)
  })

  test('null / 空串 / 非法数值统一归一为 undefined（未填）', () => {
    const data = toProductFormData(
      productVO({
        widthCm: null as unknown as number,
        wovenTotalEnds: null,
        wovenReedNo: '' as unknown as number,
        wovenWarpDensity: 'abc' as unknown as number,
        greigeTypeId: null,
        coverImageUrl: undefined
      })
    )
    expect(data.widthCm).toBeUndefined()
    expect(data.wovenTotalEnds).toBeUndefined()
    expect(data.wovenReedNo).toBeUndefined()
    expect(data.wovenWarpDensity).toBeUndefined()
    expect(data.greigeTypeId).toBeUndefined()
    expect(data.coverImageUrl).toBe('')
  })

  test('VO 缺失 status 时缺省 ACTIVE，文本字段缺失时缺省空串', () => {
    const data = toProductFormData({ code: 'P0002', name: 'x' } as ProductVO)
    expect(data.status).toBe('ACTIVE')
    expect(data.type).toBe('')
    expect(data.typeLabel).toBe('')
    expect(data.fabricType).toBe('')
    expect(data.fabricTypeLabel).toBe('')
    expect(data.remark).toBe('')
  })
})

describe('toProductSavePayload - 提交载荷', () => {
  test('类别与布种提交字典 value + label 冗余', () => {
    const payload = toProductSavePayload(
      form({ type: 'FINISHED', fabricType: 'KNIT' }),
      resolveDictLabel
    )
    expect(payload.type).toBe('FINISHED')
    expect(payload.typeLabel).toBe('成品')
    expect(payload.fabricType).toBe('KNIT')
    expect(payload.fabricTypeLabel).toBe('针织')
  })

  test('字典查不到该值时保留后端原标签，不把冗余列洗空', () => {
    const payload = toProductSavePayload(
      form({ type: 'LEGACY', typeLabel: '旧类别', fabricType: 'OLD', fabricTypeLabel: '旧布种' }),
      resolveEmptyDictLabel
    )
    expect(payload.type).toBe('LEGACY')
    expect(payload.typeLabel).toBe('旧类别')
    expect(payload.fabricType).toBe('OLD')
    expect(payload.fabricTypeLabel).toBe('旧布种')
  })

  test('布种清空时 value 与冗余标签成对提交 null（而非丢键）', () => {
    const payload = toProductSavePayload(
      form({ fabricType: '', fabricTypeLabel: '针织' }),
      resolveDictLabel
    )
    expect(Object.keys(payload)).toContain('fabricType')
    expect(Object.keys(payload)).toContain('fabricTypeLabel')
    expect(payload.fabricType).toBeNull()
    expect(payload.fabricTypeLabel).toBeNull()
  })

  test('类别为必填项无清空路径，字典缺失时不带 typeLabel 键', () => {
    const payload = toProductSavePayload(form({ type: '', typeLabel: '成品' }), resolveDictLabel)
    expect(payload.type).toBe('')
    expect(payload.typeLabel).toBeUndefined()
  })

  test('梭织参数已填时原样提交数字', () => {
    const payload = toProductSavePayload(
      form({
        wovenTotalEnds: 8880,
        wovenReedNo: 144.5,
        wovenWarpDensity: 52,
        wovenWeftDensity: 28,
        wovenLoomWeftDensity: 30
      }),
      resolveDictLabel
    )
    expect(payload.wovenTotalEnds).toBe(8880)
    expect(payload.wovenReedNo).toBe(144.5)
    expect(payload.wovenWarpDensity).toBe(52)
    expect(payload.wovenWeftDensity).toBe(28)
    expect(payload.wovenLoomWeftDensity).toBe(30)
  })

  test('梭织参数清空后提交 null（而非丢键），保证后端可清掉旧值', () => {
    const payload = toProductSavePayload(form(), resolveDictLabel)
    for (const key of [
      'wovenTotalEnds',
      'wovenReedNo',
      'wovenWarpDensity',
      'wovenWeftDensity',
      'wovenLoomWeftDensity'
    ] as const) {
      expect(Object.keys(payload)).toContain(key)
      expect(payload[key]).toBeNull()
    }
  })

  test('胚布品种未选择时提交 null 解除关联', () => {
    expect(toProductSavePayload(form(), resolveDictLabel).greigeTypeId).toBeNull()
    expect(
      toProductSavePayload(form({ greigeTypeId: 'gt-001' }), resolveDictLabel).greigeTypeId
    ).toBe('gt-001')
  })

  test('不含 yarnSpecs / costItems 键（工单 05 负责，避免覆盖库里已有 JSON）', () => {
    const keys = Object.keys(toProductSavePayload(form(), resolveDictLabel))
    expect(keys).not.toContain('yarnSpecs')
    expect(keys).not.toContain('costItems')
  })

  test('不含 ADR-0035 已删除的 4 个字段', () => {
    const keys = Object.keys(toProductSavePayload(form(), resolveDictLabel))
    for (const removed of REMOVED_FIELDS) {
      expect(keys).not.toContain(removed)
    }
  })

  test('封面图为空串时提交 undefined，基础字段原样透传', () => {
    const payload = toProductSavePayload(
      form({
        id: 1024,
        code: 'P0001',
        name: '莫代尔汗布',
        composition: 'NR',
        widthCm: 160,
        weightGsm: 260,
        supplierCode: 'SUP-001',
        standardLength: '100',
        color: '本白',
        craftType: '染色',
        machineType: '大圆机',
        remark: '备注',
        isPublic: true,
        isFeatured: true,
        status: 'DRAFT',
        coverImageUrl: ''
      }),
      resolveDictLabel
    )
    expect(payload.coverImageUrl).toBeUndefined()
    expect(payload.id).toBe(1024)
    expect(payload.code).toBe('P0001')
    expect(payload.composition).toBe('NR')
    expect(payload.widthCm).toBe(160)
    expect(payload.weightGsm).toBe(260)
    expect(payload.supplierCode).toBe('SUP-001')
    expect(payload.standardLength).toBe('100')
    expect(payload.color).toBe('本白')
    expect(payload.craftType).toBe('染色')
    expect(payload.machineType).toBe('大圆机')
    expect(payload.remark).toBe('备注')
    expect(payload.isPublic).toBe(true)
    expect(payload.isFeatured).toBe(true)
    expect(payload.status).toBe('DRAFT')
  })
})

describe('回显 → 提交 往返', () => {
  test('详情回显后再提交，表单自有字段保持稳定', () => {
    const origin = form({
      id: 1024,
      code: 'P0001',
      name: '莫代尔汗布',
      type: 'FINISHED',
      typeLabel: '成品',
      composition: 'NR',
      widthCm: 160,
      weightGsm: 260,
      fabricType: 'WOVEN',
      fabricTypeLabel: '梭织',
      greigeTypeId: 'gt-001',
      wovenTotalEnds: 8880,
      wovenReedNo: 144.5,
      wovenWarpDensity: 52,
      wovenWeftDensity: 28,
      wovenLoomWeftDensity: 30,
      standardLength: '100',
      supplierCode: 'SUP-001'
    })
    const roundTripped = toProductFormData(toProductSavePayload(origin, resolveDictLabel))
    expect(roundTripped).toEqual(origin)
  })
})
