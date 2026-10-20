<template>
  <el-dialog
    v-model="visible"
    :title="`工资发放批次记录 - ${payroll?.payrollNo || ''}`"
    width="920px"
    destroy-on-close
    append-to-body
  >
    <div
      class="bg-gray-50 border border-gray-200 rounded p-12px mb-16px text-12px flex items-center justify-between"
    >
      <div class="flex items-center gap-16px">
        <span
          >期间：<strong class="font-mono">{{ payroll?.period }}</strong></span
        >
        <span
          >实发总额：<strong class="font-mono text-blue-700"
            >¥ {{ formatAmount(payroll?.netAmount) }}</strong
          ></span
        >
        <span
          >已发总额：<strong class="font-mono text-green-700"
            >¥ {{ formatAmount(payroll?.paidAmount) }}</strong
          ></span
        >
      </div>
      <div>
        发放状态：
        <el-tag
          :type="
            payroll?.paymentStatus === 'PAID'
              ? 'success'
              : payroll?.paymentStatus === 'PARTIAL'
                ? 'warning'
                : 'info'
          "
          size="small"
        >
          {{
            payroll?.paymentStatus === 'PAID'
              ? '已发清'
              : payroll?.paymentStatus === 'PARTIAL'
                ? '部分发放'
                : '未发放'
          }}
        </el-tag>
      </div>
    </div>

    <!-- 批次流水表格（只读不可改删） -->
    <el-table v-loading="loading" :data="paymentList" border style="width: 100%" max-height="420px">
      <el-table-column label="发放流水号" prop="paymentNo" min-width="160" align="center">
        <template #default="{ row }">
          <span class="font-mono font-bold">{{ row.paymentNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="付款资金账户" prop="accountName" min-width="140" />
      <el-table-column label="币种" prop="currency" width="80" align="center" />
      <el-table-column label="发放金额" prop="originalAmount" width="130" align="right">
        <template #default="{ row }">
          <span class="font-mono font-bold text-green-700">
            ¥ {{ formatAmount(row.originalAmount) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="支付日期" prop="payDate" width="120" align="center">
        <template #default="{ row }">
          <span class="font-mono">{{ row.payDate }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作人" prop="creator" width="110" align="center" />
      <el-table-column label="备注说明" prop="remark" min-width="150" show-overflow-tooltip />
    </el-table>

    <!-- 分页器 -->
    <div class="mt-12px flex justify-end">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { getPayrollPaymentPage } from '@/api/lunaerp-elmui/finance/payroll'
import type { PayrollRespVO, PayrollPaymentRespVO } from '@/api/lunaerp-elmui/finance/payroll'
import { formatAmount } from '../model/payroll'

defineOptions({ name: 'PaymentListDialog' })

const props = defineProps<{
  modelValue: boolean
  payroll?: PayrollRespVO
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const paymentList = ref<PayrollPaymentRespVO[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10
})

const loadData = async () => {
  if (!props.payroll?.id) return
  loading.value = true
  try {
    const res = await getPayrollPaymentPage({
      payrollId: props.payroll.id,
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize
    })
    paymentList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      queryParams.pageNo = 1
      loadData()
    } else {
      paymentList.value = []
      total.value = 0
    }
  }
)
</script>
