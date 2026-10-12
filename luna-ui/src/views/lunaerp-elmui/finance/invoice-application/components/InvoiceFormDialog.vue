<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑发票申请' : '新建发票申请'"
    width="1000px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="loading"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户名称" prop="customerId">
            <el-select
              v-model="formData.customerId"
              placeholder="请搜索选择客户"
              filterable
              remote
              reserve-keyword
              :remote-method="searchCustomers"
              :loading="customerLoading"
              :disabled="isEdit || formData.items.length > 0"
              style="width: 100%"
              @change="handleCustomerChange"
            >
              <el-option
                v-for="c in customerOptions"
                :key="c.id"
                :label="c.companyName"
                :value="c.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="发票种类" prop="invoiceKind">
            <el-select v-model="formData.invoiceKind" placeholder="选择发票种类" style="width: 100%">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_FINANCE_INVOICE_KIND)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="6">
          <el-form-item label="电子发票" prop="electronic">
            <el-radio-group v-model="formData.electronic">
              <el-radio :label="1">是</el-radio>
              <el-radio :label="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="币种" prop="currency">
            <el-input v-model="formData.currency" placeholder="由发货单自动带出" readonly />
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="开票税率" prop="taxRate">
            <el-select v-model="formData.taxRate" placeholder="请选择税率" style="width: 100%" @change="recalculateAmounts">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_FINANCE_TAX_RATE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="价税合计预览">
            <span class="font-bold text-red-600 text-lg">￥{{ formatAmount(previewTotals.inclTax) }}</span>
          </el-form-item>
        </el-col>
      </el-row>

      <el-card shadow="never" class="mb-4 bg-gray-50/50">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-medium text-sm text-gray-700">开票四要素（默认带出客户档案，可根据开票要求修改）</span>
            <el-button link type="primary" size="small" @click="resetBillingInfoFromCustomer">
              从客户档案重置
            </el-button>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开票抬头" prop="invoiceTitle">
              <el-input v-model="formData.invoiceTitle" placeholder="请输入开票抬头" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税号/信用代码" prop="taxNo">
              <el-input v-model="formData.taxNo" placeholder="请输入纳税人识别号 / 统一社会信用代码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开票地址电话" prop="address">
              <el-input v-model="formData.address" placeholder="请输入开票地址与联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开户行及账号" prop="bankInfo">
              <el-input v-model="formData.bankInfo" placeholder="请输入开户银行名称及账号" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 关联发货单明细 -->
      <div class="mb-3 flex items-center justify-between">
        <div class="font-medium text-base text-gray-800">
          发货单开票明细（至少选择一张）
        </div>
        <el-button
          type="primary"
          plain
          size="small"
          :disabled="!formData.customerId"
          @click="openSelectShipmentDialog"
        >
          <Icon icon="ep:plus" class="mr-1" /> 添加发货单
        </el-button>
      </div>

      <el-table :data="formData.items" border style="width: 100%" class="mb-4">
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="发货单号" prop="shipmentNo" min-width="160" />
        <el-table-column label="单据可开票总额" width="140" align="right">
          <template #default="{ row }">
            {{ formatAmount(row.shipmentAmount) }}
          </template>
        </el-table-column>
        <el-table-column label="本次开票金额" width="180" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.amount"
              :min="0.01"
              :max="row.maxAmount || row.shipmentAmount || 99999999"
              :precision="2"
              :step="100"
              controls-position="right"
              style="width: 150px"
              @change="recalculateAmounts"
            />
          </template>
        </el-table-column>
        <el-table-column label="行备注" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注（选填）" maxlength="128" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeItem($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-between items-center px-4 py-2 bg-gray-100 rounded mb-4 text-sm">
        <div>
          <span>已选发货单：<b>{{ formData.items.length }}</b> 笔</span>
        </div>
        <div class="flex space-x-6">
          <span>价税合计：<b class="text-red-600">￥{{ formatAmount(previewTotals.inclTax) }}</b></span>
          <span>不含税金额：<b>￥{{ formatAmount(previewTotals.exclTax) }}</b></span>
          <span>税额：<b>￥{{ formatAmount(previewTotals.tax) }}</b></span>
        </div>
      </div>

      <el-form-item label="申请备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          rows="2"
          placeholder="请输入申请备注（选填）"
          maxlength="256"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
        保存草稿
      </el-button>
    </template>

    <ShipmentSelectDialog ref="shipmentSelectDialogRef" @select="handleShipmentsSelected" />
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as InvoiceApi from '@/api/lunaerp-elmui/finance/invoice'
import type { InvoiceCandidateShipmentVO } from '@/api/lunaerp-elmui/finance/invoice'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import ShipmentSelectDialog from './ShipmentSelectDialog.vue'

defineOptions({ name: 'InvoiceFormDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref()
const shipmentSelectDialogRef = ref()

const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])
const selectedCustomer = ref<CustomerVO | null>(null)

interface FormItem {
  id?: number
  shipmentId: number
  shipmentNo: string
  shipmentAmount: number
  maxAmount?: number
  amount: number
  remark?: string
}

const formData = reactive({
  id: undefined as number | undefined,
  customerId: undefined as number | undefined,
  currency: 'CNY',
  taxRate: '13',
  invoiceKind: 'ELEC_SPECIAL',
  electronic: 1,
  invoiceTitle: '',
  taxNo: '',
  address: '',
  bankInfo: '',
  remark: '',
  items: [] as FormItem[]
})

const formRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  invoiceKind: [{ required: true, message: '请选择发票种类', trigger: 'change' }],
  invoiceTitle: [{ required: true, message: '请输入开票抬头', trigger: 'blur' }]
}

