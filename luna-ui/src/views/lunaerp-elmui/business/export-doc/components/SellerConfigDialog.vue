<template>
  <el-dialog
    v-model="dialogVisible"
    title="卖方出口资料配置"
    width="650px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="140px"
      v-loading="loading"
    >
      <el-alert
        title="此处维护当前企业的统一出口抬头与银行收款资料，新建单证套时将自动预填。"
        type="info"
        show-icon
        :closable="false"
        class="!mb-16px"
      />

      <el-form-item label="卖方英文名称" prop="sellerName">
        <el-input
          v-model="formData.sellerName"
          placeholder="例如：HANGZHOU TEXTILE IMPORT & EXPORT CO., LTD."
          maxlength="256"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="卖方英文地址" prop="sellerAddress">
        <el-input
          v-model="formData.sellerAddress"
          type="textarea"
          :rows="2"
          placeholder="例如：No. 88 Luna Industrial Road, Hangzhou, Zhejiang, China"
          maxlength="512"
          show-word-limit
        />
      </el-form-item>

      <el-divider content-position="left">收款银行全量信息（PI / 套打默认资料）</el-divider>

      <el-form-item label="收款银行名称" prop="bankName">
        <el-input
          v-model="formData.bankName"
          placeholder="例如：BANK OF CHINA HANGZHOU BRANCH"
          maxlength="256"
        />
      </el-form-item>

      <el-form-item label="银行账号 (A/C)" prop="bankAccount">
        <el-input
          v-model="formData.bankAccount"
          placeholder="例如：3880 1234 5678 90"
          maxlength="128"
        />
      </el-form-item>

      <el-form-item label="银行 SWIFT 码" prop="bankSwift">
        <el-input
          v-model="formData.bankSwift"
          placeholder="例如：BKCHCNBJ330"
          maxlength="32"
        />
      </el-form-item>

      <el-form-item label="银行详细地址" prop="bankAddress">
        <el-input
          v-model="formData.bankAddress"
          type="textarea"
          :rows="2"
          placeholder="例如：No. 128 Yan'an Road, Hangzhou, Zhejiang, China"
          maxlength="512"
        />
      </el-form-item>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          placeholder="内部备注"
          maxlength="256"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getSellerConfig,
  saveSellerConfig,
  type ExportSellerConfigVO
} from '@/api/lunaerp-elmui/business/export-doc'

defineOptions({ name: 'SellerConfigDialog' })

const emit = defineEmits(['success'])

const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<ExportSellerConfigVO>({
  id: undefined,
  sellerName: '',
  sellerAddress: '',
  bankName: '',
  bankAccount: '',
  bankSwift: '',
  bankAddress: '',
  remark: ''
})

const formRules: FormRules = {
  sellerName: [{ required: true, message: '请输入卖方英文名称', trigger: 'blur' }]
}

/** 打开弹窗并载入配置 */
const open = async () => {
  dialogVisible.value = true
  loading.value = true
  try {
    const res = await getSellerConfig()
    if (res) {
      Object.assign(formData, {
        id: res.id,
        sellerName: res.sellerName || '',
        sellerAddress: res.sellerAddress || '',
        bankName: res.bankName || '',
        bankAccount: res.bankAccount || '',
        bankSwift: res.bankSwift || '',
        bankAddress: res.bankAddress || '',
        remark: res.remark || ''
      })
    } else {
      resetForm()
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取卖方配置失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formData.id = undefined
  formData.sellerName = ''
  formData.sellerAddress = ''
  formData.bankName = ''
  formData.bankAccount = ''
  formData.bankSwift = ''
  formData.bankAddress = ''
  formData.remark = ''
}

/** 提交保存 */
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await saveSellerConfig({ ...formData })
      ElMessage.success('卖方出口资料保存成功')
      dialogVisible.value = false
      emit('success')
    } catch (err: any) {
      ElMessage.error(err?.message || '保存卖方配置失败')
    } finally {
      saving.value = false
    }
  })
}

defineExpose({ open })
</script>
