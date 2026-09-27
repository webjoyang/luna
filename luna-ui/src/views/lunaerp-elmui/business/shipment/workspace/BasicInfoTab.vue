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
        <el-form-item label="发货单号" prop="shipmentNo">
          <el-input
            v-model="formData.shipmentNo"
            :placeholder="isCreate ? (previewNo ? `留空自动生成：${previewNo}` : '留空自动生成：SH-YYYYMMDD-NNN') : '请输入发货单号'"
            clearable
            :disabled="isReadOnly || (!isCreate && formData.status !== 'DRAFT')"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="发货日期" prop="shippingDate">
          <el-date-picker
            v-model="formData.shippingDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择发货日期"
            class="!w-full"
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="formData.customerId"
            placeholder="搜索客户（必填）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleCustomerChange"
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
      <el-col :span="12">
        <el-form-item label="发货仓库" prop="warehouseId">
          <el-select
            v-model="formData.warehouseId"
            placeholder="请选择发货仓库（必填）"
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
        <el-form-item label="销售合同" prop="salesContractId">
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
              :label="item.contractNo"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="业务员" prop="salespersonUserId">
          <el-select
            v-model="formData.salespersonUserId"
            placeholder="选择业务员（可选）"
            clearable
            filterable
            class="!w-full"
            :disabled="isReadOnly"
            @change="handleSalespersonChange"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.nickname"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="发货地址" prop="shippingAddress">
          <el-input
            v-model="formData.shippingAddress"
            placeholder="请输入发货地址"
            clearable
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="收货地址" prop="receiveAddress">
          <el-input
            v-model="formData.receiveAddress"
            placeholder="请输入收货地址"
            clearable
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="承运商/物流" prop="carrier">
          <el-input
            v-model="formData.carrier"
            placeholder="请输入承运商/物流公司"
            clearable
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="物流单号" prop="trackingNumber">
          <el-input
            v-model="formData.trackingNumber"
            placeholder="请输入物流运单号"
            clearable
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="预计到货" prop="expectedDeliveryDate">
          <el-date-picker
            v-model="formData.expectedDeliveryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择预计到货日期"
            class="!w-full"
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item label="运费" prop="freightFee">
          <el-input-number
            v-model="formData.freightFee"
            :min="0"
            :precision="2"
            :step="10"
            controls-position="right"
            class="!w-full"
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
      <el-col :span="6">
        <el-form-item label="币种" prop="currency">
          <el-select
            v-model="formData.currency"
            placeholder="币种"
            class="!w-full"
            :disabled="isReadOnly"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_CURRENCY)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="发货备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入发货备注说明"
            maxlength="1000"
            show-word-limit
            :disabled="isReadOnly"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 签收登记信息区（仅已发货单可修改，草稿与已取消只读） -->
    <el-divider content-position="left">签收信息</el-divider>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="签收状态" prop="signStatus">
          <el-select
            v-model="formData.signStatus"
            placeholder="签收状态"
            class="!w-full"
            disabled
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SHIPMENT_SIGN_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="签收人" prop="signedByName">
          <el-input
            v-model="formData.signedByName"
            placeholder="签收人姓名"
            disabled
          />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="签收时间" prop="signedAt">
          <el-date-picker
            v-model="formData.signedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="签收时间"
            class="!w-full"
            disabled
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="签收备注" prop="signRemark">
          <el-input
            v-model="formData.signRemark"
            placeholder="签收备注说明"
            disabled
          />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type { ShipmentVO } from '@/api/lunaerp-elmui/business/shipment'
import * as WarehouseApi from '@/api/lunaerp-elmui/stock/warehouse'
import type { WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as UserApi from '@/api/system/user'
import type { UserVO } from '@/api/system/user'

const props = defineProps<{
  modelValue: ShipmentVO
  isReadOnly: boolean
  isCreate: boolean
  previewNo: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: ShipmentVO): void
  (e: 'warehouseChange', warehouseId: number): void
}>()

const formRef = ref<FormInstance>()
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const rules = reactive<FormRules>({
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择发货仓库', trigger: 'change' }],
  shippingDate: [{ required: true, message: '请选择发货日期', trigger: 'change' }]
})

// 仓库下拉选项
const warehouseOptions = ref<WarehouseVO[]>([])
const loadWarehouses = async () => {
  try {
    warehouseOptions.value = await WarehouseApi.getWarehouseListAll('ACTIVE')
  } catch {}
}

const handleWarehouseChange = (warehouseId: number) => {
  const selected = warehouseOptions.value.find((w) => w.id === warehouseId)
  if (selected) {
    formData.value.warehouseName = selected.name
  }
  emit('warehouseChange', warehouseId)
}

// 客户远程搜索
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
    customerOptions.value = res.list
    if (formData.value.customerId && !customerOptions.value.some((c) => c.id === formData.value.customerId)) {
      if (formData.value.customerName) {
        customerOptions.value.unshift({
          id: formData.value.customerId,
          companyName: formData.value.customerName,
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
    formData.value.customerName = selected.companyName
    if (!formData.value.receiveAddress && selected.address) {
      formData.value.receiveAddress = selected.address
    }
  }
}

// 合同远程搜索
const contractOptions = ref<SalesContractVO[]>([])
const contractLoading = ref(false)
const searchContracts = async (query: string) => {
  contractLoading.value = true
  try {
    const res = await SalesContractApi.getSalesContractPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || '',
      customerId: formData.value.customerId
    })
    contractOptions.value = res.list
    if (formData.value.salesContractId && !contractOptions.value.some((c) => c.id === formData.value.salesContractId)) {
      if (formData.value.contractNo) {
        contractOptions.value.unshift({
          id: formData.value.salesContractId,
          contractNo: formData.value.contractNo,
          customerId: formData.value.customerId || 0,
          clientName: formData.value.customerName || '',
          signDate: '',
          status: ''
        } as SalesContractVO)
      }
    }
  } finally {
    contractLoading.value = false
  }
}

const handleContractChange = (contractId: number) => {
  const selected = contractOptions.value.find((c) => c.id === contractId)
  if (selected) {
    formData.value.contractNo = selected.contractNo
  } else {
    formData.value.contractNo = undefined
  }
}

// 业务员用户列表
const userOptions = ref<UserVO[]>([])
const loadUsers = async () => {
  try {
    userOptions.value = await UserApi.getSimpleUserList()
  } catch {}
}

const handleSalespersonChange = (userId: number) => {
  const selected = userOptions.value.find((u) => u.id === userId)
  if (selected) {
    formData.value.salespersonName = selected.nickname
  } else {
    formData.value.salespersonName = undefined
  }
}

const validate = async () => {
  return await formRef.value?.validate()
}

defineExpose({ validate })

onMounted(() => {
  loadWarehouses()
  loadUsers()
  searchCustomers('')
  searchContracts('')
})
</script>
