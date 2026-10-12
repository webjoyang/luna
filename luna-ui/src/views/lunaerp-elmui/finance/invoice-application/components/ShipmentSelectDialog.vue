<template>
  <el-dialog
    v-model="visible"
    title="选择发货单（同客户同币种）"
    width="850px"
    append-to-body
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <el-alert
        title="提示：仅展示状态为已完成（COMPLETED）且有可开票余额的发货单；同一发票申请必须为同币种。"
        type="info"
        show-icon
        :closable="false"
        class="mb-3"
      />

      <el-table
        ref="tableRef"
        :data="tableData"
        row-key="id"
        border
        max-height="400"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" :selectable="isRowSelectable" />
        <el-table-column label="发货单号" prop="shipmentNo" min-width="150" show-overflow-tooltip />
        <el-table-column label="币种" prop="currency" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发货日期" prop="shippingDate" width="110" align="center" />
        <el-table-column label="发货总额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.shipmentAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="已开票金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.invoicedAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="本次可开票余额" width="130" align="right">
          <template #default="{ row }">
            <span class="font-bold text-green-600">{{ formatAmount(row.uninvoicedBalance) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-3 text-right text-gray-500 text-sm">
        已选 {{ selectedRows.length }} 张发货单
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="selectedRows.length === 0" @click="handleConfirm">
        确认选择
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as InvoiceApi from '@/api/lunaerp-elmui/finance/invoice'
import type { InvoiceCandidateShipmentVO } from '@/api/lunaerp-elmui/finance/invoice'

defineOptions({ name: 'ShipmentSelectDialog' })

const emit = defineEmits<{
  (e: 'select', rows: InvoiceCandidateShipmentVO[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const tableData = ref<InvoiceCandidateShipmentVO[]>([])
const selectedRows = ref<InvoiceCandidateShipmentVO[]>([])
let currentCurrency = ''
let excludeIds: number[] = []

function formatAmount(val?: number | string | null): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function isRowSelectable(row: InvoiceCandidateShipmentVO) {
  if (excludeIds.includes(row.id)) {
    return false
  }
  if (currentCurrency && row.currency !== currentCurrency) {
    return false
  }
  return Number(row.uninvoicedBalance) > 0
}

const open = async (customerId: number, currency = '', alreadySelectedIds: number[] = []) => {
  if (!customerId) {
    ElMessage.warning('请先选择客户')
    return
  }
  visible.value = true
  loading.value = true
  currentCurrency = currency
  excludeIds = alreadySelectedIds
  selectedRows.value = []
  try {
    tableData.value = await InvoiceApi.getCandidateShipments(customerId)
  } catch (e) {
    ElMessage.error('加载待开票发货单失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows: InvoiceCandidateShipmentVO[]) => {
  // 检查币种一致性
  if (rows.length > 1) {
    const firstCurrency = rows[0].currency
    const mixed = rows.some((r) => r.currency !== firstCurrency)
    if (mixed) {
      ElMessage.warning('所选发货单币种不一致，禁止混选！')
      return
    }
  }
  selectedRows.value = rows
}

const handleConfirm = () => {
  if (selectedRows.value.length === 0) return
  emit('select', selectedRows.value)
  visible.value = false
}

defineExpose({ open })
</script>
