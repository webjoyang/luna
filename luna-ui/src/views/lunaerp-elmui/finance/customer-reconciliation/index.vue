<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" class="customer-recon-tabs" @tab-change="handleTabChange">
      <!-- ====================== TAB 1: 客户对账单 ====================== -->
      <el-tab-pane label="客户对账单" name="statement">
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

        <!-- 操作按钮栏与数据表格 -->
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
            <el-table-column label="确认方式" prop="confirmType" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.confirmType === 'ONLINE_PORTAL'">客户线上</span>
                <span v-else-if="row.confirmType === 'ON_BEHALF'">内部代客</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="确认人" prop="confirmByName" width="100" show-overflow-tooltip />
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="220" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-hasPermi="['lunaerp:finance:customer-reconciliation:query']"
                  link
                  type="primary"
                  @click="handleDetail(row.id)"
                >
                  详情
                </el-button>
                <el-button
                  v-hasPermi="['lunaerp:finance:customer-reconciliation:print']"
                  link
                  type="primary"
                  @click="handlePrint(row)"
                >
                  打印
                </el-button>

                <!-- 草稿态操作 -->
                <template v-if="row.status === 'DRAFT'">
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:update']"
                    link
                    type="primary"
                    @click="handleAddEntry(row)"
                  >
                    追加分录
                  </el-button>
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:send']"
                    link
                    type="success"
                    @click="handleSend(row)"
                  >
                    发送
                  </el-button>
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:update']"
                    link
                    type="primary"
                    @click="handleEditRemark(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:delete']"
                    link
                    type="danger"
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-button>
                </template>

                <!-- 已发出操作 -->
                <template v-else-if="row.status === 'SENT'">
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:confirm-on-behalf']"
                    link
                    type="success"
                    @click="handleConfirmOnBehalf(row)"
                  >
                    代客确认
                  </el-button>
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:update']"
                    link
                    type="danger"
                    @click="handleReject(row)"
                  >
                    驳回
                  </el-button>
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:void']"
                    link
                    type="warning"
                    @click="handleVoid(row)"
                  >
                    作废
                  </el-button>
                </template>

                <!-- 已驳回操作 -->
                <template v-else-if="row.status === 'REJECTED'">
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:update']"
                    link
                    type="primary"
                    @click="handleRejectReply(row)"
                  >
                    回复处理
                  </el-button>
                  <el-button
                    v-hasPermi="['lunaerp:finance:customer-reconciliation:void']"
                    link
                    type="warning"
                    @click="handleVoid(row)"
                  >
                    作废
                  </el-button>
                </template>
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
      </el-tab-pane>

      <!-- ====================== TAB 2: FIFO 账龄分析 ====================== -->
      <el-tab-pane label="FIFO 账龄分析" name="aging">
        <!-- 搜索表单 -->
        <el-card shadow="never" class="mb-4">
          <el-form ref="agingQueryFormRef" :model="agingQueryParams" :inline="true" label-width="90px">
            <el-form-item label="账龄基准日">
              <el-date-picker
                v-model="agingQueryParams.agingDate"
                type="date"
                placeholder="请选择基准日"
                value-format="YYYY-MM-DD"
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="客户过滤">
              <el-input
                v-model="agingCustomerKeyword"
                placeholder="输入客户名称过滤"
                clearable
                style="width: 180px"
              />
            </el-form-item>
            <el-form-item label="币种">
              <el-select v-model="agingQueryParams.currency" placeholder="全部币种" clearable style="width: 120px">
                <el-option label="CNY" value="CNY" />
                <el-option label="USD" value="USD" />
                <el-option label="EUR" value="EUR" />
                <el-option label="HKD" value="HKD" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleAgingQuery">
                <Icon icon="ep:search" class="mr-1" /> 查询
              </el-button>
              <el-button @click="resetAgingQuery">
                <Icon icon="ep:refresh" class="mr-1" /> 重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- KPI 指标概览 -->
        <el-row :gutter="12" class="mb-4">
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">应收余额合计</div>
              <div class="kpi-value text-blue-600">{{ formatAmount(kpiTotalBalance) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">0-30 天</div>
              <div class="kpi-value text-green-600">{{ formatAmount(kpiBucket0To30) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">31-60 天</div>
              <div class="kpi-value text-teal-600">{{ formatAmount(kpiBucket31To60) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">61-90 天</div>
              <div class="kpi-value text-yellow-600">{{ formatAmount(kpiBucket61To90) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">91-180 天</div>
              <div class="kpi-value text-orange-600">{{ formatAmount(kpiBucket91To180) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">181 天以上</div>
              <div class="kpi-value text-purple-600">{{ formatAmount(kpiBucket181Plus) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">其中已逾期</div>
              <div class="kpi-value text-red-600 font-bold">{{ formatAmount(kpiOverdueAmount) }}</div>
            </el-card>
          </el-col>
          <el-col :span="3">
            <el-card shadow="never" class="kpi-card">
              <div class="kpi-title">预收余额</div>
              <div class="kpi-value text-emerald-600">{{ formatAmount(kpiAdvanceBalance) }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 账龄汇总表格 -->
        <el-card shadow="never">
          <el-table v-loading="agingLoading" :data="filteredAgingList" border stripe>
            <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip />
            <el-table-column label="币种" prop="currency" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.currency }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="信用账期" prop="creditDays" width="90" align="center">
              <template #default="{ row }">
                {{ row.creditDays != null ? `${row.creditDays} 天` : '未设置' }}
              </template>
            </el-table-column>
            <el-table-column label="业务员" prop="salesmanName" width="100" align="center" show-overflow-tooltip />
            <el-table-column label="应收余额合计" prop="totalBalance" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-primary">{{ formatAmount(row.totalBalance) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="0-30天" prop="bucket0To30" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.bucket0To30) }}</template>
            </el-table-column>
            <el-table-column label="31-60天" prop="bucket31To60" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.bucket31To60) }}</template>
            </el-table-column>
            <el-table-column label="61-90天" prop="bucket61To90" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.bucket61To90) }}</template>
            </el-table-column>
            <el-table-column label="91-180天" prop="bucket91To180" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.bucket91To180) }}</template>
            </el-table-column>
            <el-table-column label="181天以上" prop="bucket181Plus" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.bucket181Plus) }}</template>
            </el-table-column>
            <el-table-column label="其中已逾期" prop="overdueAmount" width="120" align="right">
              <template #default="{ row }">
                <span :class="{ 'text-red-600 font-bold': Number(row.overdueAmount) > 0 }">
                  {{ formatAmount(row.overdueAmount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="预收余额" prop="advanceBalance" width="120" align="right">
              <template #default="{ row }">
                <span class="text-green-600 font-medium">{{ formatAmount(row.advanceBalance) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleDrillDown(row)">
                  明细下钻
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 生成对账单弹窗 -->
    <CustomerStatementCreateDialog ref="createDialogRef" @success="getList" />

    <!-- 详情弹窗 -->
    <CustomerStatementDetailDialog ref="detailDialogRef" />

    <!-- 追加分录弹窗 -->
    <CustomerStatementAddEntryDialog ref="addEntryDialogRef" @success="getList" />

    <!-- 代客确认弹窗 -->
    <CustomerStatementConfirmOnBehalfDialog ref="confirmOnBehalfDialogRef" @success="getList" />

    <!-- 驳回弹窗 -->
    <CustomerStatementRejectDialog ref="rejectDialogRef" @success="getList" />

    <!-- 异议回复处理弹窗 -->
    <CustomerStatementRejectReplyDialog ref="rejectReplyDialogRef" @success="getList" />

    <!-- 打印调度组件 -->
    <PrintDispatcher biz-type="FINANCE_STATEMENT" ref="printDispatcherRef" />

    <!-- 账龄未核销明细下钻弹窗 -->
    <CustomerAgingDetailDialog ref="agingDetailDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import {
  getCustomerStatementPage,
  updateCustomerStatement,
  deleteCustomerStatement,
  sendCustomerStatement,
  voidCustomerStatement,
  getCustomerAgingSummary,
  CustomerAgingSummaryVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import type { CustomerStatementVO } from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { PrintDispatcher } from '@/components/Print'
import CustomerStatementCreateDialog from './components/CustomerStatementCreateDialog.vue'
import CustomerStatementDetailDialog from './components/CustomerStatementDetailDialog.vue'
import CustomerStatementAddEntryDialog from './components/CustomerStatementAddEntryDialog.vue'
import CustomerStatementConfirmOnBehalfDialog from './components/CustomerStatementConfirmOnBehalfDialog.vue'
import CustomerStatementRejectDialog from './components/CustomerStatementRejectDialog.vue'
import CustomerStatementRejectReplyDialog from './components/CustomerStatementRejectReplyDialog.vue'
import CustomerAgingDetailDialog from './components/CustomerAgingDetailDialog.vue'

defineOptions({ name: 'LunaerpElmuiCustomerReconciliation' })

const activeTab = ref('statement')

// ======================= Tab 1: 对账单逻辑 =======================
const loading = ref(false)
const statementList = ref<CustomerStatementVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const periodRange = ref<[string, string] | null>(null)

const createDialogRef = ref()
const detailDialogRef = ref()
const addEntryDialogRef = ref()
const confirmOnBehalfDialogRef = ref()
const rejectDialogRef = ref()
const rejectReplyDialogRef = ref()
const printDispatcherRef = ref<InstanceType<typeof PrintDispatcher> | null>(null)

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
  if (val === undefined || val === null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return String(val)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
  } catch (err: any) {
    ElMessage.error(err.message || '获取客户对账单列表失败')
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
  queryParams.periodStart = undefined
  queryParams.periodEnd = undefined
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

const handlePrint = (row: CustomerStatementVO) => {
  printDispatcherRef.value?.open(row.id)
}

const handleSend = async (row: CustomerStatementVO) => {
  try {
    await ElMessageBox.confirm(`确定发送对账单【${row.statementNo}】给客户吗？发送后将进入客户对账流程。`, '确认发送', {
      type: 'info',
      confirmButtonText: '确定发送',
      cancelButtonText: '取消'
    })
    await sendCustomerStatement(row.id)
    ElMessage.success('对账单发送成功')
    getList()
  } catch (e) {
    // 用户取消
  }
}

const handleVoid = async (row: CustomerStatementVO) => {
  try {
    await ElMessageBox.confirm(`确定作废对账单【${row.statementNo}】吗？作废后占用的分录将被释放。`, '确认作废', {
      type: 'warning',
      confirmButtonText: '确定作废',
      cancelButtonText: '取消'
    })
    await voidCustomerStatement(row.id)
    ElMessage.success('对账单已作废')
    getList()
  } catch (e) {
    // 用户取消
  }
}

const handleConfirmOnBehalf = (row: CustomerStatementVO) => {
  confirmOnBehalfDialogRef.value?.open(row)
}

const handleReject = (row: CustomerStatementVO) => {
  rejectDialogRef.value?.open(row)
}

const handleRejectReply = (row: CustomerStatementVO) => {
  rejectReplyDialogRef.value?.open(row)
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

// ======================= Tab 2: FIFO 账龄逻辑 =======================
const agingLoading = ref(false)
const agingSummaryList = ref<CustomerAgingSummaryVO[]>([])
const agingDetailDialogRef = ref()
const agingCustomerKeyword = ref('')

const getTodayString = () => {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const agingQueryParams = reactive({
  agingDate: getTodayString(),
  currency: 'CNY'
})

const filteredAgingList = computed(() => {
  if (!agingCustomerKeyword.value) {
    return agingSummaryList.value
  }
  const kw = agingCustomerKeyword.value.trim().toLowerCase()
  return agingSummaryList.value.filter((item) =>
    item.customerName?.toLowerCase().includes(kw)
  )
})

const kpiTotalBalance = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.totalBalance) || 0), 0)
)
const kpiBucket0To30 = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.bucket0To30) || 0), 0)
)
const kpiBucket31To60 = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.bucket31To60) || 0), 0)
)
const kpiBucket61To90 = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.bucket61To90) || 0), 0)
)
const kpiBucket91To180 = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.bucket91To180) || 0), 0)
)
const kpiBucket181Plus = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.bucket181Plus) || 0), 0)
)
const kpiOverdueAmount = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.overdueAmount) || 0), 0)
)
const kpiAdvanceBalance = computed(() =>
  filteredAgingList.value.reduce((acc, cur) => acc + (Number(cur.advanceBalance) || 0), 0)
)

const getAgingList = async () => {
  agingLoading.value = true
  try {
    const res = await getCustomerAgingSummary({
      agingDate: agingQueryParams.agingDate,
      currency: agingQueryParams.currency
    })
    agingSummaryList.value = res || []
  } catch (err: any) {
    ElMessage.error(err.message || '获取账龄汇总数据失败')
    agingSummaryList.value = []
  } finally {
    agingLoading.value = false
  }
}

const handleAgingQuery = () => {
  getAgingList()
}

const resetAgingQuery = () => {
  agingQueryParams.agingDate = getTodayString()
  agingQueryParams.currency = 'CNY'
  agingCustomerKeyword.value = ''
  getAgingList()
}

const handleDrillDown = (row: CustomerAgingSummaryVO) => {
  agingDetailDialogRef.value?.open(row, agingQueryParams.agingDate)
}

const handleTabChange = (name: string | number) => {
  if (name === 'aging' && agingSummaryList.value.length === 0) {
    getAgingList()
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.customer-recon-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.kpi-card {
  text-align: center;
  padding: 8px 4px;
}
.kpi-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.kpi-value {
  font-size: 16px;
  font-weight: 600;
}
.text-primary {
  color: #409eff;
}
</style>
