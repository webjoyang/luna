<template>
  <el-dialog
    v-model="visible"
    title="手工登记质检确认"
    width="680px"
    destroy-on-close
    append-to-body
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="submitting"
    >
      <el-divider content-position="left">核心认定信息（必填）</el-divider>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="卷号" prop="rollNo">
            <el-input v-model="formData.rollNo" placeholder="请输入卷号" maxlength="64" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="等级" prop="grade">
            <el-select v-model="formData.grade" placeholder="请选择等级" clearable class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_GRADE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

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

      <el-row :gutter="16">
        <el-col :span="12" v-if="formData.responsibility === 'OPERATOR'">
          <el-form-item label="挡车工" prop="workerUserId">
            <el-select
              v-model="formData.workerUserId"
              placeholder="请选择挡车工"
              clearable
              filterable
              class="!w-full"
            >
              <el-option v-for="w in workerList" :key="w.id" :label="w.nickname" :value="w.id" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="formData.responsibility === 'OPERATOR' ? 12 : 24">
          <el-form-item label="认定时间" prop="confirmedAt">
            <el-date-picker
              v-model="formData.confirmedAt"
              type="datetime"
              placeholder="缺省当前时间，可补录过去时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled-date="disableFutureDate"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">卷信息与源单（选填）</el-divider>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="缸号" prop="vatNo">
            <el-input v-model="formData.vatNo" placeholder="请输入缸号" maxlength="64" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="机台号" prop="machineNo">
            <el-input
              v-model="formData.machineNo"
              placeholder="请输入机台号"
              maxlength="64"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="产品名称" prop="productName">
            <el-input
              v-model="formData.productName"
              placeholder="请输入产品名称"
              maxlength="128"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品编码" prop="productCode">
            <el-input
              v-model="formData.productCode"
              placeholder="请输入产品编码"
              maxlength="64"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="颜色名称" prop="colorName">
            <el-input
              v-model="formData.colorName"
              placeholder="请输入颜色名称"
              maxlength="128"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品色号" prop="colorNo">
            <el-input
              v-model="formData.colorNo"
              placeholder="请输入产品色号"
              maxlength="64"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="款号" prop="styleNo">
            <el-input
              v-model="formData.styleNo"
              placeholder="请输入款号"
              maxlength="64"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联检验单" prop="inspectionOrderId">
            <el-select
              v-model="formData.inspectionOrderId"
              placeholder="搜索检验单号"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchInspectionOrders"
              :loading="orderLoading"
              class="!w-full"
            >
              <el-option
                v-for="item in orderOptions"
                :key="item.id"
                :label="item.code"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="实称重量" prop="measuredWeight">
            <el-input-number
              v-model="formData.measuredWeight"
              :min="0"
              :precision="2"
              :step="0.5"
              placeholder="KG"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="米数" prop="lengthM">
            <el-input-number
              v-model="formData.lengthM"
              :min="0"
              :precision="2"
              :step="1"
              placeholder="M"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="原因备注" prop="note">
        <el-input
          v-model="formData.note"
          type="textarea"
          :rows="3"
          placeholder="请输入认定原因或备注"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm"> 确认登记 </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import type {
  InspectionConfirmationManualCreateVO,
  WorkerOptionVO
} from '@/api/lunaerp-elmui/production/inspection-confirmation'
import * as InspectionOrderApi from '@/api/lunaerp-elmui/production/inspection-order'
import type { InspectionOrderVO } from '@/api/lunaerp-elmui/production/inspection-order'

defineOptions({ name: 'InspectionConfirmationManualCreateDialog' })

const props = defineProps<{
  workerOptions?: WorkerOptionVO[]
}>()

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()

// 挡车工列表：优先使用 props，若空则自行拉取
const workerList = ref<WorkerOptionVO[]>([])

// 检验单搜索候选
const orderOptions = ref<InspectionOrderVO[]>([])
const orderLoading = ref(false)

const searchInspectionOrders = async (query: string) => {
  orderLoading.value = true
  try {
    const res = await InspectionOrderApi.getInspectionOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    orderOptions.value = res.list || []
  } catch {
    orderOptions.value = []
  } finally {
    orderLoading.value = false
  }
}

/** 初始化表单数据模型 */
const initFormData = (): InspectionConfirmationManualCreateVO => ({
  rollNo: '',
  grade: '',
  responsibility: '',
  workerUserId: undefined,
  vatNo: '',
  machineNo: '',
  productCode: '',
  productName: '',
  colorName: '',
  colorNo: '',
  styleNo: '',
  measuredWeight: undefined,
  lengthM: undefined,
  inspectionOrderId: undefined,
  confirmedAt: formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss'),
  note: ''
})

/** 表单数据模型 */
const formData = ref<InspectionConfirmationManualCreateVO>(initFormData())

/** 禁止选择未来时间 */
const disableFutureDate = (time: Date) => {
  return time.getTime() > Date.now()
}

/** 校验挡车工：仅当 responsibility === 'OPERATOR' 时必填 */
const validateWorker = (_rule: any, value: any, callback: any) => {
  if (formData.value.responsibility === 'OPERATOR') {
    if (!value) {
      callback(new Error('操作失误必须指定挡车工'))
      return
    }
  }
  callback()
}

/** 校验认定时间：不允许未来时间 */
const validateConfirmedAt = (_rule: any, value: any, callback: any) => {
  if (value) {
    const time = new Date(value).getTime()
    if (time > Date.now() + 60000) {
      // 留 1 分钟时钟误差缓冲
      callback(new Error('认定时间不允许为未来时间'))
      return
    }
  }
  callback()
}

/** 表单校验规则 */
const formRules = reactive({
  rollNo: [{ required: true, message: '请输入卷号', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择等级', trigger: 'change' }],
  responsibility: [{ required: true, message: '请选择责任类型', trigger: 'change' }],
  workerUserId: [{ validator: validateWorker, trigger: 'change' }],
  confirmedAt: [{ validator: validateConfirmedAt, trigger: 'change' }]
})

/** 切换责任类型 */
const handleResponsibilityChange = (val: string) => {
  if (val !== 'OPERATOR') {
    formData.value.workerUserId = undefined
    formRef.value?.clearValidate('workerUserId')
  }
}

/** 打开弹窗 */
const open = async () => {
  visible.value = true
  // 初始化默认值
  formData.value = initFormData()

  // 加载挡车工列表
  if (props.workerOptions && props.workerOptions.length > 0) {
    workerList.value = props.workerOptions
  } else {
    try {
      const res = await InspectionConfirmationApi.getWorkerOptions()
      workerList.value = res || []
    } catch {
      workerList.value = []
    }
  }

  // 初始化检验单搜索
  searchInspectionOrders('')

  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

/** 提交表单 */
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const submitData: InspectionConfirmationManualCreateVO = {
      ...formData.value,
      rollNo: formData.value.rollNo?.trim(),
      vatNo: formData.value.vatNo?.trim() || undefined,
      machineNo: formData.value.machineNo?.trim() || undefined,
      productCode: formData.value.productCode?.trim() || undefined,
      productName: formData.value.productName?.trim() || undefined,
      colorName: formData.value.colorName?.trim() || undefined,
      colorNo: formData.value.colorNo?.trim() || undefined,
      styleNo: formData.value.styleNo?.trim() || undefined,
      note: formData.value.note?.trim() || undefined,
      workerUserId:
        formData.value.responsibility === 'OPERATOR' ? formData.value.workerUserId : undefined
    }
    await InspectionConfirmationApi.manualCreateInspectionConfirmation(submitData)
    ElMessage.success('手工登记成功')
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
