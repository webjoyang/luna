<template>
  <ContentWrap>
    <!-- 顶部说明提示栏 -->
    <el-alert type="info" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">员工工资核算与两步制发放管理</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          1.
          <strong>两步制流程</strong
          >：人事编制工资单并提交审批；审批通过（APPROVED）后出纳在此执行【发放登记】，跨账户多次发放扣除资金并过账生成
          OUT_SALARY 流水分录；<br />
          2. <strong>次品扣款逐卷追溯</strong>：支持一键从质检域导入当期责任次品（DEFECT
          行），锁定每卷指纹防重；<br />
          3. <strong>全员实发非负门禁</strong>：严格守卫每位员工实发工资不得为负数（025_011）。
        </div>
      </template>
    </el-alert>

    <!-- 搜索筛选表单 -->
    <el-card shadow="never" class="mb-14px !border-gray-200">
      <el-form
        ref="queryFormRef"
        :model="queryParams"
        :inline="true"
        label-width="70px"
        class="-mb-15px"
      >
        <el-form-item label="工资单号" prop="payrollNo">
          <el-input
            v-model="queryParams.payrollNo"
            placeholder="单号模糊搜索"
            clearable
            class="!w-160px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="工资期间" prop="period">
          <el-date-picker
            v-model="queryParams.period"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            clearable
            class="!w-140px"
            @change="handleQuery"
          />
        </el-form-item>

        <el-form-item label="审批状态" prop="approvalStatus">
          <el-select
            v-model="queryParams.approvalStatus"
            placeholder="全部状态"
            clearable
            class="!w-130px"
            @change="handleQuery"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_APPROVAL_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="发放状态" prop="paymentStatus">
          <el-select
            v-model="queryParams.paymentStatus"
            placeholder="全部状态"
            clearable
            class="!w-130px"
            @change="handleQuery"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_PAYROLL_PAYMENT_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-4px" />
            查询
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-4px" />
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作工具栏 -->
    <el-card shadow="never" class="!border-gray-200">
      <div class="mb-14px flex items-center justify-between">
        <div class="flex items-center gap-8px">
          <el-button
            type="primary"
            v-hasPermi="['lunaerp:finance:payroll:create']"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-4px" />
            新建工资单
          </el-button>
        </div>
      </div>

      <!-- 数据列表表格 -->
      <el-table v-loading="loading" :data="list" border style="width: 100%">
        <el-table-column label="工资期间" prop="period" width="105" align="center">
          <template #default="{ row }">
            <el-tag type="primary" class="font-mono font-bold">{{ row.period }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="工资单号" prop="payrollNo" min-width="150" align="center">
          <template #default="{ row }">
            <el-link type="primary" class="font-mono font-bold" @click="handleView(row)">
              {{ row.payrollNo }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="工资表标题" prop="title" min-width="150" show-overflow-tooltip />

        <el-table-column label="应发总计 (元)" prop="earningTotal" width="125" align="right">
          <template #default="{ row }">
            <span class="font-mono font-medium">¥ {{ formatAmount(row.earningTotal) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="扣款总计 (元)" prop="deductionTotal" width="125" align="right">
          <template #default="{ row }">
            <span class="font-mono font-medium text-red-600">
              ¥ {{ formatAmount(row.deductionTotal) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="实发总计 (元)" prop="netAmount" width="135" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-blue-800">
              ¥ {{ formatAmount(row.netAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="已发累计 (元)" prop="paidAmount" width="125" align="right">
          <template #default="{ row }">
            <span class="font-mono text-green-700">¥ {{ formatAmount(row.paidAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="未发余额 (元)" width="125" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-orange-600">
              ¥ {{ formatAmount(calculateUnpaid(row)) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="审批状态" prop="approvalStatus" width="105" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_STATUS" :value="row.approvalStatus" />
          </template>
        </el-table-column>

        <el-table-column label="发放状态" prop="paymentStatus" width="105" align="center">
          <template #default="{ row }">
            <dict-tag
              :type="DICT_TYPE.LUNAERP_PAYROLL_PAYMENT_STATUS"
              :value="row.paymentStatus || 'UNPAID'"
            />
          </template>
        </el-table-column>

        <el-table-column label="创建人" prop="creator" width="100" align="center" />

        <el-table-column label="创建时间" prop="createTime" width="160" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 行操作按钮 -->
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <!-- 查看工作台 -->
            <el-button link type="primary" size="small" @click="handleView(row)"> 查看 </el-button>

            <!-- 编辑 / 修改再提 -->
            <el-button
              v-if="getActionFlags(row).canEdit"
              link
              type="primary"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:update']"
              @click="handleEdit(row)"
            >
              {{ row.approvalStatus === 'REJECTED' ? '修改再提' : '编辑' }}
            </el-button>

            <!-- 提交审批 -->
            <el-button
              v-if="getActionFlags(row).canSubmit"
              link
              type="success"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:submit']"
              @click="handleSubmit(row)"
            >
              提交
            </el-button>

            <!-- 撤回审批 -->
            <el-button
              v-if="getActionFlags(row).canCancelApprove"
              link
              type="warning"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:submit']"
              @click="handleCancelApprove(row)"
            >
              撤回
            </el-button>

            <!-- 发放登记（仅 APPROVED 且未发清） -->
            <el-button
              v-if="getActionFlags(row).canPay"
              link
              type="success"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:pay']"
              @click="handlePay(row)"
            >
              发放
            </el-button>

            <!-- A4 套打（仅 APPROVED） -->
            <PrintDispatcher
              v-if="getActionFlags(row).canPrint"
              biz-type="PAYROLL"
              :biz-id="row.id"
              class="inline-block"
            >
              <template #default="{ open }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  v-hasPermi="['lunaerp:finance:payroll:print']"
                  @click="open()"
                >
                  套打
                </el-button>
              </template>
            </PrintDispatcher>

            <!-- 作废（仅 APPROVED 且零发放，025_006 守卫） -->
            <el-button
              v-if="getActionFlags(row).canCancel"
              link
              type="danger"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:cancel']"
              @click="handleCancel(row)"
            >
              作废
            </el-button>

            <!-- 删除（仅草稿/驳回态） -->
            <el-button
              v-if="getActionFlags(row).canDelete"
              link
              type="danger"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:delete']"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="mt-14px flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 工作台抽屉 -->
    <PayrollWorkspaceDrawer
      v-model="showWorkspace"
      :payroll-id="selectedPayrollId"
      :mode="workspaceMode"
      @success="getList"
    />

    <!-- 发放登记弹窗 -->
    <PaymentRegisterDialog v-model="showPayDialog" :payroll="selectedPayroll" @success="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import { PrintDispatcher } from '@/components/Print'
import {
  getPayrollPage,
  deletePayroll,
  submitPayroll,
  cancelApprovePayroll,
  cancelPayroll
} from '@/api/lunaerp-elmui/finance/payroll'
import type { PayrollRespVO, PayrollPageReqVO } from '@/api/lunaerp-elmui/finance/payroll'
import { formatAmount, getPayrollActionFlags, parsePayrollErrorMessage } from './model/payroll'
import PayrollWorkspaceDrawer from './components/PayrollWorkspaceDrawer.vue'
import PaymentRegisterDialog from './components/PaymentRegisterDialog.vue'

defineOptions({ name: 'LunaerpFinancePayroll' })

const loading = ref(false)
const list = ref<PayrollRespVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()

const showWorkspace = ref(false)
const workspaceMode = ref<'create' | 'edit' | 'view'>('create')
const selectedPayrollId = ref<number>()
const selectedPayroll = ref<PayrollRespVO>()
const showPayDialog = ref(false)

const queryParams = reactive<PayrollPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  payrollNo: undefined,
  period: undefined,
  approvalStatus: undefined,
  paymentStatus: undefined
})

const calculateUnpaid = (row: PayrollRespVO) => {
  const net = Number(row.netAmount) || 0
  const paid = Number(row.paidAmount) || 0
  return Math.max(0, Math.round((net - paid) * 100) / 100)
}

const getActionFlags = (row: PayrollRespVO) => {
  return getPayrollActionFlags(row.approvalStatus, row.paymentStatus, Number(row.paidAmount) || 0)
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getPayrollPage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
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

const handleCreate = () => {
  selectedPayrollId.value = undefined
  workspaceMode.value = 'create'
  showWorkspace.value = true
}

const handleView = (row: PayrollRespVO) => {
  selectedPayrollId.value = row.id
  workspaceMode.value = 'view'
  showWorkspace.value = true
}

const handleEdit = (row: PayrollRespVO) => {
  selectedPayrollId.value = row.id
  workspaceMode.value = 'edit'
  showWorkspace.value = true
}

const handleSubmit = async (row: PayrollRespVO) => {
  await ElMessageBox.confirm(`确认提交工资单【${row.payrollNo}】进入审批流程？`, '提交审批', {
    confirmButtonText: '确定提交',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await submitPayroll(row.id)
    ElMessage.success('提交审批成功')
    getList()
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  }
}

const handleCancelApprove = async (row: PayrollRespVO) => {
  await ElMessageBox.confirm(`确认撤回工资单【${row.payrollNo}】的审批申请？`, '撤回审批', {
    confirmButtonText: '确定撤回',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    await cancelApprovePayroll(row.id)
    ElMessage.success('审批已撤回')
    getList()
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  }
}

const handlePay = (row: PayrollRespVO) => {
  selectedPayroll.value = row
  showPayDialog.value = true
}

const handleCancel = async (row: PayrollRespVO) => {
  const { value: cancelReason } = await ElMessageBox.prompt(
    '请输入作废原因（作废后审批记录保留，次品指纹将释放）：',
    `作废工资单 - ${row.payrollNo}`,
    {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '作废原因不能为空',
      type: 'warning'
    }
  )

  try {
    await cancelPayroll(row.id, cancelReason)
    ElMessage.success('工资单已成功作废')
    getList()
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  }
}

const handleDelete = async (row: PayrollRespVO) => {
  await ElMessageBox.confirm(
    `确认删除工资单草稿【${row.payrollNo}】？删除后将移入回收站。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  try {
    await deletePayroll(row.id)
    ElMessage.success('工资单草稿已删除')
    getList()
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  }
}

onMounted(() => {
  getList()
})
</script>
