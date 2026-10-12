<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form
      class="-mb-15px mt-2"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="70px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="申请单号 / 发票号 / 开票抬头"
          clearable
          @keyup.enter="handleQuery"
          class="!w-220px"
        />
      </el-form-item>

      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="全部客户"
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

      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-130px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已提交" value="SUBMITTED" />
          <el-option label="已开票" value="ISSUED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>

      <el-form-item label="发票种类" prop="invoiceKind">
        <el-select v-model="queryParams.invoiceKind" placeholder="全部种类" clearable class="!w-140px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_FINANCE_INVOICE_KIND)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-240px"
        />
      </el-form-item>

      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-2px" />搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-2px" />重置
        </el-button>
        <el-button
          type="primary"
          @click="openFormDialog()"
          v-hasPermi="['lunaerp:finance:invoice:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" />新建发票申请
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 列表展示 -->
    <div class="mt-4">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="申请单号" prop="invoiceNo" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetailDrawer(row.id)">
              {{ row.invoiceNo }}
            </el-button>
          </template>
        </el-table-column>

        <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip />

        <el-table-column label="发票种类" prop="invoiceKind" width="110" align="center">
          <template #default="{ row }">
            {{ getInvoiceKindLabel(row.invoiceKind) }}
          </template>
        </el-table-column>

        <el-table-column label="币种" prop="currency" width="70" align="center" />

        <el-table-column label="税率" prop="taxRate" width="80" align="center">
          <template #default="{ row }">
            {{ row.taxRate }}%
          </template>
        </el-table-column>

        <el-table-column label="价税合计" width="130" align="right">
          <template #default="{ row }">
            <span class="font-bold text-red-600">￥{{ formatAmount(row.totalAmountInclTax) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="不含税金额" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalAmountExclTax) }}
          </template>
        </el-table-column>

        <el-table-column label="税额" width="110" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.totalTax) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="官方发票号" prop="officialInvoiceNo" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.officialInvoiceNo" class="font-mono text-green-700">
              {{ row.officialInvoiceNo }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="开票日期" prop="invoiceDate" width="110" align="center">
          <template #default="{ row }">
            {{ row.invoiceDate || '—' }}
          </template>
        </el-table-column>

        <el-table-column
          label="创建时间"
          prop="createTime"
          width="160"
          align="center"
          :formatter="dateFormatter"
        />

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetailDrawer(row.id)">
              详情
            </el-button>

            <!-- 草稿/已驳回可编辑与提交 -->
            <template v-if="row.status === 'DRAFT' || row.status === 'REJECTED'">
              <el-button
                link
                type="success"
                size="small"
                @click="handleSubmit(row)"
                v-hasPermi="['lunaerp:finance:invoice:update']"
              >
                提交
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="openFormDialog(row.id)"
                v-hasPermi="['lunaerp:finance:invoice:update']"
              >
                编辑
              </el-button>
            </template>

            <!-- 仅草稿可删除 -->
            <el-button
              v-if="row.status === 'DRAFT'"
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
              v-hasPermi="['lunaerp:finance:invoice:delete']"
            >
              删除
            </el-button>

            <!-- 已提交待开票：财务复核节点 -->
            <template v-if="row.status === 'SUBMITTED'">
              <el-button
                link
                type="primary"
                size="small"
                @click="openIssueDialog(row)"
                v-hasPermi="['lunaerp:finance:invoice:issue']"
              >
                登记开票
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                @click="handleReject(row)"
                v-hasPermi="['lunaerp:finance:invoice:issue']"
              >
                驳回
              </el-button>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无发票申请数据" :image-size="80" />
        </template>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 创建 / 编辑弹窗 -->
    <InvoiceFormDialog ref="formDialogRef" @success="getList" />

    <!-- 登记开票弹窗 -->
    <InvoiceIssueDialog ref="issueDialogRef" @success="getList" />

    <!-- 详情抽屉 -->
    <InvoiceDetailDrawer ref="detailDrawerRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { useMessage } from '@/hooks/web/useMessage'
import * as InvoiceApi from '@/api/lunaerp-elmui/finance/invoice'
import type { InvoiceVO, InvoicePageParam } from '@/api/lunaerp-elmui/finance/invoice'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import InvoiceFormDialog from './components/InvoiceFormDialog.vue'
import InvoiceIssueDialog from './components/InvoiceIssueDialog.vue'
import InvoiceDetailDrawer from './components/InvoiceDetailDrawer.vue'

defineOptions({ name: 'LunaerpElmuiInvoiceApplication' })

const message = useMessage()

const loading = ref(false)
const list = ref<InvoiceVO[]>([])
const total = ref(0)

const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const queryParams = reactive<InvoicePageParam>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  customerId: undefined,
  status: undefined,
  invoiceKind: undefined,
  currency: undefined,
  createTime: undefined
})

const queryFormRef = ref()
const formDialogRef = ref()
const issueDialogRef = ref()
const detailDrawerRef = ref()

function formatAmount(val?: number | string | null): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getStatusTagType(status?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'DRAFT':
      return 'info'
    case 'SUBMITTED':
      return 'warning'
    case 'ISSUED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

function getStatusLabel(status?: string): string {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'SUBMITTED':
      return '已提交'
    case 'ISSUED':
      return '已开票'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '—'
  }
}

function getInvoiceKindLabel(kind?: string): string {
  switch (kind) {
    case 'ELEC_SPECIAL':
      return '数电专票'
    case 'ELEC_NORMAL':
      return '数电普票'
    case 'VAT_SPECIAL':
      return '增值税专票'
    case 'VAT_NORMAL':
      return '增值税普票'
    default:
      return kind || '—'
  }
}

const searchCustomers = async (query: string) => {
  if (!query) return
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({ search: query, pageNo: 1, pageSize: 20 })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const res = await InvoiceApi.getInvoicePage(queryParams)
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
  queryFormRef.value.resetFields()
  handleQuery()
}

const openFormDialog = (id?: number) => {
  formDialogRef.value.open(id)
}

const openIssueDialog = (row: InvoiceVO) => {
  issueDialogRef.value.open(row)
}

const openDetailDrawer = (id: number) => {
  detailDrawerRef.value.open(id)
}

const handleSubmit = async (row: InvoiceVO) => {
  await message.confirm(`确认提交发票申请单【${row.invoiceNo}】吗？提交后将占用发货单开票额度并进入财务复核。`)
  await InvoiceApi.submitInvoice(row.id!)
  message.success('发票申请已提交')
  getList()
}

const handleDelete = async (row: InvoiceVO) => {
  await message.confirm(`确认删除草稿发票申请【${row.invoiceNo}】吗？`)
  await InvoiceApi.deleteInvoice(row.id!)
  message.success('删除成功')
  getList()
}

const handleReject = async (row: InvoiceVO) => {
  const { value } = await ElMessageBox.prompt(
    `请输入驳回申请单【${row.invoiceNo}】的原因（驳回后将立即释放发货单开票额度）：`,
    '驳回发票申请',
    {
      confirmButtonText: '确定驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入驳回原因（选填）'
    }
  )
  await InvoiceApi.rejectInvoice(row.id!, value)
  message.success('发票申请已驳回，开票额度已释放')
  getList()
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
</style>
