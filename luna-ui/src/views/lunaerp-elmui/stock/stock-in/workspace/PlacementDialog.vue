<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isReadOnly ? '查看库位分配去向' : '分配库位'"
    width="850px"
    destroy-on-close
    append-to-body
  >
    <div class="space-y-4">
      <!-- 明细基本信息与分配对比看板 -->
      <div class="bg-gray-50 p-4 rounded border border-gray-200">
        <div class="grid grid-cols-2 gap-4 text-13px text-gray-700 mb-3">
          <div>
            <span class="text-gray-500">物品：</span>
            <span v-if="item?.itemType === 'YARN_SPEC'" class="font-medium">{{ item?.yarnSpecName || '—' }}</span>
            <span v-else class="font-medium">{{ item?.productName || '—' }}</span>
            <span v-if="item?.itemType !== 'YARN_SPEC' && item?.productCode" class="text-gray-400 ml-1">({{ item.productCode }})</span>
          </div>
          <div>
            <span class="text-gray-500">入库仓库：</span>
            <span class="font-medium">{{ warehouseName || '当前仓库' }}</span>
          </div>
          <div>
            <span class="text-gray-500">批次/缸号：</span>
            <span>{{ item?.batchNo || '—' }}</span>
            <span v-if="item?.rollNo" class="ml-2 text-gray-500">卷号：{{ item.rollNo }}</span>
          </div>
          <div>
            <span class="text-gray-500">明细总量：</span>
            <span class="font-bold text-gray-900">{{ targetPieces }} 匹</span> /
            <span class="font-bold text-gray-900">{{ targetQuantity }} {{ targetUnit }}</span>
          </div>
        </div>

        <!-- 对比提示条 -->
        <div class="flex items-center justify-between bg-white px-3 py-2 rounded border border-gray-200">
          <div class="flex items-center gap-4 text-13px">
            <div>
              <span class="text-gray-500">已分配匹数：</span>
              <span :class="piecesMatched ? 'text-green-600 font-bold' : 'text-orange-600 font-bold'">
                {{ totalAssignedPieces }}
              </span>
              <span class="text-gray-400"> / 共 {{ targetPieces }} 匹</span>
            </div>
            <div class="h-12px w-1px bg-gray-300"></div>
            <div>
              <span class="text-gray-500">已分配数量：</span>
              <span :class="quantityMatched ? 'text-green-600 font-bold' : 'text-orange-600 font-bold'">
                {{ totalAssignedQuantity.toFixed(2) }}
              </span>
              <span class="text-gray-400"> / 共 {{ targetQuantity.toFixed(2) }} {{ targetUnit }}</span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <el-tag v-if="isFullyAllocated" type="success" size="small">齐套</el-tag>
            <el-tag v-else-if="totalAssignedPieces > 0 || totalAssignedQuantity > 0" type="warning" size="small">部分分配</el-tag>
            <el-tag v-else type="info" size="small">未分配</el-tag>
            <span class="text-11px text-gray-400">（允许部分上架）</span>
          </div>
        </div>
      </div>

      <!-- 放置子表标题与新增按钮 -->
      <div class="flex justify-between items-center">
        <div class="text-14px font-medium text-gray-800">
          放置记录明细（{{ placementsList.length }} 处）
        </div>
        <el-button
          v-if="!isReadOnly"
          type="primary"
          plain
          size="small"
          @click="handleAddPlacement"
        >
          <Icon icon="ep:plus" class="mr-1" /> 添加库位
        </el-button>
      </div>

      <!-- 放置记录子表 -->
      <el-table :data="placementsList" border stripe size="small" class="w-full">
        <el-table-column label="#" type="index" width="50" align="center" />

        <!-- 库位选择（限本单仓库启用状态的库位） -->
        <el-table-column label="库位" min-width="200">
          <template #default="{ row }">
            <template v-if="!isReadOnly">
              <el-select
                v-model="row.locationId"
                placeholder="请选择库位"
                filterable
                class="w-full"
                @change="(val: number) => handleLocationChange(row, val)"
              >
                <el-option
                  v-for="loc in locationOptions"
                  :key="loc.id"
                  :label="`${loc.code}${loc.name ? ' (' + loc.name + ')' : ''}`"
                  :value="loc.id!"
                />
              </el-select>
            </template>
            <template v-else>
              <el-tag size="small">{{ row.locationCode || '—' }}</el-tag>
            </template>
          </template>
        </el-table-column>

        <!-- 匹数 -->
        <el-table-column label="匹数" width="110">
          <template #default="{ row }">
            <el-input-number
              v-if="!isReadOnly"
              v-model="row.pieceCount"
              :min="1"
              :max="99999"
              :controls="false"
              placeholder="匹数"
              class="!w-full"
            />
            <span v-else>{{ row.pieceCount != null ? `${row.pieceCount} 匹` : '—' }}</span>
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
            />
            <span v-else>{{ row.quantity != null ? Number(row.quantity).toFixed(2) : '—' }}</span>
          </template>
        </el-table-column>

        <!-- 单位 -->
        <el-table-column label="单位" width="70" align="center">
          <template #default>
            <span>{{ targetUnit }}</span>
          </template>
        </el-table-column>

        <!-- 备注 -->
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <el-input
              v-if="!isReadOnly"
              v-model="row.remark"
              placeholder="备注（可选）"
              clearable
              maxlength="200"
            />
            <span v-else>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column v-if="!isReadOnly" label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="handleDeletePlacement($index)">
              <Icon icon="ep:delete" />
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无库位分配（未分配将默认入库到仓库头，库位留空）" :image-size="60" />
        </template>
      </el-table>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2">
        <template v-if="!isReadOnly">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirm">确定</el-button>
        </template>
        <template v-else>
          <el-button @click="dialogVisible = false">关闭</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { useMessage } from '@/hooks/web/useMessage'
