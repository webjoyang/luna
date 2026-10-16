<template>
  <el-dialog
    v-model="visible"
    title="选择待核销应收分录（发货单/合同视角）"
    width="920px"
    destroy-on-close
    append-to-body
  >
    <div v-loading="loading">
      <!-- 顶部搜索与统计区 -->
      <div class="flex items-center justify-between mb-14px">
        <div class="flex items-center space-x-12px">
          <el-input
            v-model="keyword"
            placeholder="搜索发货单号 / 合同编号 / 产品摘要"
            clearable
            class="!w-320px"
          >
            <template #prefix>
              <Icon icon="ep:search" />
            </template>
          </el-input>

          <el-radio-group v-model="perspectiveFilter" size="small">
            <el-radio-button label="ALL">全部明细</el-radio-button>
            <el-radio-button label="STATEMENT">对账单视角</el-radio-button>
            <el-radio-button label="SHIPMENT">发货单视角</el-radio-button>
            <el-radio-button label="CONTRACT">合同视角</el-radio-button>
          </el-radio-group>
        </div>

        <div class="text-13px text-gray-500">
          已选 <span class="font-bold text-primary">{{ currentSelection.length }}</span> 笔，
          待核销合计：
          <span class="font-mono font-bold text-emerald-700">
            ¥ {{ formatMoney(selectedTotalUnallocated) }}
          </span>
        </div>
      </div>

      <!-- 待核销分录表格 -->
      <el-table
        ref="tableRef"
        :data="filteredEntries"
        border
        stripe
        size="small"
        max-height="420px"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" align="center" :selectable="canSelectEntry" />
        <el-table-column label="业务日期" prop="bizDate" width="105" align="center" sortable />
        <el-table-column
          v-if="perspectiveFilter === 'STATEMENT' || hasStatementEntries"
          label="对账单号"
          prop="statementNo"
          min-width="130"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="font-mono text-blue-600">{{ row.statementNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发货单号" prop="refNo" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono font-medium">{{ row.refNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="合同编号" prop="contractNo" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono text-gray-700">{{ row.contractNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="产品摘要"
          prop="productName"
          min-width="130"
          show-overflow-tooltip
        />
        <el-table-column label="币种" prop="currency" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原单金额" prop="originalAmount" width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-gray-500">¥ {{ formatMoney(row.originalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已核销" prop="allocatedAmount" width="100" align="right">
          <template #default="{ row }">
            <span class="font-mono text-gray-400">¥ {{ formatMoney(row.allocatedAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="未核销余额"
          prop="unallocatedAmount"
          width="120"
          align="right"
          sortable
        >
          <template #default="{ row }">
            <span class="font-mono font-bold text-emerald-700">
              ¥ {{ formatMoney(row.unallocatedAmount) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <div class="flex items-center justify-between">
        <span class="text-12px text-gray-400">
          系统将按选定分录导入至收款单核销明细表，可进行手动微调或一键 FIFO 自动分配
        </span>
        <div class="space-x-8px">
          <el-button @click="visible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="currentSelection.length === 0"
            v-hasPermi="['lunaerp:finance:payment:create', 'lunaerp:finance:payment:update']"
            @click="handleConfirm"
          >
            确认选入 ({{ currentSelection.length }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getArBalanceList, type ArBalanceRespVO } from '@/api/lunaerp-elmui/finance/payment'
import { formatMoney } from '../model/payment'

defineOptions({ name: 'ReceiptAllocateSelectDialog' })

const emit = defineEmits<{
  (e: 'select', items: ArBalanceRespVO[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const keyword = ref('')
const perspectiveFilter = ref<'ALL' | 'STATEMENT' | 'SHIPMENT' | 'CONTRACT'>('ALL')
const allEntries = ref<ArBalanceRespVO[]>([])
const currentSelection = ref<ArBalanceRespVO[]>([])
const existingEntryIds = ref<Set<number>>(new Set())

const hasStatementEntries = computed(() => {
  return allEntries.value.some((item) => !!item.statementNo)
})

const filteredEntries = computed(() => {
  let list = allEntries.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      (item) =>
        (item.refNo && item.refNo.toLowerCase().includes(kw)) ||
        (item.contractNo && item.contractNo.toLowerCase().includes(kw)) ||
        (item.statementNo && item.statementNo.toLowerCase().includes(kw)) ||
        (item.productName && item.productName.toLowerCase().includes(kw))
    )
  }

  if (perspectiveFilter.value === 'STATEMENT') {
    list = list.filter((item) => !!item.statementNo)
  } else if (perspectiveFilter.value === 'SHIPMENT') {
    // 发货单聚合视角：按发货单号聚合归类
    list = [...list].sort((a, b) => (a.refNo || '').localeCompare(b.refNo || ''))
  } else if (perspectiveFilter.value === 'CONTRACT') {
    // 销售合同聚合视角：按合同单号聚合归类
    list = [...list].sort((a, b) => (a.contractNo || '').localeCompare(b.contractNo || ''))
  }

  return list
})

const selectedTotalUnallocated = computed(() => {
  return currentSelection.value.reduce((sum, item) => sum + Number(item.unallocatedAmount || 0), 0)
})

const canSelectEntry = (row: ArBalanceRespVO) => {
  // 若已被父页面选中，则不可重复勾选
  return !existingEntryIds.value.has(row.entryId)
}

const handleSelectionChange = (val: ArBalanceRespVO[]) => {
  currentSelection.value = val
}

const open = async (customerId: number, currency: string, chosenEntryIds: number[] = []) => {
  if (!customerId) {
    ElMessage.warning('请先选择收款客户')
    return
  }
  visible.value = true
  keyword.value = ''
  perspectiveFilter.value = 'ALL'
  currentSelection.value = []
  existingEntryIds.value = new Set(chosenEntryIds)
  loading.value = true

  try {
    const list = await getArBalanceList(customerId, currency || 'RMB')
    allEntries.value = list || []
  } catch (err: any) {
    ElMessage.error(err.message || '加载待核销分录失败')
    allEntries.value = []
  } finally {
    loading.value = false
  }
}

const handleConfirm = () => {
  if (currentSelection.value.length === 0) {
    ElMessage.warning('请至少勾选一笔待核销分录')
    return
  }
  emit('select', [...currentSelection.value])
  visible.value = false
}

defineExpose({ open })
</script>
