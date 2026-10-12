<template>
  <el-drawer v-model="visible" title="付款申请详情" size="600px" destroy-on-close>
    <div v-loading="loading" class="p-2">
      <template v-if="detail">
        <div class="mb-4 flex items-center justify-between pb-3 border-b border-[#e2e8f0]">
          <div>
            <span class="text-xs text-gray-500 block mb-1">申请单号</span>
            <span class="text-lg font-bold font-mono text-blue-600">{{ detail.applyNo }}</span>
          </div>
          <div class="flex items-center gap-2">
            <dict-tag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="detail.applyType" />
            <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="detail.approvalStatus" />
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="供应商名称" :span="2">
            {{ detail.supplierName || '—' }}
          </el-descriptions-item>

          <el-descriptions-item v-if="detail.applyType === 'RECON'" label="关联对账单" :span="2">
            <span class="font-mono text-blue-600">{{ detail.reconNo || '—' }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="申请金额">
            <span class="font-bold text-red-600">￥{{ formatAmount(detail.amount) }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="已核销金额">
            <span class="font-bold text-green-600">￥{{ formatAmount(detail.allocatedAmount || 0) }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="未核销余额" :span="2">
            <span class="font-bold text-orange-600">
              ￥{{ formatAmount(Math.max(0, detail.amount - (detail.allocatedAmount || 0))) }}
            </span>
          </el-descriptions-item>

          <template v-if="detail.applyType === 'PREPAY'">
            <el-descriptions-item label="预付用途" :span="2">
              {{ detail.purpose || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="收款开户行">
              {{ detail.bankName || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="收款账号">
              {{ detail.bankAccount || '—' }}
            </el-descriptions-item>
          </template>

          <el-descriptions-item label="创建时间" :span="2">
            {{ detail.createTime ? formatDate(detail.createTime) : '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="备注说明" :span="2">
            {{ detail.remark || '暂无备注' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 引导动作 -->
        <div
          v-if="detail.approvalStatus === 'APPROVED' && (detail.amount - (detail.allocatedAmount || 0)) > 0"
          class="p-3 bg-green-50 border border-green-200 rounded flex items-center justify-between"
        >
          <div class="text-xs text-green-700">
            该申请已通过审批，尚有 ￥{{ formatAmount(detail.amount - (detail.allocatedAmount || 0)) }} 待核销。
          </div>
          <el-button type="success" size="small" @click="handleGoToRegister">
            前往付款登记
          </el-button>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type { PaymentApplyVO } from '@/api/lunaerp-elmui/finance/payment'
import { formatAmount } from '../utils'

const emit = defineEmits(['go-to-register'])

const visible = ref(false)
const loading = ref(false)
const detail = ref<PaymentApplyVO | null>(null)

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    detail.value = await PaymentApi.getPaymentApply(id)
  } finally {
    loading.value = false
  }
}

const handleGoToRegister = () => {
  visible.value = false
  if (detail.value) {
    emit('go-to-register', detail.value)
  }
}

defineExpose({ open })
</script>
