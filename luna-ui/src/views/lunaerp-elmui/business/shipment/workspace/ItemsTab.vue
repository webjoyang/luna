<template>
  <div class="py-16px">
    <div class="mb-12px flex justify-between items-center">
      <div class="text-14px font-medium text-gray-700">
        发货明细列表（共 {{ itemsList.length }} 条明细）
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

      <!-- 产品档案选择 -->
      <el-table-column label="产品（必填）" min-width="220">
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

      <!-- 客户色号 -->
      <el-table-column label="客户色号" min-width="120">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.customerColorNo"
            placeholder="客户色号"
            clearable
          />
          <span v-else>{{ row.customerColorNo || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 颜色名称 -->
      <el-table-column label="颜色" min-width="100">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.colorName"
            placeholder="颜色名称"
            clearable
          />
          <span v-else>{{ row.colorName || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 缸号/批次号 -->
      <el-table-column label="缸号/批次" min-width="120">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.vatNo"
            placeholder="缸号/批次号"
            clearable
          />
          <span v-else>{{ row.vatNo || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 发货数量 -->
      <el-table-column label="发货数量（必填）" width="130">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.quantity"
            :min="0.01"
            :precision="2"
            :step="1"
            controls-position="right"
            class="!w-full"
            @change="handleAmountRecalculate(row)"
          />
          <span v-else>{{ row.quantity != null ? row.quantity : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 匹数 -->
      <el-table-column label="匹数" width="100">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.pieces"
            :min="1"
            :precision="0"
            :step="1"
            controls-position="right"
            class="!w-full"
            @change="handlePiecesChange"
          />
          <span v-else>{{ row.pieces != null ? `${row.pieces} 匹` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 单位 -->
      <el-table-column label="单位" width="90">
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
          <span v-else>{{ row.unit || '—' }}</span>
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
            :step="0.5"
            controls-position="right"
            class="!w-full"
            @change="handleAmountRecalculate(row)"
          />
          <span v-else>{{ row.unitPrice != null ? `¥ ${Number(row.unitPrice).toFixed(2)}` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 金额（服务端权威重算） -->
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          {{ row.amount != null ? `¥ ${Number(row.amount).toFixed(2)}` : '¥ 0.00' }}
        </template>
      </el-table-column>

      <!-- 来源入库明细ID -->
      <el-table-column label="来源入库明细" min-width="120">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model.number="row.sourceStockInItemId"
            placeholder="入库明细ID"
            clearable
          />
          <span v-else>{{ row.sourceStockInItemId || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 备注 -->
      <el-table-column label="备注" min-width="120">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.remark"
            placeholder="明细备注"
            clearable
          />
          <span v-else>{{ row.remark || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row, $index }">
          <el-button
            link
            :type="isReadOnly ? 'info' : 'primary'"
            size="small"
            @click="handleOpenRolls(row)"
          >
            {{ isReadOnly ? '查看卷清单' : '卷清单' }}<span v-if="row.rolls && row.rolls.length">({{ row.rolls.length }})</span>
          </el-button>
          <el-button
            v-if="!isReadOnly"
            link
            type="danger"
            size="small"
            @click="handleDeleteRow($index)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无发货明细，点击上方'添加明细'按钮录入" :image-size="70" />
      </template>
    </el-table>

    <!-- 底部合计汇总栏 -->
    <div class="mt-16px p-12px bg-gray-50 rounded flex justify-end gap-6 text-14px">
      <div>
        <span class="text-gray-500">总匹数合计：</span>
        <span class="font-bold text-gray-800">{{ summaryPieces }} 匹</span>
      </div>
      <div>
        <span class="text-gray-500">货物金额合计：</span>
        <span class="font-bold text-blue-600 text-16px">¥ {{ summaryAmount.toFixed(2) }}</span>
      </div>
    </div>

    <!-- 卷清单弹窗 -->
    <RollsDialog
      v-model="rollsDialogVisible"
      :item-label="currentRollItemLabel"
      :rolls="currentRollItemRolls"
      :is-read-only="isReadOnly"
      @confirm="handleRollsConfirm"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { ShipmentItemVO, ShipmentRollVO } from '@/api/lunaerp-elmui/business/shipment'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import RollsDialog from './RollsDialog.vue'

const props = defineProps<{
  items?: ShipmentItemVO[]
  isReadOnly: boolean
}>()

const emit = defineEmits<{
  (e: 'update:items', val: ShipmentItemVO[]): void
  (e: 'recalculate'): void
}>()

const itemsList = computed<ShipmentItemVO[]>({
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

const getAvailableProductOptions = (row: ShipmentItemVO): ProductVO[] => {
  if (row.productId && !productOptions.value.some((p) => p.id === row.productId)) {
    return [
      {
        id: row.productId,
        name: row.productName || '已选产品',
        code: row.productCode || '',
        type: '',
        composition: '',
        widthCm: 0,
        weightGsm: 0
      } as ProductVO,
      ...productOptions.value
    ]
  }
  return productOptions.value
}

const handleProductChange = (row: ShipmentItemVO, productId: number) => {
  const selected = productOptions.value.find((p) => p.id === productId)
  if (selected) {
    row.productCode = selected.code
    row.productName = selected.name
  }
}

const handleAmountRecalculate = (row: ShipmentItemVO) => {
  if (row.quantity != null && row.unitPrice != null) {
    row.amount = Math.round(row.quantity * row.unitPrice * 100) / 100
  } else {
    row.amount = 0
  }
  emit('recalculate')
}

const handlePiecesChange = () => {
  emit('recalculate')
}

const handleAddRow = () => {
  const newRow: ShipmentItemVO = {
    productId: undefined as any,
    productCode: '',
    productName: '',
    customerColorNo: '',
    colorName: '',
    vatNo: '',
    quantity: 1,
    pieces: 1,
    unit: 'KG',
    unitPrice: 0,
    amount: 0,
    remark: '',
    rolls: []
  }
  const next = [...itemsList.value, newRow]
  emit('update:items', next)
  emit('recalculate')
}

const handleDeleteRow = (index: number) => {
  const next = [...itemsList.value]
  next.splice(index, 1)
  emit('update:items', next)
  emit('recalculate')
}

// ---- 卷清单弹窗逻辑 ----
const rollsDialogVisible = ref(false)
/** 当前打开的明细行索引（-1 表示未打开） */
const currentRollItemIndex = ref(-1)
/** 弹窗标题用的产品标签 */
const currentRollItemLabel = computed(() => {
  const idx = currentRollItemIndex.value
  if (idx < 0 || idx >= itemsList.value.length) return ''
  const item = itemsList.value[idx]
  return item.productName
    ? `${item.productName}${item.vatNo ? ' / 缸 ' + item.vatNo : ''}`
    : ''
})
/** 传入弹窗的卷数据（只读时为 ref 快照，可写时为空数组后由弹窗 watch 填充） */
const currentRollItemRolls = computed<ShipmentRollVO[]>(() => {
  const idx = currentRollItemIndex.value
  if (idx < 0 || idx >= itemsList.value.length) return []
  return itemsList.value[idx].rolls ?? []
})

const handleOpenRolls = (row: ShipmentItemVO) => {
  const idx = itemsList.value.indexOf(row)
  if (idx < 0) return
  if (!row.rolls) {
    row.rolls = []
  }
  currentRollItemIndex.value = idx
  rollsDialogVisible.value = true
}

const handleRollsConfirm = (rolls: ShipmentRollVO[]) => {
  const idx = currentRollItemIndex.value
  if (idx < 0 || idx >= itemsList.value.length) return
  const next = [...itemsList.value]
  next[idx] = { ...next[idx], rolls }
  emit('update:items', next)
}

// ---- 汇总计算 ----
const summaryPieces = computed(() => {
  return itemsList.value.reduce((acc, cur) => acc + (cur.pieces || 0), 0)
})

const summaryAmount = computed(() => {
  return itemsList.value.reduce((acc, cur) => acc + (cur.amount || 0), 0)
})

onMounted(() => {
  searchProducts('')
})
</script>
