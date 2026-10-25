<template>
  <el-dialog
    v-model="visible"
    title="草稿对账单 - 手工追加未占用分录"
    width="880px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <!-- 检索与期间选择 -->
      <el-form :inline="true" class="mb-3">
        <el-form-item label="分录业务期间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="!w-240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain @click="fetchCandidates" :loading="searchLoading">
            <Icon icon="ep:search" class="mr-1" /> 检索候选分录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 候选分录表格 -->
      <div class="mb-2 text-12px text-gray-500">
        提示：仅展示属于该客户与币种、且未被任何对账单占用的历史经营分录。请单选需追加的分录。
      </div>

      <el-table
        :data="filteredCandidates"
        border
        size="small"
        max-height="320px"
        highlight-current-row
        @current-change="handleCurrentChange"
      >
        <el-table-column width="45" align="center">
          <template #default="{ row }">
            <el-radio
              v-model="selectedEntryId"
              :label="row.ledgerEntryId"
              :disabled="row.occupied || isAlreadyInStatement(row.ledgerEntryId)"
            >
              <span></span>
            </el-radio>
          </template>
        </el-table-column>
        <el-table-column label="业务日期" prop="bizDate" width="105" align="center" />
        <el-table-column label="分录类型" prop="entryType" width="120" show-overflow-tooltip />
        <el-table-column label="关联单号" prop="refNo" width="140" show-overflow-tooltip />
        <el-table-column label="摘要说明" prop="summary" min-width="150" show-overflow-tooltip />
        <el-table-column label="借方发生" prop="debitAmount" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.debitAmount > 0" class="font-mono text-blue-600">
              ¥{{ formatMoney(row.debitAmount) }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方核销" prop="creditAmount" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.creditAmount > 0" class="font-mono text-green-600">
              ¥{{ formatMoney(row.creditAmount) }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="签收状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.signStatus"
              :type="formatSignStatusTag(row.signStatus).type"
              size="small"
            >
              {{ formatSignStatusTag(row.signStatus).label }}
            </el-tag>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 追加说明与行备注 -->
      <div class="mt-4">
        <el-form label-width="80px">
          <el-form-item label="行备注">
            <el-input
              v-model="itemRemark"
              placeholder="请输入追加分录的行备注（选填）"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        @click="handleSubmit"
        :loading="submitLoading"
        :disabled="!selectedEntryId"
        v-hasPermi="['lunaerp:finance:customerrecon:update']"
      >
        确定追加
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  prefetchLines,
  addStatementEntry,
  type CustomerStatementCandidateItemRespVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import {
  formatMoney,
  formatSignStatusTag,
  validateStatementPeriod
} from '../model/customerStatement'

defineOptions({ name: 'StatementAddEntryDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const searchLoading = ref(false)
const submitLoading = ref(false)

const currentStatementId = ref<number>(0)
const currentCustomerId = ref<number>(0)
const currentCurrency = ref<string>('CNY')
const existingIds = ref<number[]>([])

const dateRange = ref<[string, string]>(['', ''])
const candidateList = ref<CustomerStatementCandidateItemRespVO[]>([])
const selectedEntryId = ref<number | null>(null)
const itemRemark = ref('')

const isAlreadyInStatement = (entryId: number) => {
  return existingIds.value.includes(entryId)
}

const filteredCandidates = computed(() => {
  return candidateList.value.filter(
    (item) => !item.occupied && !isAlreadyInStatement(item.ledgerEntryId)
  )
})

const handleCurrentChange = (row?: CustomerStatementCandidateItemRespVO) => {
  if (row && !row.occupied && !isAlreadyInStatement(row.ledgerEntryId)) {
    selectedEntryId.value = row.ledgerEntryId
  }
}

const fetchCandidates = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择查询期间')
    return
  }

  const check = validateStatementPeriod(dateRange.value[0], dateRange.value[1])
  if (!check.valid) {
    ElMessage.warning(check.message || '查询期间非法')
    return
  }

  searchLoading.value = true
  try {
    const res = await prefetchLines({
      customerId: currentCustomerId.value,
      currency: currentCurrency.value,
      periodStart: dateRange.value[0],
      periodEnd: dateRange.value[1]
    })
    candidateList.value = res.lines || []
    selectedEntryId.value = null
  } finally {
    searchLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!currentStatementId.value || !selectedEntryId.value) return

  submitLoading.value = true
  try {
    await addStatementEntry({
      statementId: currentStatementId.value,
      ledgerEntryId: selectedEntryId.value,
      remark: itemRemark.value
    })
    ElMessage.success('成功追加分录至对账单')
    visible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}

const open = (params: {
  statementId: number
  customerId: number
  currency: string
  periodStart: string
  periodEnd: string
  existingEntryIds: number[]
}) => {
  currentStatementId.value = params.statementId
  currentCustomerId.value = params.customerId
  currentCurrency.value = params.currency || 'CNY'
  dateRange.value = [params.periodStart, params.periodEnd]
  existingIds.value = params.existingEntryIds || []
  candidateList.value = []
  selectedEntryId.value = null
  itemRemark.value = ''
  visible.value = true

  fetchCandidates()
}

defineExpose({ open })
</script>
