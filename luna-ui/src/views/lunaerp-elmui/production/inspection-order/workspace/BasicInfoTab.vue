<template>
  <div v-loading="loading" class="basic-info-tab">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :disabled="isReadOnly"
      label-width="110px"
      class="inspection-order-form"
    >
      <!-- 第一区：单据信息 -->
      <div class="section-title">单据信息</div>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="检验单号" prop="code">
            <el-input
              v-model="formData.code"
              placeholder="系统按天自动生成（可手填）"
              maxlength="32"
              :disabled="!isCreateMode && formData.status !== 'DRAFT'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
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
        <el-col :span="8">
          <el-form-item label="检验类型" prop="type">
            <el-radio-group v-model="formData.type" @change="handleTypeChange">
              <el-radio value="GREIGE">胚布检验</el-radio>
              <el-radio value="FINISHED">成品检验</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第二区：关联对象 -->
      <div class="section-title">关联对象</div>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="关联产品" prop="productId">
            <el-select
              v-model="formData.productId"
              placeholder="输入编码或名称搜索产品（必选）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchProducts"
              :loading="productLoading"
              class="!w-full"
              @change="handleProductChange"
            >
              <el-option
                v-for="item in productOptions"
                :key="item.id"
                :label="`[${item.code}] ${item.name}`"
                :value="item.id!"
              >
                <span class="font-mono text-13px">{{ item.code }}</span>
                <span class="ml-2 text-gray-700">{{ item.name }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="承检方" prop="inspectionFactoryId">
            <el-select
              v-model="formData.inspectionFactoryId"
              placeholder="输入供应商搜索承检方（可选）"
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
                <span v-if="item.supplierNo" class="text-gray-400 ml-2">({{ item.supplierNo }})</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <!-- 胚布单（type=GREIGE 时显示） -->
        <el-col v-if="formData.type === 'GREIGE'" :span="12">
          <el-form-item label="关联胚布单" prop="greigeOrderId">
            <el-select
              v-model="formData.greigeOrderId"
              placeholder="输入单号或产品搜索胚布单（可选）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchGreigeOrders"
              :loading="greigeLoading"
              class="!w-full"
              @change="handleGreigeOrderChange"
            >
              <el-option
                v-for="item in greigeOptions"
                :key="item.id"
                :label="item.orderNo"
                :value="item.id!"
              >
                <span>{{ item.orderNo }}</span>
                <span v-if="item.productName" class="text-gray-400 ml-2">({{ item.productName }})</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <!-- 染色单（type=FINISHED 时显示） -->
        <el-col v-if="formData.type === 'FINISHED'" :span="12">
          <el-form-item label="关联染色单" prop="dyeingOrderId">
            <el-select
              v-model="formData.dyeingOrderId"
              placeholder="输入单号或产品搜索染色单（可选）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchDyeingOrders"
              :loading="dyeingLoading"
              class="!w-full"
              @change="handleDyeingOrderChange"
            >
              <el-option
                v-for="item in dyeingOptions"
                :key="item.id"
                :label="`染色单 #${item.id}${item.productName ? ' (' + item.productName + ')' : ''}`"
                :value="item.id!"
              >
                <span>染色单 #{{ item.id }}</span>
                <span v-if="item.productName" class="text-gray-400 ml-2">({{ item.productName }})</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <!-- 销售合同 -->
        <el-col :span="12">
          <el-form-item label="销售合同" prop="salesContractId">
            <el-select
              v-model="formData.salesContractId"
              placeholder="输入合同号搜索销售合同（可选）"
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
                <span v-if="item.clientName" class="text-gray-400 ml-2">({{ item.clientName }})</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 第三区：目标与备注 -->
      <div class="section-title">目标与备注</div>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="目标重量(KG)" prop="targetWeight">
            <el-input-number
              v-model="formData.targetWeight"
              :min="0"
              :precision="2"
              :step="10"
              class="!w-full"
              placeholder="目标重量 KG"
            />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="请输入备注说明"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import * as InspectionOrderApi from '@/api/lunaerp-elmui/production/inspection-order'
import type { InspectionOrderVO } from '@/api/lunaerp-elmui/production/inspection-order'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import * as DyeingOrderApi from '@/api/lunaerp-elmui/production/dyeing-order'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'

defineOptions({ name: 'InspectionOrderBasicInfoTab' })

const props = defineProps<{
  orderId?: number | null
  isViewMode?: boolean
}>()

const emit = defineEmits<{
  (e: 'loaded', order: InspectionOrderVO): void
  (e: 'saved', id: number): void
  (e: 'cancel'): void
}>()

const message = useMessage()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

const isCreateMode = computed(() => !props.orderId)
const isReadOnly = computed(() => {
  if (props.isViewMode) return true
  if (!isCreateMode.value && formData.status && formData.status !== 'DRAFT') {
    return true
  }
  return false
})

const formData = reactive<InspectionOrderVO>({
  id: undefined,
  code: '',
  type: 'GREIGE',
  status: 'DRAFT',
  productId: undefined as unknown as number,
  productCode: '',
  productName: '',
  greigeOrderId: undefined,
  dyeingOrderId: undefined,
  inspectionFactoryId: undefined,
  inspectionFactoryName: '',
  salesContractId: undefined,
  contractNo: '',
  targetWeight: undefined,
  remark: '',
  orderDate: new Date().toISOString().split('T')[0]
})

const rules: FormRules = {
  orderDate: [{ required: true, message: '请选择单据日期', trigger: 'change' }],
  type: [{ required: true, message: '请选择检验类型', trigger: 'change' }],
  productId: [{ required: true, message: '请选择关联产品', trigger: 'change' }]
}

// 选项与远程搜索状态
const productOptions = ref<ProductVO[]>([])
const productLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const greigeOptions = ref<GreigeOrderVO[]>([])
const greigeLoading = ref(false)
const dyeingOptions = ref<DyeingOrderVO[]>([])
const dyeingLoading = ref(false)
const contractOptions = ref<SalesContractVO[]>([])
const contractLoading = ref(false)

// 类型切换处理
const handleTypeChange = (val: string | number | boolean | undefined) => {
  if (val === 'GREIGE') {
    formData.dyeingOrderId = undefined
  } else if (val === 'FINISHED') {
    formData.greigeOrderId = undefined
  }
}

// 产品搜索与选择
const searchProducts = async (query: string) => {
  if (!query) {
    productOptions.value = []
    return
  }
  productLoading.value = true
  try {
    const res = await ProductApi.getProductPage({
      search: query.trim(),
      pageNo: 1,
      pageSize: 20
    })
    productOptions.value = res.list || []
  } finally {
    productLoading.value = false
  }
}

const handleProductChange = (productId?: number) => {
  if (!productId) {
    formData.productCode = ''
    formData.productName = ''
    return
  }
  const found = productOptions.value.find(p => p.id === productId)
  if (found) {
    formData.productCode = found.code
    formData.productName = found.name
  }
}

// 供应商搜索与选择
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      search: query ? query.trim() : '',
      pageNo: 1,
      pageSize: 20,
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list || []
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (supplierId?: number) => {
  if (!supplierId) {
    formData.inspectionFactoryName = ''
    return
  }
  const found = supplierOptions.value.find(s => s.id === supplierId)
  if (found) {
    formData.inspectionFactoryName = found.companyName
  }
}

// 胚布单搜索与选择
const searchGreigeOrders = async (query: string) => {
  if (!query) {
    greigeOptions.value = []
    return
  }
  greigeLoading.value = true
  try {
    const res = await GreigeOrderApi.getGreigeOrderPage({
      keyword: query.trim(),
      pageNo: 1,
      pageSize: 20
    })
    greigeOptions.value = res.list || []
  } finally {
    greigeLoading.value = false
  }
}

const handleGreigeOrderChange = (_orderId?: number) => {
  // 仅保存 ID，不自动带出产品（本期纯手工新建，不从上游单据自动带入）
}

// 染色单搜索与选择
const searchDyeingOrders = async (query: string) => {
  if (!query) {
    dyeingOptions.value = []
    return
  }
  dyeingLoading.value = true
  try {
    const res = await DyeingOrderApi.getDyeingOrderPage({
      keyword: query.trim(),
      pageNo: 1,
      pageSize: 20
    })
    dyeingOptions.value = res.list || []
  } finally {
    dyeingLoading.value = false
  }
}

const handleDyeingOrderChange = (_orderId?: number) => {
  // 仅保存 ID，由后端校验
}

// 销售合同搜索与选择
const searchContracts = async (query: string) => {
  if (!query) {
    contractOptions.value = []
    return
  }
  contractLoading.value = true
  try {
    const res = await SalesContractApi.getSalesContractPage({
      keyword: query.trim(),
      pageNo: 1,
      pageSize: 20
    })
    contractOptions.value = res.list || []
  } finally {
    contractLoading.value = false
  }
}

const handleContractChange = (contractId?: number) => {
  if (!contractId) {
    formData.contractNo = ''
    return
  }
  const found = contractOptions.value.find(c => c.id === contractId)
  if (found) {
    formData.contractNo = found.contractNo
  }
}

// 提交表单
const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (props.orderId) {
      await InspectionOrderApi.updateInspectionOrder({
        ...formData,
        id: props.orderId
      })
      message.success('修改成功')
      emit('saved', props.orderId)
    } else {
      const newId = await InspectionOrderApi.createInspectionOrder({
        ...formData,
        code: formData.code || undefined
      })
      message.success('创建成功')
      emit('saved', newId)
    }
  } finally {
    saving.value = false
  }
}

