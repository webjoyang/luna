<template>
  <el-dialog
    v-model="visible"
    title="付款单流水详情"
    width="720px"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="mb-4 flex items-center justify-between pb-3 border-b border-[#e2e8f0]">
          <div>
            <span class="text-xs text-gray-500 block mb-1">付款单号</span>
            <span class="text-lg font-bold font-mono text-blue-600">{{ detail.paymentNo }}</span>
          </div>
          <div class="flex items-center gap-2">
            <el-tag type="warning" effect="plain">付款支出</el-tag>
            <el-tag type="success" effect="dark">已登记</el-tag>
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="收款供应商" :span="2">
            {{ detail.supplierName || '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="付款资金账户">
            {{ accountLabel }}
          </el-descriptions-item>

          <el-descriptions-item label="登记时间">
            {{ detail.createTime ? formatDate(detail.createTime) : '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="本次实付金额">
            <span class="font-bold text-red-600">￥{{ formatAmount(detail.payAmount) }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="预付抵扣金额">
            <span class="font-bold text-orange-600">￥{{ formatAmount(detail.prepayUsedAmount || 0) }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="支付合计" :span="2">
            <span class="font-bold text-blue-600 text-base">
              ￥{{ formatAmount(Number(detail.payAmount || 0) + Number(detail.prepayUsedAmount || 0)) }}
            </span>
          </el-descriptions-item>

          <el-descriptions-item label="备注说明" :span="2">
            {{ detail.remark || '暂无备注' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 核销申请明细列表 -->
        <div class="mb-2">
          <div class="text-13px font-bold text-gray-700 mb-2">
            核销申请项明细（共 {{ (detail.items || []).length }} 笔）
          </div>
          <el-table :data="detail.items || []" border size="small" stripe>
            <el-table-column label="核销申请单号" prop="applyNo" min-width="150">
              <template #default="{ row }">
                <span class="font-mono text-blue-600">{{ row.applyNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="核销对象类型" prop="refType" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.refType || 'PAY_APPLY' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核销分配金额" prop="allocateAmount" min-width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold text-green-600">￥{{ formatAmount(row.allocateAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { formatDate } from '@/utils/formatTime'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type { PaymentVO } from '@/api/lunaerp-elmui/finance/payment'
import { formatAmount, DEFAULT_FINANCE_ACCOUNTS } from '../utils'

const visible = ref(false)
const loading = ref(false)
const detail = ref<PaymentVO | null>(null)

const accountLabel = computed(() => {
  if (!detail.value) return '—'
  const acc = DEFAULT_FINANCE_ACCOUNTS.find((a) => a.id === detail.value?.accountId)
  return acc ? acc.name : `账户 #${detail.value.accountId}`
})

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    detail.value = await PaymentApi.getPayment(id)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
