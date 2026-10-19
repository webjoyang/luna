<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="退货单号 / 客户名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="业务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-140px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_RETURN_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审批状态" prop="approvalStatus">
        <el-select
          v-model="queryParams.approvalStatus"
          placeholder="全部状态"
          clearable
          class="!w-140px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="发货单号" prop="shipmentNo">
        <el-input
          v-model="queryParams.shipmentNo"
          placeholder="发货单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="搜索客户"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchCustomers"
          :loading="customerLoading"
          class="!w-180px"
        >
          <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据日期" prop="returnDate">
        <el-date-picker
          v-model="queryParams.returnDate"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"> <Icon icon="ep:search" class="mr-2px" /> 搜索 </el-button>
        <el-button @click="resetQuery"> <Icon icon="ep:refresh" class="mr-2px" /> 重置 </el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:business:sales-return:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" /> 新建退货单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="退货单号" prop="returnNo" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link
            type="primary"
            :underline="false"
            @click="openWorkspace(row.id, 'detail')"
            v-hasPermi="['lunaerp:business:sales-return:query']"
          >
            {{ row.returnNo }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" prop="status" width="110" align="center">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.LUNAERP_SALES_RETURN_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="审批状态" prop="approvalStatus" width="110" align="center">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_STATUS" :value="row.approvalStatus" />
        </template>
      </el-table-column>
      <el-table-column label="原发货单号" prop="shipmentNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip />
      <el-table-column label="退货仓库" prop="warehouseName" width="130" show-overflow-tooltip />
      <el-table-column label="币种" prop="currency" width="70" align="center" />
      <el-table-column label="退货总金额" prop="totalAmount" width="130" align="right">
        <template #default="{ row }">
          <span class="font-bold text-primary font-mono">
            {{ formatAmount(row.totalAmount, row.currency) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="总匹数" prop="totalPieces" width="80" align="center" />
      <el-table-column label="单据日期" prop="returnDate" width="110" align="center" />
      <el-table-column label="到货时间" prop="arrivedAt" width="150" align="center">
        <template #default="{ row }">
          <span>{{ row.arrivedAt ? parseTime(row.arrivedAt) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="过账时间" prop="postedAt" width="150" align="center">
        <template #default="{ row }">
          <span>{{ row.postedAt ? parseTime(row.postedAt) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="150" align="center">
        <template #default="{ row }">
          <span>{{ row.createTime ? parseTime(row.createTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right" align="center">
        <template #default="{ row }">
          <div class="flex items-center justify-center gap-6px">
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canDetail"
              type="primary"
              link
              size="small"
              @click="openWorkspace(row.id, 'detail')"
              v-hasPermi="['lunaerp:business:sales-return:query']"
            >
              详情
            </el-button>
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canEdit"
              type="primary"
              link
              size="small"
              @click="openWorkspace(row.id)"
              v-hasPermi="['lunaerp:business:sales-return:update']"
            >
              编辑
            </el-button>
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canSubmit"
              type="success"
              link
              size="small"
              @click="handleSubmit(row)"
              v-hasPermi="['lunaerp:business:sales-return:submit']"
            >
              提交
            </el-button>
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canWithdraw"
              type="warning"
              link
              size="small"
              @click="handleWithdraw(row)"
              v-hasPermi="['lunaerp:business:sales-return:submit']"
            >
              撤回
            </el-button>
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canReceive"
              type="warning"
              link
              size="small"
              @click="handleReceive(row)"
              v-hasPermi="['lunaerp:business:sales-return:receive']"
            >
              到货质检
            </el-button>
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canPost"
              type="success"
              link
              size="small"
              @click="handlePost(row)"
              v-hasPermi="['lunaerp:business:sales-return:post']"
            >
              财务确认
            </el-button>
            <PrintDispatcher
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canPrint"
              biz-type="SALES_RETURN"
              :biz-id="row.id"
              class="inline-block"
            >
              <template #default="{ open }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="open()"
                  v-hasPermi="['lunaerp:business:sales-return:print']"
                >
                  打印
                </el-button>
              </template>
            </PrintDispatcher>
            <el-button
              v-if="getSalesReturnActionFlags(row.status, row.approvalStatus).canDelete"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
              v-hasPermi="['lunaerp:business:sales-return:delete']"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 到货质检弹窗 -->
    <ReceiveDialog ref="receiveDialogRef" @success="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parseTime } from '@/utils/formatTime'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import {
  getSalesReturnPage,
  deleteSalesReturn,
  submitSalesReturn,
  withdrawSalesReturn,
  postSalesReturn,
  type SalesReturnVO,
  type SalesReturnPageParam
} from '@/api/lunaerp-elmui/business/sales-return'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import { getSalesReturnActionFlags, formatAmount } from './model/salesReturn'
import { PrintDispatcher } from '@/components/Print'
import ReceiveDialog from './components/ReceiveDialog.vue'

defineOptions({ name: 'LunaerpElmuiBusinessSalesReturn' })

const router = useRouter()
const loading = ref(false)
const list = ref<SalesReturnVO[]>([])
const total = ref(0)

const queryParams = reactive<SalesReturnPageParam>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  approvalStatus: undefined,
  shipmentNo: '',
  customerId: undefined,
  returnDate: []
})

const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const searchCustomers = async (query: string) => {
  if (!query || !query.trim()) {
    customerOptions.value = []
    return
  }
  customerLoading.value = true
  try {
    const res = await getCustomerPage({ search: query.trim(), pageSize: 20, status: 'ACTIVE' })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getSalesReturnPage(queryParams)
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
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.approvalStatus = undefined
  queryParams.shipmentNo = ''
  queryParams.customerId = undefined
  queryParams.returnDate = []
  handleQuery()
}

const openWorkspace = (id?: number, mode?: string) => {
  if (id) {
    router.push({
      path: `/business/sales-return/workspace/${id}`,
      query: mode ? { mode } : undefined
    })
  } else {
    router.push('/business/sales-return/workspace')
  }
}

const handleDelete = async (row: SalesReturnVO) => {
  try {
    await ElMessageBox.confirm(
      `确认删除销售退货单【${row.returnNo}】吗？删除后进入回收站。`,
      '系统提示',
      {
        type: 'warning'
      }
    )
    await deleteSalesReturn(row.id!)
    ElMessage.success('删除成功')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const handleSubmit = async (row: SalesReturnVO) => {
  try {
    await ElMessageBox.confirm(`确认提交销售退货单【${row.returnNo}】审批吗？`, '提交确认', {
      confirmButtonText: '确定提交',
      cancelButtonText: '取消',
      type: 'info'
    })
    await submitSalesReturn(row.id!)
    ElMessage.success('提交审批成功')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '提交审批失败')
    }
  }
}

const handleWithdraw = async (row: SalesReturnVO) => {
  try {
    await ElMessageBox.confirm(
      `确认撤回销售退货单【${row.returnNo}】审批吗？撤回后单据恢复为草稿状态并释放占量。`,
      '撤回确认',
      {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await withdrawSalesReturn(row.id!)
    ElMessage.success('撤回审批成功')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '撤回审批失败')
    }
  }
}

const receiveDialogRef = ref<InstanceType<typeof ReceiveDialog> | null>(null)

const handleReceive = (row: SalesReturnVO) => {
  receiveDialogRef.value?.open(row.id!)
}

const handlePost = async (row: SalesReturnVO) => {
  try {
    await ElMessageBox.confirm(
      `确认对销售退货单【${row.returnNo}】进行财务确认过账？过账后将冲减客户应收账款且单据不可撤销与修改。`,
      '财务确认',
      {
        confirmButtonText: '确定过账',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await postSalesReturn(row.id!)
    ElMessage.success('财务确认过账成功')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '财务确认过账失败')
    }
  }
}

onMounted(() => {
  getList()
})
</script>
