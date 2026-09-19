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
                :label="item.companyName"
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
            >
              <template v-if="isCreateMode" #append>
                <el-button :loading="nextNoLoading" @click="handleGetNextNo">获取编号</el-button>
              </template>
            </el-input>
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

      <div class="section-title">商品明细</div>
      <el-table :data="formData.items" border size="small" class="items-table" empty-text="暂无明细，可点击下方按钮添加（零明细合同也可保存）">
        <el-table-column label="行号" type="index" width="50" align="center" />
        <el-table-column label="产品编号" width="170">
          <template #default="{ row }">
            <el-select
              v-model="row.productCode"
              placeholder="搜编号/品名或手填"
              filterable
              remote
              allow-create
              default-first-option
              reserve-keyword
              :remote-method="(query: string) => searchProducts(row, query)"
              :loading="!!productLoadingMap.get(row)"
              :disabled="isViewMode"
              class="!w-full"
              @change="(val: string) => handleProductChange(row, val)"
            >
              <el-option
                v-for="product in productOptionsMap.get(row) || []"
                :key="product.id"
                :label="product.code"
                :value="product.code"
              >
                <span>{{ product.code }}</span>
                <span class="product-option-name">{{ product.name }}</span>
              </el-option>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="品名" width="140">
          <template #default="{ row }">
            <el-input v-model="row.productName" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column label="客户色号" width="110">
          <template #default="{ row }">
            <el-input v-model="row.customerColor" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column label="成分" width="120">
          <template #default="{ row }">
            <el-input v-model="row.composition" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column label="门幅" width="90">
          <template #default="{ row }">
            <el-input v-model="row.width" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column label="克重" width="90">
          <template #default="{ row }">
            <el-input v-model="row.weight" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :disabled="isViewMode"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.unitPrice"
              :disabled="isViewMode"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </template>
        </el-table-column>
        <el-table-column label="其他费用" width="110">
          <template #default="{ row }">
            <el-input-number
              v-model="row.otherFees"
              :disabled="isViewMode"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </template>
        </el-table-column>
        <el-table-column label="行金额" width="120" align="right">
          <template #default="{ row }">
            <span class="row-amount">{{ formatMoney(rowAmount(row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="行备注" width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" :disabled="isViewMode" />
          </template>
        </el-table-column>
        <el-table-column v-if="!isViewMode" label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!isViewMode" class="items-actions">
        <el-button type="primary" plain @click="addItem">添加明细行</el-button>
      </div>
      <div class="items-summary">
        合计：总数量 <b>{{ formatMoney(totalQuantity) }}</b>
        ／ 总金额 <b>{{ formatMoney(totalAmount) }}</b>
        <span class="summary-tip">（前端实时预览，提交后以服务端重算为准）</span>
      </div>

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
import type { SalesContractVO, SalesContractItemVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import * as CustomerContactApi from '@/api/lunaerp-elmui/baseinfo/customer/contact'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'

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
  status: 'ACTIVE',
  items: [] as SalesContractItemVO[]
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

// ==================== 商品明细（聚合保存，ADR-0003） ====================

/** 新增一行空明细 */
const addItem = () => {
  formData.items!.push({
    productCode: '',
    productName: '',
    customerColor: '',
    composition: '',
    width: '',
    weight: '',
    quantity: 0,
    unitPrice: 0,
    otherFees: 0,
    amount: 0,
    remark: ''
  })
}

/** 删除指定行（同步清理该行的远程候选状态，避免 Map 残留） */
const removeItem = (index: number) => {
  const [removed] = formData.items!.splice(index, 1)
  if (removed) {
    productOptionsMap.delete(removed)
    productLoadingMap.delete(removed)
  }
}

/** null/undefined 安全转数字 */
const toNumber = (val: number | undefined | null): number => (val == null ? 0 : Number(val))

/** 行金额 = 数量 × 单价 + 其他费用（前端实时预览，提交以服务端重算为准） */
const rowAmount = (row: SalesContractItemVO): number =>
  toNumber(row.quantity) * toNumber(row.unitPrice) + toNumber(row.otherFees)

/** 总数量预览 */
const totalQuantity = computed(() =>
  (formData.items ?? []).reduce((sum, row) => sum + toNumber(row.quantity), 0)
)

/** 总金额预览 */
const totalAmount = computed(() =>
  (formData.items ?? []).reduce((sum, row) => sum + rowAmount(row), 0)
)

/** 金额格式化（两位小数） */
const formatMoney = (val: number): string => val.toFixed(2)

// ==================== 获取编号（只读预览、不占号） ====================

const nextNoLoading = ref(false)

const handleGetNextNo = async () => {
  // 用户已手工输入（含去空格）时不自动覆盖
  if (formData.contractNo && formData.contractNo.trim()) return
  nextNoLoading.value = true
  try {
    const no = await SalesContractApi.getNextSalesContractNo()
    // 请求返回后再次判空，避免与手工输入竞态
    if (!formData.contractNo || !formData.contractNo.trim()) {
      formData.contractNo = no
    }
  } finally {
    nextNoLoading.value = false
  }
}

// 客户远程搜索：选中后保存 clientName 快照，并远程带出联系人/地址（两字段仍可手工修改）
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

/** 选中客户：① 保存 clientName 快照；② 远程带出主联系人姓名与客户地址；单个接口失败不影响另一个回填 */
const handleCustomerChange = async (customerId: number) => {
  const customer = customerOptions.value.find((item) => item.id === customerId)
  formData.clientName = customer?.companyName || ''
  // allSettled：联系人/地址任一接口失败，另一个仍照常回填，整体失败不阻断选择
  const [detailResult, contactsResult] = await Promise.allSettled([
    CustomerApi.getCustomer(customerId),
    // 联系人分页按 isPrimary DESC 排序，取主联系人（兜底取第一个）
    CustomerContactApi.getCustomerContactPage({ customerId, pageNo: 1, pageSize: 10 })
  ])
  if (contactsResult.status === 'fulfilled') {
    const contacts = contactsResult.value.list || []
    const primary = contacts.find((c) => c.isPrimary === 1) || contacts[0]
    formData.clientContact = primary?.name || ''
  }
  if (detailResult.status === 'fulfilled') {
    formData.clientAddress = detailResult.value.address || ''
  }
}

// ==================== 明细产品远程搜索（产品库回填 + 定制品种手填） ====================

/**
 * 每行独立的产品候选/加载状态，以行对象为 key（行在 reactive 数组中引用稳定）。
 * 候选仅用于远程下拉，不落库、不参与提交。
 */
const productOptionsMap = reactive(new Map<SalesContractItemVO, ProductVO[]>())
const productLoadingMap = reactive(new Map<SalesContractItemVO, boolean>())

const searchProducts = async (row: SalesContractItemVO, query: string) => {
  productLoadingMap.set(row, true)
  try {
    // 产品分页 search 对编号/品名等分词 AND 匹配；不传 status 默认查 ACTIVE
    const data = await ProductApi.getProductPage({ pageNo: 1, pageSize: 20, search: query || '' })
    productOptionsMap.set(row, data.list || [])
  } finally {
    productLoadingMap.set(row, false)
  }
}

/**
 * 选中产品后回填品名/成分/门幅/克重，并清空单价（产品库无销售单价字段——
 * 按"缺失字段留空"处理，避免切换产品时残留上一产品的单价；缺失的其他来源字段同样留空）。
 * 选中值不在候选中即为手填定制品种：不回填、不做存在性校验。
 */
const handleProductChange = (row: SalesContractItemVO, val: string) => {
  const product = (productOptionsMap.get(row) || []).find((p) => p.code === val)
  if (!product) return
  row.productName = product.name || ''
  row.composition = product.composition || ''
  row.width = product.widthCm != null ? String(product.widthCm) : ''
  row.weight = product.weightGsm != null ? String(product.weightGsm) : ''
  row.unitPrice = undefined
}

/** 加载合同（编辑/查看模式） */
const loadContract = async () => {
  if (!contractId.value) return
  loading.value = true
  try {
    const data = await SalesContractApi.getSalesContract(Number(contractId.value))
    Object.assign(formData, data)
    // 明细统一为数组（零明细合同接口可能返回 null/undefined）
    formData.items = data.items || []
    // 用快照预置当前客户下拉选项，保证选中态有 label
    if (data.customerId) {
      customerOptions.value = [{ id: data.customerId, companyName: data.clientName || '' } as CustomerVO]
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

.items-table {
  margin-bottom: 8px;
}

.product-option-name {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.items-actions {
  margin-bottom: 8px;
}

.items-summary {
  padding: 8px 12px;
  margin-bottom: 4px;
  text-align: right;
  font-size: 14px;
  background: var(--el-fill-color-light);
  border-radius: 4px;

  b {
    color: var(--el-color-primary);
    margin: 0 4px;
  }

  .summary-tip {
    margin-left: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.row-amount {
  font-weight: 600;
  color: var(--el-color-primary);
}

.form-footer {
  margin-top: 12px;
  padding-left: 100px;
}
</style>
