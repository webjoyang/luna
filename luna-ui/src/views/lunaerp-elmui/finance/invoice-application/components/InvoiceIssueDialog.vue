<template>
  <el-dialog v-model="visible" title="发票登记回填" width="560px" destroy-on-close append-to-body>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="submitting"
    >
      <el-form-item label="申请单号">
        <span class="font-bold">{{ appData?.invoiceNo }}</span>
      </el-form-item>

      <el-form-item label="客户名称">
        <span>{{ appData?.customerName }}</span>
      </el-form-item>

      <el-form-item label="价税合计">
        <span class="font-bold text-danger"
          >{{ appData?.currency }} {{ formatMoney(appData?.totalAmountInclTax) }}</span
        >
      </el-form-item>

      <el-form-item label="官方发票号" prop="officialInvoiceNo">
        <el-input
          v-model="formData.officialInvoiceNo"
          maxlength="20"
          show-word-limit
          placeholder="请输入 20 位数电发票号码"
          clearable
        />
        <div class="text-12px text-gray-400 mt-1">数电发票号码必填且为严格 20 位字符</div>
      </el-form-item>

      <el-form-item label="开票日期" prop="invoiceDate">
        <el-date-picker
          v-model="formData.invoiceDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择实际开票日期"
          class="!w-full"
        />
      </el-form-item>

      <el-form-item label="发票代码" prop="invoiceCode">
        <el-input v-model="formData.invoiceCode" placeholder="纸票可选，数电票为空" clearable />
      </el-form-item>

      <el-form-item label="发票附件URL" prop="attachmentUrl">
        <el-input
          v-model="formData.attachmentUrl"
          placeholder="请输入电子发票文件直链（如 PDF/OFD）"
          clearable
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="['lunaerp:finance:invoice:issue']"
        >
          确认登记
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  issueInvoiceApplication,
  type InvoiceApplicationRespVO
} from '@/api/lunaerp-elmui/finance/invoice'
import { formatMoney, validateOfficialInvoiceNo } from '../model/invoice'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const appData = ref<InvoiceApplicationRespVO>()

const formData = reactive<{
  officialInvoiceNo: string
  invoiceDate: string
  invoiceCode?: string
  attachmentUrl?: string
}>({
  officialInvoiceNo: '',
  invoiceDate: '',
  invoiceCode: undefined,
  attachmentUrl: undefined
})

const validateOfficialNoRule = (_rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error('官方发票号码不能为空'))
  }
  if (!validateOfficialInvoiceNo(value)) {
    return callback(new Error('官方发票号码必须为严格 20 位字母或数字'))
  }
  callback()
}

const formRules = reactive<FormRules>({
  officialInvoiceNo: [{ required: true, validator: validateOfficialNoRule, trigger: 'blur' }],
  invoiceDate: [{ required: true, message: '请选择开票日期', trigger: 'change' }]
})

const open = (row: InvoiceApplicationRespVO) => {
  appData.value = row
  formData.officialInvoiceNo = row.officialInvoiceNo || ''
  formData.invoiceDate = row.invoiceDate || new Date().toLocaleDateString('en-CA')
  formData.invoiceCode = row.invoiceCode || ''
  formData.attachmentUrl = row.attachmentUrl || ''
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value || !appData.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    await issueInvoiceApplication({
      id: appData.value.id,
      officialInvoiceNo: formData.officialInvoiceNo.trim(),
      invoiceDate: formData.invoiceDate,
      invoiceCode: formData.invoiceCode?.trim() || undefined,
      attachmentUrl: formData.attachmentUrl?.trim() || undefined
    })
    ElMessage.success('登记开票成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
