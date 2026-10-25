<template>
  <el-dialog
    v-model="visible"
    title="选择可开票发货单"
    width="900px"
    destroy-on-close
    append-to-body
  >
    <div class="mb-3 flex items-center justify-between">
      <div class="text-14px text-gray-500">
        客户：<span class="font-bold text-gray-700">{{ customerName || '-' }}</span> | 币种：<span
          class="font-bold text-primary"
          >{{ currency || 'RMB' }}</span
        >
      </div>
      <el-button link type="primary" :icon="Refresh" @click="loadShipments">刷新候选</el-button>
    </div>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="shipmentList"
      border
      max-height="450px"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column prop="shipmentNo" label="发货单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="shippingDate" label="发货日期" width="110" align="center" />
      <el-table-column prop="contractNo" label="合同号" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.contractNo || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="shipmentAmount" label="发货价税合计" min-width="120" align="right">
        <template #default="{ row }">
          {{ formatMoney(row.shipmentAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="invoicedAmount" label="已开票/在途" min-width="120" align="right">
        <template #default="{ row }">
          {{ formatMoney(row.invoicedAmount) }}
        </template>
      </el-table-column>
      <el-table-column prop="availableAmount" label="可开票余额" min-width="120" align="right">
        <template #default="{ row }">
          <span class="font-semibold text-success">{{ formatMoney(row.availableAmount) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="selectedShipments.length === 0"
          @click="handleConfirm"
          v-hasPermi="['lunaerp:finance:invoice:create', 'lunaerp:finance:invoice:update']"
        >
          确定选择 ({{ selectedShipments.length }})
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getCandidateShipments,
  type InvoiceCandidateShipmentRespVO
} from '@/api/lunaerp-elmui/finance/invoice'
import { formatMoney } from '../model/invoice'

const emit = defineEmits<{
  (e: 'confirm', shipments: InvoiceCandidateShipmentRespVO[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const customerId = ref<number>()
const customerName = ref<string>('')
const currency = ref<string>('RMB')
const shipmentList = ref<InvoiceCandidateShipmentRespVO[]>([])
const selectedShipments = ref<InvoiceCandidateShipmentRespVO[]>([])
const tableRef = ref()

const open = async (cId: number, cName: string, curr: string) => {
  customerId.value = cId
  customerName.value = cName
  currency.value = curr || 'RMB'
  visible.value = true
  selectedShipments.value = []
  await loadShipments()
}

const loadShipments = async () => {
  if (!customerId.value) return
  loading.value = true
  try {
    const res = await getCandidateShipments({
      customerId: customerId.value,
      currency: currency.value
    })
    shipmentList.value = res || []
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows: InvoiceCandidateShipmentRespVO[]) => {
  selectedShipments.value = rows
}

const handleConfirm = () => {
  emit('confirm', [...selectedShipments.value])
  visible.value = false
}

defineExpose({ open })
</script>
