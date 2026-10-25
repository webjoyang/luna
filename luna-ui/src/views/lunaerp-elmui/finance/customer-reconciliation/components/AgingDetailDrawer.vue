<template>
  <el-drawer
    v-model="visible"
    :title="`客户账龄未核销明细下钻 - ${customerSummary?.customerName || ''}`"
    size="80%"
    destroy-on-close
    direction="rtl"
  >
    <div v-loading="loading" class="p-4">
      <!-- 客户概览卡片 -->
      <el-card shadow="never" class="mb-4 bg-gray-50/50">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="客户名称">
            <span class="font-bold text-gray-800">{{ customerSummary?.customerName }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="信用账期">
            <el-tag size="small" type="info">
              {{
                customerSummary?.creditDays != null
                  ? `${customerSummary.creditDays} 天`
                  : '未设账期'
              }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="未核销应收总额">
            <span class="font-bold font-mono text-purple-700 text-15px">
              ¥{{ formatMoney(customerSummary?.totalArBalance) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="其中已逾期金额">
            <span
              :class="[
                'font-bold font-mono text-15px',
                customerSummary?.overdueAmount && Number(customerSummary.overdueAmount) > 0
                  ? 'text-red-600'
                  : 'text-gray-500'
              ]"
            >
              ¥{{ formatMoney(customerSummary?.overdueAmount) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 搜索过滤条 -->
      <div class="flex justify-between items-center mb-3">
        <div class="text-14px font-bold text-gray-700 flex items-center">
          <Icon icon="ep:list" class="mr-1" /> 未核销明细分录（按发货时间先进先出消耗）
        </div>
        <el-button @click="loadData" size="small">
          <Icon icon="ep:refresh" class="mr-1" /> 刷新
        </el-button>
      </div>

      <!-- 明细表格 -->
      <el-table :data="items" border stripe max-height="560">
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column label="发货日期" prop="bizDate" width="115" align="center">
          <template #default="{ row }">
            <span class="font-mono text-13px">{{ row.bizDate }}</span>
          </template>
        </el-table-column>

        <el-table-column label="发货单号" prop="refNo" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono font-medium">{{ row.refNo || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="合同号" prop="contractNo" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono text-gray-600">{{ row.contractNo || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="品名/摘要" prop="productName" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.productName || '发货应收' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="币种" prop="currency" width="75" align="center" />

        <el-table-column label="原始金额" prop="originalAmount" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono">¥{{ formatMoney(row.originalAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="已核销/冲减" prop="writtenOffAmount" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-green-600">¥{{ formatMoney(row.writtenOffAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="未核销余额" prop="remainingAmount" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-purple-700"
              >¥{{ formatMoney(row.remainingAmount) }}</span
            >
          </template>
        </el-table-column>

        <el-table-column label="账龄天数" prop="agingDays" width="90" align="center">
          <template #default="{ row }">
            <span class="font-mono font-bold">{{ row.agingDays }}天</span>
          </template>
        </el-table-column>

        <el-table-column label="账龄区间" prop="bucket" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="formatAgingBucketTag(row.bucket).type" size="small">
              {{ formatAgingBucketTag(row.bucket).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="应到期日" prop="dueDate" width="115" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px">{{ row.dueDate || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="逾期状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="formatAgingOverdueTag(row.isOverdue, row.overdueDays).type" size="small">
              {{ formatAgingOverdueTag(row.isOverdue, row.overdueDays).label }}
            </el-tag>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="该客户当前无未核销分录" :image-size="80" />
        </template>
      </el-table>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CustomerAgingSummaryRespVO,
  CustomerAgingDetailRespVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { getCustomerAgingDetail } from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { formatMoney, formatAgingBucketTag, formatAgingOverdueTag } from '../model/customerAging'

const visible = ref(false)
const loading = ref(false)
const customerSummary = ref<CustomerAgingSummaryRespVO | null>(null)
const items = ref<CustomerAgingDetailRespVO[]>([])

const open = (summary: CustomerAgingSummaryRespVO) => {
  customerSummary.value = summary
  visible.value = true
  loadData()
}

const loadData = async () => {
  if (!customerSummary.value?.customerId) return
  try {
    loading.value = true
    items.value = await getCustomerAgingDetail({
      customerId: customerSummary.value.customerId,
      agingDate: customerSummary.value.agingDate
    })
  } catch (error) {
    ElMessage.error('加载未核销明细分录失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
