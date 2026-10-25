<template>
  <el-dialog v-model="dialogVisible" title="新增汇率" width="540px" append-to-body destroy-on-close>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      label-position="right"
    >
      <el-form-item label="外币币种" prop="currency">
        <el-select
          v-model="formData.currency"
          placeholder="请选择或输入外币币种（如 USD/EUR/HKD）"
          filterable
          allow-create
          default-first-option
          clearable
          class="!w-full"
          @change="handleCurrencyChange"
        >
          <el-option
            v-for="item in commonCurrencies"
            :key="item.value"
            :label="`${item.value} - ${item.label}`"
            :value="item.value"
            :disabled="item.disabled"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="生效日期" prop="effectiveDate">
        <el-date-picker
          v-model="formData.effectiveDate"
          type="date"
          placeholder="请选择生效日期"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          class="!w-full"
        />
        <div class="text-12px text-gray-400 mt-2px">
          同币种同生效日全局唯一，业务发生时将撮合生效日 ≤ 业务日的最新一条启用汇率。
        </div>
      </el-form-item>

      <el-form-item label="汇率数值" prop="rate">
        <el-input-number
          v-model="formData.rate"
          :precision="8"
          :step="0.0001"
          :min="0.00000001"
          :max="99999999"
          controls-position="right"
          class="!w-full"
          placeholder="请输入汇率（1 外币 = 多少本位币）"
        />
        <div class="text-12px text-gray-500 mt-2px">
          换算关系：1 {{ formData.currency || '外币' }} =
          <span class="font-mono font-bold text-blue-600">
            {{ formatExchangeRate(formData.rate) }}
          </span>
          RMB
        </div>
      </el-form-item>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="可选输入牌价来源或备注说明（如：中国银行即期中行折算价）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-x-8px">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          v-hasPermi="['lunaerp:finance:exchangerate:create']"
          @click="handleSubmit"
        >
          确定新增
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createExchangeRate } from '@/api/lunaerp-elmui/finance/exchange-rate'
import {
  formatExchangeRate,
  validateCurrencyInput,
  validateExchangeRateInput,
  parseExchangeRateErrorMessage
} from '../model/exchange-rate'

defineOptions({ name: 'ExchangeRateFormDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const commonCurrencies = [
  { value: 'USD', label: '美元 (USD)' },
  { value: 'EUR', label: '欧元 (EUR)' },
  { value: 'HKD', label: '港币 (HKD)' },
  { value: 'JPY', label: '日元 (JPY)' },
  { value: 'GBP', label: '英镑 (GBP)' },
  { value: 'AUD', label: '澳大利亚元 (AUD)' },
  { value: 'CAD', label: '加拿大元 (CAD)' },
  { value: 'RMB', label: '人民币 (本位币无需配置)', disabled: true },
  { value: 'CNY', label: '人民币 (本位币无需配置)', disabled: true }
]

const formData = reactive<{
  currency: string
  effectiveDate: string
  rate: number | undefined
  remark?: string
}>({
  currency: '',
  effectiveDate: '',
  rate: undefined,
  remark: ''
})

const validateCurrencyRule = (_rule: any, value: string, callback: any) => {
  const result = validateCurrencyInput(value)
  if (!result.valid) {
    return callback(new Error(result.message))
  }
  callback()
}

const validateRateRule = (_rule: any, value: number, callback: any) => {
  const result = validateExchangeRateInput(value)
  if (!result.valid) {
    return callback(new Error(result.message))
  }
  callback()
}

const formRules: FormRules = {
  currency: [{ required: true, validator: validateCurrencyRule, trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  rate: [{ required: true, validator: validateRateRule, trigger: 'blur' }]
}

const handleCurrencyChange = (val: string) => {
  if (val) {
    formData.currency = val.trim().toUpperCase()
  }
}

/**
 * 获取本地 YYYY-MM-DD 格式日期（避免 ISOString UTC 时区偏差）
 */
const getLocalDateString = (): string => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const open = () => {
  dialogVisible.value = true
  formData.currency = ''
  formData.effectiveDate = getLocalDateString()
  formData.rate = undefined
  formData.remark = ''
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    await createExchangeRate({
      currency: formData.currency.trim().toUpperCase(),
      effectiveDate: formData.effectiveDate,
      rate: formData.rate!,
      remark: formData.remark
    })
    ElMessage.success('汇率新增成功')
    dialogVisible.value = false
    emit('success')
  } catch (error: any) {
    const code = error?.code || error?.response?.data?.code
    if (code) {
      ElMessage.error(parseExchangeRateErrorMessage(code, error?.message || '操作失败'))
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
