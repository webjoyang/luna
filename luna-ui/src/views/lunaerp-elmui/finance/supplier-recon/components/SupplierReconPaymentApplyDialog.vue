<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`生成付款申请 - 对账单【${currentRow?.reconNo || ''}】`"
    width="540px"
    append-to-body
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="对账单号">
        <span class="font-mono font-medium">{{ currentRow?.reconNo || '—' }}</span>
      </el-form-item>
      <el-form-item label="供应商">
        <span class="font-medium">{{ currentRow?.supplierName || '—' }}</span>
      </el-form-item>
      <el-form-item label="确认总金额">
        <span class="font-mono font-bold text-gray-800">
          ¥ {{ formatMoney(currentRow?.totalAmount) }}
        </span>
      </el-form-item>
      <el-form-item label="已申请金额">
        <span class="font-mono text-gray-600">
          ¥ {{ formatMoney(currentRow?.appliedAmount) }}
        </span>
      </el-form-item>
      <el-form-item label="可申请上限">
        <span class="font-mono font-bold text-emerald-700">
          ¥ {{ formatMoney(remainingLimit) }}
        </span>
      </el-form-item>

      <el-alert
        v-if="remainingLimit <= 0"
        type="warning"
        :closable="false"
        show-icon
        class="mb-16px"
        title="该对账单已全额申请，累计申请额已达到对账确认金额，不可重复生成付款申请。"
      />

      <el-form-item label="本次申请额" prop="amount">
        <el-input-number
          v-model="formData.amount"
          :min="0.01"
          :max="remainingLimit > 0 ? remainingLimit : 0.01"
          :precision="2"
          :step="100"
          controls-position="right"
          class="!w-full"
          :disabled="remainingLimit <= 0"
          placeholder="请输入本次申请付款金额"
        />
        <div class="text-12px text-gray-400 mt-4px">
          默认全额申请，可改小；累计申请金额不可超出对账确认金额。
        </div>
      </el-form-item>

      <el-form-item label="申请备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="可选输入本次付款申请备注说明"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="remainingLimit <= 0"
          @click="handleSubmit"
          v-hasPermi="['lunaerp:finance:supplierrecon:generate']"
        >
          确认生成
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { SupplierReconRespVO } from '@/api/lunaerp-elmui/finance/supplier-recon'
import { createPaymentApplyForRecon } from '@/api/lunaerp-elmui/finance/supplier-recon'
import { formatMoney } from '../model/supplier-recon'

defineOptions({ name: 'SupplierReconPaymentApplyDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentRow = ref<SupplierReconRespVO | null>(null)

const formData = reactive({
  amount: 0,
  remark: ''
})

const remainingLimit = computed(() => {
  if (!currentRow.value) return 0
  const total = Number(currentRow.value.totalAmount || 0)
  const applied = Number(currentRow.value.appliedAmount || 0)
  const remaining = total - applied
  return Number(Math.max(0, remaining).toFixed(2))
})

const formRules = reactive<FormRules>({
  amount: [
    { required: true, message: '请输入申请付款金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || Number(value) <= 0) {
          callback(new Error('申请付款金额必须大于 0'))
        } else if (Number(value) > remainingLimit.value) {
          callback(new Error(`申请金额不能超出剩余上限 ¥${formatMoney(remainingLimit.value)}`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const open = (row: SupplierReconRespVO) => {
  currentRow.value = row
  const limit = Math.max(0, Number((row.totalAmount || 0) - (row.appliedAmount || 0)))
  formData.amount = Number(limit.toFixed(2))
  formData.remark = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value || !currentRow.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    await createPaymentApplyForRecon({
      reconId: currentRow.value.id,
      amount: formData.amount,
      remark: formData.remark
    })
    ElMessage.success('付款申请已生成')
    dialogVisible.value = false
    emit('success')
  } catch (err: any) {
    const errorMsg = err?.msg || err?.message || '生成付款申请失败'
    ElMessage.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
