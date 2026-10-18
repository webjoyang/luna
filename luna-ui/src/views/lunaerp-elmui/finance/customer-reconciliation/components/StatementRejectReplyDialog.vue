<template>
  <el-dialog
    v-model="visible"
    title="回复对账异议并重置为草稿"
    width="520px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-alert
      type="info"
      :closable="false"
      class="mb-16px"
      show-icon
      title="异议处理说明"
      description="回复客户异议说明后，对账单状态将重置回草稿（DRAFT），财务可重新修改行明细、追加漏记分录或核准后再次发送。"
    />
    <el-form :model="formData" ref="formRef" label-width="90px">
      <el-form-item label="对账单号">
        <span class="font-mono font-bold">{{ statementNo }}</span>
      </el-form-item>
      <el-form-item v-if="rejectReason" label="客户异议">
        <div class="text-red-600 bg-red-50 p-8px rounded border border-red-200 w-full text-12px">
          {{ rejectReason }}
        </div>
      </el-form-item>
      <el-form-item label="回复说明" prop="replyRemark">
        <el-input
          v-model="formData.replyRemark"
          type="textarea"
          :rows="4"
          placeholder="请输入针对客户异议的处理回复说明（如：已重新核对第3笔发货明细，扣减差异后重新发起）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        @click="handleSubmit"
        :loading="submitLoading"
        v-hasPermi="['lunaerp:finance:customerrecon:update']"
      >
        保存并转为草稿
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { rejectReplyCustomerStatement } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'StatementRejectReplyDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitLoading = ref(false)
const statementNo = ref('')
const rejectReason = ref('')

const formData = reactive({
  id: 0,
  replyRemark: ''
})

const open = (id: number, no: string, reason?: string, previousReply?: string) => {
  formData.id = id
  formData.replyRemark = previousReply || ''
  statementNo.value = no
  rejectReason.value = reason || ''
  visible.value = true
}

const handleSubmit = async () => {
  submitLoading.value = true
  try {
    await rejectReplyCustomerStatement({
      id: formData.id,
      replyRemark: formData.replyRemark ? formData.replyRemark.trim() : undefined
    })
    ElMessage.success('已回复异议并重置为草稿状态')
    visible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
