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
      <!-- ========== 1. 图片 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, ease: EASE }"
      >
        <div class="section-title">图片</div>
        <el-form-item label="封面图">
          <upload-img v-model="formData.coverImageUrl" />
        </el-form-item>
        <el-form-item label="详情多图">
          <upload-imgs v-model="imageUrls" :limit="9" />
        </el-form-item>
      </motion.div>

      <!-- ========== 2. 基础信息 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.05, ease: EASE }"
      >
        <div class="section-title">基础信息</div>
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
            <el-form-item label="品名" prop="name">
              <el-input v-model="formData.name" placeholder="请输入品名" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别" prop="type">
              <DictSelect
                v-model="formData.type"
                :dict-type="DICT_TYPE.LUNAERP_PRODUCT_CATEGORY"
                placeholder="请选择产品类别"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商样号" prop="supplierCode">
              <el-input v-model="formData.supplierCode" placeholder="请输入供应商样号" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成分" prop="composition">
              <el-input
                v-model="formData.composition"
                placeholder="如：棉 / 涤纶 / 氨纶"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
          <el-col :span="24">
            <el-form-item label="产品标签">
              <ProductTagZone v-model="tags" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="展示设置">
              <div class="switch-row">
                <el-switch
                  v-model="formData.isPublic"
                  inline-prompt
                  active-text="对外展示"
                  inactive-text="仅内部"
                />
                <el-switch
                  v-model="formData.isFeatured"
                  inline-prompt
                  active-text="首页推荐"
                  inactive-text="不推荐"
                />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </motion.div>

      <!-- ========== 3. 技术规格 ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.08, ease: EASE }"
      >
        <div class="section-title">技术规格</div>
        <el-row :gutter="16">
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
            <el-form-item label="布种" prop="fabricType">
              <DictSelect
                v-model="formData.fabricType"
                :dict-type="DICT_TYPE.LUNAERP_FABRIC_TYPE"
                placeholder="请选择布种"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
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
          </el-col>
          <el-col :span="12">
            <el-form-item label="标准米长" prop="standardLength">
              <el-input v-model="formData.standardLength" placeholder="请输入标准米长" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="sub-title">
          梭织参数
          <span class="sub-tip">选填；经密 / 纬密 / 上机纬密单位 根/CM</span>
        </div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="总经根数" prop="wovenTotalEnds">
              <el-input-number
                v-model="formData.wovenTotalEnds"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入总经根数"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="筘号" prop="wovenReedNo">
              <el-input-number
                v-model="formData.wovenReedNo"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入筘号"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经密" prop="wovenWarpDensity">
              <el-input-number
                v-model="formData.wovenWarpDensity"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入经密"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬密" prop="wovenWeftDensity">
              <el-input-number
                v-model="formData.wovenWeftDensity"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入纬密"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上机纬密" prop="wovenLoomWeftDensity">
              <el-input-number
                v-model="formData.wovenLoomWeftDensity"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-100%"
                placeholder="请输入上机纬密"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </motion.div>

      <!-- ========== 4. 纱线规格（占位，工单 05 接入 ProductYarnCostSection） ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.11, ease: EASE }"
      >
        <div class="section-title">纱线规格</div>
        <div class="section-placeholder">
          <Icon icon="ep:box" :size="15" />
          <span>针织 / 梭织双 Tab 录入即将上线，产品已有的纱线规格数据保持不变。</span>
        </div>
      </motion.div>

      <!-- ========== 5. 成本结构明细（占位，工单 05 接入 ProductYarnCostSection） ========== -->
      <motion.div
        class="form-section"
        :initial="{ opacity: 0, y: 14 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.35, delay: 0.14, ease: EASE }"
      >
        <div class="section-title">成本结构明细</div>
        <div class="section-placeholder">
          <Icon icon="ep:coin" :size="15" />
          <span>成本项、加工费与成本汇总即将上线，产品已有的成本明细数据保持不变。</span>
        </div>
      </motion.div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm"> 确定 </el-button>
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
import { DICT_TYPE, getDictLabel } from '@/utils/dict'
import type { FormInstance, FormRules } from 'element-plus'
import DictSelect from '@/components/FormCreate/src/components/DictSelect.vue'
import ProductTagZone from './ProductTagZone.vue'
import GreigeTypePicker from './GreigeTypePicker.vue'
import {
  createDefaultProductFormData,
  toProductFormData,
  toProductSavePayload,
  type ProductFormData
} from './product-form'

defineOptions({ name: 'ProductEditModal' })

const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]

const message = ElMessage

interface ZoneTag {
  id?: number
  name: string
  color: string
}

const visible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const submitLoading = ref(false)
const codeLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref<FormInstance>()

const formData = ref<ProductFormData>(createDefaultProductFormData())
const tags = ref<ZoneTag[]>([])
const imageUrls = ref<string[]>([])

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
  name: [{ required: true, message: '品名不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '产品类别不能为空', trigger: 'change' }],
  composition: [{ required: true, message: '成分不能为空', trigger: 'blur' }],
  widthCm: [{ required: true, validator: validatePositiveNumber, trigger: 'blur' }],
  weightGsm: [{ required: true, validator: validatePositiveNumber, trigger: 'blur' }]
})

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
const open = async (type: 'create' | 'update', id?: number, status: string = 'ACTIVE') => {
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
  formData.value = createDefaultProductFormData(status)
  tags.value = []
  imageUrls.value = []
  formRef.value?.clearValidate()
}

const fillForm = (data: ProductVO) => {
  formData.value = toProductFormData(data)
  tags.value = (data.tags ?? []).map((tag) => ({
    id: tag.id,
    name: tag.name,
    color: tag.color
  }))
  imageUrls.value = (data.images ?? [])
    .slice()
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map((image) => image.url)
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
    const payload: ProductVO = {
      // 类别 / 布种的字典标签在提交时按当前字典重算，字典缺失该值则沿用原标签
      ...toProductSavePayload(formData.value, getDictLabel),
      images,
      // 标签仅提交 name/color，后端负责自动建档/同名同色
      tags: tags.value.map((tag) => ({ name: tag.name, color: tag.color }))
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
      const saved = (await fetchSavedDetail(formData.value.id)) || {
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
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.section-title {
  display: flex;
  padding-left: 10px;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  border-left: 3px solid var(--el-color-primary);
  align-items: center;
}

.sub-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 2px 0 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.sub-tip {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.section-placeholder {
  display: flex;
  padding: 18px 14px;
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  align-items: center;
  gap: 8px;
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
