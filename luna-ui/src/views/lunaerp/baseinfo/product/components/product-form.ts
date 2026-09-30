/**
 * 产品编辑表单的数据映射层（ADR-0035）：详情 VO → 表单回显、表单 → 提交载荷。
 *
 * 刻意不依赖 Vue 响应式与 pinia 字典 store，纯函数便于单元测试；
 * 字典标签由调用方注入解析函数（组件里传 `getDictLabel`）。
 */
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'

/**
 * 产品类别字典类型。
 * 与 `DICT_TYPE.LUNAERP_PRODUCT_CATEGORY` 同值，此处本地声明以保持本模块零运行时依赖（不引入 pinia）。
 */
export const PRODUCT_CATEGORY_DICT_TYPE = 'lunaerp_product_category'

/**
 * 布种字典类型。
 * 与 `DICT_TYPE.LUNAERP_FABRIC_TYPE` 同值，同上。
 */
export const FABRIC_TYPE_DICT_TYPE = 'lunaerp_fabric_type'

/** 字典标签解析：按字典类型 + 字典值取标签，取不到返回空串 */
export type DictLabelResolver = (dictType: string, value: string) => string

/**
 * 产品编辑表单数据。
 *
 * 数值字段用 `undefined` 表示「未填」以配合 el-input-number 与必填校验，提交时统一转 null 让后端能清列。
 * 仅覆盖编辑弹窗直接维护的字段：图片、标签走独立 ref，纱线规格 / 成本结构明细由工单 05 的组件接管。
 */
export interface ProductFormData {
  id?: number
  code: string
  name: string
  /** 产品类别（字典 lunaerp_product_category 的 value） */
  type: string
  /** 产品类别字典标签（名称冗余） */
  typeLabel: string
  /** 供应商样号（ADR-0035 更正标签，原「原厂编号」） */
  supplierCode: string
  composition: string
  remark: string
  isPublic: boolean
  isFeatured: boolean
  status: string
  coverImageUrl: string
  widthCm: number | undefined
  weightGsm: number | undefined
  color: string
  craftType: string
  machineType: string
  /** 布种（字典 lunaerp_fabric_type 的 value） */
  fabricType: string
  /** 布种字典标签（名称冗余） */
  fabricTypeLabel: string
  greigeTypeId: string | undefined
  standardLength: string
  wovenTotalEnds: number | undefined
  wovenReedNo: number | undefined
  wovenWarpDensity: number | undefined
  wovenWeftDensity: number | undefined
  wovenLoomWeftDensity: number | undefined
}

/** 新建表单缺省值；status 跟随列表当前分段（在售 / 草稿） */
export const createDefaultProductFormData = (status: string = 'ACTIVE'): ProductFormData => ({
  id: undefined,
  code: '',
  name: '',
  type: '',
  typeLabel: '',
  supplierCode: '',
  composition: '',
  remark: '',
  isPublic: false,
  isFeatured: false,
  status,
  coverImageUrl: '',
  widthCm: undefined,
  weightGsm: undefined,
  color: '',
  craftType: '',
  machineType: '',
  fabricType: '',
  fabricTypeLabel: '',
  greigeTypeId: undefined,
  standardLength: '',
  wovenTotalEnds: undefined,
  wovenReedNo: undefined,
  wovenWarpDensity: undefined,
  wovenWeftDensity: undefined,
  wovenLoomWeftDensity: undefined
})

/**
 * 数值归一：null / 空串 / 非法值一律视为「未填」。
 * 后端 BigDecimal 可能被序列化成字符串（如 "144.50"），此处统一转 number。
 */
const toNumberOrUndefined = (value: unknown): number | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

/** 文本归一：null / undefined 转空串，避免 el-input 受控值出现 null */
const toText = (value: unknown): string =>
  value === undefined || value === null ? '' : String(value)

