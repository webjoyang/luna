<template>
  <div class="py-16px">
    <div class="mb-12px flex justify-between items-center">
      <div class="text-14px font-medium text-gray-700">
        明细清单（共 {{ itemsList.length }} 条明细）
      </div>
      <el-button
        v-if="!isReadOnly"
        type="primary"
        plain
        size="small"
        @click="handleAddRow"
      >
        <Icon icon="ep:plus" class="mr-2px" /> 添加明细
      </el-button>
    </div>

    <el-table :data="itemsList" border stripe class="w-full">
      <el-table-column label="#" type="index" width="50" align="center" />

      <!-- 物品类型选择 -->
      <el-table-column label="物品类型" width="120">
        <template #default="{ row }">
          <el-select
            v-if="!isReadOnly"
            v-model="row.itemType"
            placeholder="类型"
            class="w-full"
            @change="(val: string) => handleItemTypeChange(row, val)"
          >
            <el-option label="产品" value="PRODUCT" />
            <el-option label="纱线规格" value="YARN_SPEC" />
          </el-select>
          <template v-else>
            <el-tag v-if="row.itemType === 'YARN_SPEC'" type="warning" size="small">纱线规格</el-tag>
            <el-tag v-else type="primary" size="small">产品</el-tag>
          </template>
        </template>
      </el-table-column>

      <!-- 物品选择（产品或纱线规格级联） -->
      <el-table-column label="物品（产品 / 纱线规格）" min-width="240">
        <template #default="{ row }">
          <!-- 纱线规格模式：材质→规格级联选择 -->
          <template v-if="row.itemType === 'YARN_SPEC'">
            <template v-if="!isReadOnly">
              <el-cascader
                v-model="row.specCascadeValue"
                :options="cascaderOptions"
                :props="{ checkStrictly: false }"
                placeholder="选择材质与规格"
                filterable
                class="!w-full"
                @change="(val: any) => handleSpecCascadeChange(row, val)"
              />
            </template>
            <template v-else>
              <span>{{ row.yarnSpecName || '—' }}</span>
            </template>
          </template>

          <!-- 产品模式：远程搜索产品档案 -->
          <template v-else>
            <template v-if="!isReadOnly">
              <el-select
                v-model="row.productId"
                placeholder="搜索产品编码/名称"
                filterable
                remote
                reserve-keyword
                :remote-method="searchProducts"
                :loading="productLoading"
                class="w-full"
                @change="(val: number) => handleProductChange(row, val)"
              >
                <el-option
                  v-for="p in getAvailableProductOptions(row)"
                  :key="p.id"
                  :label="`${p.name} (${p.code})`"
                  :value="p.id!"
                />
              </el-select>
            </template>
            <template v-else>
              <span>{{ row.productName || '—' }} <span v-if="row.productCode" class="text-gray-400">({{ row.productCode }})</span></span>
            </template>
          </template>
        </template>
      </el-table-column>

      <!-- 批次号/缸号 -->
      <el-table-column label="批次/缸号" min-width="130">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.batchNo"
            placeholder="批次号/缸号"
            clearable
          />
          <span v-else>{{ row.batchNo || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 卷号 -->
      <el-table-column label="卷号" width="100">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.rollNo"
            placeholder="卷号"
            clearable
          />
          <span v-else>{{ row.rollNo || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 匹号 -->
      <el-table-column label="匹号" min-width="120">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.pieceNo"
            placeholder="匹号"
            clearable
          />
          <span v-else>{{ row.pieceNo || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 匹数 -->
      <el-table-column label="匹数" width="90">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.pieceCount"
            :min="1"
            :max="99999"
            :controls="false"
            class="!w-full"
          />
          <span v-else>{{ row.pieceCount ?? 1 }}</span>
        </template>
      </el-table-column>

      <!-- 数量（必填，>0） -->
      <el-table-column label="数量" width="120">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.quantity"
            :min="0.01"
            :precision="2"
            :controls="false"
            placeholder="数量"
            class="!w-full"
            @change="() => handleQtyOrPriceChange(row)"
          />
          <span v-else>{{ row.quantity != null ? Number(row.quantity).toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 单位（KG/M，纱线规格固定 KG） -->
      <el-table-column label="单位" width="100">
        <template #default="{ row }">
          <template v-if="row.itemType === 'YARN_SPEC'">
            <el-tag size="small" type="info">KG</el-tag>
          </template>
          <template v-else>
            <el-select
              v-if="!isReadOnly"
              v-model="row.unit"
              placeholder="单位"
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_STOCK_IN_UNIT)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
            <template v-else>
              <DictTag :type="DICT_TYPE.LUNAERP_STOCK_IN_UNIT" :value="row.unit" />
            </template>
          </template>
        </template>
      </el-table-column>

      <!-- 单价 -->
      <el-table-column label="单价" width="110">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.unitPrice"
            :min="0"
            :precision="2"
            :controls="false"
            placeholder="单价"
            class="!w-full"
            @change="() => handleQtyOrPriceChange(row)"
          />
          <span v-else>{{ row.unitPrice != null ? `¥ ${Number(row.unitPrice).toFixed(2)}` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 金额（数量*单价，服务端权威重算） -->
      <el-table-column label="金额" width="110" align="right">
        <template #default="{ row }">
          <span>{{ row.amount != null ? `¥ ${Number(row.amount).toFixed(2)}` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 库位分配去向（展示分配去向与打开分配弹层） -->
      <el-table-column label="库位分配去向" min-width="210">
        <template #default="{ row, $index }">
          <div class="space-y-1">
            <!-- 已有库位放置展示 -->
            <div v-if="row.placements && row.placements.length > 0" class="flex flex-col gap-1">
              <div
                v-for="(p, pIdx) in row.placements"
                :key="pIdx"
                class="text-12px flex items-center gap-1.5"
              >
                <el-tag size="small" type="info" class="font-mono">{{ p.locationCode || '库位' }}</el-tag>
                <span class="text-gray-700">
                  <span v-if="p.pieceCount != null" class="text-gray-900 font-medium">{{ p.pieceCount }}匹 / </span>
                  <span class="font-semibold text-gray-900">{{ p.quantity }}</span> {{ row.unit || 'KG' }}
                </span>
                <span v-if="p.remark" class="text-gray-400 text-11px">({{ p.remark }})</span>
              </div>
            </div>

            <!-- 操作按钮：草稿态"分配库位"/"修改分配"，只读态"查看详情"或未分配提示 -->
            <div class="pt-1">
              <template v-if="!isReadOnly">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="handleOpenPlacement(row, $index)"
                >
                  <Icon icon="ep:location" class="mr-1" />
                  {{ row.placements && row.placements.length > 0 ? `修改分配 (${row.placements.length}处)` : '分配库位' }}
                </el-button>
              </template>
              <template v-else>
                <el-button
                  v-if="row.placements && row.placements.length > 0"
                  link
                  type="primary"
                  size="small"
                  @click="handleOpenPlacement(row, $index)"
                >
                  查看详情 ({{ row.placements.length }}处)
                </el-button>
                <span v-else class="text-gray-400 text-12px">未分配</span>
              </template>
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- 备注 -->
      <el-table-column label="备注" min-width="130">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.remark"
            placeholder="备注"
            clearable
          />
          <span v-else>{{ row.remark || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column v-if="!isReadOnly" label="操作" width="70" align="center" fixed="right">
        <template #default="{ $index }">
          <el-button link type="danger" @click="handleDeleteRow($index)">
            <Icon icon="ep:delete" />
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无明细数据（草稿允许先保存单头后补明细）" :image-size="80" />
      </template>
    </el-table>

    <!-- 逐匹库位分配弹层 -->
    <PlacementDialog
      v-model="placementDialogVisible"
      :warehouse-id="warehouseId"
      :warehouse-name="warehouseName"
      :item="currentPlacementItem"
      :is-read-only="isReadOnly"
      @confirm="handlePlacementConfirm"
    />
  </div>
</template>

<script lang="ts" setup>
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { StockInItemVO, StockInItemPlacementVO } from '@/api/lunaerp-elmui/stock/stock-in'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import { getYarnSpecCascadeOptions, type YarnSpecCascadeOptionVO } from '@/api/lunaerp-elmui/production/yarn-master'
import type { CascaderOption } from 'element-plus'
import PlacementDialog from './PlacementDialog.vue'

defineOptions({ name: 'StockInItemsTab' })

const props = defineProps<{
  items?: StockInItemVO[]
  warehouseId?: number
  warehouseName?: string
  isReadOnly: boolean
}>()

const emit = defineEmits<{
  (e: 'update:items', val: StockInItemVO[]): void
  (e: 'recalculate'): void
}>()

const message = useMessage()

const itemsList = computed({
  get: () => props.items || [],
  set: (val) => emit('update:items', val)
})

// 纱线规格级联选项
const rawCascadeOptions = ref<YarnSpecCascadeOptionVO[]>([])
const cascaderOptions = computed<CascaderOption[]>(() => {
  return rawCascadeOptions.value.map((type) => ({
    value: type.yarnTypeId,
    label: type.yarnTypeName,
    children: (type.specs || []).map((spec) => ({
      value: spec.id,
      label: spec.specName,
      rawSpec: spec,
      typeId: type.yarnTypeId,
      typeName: type.yarnTypeName
    }))
  }))
})

// 加载纱线规格级联选项
const loadYarnSpecOptions = async () => {
  try {
    const res = await getYarnSpecCascadeOptions()
    rawCascadeOptions.value = res || []
    initCascadeValues()
  } catch (e) {
    console.error('加载纱线规格级联选项失败', e)
  }
}

// 物品类型切换事件
const handleItemTypeChange = (row: StockInItemVO, newType: string) => {
  if (newType === 'YARN_SPEC') {
    row.productId = undefined
    row.productCode = undefined
    row.productName = undefined
    row.unit = 'KG'
  } else {
    row.yarnSpecId = undefined
    row.yarnSpecName = undefined
    row.specCascadeValue = undefined
  }
}

// 纱线规格级联选择事件
const handleSpecCascadeChange = (row: StockInItemVO, val: any) => {
  if (!val || val.length < 2) {
    row.yarnSpecId = undefined
    row.yarnSpecName = undefined
    return
  }
  const [typeId, specId] = val as [number, number]
  row.yarnSpecId = specId

  const typeOpt = rawCascadeOptions.value.find((t) => t.yarnTypeId === typeId)
  if (typeOpt) {
    const specOpt = (typeOpt.specs || []).find((s) => s.id === specId)
    if (specOpt) {
      row.yarnSpecName = specOpt.specName
      if ((row.unitPrice === undefined || row.unitPrice === null) && specOpt.refPrice != null) {
        row.unitPrice = specOpt.refPrice
        handleQtyOrPriceChange(row)
      }
    }
  }
}

// 为已有 YARN_SPEC 明细回填 specCascadeValue
const initCascadeValues = () => {
  if (!props.items || !rawCascadeOptions.value.length) return
  for (const item of props.items) {
    if (item.itemType === 'YARN_SPEC' && item.yarnSpecId && !item.specCascadeValue) {
      for (const t of rawCascadeOptions.value) {
        if (t.specs?.some((s) => s.id === item.yarnSpecId)) {
          item.specCascadeValue = [t.yarnTypeId, item.yarnSpecId]
          break
        }
      }
    }
  }
}

watch(
  [() => props.items, rawCascadeOptions],
  () => {
    initCascadeValues()
  },
  { deep: true, immediate: true }
)

// 产品远程搜索
const productOptions = ref<ProductVO[]>([])
const productLoading = ref(false)
const searchProducts = async (query: string) => {
  productLoading.value = true
  try {
    const res = await ProductApi.getProductPage({
      pageNo: 1,
      pageSize: 20,
      search: query || ''
    })
    productOptions.value = res.list
  } finally {
    productLoading.value = false
  }
}

// 确保每行已有产品数据能正常展示在下拉中
const getAvailableProductOptions = (row: StockInItemVO) => {
  if (row.productId && !productOptions.value.some((p) => p.id === row.productId)) {
    return [
      ...productOptions.value,
      {
        id: row.productId,
        code: row.productCode || '',
        name: row.productName || ''
      } as any
    ]
  }
  return productOptions.value
}

const handleProductChange = (row: StockInItemVO, productId: number) => {
  const p = productOptions.value.find((item) => item.id === productId)
  if (p) {
    row.productCode = p.code
    row.productName = p.name
  }
}

const handleQtyOrPriceChange = (row: StockInItemVO) => {
  if (row.quantity != null && row.unitPrice != null) {
    row.amount = Number((row.quantity * row.unitPrice).toFixed(2))
  } else {
    row.amount = undefined
  }
  emit('recalculate')
}

const handleAddRow = () => {
  const newList = [...itemsList.value]
  newList.push({
    itemType: 'PRODUCT',
    productId: undefined,
    productCode: '',
    productName: '',
    yarnSpecId: undefined,
    yarnSpecName: '',
    specCascadeValue: undefined,
    batchNo: '',
    rollNo: '',
    pieceCount: 1,
    quantity: undefined as any,
    unit: 'KG',
    unitPrice: undefined,
    amount: undefined,
    remark: '',
    placements: []
  })
  emit('update:items', newList)
  emit('recalculate')
}

const handleDeleteRow = (index: number) => {
  const newList = [...itemsList.value]
  newList.splice(index, 1)
  emit('update:items', newList)
  emit('recalculate')
}

// 库位分配弹层控制
const placementDialogVisible = ref(false)
const currentPlacementItem = ref<StockInItemVO>()
const currentPlacementIndex = ref<number>(-1)

const handleOpenPlacement = (row: StockInItemVO, index: number) => {
  if (!props.warehouseId && !props.isReadOnly) {
    message.warning('请先在【基本信息】选择入库仓库后再分配库位')
    return
  }
  currentPlacementItem.value = row
  currentPlacementIndex.value = index
  placementDialogVisible.value = true
}

const handlePlacementConfirm = (placements: StockInItemPlacementVO[]) => {
  if (currentPlacementIndex.value >= 0 && currentPlacementIndex.value < itemsList.value.length) {
    itemsList.value[currentPlacementIndex.value].placements = placements
    emit('update:items', [...itemsList.value])
  }
}

onMounted(() => {
  searchProducts('')
  loadYarnSpecOptions()
})
</script>
