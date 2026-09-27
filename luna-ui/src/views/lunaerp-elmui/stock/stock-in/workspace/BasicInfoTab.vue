<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="120px"
    class="max-w-1000px py-16px"
    :disabled="isReadOnly"
  >
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="入库单号" prop="inNo">
          <el-input
            v-model="formData.inNo"
            :placeholder="isCreate ? (previewNo ? `留空自动生成：${previewNo}` : '留空自动生成：IN-YYYYMMDD-NNN') : '请输入入库单号'"
            clearable
            :disabled="isReadOnly || !isCreate"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="单据日期" prop="inDate">
          <el-date-picker
            v-model="formData.inDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择单据日期"
            class="!w-full"
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="入库类型" prop="inType">
          <el-radio-group v-model="formData.inType" :disabled="isReadOnly">
            <el-radio-button
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_STOCK_IN_TYPE)"
              :key="dict.value"
              :label="dict.value"
            >
              {{ dict.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select
            v-model="formData.warehouseId"
            placeholder="请选择入库仓库"
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleWarehouseChange"
          >
            <el-option
              v-for="item in warehouseOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="formData.supplierId"
            placeholder="搜索供应商（可选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleSupplierChange"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="关联销售合同" prop="salesContractId">
          <el-select
            v-model="formData.salesContractId"
            placeholder="搜索销售合同（可选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchContracts"
            :loading="contractLoading"
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleContractChange"
          >
            <el-option
              v-for="item in contractOptions"
              :key="item.id"
              :label="item.contractNo ? `${item.contractNo} (${item.clientName || '无客户'})` : String(item.id)"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="关联胚布单" prop="greigeOrderId">
          <el-select
            v-model="formData.greigeOrderId"
            placeholder="搜索胚布单（可选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchGreigeOrders"
            :loading="greigeOrderLoading"
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleGreigeOrderChange"
          >
            <el-option
              v-for="item in greigeOrderOptions"
              :key="item.id"
              :label="item.orderNo ? `${item.orderNo} (${item.greigeFactoryName || '无承织厂'})` : String(item.id)"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="关联染色单" prop="dyeingOrderId">
          <el-select
            v-model="formData.dyeingOrderId"
            placeholder="搜索染色单（可选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchDyeingOrders"
            :loading="dyeingOrderLoading"
            class="!w-full"
            :disabled="isReadOnly"
          >
            <el-option
              v-for="item in dyeingOrderOptions"
              :key="item.id"
              :label="`染色单 #${item.id} (${item.contractNo || item.dyeingFactoryName || '—'})`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="关联胚布品种" prop="greigeTypeId">
          <el-select
            v-model="formData.greigeTypeId"
            placeholder="选择胚布品种（可选）"
            clearable
            filterable
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleGreigeTypeChange"
          >
            <el-option
              v-for="item in greigeTypeOptions"
              :key="item.id"
              :label="`${item.name} (${item.code})`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="金额合计">
          <el-input
            :model-value="formData.totalAmount != null ? `¥ ${Number(formData.totalAmount).toFixed(2)}` : '—'"
            disabled
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row v-if="formData.inspectionOrderNo || formData.inspectionOrderId" :gutter="20">
      <el-col :span="12">
        <el-form-item label="关联检验单">
          <el-input
            :model-value="formData.inspectionOrderNo ? `${formData.inspectionOrderNo}` : `检验单 #${formData.inspectionOrderId}`"
            disabled
          >
            <template #append v-if="formData.inspectionOrderId">
              <el-button @click="handleNavigateToInspection">查看检验单</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="备注" prop="remark">
      <el-input
        v-model="formData.remark"
        type="textarea"
        :rows="3"
        placeholder="请输入备注"
        maxlength="500"
        show-word-limit
        :disabled="isReadOnly"
      />
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { StockInVO } from '@/api/lunaerp-elmui/stock/stock-in'
import * as WarehouseApi from '@/api/lunaerp-elmui/stock/warehouse'
import type { WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import * as DyeingOrderApi from '@/api/lunaerp-elmui/production/dyeing-order'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import * as GreigeTypeApi from '@/api/lunaerp/baseinfo/greige-type'
import type { GreigeTypeSimpleVO } from '@/api/lunaerp/baseinfo/greige-type'

defineOptions({ name: 'StockInBasicInfoTab' })

const props = defineProps<{
  modelValue: StockInVO
  isReadOnly: boolean
  isCreate: boolean
  previewNo?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: StockInVO): void
  (e: 'warehouseChange', warehouseId: number): void
}>()

const formRef = ref<FormInstance>()
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const rules: FormRules = {
  inType: [{ required: true, message: '请选择入库类型', trigger: 'change' }],
  inDate: [{ required: true, message: '请选择单据日期', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择入库仓库', trigger: 'change' }]
}

// 仓库列表
const warehouseOptions = ref<WarehouseVO[]>([])
const loadWarehouses = async () => {
  try {
    warehouseOptions.value = await WarehouseApi.getWarehouseListAll('ACTIVE')
  } catch {}
}

const handleWarehouseChange = (val: number) => {
  const target = warehouseOptions.value.find((w) => w.id === val)
  formData.value.warehouseName = target?.name
  emit('warehouseChange', val)
}

// 供应商远程
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}
const handleSupplierChange = (val: number) => {
  const target = supplierOptions.value.find((s) => s.id === val)
  formData.value.supplierName = target?.companyName
}

// 合同远程
const contractOptions = ref<SalesContractVO[]>([])
const contractLoading = ref(false)
const searchContracts = async (query: string) => {
  contractLoading.value = true
  try {
    const res = await SalesContractApi.getSalesContractPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    contractOptions.value = res.list
  } finally {
    contractLoading.value = false
  }
}
const handleContractChange = (val: number) => {
  const target = contractOptions.value.find((c) => c.id === val)
  formData.value.contractNo = target?.contractNo
}

// 胚布单远程
const greigeOrderOptions = ref<GreigeOrderVO[]>([])
const greigeOrderLoading = ref(false)
const searchGreigeOrders = async (query: string) => {
  greigeOrderLoading.value = true
  try {
    const res = await GreigeOrderApi.getGreigeOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    greigeOrderOptions.value = res.list
  } finally {
    greigeOrderLoading.value = false
  }
}
const handleGreigeOrderChange = (val: number) => {
  const target = greigeOrderOptions.value.find((g) => g.id === val)
  formData.value.greigeOrderNo = target?.orderNo
}

// 染色单远程
const dyeingOrderOptions = ref<DyeingOrderVO[]>([])
const dyeingOrderLoading = ref(false)
const searchDyeingOrders = async (query: string) => {
  dyeingOrderLoading.value = true
  try {
    const res = await DyeingOrderApi.getDyeingOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    dyeingOrderOptions.value = res.list
  } finally {
    dyeingOrderLoading.value = false
  }
}

// 胚布品种
const greigeTypeOptions = ref<GreigeTypeSimpleVO[]>([])
const loadGreigeTypes = async () => {
  try {
    greigeTypeOptions.value = await GreigeTypeApi.getSimpleGreigeTypeList()
  } catch {}
}
const handleGreigeTypeChange = (val: number) => {
  const target = greigeTypeOptions.value.find((g) => g.id === val)
  formData.value.greigeTypeName = target?.name
}

// 预填充已选中项，避免远程搜索前不展示名称
const ensureInitialOptions = () => {
  if (formData.value.supplierId && formData.value.supplierName) {
    supplierOptions.value = [
      { id: formData.value.supplierId, companyName: formData.value.supplierName } as any
    ]
  }
  if (formData.value.salesContractId && formData.value.contractNo) {
    contractOptions.value = [
      { id: formData.value.salesContractId, contractNo: formData.value.contractNo } as any
    ]
  }
  if (formData.value.greigeOrderId && formData.value.greigeOrderNo) {
    greigeOrderOptions.value = [
      { id: formData.value.greigeOrderId, orderNo: formData.value.greigeOrderNo } as any
    ]
  }
}

watch(
  () => [props.modelValue.supplierId, props.modelValue.salesContractId, props.modelValue.greigeOrderId],
  () => ensureInitialOptions(),
  { immediate: true }
)

const validate = async () => {
  if (!formRef.value) return false
  return await formRef.value.validate()
}

defineExpose({ validate })

const router = useRouter()
const handleNavigateToInspection = () => {
  if (formData.value.inspectionOrderId) {
    router.push(`/production/inspection-order/workspace/${formData.value.inspectionOrderId}?mode=view`)
  }
}

onMounted(() => {
  loadWarehouses()
  loadGreigeTypes()
})
</script>
