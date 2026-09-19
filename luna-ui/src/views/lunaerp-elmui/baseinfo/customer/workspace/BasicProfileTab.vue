<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="120px"
    class="basic-profile-form"
    v-loading="loading"
  >
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="formData.companyName" placeholder="请输入公司名称" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="formData.shortName" placeholder="留空时自动用公司名称回填" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="公司地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入公司地址" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="客户类型" prop="clientType">
          <el-select v-model="formData.clientType" placeholder="请选择客户类型" class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_CLIENT_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="formData.industry" placeholder="请选择行业" clearable class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_INDUSTRY)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="企业规模" prop="scale">
          <el-select v-model="formData.scale" placeholder="请选择规模" clearable class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_SCALE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="客户来源" prop="source">
          <el-select v-model="formData.source" placeholder="请选择来源" clearable class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_SOURCE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="合作状态" prop="cooperationStatus">
          <el-select
            v-model="formData.cooperationStatus"
            placeholder="请选择合作状态"
            clearable
            class="!w-full"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_COOPERATION_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="统一信用代码" prop="unifiedSocialCreditCode">
          <el-input v-model="formData.unifiedSocialCreditCode" placeholder="请输入统一社会信用代码" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" class="!w-full">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <div class="form-footer">
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
        v-hasPermi="
          isCreateMode
            ? ['lunaerp:baseinfo:customer:create']
            : ['lunaerp:baseinfo:customer:update']
        "
      >
        保存
      </el-button>
      <el-button @click="handleBack">返回</el-button>
    </div>
  </el-form>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpElmuiCustomerBasicProfileTab' })

const props = defineProps<{
  /** 客户 ID，create 模式为空 */
  customerId?: number | string | null
}>()

const emit = defineEmits<{ (e: 'saved', customer: CustomerVO): void }>()

const message = ElMessage
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)

const formData = reactive<CustomerVO>({
  companyName: '',
  shortName: '',
  phone: '',
  address: '',
  clientType: '',
  industry: '',
  scale: '',
  source: '',
  cooperationStatus: '',
  unifiedSocialCreditCode: '',
  remark: '',
  status: 'ACTIVE'
})

const rules: FormRules = {
  companyName: [{ required: true, message: '公司名称不能为空', trigger: 'blur' }],
  clientType: [{ required: true, message: '客户类型不能为空', trigger: 'change' }]
}

const isCreateMode = computed(() => !props.customerId)

const loadCustomer = async () => {
  if (!props.customerId) return
  loading.value = true
  try {
    const data = await CustomerApi.getCustomer(Number(props.customerId))
    Object.assign(formData, data)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isCreateMode.value) {
      const id = await CustomerApi.createCustomer(formData)
      message.success('保存成功')
      const saved = { ...formData, id } as CustomerVO
      emit('saved', saved)
      router.push('/baseinfo/customer')
    } else {
      // 编辑模式：update 后回列表
      await CustomerApi.updateCustomer(formData)
      message.success('保存成功')
      emit('saved', { ...formData })
      router.push('/baseinfo/customer')
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push('/baseinfo/customer')
}

onMounted(() => {
  loadCustomer()
})
</script>

<style lang="scss" scoped>
.basic-profile-form {
  max-width: 1200px;
  padding: 16px;
}

.form-footer {
  margin-top: 12px;
  padding-left: 120px;
}
</style>
