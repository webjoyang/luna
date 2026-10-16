<template>
  <div>
    <!-- 搜索过滤区 -->
    <el-card shadow="never" class="mb-14px !border-gray-200">
      <el-form
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="70px"
        class="-mb-15px"
      >
        <el-form-item label="关键词" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="申请单号 / 供应商 / 对账单号 / 用途"
            clearable
            @keyup.enter="handleQuery"
            class="!w-240px"
          />
        </el-form-item>

        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="queryParams.supplierId"
            placeholder="搜索并选择供应商"
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

        <el-form-item label="申请类型" prop="applyType">
          <el-select
            v-model="queryParams.applyType"
            placeholder="全部类型"
            clearable
            class="!w-140px"
          >
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

        <el-form-item>
          <el-button @click="handleQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
          <el-button
            type="primary"
            plain
            v-hasPermi="['lunaerp:finance:payment:apply']"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-2px" /> 发起付款申请
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表数据区 -->
    <el-card shadow="never" class="!border-gray-200">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="申请单号" prop="applyNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono font-bold text-gray-800">{{ row.applyNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="申请类型" prop="applyType" width="110" align="center">
          <template #default="{ row }">
            <DictTag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
          </template>
        </el-table-column>

        <el-table-column label="供应商" prop="supplierName" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium">{{ row.supplierName || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="申请金额" prop="amount" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-gray-800">¥ {{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="已核销金额" prop="allocatedAmount" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono text-gray-600">¥ {{ formatMoney(row.allocatedAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="剩余待核销" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono font-medium text-emerald-700">
              ¥ {{ formatMoney(calcApplyRemain(row)) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="关联单据 / 用途" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.applyType === 'RECON'">
              <span class="text-gray-400 text-12px">对账单：</span>
              <span class="font-mono">{{ row.reconNo || '—' }}</span>
            </div>
            <div v-else>
              <span class="text-gray-400 text-12px">用途：</span>
              <span>{{ row.purpose || '—' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="审批状态" prop="approvalStatus" width="110" align="center">
          <template #default="{ row }">
            <DictTag :type="DICT_TYPE.LUNAERP_APPROVAL_STATUS" :value="row.approvalStatus" />
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="160" align="center">
          <template #default="{ row }">
            <span class="text-12px text-gray-500">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 操作分叉栏 -->
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <!-- 状态 1：草稿/待提交/驳回态 -->
            <template v-if="row.approvalStatus === 'PENDING' || row.approvalStatus === 'DRAFT'">
              <el-button
                link
                type="primary"
                v-hasPermi="['lunaerp:finance:payment:apply']"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="success"
                v-hasPermi="['lunaerp:finance:payment:apply']"
                @click="handleSubmitApprove(row)"
              >
                提交审批
              </el-button>
              <el-button
                link
                type="danger"
                v-hasPermi="['lunaerp:finance:payment:apply']"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>

            <!-- 状态 2：审批中态 -->
            <template v-else-if="row.approvalStatus === 'APPROVING'">
              <el-button
                link
                type="warning"
                v-hasPermi="['lunaerp:finance:payment:apply']"
                @click="handleWithdraw(row)"
              >
                撤回
              </el-button>
              <el-button link type="primary" @click="handleViewApproval(row)"> 审批流 </el-button>
            </template>

            <!-- 状态 3：已驳回态 -->
            <template v-else-if="row.approvalStatus === 'REJECTED'">
              <el-button
                link
                type="primary"
                v-hasPermi="['lunaerp:finance:payment:apply']"
                @click="handleEdit(row)"
              >
                修改重提
              </el-button>
              <el-button link type="info" @click="handleViewApproval(row)"> 驳回原因 </el-button>
            </template>

            <!-- 状态 4：已通过态 -->
            <template v-else-if="row.approvalStatus === 'APPROVED'">
              <span class="text-12px text-gray-400 mr-4px">已审批通过</span>
              <el-button
                link
                type="primary"
                v-hasPermi="['lunaerp:finance:payment:register']"
                @click="goToRegisterTab(row)"
              >
                前往付款登记
              </el-button>
              <el-button link type="info" size="small" @click="handleViewApproval(row)">
                查看
              </el-button>
            </template>

            <!-- 其他（如已撤回） -->
            <template v-else>
              <el-button link type="primary" @click="handleViewApproval(row)"> 详情 </el-button>
            </template>
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

    <!-- 发起/编辑付款申请弹窗 -->
    <PaymentApplyDialog ref="applyDialogRef" @success="getList" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { PaymentApplyRespVO, PaymentApplyPageReqVO } from '@/api/lunaerp-elmui/finance/payment'
import {
  getPaymentApplyPage,
  deletePaymentApply,
  submitPaymentApply,
  cancelApprovePaymentApply
} from '@/api/lunaerp-elmui/finance/payment'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as ApprovalInstanceApi from '@/api/approval/instance'
import PaymentApplyDialog from './PaymentApplyDialog.vue'
import { formatMoney, parsePaymentErrorMessage, calcApplyRemain } from '../model/payment'

defineOptions({ name: 'PaymentApplyTab' })

const emit = defineEmits<{
  (e: 'switch-to-register', row: PaymentApplyRespVO): void
}>()

const router = useRouter()
const loading = ref(false)
const list = ref<PaymentApplyRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const applyDialogRef = ref<InstanceType<typeof PaymentApplyDialog>>()

const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

const queryParams = reactive<PaymentApplyPageReqVO>({
  pageNo: 1,
  pageSize: 20,
  supplierId: undefined,
  applyType: undefined,
  approvalStatus: undefined,
  keyword: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getPaymentApplyPage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
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

const handleCreate = () => {
  applyDialogRef.value?.open()
}

const handleEdit = (row: PaymentApplyRespVO) => {
  applyDialogRef.value?.open(row)
}

const handleSubmitApprove = async (row: PaymentApplyRespVO) => {
  try {
    await ElMessageBox.confirm(`确认提交付款申请【${row.applyNo}】进入审批流程吗？`, '提示', {
      confirmButtonText: '确认提交',
      cancelButtonText: '取消',
      type: 'info'
    })
    await submitPaymentApply(row.id)
    ElMessage.success('已提交审批')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(parsePaymentErrorMessage(err))
    }
  }
}

const handleWithdraw = async (row: PaymentApplyRespVO) => {
  try {
    await ElMessageBox.confirm(`确认撤回审批中申请【${row.applyNo}】回草稿态吗？`, '警告', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelApprovePaymentApply(row.id)
    ElMessage.success('付款申请已撤回至草稿')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(parsePaymentErrorMessage(err))
    }
  }
}

const handleDelete = async (row: PaymentApplyRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确认删除付款申请【${row.applyNo}】吗？删除后将移至回收站。`,
      '警告',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deletePaymentApply(row.id)
    ElMessage.success('付款申请已删除并移至回收站')
    await getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(parsePaymentErrorMessage(err))
    }
  }
}

const handleViewApproval = async (row: PaymentApplyRespVO) => {
  try {
    // 尝试寻找该单据的最新审批实例
    const instance = await ApprovalInstanceApi.getLatestInstanceByBiz({
      bizType: 'payment',
      bizId: row.id
    })
    if (instance && instance.id) {
      router.push({
        path: '/approval/detail',
        query: { id: instance.id }
      })
      return
    }
  } catch {
    // 静默兜底至审批中心我的发起列表
  }
  router.push({
    path: '/approval/mine',
    query: { bizNo: row.applyNo }
  })
}

const goToRegisterTab = (row: PaymentApplyRespVO) => {
  emit('switch-to-register', row)
}

onMounted(() => {
  getList()
  searchSuppliers('')
})

defineExpose({ refresh: getList })
</script>
