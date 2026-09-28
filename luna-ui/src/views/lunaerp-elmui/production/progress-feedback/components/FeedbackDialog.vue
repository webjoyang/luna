<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="520px" destroy-on-close append-to-body>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="90px"
      v-loading="submitting"
    >
      <el-form-item label="缸号">
        <span class="vat-no-text">{{ vatNo || '-' }}</span>
      </el-form-item>

      <el-form-item label="反馈内容" prop="content">
        <el-input
          v-model="formData.content"
          type="textarea"
          :rows="4"
          placeholder="请输入沟通反馈记录内容（流水只增不改，限1000字）"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="标记问题" prop="issueFlag">
        <el-checkbox v-model="formData.issueFlag">
          标记为问题（缸卡将显示未决问题红点提示，需后续跟踪闭环）
        </el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit"> 提交反馈 </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createNote,
  type ProgressFeedbackNoteCreateVO
} from '@/api/lunaerp-elmui/production/progress-feedback/note'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const vatNo = ref('')

const formData = reactive<ProgressFeedbackNoteCreateVO>({
  feedbackId: 0,
  content: '',
  issueFlag: false
})

const formRules = reactive<FormRules>({
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('反馈内容不能为空字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const dialogTitle = computed(() => {
  return vatNo.value ? `添加反馈记录 - 缸号：${vatNo.value}` : '添加反馈记录'
})

const open = (targetFeedbackId: number, targetVatNo?: string) => {
  formData.feedbackId = targetFeedbackId
  formData.content = ''
  formData.issueFlag = false
  vatNo.value = targetVatNo || ''
  visible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  try {
    submitting.value = true
    await createNote({
      feedbackId: formData.feedbackId,
      content: formData.content.trim(),
      issueFlag: formData.issueFlag
    })
    ElMessage.success('反馈记录添加成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({
  open
})
</script>

<style scoped lang="scss">
.vat-no-text {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
