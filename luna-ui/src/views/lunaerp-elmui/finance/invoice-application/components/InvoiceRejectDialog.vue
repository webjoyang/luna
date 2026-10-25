<template>
  <el-dialog v-model="visible" title="驳回发票申请" width="500px" destroy-on-close append-to-body>
    <el-form ref="formRef" :model="formData" label-width="90px" v-loading="submitting">
      <el-form-item label="申请单号">
        <span class="font-bold">{{ appData?.invoiceNo }}</span>
      </el-form-item>

      <el-form-item label="开票客户">
        <span>{{ appData?.customerName }}</span>
      </el-form-item>

      <el-form-item label="驳回原因" prop="rejectReason">
        <el-input
          v-model="formData.rejectReason"
          type="textarea"
          :rows="4"
          placeholder="请输入驳回原因（可空，驳回后释放该单发货行开票占用，申请单回到 REJECTED 状态可改重提）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="danger"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="['lunaerp:finance:invoice:issue']"
        >
          确认驳回
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  rejectInvoiceApplication,
  type InvoiceApplicationRespVO
} from '@/api/lunaerp-elmui/finance/invoice'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const appData = ref<InvoiceApplicationRespVO>()

const formData = reactive<{
  rejectReason?: string
}>({
  rejectReason: ''
})

const open = (row: InvoiceApplicationRespVO) => {
  appData.value = row
  formData.rejectReason = ''
  visible.value = true
}

const handleSubmit = async () => {
  if (!appData.value) return

  submitting.value = true
  try {
    await rejectInvoiceApplication({
      id: appData.value.id,
      rejectReason: formData.rejectReason?.trim() || undefined
    })
    ElMessage.success('驳回成功，已释放开票占用')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
