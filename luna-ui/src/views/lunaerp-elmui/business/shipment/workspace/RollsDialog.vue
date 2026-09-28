<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="760px"
    destroy-on-close
    @closed="handleClosed"
  >
    <!-- 软提示：已录卷重 / 行数量（不做齐套强制） -->
    <div class="mb-12px text-13px text-gray-600 bg-blue-50 px-12px py-8px rounded">
      已录卷重 <span class="font-bold text-blue-700">{{ totalRecordedWeight.toFixed(2) }} KG</span>
      <span class="mx-6px text-gray-400">/</span>
      行数量 <span class="font-bold text-blue-700">{{ rollRows.length }} 条</span>
      <span v-if="!isReadOnly" class="ml-12px text-gray-400 text-12px">（软提示仅供参考，不做齐套强制）</span>
    </div>

    <el-table :data="rollRows" :row-key="getRollRowKey" border stripe class="w-full">
      <el-table-column label="#" type="index" width="50" align="center" />

      <!-- 卷号 -->
      <el-table-column label="卷号" min-width="140">
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

      <!-- 重量 KG -->
      <el-table-column label="实称重量 KG" width="140">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.weight"
            :min="0"
            :precision="2"
            :step="0.5"
            controls-position="right"
            class="!w-full"
          />
          <span v-else>{{ row.weight != null ? `${Number(row.weight).toFixed(2)} KG` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 米数 -->
      <el-table-column label="米数" width="120">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.lengthM"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            class="!w-full"
          />
          <span v-else>{{ row.lengthM != null ? `${Number(row.lengthM).toFixed(2)} m` : '—' }}</span>
        </template>
      </el-table-column>

      <!-- 备注 -->
      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">
          <el-input
            v-if="!isReadOnly"
            v-model="row.remark"
            placeholder="卷备注"
            clearable
          />
          <span v-else>{{ row.remark || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column v-if="!isReadOnly" label="操作" width="70" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="handleDeleteRow(row)">
            删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty
          :description="isReadOnly ? '该明细行暂无卷清单记录' : '点击下方按钮添加卷记录'"
          :image-size="60"
        />
      </template>
    </el-table>

    <!-- 添加行按钮 -->
    <div v-if="!isReadOnly" class="mt-10px">
      <el-button type="primary" plain size="small" @click="handleAddRow">
        <Icon icon="ep:plus" class="mr-2px" /> 添加卷
      </el-button>
    </div>

    <template #footer>
      <el-button @click="handleCancel">{{ isReadOnly ? '关闭' : '取消' }}</el-button>
      <el-button v-if="!isReadOnly" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import type { ShipmentRollVO } from '@/api/lunaerp-elmui/business/shipment'

const props = defineProps<{
  modelValue: boolean
  /** 当前编辑行的产品名称（用于弹窗标题） */
  itemLabel?: string
  /** 传入的卷清单数据（编辑时使用副本） */
  rolls: ShipmentRollVO[]
  isReadOnly: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'confirm', rolls: ShipmentRollVO[]): void
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = computed(() =>
  props.itemLabel
    ? `卷清单 — ${props.itemLabel}`
    : '卷清单'
)

/** 弹窗内操作的副本 */
const rollRows = ref<ShipmentRollVO[]>([])

let tempRollKeyCounter = 0
/** 卷清单表格以行 id 为稳定 key；新增行未落库前以临时 key 保证行唯一性 */
const getRollRowKey = (row: ShipmentRollVO): string => {
  if (row.id != null) {
    return String(row.id)
  }
  if (!(row as any)._clientKey) {
    (row as any)._clientKey = `temp_roll_${++tempRollKeyCounter}`
  }
  return String((row as any)._clientKey)
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      // 打开时深拷贝，保留已有 id、shipmentItemId 等，避免直接改父组件引用
      rollRows.value = props.rolls.map((r) => ({
        ...r,
        _clientKey: (r as any)._clientKey || (r.id != null ? undefined : `temp_roll_${++tempRollKeyCounter}`)
      }))
    }
  }
)

/** 已录卷重合计 */
const totalRecordedWeight = computed(() =>
  rollRows.value.reduce((acc, r) => acc + (r.weight ?? 0), 0)
)

const handleAddRow = () => {
  rollRows.value.push({
    id: undefined,
    shipmentItemId: undefined,
    rollNo: '',
    weight: undefined,
    lengthM: undefined,
    remark: '',
    _clientKey: `temp_roll_${++tempRollKeyCounter}`
  } as any)
}

const handleDeleteRow = (row: ShipmentRollVO) => {
  const targetKey = getRollRowKey(row)
  rollRows.value = rollRows.value.filter((r) => getRollRowKey(r) !== targetKey)
}

const handleConfirm = () => {
  emit('confirm', rollRows.value.map((r) => ({ ...r })))
  emit('update:modelValue', false)
}

const handleCancel = () => {
  emit('update:modelValue', false)
}

const handleClosed = () => {
  rollRows.value = []
}
</script>
