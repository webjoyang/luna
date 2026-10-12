<template>
  <el-dialog
    v-model="dialogVisible"
    title="付款登记（多申请核销）"
    width="860px"
    append-to-body
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
    >
      <!-- 基础信息行 -->
      <div class="grid grid-cols-2 gap-4">
        <el-form-item label="收款供应商">
          <span class="text-sm font-semibold text-gray-800">{{ supplierName }}</span>
        </el-form-item>

        <el-form-item label="资金账户" prop="accountId">
          <el-select
            v-model="formData.accountId"
            placeholder="请选择付款资金账户"
            class="w-full"
          >
            <el-option
              v-for="acc in DEFAULT_FINANCE_ACCOUNTS"
              :key="acc.id"
              :label="acc.name"
              :value="acc.id"
              :disabled="acc.disabled"
            />
          </el-select>
        </el-form-item>
      </div>

      <!-- 供应商预付余额提示卡片 -->
      <div class="mb-4 p-3 bg-[#f8fafc] border border-[#e2e8f0] rounded flex items-center justify-between text-xs">
        <div>
          <span class="text-gray-500">供应商当前预付余额：</span>
          <span class="text-sm font-bold text-green-600">￥{{ formatAmount(supplierPrepayBalance) }}</span>
          <span class="text-gray-400 ml-2">（可用于本次预付抵扣）</span>
        </div>
        <el-button link type="primary" size="small" @click="fetchPrepayBalance">
          <Icon icon="ep:refresh" class="mr-2px" />刷新余额
        </el-button>
      </div>

      <!-- 勾选申请核销分配表格 -->
      <div class="mb-4">
        <div class="text-13px font-bold text-gray-700 mb-2 flex items-center justify-between">
          <span>待核销付款申请（按创建时间排列，共 {{ allocateItems.length }} 笔）</span>
          <span class="text-xs text-blue-600">
            可核销上限合计：￥{{ formatAmount(maxAllocatableTotal) }}
          </span>
        </div>

        <el-table :data="allocateItems" border size="small" stripe>
          <el-table-column label="申请单号" prop="applyNo" min-width="140">
            <template #default="{ row }">
              <span class="font-mono text-blue-600">{{ row.applyNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" prop="applyType" width="90" align="center">
            <template #default="{ row }">
              <dict-tag :type="DICT_TYPE.LUNAERP_PAYMENT_APPLY_TYPE" :value="row.applyType" />
            </template>
          </el-table-column>
          <el-table-column label="申请总额" prop="amount" width="110" align="right">
            <template #default="{ row }">
              ￥{{ formatAmount(row.amount) }}
            </template>
          </el-table-column>
          <el-table-column label="已核销" prop="allocatedAmount" width="100" align="right">
            <template #default="{ row }">
              ￥{{ formatAmount(row.allocatedAmount || 0) }}
            </template>
          </el-table-column>
          <el-table-column label="剩余可核销" width="110" align="right">
            <template #default="{ row }">
              <span class="text-green-600 font-semibold">￥{{ formatAmount(row.remaining) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="本次核销分配额" min-width="160" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocateAmount"
                :min="0.01"
                :max="row.remaining"
                :precision="2"
                :step="100"
                size="small"
                class="!w-140px"
                @change="handleAllocationChange"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 支付方式与金额分摊 -->
      <div class="grid grid-cols-2 gap-4 bg-[#f8fafc] p-3 rounded border border-[#e2e8f0] mb-4">
        <el-form-item label="本次实付金额" prop="payAmount" class="!mb-0">
          <el-input-number
            v-model="formData.payAmount"
            :min="0"
            :precision="2"
            :step="100"
            class="!w-200px"
          />
          <div class="text-xs text-gray-400 mt-1">从资金账户扣减支出</div>
        </el-form-item>

        <el-form-item label="预付抵扣额" prop="prepayUsedAmount" class="!mb-0">
          <el-input-number
            v-model="formData.prepayUsedAmount"
            :min="0"
            :max="supplierPrepayBalance > 0 ? supplierPrepayBalance : undefined"
            :precision="2"
            :step="100"
            class="!w-200px"
          />
          <div class="text-xs text-gray-400 mt-1">
            抵扣预付款（上限: ￥{{ formatAmount(supplierPrepayBalance) }}）
          </div>
        </el-form-item>
      </div>

      <!-- 实时金额计算与校验指示条 -->
      <div class="p-3 rounded border text-xs mb-4" :class="summaryStatusClass">
        <div class="flex items-center justify-between font-medium">
          <div>
            <span>本次分配合计：</span>
            <span class="text-sm font-bold">￥{{ formatAmount(totalAllocated) }}</span>
          </div>
          <div>
            <span>支付总计（实付 + 抵扣）：</span>
            <span class="text-sm font-bold">￥{{ formatAmount(totalPaid) }}</span>
          </div>
          <div>
            <span>差额：</span>
            <span class="text-sm font-bold">￥{{ formatAmount(paymentDiff) }}</span>
          </div>
        </div>

        <!-- 错误与警示文案 -->
        <div v-if="validationWarningText" class="mt-2 text-red-600 font-bold flex items-center gap-1">
          <Icon icon="ep:warning-filled" />
          <span>{{ validationWarningText }}</span>
        </div>
      </div>

      <el-form-item label="付款备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入付款登记备注（选填）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          确认付款登记
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import { useMessage } from '@/hooks/web/useMessage'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type {
  PaymentApplyVO,
  PaymentRegisterReqVO,
  PaymentRegisterItemReqVO
} from '@/api/lunaerp-elmui/finance/payment'
import { formatAmount, DEFAULT_FINANCE_ACCOUNTS } from '../utils'

const emit = defineEmits(['success'])
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const supplierId = ref<number>(0)
const supplierName = ref<string>('')
const supplierPrepayBalance = ref<number>(0)

interface AllocateItemState {
  applyId: number
  applyNo: string
  applyType: string
  amount: number
  allocatedAmount: number
  remaining: number
  allocateAmount: number
  createTime?: string
}

const allocateItems = ref<AllocateItemState[]>([])

interface FormDataState {
  accountId: number | undefined
  payAmount: number
  prepayUsedAmount: number
  remark: string
}

const formData = reactive<FormDataState>({
  accountId: 1, // 默认基本户
  payAmount: 0,
  prepayUsedAmount: 0,
  remark: ''
})

const maxAllocatableTotal = computed(() => {
  return allocateItems.value.reduce((sum, item) => sum + (item.remaining || 0), 0)
})

const totalAllocated = computed(() => {
  return Number(allocateItems.value.reduce((sum, item) => sum + (item.allocateAmount || 0), 0).toFixed(2))
})

const totalPaid = computed(() => {
  const pay = formData.payAmount || 0
  const prepay = formData.prepayUsedAmount || 0
  return Number((pay + prepay).toFixed(2))
})

const paymentDiff = computed(() => {
  return Number((totalPaid.value - totalAllocated.value).toFixed(2))
})

const validationWarningText = computed(() => {
  if (totalAllocated.value <= 0) {
    return '请至少为一个付款申请分配金额（> 0）'
  }
  for (const item of allocateItems.value) {
    if (item.allocateAmount > item.remaining) {
      return `申请【${item.applyNo}】分配额（￥${formatAmount(item.allocateAmount)}）超过剩余可核销额（￥${formatAmount(item.remaining)}）`
    }
  }
  if (totalAllocated.value > totalPaid.value) {
    return `核销分配合计（￥${formatAmount(totalAllocated.value)}）大于支付总计（￥${formatAmount(totalPaid.value)}），钱不够分！`
  }
  if ((formData.prepayUsedAmount || 0) > supplierPrepayBalance.value) {
    return `预付抵扣额（￥${formatAmount(formData.prepayUsedAmount)}）超出供应商可用预付余额（￥${formatAmount(supplierPrepayBalance.value)}）`
  }
  return ''
})

const summaryStatusClass = computed(() => {
  if (validationWarningText.value) {
    return 'bg-red-50 border-red-200 text-red-800'
  }
  return 'bg-blue-50 border-blue-200 text-blue-900'
})

const canSubmit = computed(() => {
  return !validationWarningText.value && !!formData.accountId && totalAllocated.value > 0
})

const formRules = reactive({
  accountId: [{ required: true, message: '请选择付款资金账户', trigger: 'change' }]
})

const fetchPrepayBalance = async () => {
  if (!supplierId.value) return
  try {
    supplierPrepayBalance.value = await PaymentApi.getPrepayBalance(supplierId.value)
  } catch {}
}

/** 分配额变动时自动联动实付金额 */
const handleAllocationChange = () => {
  // 如果当前尚未自定义输入实付金额，则默认建议实付 = 分配合计 - 预付抵扣
  const needed = Math.max(0, Number((totalAllocated.value - (formData.prepayUsedAmount || 0)).toFixed(2)))
  if (formData.payAmount === 0 || formData.payAmount < needed) {
    formData.payAmount = needed
  }
}

/** 打开弹窗并接收选中的已通过申请列表 */
const open = async (selectedApplies: PaymentApplyVO[]) => {
  if (!selectedApplies || selectedApplies.length === 0) {
    message.warning('请至少勾选一笔待核销的付款申请')
    return
  }

  // 校验必须属于同一供应商
  const firstSupplierId = selectedApplies[0].supplierId
  const diffSupplier = selectedApplies.find((a) => a.supplierId !== firstSupplierId)
  if (diffSupplier) {
    message.error('所选付款申请不属于同一供应商，无法合并登记！')
    return
  }

  dialogVisible.value = true
  formLoading.value = true
  supplierId.value = firstSupplierId
  supplierName.value = selectedApplies[0].supplierName || ''

  try {
    await fetchPrepayBalance()

    // 复制并按创建时间正序排列
    const sorted = [...selectedApplies].sort((a, b) => {
      const ta = new Date(a.createTime || '').getTime() || 0
      const tb = new Date(b.createTime || '').getTime() || 0
      return ta - tb
    })

    allocateItems.value = sorted.map((apply) => {
      const remaining = Math.max(0, Number((apply.amount - (apply.allocatedAmount || 0)).toFixed(2)))
      return {
        applyId: apply.id,
        applyNo: apply.applyNo,
        applyType: apply.applyType,
        amount: apply.amount,
        allocatedAmount: apply.allocatedAmount || 0,
        remaining,
        allocateAmount: remaining, // 默认预填剩余额
        createTime: apply.createTime
      }
    })

    // 初始化默认实付金额
    formData.accountId = 1
    formData.prepayUsedAmount = 0
    formData.payAmount = totalAllocated.value
    formData.remark = ''
  } finally {
    formLoading.value = false
  }
}

/** 提交登记 */
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (validationWarningText.value) {
    message.error(validationWarningText.value)
    return
  }

  submitLoading.value = true
  try {
    const items: PaymentRegisterItemReqVO[] = allocateItems.value
      .filter((item) => (item.allocateAmount || 0) > 0)
      .map((item) => ({
        applyId: item.applyId,
        allocateAmount: item.allocateAmount
      }))

    const payload: PaymentRegisterReqVO = {
      supplierId: supplierId.value,
      accountId: formData.accountId!,
      payAmount: formData.payAmount,
      prepayUsedAmount: formData.prepayUsedAmount || 0,
      remark: formData.remark,
      items
    }

    await PaymentApi.registerPayment(payload)
    message.success('付款登记成功')
    dialogVisible.value = false
    emit('success')
  } catch (err: any) {
    const errCode = err?.response?.data?.code
    const rawMsg = err?.response?.data?.msg || err?.message || '登记失败'

    // 023 守卫错误映射
    let friendlyTitle = '付款登记失败'
    let friendlyMsg = rawMsg
    if (errCode === 1030023003) {
      friendlyTitle = '分配金额超限'
      friendlyMsg = '核销分配金额超过付款申请剩余可核销额，请核对各申请项！'
    } else if (errCode === 1030023004) {
      friendlyTitle = '账户余额不足'
      friendlyMsg = '资金账户可用余额不足，请核查账户余额或更换付款账户！'
    } else if (errCode === 1030023005) {
      friendlyTitle = '预付余额不足'
      friendlyMsg = '抵扣金额超过供应商实时预付余额，请减少抵扣额或刷新余额！'
    } else if (errCode === 1030023006) {
      friendlyTitle = '资金账户不可用'
      friendlyMsg = '所选资金账户不存在或已被停用，请选择有效账户！'
    } else if (errCode === 1030023007) {
      friendlyTitle = '付款金额不合法'
      friendlyMsg = '付款金额不合法（分配合计超出实付与预付抵扣之和）！'
    }

    await ElMessageBox.alert(friendlyMsg, friendlyTitle, {
      type: 'error',
      confirmButtonText: '我知道了'
    })
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
