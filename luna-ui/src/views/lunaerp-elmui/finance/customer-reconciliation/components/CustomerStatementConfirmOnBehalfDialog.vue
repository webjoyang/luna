<template>
  <Dialog v-model="visible" title="代客确认对账单" width="560px">
    <el-alert
      type="info"
      :closable="false"
      class="mb-16px"
      show-icon
    >
      <template #title>
        请核对客户线下沟通凭证（如微信/邮件/纸质盖章件），确认后单据将变更为【已确认】终态快照。
      </template>
    </el-alert>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="loading"
    >
      <el-form-item label="对账单号">
        <el-input :model-value="statement?.statementNo" disabled />
      </el-form-item>
      <el-form-item label="客户名称">
        <el-input :model-value="statement?.customerName" disabled />
      </el-form-item>
      <el-form-item label="期末应收余额">
        <el-input
          :model-value="statement?.closingBalance !== undefined ? `¥ ${statement.closingBalance}` : '—'"
          disabled
        />
      </el-form-item>
      <el-form-item label="线下确认方式" prop="confirmMethod">
        <el-select
          v-model="formData.confirmMethod"
          placeholder="请选择或输入线下确认方式"
          filterable
          allow-create
          default-first-option
          class="w-full"
        >
          <el-option label="微信确认截图（已归档）" value="微信确认截图（已归档）" />
          <el-option label="客户邮件确认" value="客户邮件确认" />
          <el-option label="线下盖章纸质件" value="线下盖章纸质件" />
          <el-option label="电话确认录音备查" value="电话确认录音备查" />
          <el-option label="其他凭据" value="其他凭据" />
        </el-select>
      </el-form-item>
      <el-form-item label="确认备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入代客确认的详细说明或联系人凭证备注"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确认提交
      </el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  confirmCustomerStatementOnBehalf,
  type CustomerStatementVO,
  type CustomerStatementConfirmOnBehalfReqVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'CustomerStatementConfirmOnBehalfDialog' })

const emit = defineEmits(['success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const statement = ref<CustomerStatementVO | null>(null)

const formData = reactive<CustomerStatementConfirmOnBehalfReqVO>({
  id: 0,
  confirmMethod: '',
  remark: ''
})

const formRules: FormRules = {
  confirmMethod: [
    { required: true, message: '请选择或输入线下确认方式', trigger: 'change' }
  ]
}

const open = (row: CustomerStatementVO) => {
  statement.value = row
  formData.id = row.id
  formData.confirmMethod = ''
  formData.remark = ''
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  try {
    loading.value = true
    await confirmCustomerStatementOnBehalf({
      id: formData.id,
      confirmMethod: formData.confirmMethod,
      remark: formData.remark
    })
    ElMessage.success('代客确认成功')
    visible.value = false
    emit('success')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
