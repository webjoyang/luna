<template>
  <div>
    <!-- ==================== 上半区：已审批待付款核销申请列表 ==================== -->
    <el-card shadow="never" class="mb-16px !border-gray-200">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <span class="font-bold text-15px text-gray-800">待付款核销申请（已审批通过）</span>
            <el-tag type="success" size="small" class="ml-10px">出纳待执行</el-tag>
            <span class="text-12px text-gray-400 ml-12px">
              提示：可多选同一供应商的多笔申请，在一个弹窗内按序合并核销、动用预付抵扣并扣款。
            </span>
          </div>

          <div>
            <el-button
              type="primary"
              :disabled="selectedApplies.length === 0"
              v-hasPermi="['lunaerp:finance:payment:register']"
              @click="handleOpenRegisterDialog()"
            >
              <Icon icon="ep:wallet" class="mr-3px" />
              付款登记 {{ selectedApplies.length > 0 ? `(${selectedApplies.length}笔)` : '' }}
            </el-button>
          </div>
        </div>
      </template>

      <!-- 待核销筛选区 -->
      <el-form :model="applyQueryParams" :inline="true" label-width="60px" class="-mb-15px">
        <el-form-item label="供应商">
          <el-select
            v-model="applyQueryParams.supplierId"
            placeholder="筛选供应商"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            class="!w-200px"
            @change="handleApplyQuery"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词">
          <el-input
            v-model="applyQueryParams.keyword"
            placeholder="申请单号 / 对账单号"
            clearable
            @keyup.enter="handleApplyQuery"
            class="!w-200px"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="handleApplyQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetApplyQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 待核销申请表格 -->
      <div class="mt-14px">
        <el-table
          ref="applyTableRef"
          v-loading="applyLoading"
          :data="pendingAppliesList"
          border
          stripe
          size="small"
          @selection-change="handleApplySelectionChange"
        >
          <el-table-column
            type="selection"
            width="45"
            align="center"
            :selectable="canSelectApply"
          />
          <el-table-column label="申请单号" prop="applyNo" min-width="140">
            <template #default="{ row }">
              <span class="font-mono font-medium text-gray-800">{{ row.applyNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="申请类型" prop="applyType" width="100" align="center">
            <template #default="{ row }">
              <DictTag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
            </template>
          </el-table-column>
          <el-table-column label="供应商" prop="supplierName" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="font-medium">{{ row.supplierName || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="申请金额" prop="amount" min-width="110" align="right">
            <template #default="{ row }">
              <span class="font-mono">¥ {{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已核销金额" prop="allocatedAmount" min-width="110" align="right">
            <template #default="{ row }">
              <span class="font-mono text-gray-500">¥ {{ formatMoney(row.allocatedAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="剩余待核销" min-width="120" align="right">
            <template #default="{ row }">
              <span class="font-mono font-bold text-emerald-700">
                ¥ {{ formatMoney(calcRemain(row)) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="关联合同/用途" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.applyType === 'RECON'" class="font-mono text-12px">{{
                row.reconNo || '—'
              }}</span>
              <span v-else class="text-12px text-gray-600">{{ row.purpose || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="150" align="center">
            <template #default="{ row }">
              <span class="text-12px text-gray-500">{{ row.createTime || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                v-hasPermi="['lunaerp:finance:payment:register']"
                @click="handleOpenRegisterDialog([row])"
              >
                直接登记
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="flex justify-end mt-12px">
          <el-pagination
            v-model:current-page="applyQueryParams.pageNo"
            v-model:page-size="applyQueryParams.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="applyTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchPendingApplies"
            @current-change="fetchPendingApplies"
          />
        </div>
      </div>
    </el-card>

    <!-- ==================== 下半区：付款单流水列表（不可修改/删除） ==================== -->
    <el-card shadow="never" class="!border-gray-200">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <span class="font-bold text-15px text-gray-800">已登记付款单列表（写账凭证）</span>
            <span class="text-12px text-gray-400 ml-10px">
              提示：付款单为写账凭证，不可修改或删除；退款冲销走二期红冲流程。
            </span>
          </div>
        </div>
      </template>

      <!-- 付款单筛选表单 -->
      <el-form :model="paymentQueryParams" :inline="true" label-width="70px" class="-mb-15px">
        <el-form-item label="关键词">
          <el-input
            v-model="paymentQueryParams.keyword"
            placeholder="付款单号 / 备注"
            clearable
            @keyup.enter="handlePaymentQuery"
            class="!w-200px"
          />
        </el-form-item>

        <el-form-item label="供应商">
          <el-select
            v-model="paymentQueryParams.supplierId"
            placeholder="筛选供应商"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            class="!w-180px"
            @change="handlePaymentQuery"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="资金账户">
          <el-select
            v-model="paymentQueryParams.accountId"
            placeholder="全部账户"
            clearable
            class="!w-170px"
            @change="handlePaymentQuery"
          >
            <el-option
              v-for="acc in accountOptions"
              :key="acc.id"
              :label="acc.accountName || acc.name || `账户#${acc.id}`"
              :value="acc.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="登记时间">
          <el-date-picker
            v-model="paymentQueryParams.createTime"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="起始时间"
            end-placeholder="截止时间"
            class="!w-320px"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="handlePaymentQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetPaymentQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 付款单表格 -->
      <div class="mt-14px">
        <el-table v-loading="paymentLoading" :data="paymentList" border stripe>
          <el-table-column label="付款单号" prop="paymentNo" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" :underline="false" @click="handleOpenDetail(row.id)">
                <span class="font-mono font-bold">{{ row.paymentNo }}</span>
              </el-link>
            </template>
          </el-table-column>

          <el-table-column label="供应商" prop="supplierName" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="font-medium">{{ row.supplierName || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="资金账户" prop="accountId" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.accountName || `账户 #${row.accountId}` }}</span>
            </template>
          </el-table-column>

          <el-table-column label="本次实付" prop="payAmount" min-width="115" align="right">
            <template #default="{ row }">
              <span class="font-mono font-bold text-red-600"
                >¥ {{ formatMoney(row.payAmount) }}</span
              >
            </template>
          </el-table-column>

          <el-table-column label="预付抵扣" prop="prepayUsedAmount" min-width="115" align="right">
            <template #default="{ row }">
              <span class="font-mono font-medium text-amber-700"
                >¥ {{ formatMoney(row.prepayUsedAmount) }}</span
              >
            </template>
          </el-table-column>

          <el-table-column label="本次结清总额" min-width="125" align="right">
            <template #default="{ row }">
              <span class="font-mono font-bold text-primary">
                ¥ {{ formatMoney(Number(row.payAmount || 0) + Number(row.prepayUsedAmount || 0)) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="核销申请单号" min-width="180">
            <template #default="{ row }">
              <div v-if="row.items && row.items.length > 0" class="flex flex-wrap gap-4px">
                <el-tag
                  v-for="item in row.items"
                  :key="item.id"
                  size="small"
                  type="info"
                  class="font-mono"
                >
                  {{ item.applyNo || `ID#${item.refId}` }} (¥{{ formatMoney(item.allocateAmount) }})
                </el-tag>
              </div>
              <span v-else class="text-gray-400 text-12px">—</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" prop="status" width="95" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'info'" size="small">
                {{ row.status === 'COMPLETED' ? '已完成' : row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="登记人" prop="creator" width="100" align="center">
            <template #default="{ row }">
              <span class="text-12px">{{ row.creator || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="登记时间" prop="createTime" width="150" align="center">
            <template #default="{ row }">
              <span class="text-12px text-gray-500">{{ row.createTime || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleOpenDetail(row.id)"> 详情 </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="flex justify-end mt-12px">
          <el-pagination
            v-model:current-page="paymentQueryParams.pageNo"
            v-model:page-size="paymentQueryParams.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="paymentTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchPaymentList"
            @current-change="fetchPaymentList"
          />
        </div>
      </div>
    </el-card>

    <!-- 弹窗与抽屉 -->
    <PaymentRegisterDialog ref="registerDialogRef" @success="handleRegisterSuccess" />
    <PaymentDetailDrawer ref="detailDrawerRef" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import type {
  PaymentApplyRespVO,
  PaymentRespVO,
  PaymentPageReqVO,
  FinanceAccountSimpleRespVO
} from '@/api/lunaerp-elmui/finance/payment'
import {
  getPaymentApplyPage,
  getPaymentPage,
  getFinanceAccountSimpleList
} from '@/api/lunaerp-elmui/finance/payment'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import PaymentRegisterDialog from './PaymentRegisterDialog.vue'
import PaymentDetailDrawer from './PaymentDetailDrawer.vue'
import { formatMoney, parsePaymentErrorMessage } from '../model/payment'

defineOptions({ name: 'PaymentRegisterTab' })

const registerDialogRef = ref<InstanceType<typeof PaymentRegisterDialog>>()
const detailDrawerRef = ref<InstanceType<typeof PaymentDetailDrawer>>()
const applyTableRef = ref()

// 待核销申请数据
const applyLoading = ref(false)
const pendingAppliesList = ref<PaymentApplyRespVO[]>([])
const applyTotal = ref(0)
const selectedApplies = ref<PaymentApplyRespVO[]>([])

const applyQueryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  supplierId: undefined as number | undefined,
  keyword: undefined as string | undefined
})

// 已登记付款单数据
const paymentLoading = ref(false)
const paymentList = ref<PaymentRespVO[]>([])
const paymentTotal = ref(0)

const paymentQueryParams = reactive<PaymentPageReqVO>({
  pageNo: 1,
  pageSize: 20,
  supplierId: undefined,
  accountId: undefined,
  createTime: undefined,
  keyword: undefined
})

// 供应商与资金账户选项
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])
const accountOptions = ref<FinanceAccountSimpleRespVO[]>([])

const calcRemain = (row: PaymentApplyRespVO): number => {
  if (row.remainAmount !== undefined && row.remainAmount !== null) {
    return Number(row.remainAmount)
  }
  const total = Number(row.amount || 0)
  const allocated = Number(row.allocatedAmount || 0)
  return Math.max(0, Number((total - allocated).toFixed(2)))
}

/** 只能勾选同一供应商的待核销申请 */
const canSelectApply = (row: PaymentApplyRespVO): boolean => {
  if (selectedApplies.value.length === 0) return true
  const firstSupId = selectedApplies.value[0].supplierId
  return row.supplierId === firstSupId
}

const handleApplySelectionChange = (selection: PaymentApplyRespVO[]) => {
  selectedApplies.value = selection
}

const fetchPendingApplies = async () => {
  applyLoading.value = true
  try {
    const res = await getPaymentApplyPage({
      pageNo: applyQueryParams.pageNo,
      pageSize: applyQueryParams.pageSize,
      approvalStatus: 'APPROVED',
      supplierId: applyQueryParams.supplierId,
      keyword: applyQueryParams.keyword
    })
    // 过滤出尚未完全核销完的申请 (remainAmount > 0)
    const list = res.list || []
    pendingAppliesList.value = list.filter((item) => calcRemain(item) > 0)
    applyTotal.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    applyLoading.value = false
  }
}

const fetchPaymentList = async () => {
  paymentLoading.value = true
  try {
    const res = await getPaymentPage(paymentQueryParams)
    paymentList.value = res.list || []
    paymentTotal.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    paymentLoading.value = false
  }
}

const handleApplyQuery = () => {
  applyQueryParams.pageNo = 1
  fetchPendingApplies()
}

const resetApplyQuery = () => {
  applyQueryParams.supplierId = undefined
  applyQueryParams.keyword = undefined
  handleApplyQuery()
}

const handlePaymentQuery = () => {
  paymentQueryParams.pageNo = 1
  fetchPaymentList()
}

const resetPaymentQuery = () => {
  paymentQueryParams.supplierId = undefined
  paymentQueryParams.accountId = undefined
  paymentQueryParams.createTime = undefined
  paymentQueryParams.keyword = undefined
  handlePaymentQuery()
}

const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query ? query.trim() : undefined
    })
    supplierOptions.value = res.list || []
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    supplierLoading.value = false
  }
}

const fetchAccounts = async () => {
  try {
    const list = await getFinanceAccountSimpleList()
    accountOptions.value = list || []
  } catch {
    // 降级兜底
  }
}

const handleOpenRegisterDialog = (items?: PaymentApplyRespVO[]) => {
  const targetItems = items || selectedApplies.value
  if (!targetItems || targetItems.length === 0) {
    ElMessage.warning('请至少勾选一笔待核销的付款申请')
    return
  }
  registerDialogRef.value?.open(targetItems)
}

const handleOpenDetail = (id: number) => {
  detailDrawerRef.value?.open(id)
}

const handleRegisterSuccess = () => {
  selectedApplies.value = []
  fetchPendingApplies()
  fetchPaymentList()
}

/** 外部 Tab 联动调起 */
const selectAndFocusApply = (row: PaymentApplyRespVO) => {
  applyQueryParams.supplierId = row.supplierId
  fetchPendingApplies().then(() => {
    handleOpenRegisterDialog([row])
  })
}

onMounted(() => {
  fetchPendingApplies()
  fetchPaymentList()
  searchSuppliers('')
  fetchAccounts()
})

defineExpose({
  refresh: () => {
    fetchPendingApplies()
    fetchPaymentList()
  },
  selectAndFocusApply
})
</script>
