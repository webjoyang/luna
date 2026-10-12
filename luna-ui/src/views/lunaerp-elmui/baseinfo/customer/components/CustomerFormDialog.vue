<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isCreate ? '新建客户' : '修改客户'"
    width="800px"
    destroy-on-close
    append-to-body
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
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
          <el-form-item label="英文名称" prop="nameEn">
            <el-input v-model="formData.nameEn" placeholder="请输入英文名称" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="英文地址" prop="addressEn">
            <el-input v-model="formData.addressEn" placeholder="请输入英文地址" maxlength="512" />
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
            <el-select v-model="formData.cooperationStatus" placeholder="请选择合作状态" clearable class="!w-full">
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
        <el-col :span="12">
          <el-form-item label="归属业务员" prop="salespersonUserId">
            <UserSelect
              v-model="formData.salespersonUserId"
              placeholder="请选择归属业务员（可空）"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="默认毛利率 (%)" prop="defaultMarginRate">
            <el-input-number
              v-model="marginRatePercent"
              :min="-99"
              :max="99.9"
              :step="1"
              :precision="2"
              controls-position="right"
              placeholder="留空回退全局 15%"
              class="!w-full"
            />
          </el-form-item>
        </el-col>

        <!-- 开票与账期信息 -->
        <el-col :span="24">
          <el-divider content-position="left">开票与账期信息</el-divider>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开票抬头" prop="invoiceTitle">
            <el-input v-model="formData.invoiceTitle" placeholder="留空时自动用公司名称回填" maxlength="256" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开票地址" prop="invoiceAddress">
            <el-input v-model="formData.invoiceAddress" placeholder="请输入开票地址" maxlength="512" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开户银行" prop="bankName">
            <el-input v-model="formData.bankName" placeholder="请输入开户银行" maxlength="256" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开户行账号" prop="bankAccountNo">
            <el-input v-model="formData.bankAccountNo" placeholder="请输入开户行账号" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="信用账期天数" prop="creditDays">
            <el-input-number
              v-model="formData.creditDays"
              :min="0"
              :max="3650"
              :step="1"
              controls-position="right"
              placeholder="留空表示不设账期"
              class="!w-full"
            />
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
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import UserSelect from '@/views/system/user/components/UserSelect.vue'

defineOptions({ name: 'LunaerpElmuiCustomerFormDialog' })

const emit = defineEmits<{ (e: 'success'): void }>()

const dialogVisible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const marginRatePercent = ref<number | undefined>(undefined)

const formData = reactive<CustomerVO>({
  id: undefined,
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
  status: 'ACTIVE',
  salespersonUserId: undefined,
  defaultMarginRate: undefined,
  nameEn: '',
  addressEn: '',
  invoiceTitle: '',
  invoiceAddress: '',
  bankName: '',
  bankAccountNo: '',
  creditDays: undefined
})

const formRules: FormRules = {
  companyName: [{ required: true, message: '公司名称不能为空', trigger: 'blur' }],
  clientType: [{ required: true, message: '客户类型不能为空', trigger: 'change' }]
}

const isCreate = computed(() => !formData.id)

const resetForm = () => {
  formData.id = undefined
  formData.companyName = ''
  formData.shortName = ''
  formData.phone = ''
  formData.address = ''
  formData.clientType = ''
  formData.industry = ''
  formData.scale = ''
  formData.source = ''
  formData.cooperationStatus = ''
  formData.unifiedSocialCreditCode = ''
  formData.remark = ''
  formData.status = 'ACTIVE'
  formData.salespersonUserId = undefined
  formData.defaultMarginRate = undefined
  formData.nameEn = ''
  formData.addressEn = ''
  formData.invoiceTitle = ''
  formData.invoiceAddress = ''
  formData.bankName = ''
  formData.bankAccountNo = ''
  formData.creditDays = undefined
  marginRatePercent.value = undefined
  formRef.value?.resetFields()
}

const open = async (id?: number) => {
  dialogVisible.value = true
  resetForm()
  if (id) {
    loading.value = true
    try {
      const data = await CustomerApi.getCustomer(id)
      Object.assign(formData, data)
      marginRatePercent.value =
        data.defaultMarginRate != null ? Number((data.defaultMarginRate * 100).toFixed(4)) : undefined
    } finally {
      loading.value = false
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    const payload: CustomerVO = {
      ...formData,
      defaultMarginRate:
        marginRatePercent.value != null ? Number((marginRatePercent.value / 100).toFixed(6)) : undefined
    }
    if (isCreate.value) {
      await CustomerApi.createCustomer(payload)
      ElMessage.success('创建成功')
    } else {
      await CustomerApi.updateCustomer(payload)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
