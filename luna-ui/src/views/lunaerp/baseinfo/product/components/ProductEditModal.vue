<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="980px"
    top="5vh"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      @submit.prevent
    >
      <!-- ========== 基本信息 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, ease: EASE }"
      >
        <div class="section-title">基本信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="产品编号" prop="code">
              <el-input
                v-model="formData.code"
                placeholder="请输入或获取编号"
                :disabled="formType === 'update'"
              >
                <template #append>
                  <el-button :loading="codeLoading" @click="handleGetNextCode">
                    获取编号
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入产品名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-input v-model="formData.type" placeholder="如：成品 / 胚布" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成分" prop="composition">
              <el-input v-model="formData.composition" placeholder="如：棉 / 涤纶 / 氨纶" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门幅(cm)" prop="widthCm">
              <el-input-number
                v-model="formData.widthCm"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入门幅"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="克重(g/m²)" prop="weightGsm">
              <el-input-number
                v-model="formData.weightGsm"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入克重"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="颜色" prop="color">
              <el-input v-model="formData.color" placeholder="请输入颜色" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工艺类型" prop="craftType">
              <el-input v-model="formData.craftType" placeholder="请输入工艺类型" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机型" prop="machineType">
              <el-input v-model="formData.machineType" placeholder="请输入机型" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产量" prop="output">
              <el-input v-model="formData.output" placeholder="请输入产量" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="花型号" prop="patternNo">
              <el-input v-model="formData.patternNo" placeholder="请输入花型号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原厂编号" prop="supplierCode">
              <el-input v-model="formData.supplierCode" placeholder="请输入原厂编号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="临时编号" prop="tempCode">
              <el-input v-model="formData.tempCode" placeholder="请输入临时编号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标准米长" prop="standardLength">
              <el-input v-model="formData.standardLength" placeholder="请输入标准米长" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算长系数" prop="conversionRate">
              <el-input-number
                v-model="formData.conversionRate"
                :min="0"
                :precision="3"
                :controls="false"
                class="!w-100%"
                placeholder="请输入算长系数"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公开 / 推荐">
              <div class="switch-row">
                <el-switch v-model="formData.isPublic" inline-prompt active-text="公开" inactive-text="私有" />
                <el-switch
                  v-model="formData.isFeatured"
                  inline-prompt
                  active-text="推荐"
                  inactive-text="普通"
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="请输入备注"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </motion.div>

      <!-- ========== 胚布品种 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.05, ease: EASE }"
      >
        <div class="section-title">胚布品种</div>
        <el-form-item label="胚布品种">
          <div class="greige-field">
            <el-tag
              v-if="selectedGreige"
              size="large"
              type="info"
              effect="light"
              closable
              @close="clearGreige"
            >
              {{ formatGreige(selectedGreige) }}
            </el-tag>
            <span v-else-if="formData.greigeTypeId" class="greige-fallback">
              ID: {{ formData.greigeTypeId }}
              <el-button link type="danger" size="small" @click="clearGreige">清除</el-button>
            </span>
            <span v-else class="greige-placeholder">未归类胚布品种</span>
            <el-button @click="openGreigePicker">
              <Icon icon="ep:search" class="mr-3px" />选择胚布品种
            </el-button>
          </div>
        </el-form-item>
      </motion.div>

      <!-- ========== 产品标签 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.08, ease: EASE }"
      >
        <div class="section-title">产品标签</div>
        <el-form-item label="标签">
          <ProductTagZone v-model="tags" />
        </el-form-item>
      </motion.div>

      <!-- ========== 纱线规格 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.11, ease: EASE }"
      >
        <div class="section-title">
          纱线规格
          <el-button size="small" type="primary" plain class="ml-8px" @click="addYarnRow">
            <Icon icon="ep:plus" :size="13" />新增一行
          </el-button>
        </div>
        <el-table :data="yarnRows" border size="small">
          <el-table-column label="纱线规格" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.yarnSpec" placeholder="如 32S / 40S" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="纺法" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.spinMethod" placeholder="请输入纺法" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="比例(%)" width="120">
            <template #default="{ row }">
              <el-input v-model="row.ratio" placeholder="如 60:40" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="单价(元/kg)" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="row.price"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeYarnRow($index)">
                <Icon icon="ep:delete" :size="14" />
              </el-button>
            </template>
          </el-table-column>
          <template #empty>暂无纱线规格，点击「新增一行」添加</template>
        </el-table>
      </motion.div>

      <!-- ========== 成本项 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.14, ease: EASE }"
      >
        <div class="section-title">
          成本项
          <el-button size="small" type="primary" plain class="ml-8px" @click="addCostRow">
            <Icon icon="ep:plus" :size="13" />新增一行
          </el-button>
        </div>
        <el-table :data="costRows" border size="small">
          <el-table-column label="供应商" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.supplier" placeholder="请输入供应商" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="工序" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.process" placeholder="请输入工序" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="单价(元)" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="row.price"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="损耗(%)" width="140">
            <template #default="{ row }">
              <el-input-number
                v-model="row.lossPercent"
                :min="0"
                :max="100"
                :precision="2"
                :controls="false"
                class="!w-100%"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeCostRow($index)">
                <Icon icon="ep:delete" :size="14" />
              </el-button>
            </template>
          </el-table-column>
          <template #empty>暂无成本项，点击「新增一行」添加</template>
        </el-table>
      </motion.div>

      <!-- ========== 产品图片 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.17, ease: EASE }"
      >
        <div class="section-title">产品图片</div>
        <el-form-item label="封面图">
          <upload-img v-model="formData.coverImageUrl" />
        </el-form-item>
        <el-form-item label="详情多图">
          <upload-imgs v-model="imageUrls" :limit="9" />
        </el-form-item>
      </motion.div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm">
        确定
      </el-button>
    </template>

    <!-- 胚布品种选择弹窗 -->
    <GreigeTypePicker
      ref="greigePickerRef"
      v-model="formData.greigeTypeId"
      :options="greigeOptions"
      :loading="greigeLoading"
    />
  </el-dialog>
