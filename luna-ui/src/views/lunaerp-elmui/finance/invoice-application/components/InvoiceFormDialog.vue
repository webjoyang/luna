<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑发票申请' : '新建发票申请'"
    width="960px"
    destroy-on-close
    append-to-body
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="submitting"
    >
      <!-- 基础要素 -->
      <div class="grid grid-cols-2 gap-x-4">
        <el-form-item label="开票客户" prop="customerId">
          <el-select
            v-model="formData.customerId"
            :disabled="isEdit"
            placeholder="搜索并选择客户"
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
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

        <el-form-item label="开票币种" prop="currency">
          <el-select
            v-model="formData.currency"
            :disabled="isEdit"
            placeholder="请选择币种"
            class="!w-full"
            @change="handleCurrencyChange"
          >
            <el-option label="人民币 (RMB)" value="RMB" />
            <el-option label="美元 (USD)" value="USD" />
            <el-option label="欧元 (EUR)" value="EUR" />
            <el-option label="港币 (HKD)" value="HKD" />
          </el-select>
        </el-form-item>

        <el-form-item label="开票税率" prop="taxRate">
          <el-select
            v-model="formData.taxRate"
            placeholder="请选择税率"
            class="!w-full"
            @change="handleTaxRateChange"
          >
            <el-option
              v-for="item in TAX_RATE_OPTIONS"
              :key="item.label"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="发票种类" prop="invoiceKind">
          <el-select v-model="formData.invoiceKind" placeholder="请选择发票种类" class="!w-full">
            <el-option
              v-for="item in INVOICE_KIND_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="介质类型" prop="electronic">
          <el-radio-group v-model="formData.electronic">
            <el-radio :value="1">数电 / 电子发票</el-radio>
            <el-radio :value="0">传统纸质发票</el-radio>
          </el-radio-group>
        </el-form-item>
      </div>

      <!-- 抬头四要素快照（创建时从客户档案自动带出，支持手工修改） -->
      <el-card shadow="never" class="!border-gray-200 !bg-gray-50/50 mb-4">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-bold text-14px text-gray-700"
              >开票四要素（从客户档案带出，单上可改）</span
            >
            <el-button
              link
              type="primary"
              size="small"
              @click="resetToCustomerDefaults"
              :disabled="!formData.customerId"
            >
              重置为档案默认
            </el-button>
          </div>
        </template>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="开票抬头" prop="invoiceTitle">
            <el-input v-model="formData.invoiceTitle" placeholder="请输入开票抬头" />
          </el-form-item>

          <el-form-item label="税号/信用代码" prop="taxNo">
            <el-input v-model="formData.taxNo" placeholder="请输入统一社会信用代码" />
          </el-form-item>

          <el-form-item label="开票地址电话" prop="address">
            <el-input v-model="formData.address" placeholder="请输入开票地址及电话" />
          </el-form-item>

          <el-form-item label="开户行及账号" prop="bankInfo">
            <el-input v-model="formData.bankInfo" placeholder="请输入开户银行及银行账号" />
          </el-form-item>
        </div>
      </el-card>

      <!-- 发货单明细选择与部分开票金额 -->
      <div class="mb-3 flex items-center justify-between">
        <div class="font-bold text-14px text-gray-700">
          申请开票发货单明细 ({{ formData.items.length }})
        </div>
        <el-button
          type="primary"
          plain
          size="small"
          :icon="Plus"
          :disabled="!formData.customerId"
          @click="openShipmentSelect"
        >
          选择发货单
        </el-button>
      </div>

      <el-table :data="formData.items" border size="small" class="mb-3">
        <el-table-column type="index" label="#" width="45" align="center" />
        <el-table-column prop="shipmentNo" label="发货单号" min-width="150" />
        <el-table-column prop="shipmentAmount" label="发货单总额" min-width="110" align="right">
          <template #default="{ row }">
            {{ formatMoney(row.shipmentAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="availableAmount" label="可开票余额" min-width="110" align="right">
          <template #default="{ row }">
            <span class="text-success font-semibold">{{ formatMoney(row.availableAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本次申请开票金额 *" min-width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.amount"
              :min="0.01"
              :max="row.availableAmount || 99999999"
              :precision="2"
              :step="100"
              class="!w-full"
              @change="recalculateSummary"
            />
          </template>
        </el-table-column>
        <el-table-column label="行备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注" clearable />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeItem($index)">
              移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 实时税额反算汇总条 -->
      <div
        class="p-3 bg-blue-50/60 rounded border border-blue-100 flex items-center justify-between text-13px mb-3"
      >
        <div class="space-x-4">
          <span
            >价税合计总金额：<strong class="text-danger text-15px"
              >{{ formData.currency }} {{ formatMoney(summary.totalAmountInclTax) }}</strong
            ></span
          >
          <span
            >不含税金额：<strong class="text-primary">{{
              formatMoney(summary.totalAmountExclTax)
            }}</strong></span
          >
          <span
            >税额：<strong class="text-success">{{ formatMoney(summary.totalTax) }}</strong></span
          >
        </div>
        <div class="text-gray-400 text-12px">
          按 {{ formData.taxRate }}% 实时反算（不含税 + 税额 == 价税合计）
        </div>
      </div>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入发票申请备注信息"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="['lunaerp:finance:invoice:create', 'lunaerp:finance:invoice:update']"
        >
          {{ isEdit ? '保存修改' : '确认创建草稿' }}
        </el-button>
      </div>
    </template>

    <!-- 选择发货单弹窗 -->
    <ShipmentSelectDialog ref="shipmentSelectDialogRef" @confirm="handleShipmentsSelected" />
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createInvoiceApplication,
  updateInvoiceApplication,
  getInvoiceApplication,
  type InvoiceCandidateShipmentRespVO
} from '@/api/lunaerp-elmui/finance/invoice'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import { TAX_RATE_OPTIONS, INVOICE_KIND_OPTIONS, calculateTax, formatMoney } from '../model/invoice'
import ShipmentSelectDialog from './ShipmentSelectDialog.vue'

interface FormItemRow {
  shipmentId: number
  shipmentNo: string
  shipmentAmount: number
  availableAmount: number
  amount: number
  remark?: string
}

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const isEdit = ref(false)
const editId = ref<number>()
const submitting = ref(false)
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])
const selectedCustomer = ref<CustomerVO>()
const formRef = ref<FormInstance>()
const shipmentSelectDialogRef = ref<InstanceType<typeof ShipmentSelectDialog>>()

const formData = reactive<{
  customerId?: number
  currency: string
  taxRate: number
  invoiceKind: string
  electronic: number
  invoiceTitle?: string
  taxNo?: string
  address?: string
  bankInfo?: string
  remark?: string
  items: FormItemRow[]
}>({
  customerId: undefined,
  currency: 'RMB',
  taxRate: 13.0,
  invoiceKind: 'DIGITAL_SPECIAL',
  electronic: 1,
  invoiceTitle: '',
  taxNo: '',
  address: '',
  bankInfo: '',
  remark: '',
  items: []
})

const summary = computed(() => {
  const totalIncl = formData.items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
  return calculateTax(totalIncl, formData.taxRate)
})

const formRules = reactive<FormRules>({
  customerId: [{ required: true, message: '请选择开票客户', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }],
  taxRate: [{ required: true, message: '请选择开票税率', trigger: 'change' }],
  invoiceKind: [{ required: true, message: '请选择发票种类', trigger: 'change' }]
})

const open = async (id?: number) => {
  visible.value = true
  isEdit.value = Boolean(id)
  editId.value = id
  customerOptions.value = []
  selectedCustomer.value = undefined

  if (id) {
    const res = await getInvoiceApplication(id)
    formData.customerId = res.customerId
    formData.currency = res.currency
    formData.taxRate = Number(res.taxRate)
    formData.invoiceKind = res.invoiceKind
    formData.electronic = res.electronic
    formData.invoiceTitle = res.invoiceTitle
    formData.taxNo = res.taxNo || ''
    formData.address = res.address || ''
    formData.bankInfo = res.bankInfo || ''
    formData.remark = res.remark || ''
    formData.items = (res.items || []).map((item) => ({
      shipmentId: item.shipmentId,
      shipmentNo: item.shipmentNo,
      shipmentAmount: item.shipmentAmount,
      availableAmount: item.shipmentAmount, // 详情回显
      amount: item.amount,
      remark: item.remark
    }))

    // 预加载当前客户选项
    customerOptions.value = [
      {
        id: res.customerId,
        companyName: res.customerName,
        invoiceTitle: res.invoiceTitle,
        unifiedSocialCreditCode: res.taxNo,
        invoiceAddress: res.address,
        bankName: res.bankInfo
      } as CustomerVO
    ]
  } else {
    formData.customerId = undefined
    formData.currency = 'RMB'
    formData.taxRate = 13.0
    formData.invoiceKind = 'DIGITAL_SPECIAL'
    formData.electronic = 1
    formData.invoiceTitle = ''
    formData.taxNo = ''
    formData.address = ''
    formData.bankInfo = ''
    formData.remark = ''
    formData.items = []
  }
}

const searchCustomers = async (query: string) => {
  if (!query || query.trim().length === 0) {
    customerOptions.value = []
    return
  }
  customerLoading.value = true
  try {
    const res = await getCustomerPage({
      pageNo: 1,
      pageSize: 20,
      companyName: query.trim()
    })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = (val?: number) => {
  const customer = customerOptions.value.find((c) => c.id === val)
  selectedCustomer.value = customer
  formData.items = [] // 客户改变清空发货单
  if (customer) {
    fillCustomerDefaults(customer)
  }
}

const handleCurrencyChange = () => {
  formData.items = [] // 币种改变清空发货单
}

const fillCustomerDefaults = (customer: CustomerVO) => {
  formData.invoiceTitle = customer.invoiceTitle || customer.companyName || ''
  formData.taxNo = customer.unifiedSocialCreditCode || ''
  formData.address = customer.invoiceAddress || customer.address || ''
  const bank = customer.bankName ? customer.bankName.trim() : ''
  const acc = customer.bankAccountNo ? customer.bankAccountNo.trim() : ''
  formData.bankInfo = bank && acc ? `${bank} ${acc}` : bank || acc
}

const resetToCustomerDefaults = () => {
  if (selectedCustomer.value) {
    fillCustomerDefaults(selectedCustomer.value)
  } else if (formData.customerId) {
    const c = customerOptions.value.find((item) => item.id === formData.customerId)
    if (c) fillCustomerDefaults(c)
  }
}

const handleTaxRateChange = () => {
  recalculateSummary()
}

const recalculateSummary = () => {
  // computed 自动重新计算
}

const openShipmentSelect = () => {
  if (!formData.customerId) {
    ElMessage.warning('请先选择开票客户')
    return
  }
  const customer = customerOptions.value.find((c) => c.id === formData.customerId)
  shipmentSelectDialogRef.value?.open(
    formData.customerId,
    customer?.companyName || '',
    formData.currency
  )
}

const handleShipmentsSelected = (shipments: InvoiceCandidateShipmentRespVO[]) => {
  const existingMap = new Map(formData.items.map((i) => [i.shipmentId, i]))
  const newItems: FormItemRow[] = []

  for (const s of shipments) {
    if (existingMap.has(s.shipmentId)) {
      newItems.push(existingMap.get(s.shipmentId)!)
    } else {
      newItems.push({
        shipmentId: s.shipmentId,
        shipmentNo: s.shipmentNo,
        shipmentAmount: s.shipmentAmount,
        availableAmount: s.availableAmount,
        amount: s.availableAmount, // 默认填入该发货单未开票余额
        remark: ''
      })
    }
  }
  formData.items = newItems
}

const removeItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (formData.items.length === 0) {
    ElMessage.warning('请至少选择并添加一条发货单明细')
    return
  }

  // 校验每行金额
  for (const item of formData.items) {
    if (!item.amount || item.amount <= 0) {
      ElMessage.warning(`发货单【${item.shipmentNo}】开票金额必须大于 0`)
      return
    }
    if (item.availableAmount && item.amount > item.availableAmount) {
      ElMessage.warning(`发货单【${item.shipmentNo}】申请金额超出可开票余额`)
      return
    }
  }

  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateInvoiceApplication({
        id: editId.value,
        taxRate: formData.taxRate,
        invoiceKind: formData.invoiceKind,
        electronic: formData.electronic,
        invoiceTitle: formData.invoiceTitle?.trim() || undefined,
        taxNo: formData.taxNo?.trim() || undefined,
        address: formData.address?.trim() || undefined,
        bankInfo: formData.bankInfo?.trim() || undefined,
        remark: formData.remark?.trim() || undefined,
        items: formData.items.map((i) => ({
          shipmentId: i.shipmentId,
          amount: i.amount,
          remark: i.remark?.trim() || undefined
        }))
      })
      ElMessage.success('发票申请修改成功')
    } else {
      await createInvoiceApplication({
        customerId: formData.customerId!,
        currency: formData.currency,
        taxRate: formData.taxRate,
        invoiceKind: formData.invoiceKind,
        electronic: formData.electronic,
        invoiceTitle: formData.invoiceTitle?.trim() || undefined,
        taxNo: formData.taxNo?.trim() || undefined,
        address: formData.address?.trim() || undefined,
        bankInfo: formData.bankInfo?.trim() || undefined,
        remark: formData.remark?.trim() || undefined,
        items: formData.items.map((i) => ({
          shipmentId: i.shipmentId,
          amount: i.amount,
          remark: i.remark?.trim() || undefined
        }))
      })
      ElMessage.success('发票申请创建成功')
    }
    visible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
