<template>
  <ContentWrap>
    <!-- 头部导航与操作 -->
    <div class="workspace-header flex justify-between items-center mb-20px">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="font-bold text-16px">{{ pageTitle }}</span>
        </template>
      </el-page-header>
      <div class="action-buttons">
        <el-button @click="handleBack">返回</el-button>
        <el-button
          v-if="bomId"
          type="danger"
          plain
          @click="handleDelete"
          v-hasPermi="['lunaerp:production:yarn-recipe-bom:delete']"
        >
          删除
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave"
          v-hasPermi="['lunaerp:production:yarn-recipe-bom:create', 'lunaerp:production:yarn-recipe-bom:update']"
        >
          保存
        </el-button>
      </div>
    </div>

    <!-- 主表信息表单 -->
    <el-card shadow="never" class="mb-20px">
      <template #header>
        <div class="font-bold">基本信息</div>
      </template>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="产品档案" prop="productId">
              <el-select
                v-model="formData.productId"
                placeholder="请输入产品编号/名称远程搜索"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchProducts"
                :loading="productLoading"
                class="!w-full"
                @change="handleProductChange"
                :disabled="!!bomId"
              >
                <el-option
                  v-for="item in productOptions"
                  :key="item.id"
                  :label="`${item.code} - ${item.name}`"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品编码">
              <el-input v-model="formData.productCode" disabled placeholder="选择产品后自动带出" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品名称">
              <el-input v-model="formData.productName" disabled placeholder="选择产品后自动带出" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" class="!w-full">
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入备注说明"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 配比明细表单 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-4">
            <span class="font-bold">配比明细</span>
            <el-tag :type="totalRatio === 100 ? 'success' : 'warning'" size="large">
              配比合计：{{ totalRatio.toFixed(2) }}%
            </el-tag>
            <span class="text-gray-400 text-12px">（配比之和不强制等于 100%）</span>
          </div>
          <el-button
            type="primary"
            plain
            size="small"
            @click="handleAddRow"
          >
            <Icon icon="ep:plus" class="mr-1" /> 添加明细行
          </el-button>
        </div>
      </template>

      <el-table :data="formData.items" :row-key="getRowKey" border style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column label="纱线规格" min-width="240">
          <template #default="{ row }">
            <el-cascader
              v-model="row.specCascadeValue"
              :options="cascaderOptions"
              :props="{ checkStrictly: false }"
              placeholder="选择材质与规格"
              filterable
              class="!w-full"
              @change="(val) => handleSpecCascadeChange(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="配比(%)" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.ratio"
              :min="0"
              :max="100"
              :precision="2"
              :step="1"
              class="!w-full"
              placeholder="比例"
            />
          </template>
        </el-table-column>

        <el-table-column label="纺纱工艺" min-width="140">
          <template #default="{ row }">
            <el-input
              v-model="row.spinMethod"
              placeholder="如：环锭纺"
            />
          </template>
        </el-table-column>

        <el-table-column label="参考单价" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.refPrice"
              :min="0"
              :precision="4"
              :step="0.5"
              class="!w-full"
              placeholder="单价"
            />
          </template>
        </el-table-column>

        <el-table-column label="方向" width="120">
          <template #default="{ row }">
            <el-select v-model="row.direction" placeholder="通用" clearable class="!w-full">
              <el-option label="通用" value="GENERAL" />
              <el-option label="经纱" value="WARP" />
              <el-option label="纬纱" value="WEFT" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="损耗率(%)" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.wasteRate"
              :min="0"
              :max="100"
              :precision="2"
              :step="0.5"
              class="!w-full"
              placeholder="损耗率"
            />
          </template>
        </el-table-column>

        <el-table-column label="密度" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.density"
              :min="0"
              :precision="2"
              :step="1"
              class="!w-full"
              placeholder="经/纬密"
            />
          </template>
        </el-table-column>

        <el-table-column label="织缩率(%)" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.crimpRate"
              :min="0"
              :max="100"
              :precision="2"
              :step="0.5"
              class="!w-full"
              placeholder="织缩率"
            />
          </template>
        </el-table-column>

        <el-table-column label="每米用量(kg/m)" width="150">
          <template #default="{ row }">
            <el-input-number
              v-model="row.consumptionKgPerMeter"
              :min="0"
              :precision="6"
              :step="0.01"
              class="!w-full"
              placeholder="每米用量"
            />
          </template>
        </el-table-column>

        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <el-input
              v-model="row.remark"
              placeholder="行备注"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button
              link
              type="danger"
              @click="handleRemoveRow($index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="formData.items.length === 0" class="text-center py-40px text-gray-400">
        暂无明细行，请点击右上角"添加明细行"按钮
      </div>
    </el-card>
  </ContentWrap>