function formatAmount(val?: number | string | null): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const previewTotals = computed(() => {
  const inclTax = formData.items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
  let rateNum = 0
  if (formData.taxRate && formData.taxRate !== 'EXEMPT') {
    rateNum = Number(formData.taxRate) || 0
  }
  let exclTax = inclTax
  let tax = 0
  if (rateNum > 0) {
    exclTax = Math.round((inclTax / (1 + rateNum / 100)) * 100) / 100
    tax = Math.round((inclTax - exclTax) * 100) / 100
  }
  return { inclTax, exclTax, tax }
})

const searchCustomers = async (query: string) => {
  if (!query) return
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({ search: query, pageNo: 1, pageSize: 20 })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = async (customerId?: number) => {
  if (!customerId) {
    selectedCustomer.value = null
    return
  }
  try {
    const cust = await CustomerApi.getCustomer(customerId)
    selectedCustomer.value = cust
    applyCustomerBillingInfo(cust)
  } catch (e) {
    // ignore
  }
}

const applyCustomerBillingInfo = (cust: CustomerVO) => {
  formData.invoiceTitle = cust.invoiceTitle || cust.companyName || ''
  formData.taxNo = cust.unifiedSocialCreditCode || ''
  formData.address = cust.invoiceAddress || cust.address || ''
  if (cust.bankName || cust.bankAccountNo) {
    formData.bankInfo = `${cust.bankName || ''} ${cust.bankAccountNo || ''}`.trim()
  } else {
    formData.bankInfo = ''
  }
}

const resetBillingInfoFromCustomer = () => {
  if (selectedCustomer.value) {
    applyCustomerBillingInfo(selectedCustomer.value)
  }
}

const openSelectShipmentDialog = () => {
  if (!formData.customerId) return
  const alreadyIds = formData.items.map((i) => i.shipmentId)
  shipmentSelectDialogRef.value.open(formData.customerId, formData.currency, alreadyIds)
}

const handleShipmentsSelected = (shipments: InvoiceCandidateShipmentVO[]) => {
  if (!shipments.length) return
  if (!formData.currency && shipments[0].currency) {
    formData.currency = shipments[0].currency
  }
  for (const s of shipments) {
    formData.items.push({
      shipmentId: s.id,
      shipmentNo: s.shipmentNo,
      shipmentAmount: Number(s.shipmentAmount),
      maxAmount: Number(s.uninvoicedBalance),
      amount: Number(s.uninvoicedBalance),
      remark: ''
    })
  }
  recalculateAmounts()
}

const removeItem = (index: number) => {
  formData.items.splice(index, 1)
  if (formData.items.length === 0 && !isEdit.value) {
    formData.currency = 'CNY'
  }
}

const recalculateAmounts = () => {
  // trigger reactivity
}

const open = async (id?: number) => {
  visible.value = true
  isEdit.value = !!id
  formData.id = id
  formData.customerId = undefined
  formData.currency = 'CNY'
  formData.taxRate = '13'
  formData.invoiceKind = 'ELEC_SPECIAL'
  formData.electronic = 1
  formData.invoiceTitle = ''
  formData.taxNo = ''
  formData.address = ''
  formData.bankInfo = ''
  formData.remark = ''
  formData.items = []
  customerOptions.value = []
  selectedCustomer.value = null

  if (id) {
    loading.value = true
    try {
      const data = await InvoiceApi.getInvoice(id)
      formData.id = data.id
      formData.customerId = data.customerId
      formData.currency = data.currency || 'CNY'
      formData.taxRate = data.taxRate || '13'
      formData.invoiceKind = data.invoiceKind || 'ELEC_SPECIAL'
      formData.electronic = data.electronic ?? 1
      formData.invoiceTitle = data.invoiceTitle || ''
      formData.taxNo = data.taxNo || ''
      formData.address = data.address || ''
      formData.bankInfo = data.bankInfo || ''
      formData.remark = data.remark || ''

      if (data.customerId) {
        const cust = await CustomerApi.getCustomer(data.customerId)
        customerOptions.value = [cust]
        selectedCustomer.value = cust
      }

      if (data.items) {
        formData.items = data.items.map((it) => ({
          id: it.id,
          shipmentId: it.shipmentId,
          shipmentNo: it.shipmentNo || '',
          shipmentAmount: Number(it.shipmentAmount || 0),
          maxAmount: Number(it.shipmentAmount || 0),
          amount: Number(it.amount || 0),
          remark: it.remark || ''
        }))
      }
    } finally {
      loading.value = false
    }
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (formData.items.length === 0) {
    ElMessage.warning('请至少添加一张发货单！')
    return
  }
  for (const item of formData.items) {
    if (!item.amount || item.amount <= 0) {
      ElMessage.warning(`发货单 ${item.shipmentNo} 本次开票金额必须大于0！`)
      return
    }
  }

  submitLoading.value = true
  try {
    const payload: InvoiceApi.InvoiceSaveReqVO = {
      id: formData.id,
      customerId: formData.customerId!,
      currency: formData.currency,
      taxRate: formData.taxRate,
      invoiceKind: formData.invoiceKind,
      electronic: formData.electronic,
      invoiceTitle: formData.invoiceTitle,
      taxNo: formData.taxNo,
      address: formData.address,
      bankInfo: formData.bankInfo,
      remark: formData.remark,
      items: formData.items.map((i) => ({
        id: i.id,
        shipmentId: i.shipmentId,
        amount: i.amount,
        remark: i.remark
      }))
    }
    if (isEdit.value) {
      await InvoiceApi.updateInvoice(payload)
      ElMessage.success('发票申请更新成功')
    } else {
      await InvoiceApi.createInvoice(payload)
      ElMessage.success('发票申请草稿创建成功')
    }
    visible.value = false
    emit('success')
  } catch (e) {
    // handled by axios interceptor
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
