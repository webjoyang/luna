<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="580px"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="loading"
    >
      <el-form-item label="账户名称" prop="accountName">
        <el-input
          v-model="formData.accountName"
          placeholder="如：基本户（工行盛泽支行）"
          maxlength="64"
          clearable
        />
      </el-form-item>

      <el-form-item label="账户类型" prop="accountType">
        <el-select
          v-model="formData.accountType"
          placeholder="请选择账户类型"
          :disabled="isEdit"
          class="w-full"
        >
          <el-option
            v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
        <div v-if="isEdit" class="text-12px text-gray-400 mt-2px">账户类型建后不可更改</div>
      </el-form-item>

      <el-form-item label="账户性质" prop="accountNature">
        <el-radio-group v-model="formData.accountNature">
          <el-radio
            v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_NATURE)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        label="账号/标识"
        prop="accountNo"
        :rules="[
          {
            required: isAccountNoRequired(formData.accountType),
            message: '银行、微信与支付宝账户必须填写账号或标识（020_105）',
            trigger: 'blur'
          }
        ]"
      >
        <el-input
          v-model="formData.accountNo"
          :placeholder="
            formData.accountType === 'CASH'
              ? '现金账户无需填写账号'
              : '请输入银行卡号或微信/支付宝商户标识'
          "
          maxlength="64"
          clearable
        />
        <div class="text-12px text-gray-400 mt-2px">
          {{
            isAccountNoRequired(formData.accountType)
              ? '银行/微信/支付宝类型必填，保存后敏感信息受控脱敏展示'
              : '现金账户选填'
          }}
        </div>
      </el-form-item>

      <el-form-item label="账户币种" prop="currency">
        <el-select
          v-model="formData.currency"
          placeholder="请选择币种"
          :disabled="isEdit"
          class="w-full"
        >
          <el-option label="人民币 (RMB)" value="RMB" />
          <el-option label="美元 (USD)" value="USD" />
          <el-option label="欧元 (EUR)" value="EUR" />
          <el-option label="港币 (HKD)" value="HKD" />
        </el-select>
        <div v-if="isEdit" class="text-12px text-gray-400 mt-2px">币种建后不可更改</div>
      </el-form-item>

      <el-form-item label="期初余额" prop="openingBalance">
        <el-input-number
          v-model="formData.openingBalance"
          :precision="2"
          :step="100"
          :min="-999999999"
          :max="999999999"
          class="!w-full"
        />
        <div v-if="isEdit" class="text-12px text-amber-600 mt-2px">
          提示：期初余额仅在账户无任何流水时允许修改；已有分录账户将自动拦截（错误码 020_104）
        </div>
        <div v-else class="text-12px text-gray-400 mt-2px">
          期初余额创建后系统将自动物化为当前可用余额
        </div>
      </el-form-item>

      <el-form-item v-if="isEdit" label="账户状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注说明（选填）"
          maxlength="255"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          v-hasPermi="['lunaerp:finance:account:create', 'lunaerp:finance:account:update']"
          @click="handleSubmit"
        >
          确定保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import { createAccount, updateAccount, getAccount } from '@/api/lunaerp-elmui/finance/account'
import type {
  AccountRespVO,
  AccountSaveReqVO,
  AccountUpdateReqVO
} from '@/api/lunaerp-elmui/finance/account'
import { isAccountNoRequired, parseAccountErrorMessage } from '../model/account'

defineOptions({ name: 'AccountFormDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editId = ref<number>()

const isEdit = computed(() => !!editId.value)
const dialogTitle = computed(() => (isEdit.value ? '编辑资金账户' : '新建资金账户'))

interface AccountFormModel {
  accountName: string
  accountType: string
  accountNature: string
  accountNo: string
  currency: string
  openingBalance: number
  status: string
  remark: string
}

const defaultFormData: AccountFormModel = {
  accountName: '',
  accountType: 'BANK',
  accountNature: 'PUBLIC',
  accountNo: '',
  currency: 'RMB',
  openingBalance: 0,
  status: 'ENABLE',
  remark: ''
}

const formData = reactive<AccountFormModel>({ ...defaultFormData })

const formRules: FormRules = {
  accountName: [
    { required: true, message: '请输入账户名称', trigger: 'blur' },
    { min: 2, max: 64, message: '账户名称长度在 2 到 64 个字符', trigger: 'blur' }
  ],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
  accountNature: [{ required: true, message: '请选择账户性质', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }],
  openingBalance: [{ required: true, message: '请输入期初余额', trigger: 'blur' }]
}

const open = async (row?: AccountRespVO) => {
  visible.value = true
  editId.value = row?.id
  Object.assign(formData, defaultFormData)

  if (row?.id) {
    loading.value = true
    try {
      const res = await getAccount(row.id)
      formData.accountName = res.accountName
      formData.accountType = res.accountType
      formData.accountNature = res.accountNature || 'PUBLIC'
      formData.accountNo = res.accountNo || ''
      formData.currency = res.currency || 'RMB'
      formData.openingBalance = res.openingBalance ?? 0
      formData.status = res.status || 'ENABLE'
      formData.remark = res.remark || ''
    } catch {
      // 容错回退使用行数据
      formData.accountName = row.accountName
      formData.accountType = row.accountType
      formData.accountNature = row.accountNature || 'PUBLIC'
      formData.accountNo = row.accountNo || ''
      formData.currency = row.currency || 'RMB'
      formData.openingBalance = row.openingBalance ?? 0
      formData.status = row.status || 'ENABLE'
      formData.remark = row.remark || ''
    } finally {
      loading.value = false
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      const updateData: AccountUpdateReqVO = {
        id: editId.value,
        accountName: formData.accountName,
        accountNature: formData.accountNature,
        accountNo: formData.accountNo,
        openingBalance: formData.openingBalance,
        status: formData.status,
        remark: formData.remark
      }
      await updateAccount(updateData)
      ElMessage.success('资金账户修改成功')
    } else {
      const createData: AccountSaveReqVO = {
        accountName: formData.accountName,
        accountType: formData.accountType,
        accountNature: formData.accountNature,
        accountNo: formData.accountNo,
        currency: formData.currency,
        openingBalance: formData.openingBalance,
        remark: formData.remark
      }
      await createAccount(createData)
      ElMessage.success('资金账户创建成功')
    }
    visible.value = false
    emit('success')
  } catch (error: any) {
    const msg = parseAccountErrorMessage(error?.code, error?.message || '保存失败')
    ElMessage.error(msg)
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