</template>

<script lang="ts" setup>
import { motion } from 'motion-v'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO, ProductImageVO } from '@/api/lunaerp/baseinfo/product'
import * as GreigeTypeApi from '@/api/lunaerp/baseinfo/greige-type'
import type { GreigeTypeSimpleVO } from '@/api/lunaerp/baseinfo/greige-type'
import type { FormInstance, FormRules } from 'element-plus'
import ProductTagZone from './ProductTagZone.vue'
import GreigeTypePicker from './GreigeTypePicker.vue'

defineOptions({ name: 'ProductEditModal' })

const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]

const message = ElMessage

/** 纱线规格动态行 */
interface YarnSpecRow {
  yarnSpec: string
  spinMethod: string
  ratio: string
  remark: string
  price: number | undefined
}

/** 成本项动态行 */
interface CostItemRow {
  supplier: string
  process: string
  price: number | undefined
  lossPercent: number | undefined
}

interface ZoneTag {
  id?: number
  name: string
  color: string
}

/** 表单数据（数值字段允许 undefined 以配合必填校验） */
interface ProductFormData {
  id?: number
  code: string
  name: string
  type: string
  composition: string
  widthCm: number | undefined
  weightGsm: number | undefined
  color: string
  craftType: string
  machineType: string
  output: string
  patternNo: string
  supplierCode: string
  tempCode: string
  standardLength: string
  conversionRate: number | undefined
  greigeTypeId: string | undefined
  remark: string
  isPublic: boolean
  isFeatured: boolean
  status: string
  coverImageUrl: string
}

const visible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const submitLoading = ref(false)
const codeLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref<FormInstance>()

const defaultFormData = (status: string = 'ACTIVE'): ProductFormData => ({
  id: undefined,
  code: '',
  name: '',
  type: '',
  composition: '',
  widthCm: undefined,
  weightGsm: undefined,
  color: '',
  craftType: '',
  machineType: '',
  output: '',
  patternNo: '',
  supplierCode: '',
  tempCode: '',
  standardLength: '',
  conversionRate: undefined,
  greigeTypeId: undefined,
  remark: '',
  isPublic: false,
  isFeatured: false,
  status,
  coverImageUrl: ''
})

