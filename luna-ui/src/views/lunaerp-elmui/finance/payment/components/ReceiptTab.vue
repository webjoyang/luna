<template>
  <div>
    <!-- 筛选条件区 -->
    <el-card shadow="never" class="mb-16px !border-gray-200">
      <el-form :model="queryParams" :inline="true" label-width="70px" class="-mb-15px">
        <el-form-item label="收款客户">
          <el-select
            v-model="queryParams.customerId"
            placeholder="筛选客户"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
            class="!w-200px"
            @change="handleQuery"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资金账户">
          <el-select
            v-model="queryParams.accountId"
            placeholder="筛选账户"
            clearable
            filterable
            class="!w-180px"
            @change="handleQuery"
          >
            <el-option
              v-for="acc in accountOptions"
              :key="acc.id"
              :label="acc.accountName || acc.name || '账户'"
              :value="acc.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="收款类型">
          <el-select
            v-model="queryParams.receiptType"
            placeholder="全部类型"
            clearable
            class="!w-130px"
            @change="handleQuery"
          >
            <el-option label="客户收款" value="RECEIPT" />
            <el-option label="预收款" value="ADVANCE" />
          </el-select>
        </el-form-item>

        <el-form-item label="审批状态">
          <el-select
            v-model="queryParams.approvalStatus"
            placeholder="全部状态"
            clearable
            class="!w-140px"
            @change="handleQuery"
          >
            <el-option label="草稿待提交" value="PENDING" />
            <el-option label="审批中" value="APPROVING" />
            <el-option label="审批通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>

        <el-form-item label="收款日期">
          <el-date-picker
            v-model="queryParams.receiptDate"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="!w-240px"
            @change="handleQuery"
          />
        </el-form-item>

        <el-form-item label="收款单号">
          <el-input
            v-model="queryParams.paymentNo"
            placeholder="收款单号"
            clearable
            class="!w-180px"
            @keyup.enter="handleQuery"
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
            v-hasPermi="['lunaerp:finance:payment:create']"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-2px" /> 新增收款单
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 收款单列表表格 -->
    <el-card shadow="never" class="!border-gray-200">
      <el-table v-loading="loading" :data="receiptList" border stripe size="small">
        <el-table-column label="#" type="index" width="45" align="center" />
        <el-table-column label="收款单号" prop="paymentNo" min-width="150">
          <template #default="{ row }">
            <span class="font-mono font-medium text-gray-800">{{ row.paymentNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="客户名称" prop="customerName" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium">{{ row.customerName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="收款账户"
          prop="accountName"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column label="类型" prop="receiptType" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.receiptType === 'ADVANCE' ? 'warning' : 'primary'">
              {{ row.receiptType === 'ADVANCE' ? '预收' : '收款' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="币种" prop="currency" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收款金额" prop="payAmount" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-emerald-700"
              >¥ {{ formatMoney(row.payAmount) }}</span
            >
          </template>
        </el-table-column>
        <el-table-column
          label="已核销金额"
          prop="allocatedTotalAmount"
          min-width="110"
          align="right"
        >
          <template #default="{ row }">
            <span class="font-mono text-gray-600"
              >¥ {{ formatMoney(row.allocatedTotalAmount) }}</span
            >
          </template>
        </el-table-column>
        <el-table-column label="未核销金额" prop="unallocatedAmount" min-width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono text-amber-600">¥ {{ formatMoney(row.unallocatedAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收款日期" prop="receiptDate" width="105" align="center" />
        <el-table-column label="经手人" prop="handlerName" width="90" align="center" />
        <el-table-column label="审批状态" prop="approvalStatus" width="105" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getApprovalStatusTagType(row.approvalStatus)">
              {{ getApprovalStatusText(row.approvalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="单据状态" prop="status" width="95" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'COMPLETED'" size="small" type="success" effect="plain">
              已过账
            </el-tag>
            <el-tag v-else-if="row.status === 'REVERSED'" size="small" type="danger" effect="plain">
              已红冲
            </el-tag>
            <el-tag v-else size="small" type="info" effect="plain"> 草稿待账 </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注说明" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="270" fixed="right" align="center">
          <template #default="{ row }">
            <div class="space-x-4px">
              <el-button
                type="primary"
                link
                size="small"
                v-hasPermi="['lunaerp:finance:payment:query']"
                @click="handleDetail(row)"
              >
                详情
              </el-button>

              <el-button
                v-if="
                  getReceiptActionFlags(row.approvalStatus, row.status, row.receiptType).canEdit
                "
                type="primary"
                link
                size="small"
                v-hasPermi="['lunaerp:finance:payment:update']"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>

              <el-button
                v-if="
                  getReceiptActionFlags(row.approvalStatus, row.status, row.receiptType).canSubmit
                "
                type="success"
                link
                size="small"
                v-hasPermi="['lunaerp:finance:payment:create']"
                @click="handleSubmit(row)"
              >
                提交审批
              </el-button>

              <el-button
                v-if="
                  getReceiptActionFlags(row.approvalStatus, row.status, row.receiptType).canWriteoff
                "
                type="warning"
                link
                size="small"
                v-hasPermi="['lunaerp:finance:payment:writeoff']"
                @click="handleAdvanceWriteoff(row)"
              >
                预收转核销
              </el-button>

              <el-button
                v-if="
                  getReceiptActionFlags(row.approvalStatus, row.status, row.receiptType).canReverse
                "
                type="danger"
                link
                size="small"
                v-hasPermi="['lunaerp:finance:payment:reverse']"
                @click="handleReverse(row)"
              >
                红冲
              </el-button>

              <el-button
                v-if="
                  getReceiptActionFlags(row.approvalStatus, row.status, row.receiptType).canDelete
                "
                type="danger"
                link
                size="small"
                v-hasPermi="['lunaerp:finance:payment:delete']"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <Pagination
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="loadReceiptPage"
      />
    </el-card>

    <!-- 弹窗与抽屉 -->
    <ReceiptFormDialog ref="formDialogRef" @success="loadReceiptPage" />
    <ReceiptDetailDrawer ref="detailDrawerRef" />
    <ReceiptAdvanceWriteoffDialog
      v-model="advanceWriteoffVisible"
      :receipt-row="selectedReceiptRow"
      @success="loadReceiptPage"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getReceiptPage,
  deleteReceipt,
  submitReceipt,
  reverseReceipt,
  getFinanceAccountSimpleList,
  type ReceiptRespVO,
  type ReceiptPageReqVO,
  type FinanceAccountSimpleRespVO
} from '@/api/lunaerp-elmui/finance/payment'
import {
  formatMoney,
  getReceiptActionFlags,
  getApprovalStatusText,
  getApprovalStatusTagType,
  parsePaymentErrorMessage
} from '../model/payment'
import ReceiptFormDialog from './ReceiptFormDialog.vue'
import ReceiptDetailDrawer from './ReceiptDetailDrawer.vue'
import ReceiptAdvanceWriteoffDialog from './ReceiptAdvanceWriteoffDialog.vue'

defineOptions({ name: 'ReceiptTab' })

const loading = ref(false)
const total = ref(0)
const receiptList = ref<ReceiptRespVO[]>([])

const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])
const accountOptions = ref<FinanceAccountSimpleRespVO[]>([])

const formDialogRef = ref<InstanceType<typeof ReceiptFormDialog>>()
const detailDrawerRef = ref<InstanceType<typeof ReceiptDetailDrawer>>()
const advanceWriteoffVisible = ref(false)
const selectedReceiptRow = ref<ReceiptRespVO | null>(null)

const queryParams = reactive<ReceiptPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  customerId: undefined,
  accountId: undefined,
  receiptType: undefined,
  approvalStatus: undefined,
  receiptDate: undefined,
  paymentNo: ''
})

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

const loadAccounts = async () => {
  try {
    const list = await getFinanceAccountSimpleList()
    accountOptions.value = list || []
  } catch {
    accountOptions.value = []
  }
}

const loadReceiptPage = async () => {
  loading.value = true
  try {
    const res = await getReceiptPage(queryParams)
    receiptList.value = res.list || []
    total.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(err.message || '加载收款单列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  loadReceiptPage()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.customerId = undefined
  queryParams.accountId = undefined
  queryParams.receiptType = undefined
  queryParams.approvalStatus = undefined
  queryParams.receiptDate = undefined
  queryParams.paymentNo = ''
  loadReceiptPage()
}

const handleCreate = () => {
  formDialogRef.value?.open()
}

const handleEdit = (row: ReceiptRespVO) => {
  formDialogRef.value?.open(row.id)
}

const handleDetail = (row: ReceiptRespVO) => {
  detailDrawerRef.value?.open(row.id)
}

const handleSubmit = async (row: ReceiptRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定将收款单【${row.paymentNo}】提交至审批引擎进行审核吗？`,
      '提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await submitReceipt(row.id)
    ElMessage.success('收款单已提交审批')
    loadReceiptPage()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(parsePaymentErrorMessage(err))
    }
  }
}

const handleDelete = async (row: ReceiptRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定删除收款单草稿【${row.paymentNo}】吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'danger'
      }
    )
    await deleteReceipt(row.id)
    ElMessage.success('收款单删除成功')
    loadReceiptPage()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(parsePaymentErrorMessage(err))
    }
  }
}

const handleAdvanceWriteoff = (row: ReceiptRespVO) => {
  selectedReceiptRow.value = row
  advanceWriteoffVisible.value = true
}

const handleReverse = async (row: ReceiptRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定红冲已过账收款单【${row.paymentNo}】吗？红冲后将生成反向冲销分录、扣减资金账户余额、自动重开全部核销分录，且不可撤回。`,
      '红冲确认',
      {
        confirmButtonText: '确定红冲',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await reverseReceipt(row.id)
    ElMessage.success('收款单红冲成功')
    loadReceiptPage()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(parsePaymentErrorMessage(err))
    }
  }
}

onMounted(() => {
  loadReceiptPage()
  loadAccounts()
  searchCustomers()
})
</script>
