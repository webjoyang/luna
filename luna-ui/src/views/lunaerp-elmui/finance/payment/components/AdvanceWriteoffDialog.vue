<template>
  <el-dialog
    v-model="visible"
    title="客户预收转核销"
    width="920px"
    append-to-body
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-loading="loading">
      <!-- 预收款单基本信息卡片 -->
      <div v-if="payment" class="mb-4 p-4 bg-[#f8fafc] border border-[#e2e8f0] rounded">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="预收款单号">
            <span class="font-mono text-blue-600 font-bold">{{ payment.paymentNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ payment.customerName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="币种">
            <el-tag size="small">{{ payment.currency }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="原预收总额">
            <span class="font-bold">￥{{ formatAmount(payment.payAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="当前可用预收余额" :span="2">
            <span class="font-bold text-green-600 text-base">￥{{ formatAmount(advanceBalance) }}</span>
            <span class="text-xs text-gray-400 ml-2">（可用于抵扣同币种应收分录）</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 核销分录分配明细 -->
      <div class="mb-4 p-3 bg-white border border-[#e2e8f0] rounded">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <span class="font-bold text-sm text-[#1e293b]">拟转核销应收分录（共 {{ items.length }} 笔）</span>
          </div>
          <div class="flex items-center gap-2">
            <el-button
              type="primary"
              size="small"
              plain
              @click="openSelectArDialog"
            >
              <Icon icon="ep:plus" class="mr-2px" />选择应收分录
            </el-button>
            <el-button
              size="small"
              :disabled="items.length === 0"
              @click="handleClearItems"
            >
              清空
            </el-button>
          </div>
        </div>

        <el-table :data="items" border size="small" stripe max-height="260">
          <el-table-column label="分录单号" prop="entryNo" min-width="140">
            <template #default="{ row }">
              <span class="font-mono text-blue-600">{{ row.entryNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="业务日期" prop="bizDate" width="110" align="center">
            <template #default="{ row }">{{ row.bizDate || '—' }}</template>
          </el-table-column>
          <el-table-column label="关联单据" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.shipmentNo" class="font-mono text-xs block text-gray-700">发货: {{ row.shipmentNo }}</span>
              <span v-if="row.contractNo" class="font-mono text-xs block text-gray-500">合同: {{ row.contractNo }}</span>
              <span v-if="!row.shipmentNo && !row.contractNo" class="text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column label="未核销余额" width="120" align="right">
            <template #default="{ row }">
              <span class="font-mono text-gray-700">￥{{ formatAmount(row.unallocatedBalance) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="本次核销金额" width="160" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocateAmount"
                :min="0.01"
                :max="row.unallocatedBalance || undefined"
                :precision="2"
                :step="100"
                size="small"
                class="!w-140px"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="handleRemoveItem($index)">
                移除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无核销分录，请点击「选择应收分录」添加" :image-size="60" />
          </template>
        </el-table>

        <!-- 金额合计与校验提示 -->
        <div class="mt-3 flex items-center justify-between text-xs bg-[#f8fafc] p-2 rounded border border-[#e2e8f0]">
          <div class="flex items-center gap-4">
            <span>可用预收：<strong class="text-green-600 text-sm">￥{{ formatAmount(advanceBalance) }}</strong></span>
            <span>本次核销合计：<strong class="text-blue-600 text-sm">￥{{ formatAmount(totalAllocated) }}</strong></span>
            <span>
              转核销后剩余：
              <strong :class="remainingAdvance < 0 ? 'text-red-600 text-sm' : 'text-gray-700 text-sm'">
                ￥{{ formatAmount(remainingAdvance) }}
              </strong>
            </span>
          </div>
          <div v-if="totalAllocated > advanceBalance" class="text-red-600 font-bold">
            ⚠ 本次核销总额超出可用预收余额！
          </div>
        </div>
      </div>

      <el-form label-width="80px">
        <el-form-item label="备注说明">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="可填写预收转核销备注说明"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="items.length === 0 || totalAllocated > advanceBalance"
        @click="handleSubmit"
      >
        确认转核销
      </el-button>
    </template>
  </el-dialog>

  <!-- 应收分录选择弹窗 -->
  <ArEntrySelectDialog ref="arSelectDialogRef" @select="handleArSelect" />
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type {
  PaymentVO,
  ReceiptRefSaveVO,
  ArBalanceRespVO
} from '@/api/lunaerp-elmui/finance/payment'
import { formatAmount } from '../utils'
import ArEntrySelectDialog from './ArEntrySelectDialog.vue'

defineOptions({ name: 'AdvanceWriteoffDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const payment = ref<PaymentVO | null>(null)
const advanceBalance = ref(0)
const remark = ref('')
const arSelectDialogRef = ref()

interface ExtendedRefVO extends ReceiptRefSaveVO {
  unallocatedBalance?: number
  shipmentNo?: string
  contractNo?: string
}

const items = ref<ExtendedRefVO[]>([])

const totalAllocated = computed(() => {
  return items.value.reduce((sum, it) => sum + (Number(it.allocateAmount) || 0), 0)
})

const remainingAdvance = computed(() => {
  return Number((advanceBalance.value - totalAllocated.value).toFixed(2))
})

const open = async (row: PaymentVO) => {
  visible.value = true
  loading.value = true
  payment.value = row
  remark.value = ''
  items.value = []
  try {
    advanceBalance.value = await PaymentApi.getAdvanceBalance(row.id)
  } catch (e) {
    ElMessage.error('加载预收余额失败')
  } finally {
    loading.value = false
  }
}

const openSelectArDialog = () => {
  if (!payment.value) return
  const excludeIds = items.value.map((it) => it.ledgerEntryId)
  arSelectDialogRef.value?.open(payment.value.customerId!, payment.value.currency || 'CNY', excludeIds)
}

const handleArSelect = (selectedList: ArBalanceRespVO[]) => {
  for (const item of selectedList) {
    const curAllocated = totalAllocated.value
    const remainingToAllocate = Math.max(0, advanceBalance.value - curAllocated)
    const initAlloc = Math.min(Number(item.unallocatedBalance || 0), remainingToAllocate)

    items.value.push({
      ledgerEntryId: item.ledgerEntryId,
      entryNo: item.entryNo,
      bizDate: item.bizDate,
      allocateAmount: initAlloc > 0 ? Number(initAlloc.toFixed(2)) : Number(item.unallocatedBalance || 0),
      unallocatedBalance: item.unallocatedBalance,
      shipmentNo: item.shipmentNo,
      contractNo: item.contractNo
    })
  }
}

const handleClearItems = () => {
  items.value = []
}

const handleRemoveItem = (index: number) => {
  items.value.splice(index, 1)
}

const handleSubmit = async () => {
  if (!payment.value) return
  if (items.value.length === 0) {
    ElMessage.warning('请至少添加一条核销分录')
    return
  }
  if (totalAllocated.value > advanceBalance.value) {
    ElMessage.warning('本次核销总额不能超出可用预收余额！')
    return
  }

  for (const it of items.value) {
    if (!it.allocateAmount || it.allocateAmount <= 0) {
      ElMessage.warning('核销分配金额必须大于 0！')
      return
    }
  }

  submitting.value = true
  try {
    await PaymentApi.advanceWriteoff({
      paymentId: payment.value.id,
      remark: remark.value,
      items: items.value.map((it) => ({
        ledgerEntryId: it.ledgerEntryId,
        allocateAmount: it.allocateAmount,
        entryNo: it.entryNo,
        bizDate: it.bizDate
      }))
    })
    ElMessage.success('预收转核销成功')
    visible.value = false
    emit('success')
  } catch (e: any) {
    ElMessage.error(e?.message || '预收转核销失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