const formData = ref<ProductFormData>(defaultFormData())
const tags = ref<ZoneTag[]>([])
const imageUrls = ref<string[]>([])
const yarnRows = ref<YarnSpecRow[]>([])
const costRows = ref<CostItemRow[]>([])

/** 数字必填且必须大于 0 */
const validatePositiveNumber = (_rule: any, value: number, callback: any) => {
  if (value === undefined || value === null || (value as unknown as string) === '') {
    callback(new Error('请输入大于 0 的数字'))
  } else if (Number(value) <= 0) {
    callback(new Error('必须大于 0'))
  } else {
    callback()
  }
}

const formRules = reactive<FormRules>({
  code: [{ required: true, message: '产品编号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '产品名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '类型不能为空', trigger: 'blur' }],
  composition: [{ required: true, message: '成分不能为空', trigger: 'blur' }],
  widthCm: [{ required: true, validator: validatePositiveNumber, trigger: 'blur' }],
  weightGsm: [{ required: true, validator: validatePositiveNumber, trigger: 'blur' }]
})

/** 容错解析 JSON 字符串数组（详情接口历史上可能回传字符串或数组） */
const parseJsonRows = <T,>(raw: unknown): T[] => {
  if (Array.isArray(raw)) return raw as T[]
  if (typeof raw === 'string' && raw.trim()) {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? (parsed as T[]) : []
    } catch {
      return []
    }
  }
  return []
}

// ========== 胚布品种 ==========
const greigeOptions = ref<GreigeTypeSimpleVO[]>([])
const greigeLoading = ref(false)
const greigePickerRef = ref()

const loadGreigeOptions = async () => {
  greigeLoading.value = true
  try {
    greigeOptions.value = await GreigeTypeApi.getSimpleGreigeTypeList()
  } finally {
    greigeLoading.value = false
  }
}

const formatGreige = (row?: GreigeTypeSimpleVO | null): string => {
  if (!row) return ''
  return `${row.name ?? ''} ${row.weightGsm ?? '-'}g×${row.widthCm ?? '-'}cm`
}

const selectedGreige = computed(() =>
  greigeOptions.value.find(
    (item) => formData.value.greigeTypeId && String(item.id) === String(formData.value.greigeTypeId)
  )
)

const openGreigePicker = () => {
  greigePickerRef.value?.open()
}

const clearGreige = () => {
  formData.value.greigeTypeId = undefined
}

// ========== 动态行 ==========
const addYarnRow = () => {
  yarnRows.value.push({ yarnSpec: '', spinMethod: '', ratio: '', remark: '', price: undefined })
}
const removeYarnRow = (index: number) => yarnRows.value.splice(index, 1)

const addCostRow = () => {
  costRows.value.push({ supplier: '', process: '', price: undefined, lossPercent: undefined })
}
const removeCostRow = (index: number) => costRows.value.splice(index, 1)

// ========== 获取编号 ==========
const handleGetNextCode = async () => {
  if (formType.value === 'update') return
  codeLoading.value = true
  try {
    const code = await ProductApi.getNextCode()
    formData.value.code = code || ''
  } finally {
    codeLoading.value = false
  }
}

/** 保存成功后回调父组件，父组件做原位缓存更新 */
const emit = defineEmits<{
  (e: 'success', payload: { mode: 'create' | 'update'; product: ProductVO }): void
}>()

