<template>
  <el-dialog
    v-model="dialogVisible"
    title="付款登记（出纳执行）"
    width="880px"
    append-to-body
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" label-width="110px">
      <!-- 供应商及账户头部 -->
      <div class="bg-gray-50 border border-gray-200 rounded p-14px mb-16px">
        <div class="grid grid-cols-2 gap-y-10px text-13px">
          <div>
            <span class="text-gray-500">结算供应商：</span>
            <span class="font-bold text-gray-800 text-14px">{{ supplierName || '—' }}</span>
          </div>
          <div class="flex items-center">
            <span class="text-gray-500">当前实时预付余额：</span>
            <span class="font-mono font-bold text-amber-700 text-15px"
              >¥ {{ formatMoney(prepayBalance) }}</span
            >
            <el-tooltip content="刷新预付余额" placement="top">
              <el-button
                link
                type="primary"
                size="small"
                class="ml-6px"
                :loading="balanceLoading"
                @click="fetchPrepayBalance"
              >
                <Icon icon="ep:refresh" />
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <div class="mt-12px pt-12px border-t border-gray-200">
          <el-form-item
            label="付款资金账户"
            prop="accountId"
            :rules="[{ required: true, message: '请选择付款资金账户', trigger: 'change' }]"
          >
            <el-select
              v-model="formData.accountId"
              placeholder="请选择付款资金账户（停用账户不可选）"
              class="!w-full"
              filterable
              :loading="accountsLoading"
            >
              <el-option
                v-for="acc in accountOptions"
                :key="acc.id"
                :label="`${acc.accountName || acc.name || '账户' + acc.id} ${acc.currentBalance !== undefined ? `(可用: ¥${formatMoney(acc.currentBalance)})` : ''} ${isAccountDisabled(acc) ? '【已停用】' : ''}`"
                :value="acc.id"
                :disabled="isAccountDisabled(acc)"
              />
            </el-select>
            <div
              v-if="accountOptions.length === 0 && !accountsLoading"
              class="text-12px text-amber-600 mt-2px"
            >
              当前暂未查询到资金账户，使用默认基本账户 (ID: 1)
            </div>
          </el-form-item>
        </div>
      </div>

      <!-- 勾选核销申请列表 -->
      <div class="mb-16px">
        <div class="flex items-center justify-between mb-8px">
          <div class="flex items-center">
            <span class="font-bold text-14px">核销付款申请列表</span>
            <span class="text-12px text-gray-400 ml-8px">（已按申请创建顺序排列）</span>
          </div>
          <el-button type="primary" link size="small" @click="handleAutoDistribute">
            <Icon icon="ep:magic-stick" class="mr-2px" /> 按顺序自动分配
          </el-button>
        </div>

        <el-table :data="tableItems" border stripe size="small" max-height="260px">
          <el-table-column type="index" label="#" width="45" align="center" />
          <el-table-column label="申请单号" prop="applyNo" min-width="140">
            <template #default="{ row }">
              <span class="font-mono font-medium text-primary">{{ row.applyNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="申请类型" prop="applyType" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.applyType === 'RECON' ? 'primary' : 'success'" size="small">
                {{ row.applyType === 'RECON' ? '对账付款' : '预付付款' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请总额" prop="amount" min-width="110" align="right">
            <template #default="{ row }">
              <span class="font-mono">¥ {{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="剩余待核销" prop="remainAmount" min-width="110" align="right">
            <template #default="{ row }">
              <span class="font-mono font-medium text-emerald-700"
                >¥ {{ formatMoney(row.remainAmount) }}</span
              >
            </template>
          </el-table-column>
          <el-table-column label="本次分配核销金额" min-width="160" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocateAmount"
                :min="0.01"
                :max="row.remainAmount"
                :precision="2"
                :step="100"
                size="small"
                controls-position="right"
                class="!w-140px"
                @change="handleAllocateChange"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 资金结算与抵扣区 -->
      <el-card shadow="never" class="bg-gray-50 !border-gray-200 mb-16px">
        <div class="grid grid-cols-2 gap-x-20px">
          <el-form-item label="本次资金实付" prop="payAmount">
            <el-input-number
              v-model="formData.payAmount"
              :min="0"
              :precision="2"
              :step="100"
              controls-position="right"
              class="!w-full"
              placeholder="从资金账户实际支付金额"
            />
            <div class="text-12px text-gray-400 mt-2px">从所选资金账户实际划扣的资金总额</div>
          </el-form-item>

          <el-form-item label="预付抵扣额" prop="prepayUsedAmount">
            <el-input-number
              v-model="formData.prepayUsedAmount"
              :min="0"
              :max="prepayBalance > 0 ? prepayBalance : 0"
              :precision="2"
              :step="100"
              controls-position="right"
              class="!w-full"
              placeholder="动用预付余额抵扣额"
            />
            <div class="text-12px text-gray-400 mt-2px">
              当前可用预付余额：¥ {{ formatMoney(prepayBalance) }}
            </div>
          </el-form-item>
        </div>

        <!-- 校验汇总栏 -->
        <div
          class="pt-10px mt-6px border-t border-gray-200 flex items-center justify-between text-13px"
        >
          <div>
            <span class="text-gray-500">申请分配合计：</span>
            <span class="font-mono font-bold text-gray-800 text-15px"
              >¥ {{ formatMoney(totalAllocated) }}</span
            >
            <span class="mx-12px text-gray-300">|</span>
            <span class="text-gray-500">实付+抵扣合计：</span>
            <span class="font-mono font-bold text-primary text-15px"
              >¥ {{ formatMoney(totalFunding) }}</span
            >
          </div>

          <div>
            <template v-if="isBalanced">
              <el-tag type="success" effect="dark" size="small">
                <Icon icon="ep:check" class="mr-2px" /> 金额平衡
              </el-tag>
            </template>
            <template v-else>
              <span class="text-red-500 text-12px mr-8px font-medium">
                差额: ¥ {{ formatMoney(Math.abs(totalAllocated - totalFunding)) }}
              </span>
              <el-button size="small" type="warning" plain @click="matchPayAmount">
                一键匹配实付
              </el-button>
            </template>
          </div>
        </div>
      </el-card>

      <!-- 登记备注 -->
      <el-form-item label="登记备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="可选输入本次付款登记的备注说明"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!isBalanced || totalAllocated <= 0"
          v-hasPermi="['lunaerp:finance:payment:register']"
          @click="handleSubmit"
        >
          确认登记
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type {
  PaymentApplyRespVO,
  PaymentRegisterReqVO,
  FinanceAccountSimpleRespVO
} from '@/api/lunaerp-elmui/finance/payment'
import {
  registerPayment,
  getPrepayBalance,
  getFinanceAccountSimpleList
} from '@/api/lunaerp-elmui/finance/payment'
import {
  formatMoney,
  distributePaymentAllocation,
  validatePaymentRegister,
  parsePaymentErrorMessage
} from '../model/payment'

defineOptions({ name: 'PaymentRegisterDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const supplierId = ref<number>()
const supplierName = ref<string>('')
const prepayBalance = ref<number>(0)
const balanceLoading = ref(false)

// 资金账户列表
const accountsLoading = ref(false)
const accountOptions = ref<FinanceAccountSimpleRespVO[]>([])

// 勾选的核销明细列表
interface RegisterTableItem {
  applyId: number
  applyNo: string
  applyType: string
  amount: number
  remainAmount: number
  allocateAmount: number
  createTime?: string
}
const tableItems = ref<RegisterTableItem[]>([])

const formData = reactive({
  accountId: undefined as number | undefined,
  payAmount: 0,
  prepayUsedAmount: 0,
  remark: ''
})

const isAccountDisabled = (acc: FinanceAccountSimpleRespVO): boolean => {
  return (
    acc.status === 'DISABLE' || acc.status === 'disabled' || acc.status === 1 || acc.status === '1'
  )
}

const totalAllocated = computed(() => {
  return Number(
    tableItems.value.reduce((sum, item) => sum + Number(item.allocateAmount || 0), 0).toFixed(2)
  )
})

const totalFunding = computed(() => {
  return Number(
    (Number(formData.payAmount || 0) + Number(formData.prepayUsedAmount || 0)).toFixed(2)
  )
})

const isBalanced = computed(() => {
  return Math.abs(totalAllocated.value - totalFunding.value) <= 0.005
})

const handleAllocateChange = () => {
  // If user only modifies allocations and haven't touched prepay, we can optionally align payAmount
}

const matchPayAmount = () => {
  const prepay = Number(formData.prepayUsedAmount || 0)
  const requiredPay = Math.max(0, Number((totalAllocated.value - prepay).toFixed(2)))
  formData.payAmount = requiredPay
}

const handleAutoDistribute = () => {
  const funds = totalFunding.value > 0 ? totalFunding.value : totalAllocated.value
  const distributed = distributePaymentAllocation(
    tableItems.value.map((t) => ({ applyId: t.applyId, remainAmount: t.remainAmount })),
    funds
  )
  const map = new Map(distributed.map((d) => [d.applyId, d.allocateAmount]))
  tableItems.value.forEach((item) => {
    item.allocateAmount = map.get(item.applyId) || 0
  })
}

const fetchPrepayBalance = async () => {
  if (!supplierId.value) return
  balanceLoading.value = true
  try {
    const res = await getPrepayBalance(supplierId.value)
    prepayBalance.value = Number(res || 0)
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    balanceLoading.value = false
  }
}

const fetchAccounts = async () => {
  accountsLoading.value = true
  try {
    const list = await getFinanceAccountSimpleList()
    accountOptions.value = list || []
    if (accountOptions.value.length > 0 && !formData.accountId) {
      const active = accountOptions.value.find((a) => !isAccountDisabled(a))
      if (active) formData.accountId = active.id
    }
  } catch {
    // 账户接口若尚未在后端落地，提供默认降级账户
    if (accountOptions.value.length === 0) {
      accountOptions.value = [
        { id: 1, name: '基本存款账户 (工行)', status: 'ENABLE', currentBalance: 9999999.0 }
      ]
      formData.accountId = 1
    }
  } finally {
    accountsLoading.value = false
  }
}

/** 打开弹窗并载入选中的申请 */
const open = async (applies: PaymentApplyRespVO[]) => {
  if (!applies || applies.length === 0) {
    ElMessage.warning('请至少选择一笔待核销的付款申请')
    return
  }

  // 跨供应商校验
  const firstSupplierId = applies[0].supplierId
  const hasMultipleSuppliers = applies.some((a) => a.supplierId !== firstSupplierId)
  if (hasMultipleSuppliers) {
    ElMessage.error('付款登记只能同时核销属于同一供应商的申请单')
    return
  }

  supplierId.value = firstSupplierId
  supplierName.value = applies[0].supplierName || '—'

  // 按创建时间正序排列
  const sorted = [...applies].sort((a, b) => {
    const tA = new Date(a.createTime || 0).getTime()
    const tB = new Date(b.createTime || 0).getTime()
    return tA - tB
  })

  tableItems.value = sorted.map((row) => {
    const total = Number(row.amount || 0)
    const allocated = Number(row.allocatedAmount || 0)
    const remain = Math.max(0, Number((total - allocated).toFixed(2)))
    return {
      applyId: row.id,
      applyNo: row.applyNo,
      applyType: row.applyType,
      amount: total,
      remainAmount: remain,
      allocateAmount: remain, // 默认预填剩余可核销额
      createTime: row.createTime
    }
  })

  // 默认实付预填所有分配之和
  const initialSum = Number(
    tableItems.value.reduce((sum, item) => sum + item.allocateAmount, 0).toFixed(2)
  )
  formData.payAmount = initialSum
  formData.prepayUsedAmount = 0
  formData.remark = ''

  dialogVisible.value = true

  // 异步加载账户与预付余额
  await Promise.all([fetchAccounts(), fetchPrepayBalance()])
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  const validation = validatePaymentRegister({
    supplierId: supplierId.value,
    accountId: formData.accountId,
    payAmount: formData.payAmount,
    prepayUsedAmount: formData.prepayUsedAmount,
    prepayBalance: prepayBalance.value,
    items: tableItems.value.map((it) => ({
      applyId: it.applyId,
      applyNo: it.applyNo,
      allocateAmount: it.allocateAmount,
      remainAmount: it.remainAmount
    }))
  })

  if (!validation.valid) {
    ElMessage.warning(validation.error || '付款登记表单校验未通过')
    return
  }

  submitting.value = true
  try {
    const payload: PaymentRegisterReqVO = {
      supplierId: supplierId.value!,
      accountId: formData.accountId!,
      payAmount: formData.payAmount,
      prepayUsedAmount: formData.prepayUsedAmount,
      remark: formData.remark,
      items: tableItems.value.map((it) => ({
        applyId: it.applyId,
        allocateAmount: it.allocateAmount
      }))
    }

    await registerPayment(payload)
    ElMessage.success('付款登记成功')
    dialogVisible.value = false
    emit('success')
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
