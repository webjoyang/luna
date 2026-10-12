<template>
  <el-dialog
    v-model="visible"
    title="客户收款单详情"
    width="820px"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="mb-4 flex items-center justify-between pb-3 border-b border-[#e2e8f0]">
          <div>
            <span class="text-xs text-gray-500 block mb-1">收款单号</span>
            <span class="text-lg font-bold font-mono text-blue-600">{{ detail.paymentNo }}</span>
          </div>
          <div class="flex items-center gap-2">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_RECEIPT_TYPE" :value="detail.receiptType || ''" />
            <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="detail.approvalStatus || ''" />
            <el-tag v-if="detail.status === 'COMPLETED'" type="success" size="small">已过账入账</el-tag>
            <el-tag v-else-if="detail.status === 'REVERSED'" type="danger" size="small">已红冲</el-tag>
            <el-tag v-else type="info" size="small">未过账</el-tag>
          </div>
        </div>

        <el-descriptions :column="2" border size="small" class="mb-4">
          <el-descriptions-item label="客户名称">
            {{ detail.customerName || '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="币种">
            <el-tag size="small">{{ detail.currency || 'CNY' }}</el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="收款金额">
            <span class="font-bold text-blue-600 text-base">￥{{ formatAmount(detail.payAmount) }}</span>
          </el-descriptions-item>

          <el-descriptions-item label="收款资金账户">
            {{ detail.accountName || `账户 #${detail.accountId}` }}
          </el-descriptions-item>

          <el-descriptions-item label="收款日期">
            {{ detail.receiptDate || '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="经手人">
            {{ detail.handlerName || '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="银行水单号">
            {{ detail.bankRefNo || '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="创建时间">
            {{ detail.createTime ? formatDate(detail.createTime) : '—' }}
          </el-descriptions-item>

          <el-descriptions-item label="备注说明" :span="2">
            {{ detail.remark || '暂无备注' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 核销分录明细列表 -->
        <div class="mb-2">
          <div class="text-13px font-bold text-gray-700 mb-2 flex items-center justify-between">
            <span>核销应收分录明细（共 {{ (detail.items || []).length }} 笔）</span>
            <span class="text-xs text-green-600">
              核销总额：￥{{ formatAmount(totalAllocated) }}
            </span>
          </div>

          <el-table :data="detail.items || []" border size="small" stripe>
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column label="核销分录单号" prop="entryNo" min-width="160">
              <template #default="{ row }">
                <span class="font-mono text-blue-600">{{ row.entryNo || `分录 #${row.refId}` }}</span>
              </template>
            </el-table-column>
            <el-table-column label="业务日期" prop="bizDate" width="120" align="center">
              <template #default="{ row }">{{ row.bizDate || '—' }}</template>
            </el-table-column>
            <el-table-column label="核销金额" prop="allocateAmount" min-width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold text-green-600">￥{{ formatAmount(row.allocateAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="本币核销金额" prop="baseAmount" min-width="120" align="right">
              <template #default="{ row }">
                <span class="font-mono text-gray-700">￥{{ formatAmount(row.baseAmount || row.allocateAmount) }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前收款单无核销明细（预收款或未分配）" :image-size="60" />
            </template>
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
import { ElMessage } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type { PaymentVO } from '@/api/lunaerp-elmui/finance/payment'
import { formatAmount } from '../utils'

defineOptions({ name: 'ReceiptDetailDialog' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<PaymentVO | null>(null)

const totalAllocated = computed(() => {
  if (!detail.value || !detail.value.items) return 0
  return detail.value.items.reduce((sum, it) => sum + (Number(it.allocateAmount) || 0), 0)
})

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    detail.value = await PaymentApi.getPayment(id)
  } catch (e) {
    ElMessage.error('加载收款单详情失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
