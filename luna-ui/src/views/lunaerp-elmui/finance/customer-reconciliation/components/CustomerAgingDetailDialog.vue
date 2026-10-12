<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="1100px"
    destroy-on-close
    append-to-body
  >
    <!-- 头部摘要信息 -->
    <el-descriptions :column="4" border class="mb-4">
      <el-descriptions-item label="客户名称">
        {{ currentCustomer?.customerName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="币种">
        <el-tag size="small">{{ currentCustomer?.currency || 'CNY' }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="信用账期">
        {{ currentCustomer?.creditDays != null ? `${currentCustomer.creditDays} 天` : '未设置' }}
      </el-descriptions-item>
      <el-descriptions-item label="未核销应收合计">
        <span class="text-danger font-bold">
          {{ formatAmount(currentCustomer?.totalBalance) }}
        </span>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 未核销明细分录表格 -->
    <el-table
      v-loading="loading"
      :data="detailList"
      border
      stripe
      max-height="450px"
      show-summary
      :summary-method="getSummary"
    >
      <el-table-column label="业务日期" prop="bizDate" width="110" align="center" sortable />
      <el-table-column label="单据类型" prop="refType" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.refType || '分录' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="单据编号" prop="refNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="原始发生额" prop="originalAmount" width="120" align="right">
        <template #default="{ row }">
          {{ formatAmount(row.originalAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="已核销金额" prop="settledAmount" width="110" align="right">
        <template #default="{ row }">
          {{ formatAmount(row.settledAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="未核销余额" prop="unsettledAmount" width="120" align="right">
        <template #default="{ row }">
          <span class="text-primary font-bold">{{ formatAmount(row.unsettledAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账龄天数" prop="agingDays" width="90" align="right" sortable>
        <template #default="{ row }">
          <span>{{ row.agingDays }} 天</span>
        </template>
      </el-table-column>
      <el-table-column label="账龄区间" prop="bucket" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getBucketTagType(row.bucket)" size="small">
            {{ row.bucket }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="到期日" prop="dueDate" width="110" align="center">
        <template #default="{ row }">
          {{ row.dueDate || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="逾期状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isOverdue" type="danger" size="small" effect="dark">
            已逾期 ({{ row.overdueDays }}天)
          </el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="摘要 / 备注" prop="summary" min-width="160" show-overflow-tooltip />
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  getCustomerAgingDetail,
  CustomerAgingDetailVO,
  CustomerAgingSummaryVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { message } from '@/utils/message'

const visible = ref(false)
const loading = ref(false)
const detailList = ref<CustomerAgingDetailVO[]>([])
const currentCustomer = ref<CustomerAgingSummaryVO | null>(null)
const queryAgingDate = ref<string>('')

const dialogTitle = computed(() => {
  return currentCustomer.value
    ? `客户账龄未核销明细 - ${currentCustomer.value.customerName}`
    : '客户账龄未核销明细'
})

const open = async (customer: CustomerAgingSummaryVO, agingDate: string) => {
  currentCustomer.value = customer
  queryAgingDate.value = agingDate
  visible.value = true
  await fetchDetails(customer.customerId, agingDate, customer.currency)
}

const fetchDetails = async (customerId: number, agingDate: string, currency?: string) => {
  loading.value = true
  try {
    const res = await getCustomerAgingDetail({
      customerId,
      agingDate,
      currency
    })
    detailList.value = res || []
  } catch (err: any) {
    message.error(err.message || '获取未核销明细失败')
    detailList.value = []
  } finally {
    loading.value = false
  }
}

const formatAmount = (val: number | string | undefined) => {
  if (val === undefined || val === null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return String(val)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getBucketTagType = (bucket: string) => {
  switch (bucket) {
    case '0-30':
      return 'success'
    case '31-60':
      return ''
    case '61-90':
      return 'warning'
    case '91-180':
      return 'danger'
    case '181+':
      return 'danger'
    default:
      return 'info'
  }
}

const getSummary = (param: { columns: any[]; data: CustomerAgingDetailVO[] }) => {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (['originalAmount', 'settledAmount', 'unsettledAmount'].includes(column.property)) {
      const values = data.map((item) => Number(item[column.property as keyof CustomerAgingDetailVO]))
      if (!values.every((value) => Number.isNaN(value))) {
        const sum = values.reduce((prev, curr) => {
          const value = Number(curr)
          if (!Number.isNaN(value)) {
            return prev + curr
          } else {
            return prev
          }
        }, 0)
        sums[index] = formatAmount(sum)
      } else {
        sums[index] = '0.00'
      }
    } else {
      sums[index] = ''
    }
  })
  return sums
}

defineExpose({ open })
</script>

<style scoped>
.text-danger {
  color: #f56c6c;
}
.text-primary {
  color: #409eff;
}
</style>
