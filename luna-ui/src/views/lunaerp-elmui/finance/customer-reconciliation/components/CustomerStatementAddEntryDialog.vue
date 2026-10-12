<template>
  <el-dialog
    v-model="visible"
    :title="`手工追加分录 - ${statement?.statementNo || ''}`"
    width="900px"
    append-to-body
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="statement" class="mb-3 text-sm text-gray-600 flex gap-4">
      <span><strong>客户：</strong>{{ statement.customerName }}</span>
      <span><strong>币种：</strong>{{ statement.currency }}</span>
      <span><strong>期间：</strong>{{ statement.periodStart }} ~ {{ statement.periodEnd }}</span>
    </div>

    <el-table
      v-loading="loading"
      :data="availableEntries"
      border
      max-height="350px"
      highlight-current-row
      empty-text="当前对账期间无可追加的未占用分录"
      @current-change="handleCurrentChange"
    >
      <el-table-column width="40" align="center">
        <template #default="{ row }">
          <el-radio :model-value="selectedEntry?.ledgerEntryId" :label="row.ledgerEntryId">
            &nbsp;
          </el-radio>
        </template>
      </el-table-column>
      <el-table-column label="业务日期" prop="bizDate" width="110" align="center" />
      <el-table-column label="借贷方向" prop="direction" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'DEBIT' ? 'primary' : 'success'" size="small">
            {{ row.direction === 'DEBIT' ? '借方' : '贷方' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分录类别" prop="category" width="110" align="center">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_LEDGER_CATEGORY" :value="row.category" />
        </template>
      </el-table-column>
      <el-table-column label="源单单号" prop="refNo" min-width="140" show-overflow-tooltip />
      <el-table-column label="摘要/品名" prop="summary" min-width="160" show-overflow-tooltip />
      <el-table-column label="借方金额" prop="debitAmount" width="110" align="right">
        <template #default="{ row }">
          <span v-if="row.direction === 'DEBIT'" class="text-blue-600 font-medium">
            {{ formatAmount(row.debitAmount) }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="贷方金额" prop="creditAmount" width="110" align="right">
        <template #default="{ row }">
          <span v-if="row.direction === 'CREDIT'" class="text-green-600 font-medium">
            {{ formatAmount(row.creditAmount) }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedEntry" :loading="submitLoading" @click="handleSubmit">
          确定追加
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import {
  prefetchCustomerStatementLines,
  addCustomerStatementEntry,
  getCustomerStatement
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import type {
  CustomerStatementVO,
  CustomerStatementCandidateItemVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'

const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const statement = ref<CustomerStatementVO | null>(null)
const availableEntries = ref<CustomerStatementCandidateItemVO[]>([])
const selectedEntry = ref<CustomerStatementCandidateItemVO | null>(null)

const formatAmount = (val?: number | string) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleCurrentChange = (val: CustomerStatementCandidateItemVO) => {
  selectedEntry.value = val
}

const open = async (targetStatement: CustomerStatementVO) => {
  visible.value = true
  statement.value = targetStatement
  selectedEntry.value = null
  loading.value = true

  try {
    // 获取当前对账单已包含的分录 ID 集合
    const detail = await getCustomerStatement(targetStatement.id)
    const existingEntryIds = new Set((detail.items || []).map((i) => i.ledgerEntryId).filter(Boolean))

    // 预取期间内候选分录
    const res = await prefetchCustomerStatementLines({
      customerId: targetStatement.customerId,
      currency: targetStatement.currency,
      periodStart: targetStatement.periodStart,
      periodEnd: targetStatement.periodEnd,
      excludeStatementId: targetStatement.id
    })

    // 排除已被占用或已在本对账单中的项
    availableEntries.value = (res.items || []).filter(
      (item) => !item.occupied && !existingEntryIds.has(item.ledgerEntryId)
    )
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!statement.value || !selectedEntry.value) return

  submitLoading.value = true
  try {
    await addCustomerStatementEntry({
      statementId: statement.value.id,
      ledgerEntryId: selectedEntry.value.ledgerEntryId
    })
    ElMessage.success('成功追加分录到对账单')
    visible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
