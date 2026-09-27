<template>
  <el-dialog
    v-model="visible"
    title="质检责任认定"
    width="640px"
    destroy-on-close
    append-to-body
    :close-on-click-modal="false"
  >
    <div v-loading="submitting">
      <!-- 卷快照只读区 -->
      <div class="mb-4 bg-gray-50 dark:bg-gray-800 p-3 rounded border border-gray-200 dark:border-gray-700">
        <div class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">卷信息快照（只读）</div>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="卷号">
            <span class="font-semibold">{{ record?.rollNo || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="缸号">{{ record?.vatNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="等级">
            <DictTag
              v-if="record?.grade"
              :type="DICT_TYPE.LUNAERP_INSPECTION_GRADE"
              :value="record.grade"
            />
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="机台号">{{ record?.machineNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="产品名称" :span="2">{{ record?.productName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="检验单号">
            <span>{{ record?.inspectionOrderCode || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实称/米数">
            <span>{{ record?.measuredWeight != null ? record.measuredWeight + ' kg' : '—' }}</span>
            <span class="mx-1">/</span>
            <span>{{ record?.lengthM != null ? record.lengthM + ' m' : '—' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 认定表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
        class="mt-2"
      >
        <el-form-item label="责任类型" prop="responsibility">
          <el-radio-group v-model="formData.responsibility" @change="handleResponsibilityChange">
            <el-radio-button
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_RESPONSIBILITY)"
              :key="dict.value"
              :label="dict.value"
            >
              {{ dict.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="formData.responsibility === 'OPERATOR'"
          label="挡车工"
          prop="workerUserId"
        >
          <el-select
            v-model="formData.workerUserId"
            placeholder="请选择扣款挡车工"
            clearable
            filterable
            class="!w-full"
          >
            <el-option
              v-for="w in workerOptions"
              :key="w.id"
              :label="w.nickname"
              :value="w.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="原因备注" prop="note">
          <el-input
            v-model="formData.note"
            type="textarea"
            :rows="3"
            placeholder="请输入责任认定原因备注（选填，如氨纶灯不亮导致断氨纶）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false" :disabled="submitting">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确认认定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import type {
  InspectionConfirmationVO,
  WorkerOptionVO
} from '@/api/lunaerp-elmui/production/inspection-confirmation'

const props = defineProps<{
  workerOptions: WorkerOptionVO[]
}>()

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const record = ref<InspectionConfirmationVO | null>(null)
const formRef = ref<FormInstance>()

interface FormData {
  id: number | undefined
  responsibility: string
  workerUserId: number | undefined
  note: string
}

const formData = reactive<FormData>({
  id: undefined,
  responsibility: 'EQUIPMENT',
  workerUserId: undefined,
  note: ''
})

const formRules = reactive<FormRules>({
  responsibility: [{ required: true, message: '请选择责任类型', trigger: 'change' }],
  workerUserId: [
    {
      validator: (_rule, value, callback) => {
        if (formData.responsibility === 'OPERATOR' && !value) {
          callback(new Error('操作失误必须指定挡车工'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
})

/** 切换责任类型时清空非 OPERATOR 下的挡车工 */
const handleResponsibilityChange = (val: string) => {
  if (val !== 'OPERATOR') {
    formData.workerUserId = undefined
  }
}

/** 打开弹窗 */
const open = (row: InspectionConfirmationVO) => {
  record.value = row
  formData.id = row.id
  formData.responsibility = 'EQUIPMENT'
  // 若该行之前已有挡车工信息，初始化可填入；否则留空
  formData.workerUserId = row.workerUserId || undefined
  formData.note = ''
  visible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

/** 提交认定 */
const handleSubmit = async () => {
  if (!formRef.value || !formData.id) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await InspectionConfirmationApi.confirmInspectionConfirmation({
      id: formData.id,
      responsibility: formData.responsibility,
      workerUserId: formData.responsibility === 'OPERATOR' ? formData.workerUserId : undefined,
      note: formData.note || undefined
    })
    ElMessage.success('责任认定成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
