<template>
  <el-dialog
    v-model="visible"
    title="生成客户对账单（向导）"
    width="900px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <!-- 步骤 1：条件选择与参数配置 -->
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="90px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerId">
              <el-select
                v-model="formData.customerId"
                placeholder="搜索并选择客户"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchCustomers"
                :loading="customerLoading"
                class="!w-full"
                @change="handleCustomerChange"
              >
                <el-option
                  v-for="c in customerOptions"
                  :key="c.id"
                  :label="c.companyName"
                  :value="c.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对账币种" prop="currency">
              <el-select
                v-model="formData.currency"
                placeholder="请选择币种"
                class="!w-full"
                @change="handleCriteriaChange"
              >
                <el-option label="人民币 (CNY)" value="CNY" />
                <el-option label="美元 (USD)" value="USD" />
                <el-option label="欧元 (EUR)" value="EUR" />
                <el-option label="港币 (HKD)" value="HKD" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="对账区间" prop="dateRange">
              <el-date-picker
                v-model="formData.dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="!w-full"
                @change="handleCriteriaChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8" class="flex items-center">
            <el-button type="primary" plain @click="fetchPreview" :loading="previewLoading">
              <Icon icon="ep:search" class="mr-1" /> 取数预览
            </el-button>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="对账备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入对账备注（选填）"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 步骤 2：取数预览指标卡片 -->
      <template v-if="previewData">
        <div class="grid grid-cols-4 gap-3 my-4 p-4 bg-gray-50 rounded-lg border border-gray-100">
          <div class="text-center">
            <div class="text-12px text-gray-500 mb-1">期初欠款余额</div>
            <div class="text-16px font-bold font-mono text-gray-800">
              ¥ {{ formatMoney(previewData.openingBalance) }}
            </div>
          </div>
          <div class="text-center border-l border-gray-200">
            <div class="text-12px text-blue-600 mb-1">本期应收（借方）</div>
            <div class="text-16px font-bold font-mono text-blue-600">
              ¥ {{ formatMoney(calculatedSummary.periodDebit) }}
            </div>
          </div>
          <div class="text-center border-l border-gray-200">
            <div class="text-12px text-green-600 mb-1">本期核销（贷方）</div>
            <div class="text-16px font-bold font-mono text-green-600">
              ¥ {{ formatMoney(calculatedSummary.periodCredit) }}
            </div>
          </div>
          <div class="text-center border-l border-gray-200">
            <div class="text-12px text-purple-600 mb-1">期末应收余额</div>
            <div class="text-16px font-bold font-mono text-purple-700">
              ¥ {{ formatMoney(calculatedSummary.closingBalance) }}
            </div>
          </div>
        </div>

        <!-- 步骤 3：候选分录明细勾选表格 -->
        <div class="mb-2 flex justify-between items-center">
          <span class="text-13px font-bold text-gray-700">
            候选分录列表（共 {{ previewData.lines.length }} 条，已选 {{ selectedRows.length }} 条）
          </span>
          <span class="text-12px text-gray-500"> 提示：仅未被占用的分录可勾选入单 </span>
        </div>

        <el-table
          ref="tableRef"
          :data="previewData.lines"
          border
          size="small"
          max-height="300px"
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            type="selection"
            width="45"
            align="center"
            :selectable="(row: CustomerStatementCandidateItemRespVO) => !row.occupied"
          />
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
          <el-table-column label="占用状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.occupied" type="danger" size="small">
                已占用({{ row.occupyingStatementNo }})
              </el-tag>
              <el-tag v-else type="success" size="small">未占用</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        @click="handleSubmit"
        :loading="submitLoading"
        :disabled="!previewData || selectedRows.length === 0"
        v-hasPermi="['lunaerp:finance:customerrecon:create']"
      >
        生成对账单（草稿）
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import {
  prefetchLines,
  createCustomerStatement,
  type CustomerStatementCandidateItemRespVO,
  type CustomerStatementPrefetchRespVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  getDefaultPreviousMonthRange,
  validateStatementPeriod,
  calculateStatementSummary,
  formatMoney,
  formatSignStatusTag
} from '../model/customerStatement'

