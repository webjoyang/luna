<template>
  <el-dialog
    title="登记其他收支"
    v-model="visible"
    width="540px"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <el-alert type="info" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">其他收支凭证登记规则</span>
      </template>
      <template #default>
        <div class="text-12px leading-relaxed mt-2px">
          其他收支单（QT）用于维护非主营业务收支（如结息、废料款、手续费等），登记即生效并记入资金流水。
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
      <el-form-item label="资金账户" prop="accountId">
        <el-select
          v-model="formData.accountId"
          placeholder="请选择资金账户"
          class="w-full"
          @change="handleAccountChange"
        >
          <el-option
            v-for="acc in accountList"
            :key="acc.id"
            :label="`${acc.accountName}（${acc.currency} 余额: ¥${formatAmount(acc.currentBalance)}）`"
            :value="acc.id"
          />
        </el-select>
        <div v-if="selectedAccount" class="text-12px text-gray-500 mt-2px">
          当前可用余额：
          <span class="font-mono font-bold text-gray-900">
            {{ formatAmount(selectedAccount.currentBalance) }}
          </span>
          {{ selectedAccount.currency }}
        </div>
      </el-form-item>

      <el-form-item label="收支方向" prop="direction">
        <el-radio-group v-model="formData.direction" @change="handleDirectionChange">
          <el-radio label="DEBIT">
            <span class="text-emerald-700 font-medium">流入 (DEBIT)</span>
          </el-radio>
          <el-radio label="CREDIT">
            <span class="text-amber-700 font-medium">流出 (CREDIT)</span>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="收支类别" prop="category">
        <el-select v-model="formData.category" placeholder="请选择收支类别" class="w-full">
          <el-option
            v-for="cat in availableCategories"
            :key="cat.value"
            :label="cat.label"
            :value="cat.value"
          />
        </el-select>
        <div class="text-12px text-gray-400 mt-2px">
          类别选项已随收支方向（{{ formData.direction === 'DEBIT' ? '流入' : '流出' }}）自动联动过滤
        </div>
      </el-form-item>

      <el-form-item label="收支金额" prop="originalAmount">
        <el-input-number
          v-model="formData.originalAmount"
          :precision="2"
          :step="100"
          :min="0.01"
          :max="
            formData.direction === 'CREDIT'
              ? selectedAccount?.currentBalance || 999999999
              : 999999999
          "
          class="!w-full"
        />
        <div class="text-12px text-gray-400 mt-2px">
          币种：{{ selectedAccount?.currency || 'RMB' }}
          <span v-if="formData.direction === 'CREDIT' && selectedAccount">
            ，流出金额不可超过可用余额
          </span>
        </div>
      </el-form-item>

      <el-form-item label="业务日期" prop="txnDate">
        <el-date-picker
          v-model="formData.txnDate"
          type="date"
          placeholder="请选择收支业务日期"
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
          placeholder="请输入收支原因说明（选填）"
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
          v-hasPermi="['lunaerp:finance:account:other-txn']"
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
import type { FormInstance, FormRules } from 'element-plus'
import { getSimpleAccountList, createOtherTxn } from '@/api/lunaerp-elmui/finance/account'
import type {
  FinanceAccountSimpleRespVO,
  OtherTxnCreateReqVO
} from '@/api/lunaerp-elmui/finance/account'
import {
  formatAmount,
  getOtherTxnCategoriesByDirection,
  parseAccountErrorMessage
} from '../model/account'

defineOptions({ name: 'OtherTxnDialog' })

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

const formData = reactive<OtherTxnCreateReqVO>({
  accountId: undefined as unknown as number,
  direction: 'DEBIT',
  category: 'INTEREST_IN',
  originalAmount: 0.01,
  txnDate: getToday(),
  remark: ''
})

const selectedAccount = computed(() =>
  accountList.value.find((acc) => acc.id === formData.accountId)
)

const availableCategories = computed(() => getOtherTxnCategoriesByDirection(formData.direction))

const formRules: FormRules = {
  accountId: [{ required: true, message: '请选择资金账户', trigger: 'change' }],
  direction: [{ required: true, message: '请选择收支方向', trigger: 'change' }],
  category: [{ required: true, message: '请选择收支类别', trigger: 'change' }],
  originalAmount: [
    { required: true, message: '请输入收支金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || Number(value) <= 0) {
          callback(new Error('收支金额必须大于 0'))
        } else if (
          formData.direction === 'CREDIT' &&
          selectedAccount.value &&
          Number(value) > selectedAccount.value.currentBalance
        ) {
          callback(new Error('流出金额超出资金账户可用余额'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  txnDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }]
}

const handleDirectionChange = () => {
  const list = availableCategories.value
  if (!list.some((item) => item.value === formData.category)) {
    formData.category = list[0]?.value || ''
  }
}

const handleAccountChange = () => {
  // 账户变更后触发表单金额重新校验
  if (formRef.value) {
    formRef.value.validateField('originalAmount')
  }
}

const open = async (defaultAccountId?: number) => {
  visible.value = true
  formData.accountId = defaultAccountId || (undefined as unknown as number)
  formData.direction = 'DEBIT'
  formData.category = 'INTEREST_IN'
  formData.originalAmount = 0.01
  formData.txnDate = getToday()
  formData.remark = ''

  loading.value = true
  try {
    accountList.value = await getSimpleAccountList()
  } catch (error: any) {
    ElMessage.error(error?.message || '获取资金账户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    await createOtherTxn({
      accountId: formData.accountId,
      direction: formData.direction,
      category: formData.category,
      originalAmount: formData.originalAmount,
      txnDate: formData.txnDate,
      remark: formData.remark
    })
    ElMessage.success('其他收支单登记成功')
    visible.value = false
    emit('success')
  } catch (error: any) {
    const msg = parseAccountErrorMessage(error?.code, error?.message || '登记失败')
    ElMessage.error(msg)
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
