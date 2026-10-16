<template>
  <el-dialog v-model="visible" title="预收转核销" width="960px" destroy-on-close append-to-body>
    <div v-loading="loading">
      <!-- 预收款单基本信息提示卡片 -->
      <div class="bg-gray-50 border border-gray-200 rounded p-12px mb-14px text-13px text-gray-700">
        <div class="grid grid-cols-3 gap-y-8px gap-x-16px">
          <div>
            <span class="text-gray-500">收款单号：</span>
            <span class="font-mono font-medium text-gray-900">{{ receiptRow?.paymentNo }}</span>
          </div>
          <div>
            <span class="text-gray-500">客户名称：</span>
            <span class="font-medium text-gray-900">{{ receiptRow?.customerName || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-500">币种：</span>
            <el-tag size="small">{{ receiptRow?.currency || 'RMB' }}</el-tag>
          </div>
          <div>
            <span class="text-gray-500">预收总额：</span>
            <span class="font-mono font-bold text-gray-900">
              ¥ {{ formatMoney(receiptRow?.payAmount) }}
            </span>
          </div>
          <div>
            <span class="text-gray-500">已核销金额：</span>
            <span class="font-mono text-gray-600">
              ¥ {{ formatMoney(receiptRow?.allocatedTotalAmount) }}
            </span>
          </div>
          <div>
            <span class="text-gray-500">预收可用余额：</span>
            <span class="font-mono font-bold text-emerald-700">
              ¥ {{ formatMoney(availableAdvanceBalance) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 操作与统计栏 -->
      <div class="flex items-center justify-between mb-12px">
        <div class="flex items-center space-x-10px">
          <el-input
            v-model="keyword"
            placeholder="搜索发货单号 / 合同编号 / 品名摘要"
            clearable
            class="!w-280px"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>

          <el-button type="primary" plain size="small" @click="handleAutoAllocate">
            <Icon icon="ep:magic-stick" class="mr-2px" /> FIFO 自动分配
          </el-button>
          <el-button size="small" @click="handleClearAllocations">
            <Icon icon="ep:brush" class="mr-2px" /> 清空分配
          </el-button>
        </div>

        <div class="text-13px text-gray-500">
          本次转核销合计：
          <span
            class="font-mono font-bold text-15px"
            :class="totalAllocating > availableAdvanceBalance ? 'text-red-600' : 'text-emerald-700'"
          >
            ¥ {{ formatMoney(totalAllocating) }}
          </span>
          <span class="ml-8px text-12px text-gray-400">
            (剩余预收: ¥ {{ formatMoney(Math.max(0, availableAdvanceBalance - totalAllocating)) }})
          </span>
        </div>
      </div>

      <!-- 待核销分录列表表格 -->
      <el-table :data="filteredCandidates" border stripe size="small" max-height="400px">
        <el-table-column label="#" type="index" width="45" align="center" />
        <el-table-column label="业务日期" prop="bizDate" width="100" align="center" sortable />
        <el-table-column label="发货单号" prop="refNo" min-width="130">
          <template #default="{ row }">
            <span class="font-mono text-gray-800">{{ row.refNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="销售合同" prop="contractNo" min-width="120">
          <template #default="{ row }">
            <span class="text-gray-700">{{ row.contractNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="品名/事由"
          prop="productName"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column label="原金额" prop="originalAmount" min-width="100" align="right">
          <template #default="{ row }">
            <span class="font-mono text-gray-600">¥ {{ formatMoney(row.originalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="未核销余额" prop="unallocatedAmount" min-width="105" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-amber-700">
              ¥ {{ formatMoney(row.unallocatedAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="本次核销金额" width="160" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="allocateMap[row.entryId]"
              :min="0"
              :max="Number(row.unallocatedAmount || 0)"
              :precision="2"
              :step="100"
              size="small"
              class="!w-135px"
              placeholder="0.00"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <div class="flex items-center justify-between">
        <div class="text-12px text-gray-400">
          * 预收转核销在原 ADVANCE 单上追加核销行，无需审批，提交后实时落库。
        </div>
        <div class="space-x-8px">
          <el-button @click="visible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="totalAllocating <= 0 || totalAllocating > availableAdvanceBalance"
            v-hasPermi="['lunaerp:finance:payment:writeoff']"
            @click="handleSubmit"
          >
            确定转核销 (¥ {{ formatMoney(totalAllocating) }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getArBalanceList,
  advanceWriteoff,
  type ArBalanceRespVO,
  type ReceiptRespVO
} from '@/api/lunaerp-elmui/finance/payment'
import {
  formatMoney,
  parsePaymentErrorMessage,
  validateAdvanceWriteoff,
  distributeReceiptFifo
} from '../model/payment'

const props = defineProps<{
  modelValue: boolean
  receiptRow: ReceiptRespVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
})

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const candidates = ref<ArBalanceRespVO[]>([])
const allocateMap = ref<Record<number, number>>({})

// 预收可用余额
const availableAdvanceBalance = computed(() => {
  if (!props.receiptRow) return 0
  if (
    props.receiptRow.unallocatedAmount !== undefined &&
    props.receiptRow.unallocatedAmount !== null
  ) {
    return Number(props.receiptRow.unallocatedAmount)
  }
  const pay = Number(props.receiptRow.payAmount || 0)
  const alloc = Number(props.receiptRow.allocatedTotalAmount || 0)
  return Math.max(0, Number((pay - alloc).toFixed(2)))
})

// 本次核销分配合计
const totalAllocating = computed(() => {
  let sum = 0
  for (const entryId in allocateMap.value) {
    const val = Number(allocateMap.value[entryId] || 0)
    if (val > 0) {
      sum = Number((sum + val).toFixed(2))
    }
  }
  return sum
})

// 过滤候选分录
const filteredCandidates = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return candidates.value
  return candidates.value.filter((e) => {
    return (
      (e.refNo && e.refNo.toLowerCase().includes(kw)) ||
      (e.contractNo && e.contractNo.toLowerCase().includes(kw)) ||
      (e.productName && e.productName.toLowerCase().includes(kw))
    )
  })
})

// 加载待核销分录
const loadCandidates = async () => {
  if (!props.receiptRow || !props.receiptRow.customerId) return
  loading.value = true
  allocateMap.value = {}
  try {
    const res = await getArBalanceList(
      props.receiptRow.customerId,
      props.receiptRow.currency || 'RMB'
    )
    candidates.value = res || []
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    loading.value = false
  }
}

// 自动分配 FIFO
const handleAutoAllocate = () => {
  const unallocBal = availableAdvanceBalance.value
  if (unallocBal <= 0) {
    ElMessage.warning('当前收款单预收可用余额为 0')
    return
  }

  const entriesToDistribute = candidates.value.map((e) => ({
    entryId: e.entryId,
    unallocatedAmount: Number(e.unallocatedAmount || 0),
    bizDate: e.bizDate
  }))

  const allocatedList = distributeReceiptFifo(entriesToDistribute, unallocBal)
  const newMap: Record<number, number> = {}
  for (const item of allocatedList) {
    if (item.allocateAmount > 0) {
      newMap[item.entryId] = item.allocateAmount
    }
  }
  allocateMap.value = newMap
  ElMessage.success('已完成 FIFO 自动分配凑平')
}

// 清空分配
const handleClearAllocations = () => {
  allocateMap.value = {}
}

// 提交预收转核销
const handleSubmit = async () => {
  if (!props.receiptRow) return

  const items = []
  for (const entryIdStr in allocateMap.value) {
    const entryId = Number(entryIdStr)
    const alloc = Number(allocateMap.value[entryId] || 0)
    if (alloc > 0) {
      const matched = candidates.value.find((c) => c.entryId === entryId)
      items.push({
        entryId,
        entryNo: matched?.refNo || String(entryId),
        allocateAmount: alloc,
        unallocatedAmount: Number(matched?.unallocatedAmount || 0)
      })
    }
  }

  const validRes = validateAdvanceWriteoff({
    advanceBalance: availableAdvanceBalance.value,
    items
  })

  if (!validRes.valid) {
    ElMessage.warning(validRes.error || '校验失败')
    return
  }

  submitting.value = true
  try {
    await advanceWriteoff({
      paymentId: props.receiptRow.id,
      items: items.map((i) => ({
        entryId: i.entryId,
        allocateAmount: i.allocateAmount
      }))
    })
    ElMessage.success('预收转核销成功')
    visible.value = false
    emit('success')
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      keyword.value = ''
      loadCandidates()
    }
  }
)
</script>
