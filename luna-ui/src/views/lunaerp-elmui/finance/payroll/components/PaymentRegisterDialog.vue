<template>
  <el-dialog
    v-model="visible"
    title="工资发放登记"
    width="540px"
    destroy-on-close
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert type="warning" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">出纳发放过账与资金扣减</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          登记发放后将同步在所选资金账户扣除金额并记账（OUT_SALARY 分录），生成独立的 GF
          发放批次流水；<br />
          <strong>批次记录不可篡改或删除</strong>；支持跨多账户分批次发放直至全额发清。
        </div>
      </template>
    </el-alert>

    <!-- 工资单概要概览 -->
    <div class="bg-gray-50 border border-gray-200 rounded p-12px mb-16px text-12px">
      <div class="grid grid-cols-2 gap-y-6px text-gray-700">
        <div
          >工资单号：<span class="font-mono font-bold">{{ payroll?.payrollNo }}</span></div
        >
        <div
          >工资期间：<span class="font-mono font-bold">{{ payroll?.period }}</span></div
        >
        <div
          >实发总计：<span class="font-mono font-bold text-blue-800"
            >¥ {{ formatAmount(payroll?.netAmount) }}</span
          ></div
        >
        <div
          >已发累计：<span class="font-mono font-bold text-green-700"
            >¥ {{ formatAmount(payroll?.paidAmount) }}</span
          ></div
        >
      </div>
      <div class="mt-8px pt-8px border-t border-gray-200 flex items-center justify-between">
        <span class="font-medium text-gray-800">当前未发余额：</span>
        <span class="font-mono font-bold text-15px text-red-600">
          ¥ {{ formatAmount(unpaidBalance) }}
        </span>
      </div>
    </div>

    <!-- 登记表单 -->
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      label-position="right"
    >
      <el-form-item label="付款资金账户" prop="accountId">
        <el-select
          v-model="formData.accountId"
          placeholder="请选择付款资金账户"
          class="!w-1/1"
          filterable
        >
          <el-option
            v-for="acc in activeAccountList"
            :key="acc.id"
            :label="`${acc.accountName} (${acc.currency || 'RMB'})`"
            :value="acc.id"
          >
            <div class="flex items-center justify-between">
              <span>{{ acc.accountName }}</span>
              <span class="text-gray-400 font-mono text-12px">
                余额: ¥{{ formatAmount(acc.currentBalance) }}
              </span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="本次发放金额" prop="amount">
        <el-input-number
          v-model="formData.amount"
          :min="0.01"
          :max="unpaidBalance"
          :precision="2"
          :step="1000"
          placeholder="请输入本次发放金额"
          class="!w-1/1"
        />
        <div class="text-11px text-gray-400 mt-2px">
          缺省填入当前未发余额，可分批次跨账户发放，不可超出未发余额（025_007）
        </div>
      </el-form-item>

      <el-form-item label="发放支付日期" prop="payDate">
        <el-date-picker
          v-model="formData.payDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择支付日期"
          class="!w-1/1"
        />
      </el-form-item>

      <el-form-item label="发放备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="例如：银行代发首批、出纳现金发放等"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex items-center justify-end gap-8px">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          v-hasPermi="['lunaerp:finance:payroll:pay']"
          @click="handleSubmit"
        >
          确认发放并过账
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { registerPayrollPayment } from '@/api/lunaerp-elmui/finance/payroll'
import type { PayrollRespVO } from '@/api/lunaerp-elmui/finance/payroll'
import { getSimpleAccountList } from '@/api/lunaerp-elmui/finance/account'
import type { AccountRespVO } from '@/api/lunaerp-elmui/finance/account'
import { formatAmount, parsePayrollErrorMessage } from '../model/payroll'

defineOptions({ name: 'PaymentRegisterDialog' })

const props = defineProps<{
  modelValue: boolean
  payroll?: PayrollRespVO
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  success: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const activeAccountList = ref<AccountRespVO[]>([])

const unpaidBalance = computed(() => {
  if (!props.payroll) return 0
  const net = Number(props.payroll.netAmount) || 0
  const paid = Number(props.payroll.paidAmount) || 0
  return Math.max(0, Math.round((net - paid) * 100) / 100)
})

const formData = reactive({
  accountId: undefined as number | undefined,
  amount: 0,
  payDate: new Date().toLocaleDateString('en-CA'),
  remark: ''
})

const formRules: FormRules = {
  accountId: [{ required: true, message: '请选择付款资金账户', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入本次发放金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const val = Number(value)
        if (isNaN(val) || val <= 0) {
          callback(new Error('发放金额必须大于0'))
        } else if (val > unpaidBalance.value) {
          callback(new Error('本次发放金额超出剩余未发余额（025_007）'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  payDate: [{ required: true, message: '请选择发放支付日期', trigger: 'change' }]
}

const loadAccounts = async () => {
  try {
    const list = await getSimpleAccountList()
    activeAccountList.value = (list || []).filter((a) => a.status !== 'DISABLE')
    if (activeAccountList.value.length > 0 && !formData.accountId) {
      formData.accountId = activeAccountList.value[0].id
    }
  } catch (e) {
    console.error('加载资金账户失败:', e)
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      loadAccounts()
      formData.amount = unpaidBalance.value
      formData.payDate = new Date().toLocaleDateString('en-CA')
      formData.remark = ''
    }
  }
)

const handleSubmit = async () => {
  if (!formRef.value || !props.payroll?.id) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await registerPayrollPayment({
        payrollId: props.payroll!.id,
        accountId: formData.accountId!,
        amount: formData.amount,
        payDate: formData.payDate,
        remark: formData.remark
      })
      ElMessage.success('工资发放登记成功，已完成资金扣减与过账')
      visible.value = false
      emit('success')
    } catch (err: any) {
      ElMessage.error(parsePayrollErrorMessage(err))
    } finally {
      submitting.value = false
    }
  })
}
</script>
