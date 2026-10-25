<template>
  <el-dialog
    v-model="visible"
    title="编辑对账单备注"
    width="500px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form :model="formData" ref="formRef" label-width="80px">
      <el-form-item label="对账备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="4"
          placeholder="请输入对账备注（支持清空或修改）"
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
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { updateCustomerStatement } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'StatementEditDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitLoading = ref(false)

const formData = reactive({
  id: 0,
  remark: ''
})

const open = (id: number, currentRemark?: string) => {
  formData.id = id
  formData.remark = currentRemark || ''
  visible.value = true
}

const handleSubmit = async () => {
  if (!formData.id) return
  submitLoading.value = true
  try {
    await updateCustomerStatement({
      id: formData.id,
      remark: formData.remark
    })
    ElMessage.success('对账单备注更新成功')
    visible.value = false
    emit('success')
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
