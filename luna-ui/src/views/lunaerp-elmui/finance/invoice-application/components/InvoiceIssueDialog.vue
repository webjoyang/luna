<template>
  <el-dialog
    v-model="visible"
    title="发票登记回填（开票确认）"
    width="540px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      title="登记发票号为财务最终复核节点。回填正式发票号码后，状态变更为「已开票」，不可再次修改。"
      type="warning"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="loading"
    >
      <el-form-item label="发票申请单号">
        <span class="font-bold text-gray-700">{{ invoiceNo }}</span>
      </el-form-item>

      <el-form-item label="开票金额合计">
        <span class="font-bold text-red-600">￥{{ formatAmount(totalAmount) }}</span>
      </el-form-item>

      <el-form-item label="官方发票号码" prop="officialInvoiceNo">
        <el-input
          v-model="formData.officialInvoiceNo"
          placeholder="请输入全国统一20位发票号码"
          maxlength="20"
          show-word-limit
          clearable
        />
        <div class="text-xs text-gray-400 mt-1">数电发票为20位统一发票号码</div>
      </el-form-item>

      <el-form-item label="开票日期" prop="invoiceDate">
        <el-date-picker
          v-model="formData.invoiceDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择开票日期"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="发票附件URL" prop="attachmentUrl">
        <el-input
          v-model="formData.attachmentUrl"
          placeholder="请输入发票电子文件/扫描件 URL（选填）"
          clearable
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
        确认登记
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import * as InvoiceApi from '@/api/lunaerp-elmui/finance/invoice'

defineOptions({ name: 'InvoiceIssueDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const invoiceId = ref<number>()
const invoiceNo = ref('')
const totalAmount = ref<number>(0)

const formData = reactive({
  officialInvoiceNo: '',
  invoiceDate: new Date().toISOString().substring(0, 10),
  attachmentUrl: ''
})

const validateInvoiceNo = (_rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error('请输入20位发票号码'))
  }
  const trimmed = value.trim()
  if (trimmed.length !== 20) {
    return callback(new Error('官方发票号码必须为20位'))
  }
  callback()
}

const formRules = {
  officialInvoiceNo: [
    { required: true, message: '请输入官方发票号码', trigger: 'blur' },
    { validator: validateInvoiceNo, trigger: 'blur' }
  ],
  invoiceDate: [{ required: true, message: '请选择开票日期', trigger: 'change' }]
}

function formatAmount(val?: number | string | null): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const open = (row: InvoiceApi.InvoiceVO) => {
  visible.value = true
  invoiceId.value = row.id
  invoiceNo.value = row.invoiceNo || ''
  totalAmount.value = row.totalAmountInclTax || 0
  formData.officialInvoiceNo = ''
  formData.invoiceDate = new Date().toISOString().substring(0, 10)
  formData.attachmentUrl = ''
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (!invoiceId.value) return

  submitLoading.value = true
  try {
    await InvoiceApi.issueInvoice({
      id: invoiceId.value,
      officialInvoiceNo: formData.officialInvoiceNo.trim(),
      invoiceDate: formData.invoiceDate,
      attachmentUrl: formData.attachmentUrl?.trim() || undefined
    })
    ElMessage.success('发票登记成功')
    visible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
