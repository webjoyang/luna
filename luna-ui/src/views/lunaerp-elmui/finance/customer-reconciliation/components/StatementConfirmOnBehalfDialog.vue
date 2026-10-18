<template>
  <el-dialog
    v-model="visible"
    title="对账单线下代客确认"
    width="540px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-alert
      type="info"
      :closable="false"
      class="mb-16px"
      show-icon
      title="代客确认说明"
      description="内部财务人员依据客户在线下渠道（微信/电话/邮件/纸质签字等）的对账确认结果进行代录确认。确认后单据将冻结为已确认终态。"
    />
    <el-form :model="formData" :rules="rules" ref="formRef" label-width="110px">
      <el-form-item label="对账单号">
        <span class="font-mono font-bold">{{ statementNo }}</span>
      </el-form-item>
      <el-form-item label="代客确认方式" prop="confirmMethod">
        <el-select
          v-model="formData.confirmMethod"
          placeholder="请选择或输入线下确认方式"
          filterable
          allow-create
          default-first-option
          class="w-full"
        >
          <el-option label="微信确认" value="微信" />
          <el-option label="电话确认" value="电话" />
          <el-option label="邮件确认" value="邮件" />
          <el-option label="纸质签字盖章" value="纸质签字盖章" />
          <el-option label="钉钉/企微" value="钉钉/企微" />
        </el-select>
      </el-form-item>
      <el-form-item label="确认备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入代客确认依据及备注（如：客户财务李总微信确认9月份对账单无误）"
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
        v-hasPermi="['lunaerp:finance:customerrecon:confirm-on-behalf']"
      >
        确认提交
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { confirmOnBehalf } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'StatementConfirmOnBehalfDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitLoading = ref(false)
const statementNo = ref('')
const formRef = ref<FormInstance>()

const formData = reactive({
  id: 0,
  confirmMethod: '',
  remark: ''
})

const rules: FormRules = {
  confirmMethod: [{ required: true, message: '请选择或输入代客确认方式', trigger: 'change' }]
}

const open = (id: number, no: string) => {
  formData.id = id
  formData.confirmMethod = '微信'
  formData.remark = ''
  statementNo.value = no
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      await confirmOnBehalf({
        id: formData.id,
        confirmMethod: formData.confirmMethod.trim(),
        remark: formData.remark ? formData.remark.trim() : undefined
      })
      ElMessage.success('代客确认成功')
      visible.value = false
      emit('success')
    } finally {
      submitLoading.value = false
    }
  })
}

defineExpose({ open })
</script>