import type { StockInItemVO, StockInItemPlacementVO } from '@/api/lunaerp-elmui/stock/stock-in'
import * as WarehouseLocationApi from '@/api/lunaerp-elmui/stock/warehouse-location'
import type { WarehouseLocationVO } from '@/api/lunaerp-elmui/stock/warehouse-location'

defineOptions({ name: 'StockInPlacementDialog' })

const props = defineProps<{
  modelValue: boolean
  warehouseId?: number
  warehouseName?: string
  item?: StockInItemVO
  isReadOnly?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm', placements: StockInItemPlacementVO[]): void
}>()

const message = useMessage()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 库位选项列表
const locationOptions = ref<WarehouseLocationVO[]>([])

// 放置列表本地副本
const placementsList = ref<StockInItemPlacementVO[]>([])

// 明细目标指标
const targetPieces = computed(() => props.item?.pieceCount ?? 1)
const targetQuantity = computed(() => Number(props.item?.quantity ?? 0))
const targetUnit = computed(() => props.item?.unit || 'KG')

// 已分配统计
const totalAssignedPieces = computed(() => {
  return placementsList.value.reduce((sum, p) => sum + (Number(p.pieceCount) || 0), 0)
})

const totalAssignedQuantity = computed(() => {
  return placementsList.value.reduce((sum, p) => sum + (Number(p.quantity) || 0), 0)
})

const piecesMatched = computed(() => totalAssignedPieces.value === targetPieces.value)
const quantityMatched = computed(() => Math.abs(totalAssignedQuantity.value - targetQuantity.value) < 0.001)
const isFullyAllocated = computed(() => piecesMatched.value && quantityMatched.value)

/** 加载本仓库下的启用库位 */
const loadLocations = async (warehouseId: number) => {
  try {
    locationOptions.value = await WarehouseLocationApi.getWarehouseLocationListByWarehouse(warehouseId, 'ACTIVE')
  } catch {
    locationOptions.value = []
  }
}

/** 库位变更回填编码 */
const handleLocationChange = (row: StockInItemPlacementVO, locationId: number) => {
  const loc = locationOptions.value.find((l) => l.id === locationId)
  if (loc) {
    row.locationCode = loc.code
  }
}

/** 添加放置行 */
const handleAddPlacement = () => {
  // 智能建议默认匹数与数量：若剩余未分配匹数/数量大于0，直接带入剩余
  const remainingPieces = Math.max(0, targetPieces.value - totalAssignedPieces.value)
  const remainingQty = Math.max(0, Number((targetQuantity.value - totalAssignedQuantity.value).toFixed(2)))

  placementsList.value.push({
    locationId: undefined,
    locationCode: '',
    pieceCount: remainingPieces > 0 ? remainingPieces : 1,
    quantity: remainingQty > 0 ? remainingQty : undefined,
    unit: targetUnit.value,
    remark: ''
  })
}

/** 删除放置行 */
const handleDeletePlacement = (index: number) => {
  placementsList.value.splice(index, 1)
}

/** 确认保存 */
const handleConfirm = () => {
  // 校验每行必填
  for (let i = 0; i < placementsList.value.length; i++) {
    const p = placementsList.value[i]
    if (!p.locationId) {
      message.warning(`第 ${i + 1} 行放置记录未选择库位`)
      return
    }
    if (p.quantity == null || p.quantity <= 0) {
      message.warning(`第 ${i + 1} 行放置数量必须大于0`)
      return
    }
    // 回填快照
    const loc = locationOptions.value.find((l) => l.id === p.locationId)
    if (loc) {
      p.locationCode = loc.code
    }
    p.unit = targetUnit.value
  }

  // 深拷贝返回
  emit('confirm', JSON.parse(JSON.stringify(placementsList.value)))
  dialogVisible.value = false
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      if (props.warehouseId) {
        await loadLocations(props.warehouseId)
      } else {
        locationOptions.value = []
      }

      // 深拷贝初始化本地副本
      if (props.item?.placements && props.item.placements.length > 0) {
        placementsList.value = JSON.parse(JSON.stringify(props.item.placements))
      } else {
        placementsList.value = []
      }
    }
  }
)
</script>
