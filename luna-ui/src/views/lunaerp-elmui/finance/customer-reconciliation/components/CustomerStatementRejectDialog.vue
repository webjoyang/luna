<template>
  <Dialog v-model="visible" title="驳回对账单" width="520px">
    <el-alert
      type="warning"
      :closable="false"
      class="mb-16px"
      show-icon
    >
      <template #title>
        驳回后单据状态将变更为【已驳回】，请填写客户异议原因或驳回理由。
      </template>
    </el-alert>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="loading"
    >
      <el-form-item label="对账单号">
        <el-input :model-value="statement?.statementNo" disabled />
      </el-form-item>
      <el-form-item label="客户名称">
        <el-input :model-value="statement?.customerName" disabled />
      </el-form-item>
      <el-form-item label="异议原因" prop="rejectReason">
        <el-input
          v-model="formData.rejectReason"
          type="textarea"
          :rows="4"
          placeholder="请输入详细异议原因或驳回理由（必填）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit">
        确认驳回
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  rejectCustomerStatement,
  type CustomerStatementVO,
  type CustomerStatementRejectReqVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'CustomerStatementRejectDialog' })

const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const statement = ref<CustomerStatementVO | null>(null)

const formData = reactive<CustomerStatementRejectReqVO>({
  id: 0,
  rejectReason: ''
})

const formRules: FormRules = {
  rejectReason: [
    { required: true, message: '请输入驳回异议原因', trigger: 'blur' },
    { max: 500, message: '原因长度不能超过 500 个字符', trigger: 'blur' }
  ]
}

const open = (row: CustomerStatementVO) => {
  statement.value = row
  formData.id = row.id
  formData.rejectReason = ''
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  try {
    loading.value = true
    await rejectCustomerStatement({
      id: formData.id,
      rejectReason: formData.rejectReason
    })
    ElMessage.success('驳回对账单成功')
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
