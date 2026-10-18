<template>
  <el-drawer
    v-model="visible"
    :title="`客户对账单详情 - ${detail?.statementNo || ''}`"
    size="75%"
    destroy-on-close
    direction="rtl"
  >
    <div v-loading="loading" class="detail-container p-4">
      <!-- 1. 顶部操作栏与状态 -->
      <div class="flex justify-between items-center mb-16px pb-12px border-b border-gray-200">
        <div class="flex items-center gap-3">
          <span class="text-18px font-bold font-mono">{{ detail?.statementNo }}</span>
          <el-tag
            v-if="detail?.status"
            :type="formatStatementStatusTag(detail.status).type"
            size="default"
          >
            {{ formatStatementStatusTag(detail.status).label }}
          </el-tag>
        </div>

        <div class="action-buttons flex items-center gap-2">
          <!-- 草稿态操作按钮 -->
          <template v-if="getCustomerStatementActionFlags(detail?.status).canEdit">
            <el-button
              type="primary"
              plain
              @click="handleOpenAddEntry"
              v-hasPermi="['lunaerp:finance:customerrecon:update']"
            >
              <Icon icon="ep:plus" class="mr-1" /> 追加分录
            </el-button>
            <el-button
              type="primary"
              plain
              @click="handleEditRemark"
              v-hasPermi="['lunaerp:finance:customerrecon:update']"
            >
              <Icon icon="ep:edit" class="mr-1" /> 编辑备注
            </el-button>
          </template>

          <!-- 发送按钮：仅 DRAFT -->
          <el-button
            v-if="getCustomerStatementActionFlags(detail?.status).canSend"
            type="primary"
            @click="handleSend"
            v-hasPermi="['lunaerp:finance:customerrecon:send']"
          >
            <Icon icon="ep:position" class="mr-1" /> 发送客户
          </el-button>

          <!-- 代客确认按钮：仅 SENT -->
          <el-button
            v-if="getCustomerStatementActionFlags(detail?.status).canConfirmOnBehalf"
            type="success"
            @click="handleConfirmOnBehalf"
            v-hasPermi="['lunaerp:finance:customerrecon:confirm-on-behalf']"
          >
            <Icon icon="ep:check" class="mr-1" /> 代客确认
          </el-button>

          <!-- 驳回按钮：仅 SENT -->
          <el-button
            v-if="getCustomerStatementActionFlags(detail?.status).canReject"
            type="warning"
            plain
            @click="handleReject"
            v-hasPermi="['lunaerp:finance:customerrecon:update']"
          >
            <Icon icon="ep:close" class="mr-1" /> 驳回单据
          </el-button>

          <!-- 异议回复按钮：仅 REJECTED -->
          <el-button
            v-if="getCustomerStatementActionFlags(detail?.status).canRejectReply"
            type="primary"
            plain
            @click="handleRejectReply"
            v-hasPermi="['lunaerp:finance:customerrecon:update']"
          >
            <Icon icon="ep:chat-dot-round" class="mr-1" /> 异议回复
          </el-button>

          <!-- 打印按钮：非 VOID -->
          <PrintDispatcher
            v-if="getCustomerStatementActionFlags(detail?.status).canPrint && detail?.id"
            biz-type="FINANCE_STATEMENT"
            :biz-id="detail.id"
            class="inline-block"
          >
            <template #default="{ open: openPrint }">
              <el-button
                type="primary"
                plain
                @click="openPrint()"
                v-hasPermi="['lunaerp:finance:customerrecon:print']"
              >
                <Icon icon="ep:printer" class="mr-1" /> 打印
              </el-button>
            </template>
          </PrintDispatcher>

          <!-- 作废单据：非终态（DRAFT / SENT / REJECTED） -->
          <el-button
            v-if="getCustomerStatementActionFlags(detail?.status).canVoid"
            type="warning"
            plain
            @click="handleVoid"
            v-hasPermi="['lunaerp:finance:customerrecon:void']"
          >
            <Icon icon="ep:circle-close" class="mr-1" /> 作废单据
          </el-button>

          <!-- 删除草稿：仅 DRAFT -->
          <el-button
            v-if="getCustomerStatementActionFlags(detail?.status).canDelete"
            type="danger"
            plain
            @click="handleDelete"
            v-hasPermi="['lunaerp:finance:customerrecon:delete']"
          >
            <Icon icon="ep:delete" class="mr-1" /> 删除草稿
          </el-button>
        </div>
      </div>

      <!-- 异议警示条（若有驳回原因） -->
      <el-alert
        v-if="detail?.rejectReason"
        type="error"
        :closable="false"
        class="mb-16px"
        show-icon
        title="客户异议驳回"
        :description="`驳回原因：${detail.rejectReason}`"
      />

      <!-- 2. 四大汇总指标卡片 -->
      <div class="grid grid-cols-4 gap-3 mb-16px p-4 bg-gray-50 rounded-lg border border-gray-100">
        <div class="text-center">
          <div class="text-12px text-gray-500 mb-1">期初欠款余额</div>
          <div class="text-18px font-bold font-mono text-gray-800">
            ¥ {{ formatMoney(detail?.openingBalance) }}
          </div>
        </div>
        <div class="text-center border-l border-gray-200">
          <div class="text-12px text-blue-600 mb-1">本期应收（借方合计）</div>
          <div class="text-18px font-bold font-mono text-blue-600">
            ¥ {{ formatMoney(detail?.periodDebit) }}
          </div>
        </div>
        <div class="text-center border-l border-gray-200">
          <div class="text-12px text-green-600 mb-1">本期核销（贷方合计）</div>
          <div class="text-18px font-bold font-mono text-green-600">
            ¥ {{ formatMoney(detail?.periodCredit) }}
          </div>
        </div>
        <div class="text-center border-l border-gray-200">
          <div class="text-12px text-purple-600 mb-1">期末应收余额</div>
          <div class="text-18px font-bold font-mono text-purple-700">
            ¥ {{ formatMoney(detail?.closingBalance) }}
          </div>
        </div>
      </div>

      <!-- 3. 基本信息卡片 -->
      <el-descriptions title="基本信息" :column="3" border class="mb-20px">
        <el-descriptions-item label="对账单号">
          <span class="font-mono font-medium">{{ detail?.statementNo || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">
          <span class="font-bold text-gray-800">{{ detail?.customerName || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="对账币种">
          <span class="font-medium">{{ detail?.currency || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="对账区间" :span="2">
          <span class="font-medium">
            {{ detail?.periodStart || '—' }} 至 {{ detail?.periodEnd || '—' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          <span class="text-12px">{{ detail?.createTime || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="确认方式" v-if="detail?.confirmType">
          <el-tag size="small" type="success">
            {{ detail.confirmType === 'ONLINE_PORTAL' ? '客户在线确认' : '我方代办确认' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="确认人" v-if="detail?.confirmByName || detail?.confirmedBy">
          <span>{{ detail.confirmByName || detail.confirmedBy }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="确认时间" v-if="detail?.confirmTime || detail?.confirmedAt">
          <span class="text-12px">{{ detail.confirmTime || detail.confirmedAt }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="代办方式" v-if="detail?.confirmMethod">
          <el-tag size="small" type="info">{{ detail.confirmMethod }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="异议回复" v-if="detail?.replyRemark" :span="2">
          <span class="text-blue-600">{{ detail.replyRemark }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注说明" :span="3">
          <span>{{ detail?.remark || '—' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 4. 对账单明细分录表格 -->
      <div class="mb-2 flex justify-between items-center">
        <span class="text-14px font-bold text-gray-700">
          对账分录明细（共 {{ detail?.items?.length || 0 }} 笔）
        </span>
      </div>

      <el-table
        :data="detail?.items || []"
        border
        stripe
        size="small"
        max-height="450px"
        :row-class-name="itemRowClassName"
      >
        <el-table-column type="index" label="序号" width="50" align="center" />
        <el-table-column label="业务日期" prop="bizDate" width="105" align="center" />
        <el-table-column label="分录类型" prop="entryType" width="130" show-overflow-tooltip />
        <el-table-column label="源单号" prop="refNo" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono font-medium">{{ row.refNo || '—' }}</span>
            <el-tag v-if="row.hasReversal" type="danger" size="small" effect="dark" class="ml-1">
              已红冲
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="摘要说明" prop="summary" min-width="160" show-overflow-tooltip />
        <el-table-column label="借方发生" prop="debitAmount" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.debitAmount > 0" class="font-mono font-medium text-blue-600">
              ¥{{ formatMoney(row.debitAmount) }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方核销" prop="creditAmount" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.creditAmount > 0" class="font-mono font-medium text-green-600">
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
        <el-table-column label="行备注" prop="remark" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="getCustomerStatementActionFlags(detail?.status).canEdit"
          label="操作"
          width="80"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              size="small"
              @click="handleRemoveItem(row.id)"
              v-hasPermi="['lunaerp:finance:customerrecon:update']"
            >
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 子弹窗组件 -->
    <StatementAddEntryDialog ref="addEntryDialogRef" @success="handleSubSuccess" />
    <StatementEditDialog ref="editDialogRef" @success="handleSubSuccess" />
    <StatementConfirmOnBehalfDialog ref="confirmOnBehalfDialogRef" @success="handleSubSuccess" />
    <StatementRejectDialog ref="rejectDialogRef" @success="handleSubSuccess" />
    <StatementRejectReplyDialog ref="rejectReplyDialogRef" @success="handleSubSuccess" />
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCustomerStatement,
  deleteCustomerStatement,
  updateCustomerStatement,
  sendCustomerStatement,
  voidCustomerStatement,
  type CustomerStatementDetailRespVO,
  type CustomerStatementItemRespVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import {
  formatMoney,
  formatSignStatusTag,
  formatStatementStatusTag,
  getCustomerStatementActionFlags
} from '../model/customerStatement'
import { PrintDispatcher } from '@/components/Print'
import StatementAddEntryDialog from './StatementAddEntryDialog.vue'
import StatementEditDialog from './StatementEditDialog.vue'
import StatementConfirmOnBehalfDialog from './StatementConfirmOnBehalfDialog.vue'
import StatementRejectDialog from './StatementRejectDialog.vue'
import StatementRejectReplyDialog from './StatementRejectReplyDialog.vue'

defineOptions({ name: 'StatementDetailDrawer' })

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const visible = ref(false)
const loading = ref(false)
const detail = ref<CustomerStatementDetailRespVO | null>(null)

const addEntryDialogRef = ref<InstanceType<typeof StatementAddEntryDialog>>()
const editDialogRef = ref<InstanceType<typeof StatementEditDialog>>()
const confirmOnBehalfDialogRef = ref<InstanceType<typeof StatementConfirmOnBehalfDialog>>()
const rejectDialogRef = ref<InstanceType<typeof StatementRejectDialog>>()
const rejectReplyDialogRef = ref<InstanceType<typeof StatementRejectReplyDialog>>()

// 加载详情数据
const fetchDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getCustomerStatement(id)
    detail.value = res
  } finally {
    loading.value = false
  }
}

// 打开抽屉
const open = (id: number) => {
  visible.value = true
  fetchDetail(id)
}

// 标红已红冲行
const itemRowClassName = ({ row }: { row: CustomerStatementItemRespVO }) => {
  return row.hasReversal ? 'reversal-highlight-row' : ''
}

// 发送对账单
const handleSend = async () => {
  if (!detail.value) return
  await ElMessageBox.confirm(
    `确定要将客户对账单【${detail.value.statementNo}】发送给客户吗？发送后客户可在门户查阅并进行核对确认。`,
    '发送确认',
    {
      confirmButtonText: '确定发送',
      cancelButtonText: '取消',
      type: 'info'
    }
  )

  loading.value = true
  try {
    await sendCustomerStatement(detail.value.id)
    ElMessage.success('对账单已成功发送')
    await fetchDetail(detail.value.id)
    emit('refresh')
  } finally {
    loading.value = false
  }
}

// 作废对账单
const handleVoid = async () => {
  if (!detail.value) return
  await ElMessageBox.confirm(
    `确定要作废客户对账单【${detail.value.statementNo}】吗？作废后已占用的分录将被释放。`,
    '作废确认',
    {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  loading.value = true
  try {
    await voidCustomerStatement(detail.value.id)
    ElMessage.success('对账单已成功作废')
    await fetchDetail(detail.value.id)
    emit('refresh')
  } finally {
    loading.value = false
  }
}

// 代客确认
const handleConfirmOnBehalf = () => {
  if (!detail.value) return
  confirmOnBehalfDialogRef.value?.open(detail.value.id, detail.value.statementNo)
}

// 驳回
const handleReject = () => {
  if (!detail.value) return
  rejectDialogRef.value?.open(detail.value.id, detail.value.statementNo)
}

// 异议回复
const handleRejectReply = () => {
  if (!detail.value) return
  rejectReplyDialogRef.value?.open(
    detail.value.id,
    detail.value.statementNo,
    detail.value.rejectReason,
    detail.value.replyRemark
  )
}

// 移除草稿中的某条分录
const handleRemoveItem = async (itemId: number) => {
  if (!detail.value) return

  await ElMessageBox.confirm(
    '确定要从对账单草稿中移除该分录行吗？移除后将自动重算汇总余额。',
    '移除确认',
    {
      confirmButtonText: '确定移除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  loading.value = true
  try {
    await updateCustomerStatement({
      id: detail.value.id,
      deletedItemIds: [itemId]
    })
    ElMessage.success('分录行已移除')
    await fetchDetail(detail.value.id)
    emit('refresh')
  } finally {
    loading.value = false
  }
}

// 追加分录
const handleOpenAddEntry = () => {
  if (!detail.value) return
  const existingIds = (detail.value.items || []).map((i) => i.ledgerEntryId)
  addEntryDialogRef.value?.open({
    statementId: detail.value.id,
    customerId: detail.value.customerId,
    currency: detail.value.currency,
    periodStart: detail.value.periodStart,
    periodEnd: detail.value.periodEnd,
    existingEntryIds: existingIds
  })
}

// 编辑备注
const handleEditRemark = () => {
  if (!detail.value) return
  editDialogRef.value?.open(detail.value.id, detail.value.remark)
}

// 删除草稿
const handleDelete = async () => {
  if (!detail.value) return

  await ElMessageBox.confirm(
    `确定要删除客户对账单草稿【${detail.value.statementNo}】吗？删除后关联分录将全部释放。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  loading.value = true
  try {
    await deleteCustomerStatement(detail.value.id)
    ElMessage.success('对账单草稿已删除')
    visible.value = false
    emit('refresh')
  } finally {
    loading.value = false
  }
}

const handleSubSuccess = async () => {
  if (detail.value?.id) {
    await fetchDetail(detail.value.id)
    emit('refresh')
  }
}

defineExpose({ open })
</script>

<style scoped>
:deep(.reversal-highlight-row) {
  background-color: #fff1f2 !important;
}
:deep(.reversal-highlight-row td) {
  color: #e11d48 !important;
}
</style>
