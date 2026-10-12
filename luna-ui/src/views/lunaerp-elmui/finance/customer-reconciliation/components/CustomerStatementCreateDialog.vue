<template>
  <el-dialog
    v-model="visible"
    title="生成客户对账单"
    width="1100px"
    append-to-body
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="对账客户" prop="customerId">
            <el-select
              v-model="formData.customerId"
              filterable
              remote
              reserve-keyword
              placeholder="请输入客户名称搜索"
              :remote-method="searchCustomers"
              :loading="customerLoading"
              style="width: 100%"
              @change="handleCustomerChange"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="币种" prop="currency">
            <el-select v-model="formData.currency" style="width: 100%" @change="handleCriteriaChange">
              <el-option label="CNY (人民币)" value="CNY" />
              <el-option label="USD (美元)" value="USD" />
              <el-option label="EUR (欧元)" value="EUR" />
              <el-option label="HKD (港币)" value="HKD" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="10">
          <el-form-item label="对账期间" prop="periodRange">
            <el-date-picker
              v-model="formData.periodRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              @change="handleCriteriaChange"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="对账单号" prop="statementNo">
            <el-input
              v-model="formData.statementNo"
              placeholder="选填，不填按 CS{yyyy}{4位流水} 自动生成"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="对账备注" prop="remark">
            <el-input v-model="formData.remark" placeholder="请输入对账备注" clearable />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="my-3 flex justify-between items-center">
      <div class="text-sm font-semibold text-gray-700">
        候选分录明细及试算
        <span class="text-xs text-gray-500 font-normal ml-2">
          （发货日落区间的已审核发货明细分录 + 区间内核销/退货贷方分录）
        </span>
      </div>
      <el-button type="primary" plain :loading="prefetchLoading" @click="handlePrefetch">
        <Icon icon="ep:refresh" class="mr-1" /> 取数试算
      </el-button>
    </div>

    <!-- 试算 KPI 汇总栏 -->
    <div v-if="prefetchResult" class="grid grid-cols-4 gap-4 p-3 bg-blue-50/50 rounded border border-blue-100 mb-3">
      <div class="text-center">
        <div class="text-xs text-gray-500">期初余额 (起始日前未核销AR)</div>
        <div class="text-base font-bold text-gray-800 mt-1">
          {{ formatAmount(prefetchResult.openingBalance) }} {{ formData.currency }}
        </div>
      </div>
      <div class="text-center">
        <div class="text-xs text-blue-600">本期借方发生额 (发货应收)</div>
        <div class="text-base font-bold text-blue-600 mt-1">
          + {{ formatAmount(calculatedPeriodDebit) }} {{ formData.currency }}
        </div>
      </div>
      <div class="text-center">
        <div class="text-xs text-green-600">本期贷方发生额 (核销/退货)</div>
        <div class="text-base font-bold text-green-600 mt-1">
          - {{ formatAmount(calculatedPeriodCredit) }} {{ formData.currency }}
        </div>
      </div>
      <div class="text-center">
        <div class="text-xs text-amber-600">预计期末余额 (期初+借-贷)</div>
        <div class="text-base font-bold text-amber-600 mt-1">
          = {{ formatAmount(calculatedClosingBalance) }} {{ formData.currency }}
        </div>
      </div>
    </div>

    <!-- 候选分录表格 -->
    <el-table
      ref="candidateTableRef"
      v-loading="prefetchLoading"
      :data="candidateList"
      border
      max-height="340px"
      empty-text="暂无候选分录，请点击【取数试算】"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" align="center" :selectable="checkSelectable" />
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
      <el-table-column label="摘要/品名事由" prop="summary" min-width="160" show-overflow-tooltip />
      <el-table-column label="借方金额" prop="debitAmount" width="110" align="right">
        <template #default="{ row }">
          <span v-if="row.direction === 'DEBIT'" class="font-medium text-blue-600">
            {{ formatAmount(row.debitAmount) }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="贷方金额" prop="creditAmount" width="110" align="right">
        <template #default="{ row }">
          <span v-if="row.direction === 'CREDIT'" class="font-medium text-green-600">
            {{ formatAmount(row.creditAmount) }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="签收标记" prop="signStatus" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.signStatus === 'SIGNED'" type="success" size="small">已签收</el-tag>
          <el-tag v-else-if="row.signStatus === 'REFUSED'" type="danger" size="small">拒签</el-tag>
          <el-tag v-else-if="row.direction === 'DEBIT'" type="info" size="small">未签收</el-tag>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="占用状态" width="120" align="center">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.occupied"
            :content="`已占用: ${row.occupiedStatementNo} (${row.occupiedStatus})`"
            placement="top"
          >
            <el-tag type="danger" size="small">
              已占用
            </el-tag>
          </el-tooltip>
          <el-tag v-else type="success" size="small">未占用</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定生成对账单
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  prefetchCustomerStatementLines,
  createCustomerStatement
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import type {
  CustomerStatementCandidateItemVO,
  CustomerStatementPrefetchRespVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'

const emit = defineEmits(['success'])

const visible = ref(false)
const formRef = ref<FormInstance>()
const candidateTableRef = ref()
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])
const prefetchLoading = ref(false)
const submitLoading = ref(false)

const prefetchResult = ref<CustomerStatementPrefetchRespVO | null>(null)
const candidateList = ref<CustomerStatementCandidateItemVO[]>([])
const selectedRows = ref<CustomerStatementCandidateItemVO[]>([])

// 计算上一自然月的起止日期
const getDefaultPeriodRange = (): [string, string] => {
  const now = new Date()
  const firstDayCurrent = new Date(now.getFullYear(), now.getMonth(), 1)
  const lastDayPrev = new Date(firstDayCurrent.getTime() - 24 * 60 * 60 * 1000)
  const firstDayPrev = new Date(lastDayPrev.getFullYear(), lastDayPrev.getMonth(), 1)
  const formatDate = (d: Date) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }
  return [formatDate(firstDayPrev), formatDate(lastDayPrev)]
}

