<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    :disabled="isViewMode"
    label-width="110px"
    class="dyeing-order-form"
    v-loading="loading"
  >
    <!-- 第一区：基本信息 -->
    <div class="section-title">基本信息</div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="销售合同" prop="salesContractId">
          <el-select
            v-model="formData.salesContractId"
            placeholder="输入合同号搜索销售合同（必选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchContracts"
            :loading="contractLoading"
            class="!w-full"
            @change="handleContractChange"
          >
            <el-option
              v-for="item in contractOptions"
              :key="item.id"
              :label="item.contractNo"
              :value="item.id!"
            >
              <span>{{ item.contractNo }}</span>
              <span v-if="item.clientName" class="option-client">({{ item.clientName }})</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="客户名称">
          <el-input :model-value="formData.clientName || '—'" disabled placeholder="选合同后自动带出" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="承染方" prop="dyeingFactoryId">
          <el-select
            v-model="formData.dyeingFactoryId"
            placeholder="输入供应商名称搜索染厂（必选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            class="!w-full"
            @change="handleSupplierChange"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            >
              <span>{{ item.companyName }}</span>
              <span v-if="item.supplierNo" class="option-supplier-no">({{ item.supplierNo }})</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="单据日期" prop="orderDate">
          <el-date-picker
            v-model="formData.orderDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择单据日期"
            class="!w-full"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="交货日期" prop="deliveryDate">
          <el-date-picker
            v-model="formData.deliveryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择交货日期（可空）"
            class="!w-full"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二区：产品规格 -->
    <div class="section-title">产品规格</div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="formData.productCode" maxlength="64" placeholder="请输入产品编码" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="formData.productName" maxlength="128" placeholder="请输入产品名称" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="成分" prop="composition">
          <el-input v-model="formData.composition" maxlength="128" placeholder="如 95棉5氨" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="幅宽" prop="width">
          <el-input v-model="formData.width" maxlength="64" placeholder="如 160cm 有效" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="克重" prop="gsm">
          <el-input v-model="formData.gsm" maxlength="64" placeholder="如 180g" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="缩水率" prop="shrinkage">
          <el-input v-model="formData.shrinkage" maxlength="64" placeholder="如 ≤5%" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第三区：技术规格与色牢度 -->
    <div class="section-title">技术规格&色牢度</div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="损耗率" prop="loss">
          <el-input v-model="formData.loss" maxlength="64" placeholder="如 5%" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="pH 值" prop="ph">
          <el-input v-model="formData.ph" maxlength="64" placeholder="如 6.5" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="水洗牢度" prop="fastnessWash">
          <el-input v-model="formData.fastnessWash" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="皂洗牢度" prop="fastnessSoap">
          <el-input v-model="formData.fastnessSoap" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="干摩擦牢度" prop="fastnessDryRub">
          <el-input v-model="formData.fastnessDryRub" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="湿摩擦牢度" prop="fastnessWetRub">
          <el-input v-model="formData.fastnessWetRub" maxlength="64" placeholder="如 3-4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="日晒牢度" prop="fastnessLight">
          <el-input v-model="formData.fastnessLight" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="汗渍牢度" prop="fastnessPerspiration">
          <el-input v-model="formData.fastnessPerspiration" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="干洗牢度" prop="fastnessDryClean">
          <el-input v-model="formData.fastnessDryClean" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="六千沾色" prop="fastnessSixFiber">
          <el-input v-model="formData.fastnessSixFiber" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="其他要求" prop="otherReq">
          <el-input
            v-model="formData.otherReq"
            type="textarea"
            :rows="2"
            maxlength="512"
            placeholder="请输入其他技术或质量要求"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四区：备注 -->
    <div class="section-title">备注</div>
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            maxlength="512"
            placeholder="请输入备注说明"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 操作按钮 -->
    <div v-if="!isViewMode" class="form-actions">
      <el-button type="primary" :loading="submitLoading" @click="handleSave('DRAFT')">
        保存为草稿
      </el-button>
      <el-button type="success" :loading="submitLoading" @click="handleSave('ACTIVE')">
        下达
      </el-button>
      <el-button @click="handleCancel">取消</el-button>
    </div>
  </el-form>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import * as DyeingOrderApi from '@/api/lunaerp-elmui/production/dyeing-order'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'DyeingOrderBasicInfoTab' })

const props = defineProps<{
  orderId?: string | number | null
  isViewMode?: boolean
}>()

const emit = defineEmits<{
  (e: 'success', id: number): void
  (e: 'cancel'): void
}>()

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = ref<DyeingOrderVO>({
  salesContractId: undefined,
  contractNo: '',
  customerId: undefined,
  clientName: '',
  dyeingFactoryId: undefined as any,
  dyeingFactoryName: '',
  orderDate: formatDate(new Date(), 'YYYY-MM-DD'),
  deliveryDate: '',
  productCode: '',
  productName: '',
  composition: '',
  width: '',
  gsm: '',
  shrinkage: '',
  loss: '',
  ph: '',
  fastnessWash: '',
  fastnessSoap: '',
  fastnessDryRub: '',
  fastnessWetRub: '',
  fastnessLight: '',
  fastnessPerspiration: '',
  fastnessDryClean: '',
  fastnessSixFiber: '',
  otherReq: '',
  remark: '',
  status: 'DRAFT'
})

const rules: FormRules = {
  salesContractId: [{ required: true, message: '请选择销售合同', trigger: 'change' }],
  dyeingFactoryId: [{ required: true, message: '请选择承染方（染厂）', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择单据日期', trigger: 'change' }]
}

// 销售合同远程搜索
const contractOptions = ref<SalesContractVO[]>([])
const contractLoading = ref(false)
const searchContracts = async (query: string) => {
  contractLoading.value = true
  try {
    const res = await SalesContractApi.getSalesContractPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || undefined
    })
    contractOptions.value = res.list || []
  } finally {
    contractLoading.value = false
  }
}

const handleContractChange = (val?: number) => {
  if (!val) {
    formData.value.contractNo = ''
    formData.value.customerId = undefined
    formData.value.clientName = ''
    return
  }
  const found = contractOptions.value.find((c) => c.id === val)
  if (found) {
    formData.value.contractNo = found.contractNo || ''
    formData.value.customerId = found.customerId
    formData.value.clientName = found.clientName || ''
  }
}

// 承染方（供应商）远程搜索
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || undefined,
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list || []
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (val?: number) => {
  if (!val) {
    formData.value.dyeingFactoryName = ''
    return
  }
  const found = supplierOptions.value.find((s) => s.id === val)
  if (found) {
    formData.value.dyeingFactoryName = found.companyName || ''
  }
}

// 保存/下达
const handleSave = async (status: 'DRAFT' | 'ACTIVE') => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    const submitData: DyeingOrderVO = {
      ...formData.value,
      status
    }
    const id = await DyeingOrderApi.createDyeingOrder(submitData)
    message.success(status === 'ACTIVE' ? '染色单下达成功' : '染色单保存为草稿成功')
    emit('success', id)
    router.push('/production/dyeing-order')
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  emit('cancel')
  router.push('/production/dyeing-order')
}
</script>

<style lang="scss" scoped>
.section-title {
  margin: 16px 0 12px;
  padding-left: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-left: 3px solid var(--el-color-primary);

  &:first-child {
    margin-top: 0;
  }
}

.option-client,
.option-supplier-no {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  text-align: center;
}
</style>
