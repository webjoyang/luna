<template>
  <Dialog v-model="visible" title="异议回复处理" width="560px">
    <el-alert
      type="warning"
      :closable="false"
      class="mb-16px"
      show-icon
    >
      <template #title>
        回复处理后，对账单状态将重新恢复为【草稿】，您可以调整或追加分录明细后再次发送。
      </template>
    </el-alert>

    <el-form
      ref="formRef"
      :model="formData"
      label-width="110px"
      v-loading="loading"
    >
      <el-form-item label="对账单号">
        <el-input :model-value="statement?.statementNo" disabled />
      </el-form-item>
      <el-form-item label="客户名称">
        <el-input :model-value="statement?.customerName" disabled />
      </el-form-item>
      <el-form-item label="客户驳回原因">
        <el-input
          :model-value="statement?.rejectReason || '—'"
          type="textarea"
          :rows="3"
          disabled
        />
      </el-form-item>
      <el-form-item label="财务回复说明" prop="replyRemark">
        <el-input
          v-model="formData.replyRemark"
          type="textarea"
          :rows="4"
          placeholder="请输入针对客户异议的处理说明或核实情况（可选）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确认并恢复草稿
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  rejectReplyCustomerStatement,
  type CustomerStatementVO,
  type CustomerStatementRejectReplyReqVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'CustomerStatementRejectReplyDialog' })

const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const statement = ref<CustomerStatementVO | null>(null)

const formData = reactive<CustomerStatementRejectReplyReqVO>({
  id: 0,
  replyRemark: ''
})

const open = (row: CustomerStatementVO) => {
  statement.value = row
  formData.id = row.id
  formData.replyRemark = ''
  visible.value = true
}

const handleSubmit = async () => {
  try {
    loading.value = true
    await rejectReplyCustomerStatement({
      id: formData.id,
      replyRemark: formData.replyRemark
    })
    ElMessage.success('已回复处理并恢复草稿状态')
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
