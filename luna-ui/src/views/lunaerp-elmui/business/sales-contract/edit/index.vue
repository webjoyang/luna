<template>
  <ContentWrap>
    <div class="edit-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
          <span v-if="formData.contractNo" class="header-sub">· {{ formData.contractNo }}</span>
        </template>
      </el-page-header>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      class="contract-form"
      v-loading="loading"
    >
      <div class="section-title">基本信息</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户" prop="customerId">
            <el-select
              v-model="formData.customerId"
              placeholder="输入关键词搜索客户"
              filterable
              remote
              reserve-keyword
              :remote-method="searchCustomers"
              :loading="customerLoading"
              :disabled="isViewMode"
              class="!w-full"
              @change="handleCustomerChange"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.id"
                :label="item.company"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="业务员" prop="salespersonUserId">
            <UserSelect
              v-model="formData.salespersonUserId"
              :disabled="isViewMode"
              placeholder="请选择业务员（可空）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="合同编号" prop="contractNo">
            <el-input
              v-model="formData.contractNo"
              :disabled="isViewMode"
              maxlength="32"
              placeholder="留空自动生成（如 CH20260001）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户合同号" prop="clientContractNo">
            <el-input v-model="formData.clientContractNo" :disabled="isViewMode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="签订日期" prop="signDate">
            <el-date-picker
              v-model="formData.signDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择签订日期"
              :disabled="isViewMode"
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
              :disabled="isViewMode"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="贸易类型" prop="tradeType">
            <el-select v-model="formData.tradeType" :disabled="isViewMode" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_TRADE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="订单属性" prop="orderType">
            <el-select v-model="formData.orderType" :disabled="isViewMode" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_ORDER_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位" prop="unit">
            <el-select v-model="formData.unit" :disabled="isViewMode" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_UNIT)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="币种" prop="currency">
            <el-select v-model="formData.currency" :disabled="isViewMode" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_CURRENCY)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="formData.status" :disabled="isViewMode" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人" prop="clientContact">
            <el-input v-model="formData.clientContact" :disabled="isViewMode" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="客户地址" prop="clientAddress">
            <el-input v-model="formData.clientAddress" :disabled="isViewMode" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remarks">
            <el-input
              v-model="formData.remarks"
              type="textarea"
              :rows="3"
              maxlength="1000"
              :disabled="isViewMode"
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <div v-if="!isViewMode" class="form-footer">
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="
            isCreateMode
              ? ['lunaerp:business:sales-contract:create']
              : ['lunaerp:business:sales-contract:update']
          "
        >
          保存
        </el-button>
        <el-button @click="handleBack">返回</el-button>
      </div>
    </el-form>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpElmuiBusinessSalesContractEdit' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const formData = reactive<SalesContractVO>({
  contractNo: '',
  customerId: undefined as unknown as number,
  clientName: '',
  clientContact: '',
  clientAddress: '',
  clientContractNo: '',
  salespersonUserId: undefined,
  signDate: formatDate(new Date(), 'YYYY-MM-DD'),
  deliveryDate: undefined,
  unit: 'KG',
  currency: 'RMB',
  tradeType: 'DOMESTIC',
  orderType: 'BULK',
  remarks: '',
  status: 'ACTIVE'
})

const rules = {
  customerId: [{ required: true, message: '客户不能为空', trigger: 'change' }]
}

const contractId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !contractId.value)
/** 查看模式：列表"查看"以同一页只读打开（?mode=view），全部控件 disabled、隐藏保存 */
const isViewMode = computed(() => route.query.mode === 'view')

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建销售合同'
  return isViewMode.value ? '查看销售合同' : '编辑销售合同'
})

// 客户远程搜索：只要求能搜索选中并保存 customerId + clientName 快照
// （选中后自动带出联系人/地址的回填增强属于后续票，本票不做）
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const data = await CustomerApi.getCustomerPage({ pageNo: 1, pageSize: 20, search: query || '' })
    customerOptions.value = data.list
  } finally {
    customerLoading.value = false
  }
}

/** 选中客户：保存 clientName 快照 */
const handleCustomerChange = (customerId: number) => {
  const customer = customerOptions.value.find((item) => item.id === customerId)
  formData.clientName = customer?.company || ''
}

/** 加载合同（编辑/查看模式） */
const loadContract = async () => {
  if (!contractId.value) return
  loading.value = true
  try {
    const data = await SalesContractApi.getSalesContract(Number(contractId.value))
    Object.assign(formData, data)
    // 用快照预置当前客户下拉选项，保证选中态有 label
    if (data.customerId) {
      customerOptions.value = [{ id: data.customerId, company: data.clientName || '' } as CustomerVO]
    }
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isCreateMode.value) {
      await SalesContractApi.createSalesContract(formData)
      message.success('保存成功')
    } else {
      await SalesContractApi.updateSalesContract(formData)
      message.success('保存成功')
    }
    router.push('/business/sales-contract')
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push('/business/sales-contract')
}

onMounted(() => {
  loadContract()
})
</script>

<style lang="scss" scoped>
.edit-header {
  margin-bottom: 12px;

  .header-title {
    font-size: 16px;
    font-weight: 600;
  }

  .header-sub {
    margin-left: 6px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.section-title {
  margin-bottom: 12px;
  padding-left: 8px;
  font-weight: 600;
  border-left: 3px solid var(--el-color-primary);
}

.contract-form {
  max-width: 1100px;
  padding: 4px 8px;
}

.form-footer {
  margin-top: 12px;
  padding-left: 100px;
}
</style>
