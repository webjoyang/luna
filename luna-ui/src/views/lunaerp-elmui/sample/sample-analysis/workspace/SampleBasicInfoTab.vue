<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="formRules"
    label-width="110px"
    :disabled="isReadOnly"
  >
    <!-- 基础来样属性卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="font-bold text-14px">来样与客户信息</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="样品编号" prop="sampleAnalysisNo">
            <el-input
              v-model="formData.sampleAnalysisNo"
              :placeholder="isCreate ? (previewNo ? `预览：${previewNo}（可手填）` : '系统自动生成') : ''"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="选择客户" prop="customerId">
            <el-select
              v-model="formData.customerId"
              placeholder="搜索或选择客户档案"
              clearable
              filterable
              remote
              :remote-method="searchCustomers"
              :loading="customerLoading"
              @change="handleCustomerChange"
              @clear="handleCustomerClear"
              class="!w-full"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.id"
                :label="item.companyName"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="客户名称" prop="clientName">
            <el-input
              v-model="formData.clientName"
              placeholder="可由客户档案带出或纯手填"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="联系人" prop="clientContact">
            <el-input
              v-model="formData.clientContact"
              placeholder="来样联系人（自由文本）"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="客户款号" prop="clientStyleNo">
            <el-input
              v-model="formData.clientStyleNo"
              placeholder="客户款号（自由文本）"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="登记人">
            <el-input
              :model-value="formData.creatorName || '系统自动记录'"
              disabled
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 来样规格属性卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="font-bold text-14px">规格属性</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="产品名称" prop="productName" required>
            <el-input
              v-model="formData.productName"
              placeholder="请输入产品名称（必填）"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="成分" prop="composition">
            <el-input
              v-model="formData.composition"
              placeholder="如：100%棉、95%涤5%氨纶"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="米长(M/KG)">
            <el-input
              :model-value="calculatedMeterLength"
              disabled
              placeholder="-"
            >
              <template #append>米/公斤</template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="门幅(CM)" prop="width">
            <el-input-number
              v-model="formData.width"
              :precision="2"
              :step="1"
              :min="0"
              placeholder="门幅"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="克重(GSM)" prop="weight">
            <el-input-number
              v-model="formData.weight"
              :precision="2"
              :step="1"
              :min="0"
              placeholder="克重"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        <el-col :span="8">
          <el-form-item label="月结单价(元)" prop="unitPrice">
            <el-input-number
              v-model="formData.unitPrice"
              :precision="2"
              :step="5"
              :min="0"
              placeholder="默认 30.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="2"
              placeholder="其他备注说明"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 来样照片卡片 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="font-bold text-14px">来样实物照片</span>
            <el-tag :type="(formData.photos?.length || 0) >= MAX_PHOTOS_LIMIT ? 'warning' : 'info'" size="small">
              {{ formData.photos?.length || 0 }} / {{ MAX_PHOTOS_LIMIT }} 张
            </el-tag>
          </div>
          <span v-if="!isReadOnly" class="text-12px text-gray-400">
            最多 {{ MAX_PHOTOS_LIMIT }} 张；按上传顺序排序，第 1 张作为列表封面图；已满 {{ MAX_PHOTOS_LIMIT }} 张时禁止继续上传
          </span>
        </div>
      </template>

      <!-- 查看模式无照片 -->
      <div
        v-if="isReadOnly && (!formData.photos || formData.photos.length === 0)"
        class="py-20px text-center text-gray-400"
      >
        <Icon icon="ep:picture" class="text-32px mb-8px" />
        <div>暂无来样照片</div>
      </div>

      <!-- 照片上传与展示 -->
      <div
        v-else
        :class="{
          'hide-uploader': isReadOnly || (formData.photos && formData.photos.length >= MAX_PHOTOS_LIMIT)
        }"
      >
        <UploadImgs
          v-model="formData.photos"
          :limit="MAX_PHOTOS_LIMIT"
          :disabled="isReadOnly"
          height="110px"
          width="110px"
        />
      </div>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { UploadImgs } from '@/components/UploadFile'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import type { SampleAnalysisVO } from '@/api/lunaerp-elmui/sample/sample-analysis'

/** 样品照片最大上传限制（对齐后端 MAX_PHOTOS_COUNT = 9） */
const MAX_PHOTOS_LIMIT = 9

const props = defineProps<{
  isReadOnly: boolean
  isCreate: boolean
  previewNo?: string
}>()

const formData = defineModel<SampleAnalysisVO>({ required: true })

const formRef = ref<FormInstance>()

const formRules: FormRules = {
  productName: [{ required: true, message: '产品名称不能为空', trigger: 'blur' }]
}

// 客户远程下拉搜索
const customerOptions = ref<CustomerVO[]>([])
const customerLoading = ref(false)

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    customerOptions.value = res.list || []
    // 保证当前选中项在下拉选项中存在
    if (formData.value.customerId && !customerOptions.value.some((c) => c.id === formData.value.customerId)) {
      if (formData.value.clientName) {
        customerOptions.value.unshift({
          id: formData.value.customerId,
          companyName: formData.value.clientName,
          clientType: ''
        })
      }
    }
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = (customerId: number) => {
  const selected = customerOptions.value.find((c) => c.id === customerId)
  if (selected) {
    formData.value.clientName = selected.companyName
  }
}

const handleCustomerClear = () => {
  formData.value.customerId = undefined
}

// 米长计算：100000 / (width * weight)，若门幅或克重缺失或非正，显示 "-"
const calculatedMeterLength = computed(() => {
  const w = formData.value.width
  const g = formData.value.weight
  if (w != null && g != null && Number(w) > 0 && Number(g) > 0) {
    const val = 100000 / (Number(w) * Number(g))
    return val.toFixed(2)
  }
  return '-'
})

/** 表单校验暴露给父组件 */
const validate = async () => {
  return formRef.value?.validate()
}

defineExpose({
  validate
})

onMounted(() => {
  if (!formData.value.photos) {
    formData.value.photos = []
  }
  searchCustomers('')
})
</script>

<style lang="scss" scoped>
:deep(.hide-uploader .el-upload--picture-card) {
  display: none !important;
}
</style>