</template>

<script lang="ts" setup>
import {
  getYarnRecipeBom,
  createYarnRecipeBom,
  updateYarnRecipeBom,
  deleteYarnRecipeBom,
  type YarnRecipeBomSaveReqVO
} from '@/api/lunaerp-elmui/production/yarn-recipe-bom'
import {
  getYarnSpecCascadeOptions,
  type YarnSpecCascadeOptionVO
} from '@/api/lunaerp-elmui/production/yarn-master'
import { getProductPage, type ProductVO } from '@/api/lunaerp/baseinfo/product'

defineOptions({ name: 'LunaerpElmuiProductionYarnRecipeBomWorkspace' })

const router = useRouter()
const route = useRoute()
const message = useMessage()
const { t } = useI18n()

const bomId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : null
})

const pageTitle = computed(() => {
  return !bomId.value ? '新建纱线配比BOM' : '编辑纱线配比BOM'
})

interface EditableBomItem {
  id?: number
  recipeBomId?: number
  yarnTypeId: number
  yarnTypeName?: string
  yarnSpecId: number
  yarnSpecName?: string
  ratio: number
  spinMethod?: string
  remark?: string
  refPrice?: number
  wasteRate?: number
  direction?: string
  density?: number
  crimpRate?: number
  consumptionKgPerMeter?: number
  specCascadeValue?: [number, number]
  _clientKey?: string
}

let tempKeyCounter = 0
/** 明细表格以行 id 为稳定 key；新增行未落库前以临时 key 保证行唯一性（ADR-0032） */
const getRowKey = (row: EditableBomItem): string => {
  if (row.id != null) {
    return String(row.id)
  }
  if (!row._clientKey) {
    row._clientKey = `temp_${++tempKeyCounter}`
  }
  return row._clientKey
}

const formRef = ref()
const saving = ref(false)
const productLoading = ref(false)
const productOptions = ref<ProductVO[]>([])
const rawCascadeOptions = ref<YarnSpecCascadeOptionVO[]>([])

const formData = reactive<{
  id?: number
  productId?: number
  productCode?: string
  productName?: string
  remark?: string
  status: string
  items: EditableBomItem[]
}>({
  id: undefined,
  productId: undefined,
  productCode: '',
  productName: '',
  remark: '',
  status: 'ACTIVE',
  items: []
})

