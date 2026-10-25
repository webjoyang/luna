<template>
  <ContentWrap class="!mb-4">
    <el-tabs v-model="activeTab" class="recon-tabs" type="card">
      <el-tab-pane label="客户对账单" name="statement">
        <template #label>
          <span class="flex items-center">
            <Icon icon="ep:document" class="mr-4px" /> 客户对账单
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="FIFO 账龄汇总" name="aging">
        <template #label>
          <span class="flex items-center">
            <Icon icon="ep:data-analysis" class="mr-4px" /> FIFO 账龄汇总
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <!-- 1. 客户对账单视图 -->
  <div v-show="activeTab === 'statement'">
    <ContentWrap>
      <!-- 搜索过滤区 -->
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="70px"
      >
        <el-form-item label="对账单号" prop="statementNo">
          <el-input
            v-model="queryParams.statementNo"
            placeholder="请输入对账单号"
            clearable
            @keyup.enter="handleQuery"
            class="!w-200px"
          />
        </el-form-item>

        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="queryParams.customerId"
            placeholder="搜索并选择客户"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
            class="!w-200px"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="币种" prop="currency">
          <el-select
            v-model="queryParams.currency"
            placeholder="全部币种"
            clearable
            class="!w-120px"
          >
            <el-option label="人民币 (CNY)" value="CNY" />
            <el-option label="美元 (USD)" value="USD" />
            <el-option label="欧元 (EUR)" value="EUR" />
            <el-option label="港币 (HKD)" value="HKD" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-120px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发送" value="SENT" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已退回" value="REJECTED" />
            <el-option label="已作废" value="VOID" />
          </el-select>
        </el-form-item>

        <el-form-item label="对账区间" prop="dateRange">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="起始日期"
            end-placeholder="截止日期"
            class="!w-240px"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="handleQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
          <el-button
            type="primary"
            plain
            @click="openCreateDialog"
            v-hasPermi="['lunaerp:finance:customerrecon:create']"
          >
            <Icon icon="ep:plus" class="mr-2px" /> 生成对账单
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="对账单号" prop="statementNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row.id)">
              <span class="font-mono font-bold">{{ row.statementNo }}</span>
            </el-link>
          </template>
        </el-table-column>

        <el-table-column
          label="客户公司名"
          prop="customerName"
          min-width="160"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="font-medium">{{ row.customerName || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="币种" prop="currency" width="80" align="center" />

        <el-table-column label="对账区间" min-width="190" align="center">
          <template #default="{ row }">
            <span class="text-12px"
              >{{ row.periodStart || '—' }} 至 {{ row.periodEnd || '—' }}</span
            >
          </template>
        </el-table-column>

        <el-table-column label="期初欠款" prop="openingBalance" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono">¥{{ formatMoney(row.openingBalance) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="本期应收" prop="periodDebit" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-blue-600">¥{{ formatMoney(row.periodDebit) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="本期核销" prop="periodCredit" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-green-600">¥{{ formatMoney(row.periodCredit) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="期末应收" prop="closingBalance" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-purple-700"
              >¥{{ formatMoney(row.closingBalance) }}</span
            >
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="formatStatementStatusTag(row.status).type" size="small">
              {{ formatStatementStatusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注说明" prop="remark" min-width="130" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" width="160" align="center" />

        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openDetail(row.id)"
              v-hasPermi="['lunaerp:finance:customerrecon:query']"
            >
              详情
            </el-button>

            <!-- 发送：仅 DRAFT -->
            <el-button
              v-if="getCustomerStatementActionFlags(row.status).canSend"
              link
              type="primary"
              @click="handleSend(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:send']"
            >
              发送
            </el-button>

            <!-- 代客确认：仅 SENT -->
            <el-button
              v-if="getCustomerStatementActionFlags(row.status).canConfirmOnBehalf"
              link
              type="success"
              @click="openConfirmOnBehalfDialog(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:confirm-on-behalf']"
            >
              代客确认
            </el-button>

            <!-- 异议回复：仅 REJECTED -->
            <el-button
              v-if="getCustomerStatementActionFlags(row.status).canRejectReply"
              link
              type="primary"
              @click="openRejectReplyDialog(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:update']"
            >
              回复
            </el-button>

            <!-- 打印：非 VOID -->
            <PrintDispatcher
              v-if="getCustomerStatementActionFlags(row.status).canPrint"
              biz-type="FINANCE_STATEMENT"
              :biz-id="row.id"
              class="inline-block"
            >
              <template #default="{ open }">
                <el-button
                  link
                  type="primary"
                  @click="open()"
                  v-hasPermi="['lunaerp:finance:customerrecon:print']"
                >
                  打印
                </el-button>
              </template>
            </PrintDispatcher>

            <!-- 编辑：仅 DRAFT -->
            <el-button
              v-if="getCustomerStatementActionFlags(row.status).canEdit"
              link
              type="primary"
              @click="openEditDialog(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:update']"
            >
              编辑
            </el-button>

            <!-- 作废：非终态（DRAFT / SENT / REJECTED） -->
            <el-button
              v-if="getCustomerStatementActionFlags(row.status).canVoid"
              link
              type="warning"
              @click="handleVoid(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:void']"
            >
              作废
            </el-button>

            <!-- 删除：仅 DRAFT -->
            <el-button
              v-if="getCustomerStatementActionFlags(row.status).canDelete"
              link
              type="danger"
              @click="handleDelete(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:delete']"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无客户对账单数据" :image-size="90" />
        </template>
      </el-table>

      <!-- 分页 -->
      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />

      <!-- 向导生成弹窗 -->
      <StatementCreateDialog ref="createDialogRef" @success="handleCreateSuccess" />

      <!-- 详情抽屉 -->
      <StatementDetailDrawer ref="detailDrawerRef" @refresh="getList" />

      <!-- 编辑备注弹窗 -->
      <StatementEditDialog ref="editDialogRef" @success="getList" />

      <!-- 代客确认弹窗 -->
      <StatementConfirmOnBehalfDialog ref="confirmOnBehalfDialogRef" @success="getList" />

      <!-- 异议回复弹窗 -->
      <StatementRejectReplyDialog ref="rejectReplyDialogRef" @success="getList" />
    </ContentWrap>
  </div>

  <!-- 2. FIFO 账龄汇总视图 -->
  <div v-show="activeTab === 'aging'">
    <AgingSummaryTab />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  CustomerStatementRespVO,
  CustomerStatementPageReqVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import {
  getCustomerStatementPage,
  deleteCustomerStatement
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  formatMoney,
  formatStatementStatusTag,
  getCustomerStatementActionFlags
} from './model/customerStatement'
import { PrintDispatcher } from '@/components/Print'
import { useCustomerStatementActions } from './composables/useCustomerStatementActions'
import StatementCreateDialog from './components/StatementCreateDialog.vue'
import StatementDetailDrawer from './components/StatementDetailDrawer.vue'
import StatementEditDialog from './components/StatementEditDialog.vue'
import StatementConfirmOnBehalfDialog from './components/StatementConfirmOnBehalfDialog.vue'
import StatementRejectReplyDialog from './components/StatementRejectReplyDialog.vue'
import AgingSummaryTab from './components/AgingSummaryTab.vue'

defineOptions({ name: 'LunaerpFinanceCustomerReconciliation' })

const activeTab = ref<'statement' | 'aging'>('statement')

const loading = ref(false)
const customerLoading = ref(false)
const list = ref<CustomerStatementRespVO[]>([])
const total = ref(0)
const customerOptions = ref<CustomerVO[]>([])

const { handleSend: doSend, handleVoid: doVoid } = useCustomerStatementActions()

const queryFormRef = ref()
const createDialogRef = ref<InstanceType<typeof StatementCreateDialog>>()
const detailDrawerRef = ref<InstanceType<typeof StatementDetailDrawer>>()
const editDialogRef = ref<InstanceType<typeof StatementEditDialog>>()
const confirmOnBehalfDialogRef = ref<InstanceType<typeof StatementConfirmOnBehalfDialog>>()
const rejectReplyDialogRef = ref<InstanceType<typeof StatementRejectReplyDialog>>()

const queryParams = reactive<{
  pageNo: number
  pageSize: number
  statementNo?: string
  customerId?: number
  currency?: string
  status?: string
  dateRange?: [string, string]
}>({
  pageNo: 1,
  pageSize: 10,
  statementNo: undefined,
  customerId: undefined,
  currency: undefined,
  status: undefined,
  dateRange: undefined
})

// 搜索客户选项
const searchCustomers = async (query?: string) => {
  customerLoading.value = true
  try {
    const res = await getCustomerPage({
      pageNo: 1,
      pageSize: 30,
      search: query || ''
    })
    customerOptions.value = res.list || []
  } catch {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

// 查询列表
const getList = async () => {
  loading.value = true
  try {
    const params: CustomerStatementPageReqVO = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      statementNo: queryParams.statementNo || undefined,
      customerId: queryParams.customerId || undefined,
      currency: queryParams.currency || undefined,
      status: queryParams.status || undefined,
      periodStart: queryParams.dateRange?.[0] || undefined,
      periodEnd: queryParams.dateRange?.[1] || undefined
    }
    const res = await getCustomerStatementPage(params)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.dateRange = undefined
  handleQuery()
}

const openCreateDialog = () => {
  createDialogRef.value?.open()
}

const handleCreateSuccess = (id: number) => {
  getList()
  detailDrawerRef.value?.open(id)
}

const openDetail = (id: number) => {
  detailDrawerRef.value?.open(id)
}

const openEditDialog = (row: CustomerStatementRespVO) => {
  editDialogRef.value?.open(row.id, row.remark)
}

const handleSend = (row: CustomerStatementRespVO) => {
  return doSend(row, () => getList())
}

const handleVoid = (row: CustomerStatementRespVO) => {
  return doVoid(row, () => getList())
}

const openConfirmOnBehalfDialog = (row: CustomerStatementRespVO) => {
  confirmOnBehalfDialogRef.value?.open(row.id, row.statementNo)
}

const openRejectReplyDialog = (row: CustomerStatementRespVO) => {
  rejectReplyDialogRef.value?.open(row.id, row.statementNo, row.rejectReason, row.replyRemark)
}

const handleDelete = async (row: CustomerStatementRespVO) => {
  await ElMessageBox.confirm(
    `确定要删除客户对账单草稿【${row.statementNo}】吗？删除后关联分录将全部释放。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  loading.value = true
  try {
    await deleteCustomerStatement(row.id)
    ElMessage.success('对账单草稿已成功删除')
    getList()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getList()
  searchCustomers('')
})
</script>
