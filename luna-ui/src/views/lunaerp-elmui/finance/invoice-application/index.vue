<template>
  <ContentWrap>
    <!-- 搜索过滤区 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="85px"
    >
      <el-form-item label="发票申请单" prop="invoiceNo">
        <el-input
          v-model="queryParams.invoiceNo"
          placeholder="请输入申请单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>

      <el-form-item label="开票客户" prop="customerId">
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

      <el-form-item label="申请状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已提交" value="SUBMITTED" />
          <el-option label="已开票" value="ISSUED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>

      <el-form-item label="发票种类" prop="invoiceKind">
        <el-select
          v-model="queryParams.invoiceKind"
          placeholder="全部种类"
          clearable
          class="!w-150px"
        >
          <el-option
            v-for="item in INVOICE_KIND_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="官方发票号" prop="officialInvoiceNo">
        <el-input
          v-model="queryParams.officialInvoiceNo"
          placeholder="请输入 20 位发票号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap class="mt-4">
    <!-- 头部操作栏 -->
    <div class="mb-4 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <el-button
          type="primary"
          :icon="Plus"
          @click="openFormDialog()"
          v-hasPermi="['lunaerp:finance:invoice:create']"
        >
          新建发票申请
        </el-button>
      </div>
      <el-button link type="primary" :icon="Refresh" @click="getList">刷新列表</el-button>
    </div>

    <!-- 表格列表 -->
    <el-table v-loading="loading" :data="list" border row-key="id">
      <el-table-column prop="invoiceNo" label="申请单号" min-width="150">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" class="font-bold" @click="openDetail(row)">
            {{ row.invoiceNo }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column prop="customerName" label="客户名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="currency" label="币种" width="70" align="center" />

      <el-table-column prop="invoiceKind" label="种类" width="120" align="center">
        <template #default="{ row }">
          {{ getInvoiceKindLabel(row.invoiceKind) }}
        </template>
      </el-table-column>

      <el-table-column prop="taxRate" label="税率" width="80" align="center">
        <template #default="{ row }"> {{ row.taxRate }}% </template>
      </el-table-column>

      <el-table-column prop="totalAmountInclTax" label="价税合计" min-width="130" align="right">
        <template #default="{ row }">
          <span class="font-bold text-danger">{{ formatMoney(row.totalAmountInclTax) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="totalTax" label="税额" min-width="110" align="right">
        <template #default="{ row }">
          {{ formatMoney(row.totalTax) }}
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="95" align="center">
        <template #default="{ row }">
          <el-tag :type="formatInvoiceStatusTag(row.status).type" size="small">
            {{ formatInvoiceStatusTag(row.status).label }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        prop="officialInvoiceNo"
        label="官方发票号"
        min-width="170"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="row.officialInvoiceNo" class="font-mono text-success font-semibold">
            {{ row.officialInvoiceNo }}
          </span>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>

      <el-table-column prop="invoiceDate" label="开票日期" width="110" align="center">
        <template #default="{ row }">
          {{ row.invoiceDate || '-' }}
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="申请时间" width="160" align="center" />

      <!-- 操作列 -->
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <!-- 详情 -->
          <el-button
            link
            type="primary"
            @click="openDetail(row)"
            v-hasPermi="['lunaerp:finance:invoice:query']"
          >
            详情
          </el-button>

          <!-- 提交：仅 DRAFT 与 REJECTED -->
          <el-button
            v-if="getInvoiceActionFlags(row.status).canSubmit"
            link
            type="primary"
            @click="handleSubmit(row)"
            v-hasPermi="['lunaerp:finance:invoice:update']"
          >
            提交
          </el-button>

          <!-- 登记开票：仅 SUBMITTED -->
          <el-button
            v-if="getInvoiceActionFlags(row.status).canIssue"
            link
            type="success"
            @click="openIssueDialog(row)"
            v-hasPermi="['lunaerp:finance:invoice:issue']"
          >
            登记开票
          </el-button>

          <!-- 驳回：仅 SUBMITTED -->
          <el-button
            v-if="getInvoiceActionFlags(row.status).canReject"
            link
            type="danger"
            @click="openRejectDialog(row)"
            v-hasPermi="['lunaerp:finance:invoice:issue']"
          >
            驳回
          </el-button>

          <!-- 编辑：仅 DRAFT -->
          <el-button
            v-if="getInvoiceActionFlags(row.status).canEdit"
            link
            type="primary"
            @click="openFormDialog(row.id)"
            v-hasPermi="['lunaerp:finance:invoice:update']"
          >
            编辑
          </el-button>

          <!-- 删除：仅 DRAFT -->
          <el-button
            v-if="getInvoiceActionFlags(row.status).canDelete"
            link
            type="danger"
            @click="handleDelete(row)"
            v-hasPermi="['lunaerp:finance:invoice:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无发票申请数据" :image-size="90" />
      </template>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新建/编辑发票申请弹窗 -->
    <InvoiceFormDialog ref="formDialogRef" @success="getList" />

    <!-- 登记开票弹窗 -->
    <InvoiceIssueDialog ref="issueDialogRef" @success="getList" />

    <!-- 驳回发票弹窗 -->
    <InvoiceRejectDialog ref="rejectDialogRef" @success="getList" />

    <!-- 详情抽屉 -->
    <InvoiceDetailDrawer ref="detailDrawerRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  InvoiceApplicationRespVO,
  InvoiceApplicationPageReqVO
} from '@/api/lunaerp-elmui/finance/invoice'
import {
  getInvoiceApplicationPage,
  deleteInvoiceApplication,
  submitInvoiceApplication
} from '@/api/lunaerp-elmui/finance/invoice'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  formatMoney,
  formatInvoiceStatusTag,
  getInvoiceActionFlags,
  INVOICE_KIND_OPTIONS
} from './model/invoice'
import InvoiceFormDialog from './components/InvoiceFormDialog.vue'
import InvoiceIssueDialog from './components/InvoiceIssueDialog.vue'
import InvoiceRejectDialog from './components/InvoiceRejectDialog.vue'
import InvoiceDetailDrawer from './components/InvoiceDetailDrawer.vue'

defineOptions({ name: 'LunaerpFinanceInvoiceApplication' })

const loading = ref(false)
const customerLoading = ref(false)
const list = ref<InvoiceApplicationRespVO[]>([])
const total = ref(0)
const customerOptions = ref<CustomerVO[]>([])

const queryFormRef = ref()
const formDialogRef = ref<InstanceType<typeof InvoiceFormDialog>>()
const issueDialogRef = ref<InstanceType<typeof InvoiceIssueDialog>>()
const rejectDialogRef = ref<InstanceType<typeof InvoiceRejectDialog>>()
const detailDrawerRef = ref<InstanceType<typeof InvoiceDetailDrawer>>()

const queryParams = reactive<InvoiceApplicationPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  invoiceNo: undefined,
  customerId: undefined,
  status: undefined,
  invoiceKind: undefined,
  officialInvoiceNo: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getInvoiceApplicationPage(queryParams)
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
  handleQuery()
}

const searchCustomers = async (query: string) => {
  if (!query || query.trim().length === 0) {
    customerOptions.value = []
    return
  }
  customerLoading.value = true
  try {
    const res = await getCustomerPage({
      pageNo: 1,
      pageSize: 20,
      companyName: query.trim()
    })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const getInvoiceKindLabel = (kind?: string) => {
  const item = INVOICE_KIND_OPTIONS.find((k) => k.value === kind)
  return item ? item.label : kind || '-'
}

const openFormDialog = (id?: number) => {
  formDialogRef.value?.open(id)
}

const openIssueDialog = (row: InvoiceApplicationRespVO) => {
  issueDialogRef.value?.open(row)
}

const openRejectDialog = (row: InvoiceApplicationRespVO) => {
  rejectDialogRef.value?.open(row)
}

const openDetail = (row: InvoiceApplicationRespVO) => {
  detailDrawerRef.value?.open(row.id)
}

const handleSubmit = async (row: InvoiceApplicationRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定提交发票申请【${row.invoiceNo}】吗？提交后将锁定发货单开票额度并进入开票登记队列。`,
      '提交确认',
      { confirmButtonText: '确定提交', cancelButtonText: '取消', type: 'info' }
    )
    await submitInvoiceApplication(row.id)
    ElMessage.success('提交申请成功')
    getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      // 错误提示由全局拦截器处理
    }
  }
}

const handleDelete = async (row: InvoiceApplicationRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定删除草稿发票申请【${row.invoiceNo}】吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteInvoiceApplication(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      // 错误提示由全局拦截器处理
    }
  }
}

onMounted(() => {
  getList()
})
</script>