/** 详情 VO → 表单数据（编辑回显） */
export const toProductFormData = (data: ProductVO): ProductFormData => ({
  ...createDefaultProductFormData(data.status || 'ACTIVE'),
  id: data.id,
  code: toText(data.code),
  name: toText(data.name),
  type: toText(data.type),
  typeLabel: toText(data.typeLabel),
  supplierCode: toText(data.supplierCode),
  composition: toText(data.composition),
  remark: toText(data.remark),
  isPublic: !!data.isPublic,
  isFeatured: !!data.isFeatured,
  coverImageUrl: toText(data.coverImageUrl),
  widthCm: toNumberOrUndefined(data.widthCm),
  weightGsm: toNumberOrUndefined(data.weightGsm),
  color: toText(data.color),
  craftType: toText(data.craftType),
  machineType: toText(data.machineType),
  fabricType: toText(data.fabricType),
  fabricTypeLabel: toText(data.fabricTypeLabel),
  greigeTypeId: data.greigeTypeId ? String(data.greigeTypeId) : undefined,
  standardLength: toText(data.standardLength),
  wovenTotalEnds: toNumberOrUndefined(data.wovenTotalEnds),
  wovenReedNo: toNumberOrUndefined(data.wovenReedNo),
  wovenWarpDensity: toNumberOrUndefined(data.wovenWarpDensity),
  wovenWeftDensity: toNumberOrUndefined(data.wovenWeftDensity),
  wovenLoomWeftDensity: toNumberOrUndefined(data.wovenLoomWeftDensity)
})

/**
 * 字典标签冗余：以当前字典为准，字典查不到该值（未加载 / 已停用）时回退到已存标签，
 * 避免把 typeLabel / fabricTypeLabel 冗余列洗空。value 为空则标签一并清空。
 */
const resolveDictLabelOrFallback = (
  resolveDictLabel: DictLabelResolver,
  dictType: string,
  value: string,
  fallback: string
): string | undefined => {
  if (!value) {
    return undefined
  }
  return resolveDictLabel(dictType, value) || fallback || undefined
}

/**
 * 表单数据 → 保存载荷。
 *
 * 刻意不带 yarnSpecs / costItems：这两个 JSON 列由工单 05 的纱线规格与成本结构明细分区维护，
 * 缺键时后端 `updateById` 跳过该列，库里已有内容不会被覆盖。
 * 选填字段清空后一律以 null 提交（而非丢键），配合 ProductDO 的 `FieldStrategy.ALWAYS` 才能真正清掉旧值。
 */
export const toProductSavePayload = (
  form: ProductFormData,
  resolveDictLabel: DictLabelResolver
): ProductVO => ({
  id: form.id,
  code: form.code,
  name: form.name,
  type: form.type,
  // 类别为必填项（前端规则 + 后端 @NotBlank），没有清空路径，标签缺字典时直接不带该键
  typeLabel: resolveDictLabelOrFallback(
    resolveDictLabel,
    PRODUCT_CATEGORY_DICT_TYPE,
    form.type,
    form.typeLabel
  ),
  supplierCode: form.supplierCode,
  composition: form.composition,
  remark: form.remark,
  isPublic: form.isPublic,
  isFeatured: form.isFeatured,
  status: form.status,
  // 封面图未上传时不带该键，避免把已有封面写成空串
  coverImageUrl: form.coverImageUrl || undefined,
  widthCm: form.widthCm as number,
  weightGsm: form.weightGsm as number,
  color: form.color,
  craftType: form.craftType,
  machineType: form.machineType,
  // 布种为选填项，清空时 value 与冗余标签成对提交 null
  fabricType: form.fabricType || null,
  fabricTypeLabel:
    resolveDictLabelOrFallback(
      resolveDictLabel,
      FABRIC_TYPE_DICT_TYPE,
      form.fabricType,
      form.fabricTypeLabel
    ) ?? null,
  // 清空时回传 null，后端解除胚布品种关联
  greigeTypeId: form.greigeTypeId || null,
  standardLength: form.standardLength,
  wovenTotalEnds: form.wovenTotalEnds ?? null,
  wovenReedNo: form.wovenReedNo ?? null,
  wovenWarpDensity: form.wovenWarpDensity ?? null,
  wovenWeftDensity: form.wovenWeftDensity ?? null,
  wovenLoomWeftDensity: form.wovenLoomWeftDensity ?? null
})
