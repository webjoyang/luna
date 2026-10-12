<template>
  <ContentWrap>
    <el-tabs v-model="activeTab" class="payment-tabs" @tab-change="handleTabChange">
      <!-- ====================== TAB 1: 付款申请 ====================== -->
      <el-tab-pane label="付款申请" name="apply">
        <!-- 搜索区 -->
        <el-form
          class="-mb-15px mt-2"
          :model="applyQuery"
          ref="applyQueryFormRef"
          :inline="true"
          label-width="80px"
        >
          <el-form-item label="关键词" prop="keyword">
            <el-input
              v-model="applyQuery.keyword"
              placeholder="申请单号 / 供应商名称"
              clearable
              @keyup.enter="handleApplyQuery"
              class="!w-220px"
            />
          </el-form-item>

          <el-form-item label="供应商" prop="supplierId">
            <el-select
              v-model="applyQuery.supplierId"
              placeholder="全部供应商"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchSuppliers"
              :loading="supplierLoading"
              class="!w-200px"
            >
              <el-option
                v-for="item in supplierOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="类型" prop="applyType">
            <el-select v-model="applyQuery.applyType" placeholder="全部类型" clearable class="!w-140px">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="审批状态" prop="approvalStatus">
            <el-select
              v-model="applyQuery.approvalStatus"
              placeholder="全部状态"
              clearable
              class="!w-140px"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button @click="handleApplyQuery">
              <Icon icon="ep:search" class="mr-2px" />搜索
            </el-button>
            <el-button @click="resetApplyQuery">
              <Icon icon="ep:refresh" class="mr-2px" />重置
            </el-button>
            <el-button
              type="primary"
              plain
              @click="openCreateApply()"
              v-hasPermi="['lunaerp:finance:payment:apply']"
            >
              <Icon icon="ep:plus" class="mr-2px" />发起付款申请
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 申请单列表表格 -->
        <el-table v-loading="applyLoading" :data="applyList" border stripe class="mt-4">
          <el-table-column label="申请单号" prop="applyNo" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" :underline="false" @click="openApplyDetail(row.id)">
                <span class="cell-code">{{ row.applyNo }}</span>
              </el-link>
            </template>
          </el-table-column>

          <el-table-column label="申请类型" prop="applyType" width="110" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
            </template>
          </el-table-column>

          <el-table-column label="供应商名称" prop="supplierName" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.supplierName || '—' }}</template>
          </el-table-column>

          <el-table-column label="关联对账单" prop="reconNo" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.reconNo" class="cell-code text-xs">{{ row.reconNo }}</span>
              <span v-else class="text-gray-400">—</span>
            </template>
          </el-table-column>

          <el-table-column label="申请金额" prop="amount" width="130" align="right">
            <template #default="{ row }">
              <span class="font-bold text-red-600">￥{{ formatAmount(row.amount) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="已核销金额" prop="allocatedAmount" width="130" align="right">
            <template #default="{ row }">
              <span class="font-bold text-green-600">￥{{ formatAmount(row.allocatedAmount || 0) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="审批状态" prop="approvalStatus" width="110" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="row.approvalStatus" />
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
          <el-table-column label="操作" width="230" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openApplyDetail(row.id)"
                v-hasPermi="['lunaerp:finance:payment:query']"
              >
                详情
              </el-button>

              <!-- 草稿态操作 (PENDING 或 DRAFT) -->
              <template v-if="row.approvalStatus === 'PENDING' || row.approvalStatus === 'DRAFT'">
                <el-button
                  link
                  type="primary"
                  @click="openEditApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="success"
                  @click="handleSubmitApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  提交
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleDeleteApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  删除
                </el-button>
              </template>

              <!-- 审批中操作 (APPROVING) -->
              <template v-else-if="row.approvalStatus === 'APPROVING'">
                <el-button
                  link
                  type="warning"
                  @click="handleCancelApproveApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  撤回
                </el-button>
              </template>

              <!-- 已驳回操作 (REJECTED) -->
              <template v-else-if="row.approvalStatus === 'REJECTED'">
                <el-button
                  link
                  type="primary"
                  @click="openEditApply(row)"
                  v-hasPermi="['lunaerp:finance:payment:apply']"
                >
                  重新编辑
                </el-button>
              </template>

              <!-- 已通过 (APPROVED) -->
              <template v-else-if="row.approvalStatus === 'APPROVED'">
                <el-button
                  link
                  type="success"
                  @click="handleGoToRegisterTab(row)"
                >
                  前往付款登记
                </el-button>
              </template>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无付款申请数据" :image-size="80" />
          </template>
        </el-table>

        <!-- 分页 -->
        <Pagination
          :total="applyTotal"
          v-model:page="applyQuery.pageNo"
          v-model:limit="applyQuery.pageSize"
          @pagination="getApplyList"
        />
      </el-tab-pane>

      <!-- ====================== TAB 2: 付款登记 ====================== -->
      <el-tab-pane label="付款登记" name="register">
        <!-- 上半区：待核销付款申请 -->
        <div class="mb-6 p-4 bg-[#f8fafc] rounded border border-[#e2e8f0]">
          <div class="flex items-center justify-between mb-3 pb-2 border-b border-[#e2e8f0]">
            <div class="flex items-center gap-2">
              <span class="font-bold text-sm text-[#1e293b]">待核销付款申请（已审批未核销完）</span>
              <el-tag size="small" type="success">审批已通过</el-tag>
            </div>
            <div class="flex items-center gap-2">
              <el-select
                v-model="pendingSupplierId"
                placeholder="筛选供应商"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                class="!w-200px"
                @change="getPendingApplies"
              >
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
              <el-button @click="getPendingApplies">
                <Icon icon="ep:refresh" class="mr-2px" />刷新
              </el-button>
              <el-button
                type="primary"
                :disabled="selectedPendingApplies.length === 0"
                @click="openRegisterDialog(selectedPendingApplies)"
                v-hasPermi="['lunaerp:finance:payment:register']"
              >
                <Icon icon="ep:money" class="mr-2px" />
                付款登记 (已选 {{ selectedPendingApplies.length }} 笔)
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="pendingLoading"
            :data="pendingApplies"
            border
            size="small"
            stripe
            @selection-change="handlePendingSelectionChange"
          >
            <el-table-column type="selection" width="45" align="center" />
            <el-table-column label="申请单号" prop="applyNo" min-width="140">
              <template #default="{ row }">
                <span class="cell-code">{{ row.applyNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" prop="applyType" width="100" align="center">
              <template #default="{ row }">
                <dict-tag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
              </template>
            </el-table-column>
            <el-table-column label="供应商" prop="supplierName" min-width="150" show-overflow-tooltip />
            <el-table-column label="申请金额" prop="amount" width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold text-red-600">￥{{ formatAmount(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已核销金额" prop="allocatedAmount" width="120" align="right">
              <template #default="{ row }">
                ￥{{ formatAmount(row.allocatedAmount || 0) }}
              </template>
            </el-table-column>
            <el-table-column label="剩余可核销" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-green-600">
                  ￥{{ formatAmount(row.amount - (row.allocatedAmount || 0)) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              label="申请时间"
              prop="createTime"
              width="150"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openRegisterDialog([row])"
                  v-hasPermi="['lunaerp:finance:payment:register']"
                >
                  去登记
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前无待核销的付款申请" :image-size="60" />
            </template>
          </el-table>
        </div>

        <!-- 下半区：付款单流水列表 -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <span class="font-bold text-sm text-[#1e293b]">付款单流水列表</span>
          </div>

          <el-form
            class="-mb-15px"
            :model="paymentQuery"
            ref="paymentQueryFormRef"
            :inline="true"
            label-width="80px"
          >
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                v-model="paymentQuery.supplierId"
                placeholder="全部供应商"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                class="!w-200px"
              >
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="资金账户" prop="accountId">
              <el-select
                v-model="paymentQuery.accountId"
                placeholder="全部账户"
                clearable
                class="!w-180px"
              >
                <el-option
                  v-for="acc in DEFAULT_FINANCE_ACCOUNTS"
                  :key="acc.id"
                  :label="acc.name"
                  :value="acc.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button @click="handlePaymentQuery">
                <Icon icon="ep:search" class="mr-2px" />搜索
              </el-button>
              <el-button @click="resetPaymentQuery">
                <Icon icon="ep:refresh" class="mr-2px" />重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="paymentLoading" :data="paymentList" border stripe class="mt-4">
            <el-table-column label="付款单号" prop="paymentNo" min-width="150">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="openPaymentDetail(row.id)">
                  <span class="cell-code">{{ row.paymentNo }}</span>
                </el-link>
              </template>
            </el-table-column>

            <el-table-column label="收款供应商" prop="supplierName" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.supplierName || '—' }}</template>
            </el-table-column>

            <el-table-column label="付款资金账户" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ getAccountName(row.accountId) }}
              </template>
            </el-table-column>

            <el-table-column label="实付金额" prop="payAmount" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-red-600">￥{{ formatAmount(row.payAmount) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="预付抵扣" prop="prepayUsedAmount" width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold text-orange-600">￥{{ formatAmount(row.prepayUsedAmount || 0) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="支付总计" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-blue-600">
                  ￥{{ formatAmount(Number(row.payAmount || 0) + Number(row.prepayUsedAmount || 0)) }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="核销申请单" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <template v-if="row.items && row.items.length > 0">
                  <el-tag
                    v-for="refItem in row.items"
                    :key="refItem.id || refItem.applyNo"
                    size="small"
                    class="mr-1 mb-1"
                  >
                    {{ refItem.applyNo }} (￥{{ formatAmount(refItem.allocateAmount) }})
                  </el-tag>
                </template>
                <span v-else class="text-gray-400">—</span>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="90" align="center">
              <template #default>
                <el-tag type="success" size="small">已完成</el-tag>
              </template>
            </el-table-column>

            <el-table-column
              label="登记时间"
              prop="createTime"
              width="160"
              align="center"
              :formatter="dateFormatter"
            />

            <!-- 操作列（付款单无编辑/删除操作，红冲二期） -->
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openPaymentDetail(row.id)"
                  v-hasPermi="['lunaerp:finance:payment:query']"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无付款单流水数据" :image-size="80" />
            </template>
          </el-table>

          <Pagination
            :total="paymentTotal"
            v-model:page="paymentQuery.pageNo"
            v-model:limit="paymentQuery.pageSize"
            @pagination="getPaymentList"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <!-- 付款申请发起/编辑弹窗 -->
  <PaymentApplyFormDialog ref="applyFormDialogRef" @success="handleApplySuccess" />

  <!-- 付款申请详情抽屉 -->
  <PaymentApplyDetailDrawer ref="applyDetailDrawerRef" @go-to-register="handleGoToRegisterFromDrawer" />

  <!-- 付款登记弹窗 -->
  <PaymentRegisterDialog ref="registerDialogRef" @success="handleRegisterSuccess" />

  <!-- 付款单流水详情弹窗 -->
  <PaymentDetailDialog ref="paymentDetailDialogRef" />
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { useMessage } from '@/hooks/web/useMessage'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type {
  PaymentApplyVO,
  PaymentApplyPageParam,
  PaymentVO,
  PaymentPageParam
} from '@/api/lunaerp-elmui/finance/payment'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import { formatAmount, DEFAULT_FINANCE_ACCOUNTS } from './utils'
import PaymentApplyFormDialog from './components/PaymentApplyFormDialog.vue'
import PaymentApplyDetailDrawer from './components/PaymentApplyDetailDrawer.vue'
import PaymentRegisterDialog from './components/PaymentRegisterDialog.vue'
import PaymentDetailDialog from './components/PaymentDetailDialog.vue'

defineOptions({ name: 'LunaerpElmuiPayment' })

const route = useRoute()
const message = useMessage()

const activeTab = ref<'apply' | 'register'>('apply')

// 供应商下拉列表
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}

// ====================== 付款申请逻辑 ======================
const applyLoading = ref(false)
const applyTotal = ref(0)
const applyList = ref<PaymentApplyVO[]>([])
const applyQueryFormRef = ref()
const applyFormDialogRef = ref()
const applyDetailDrawerRef = ref()

const applyQuery = reactive<PaymentApplyPageParam>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  supplierId: undefined,
  applyType: undefined,
  approvalStatus: undefined
})

const getApplyList = async () => {
  applyLoading.value = true
  try {
    const res = await PaymentApi.getPaymentApplyPage(applyQuery)
    applyList.value = res.list
    applyTotal.value = res.total
  } finally {
    applyLoading.value = false
  }
}

const handleApplyQuery = () => {
  applyQuery.pageNo = 1
  getApplyList()
}

const resetApplyQuery = () => {
  applyQueryFormRef.value?.resetFields()
  handleApplyQuery()
}

const openCreateApply = () => {
  applyFormDialogRef.value?.open('create')
}

const openEditApply = (row: PaymentApplyVO) => {
  applyFormDialogRef.value?.open('edit', row)
}

const openApplyDetail = (id: number) => {
  applyDetailDrawerRef.value?.open(id)
}

const handleApplySuccess = () => {
  getApplyList()
}

const handleSubmitApply = async (row: PaymentApplyVO) => {
  try {
    await message.confirm(`确定提交付款申请【${row.applyNo}】进行审批吗？`, '提交审批')
    await PaymentApi.submitPaymentApply(row.id)
    message.success('已提交审批')
    getApplyList()
  } catch {}
}

const handleCancelApproveApply = async (row: PaymentApplyVO) => {
  try {
    await message.confirm(`确定撤回付款申请【${row.applyNo}】的审批吗？`, '撤回审批')
    await PaymentApi.cancelApprovePaymentApply(row.id)
    message.success('已撤回审批')
    getApplyList()
  } catch {}
}

const handleDeleteApply = async (row: PaymentApplyVO) => {
  try {
    await message.confirm(`确定删除草稿付款申请【${row.applyNo}】吗？删除后进入回收站。`, '删除申请')
    await PaymentApi.deletePaymentApply(row.id)
    message.success('已移入回收站')
    getApplyList()
  } catch {}
}

const handleGoToRegisterTab = (row: PaymentApplyVO) => {
  activeTab.value = 'register'
  pendingSupplierId.value = row.supplierId
  getPendingApplies()
}

const handleGoToRegisterFromDrawer = (apply: PaymentApplyVO) => {
  handleGoToRegisterTab(apply)
}

// ====================== 付款登记逻辑 ======================
const pendingLoading = ref(false)
const pendingSupplierId = ref<number | undefined>(undefined)
const pendingApplies = ref<PaymentApplyVO[]>([])
const selectedPendingApplies = ref<PaymentApplyVO[]>([])
const registerDialogRef = ref()

const paymentLoading = ref(false)
const paymentTotal = ref(0)
const paymentList = ref<PaymentVO[]>([])
const paymentQueryFormRef = ref()
const paymentDetailDialogRef = ref()

const paymentQuery = reactive<PaymentPageParam>({
  pageNo: 1,
  pageSize: 20,
  supplierId: undefined,
  accountId: undefined
})

const getAccountName = (accountId: number) => {
  const acc = DEFAULT_FINANCE_ACCOUNTS.find((a) => a.id === accountId)
  return acc ? acc.name : `账户 #${accountId}`
}

/** 查询 APPROVED 且未核销完的付款申请 */
const getPendingApplies = async () => {
  pendingLoading.value = true
  try {
    const res = await PaymentApi.getPaymentApplyPage({
      pageNo: 1,
      pageSize: 100,
      approvalStatus: 'APPROVED',
      supplierId: pendingSupplierId.value
    })
    // 过滤出未核销完的单据 (amount > allocatedAmount)
    pendingApplies.value = (res.list || []).filter(
      (item) => item.amount - (item.allocatedAmount || 0) > 0.001
    )
  } finally {
    pendingLoading.value = false
  }
}

const handlePendingSelectionChange = (selection: PaymentApplyVO[]) => {
  selectedPendingApplies.value = selection
}

const openRegisterDialog = (applies: PaymentApplyVO[]) => {
  registerDialogRef.value?.open(applies)
}

const handleRegisterSuccess = () => {
  getPendingApplies()
  getPaymentList()
  getApplyList()
}

/** 查询已登记付款单列表 */
const getPaymentList = async () => {
  paymentLoading.value = true
  try {
    const res = await PaymentApi.getPaymentPage(paymentQuery)
    paymentList.value = res.list
    paymentTotal.value = res.total
  } finally {
    paymentLoading.value = false
  }
}

const handlePaymentQuery = () => {
  paymentQuery.pageNo = 1
  getPaymentList()
}

const resetPaymentQuery = () => {
  paymentQueryFormRef.value?.resetFields()
  handlePaymentQuery()
}

const openPaymentDetail = (id: number) => {
  paymentDetailDialogRef.value?.open(id)
}

const handleTabChange = (name: any) => {
  if (name === 'apply') {
    getApplyList()
  } else if (name === 'register') {
    getPendingApplies()
    getPaymentList()
  }
}

onMounted(async () => {
  await searchSuppliers('')
  await getApplyList()

  // 检查是否从供应商对账单直接跳转过来生成付款申请
  if (route.query.reconId && route.query.action === 'create') {
    activeTab.value = 'apply'
    const reconId = Number(route.query.reconId)
    const supplierId = route.query.supplierId ? Number(route.query.supplierId) : undefined
    applyFormDialogRef.value?.open('create', {
      applyType: 'RECON',
      reconId,
      supplierId
    })
  }
})
</script>

<style scoped>
.cell-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 500;
  color: var(--el-color-primary);
}
</style>
