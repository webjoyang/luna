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
        <el-form-item label="供应商编号" prop="supplierNo">
          <div class="no-input-wrap">
            <el-input
              v-model="formData.supplierNo"
              :placeholder="isCreateMode ? nextNoPlaceholder : '请输入供应商编号'"
            />
            <el-button
              v-if="isCreateMode"
              type="primary"
              plain
              :loading="previewing"
              @click="handlePreviewNo"
              class="no-preview-btn"
            >
              <Icon icon="ep:refresh" class="mr-2px" />预览编号
            </el-button>
          </div>
          <div v-if="isCreateMode" class="no-tip">
            留空自动生成 <span class="no-tip-code">{{ nextNoPlaceholder }}</span>，也可手填承接历史号
          </div>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="formData.companyName" placeholder="请输入公司/档口名称" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="formData.shortName" placeholder="留空时自动用公司名称回填" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="供应商类型" prop="supplierType">
          <el-select
            v-model="formData.supplierType"
            placeholder="请选择供应商类型"
            clearable
            class="!w-full"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SUPPLIER_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="主营机型" prop="machineType">
          <el-input v-model="formData.machineType" placeholder="如 34寸28针、喷水织机" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="公司地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入公司地址" />
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
            ? ['lunaerp:baseinfo:supplier:create']
            : ['lunaerp:baseinfo:supplier:update']
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
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiSupplierBasicProfileTab' })

const props = defineProps<{
  /** 供应商 ID，create 模式为空 */
  supplierId?: number | string | null
}>()

const emit = defineEmits<{ (e: 'saved', supplier: SupplierVO): void }>()

const message = ElMessage
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const previewing = ref(false)
const nextNoPlaceholder = ref('')

const formData = reactive<SupplierVO>({
  supplierNo: '',
  companyName: '',
  shortName: '',
  phone: '',
  address: '',
  supplierType: '',
  machineType: '',
  remark: '',
  status: 'ACTIVE'
})

const rules: FormRules = {
  companyName: [{ required: true, message: '公司名称不能为空', trigger: 'blur' }]
}

const isCreateMode = computed(() => !props.supplierId)

/** 预览下一个编号（只读不占号，连续调用无新建时返回值不变） */
const handlePreviewNo = async () => {
  previewing.value = true
  try {
    nextNoPlaceholder.value = await SupplierApi.getNextSupplierNo()
  } finally {
    previewing.value = false
  }
}

const loadSupplier = async () => {
  if (!props.supplierId) return
  loading.value = true
  try {
    const data = await SupplierApi.getSupplier(Number(props.supplierId))
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
      const id = await SupplierApi.createSupplier(formData)
      message.success('保存成功')
      const saved = { ...formData, id } as SupplierVO
      emit('saved', saved)
      router.push('/baseinfo/supplier')
    } else {
      await SupplierApi.updateSupplier(formData)
      message.success('保存成功')
      emit('saved', { ...formData })
      router.push('/baseinfo/supplier')
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push('/baseinfo/supplier')
}

onMounted(async () => {
  if (isCreateMode.value) {
    // 新建时预拉一次下一个编号作为占位提示（不占号）
    await handlePreviewNo()
  } else {
    await loadSupplier()
  }
})
</script>

<style lang="scss" scoped>
.basic-profile-form {
  max-width: 1200px;
  padding: 16px;
}

.no-input-wrap {
  display: flex;
  gap: 8px;
  width: 100%;

  .el-input {
    flex: 1;
  }

  .no-preview-btn {
    flex-shrink: 0;
  }
}

.no-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;

  .no-tip-code {
    font-family: monospace;
    color: var(--el-color-primary);
  }
}

.form-footer {
  margin-top: 12px;
  padding-left: 120px;
}
</style>
