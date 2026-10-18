<template>
  <el-dialog
    v-model="visible"
    title="驳回客户对账单"
    width="500px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-alert
      type="warning"
      :closable="false"
      class="mb-16px"
      show-icon
      title="驳回说明"
      description="记录客户提出的异议或财务复核驳回原因。驳回后单据状态将变更为已驳回（REJECTED），后续可通过异议回复重置回草稿态重新修改。"
    />
    <el-form :model="formData" :rules="rules" ref="formRef" label-width="90px">
      <el-form-item label="对账单号">
        <span class="font-mono font-bold">{{ statementNo }}</span>
      </el-form-item>
      <el-form-item label="异议原因" prop="rejectReason">
        <el-input
          v-model="formData.rejectReason"
          type="textarea"
          :rows="4"
          placeholder="请输入客户异议或驳回原因（必填）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="danger"
        @click="handleSubmit"
        :loading="submitLoading"
        v-hasPermi="['lunaerp:finance:customerrecon:update']"
      >
        确认驳回
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { rejectCustomerStatement } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'StatementRejectDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitLoading = ref(false)
const statementNo = ref('')
const formRef = ref<FormInstance>()

const formData = reactive({
  id: 0,
  rejectReason: ''
})

const rules: FormRules = {
  rejectReason: [{ required: true, message: '请输入异议驳回原因', trigger: 'blur' }]
}

const open = (id: number, no: string) => {
  formData.id = id
  formData.rejectReason = ''
  statementNo.value = no
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      await rejectCustomerStatement({
        id: formData.id,
        rejectReason: formData.rejectReason.trim()
      })
      ElMessage.success('已驳回对账单')
      visible.value = false
      emit('success')
    } finally {
      submitLoading.value = false
    }
  })
}

defineExpose({ open })
</script>