const rules = {
  productId: [{ required: true, message: '产品档案不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

import type { CascaderOption } from 'element-plus'

// 级联选项计算
const cascaderOptions = computed<CascaderOption[]>(() => {
  return rawCascadeOptions.value.map(type => ({
    value: type.yarnTypeId,
    label: type.yarnTypeName,
    children: (type.specs || []).map(spec => ({
      value: spec.id,
      label: spec.specName,
      rawSpec: spec,
      typeId: type.yarnTypeId,
      typeName: type.yarnTypeName
    }))
  }))
})

// 配比合计
const totalRatio = computed(() => {
  return formData.items.reduce((sum, item) => sum + (Number(item.ratio) || 0), 0)
})

// 规格级联选择事件
const handleSpecCascadeChange = (row: EditableBomItem, val: any) => {
  if (!val || val.length < 2) {
    row.yarnTypeId = undefined as any
    row.yarnSpecId = undefined as any
    return
  }
  const [typeId, specId] = val as [number, number]
  row.yarnTypeId = typeId
  row.yarnSpecId = specId

  // 查找对应规格
  const typeOpt = rawCascadeOptions.value.find(t => t.yarnTypeId === typeId)
  if (typeOpt) {
    row.yarnTypeName = typeOpt.yarnTypeName
    const specOpt = (typeOpt.specs || []).find(s => s.id === specId)
    if (specOpt) {
      row.yarnSpecName = specOpt.specName
      if (!row.spinMethod && specOpt.spinMethod) {
        row.spinMethod = specOpt.spinMethod
      }
      if (row.refPrice === undefined || row.refPrice === null) {
        row.refPrice = specOpt.refPrice
      }
    }
  }
}

// 产品远程搜索
const searchProducts = async (query: string) => {
  if (!query) {
    productOptions.value = []
    return
  }
  productLoading.value = true
  try {
    const res = await getProductPage({
      search: query.trim(),
      pageNo: 1,
      pageSize: 20
    })
    productOptions.value = res.list || []
  } finally {
    productLoading.value = false
  }
}

// 产品选择变更
const handleProductChange = (productId?: number) => {
  if (!productId) {
    formData.productCode = ''
    formData.productName = ''
    return
  }
  const found = productOptions.value.find(p => p.id === productId)
  if (found) {
    formData.productCode = found.code
    formData.productName = found.name
  }
}

// 增行
const handleAddRow = () => {
  formData.items.push({
    _clientKey: `temp_${++tempKeyCounter}`,
    id: undefined,
    yarnTypeId: undefined as any,
    yarnSpecId: undefined as any,
    ratio: 0,
    spinMethod: '',
    remark: '',
    refPrice: undefined,
    wasteRate: undefined,
    direction: undefined,
    density: undefined,
    crimpRate: undefined,
    consumptionKgPerMeter: undefined,
    specCascadeValue: undefined
  })
}

// 删行
const handleRemoveRow = (index: number) => {
  formData.items.splice(index, 1)
}

// 加载级联选项
const loadCascadeOptions = async () => {
  try {
    const res = await getYarnSpecCascadeOptions()
    rawCascadeOptions.value = res || []
  } catch {}
}

// 加载详情
const loadDetail = async (id: number) => {
  try {
    const data = await getYarnRecipeBom(id)
    formData.id = data.id
    formData.productId = data.productId
    formData.productCode = data.productCode
    formData.productName = data.productName
    formData.remark = data.remark
    formData.status = data.status || 'ACTIVE'

    // 回显产品选项
    if (data.productId) {
      productOptions.value = [
        {
          id: data.productId,
          code: data.productCode || '',
          name: data.productName || ''
        } as ProductVO
      ]
    }

    // 回显明细
    formData.items = (data.items || []).map(item => ({
      id: item.id,
      recipeBomId: item.recipeBomId,
      yarnTypeId: item.yarnTypeId,
      yarnTypeName: item.yarnTypeName,
      yarnSpecId: item.yarnSpecId,
      yarnSpecName: item.yarnSpecName,
      ratio: item.ratio,
      spinMethod: item.spinMethod,
      remark: item.remark,
      refPrice: item.refPrice,
      wasteRate: item.wasteRate,
      direction: item.direction,
      density: item.density,
      crimpRate: item.crimpRate,
      consumptionKgPerMeter: item.consumptionKgPerMeter,
      specCascadeValue: [item.yarnTypeId, item.yarnSpecId]
    }))
  } catch {}
}

// 保存
const handleSave = async () => {
  await formRef.value.validate()

  // 校验明细
  for (let i = 0; i < formData.items.length; i++) {
    const item = formData.items[i]
    if (!item.yarnTypeId || !item.yarnSpecId) {
      message.error(`第 ${i + 1} 行纱线规格不能为空`)
      return
    }
    if (item.ratio === undefined || item.ratio === null) {
      message.error(`第 ${i + 1} 行配比比例不能为空`)
      return
    }
  }

  saving.value = true
  try {
    const payload: YarnRecipeBomSaveReqVO = {
      id: formData.id,
      productId: formData.productId!,
      productCode: formData.productCode,
      productName: formData.productName,
      remark: formData.remark,
      status: formData.status,
      items: formData.items.map(item => ({
        id: item.id != null ? item.id : undefined,
        yarnTypeId: item.yarnTypeId,
        yarnSpecId: item.yarnSpecId,
        ratio: item.ratio,
        spinMethod: item.spinMethod,
        remark: item.remark,
        refPrice: item.refPrice,
        wasteRate: item.wasteRate,
        direction: item.direction,
        density: item.density,
        crimpRate: item.crimpRate,
        consumptionKgPerMeter: item.consumptionKgPerMeter
      }))
    }

    if (bomId.value) {
      await updateYarnRecipeBom(payload)
      message.success(t('common.updateSuccess'))
    } else {
      const newId = await createYarnRecipeBom(payload)
      message.success(t('common.createSuccess'))
      router.replace(`/production/yarn-recipe-bom/workspace/${newId}`)
      return
    }
    handleBack()
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = async () => {
  if (!bomId.value) return
  try {
    await message.delConfirm()
    await deleteYarnRecipeBom(bomId.value)
    message.success(t('common.delSuccess'))
    handleBack()
  } catch {}
}

// 返回
const handleBack = () => {
  router.push('/production/yarn-recipe-bom')
}

onMounted(async () => {
  await loadCascadeOptions()
  if (bomId.value) {
    await loadDetail(bomId.value)
  }
})
</script>

<style scoped>
.workspace-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 12px;
}
</style>