defineOptions({ name: 'StatementCreateDialog' })

const emit = defineEmits<{
  (e: 'success', id: number): void
}>()

const visible = ref(false)
const loading = ref(false)
const previewLoading = ref(false)
const submitLoading = ref(false)
const customerLoading = ref(false)

const formRef = ref<FormInstance>()
const tableRef = ref<TableInstance>()

const customerOptions = ref<CustomerVO[]>([])
const previewData = ref<CustomerStatementPrefetchRespVO | null>(null)
const selectedRows = ref<CustomerStatementCandidateItemRespVO[]>([])

const formData = reactive({
  customerId: undefined as number | undefined,
  currency: 'CNY',
  dateRange: getDefaultPreviousMonthRange(),
  remark: ''
})

const formRules: FormRules = {
  customerId: [{ required: true, message: '请选择对账客户', trigger: 'change' }],
  currency: [{ required: true, message: '请选择对账币种', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择对账区间', trigger: 'change' }]
}

// 动态重算勾选分录后的借贷与期末
const calculatedSummary = computed(() => {
  const opening = previewData.value ? previewData.value.openingBalance : 0
  return calculateStatementSummary(opening, selectedRows.value)
})

// 搜索客户
const searchCustomers = async (query?: string) => {
  customerLoading.value = true
  try {
    const res = await getCustomerPage({
      pageNo: 1,
      pageSize: 30,
      search: query || ''
    })
    customerOptions.value = res.list || []
  } catch {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = () => {
  previewData.value = null
  selectedRows.value = []
}

const handleCriteriaChange = () => {
  previewData.value = null
  selectedRows.value = []
}

const handleSelectionChange = (rows: CustomerStatementCandidateItemRespVO[]) => {
  selectedRows.value = rows
}

// 执行取数预览
const fetchPreview = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (!formData.dateRange || formData.dateRange.length !== 2) {
    ElMessage.warning('请选择完整的对账起止日期')
    return
  }

  const periodCheck = validateStatementPeriod(formData.dateRange[0], formData.dateRange[1])
  if (!periodCheck.valid) {
    ElMessage.warning(periodCheck.message || '对账区间非法')
    return
  }

  previewLoading.value = true
  try {
    const res = await prefetchLines({
      customerId: formData.customerId!,
      currency: formData.currency,
      periodStart: formData.dateRange[0],
      periodEnd: formData.dateRange[1]
    })
    previewData.value = res
    await nextTick()
    // 默认全选未占用的候选行
    if (tableRef.value && res.lines && res.lines.length > 0) {
      res.lines.forEach((line) => {
        if (!line.occupied) {
          tableRef.value?.toggleRowSelection(line, true)
        }
      })
    }
  } finally {
    previewLoading.value = false
  }
}

// 提交生成草稿
const handleSubmit = async () => {
  if (!formData.customerId || !formData.dateRange || formData.dateRange.length !== 2) {
    return
  }
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少勾选一条分录行以生成对账单')
    return
  }

  submitLoading.value = true
  try {
    const statementId = await createCustomerStatement({
      customerId: formData.customerId,
      currency: formData.currency,
      periodStart: formData.dateRange[0],
      periodEnd: formData.dateRange[1],
      openingBalance: previewData.value?.openingBalance || 0,
      remark: formData.remark,
      selectedEntryIds: selectedRows.value.map((r) => r.ledgerEntryId)
    })
    ElMessage.success('客户对账单草稿生成成功')
    visible.value = false
    emit('success', statementId)
  } finally {
    submitLoading.value = false
  }
}

// 暴露 open 方法
const open = () => {
  visible.value = true
  formData.customerId = undefined
  formData.currency = 'CNY'
  formData.dateRange = getDefaultPreviousMonthRange()
  formData.remark = ''
  previewData.value = null
  selectedRows.value = []
  searchCustomers('')
}

defineExpose({ open })
</script>
