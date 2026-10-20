<template>
  <el-dialog
    title="账户间转账"
    v-model="visible"
    width="540px"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <el-alert type="warning" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">转账凭证登记规则</span>
      </template>
      <template #default>
        <div class="text-12px leading-relaxed mt-2px">
          账户间转账单（ZZ）登记即生效且不可改删；系统将分别在转出账户记
          <strong>CREDIT（转出）</strong>并扣减可用余额，在转入账户记
          <strong>DEBIT（转入）</strong>并增加可用余额。
        </div>
      </template>
    </el-alert>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="loading"
    >
      <el-form-item label="转出账户" prop="fromAccountId">
        <el-select
          v-model="formData.fromAccountId"
          placeholder="请选择转出账户"
          class="w-full"
          @change="handleFromAccountChange"
        >
          <el-option
            v-for="acc in accountList"
            :key="acc.id"
            :label="`${acc.accountName}（${acc.currency} 余额: ¥${formatAmount(acc.currentBalance)}）`"
            :value="acc.id"
          />
        </el-select>
        <div v-if="selectedFromAccount" class="text-12px text-gray-500 mt-2px">
          当前可用余额：
          <span class="font-mono font-bold text-gray-900">
            {{ formatAmount(selectedFromAccount.currentBalance) }}
          </span>
          {{ selectedFromAccount.currency }}
        </div>
      </el-form-item>

      <el-form-item label="转入账户" prop="toAccountId">
        <el-select
          v-model="formData.toAccountId"
          placeholder="请选择转入账户"
          class="w-full"
          @change="handleToAccountChange"
        >
          <el-option
            v-for="acc in accountList"
            :key="acc.id"
            :label="`${acc.accountName}（${acc.currency} 余额: ¥${formatAmount(acc.currentBalance)}）`"
            :value="acc.id"
            :disabled="acc.id === formData.fromAccountId"
          />
        </el-select>
        <div
          v-if="
            selectedFromAccount &&
            selectedToAccount &&
            selectedFromAccount.currency !== selectedToAccount.currency
          "
          class="text-12px text-red-500 mt-2px"
        >
          ⚠️ 提示：转入账户币种（{{ selectedToAccount.currency }}）与转出账户（{{
            selectedFromAccount.currency
          }}）不一致，暂不支持跨币种转账（020_106）
        </div>
      </el-form-item>

      <el-form-item label="转账金额" prop="amount">
        <el-input-number
          v-model="formData.amount"
          :precision="2"
          :step="100"
          :min="0.01"
          :max="selectedFromAccount?.currentBalance || 999999999"
          class="!w-full"
        />
        <div class="text-12px text-gray-400 mt-2px">
          币种：{{ selectedFromAccount?.currency || 'RMB' }}，单次转账不可超过转出账户可用余额
        </div>
      </el-form-item>

      <el-form-item label="转账日期" prop="transferDate">
        <el-date-picker
          v-model="formData.transferDate"
          type="date"
          placeholder="请选择转账业务日期"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          class="!w-full"
        />
      </el-form-item>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入转账原因或备注（选填）"
          maxlength="255"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          v-hasPermi="['lunaerp:finance:account:transfer']"
          @click="handleSubmit"
        >
          确认转账
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getSimpleAccountList, createTransfer } from '@/api/lunaerp-elmui/finance/account'
import type {
  FinanceAccountSimpleRespVO,
  TransferCreateReqVO
} from '@/api/lunaerp-elmui/finance/account'
import { formatAmount, validateTransfer, parseAccountErrorMessage } from '../model/account'

defineOptions({ name: 'TransferDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const accountList = ref<FinanceAccountSimpleRespVO[]>([])

const getToday = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const formData = reactive<TransferCreateReqVO>({
  fromAccountId: undefined as unknown as number,
  toAccountId: undefined as unknown as number,
  currency: 'RMB',
  amount: 0.01,
  transferDate: getToday(),
  remark: ''
})

const selectedFromAccount = computed(() =>
  accountList.value.find((acc) => acc.id === formData.fromAccountId)
)

const selectedToAccount = computed(() =>
  accountList.value.find((acc) => acc.id === formData.toAccountId)
)

const formRules: FormRules = {
  fromAccountId: [{ required: true, message: '请选择转出资金账户', trigger: 'change' }],
  toAccountId: [{ required: true, message: '请选择转入资金账户', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入转账金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || Number(value) <= 0) {
          callback(new Error('转账金额必须大于 0'))
        } else if (
          selectedFromAccount.value &&
          Number(value) > selectedFromAccount.value.currentBalance
        ) {
          callback(new Error('转账金额超出转出账户可用余额'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  transferDate: [{ required: true, message: '请选择转账业务日期', trigger: 'change' }]
}

const handleFromAccountChange = () => {
  if (selectedFromAccount.value) {
    formData.currency = selectedFromAccount.value.currency
    if (formData.toAccountId === formData.fromAccountId) {
      formData.toAccountId = undefined as unknown as number
    }
  }
}

const handleToAccountChange = () => {
  if (
    selectedFromAccount.value &&
    selectedToAccount.value &&
    selectedFromAccount.value.currency !== selectedToAccount.value.currency
  ) {
    ElMessage.warning('转入账户与转出账户币种不一致，暂不支持跨币种转账')
  }
}

const open = async (defaultFromAccountId?: number) => {
  visible.value = true
  formData.fromAccountId = defaultFromAccountId || (undefined as unknown as number)
  formData.toAccountId = undefined as unknown as number
  formData.amount = 0.01
  formData.transferDate = getToday()
  formData.remark = ''

  loading.value = true
  try {
    accountList.value = await getSimpleAccountList()
    if (defaultFromAccountId && selectedFromAccount.value) {
      formData.currency = selectedFromAccount.value.currency
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '获取资金账户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  const check = validateTransfer(
    selectedFromAccount.value,
    selectedToAccount.value,
    formData.amount
  )
  if (!check.valid) {
    ElMessage.error(check.message || '转账校验未通过')
    return
  }

  submitLoading.value = true
  try {
    await createTransfer({
      fromAccountId: formData.fromAccountId,
      toAccountId: formData.toAccountId,
      currency: formData.currency,
      amount: formData.amount,
      transferDate: formData.transferDate,
      remark: formData.remark
    })
    ElMessage.success('账户间转账登记成功')
    visible.value = false
    emit('success')
  } catch (error: any) {
    const msg = parseAccountErrorMessage(error?.code, error?.message || '转账登记失败')
    ElMessage.error(msg)
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