// 加载详情或预览单号
const init = async () => {
  if (props.orderId) {
    loading.value = true
    try {
      const data = await InspectionOrderApi.getInspectionOrder(props.orderId)
      Object.assign(formData, data)
      // 预填下拉选项以供回显
      if (data.productId) {
        productOptions.value = [{
          id: data.productId,
          code: data.productCode || '',
          name: data.productName || ''
        } as ProductVO]
      }
      if (data.inspectionFactoryId) {
        supplierOptions.value = [{
          id: data.inspectionFactoryId,
          companyName: data.inspectionFactoryName || ''
        } as SupplierVO]
      }
      if (data.greigeOrderId) {
        greigeOptions.value = [{
          id: data.greigeOrderId,
          orderNo: `胚布单 #${data.greigeOrderId}`
        } as unknown as GreigeOrderVO]
      }
      if (data.dyeingOrderId) {
        dyeingOptions.value = [{
          id: data.dyeingOrderId,
          contractNo: `染色单 #${data.dyeingOrderId}`
        } as unknown as DyeingOrderVO]
      }
      if (data.salesContractId) {
        contractOptions.value = [{
          id: data.salesContractId,
          contractNo: data.contractNo || `合同 #${data.salesContractId}`
        } as SalesContractVO]
      }
      emit('loaded', data)
    } finally {
      loading.value = false
    }
  } else {
    // 新建态：预览单号
    try {
      const nextNo = await InspectionOrderApi.getNextInspectionOrderNo()
      if (nextNo && !formData.code) {
        formData.code = nextNo
      }
    } catch {}
  }
}

watch(
  () => props.orderId,
  (newId) => {
    if (newId) {
      init()
    }
  }
)

defineExpose({
  submit,
  formData,
  init
})

onMounted(() => {
  init()
})
</script>

<style lang="scss" scoped>
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-left: 4px solid var(--el-color-primary);
  padding-left: 8px;
  margin: 16px 0 14px;
}
.inspection-order-form {
  padding: 0 8px;
}
</style>
