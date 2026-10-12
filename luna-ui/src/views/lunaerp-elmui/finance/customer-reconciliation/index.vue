<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="mb-4">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="80px">
        <el-form-item label="对账单号" prop="statementNo">
          <el-input
            v-model="queryParams.statementNo"
            placeholder="请输入对账单号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-input
            v-model="queryParams.customerName"
            placeholder="请输入客户名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="币种" prop="currency">
          <el-select v-model="queryParams.currency" placeholder="全部币种" clearable style="width: 120px">
            <el-option label="CNY" value="CNY" />
            <el-option label="USD" value="USD" />
            <el-option label="EUR" value="EUR" />
            <el-option label="HKD" value="HKD" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.LUNAERP_FINANCE_STATEMENT_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对账期间">
          <el-date-picker
            v-model="periodRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-1" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-1" /> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮栏 -->
    <el-card shadow="never">
      <div class="mb-3 flex justify-between items-center">
        <div>
          <el-button
            v-hasPermi="['lunaerp:finance:customer-reconciliation:create']"
            type="primary"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-1" /> 生成对账单
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table v-loading="loading" :data="statementList" border stripe>
        <el-table-column label="对账单号" prop="statementNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="handleDetail(row.id)">
              {{ row.statementNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip />
        <el-table-column label="币种" prop="currency" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对账期间" min-width="180" align="center">
          <template #default="{ row }">
            {{ row.periodStart }} ~ {{ row.periodEnd }}
          </template>
        </el-table-column>
        <el-table-column label="期初余额" prop="openingBalance" width="120" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.openingBalance) }}
          </template>
        </el-table-column>
        <el-table-column label="本期借方发生" prop="periodDebit" width="120" align="right">
          <template #default="{ row }">
            <span class="text-blue-600 font-medium">+{{ formatAmount(row.periodDebit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本期贷方发生" prop="periodCredit" width="120" align="right">
          <template #default="{ row }">
            <span class="text-green-600 font-medium">-{{ formatAmount(row.periodCredit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期末余额" prop="closingBalance" width="120" align="right">
          <template #default="{ row }">
            <span class="font-bold text-amber-600">{{ formatAmount(row.closingBalance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_STATEMENT_STATUS" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-hasPermi="['lunaerp:finance:customer-reconciliation:update']"
              link
              type="primary"
              @click="handleEditRemark(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-hasPermi="['lunaerp:finance:customer-reconciliation:update']"
              link
              type="success"
              @click="handleAddEntry(row)"
            >
              追加分录
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-hasPermi="['lunaerp:finance:customer-reconciliation:delete']"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <pagination
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 生成对账单弹窗 -->
    <CustomerStatementCreateDialog ref="createDialogRef" @success="getList" />

    <!-- 详情弹窗 -->
    <CustomerStatementDetailDialog ref="detailDialogRef" />

    <!-- 追加分录弹窗 -->
    <CustomerStatementAddEntryDialog ref="addEntryDialogRef" @success="getList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import {
  getCustomerStatementPage,
  updateCustomerStatement,
  deleteCustomerStatement
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import type { CustomerStatementVO } from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import CustomerStatementCreateDialog from './components/CustomerStatementCreateDialog.vue'
import CustomerStatementDetailDialog from './components/CustomerStatementDetailDialog.vue'
import CustomerStatementAddEntryDialog from './components/CustomerStatementAddEntryDialog.vue'

defineOptions({ name: 'LunaerpElmuiCustomerReconciliation' })

const loading = ref(false)
const statementList = ref<CustomerStatementVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const periodRange = ref<[string, string] | null>(null)

const createDialogRef = ref()
const detailDialogRef = ref()
const addEntryDialogRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  statementNo: '',
  customerName: '',
  currency: '',
  status: '',
  periodStart: undefined as string | undefined,
  periodEnd: undefined as string | undefined
})

const formatAmount = (val?: number | string) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getList = async () => {
  loading.value = true
  try {
    if (periodRange.value && periodRange.value.length === 2) {
      queryParams.periodStart = periodRange.value[0]
      queryParams.periodEnd = periodRange.value[1]
    } else {
      queryParams.periodStart = undefined
      queryParams.periodEnd = undefined
    }

    const res = await getCustomerStatementPage(queryParams)
    statementList.value = res.list || []
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
  periodRange.value = null
  handleQuery()
}

const handleCreate = () => {
  createDialogRef.value?.open()
}

const handleDetail = (id: number) => {
  detailDialogRef.value?.open(id)
}

const handleAddEntry = (row: CustomerStatementVO) => {
  addEntryDialogRef.value?.open(row)
}

const handleEditRemark = async (row: CustomerStatementVO) => {
  try {
    const { value: remark } = await ElMessageBox.prompt('请输入新的备注说明：', '编辑对账单草稿', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.remark || ''
    })
    await updateCustomerStatement({ id: row.id, remark })
    ElMessage.success('更新备注成功')
    getList()
  } catch (e) {
    // 用户取消操作
  }
}

const handleDelete = async (row: CustomerStatementVO) => {
  try {
    await ElMessageBox.confirm(`确定删除草稿对账单【${row.statementNo}】吗？删除后将释放占用的分录。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await deleteCustomerStatement(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (e) {
    // 用户取消操作
  }
}

onMounted(() => {
  getList()
})
</script>
