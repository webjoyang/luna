<template>
  <el-dialog
    v-model="visible"
    title="到货质检登记"
    width="900px"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading">
      <!-- 单据头基本信息摘要 -->
      <div class="bg-gray-50 p-12px rounded mb-15px text-13px text-gray-700">
        <el-row :gutter="16">
          <el-col :span="6">
            <span class="text-gray-400">退货单号：</span>
            <span class="font-bold font-mono">{{ returnOrder?.returnNo }}</span>
          </el-col>
          <el-col :span="6">
            <span class="text-gray-400">客户名称：</span>
            <span>{{ returnOrder?.customerName }}</span>
          </el-col>
          <el-col :span="6">
            <span class="text-gray-400">退货仓库：</span>
            <span class="font-bold text-primary">{{ returnOrder?.warehouseName }}</span>
          </el-col>
          <el-col :span="6">
            <span class="text-gray-400">总匹数：</span>
            <span class="font-bold">{{ qcRolls.length }} 卷</span>
          </el-col>
        </el-row>
      </div>

      <!-- 快捷批量操作工具条 -->
      <div class="flex items-center justify-between mb-12px">
        <div class="flex items-center gap-10px">
          <span class="text-13px font-bold text-gray-700">快捷操作：</span>
          <el-button size="small" type="success" plain @click="handleBatchSetQcResult('RESTOCK')">
            <Icon icon="ep:check" class="mr-2px" /> 全部可二次入库
          </el-button>
          <el-button size="small" type="danger" plain @click="handleBatchSetQcResult('SCRAP')">
            <Icon icon="ep:close" class="mr-2px" /> 全部废品
          </el-button>
        </div>
        <div class="flex items-center gap-8px">
          <span class="text-12px text-gray-500">批量赋库位：</span>
          <el-select
            v-model="batchLocationId"
            placeholder="选择目标库位"
            size="small"
            clearable
            class="!w-160px"
          >
            <el-option
              v-for="loc in locationOptions"
              :key="loc.id"
              :label="`${loc.code} (${loc.name || '库位'})`"
              :value="loc.id!"
            />
          </el-select>
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="!batchLocationId"
            @click="handleApplyBatchLocation"
          >
            应用到可入库卷
          </el-button>
        </div>
      </div>

      <!-- 质检卷清单表格 -->
      <el-table :data="qcRolls" border stripe max-height="420px" size="small">
        <el-table-column label="序号" type="index" width="50" align="center" />
        <el-table-column label="卷号" prop="rollNo" width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-bold font-mono">{{ row.rollNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="品类产品" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.productName }}</div>
            <div class="text-11px text-gray-400 font-mono">{{ row.productCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="色号 / 缸号" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.customerColorNo || row.colorName || '-' }}</div>
            <div class="text-11px text-gray-400">缸: {{ row.vatNo || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="实称数量" width="100" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold">{{ row.quantity?.toFixed(2) }}</span> {{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column label="质检判定" width="140" align="center">
          <template #default="{ row }">
            <el-select
              v-model="row.qcResult"
              placeholder="请选择判定"
              size="small"
              class="w-full"
              @change="handleQcResultChange(row)"
            >
              <el-option label="可二次入库" value="RESTOCK" />
              <el-option label="废品" value="SCRAP" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="目标库位" width="160" align="center">
          <template #default="{ row }">
            <el-select
              v-model="row.locationId"
              placeholder="先入账后上架（可空）"
              size="small"
              clearable
              class="w-full"
              :disabled="row.qcResult === 'SCRAP'"
            >
              <el-option
                v-for="loc in locationOptions"
                :key="loc.id"
                :label="`${loc.code} (${loc.name || '库位'})`"
                :value="loc.id!"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="质检处置备注" min-width="140">
          <template #default="{ row }">
            <el-input
              v-model="row.qcRemark"
              placeholder="处置说明（可选）"
              size="small"
              clearable
            />
          </template>
        </el-table-column>
      </el-table>

      <!-- 质检结果汇总统计栏 -->
      <div
        class="mt-15px p-12px bg-blue-50 border border-blue-100 rounded flex items-center justify-between text-13px"
      >
        <div class="flex items-center gap-20px">
          <div>
            可二次入库：
            <span class="font-bold text-success font-mono">{{ restockCount }} 卷</span>
            （量：<span class="font-bold text-success font-mono">{{ restockQtySummary }}</span
            >）
          </div>
          <div>
            废品：
            <span class="font-bold text-danger font-mono">{{ scrapCount }} 卷</span>
            （量：<span class="font-bold text-danger font-mono">{{ scrapQtySummary }}</span
            >）
          </div>
          <div v-if="uninspectedCount > 0" class="text-orange-600 font-bold">
            待判定：<span class="font-mono">{{ uninspectedCount }} 卷</span>
          </div>
        </div>
        <div
          v-if="uninspectedCount === 0 && restockCount === 0 && qcRolls.length > 0"
          class="text-danger font-bold"
        >
          【全废分支】入库单将自动取消，不记库存台账
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-10px">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="['lunaerp:business:sales-return:receive']"
        >
          确认到货质检
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSalesReturn,
  receiveSalesReturn,
  type SalesReturnVO
} from '@/api/lunaerp-elmui/business/sales-return'
import {
  getWarehouseLocationListByWarehouse,
  type WarehouseLocationVO
} from '@/api/lunaerp-elmui/stock/warehouse-location'
import {
  validateQcDecision,
  calculateQcRestockAndScrapQty,
  type QcRollItem
} from '../model/salesReturn'

interface DisplayQcRoll extends QcRollItem {
  returnItemId: number
  productName: string
  productCode: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  unit: string
  quantity: number
}

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const returnOrder = ref<SalesReturnVO | null>(null)
const qcRolls = ref<DisplayQcRoll[]>([])
const locationOptions = ref<WarehouseLocationVO[]>([])
const batchLocationId = ref<number | undefined>(undefined)

const restockCount = computed(() => qcRolls.value.filter((r) => r.qcResult === 'RESTOCK').length)
const scrapCount = computed(() => qcRolls.value.filter((r) => r.qcResult === 'SCRAP').length)
const uninspectedCount = computed(() => qcRolls.value.filter((r) => !r.qcResult).length)

// 按退货行单位分别汇总可二次入库与废品数量，杜绝米数与公斤混加
const restockQtySummary = computed(() => {
  if (!returnOrder.value?.items) return '0.00'
  const summaries: string[] = []
  for (const item of returnOrder.value.items) {
    const itemRolls = qcRolls.value.filter((r) => r.returnItemId === item.id)
    const { restockQty } = calculateQcRestockAndScrapQty(item.unit, itemRolls)
    if (restockQty > 0) {
      summaries.push(`${restockQty.toFixed(2)} ${item.unit || 'KG'}`)
    }
  }
  return summaries.length > 0 ? summaries.join(' + ') : '0.00'
})

const scrapQtySummary = computed(() => {
  if (!returnOrder.value?.items) return '0.00'
  const summaries: string[] = []
  for (const item of returnOrder.value.items) {
    const itemRolls = qcRolls.value.filter((r) => r.returnItemId === item.id)
    const { scrapQty } = calculateQcRestockAndScrapQty(item.unit, itemRolls)
    if (scrapQty > 0) {
      summaries.push(`${scrapQty.toFixed(2)} ${item.unit || 'KG'}`)
    }
  }
  return summaries.length > 0 ? summaries.join(' + ') : '0.00'
})

const open = async (returnId: number) => {
  visible.value = true
  loading.value = true
  batchLocationId.value = undefined
  try {
    const res = await getSalesReturn(returnId)
    returnOrder.value = res

    // 加载退货仓库启用的库位
    if (res.warehouseId) {
      locationOptions.value =
        (await getWarehouseLocationListByWarehouse(res.warehouseId, 'ACTIVE')) || []
    } else {
      locationOptions.value = []
    }

    // 扁平化展开卷列表，保持每卷原始质检状态（未判定时不预设默认值，强制仓管逐卷或批量判定）
    const flattened: DisplayQcRoll[] = []
    if (res.items && res.items.length > 0) {
      for (const item of res.items) {
        const isMeter =
          (item.unit || '').trim().toUpperCase() === 'M' ||
          (item.unit || '').trim().toUpperCase() === 'METER'
        if (item.rolls && item.rolls.length > 0) {
          for (const roll of item.rolls) {
            const qty = isMeter ? Number(roll.lengthM) || 0 : Number(roll.weight) || 0
            flattened.push({
              id: roll.id,
              returnItemId: item.id!,
              rollNo: roll.rollNo,
              productName: item.productName,
              productCode: item.productCode,
              customerColorNo: item.customerColorNo,
              colorName: item.colorName,
              vatNo: item.vatNo,
              unit: item.unit,
              weight: roll.weight,
              lengthM: roll.lengthM,
              quantity: qty,
              qcResult: roll.qcResult || '',
              locationId: roll.locationId,
              qcRemark: roll.qcRemark || ''
            })
          }
        }
      }
    }
    qcRolls.value = flattened
  } catch (err: any) {
    ElMessage.error(err.message || '加载销售退货单详情失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

const handleQcResultChange = (row: DisplayQcRoll) => {
  if (row.qcResult === 'SCRAP') {
    row.locationId = undefined
  }
}

const handleBatchSetQcResult = (result: 'RESTOCK' | 'SCRAP') => {
  for (const r of qcRolls.value) {
    r.qcResult = result
    if (result === 'SCRAP') {
      r.locationId = undefined
    }
  }
}

const handleApplyBatchLocation = () => {
  if (!batchLocationId.value) return
  for (const r of qcRolls.value) {
    if (r.qcResult === 'RESTOCK') {
      r.locationId = batchLocationId.value
    }
  }
  ElMessage.success('已成功批量应用库位至所有可入库卷')
}

const handleSubmit = async () => {
  // 校验所有卷判定
  const check = validateQcDecision(qcRolls.value)
  if (!check.valid) {
    ElMessage.warning(check.message || '请确保所有退货卷已完成质检判定')
    return
  }

  try {
    await ElMessageBox.confirm(
      '到货质检提交后不可逆，系统将重写入库单并推进退货单状态至已收货，确认提交判定结果吗？',
      '到货质检确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    submitting.value = true
    await receiveSalesReturn({
      id: returnOrder.value!.id!,
      rolls: qcRolls.value.map((r) => ({
        id: r.id!,
        qcResult: r.qcResult!,
        locationId: r.qcResult === 'RESTOCK' ? r.locationId : undefined,
        qcRemark: r.qcRemark
      }))
    })

    ElMessage.success('到货质检成功')
    visible.value = false
    emit('success')
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '到货质检提交失败')
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
