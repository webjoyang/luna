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

      <!-- 产品选择（必选） -->
      <el-table-column label="产品" min-width="220">
        <template #default="{ row }">
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
      </el-table-column>

      <!-- 批次号/缸号 -->
      <el-table-column label="批次/缸号" min-width="140">
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
      <el-table-column label="卷号" width="110">
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

      <!-- 匹数 -->
      <el-table-column label="匹数" width="100">
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
      <el-table-column label="数量" width="130">
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

      <!-- 单位（KG/M） -->
      <el-table-column label="单位" width="110">
        <template #default="{ row }">
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
      </el-table-column>

      <!-- 单价 -->
      <el-table-column label="单价" width="120">
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
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span>{{ row.amount != null ? `¥ ${Number(row.amount).toFixed(2)}` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 备注 -->
      <el-table-column label="备注" min-width="140">
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
      <el-table-column v-if="!isReadOnly" label="操作" width="80" align="center" fixed="right">
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
  </div>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { StockInItemVO } from '@/api/lunaerp-elmui/stock/stock-in'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'

defineOptions({ name: 'StockInItemsTab' })

const props = defineProps<{
  items?: StockInItemVO[]
  isReadOnly: boolean
}>()

const emit = defineEmits<{
  (e: 'update:items', val: StockInItemVO[]): void
  (e: 'recalculate'): void
}>()

const itemsList = computed({
  get: () => props.items || [],
  set: (val) => emit('update:items', val)
})

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
    productId: undefined as any,
    productCode: '',
    productName: '',
    batchNo: '',
    rollNo: '',
    pieceCount: 1,
    quantity: undefined as any,
    unit: 'KG',
    unitPrice: undefined,
    amount: undefined,
    remark: ''
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

onMounted(() => {
  searchProducts('')
})
</script>
