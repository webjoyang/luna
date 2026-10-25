<template>
  <ContentWrap>
    <!-- 顶部说明提示栏 -->
    <el-alert type="info" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">员工费用报销管理与审批过账</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          1.
          <strong>审批一步过账</strong
          >：报销单提交后进入统一审批流；审批通过（APPROVED）后同事务自动在指定付款账户扣款并生成
          CREDIT / OUT_EXPENSE 资金流水分录；<br />
          2. <strong>终态保护</strong>：审批通过（APPROVED）为终态单据，只读防篡改，可直接调用 A4
          套打；草稿或驳回单据可修改后再提。
        </div>
      </template>
    </el-alert>

    <!-- 搜索与筛选表单（四维筛选：报销人、审批状态、期间、费用类型） -->
    <el-card shadow="never" class="mb-14px !border-gray-200">
      <el-form
        ref="queryFormRef"
        :model="queryParams"
        :inline="true"
        label-width="70px"
        class="-mb-15px"
      >
        <!-- 报销人员（远程搜索） -->
        <el-form-item label="报销人员" prop="applicantUserId">
          <UserSelect
            v-model="queryParams.applicantUserId"
            placeholder="搜索报销人"
            clearable
            class="!w-160px"
          />
        </el-form-item>

        <!-- 审批状态 -->
        <el-form-item label="审批状态" prop="approvalStatus">
          <el-select
            v-model="queryParams.approvalStatus"
            placeholder="全部状态"
            clearable
            class="!w-130px"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_APPROVAL_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <!-- 费用类型 -->
        <el-form-item label="费用类别" prop="category">
          <el-select
            v-model="queryParams.category"
            placeholder="全部类别"
            clearable
            class="!w-130px"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_EXPENSE_CATEGORY)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <!-- 业务期间（发生日期范围） -->
        <el-form-item label="业务期间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="截止日期"
            class="!w-240px"
            @change="handleDateRangeChange"
          />
        </el-form-item>

        <!-- 报销单号 -->
        <el-form-item label="单据单号" prop="expenseNo">
          <el-input
            v-model="queryParams.expenseNo"
            placeholder="搜索报销单号"
            clearable
            class="!w-160px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-4px" /> 查询
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-4px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作工具栏与数据表格 -->
    <el-card shadow="never" class="!border-gray-200">
      <div class="mb-14px flex justify-between items-center">
        <div class="flex items-center gap-8px">
          <el-button
            type="primary"
            v-hasPermi="['lunaerp:finance:expense:create']"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-4px" /> 新建报销
          </el-button>
        </div>
        <div class="text-12px text-gray-500">
          共查询到 <span class="font-mono font-bold text-gray-800">{{ total }}</span> 条报销记录
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe size="small" class="w-full">
        <!-- 报销单号 -->
        <el-table-column label="报销单号" prop="expenseNo" min-width="150" fixed="left">
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              class="font-mono font-bold"
              @click="handleDetail(row)"
            >
              {{ row.expenseNo }}
            </el-link>
          </template>
        </el-table-column>

        <!-- 报销人 -->
        <el-table-column label="报销人" prop="applicantName" width="110" align="center">
          <template #default="{ row }">
            <span class="font-medium text-gray-900">{{ row.applicantName || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 付款账户 -->
        <el-table-column label="付款账户" prop="accountName" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.accountName || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 币种 -->
        <el-table-column label="币种" prop="currency" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info" class="font-mono">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>

        <!-- 原币金额（千分位，安全兼容脱敏） -->
        <el-table-column label="原币金额" prop="originalTotal" width="130" align="right">
          <template #default="{ row }">
            <span class="font-mono font-medium">
              {{ row.currency }} {{ formatAmount(row.originalTotal) }}
            </span>
          </template>
        </el-table-column>

        <!-- 报销总额（折合或实际，千分位，安全兼容脱敏） -->
        <el-table-column label="报销总额" prop="totalAmount" width="130" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-orange-600">
              {{ row.currency }} {{ formatAmount(row.totalAmount) }}
            </span>
          </template>
        </el-table-column>

        <!-- 业务日期 -->
        <el-table-column label="业务日期" prop="expenseDate" width="110" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px">{{ row.expenseDate || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 审批状态 -->
        <el-table-column label="审批状态" prop="approvalStatus" width="105" align="center">
          <template #default="{ row }">
            <dict-tag
              :type="DICT_TYPE.LUNAERP_APPROVAL_STATUS"
              :value="row.approvalStatus"
              size="small"
            />
          </template>
        </el-table-column>

        <!-- 备注说明 -->
        <el-table-column label="备注说明" prop="remark" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 创建时间 -->
        <el-table-column label="创建时间" prop="createTime" width="150" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px text-gray-500">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <!-- 详情 -->
            <el-button link type="primary" size="small" @click="handleDetail(row)">
              详情
            </el-button>

            <!-- 编辑（草稿/驳回态） -->
            <el-button
              v-if="getExpenseActionFlags(row.approvalStatus).canEdit"
              v-hasPermi="['lunaerp:finance:expense:update']"
              link
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              {{ getExpenseActionFlags(row.approvalStatus).isRejected ? '修改再提' : '编辑' }}
            </el-button>

            <!-- 提交审批（草稿/驳回态） -->
            <el-button
              v-if="getExpenseActionFlags(row.approvalStatus).canSubmit"
              v-hasPermi="['lunaerp:finance:expense:submit']"
              link
              type="success"
              size="small"
              @click="handleSubmitApprove(row)"
            >
              提交
            </el-button>

            <!-- 撤回（审批中） -->
            <el-button
              v-if="getExpenseActionFlags(row.approvalStatus).canWithdraw"
              v-hasPermi="['lunaerp:finance:expense:submit']"
              link
              type="warning"
              size="small"
              @click="handleWithdraw(row)"
            >
              撤回
            </el-button>

            <!-- A4套打（仅APPROVED） -->
            <PrintDispatcher
              v-if="getExpenseActionFlags(row.approvalStatus).isApproved"
              biz-type="EXPENSE"
              :biz-id="row.id"
              class="inline-block"
            >
              <template #default="{ open }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-hasPermi="['lunaerp:finance:expense:print']"
                  @click="open()"
                >
                  套打
                </el-button>
              </template>
            </PrintDispatcher>

            <!-- 删除（草稿/驳回态） -->
            <el-button
              v-if="getExpenseActionFlags(row.approvalStatus).canDelete"
              v-hasPermi="['lunaerp:finance:expense:delete']"
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="flex justify-end mt-16px">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 工作台弹窗（新建/编辑） -->
    <ExpenseFormDialog ref="formDialogRef" @success="getList" />

    <!-- 详情抽屉（含审批进度时间线） -->
    <ExpenseDetailDrawer ref="detailDrawerRef" @refresh="getList" @edit="handleEditById" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import { PrintDispatcher } from '@/components/Print'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import ExpenseFormDialog from './components/ExpenseFormDialog.vue'
import ExpenseDetailDrawer from './components/ExpenseDetailDrawer.vue'
import * as ExpenseApi from '@/api/lunaerp-elmui/finance/expense'
import { formatAmount, getExpenseActionFlags, parseExpenseErrorMessage } from './model/expense'

defineOptions({ name: 'LunaerpFinanceExpense' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<ExpenseApi.ExpenseRespVO[]>([])
const dateRange = ref<[string, string] | null>(null)

const queryFormRef = ref()
const formDialogRef = ref<InstanceType<typeof ExpenseFormDialog>>()
const detailDrawerRef = ref<InstanceType<typeof ExpenseDetailDrawer>>()

const queryParams = reactive<ExpenseApi.ExpensePageReqVO>({
  pageNo: 1,
  pageSize: 20,
  expenseNo: undefined,
  applicantUserId: undefined,
  approvalStatus: undefined,
  category: undefined,
  expenseDateStart: undefined,
  expenseDateEnd: undefined
})

const handleDateRangeChange = (val: [string, string] | null) => {
  if (val && val.length === 2) {
    queryParams.expenseDateStart = val[0]
    queryParams.expenseDateEnd = val[1]
  } else {
    queryParams.expenseDateStart = undefined
    queryParams.expenseDateEnd = undefined
  }
}

const getList = async () => {
  loading.value = true
  try {
    const res = await ExpenseApi.getExpensePage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (err: any) {
    message.error(parseExpenseErrorMessage(err))
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  dateRange.value = null
  queryFormRef.value?.resetFields()
  queryParams.expenseDateStart = undefined
  queryParams.expenseDateEnd = undefined
  handleQuery()
}

const handleCreate = () => {
  formDialogRef.value?.open()
}

const handleEdit = (row: ExpenseApi.ExpenseRespVO) => {
  formDialogRef.value?.open(row.id)
}

const handleEditById = (id: number) => {
  formDialogRef.value?.open(id)
}

const handleDetail = (row: ExpenseApi.ExpenseRespVO) => {
  detailDrawerRef.value?.open(row.id)
}

const handleSubmitApprove = async (row: ExpenseApi.ExpenseRespVO) => {
  try {
    await message.confirm(`确定提交报销单【${row.expenseNo}】审批吗？`)
    await ExpenseApi.submitExpense(row.id)
    message.success('报销单已提交审批')
    getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      message.error(parseExpenseErrorMessage(err))
    }
  }
}

const handleWithdraw = async (row: ExpenseApi.ExpenseRespVO) => {
  try {
    await message.confirm(`确定撤回报销单【${row.expenseNo}】的审批申请吗？`)
    await ExpenseApi.cancelApproveExpense(row.id)
    message.success('已撤回报销审批')
    getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      message.error(parseExpenseErrorMessage(err))
    }
  }
}

const handleDelete = async (row: ExpenseApi.ExpenseRespVO) => {
  try {
    await message.confirm(`确定删除报销单【${row.expenseNo}】吗？删除后进入回收站。`)
    await ExpenseApi.deleteExpense(row.id)
    message.success('删除成功')
    getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      message.error(parseExpenseErrorMessage(err))
    }
  }
}

onMounted(() => {
  getList()
})
</script>