/** 打开弹窗：create 时按当前分段状态落库，update 拉详情回显 */
const open = async (
  type: 'create' | 'update',
  id?: number,
  status: string = 'ACTIVE'
) => {
  formType.value = type
  dialogTitle.value = type === 'create' ? '新建产品' : '编辑产品'
  resetForm(status)
  visible.value = true

  // 胚布品种列表与表单互不阻塞
  loadGreigeOptions()

  if (type === 'update' && id) {
    formLoading.value = true
    try {
      const data = await ProductApi.getProduct(id)
      fillForm(data)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const resetForm = (status: string) => {
  formData.value = defaultFormData(status)
  tags.value = []
  imageUrls.value = []
  yarnRows.value = []
  costRows.value = []
  formRef.value?.clearValidate()
}

const fillForm = (data: ProductVO) => {
  formData.value = {
    ...defaultFormData(data.status || 'ACTIVE'),
    id: data.id,
    code: data.code ?? '',
    name: data.name ?? '',
    type: data.type ?? '',
    composition: data.composition ?? '',
    widthCm: data.widthCm ?? undefined,
    weightGsm: data.weightGsm ?? undefined,
    color: data.color ?? '',
    craftType: data.craftType ?? '',
    machineType: data.machineType ?? '',
    output: data.output ?? '',
    patternNo: data.patternNo ?? '',
    supplierCode: data.supplierCode ?? '',
    tempCode: data.tempCode ?? '',
    standardLength: data.standardLength ?? '',
    conversionRate: data.conversionRate ?? undefined,
    greigeTypeId: data.greigeTypeId ?? undefined,
    remark: data.remark ?? '',
    isPublic: !!data.isPublic,
    isFeatured: !!data.isFeatured,
    coverImageUrl: data.coverImageUrl ?? ''
  }
  tags.value = (data.tags ?? []).map((tag) => ({
    id: tag.id,
    name: tag.name,
    color: tag.color
  }))
  imageUrls.value = (data.images ?? [])
    .slice()
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map((image) => image.url)
  yarnRows.value = parseJsonRows<YarnSpecRow>(data.yarnSpecs).map((row) => ({
    yarnSpec: row.yarnSpec ?? '',
    spinMethod: row.spinMethod ?? '',
    ratio: row.ratio ?? '',
    remark: row.remark ?? '',
    price: row.price === undefined || row.price === null || row.price === '' ? undefined : Number(row.price)
  }))
  costRows.value = parseJsonRows<CostItemRow>(data.costItems).map((row) => ({
    supplier: row.supplier ?? '',
    process: row.process ?? '',
    price: row.price === undefined || row.price === null || row.price === '' ? undefined : Number(row.price),
    lossPercent:
      row.lossPercent === undefined || row.lossPercent === null || (row.lossPercent as unknown) === ''
        ? undefined
        : Number(row.lossPercent)
  }))
}

/** 拉取保存后的最新详情（用于父组件原位更新），失败返回 null */
const fetchSavedDetail = async (id?: number): Promise<ProductVO | null> => {
  if (!id) return null
  try {
    return await ProductApi.getProduct(id)
  } catch {
    return null
  }
}

/** 提交表单 */
const submitForm = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const images: ProductImageVO[] = imageUrls.value.map((url, sortOrder) => ({
      url,
      sortOrder
    }))
    // yarnSpecs / costItems 落库为数组 JSON 字符串
    const yarnSpecs = JSON.stringify(
      yarnRows.value.map((row) => ({ ...row, price: row.price ?? 0 }))
    )
    const costItems = JSON.stringify(
      costRows.value.map((row) => ({
        ...row,
        price: row.price ?? 0,
        lossPercent: row.lossPercent ?? 0
      }))
    )

    const payload: ProductVO = {
      ...(formData.value as ProductVO),
      coverImageUrl: formData.value.coverImageUrl || undefined,
      // 清空时回传 null，后端解除关联
      greigeTypeId: (formData.value.greigeTypeId || null) as string,
      images,
      // 标签仅提交 name/color，后端负责自动建档/同名同色
      tags: tags.value.map((tag) => ({ name: tag.name, color: tag.color })),
      yarnSpecs,
      costItems
    }

    if (formType.value === 'create') {
      const newId = await ProductApi.createProduct(payload)
      message.success('新建成功')
      const saved = (await fetchSavedDetail(newId as number)) || {
        ...payload,
        id: (newId as number) ?? undefined
      }
      visible.value = false
      emit('success', { mode: 'create', product: saved })
    } else {
      await ProductApi.updateProduct(payload)
      message.success('修改成功')
      const saved =
        (await fetchSavedDetail(formData.value.id)) || {
          ...payload,
          id: formData.value.id
        }
      visible.value = false
      emit('success', { mode: 'update', product: saved })
    }
  } finally {
    submitLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.form-section {
  padding: 14px 16px 4px;
  margin-bottom: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 18px;
}

.greige-field {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.greige-placeholder {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.greige-fallback {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

:deep(.upload-box) {
  .el-upload__tip {
    display: none;
  }
}
</style>