const formData = reactive({
  statementNo: '',
  customerId: undefined as number | undefined,
  currency: 'CNY',
  periodRange: getDefaultPeriodRange(),
  remark: ''
})

const formRules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }],
  periodRange: [{ required: true, message: '请选择对账期间', trigger: 'change' }]
}

const formatAmount = (val?: number | string) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const checkSelectable = (row: CustomerStatementCandidateItemVO) => {
  return !row.occupied
}

const calculatedPeriodDebit = computed(() => {
  return selectedRows.value.reduce((acc, row) => acc + (row.direction === 'DEBIT' ? Number(row.debitAmount || 0) : 0), 0)
})

const calculatedPeriodCredit = computed(() => {
  return selectedRows.value.reduce((acc, row) => acc + (row.direction === 'CREDIT' ? Number(row.creditAmount || 0) : 0), 0)
})

const calculatedClosingBalance = computed(() => {
  const opening = prefetchResult.value ? Number(prefetchResult.value.openingBalance || 0) : 0
  return opening + calculatedPeriodDebit.value - calculatedPeriodCredit.value
})

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({ search: query, pageNo: 1, pageSize: 50 })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = () => {
  prefetchResult.value = null
  candidateList.value = []
  selectedRows.value = []
}

const handleCriteriaChange = () => {
  prefetchResult.value = null
  candidateList.value = []
  selectedRows.value = []
}

const handlePrefetch = async () => {
  if (!formData.customerId) {
    ElMessage.warning('请先选择对账客户')
    return
  }
  if (!formData.periodRange || formData.periodRange.length < 2) {
    ElMessage.warning('请选择对账期间')
    return
  }

  prefetchLoading.value = true
  try {
    const res = await prefetchCustomerStatementLines({
      customerId: formData.customerId,
      currency: formData.currency,
      periodStart: formData.periodRange[0],
      periodEnd: formData.periodRange[1]
    })
    prefetchResult.value = res
    candidateList.value = res.items || []

    await nextTick()
    // 默认全选所有未占用的候选行
    if (candidateTableRef.value && candidateList.value.length > 0) {
      candidateTableRef.value.clearSelection()
      candidateList.value.forEach((row) => {
        if (!row.occupied) {
          candidateTableRef.value.toggleRowSelection(row, true)
        }
      })
    }
  } finally {
    prefetchLoading.value = false
  }
}

const handleSelectionChange = (selection: CustomerStatementCandidateItemVO[]) => {
  selectedRows.value = selection
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少勾选一条候选分录纳入对账单')
    return
  }

  submitLoading.value = true
  try {
    const selectedEntryIds = selectedRows.value.map((item) => item.ledgerEntryId)
    await createCustomerStatement({
      statementNo: formData.statementNo ? formData.statementNo.trim() : undefined,
      customerId: formData.customerId!,
      currency: formData.currency,
      periodStart: formData.periodRange[0],
      periodEnd: formData.periodRange[1],
      selectedEntryIds,
      remark: formData.remark
    })
    ElMessage.success('成功生成客户对账单草稿')
    visible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}

const open = () => {
  visible.value = true
  formData.statementNo = ''
  formData.customerId = undefined
  formData.currency = 'CNY'
  formData.periodRange = getDefaultPeriodRange()
  formData.remark = ''
  prefetchResult.value = null
  candidateList.value = []
  selectedRows.value = []
  searchCustomers('')
}

defineExpose({ open })
</script>
