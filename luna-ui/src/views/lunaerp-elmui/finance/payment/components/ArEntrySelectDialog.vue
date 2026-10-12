<template>
  <el-dialog
    v-model="visible"
    title="选择应收分录（同客户同币种）"
    width="960px"
    append-to-body
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <el-alert
        title="提示：系统展示该客户对应币种下尚未完全核销的应收分录（发货应收等），支持发货单、合同、分录三视角筛选。"
        type="info"
        show-icon
        :closable="false"
        class="mb-3"
      />

      <!-- 筛选栏 -->
      <el-form :inline="true" class="mb-2">
        <el-form-item label="筛选视角">
          <el-radio-group v-model="viewMode" size="small" @change="handleViewModeChange">
            <el-radio-button label="ALL">全部分录</el-radio-button>
            <el-radio-button label="SHIPMENT">按发货单</el-radio-button>
            <el-radio-button label="CONTRACT">按合同</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="keyword"
            placeholder="分录号 / 发货单号 / 合同号"
            clearable
            size="small"
            class="!w-220px"
          />
        </el-form-item>
      </el-form>

      <!-- 分录表格 -->
      <el-table
        ref="tableRef"
        :data="filteredTableData"
        row-key="ledgerEntryId"
        border
        max-height="400"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" :selectable="isRowSelectable" />
        <el-table-column label="分录单号" prop="entryNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono text-blue-600">{{ row.entryNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分录类型" prop="category" width="110" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_LEDGER_CATEGORY" :value="row.category" />
          </template>
        </el-table-column>
        <el-table-column label="业务日期" prop="bizDate" width="110" align="center" />
        <el-table-column label="关联发货单" prop="shipmentNo" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.shipmentNo" class="font-mono text-gray-700">{{ row.shipmentNo }}</span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="关联合同" prop="contractNo" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.contractNo" class="font-mono text-gray-700">{{ row.contractNo }}</span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="币种" prop="currency" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原始金额" width="110" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.originalAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="已核销" width="100" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.allocatedAmount || 0) }}
          </template>
        </el-table-column>
        <el-table-column label="未核销余额" width="120" align="right">
          <template #default="{ row }">
            <span class="font-bold text-green-600">{{ formatAmount(row.unallocatedBalance) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-3 text-right text-gray-500 text-sm">
        共 {{ filteredTableData.length }} 条分录，已勾选 {{ selectedRows.length }} 条
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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type { ArBalanceRespVO } from '@/api/lunaerp-elmui/finance/payment'
import { formatAmount } from '../utils'

defineOptions({ name: 'ArEntrySelectDialog' })

const emit = defineEmits<{
  (e: 'select', rows: ArBalanceRespVO[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const tableData = ref<ArBalanceRespVO[]>([])
const selectedRows = ref<ArBalanceRespVO[]>([])
const viewMode = ref<'ALL' | 'SHIPMENT' | 'CONTRACT'>('ALL')
const keyword = ref('')
let excludeIds: number[] = []

const filteredTableData = computed(() => {
  let list = tableData.value
  if (viewMode.value === 'SHIPMENT') {
    list = list.filter((r) => !!r.shipmentNo)
  } else if (viewMode.value === 'CONTRACT') {
    list = list.filter((r) => !!r.contractNo)
  }

  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(
      (r) =>
        (r.entryNo && r.entryNo.toLowerCase().includes(kw)) ||
        (r.shipmentNo && r.shipmentNo.toLowerCase().includes(kw)) ||
        (r.contractNo && r.contractNo.toLowerCase().includes(kw))
    )
  }
  return list
})

function isRowSelectable(row: ArBalanceRespVO) {
  if (excludeIds.includes(row.ledgerEntryId)) {
    return false
  }
  return Number(row.unallocatedBalance) > 0
}

const open = async (customerId: number, currency: string, alreadySelectedIds: number[] = []) => {
  if (!customerId) {
    ElMessage.warning('请先选择客户')
    return
  }
  if (!currency) {
    ElMessage.warning('请先选择币种')
    return
  }
  visible.value = true
  loading.value = true
  viewMode.value = 'ALL'
  keyword.value = ''
  excludeIds = alreadySelectedIds
  selectedRows.value = []
  try {
    const list = await PaymentApi.getArBalance(customerId, currency)
    tableData.value = list || []
  } catch (e) {
    ElMessage.error('加载应收分录失败')
  } finally {
    loading.value = false
  }
}

const handleViewModeChange = () => {
  // 切换视角
}

const handleSelectionChange = (rows: ArBalanceRespVO[]) => {
  selectedRows.value = rows
}

const handleConfirm = () => {
  if (selectedRows.value.length === 0) return
  emit('select', selectedRows.value)
  visible.value = false
}

defineExpose({ open })
</script>
