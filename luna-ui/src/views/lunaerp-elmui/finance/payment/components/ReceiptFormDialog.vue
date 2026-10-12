<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑客户收款单' : '新建客户收款单'"
    width="960px"
    append-to-body
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="loading"
    >
      <!-- 基本信息 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="客户" prop="customerId">
            <el-select
              v-model="formData.customerId"
              placeholder="请输入关键词搜索客户"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchCustomers"
              :loading="customerLoading"
              class="w-full"
              :disabled="isEdit"
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
          <el-form-item label="收款类型" prop="receiptType">
            <el-select
              v-model="formData.receiptType"
              placeholder="请选择收款类型"
              class="w-full"
              :disabled="isEdit"
              @change="handleReceiptTypeChange"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_FINANCE_RECEIPT_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="币种" prop="currency">
            <el-select
              v-model="formData.currency"
              placeholder="请选择币种"
              class="w-full"
              :disabled="isEdit || (formData.items && formData.items.length > 0)"
              @change="handleCurrencyChange"
            >
              <el-option label="CNY - 人民币" value="CNY" />
              <el-option label="USD - 美元" value="USD" />
              <el-option label="EUR - 欧元" value="EUR" />
              <el-option label="HKD - 港币" value="HKD" />
              <el-option label="JPY - 日元" value="JPY" />
              <el-option label="GBP - 英镑" value="GBP" />
            </el-select>
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="收款金额" prop="payAmount">
            <el-input-number
              v-model="formData.payAmount"
              :min="0.01"
              :precision="2"
              :step="1000"
              class="!w-full"
              placeholder="收款金额"
            />
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="收款账户" prop="accountId">
            <el-select
              v-model="formData.accountId"
              placeholder="请选择资金账户"
              class="w-full"
            >
              <el-option
                v-for="acc in accountOptions"
                :key="acc.id"
                :label="`${acc.accountName} (${acc.currency}) 余额:￥${formatAmount(acc.currentBalance)}`"
                :value="acc.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="收款日期" prop="receiptDate">
            <el-date-picker
              v-model="formData.receiptDate"
              type="date"
              placeholder="选择收款日期"
              value-format="YYYY-MM-DD"
              class="!w-full"
            />
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="经手人" prop="handlerName">
            <el-input v-model="formData.handlerName" placeholder="经手人姓名" clearable />
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="银行水单号" prop="bankRefNo">
            <el-input v-model="formData.bankRefNo" placeholder="银行交易流水/凭证号" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="可填写收款备注说明"
        />
      </el-form-item>

      <!-- 预收款提示 -->
      <div v-if="formData.receiptType === 'ADVANCE'" class="mb-4">
        <el-alert
          title="预收款说明：预收款登记后将作为客户预收余额挂账，不直接核销应收分录；后续可在「预收转核销」中将预收余额核销至应收分录。"
          type="success"
          show-icon
          :closable="false"
        />
      </div>

      <!-- 货款核销分配区域 -->
      <div v-else class="mb-4 p-3 bg-[#f8fafc] border border-[#e2e8f0] rounded">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2">
            <span class="font-bold text-sm text-[#1e293b]">应收核销分配明细</span>
            <el-tag size="small" type="info">已分配 {{ (formData.items || []).length }} 笔</el-tag>
          </div>
          <div class="flex items-center gap-2">
            <el-button
              type="primary"
              size="small"
              plain
              :disabled="!formData.customerId || !formData.currency"
              @click="openSelectArDialog"
            >
              <Icon icon="ep:plus" class="mr-2px" />选择应收分录
            </el-button>
            <el-button
              type="success"
              size="small"
              plain
              :disabled="!formData.customerId || !formData.currency || !formData.payAmount"
              :loading="previewLoading"
              @click="handleAutoAllocate"
            >
              <Icon icon="ep:magic-stick" class="mr-2px" />自动分配 (FIFO)
            </el-button>
            <el-button
              size="small"
              :disabled="!formData.items || formData.items.length === 0"
              @click="handleClearItems"
            >
              清空
            </el-button>
          </div>
        </div>

        <el-table
          :data="formData.items"
          border
          size="small"
          stripe
          max-height="260"
        >
          <el-table-column label="分录单号" prop="entryNo" min-width="140">
            <template #default="{ row }">
              <span class="font-mono text-blue-600">{{ row.entryNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="业务日期" prop="bizDate" width="110" align="center" />
          <el-table-column label="关联单据" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.shipmentNo" class="font-mono text-xs block text-gray-700">发货: {{ row.shipmentNo }}</span>
              <span v-if="row.contractNo" class="font-mono text-xs block text-gray-500">合同: {{ row.contractNo }}</span>
              <span v-if="!row.shipmentNo && !row.contractNo" class="text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column label="未核销余额" width="120" align="right">
            <template #default="{ row }">
              <span class="font-mono text-gray-700">￥{{ formatAmount(row.unallocatedBalance) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="本次核销金额" width="160" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.allocateAmount"
                :min="0.01"
                :max="row.unallocatedBalance || undefined"
                :precision="2"
                :step="100"
                size="small"
                class="!w-140px"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button
                link
                type="danger"
                size="small"
                @click="handleRemoveItem($index)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无核销分录，可点击「选择应收分录」或「自动分配」" :image-size="60" />
          </template>
        </el-table>

        <!-- 金额合计与校验提示 -->
        <div class="mt-3 flex items-center justify-between text-xs bg-white p-2 rounded border border-[#e2e8f0]">
          <div class="flex items-center gap-4">
            <span>收款金额：<strong class="text-blue-600 text-sm">￥{{ formatAmount(formData.payAmount) }}</strong></span>
            <span>核销合计：<strong class="text-green-600 text-sm">￥{{ formatAmount(totalAllocated) }}</strong></span>
            <span>
              剩余未核销：
              <strong :class="unallocatedDiff < 0 ? 'text-red-600 text-sm' : 'text-gray-700 text-sm'">
                ￥{{ formatAmount(unallocatedDiff) }}
              </strong>
            </span>
          </div>
          <div v-if="totalAllocated > (formData.payAmount || 0)" class="text-red-600 font-bold">
            ⚠ 核销分配总额超出收款金额，请调整！
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" plain :loading="submitting" @click="handleSave(false)">
        保存草稿
      </el-button>
      <el-button type="primary" :loading="submitting" @click="handleSave(true)">
        保存并提交审批
      </el-button>
    </template>
  </el-dialog>

  <!-- 应收分录选择弹窗 -->
  <ArEntrySelectDialog ref="arSelectDialogRef" @select="handleArSelect" />
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type {
  ReceiptSaveReqVO,
  ReceiptRefSaveVO,
  AccountSimpleVO,
  ArBalanceRespVO,
  PaymentVO
} from '@/api/lunaerp-elmui/finance/payment'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import { formatAmount } from '../utils'
import ArEntrySelectDialog from './ArEntrySelectDialog.vue'

defineOptions({ name: 'ReceiptFormDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const previewLoading = ref(false)
const isEdit = ref(false)

const formRef = ref()
const arSelectDialogRef = ref()

interface ExtendedRefVO extends ReceiptRefSaveVO {
  unallocatedBalance?: number
  shipmentNo?: string
  contractNo?: string
}

const formData = reactive<ReceiptSaveReqVO & { items: ExtendedRefVO[] }>({
  id: undefined,
  paymentNo: undefined,
  customerId: undefined as any,
  receiptType: 'RECEIPT',
  currency: 'CNY',
  receiptDate: new Date().toISOString().substring(0, 10),
  accountId: undefined as any,
  payAmount: undefined as any,
  bankRefNo: '',
  handlerName: '',
  remark: '',
  items: []
})

const formRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  receiptType: [{ required: true, message: '请选择收款类型', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }],
  payAmount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  accountId: [{ required: true, message: '请选择资金账户', trigger: 'change' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }]
}

// 客户远程下拉
const customerOptions = ref<CustomerVO[]>([])
const customerLoading = ref(false)

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({
      pageNo: 1,
      pageSize: 30,
      search: query || '',
      status: 'ACTIVE'
    })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

// 资金账户列表
const accountOptions = ref<AccountSimpleVO[]>([])

const loadAccounts = async () => {
  try {
    accountOptions.value = await PaymentApi.getAccountSimpleList()
  } catch (e) {
    ElMessage.error('加载资金账户失败')
  }
}

// 计算核销总额
const totalAllocated = computed(() => {
  if (!formData.items || formData.items.length === 0) return 0
  return formData.items.reduce((sum, item) => sum + (Number(item.allocateAmount) || 0), 0)
})

const unallocatedDiff = computed(() => {
  return Number((Number(formData.payAmount || 0) - totalAllocated.value).toFixed(2))
})

const handleCustomerChange = () => {
  formData.items = []
}

const handleReceiptTypeChange = () => {
  if (formData.receiptType === 'ADVANCE') {
    formData.items = []
  }
}

const handleCurrencyChange = () => {
  formData.items = []
}

const openSelectArDialog = () => {
  const excludeIds = (formData.items || []).map((item) => item.ledgerEntryId)
  arSelectDialogRef.value?.open(formData.customerId, formData.currency, excludeIds)
}

const handleArSelect = (selectedList: ArBalanceRespVO[]) => {
  for (const item of selectedList) {
    // 默认分配金额为尚未核销余额与剩余收款可用金额的较小值
    const curAllocated = totalAllocated.value
    const remainingToAllocate = Math.max(0, Number(formData.payAmount || 0) - curAllocated)
    const initAlloc = Math.min(Number(item.unallocatedBalance || 0), remainingToAllocate)

    formData.items.push({
      ledgerEntryId: item.ledgerEntryId,
      entryNo: item.entryNo,
      bizDate: item.bizDate,
      allocateAmount: initAlloc > 0 ? Number(initAlloc.toFixed(2)) : Number(item.unallocatedBalance || 0),
      unallocatedBalance: item.unallocatedBalance,
      shipmentNo: item.shipmentNo,
      contractNo: item.contractNo
    })
  }
}

const handleAutoAllocate = async () => {
  if (!formData.customerId || !formData.currency || !formData.payAmount) {
    ElMessage.warning('请先完善客户、币种与收款金额')
    return
  }
  previewLoading.value = true
  try {
    const res = await PaymentApi.allocatePreview({
      customerId: formData.customerId,
      currency: formData.currency,
      payAmount: formData.payAmount
    })
    if (!res || !res.items || res.items.length === 0) {
      ElMessage.info('当前该客户暂无可核销的应收分录')
      return
    }
    formData.items = res.items.map((item) => ({
      ledgerEntryId: item.ledgerEntryId,
      entryNo: item.entryNo,
      bizDate: item.bizDate,
      allocateAmount: item.allocatedAmount,
      unallocatedBalance: item.unallocatedBalance
    }))
    ElMessage.success(`自动分配完成，共匹配 ${res.items.length} 笔分录`)
  } catch (e) {
    ElMessage.error('自动分配试算失败')
  } finally {
    previewLoading.value = false
  }
}

const handleClearItems = () => {
  formData.items = []
}

const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
}

const open = async (type: 'create' | 'edit', row?: PaymentVO) => {
  visible.value = true
  isEdit.value = type === 'edit'
  await loadAccounts()
  await searchCustomers('')

  if (type === 'edit' && row) {
    formData.id = row.id
    formData.paymentNo = row.paymentNo
    formData.customerId = row.customerId!
    formData.receiptType = row.receiptType || 'RECEIPT'
    formData.currency = row.currency || 'CNY'
    formData.receiptDate = row.receiptDate || new Date().toISOString().substring(0, 10)
    formData.accountId = row.accountId
    formData.payAmount = row.payAmount
    formData.bankRefNo = row.bankRefNo || ''
    formData.handlerName = row.handlerName || ''
    formData.remark = row.remark || ''
    formData.items = (row.items || []).map((it) => ({
      id: it.id,
      ledgerEntryId: it.refId,
      entryNo: it.entryNo || '',
      bizDate: it.bizDate || '',
      allocateAmount: it.allocateAmount,
      unallocatedBalance: it.allocateAmount
    }))

    // 确保当前客户选项存在
    if (row.customerId && !customerOptions.value.some((c) => c.id === row.customerId)) {
      customerOptions.value.unshift({
        id: row.customerId,
        companyName: row.customerName || `客户 #${row.customerId}`,
        clientType: ''
      })
    }
  } else {
    formData.id = undefined
    formData.paymentNo = undefined
    formData.customerId = undefined as any
    formData.receiptType = 'RECEIPT'
    formData.currency = 'CNY'
    formData.receiptDate = new Date().toISOString().substring(0, 10)
    formData.accountId = accountOptions.value.length > 0 ? accountOptions.value[0].id : (undefined as any)
    formData.payAmount = undefined as any
    formData.bankRefNo = ''
    formData.handlerName = ''
    formData.remark = ''
    formData.items = []
  }
}

const handleSave = async (submitAfterSave: boolean) => {
  await formRef.value?.validate()

  if (formData.receiptType === 'RECEIPT') {
    if (totalAllocated.value > (formData.payAmount || 0)) {
      ElMessage.warning('核销分配总额不能超出收款金额！')
      return
    }
    for (const it of formData.items) {
      if (!it.allocateAmount || it.allocateAmount <= 0) {
        ElMessage.warning('核销分配金额必须大于 0！')
        return
      }
    }
  }

  if (submitAfterSave) {
    await ElMessageBox.confirm('确定保存并立即提交审批吗？', '提交审批确认', {
      confirmButtonText: '确定提交',
      cancelButtonText: '取消',
      type: 'warning'
    })
  }

  submitting.value = true
  try {
    const payload: ReceiptSaveReqVO = {
      id: formData.id,
      paymentNo: formData.paymentNo,
      customerId: formData.customerId,
      receiptType: formData.receiptType,
      currency: formData.currency,
      receiptDate: formData.receiptDate,
      accountId: formData.accountId,
      payAmount: formData.payAmount,
      bankRefNo: formData.bankRefNo,
      handlerName: formData.handlerName,
      remark: formData.remark,
      items: (formData.items || []).map((it) => ({
        id: it.id,
        ledgerEntryId: it.ledgerEntryId,
        allocateAmount: it.allocateAmount,
        entryNo: it.entryNo,
        bizDate: it.bizDate
      }))
    }

    let receiptId = formData.id
    if (isEdit.value && receiptId) {
      await PaymentApi.updateReceipt(payload)
      ElMessage.success('收款单更新成功')
    } else {
      receiptId = await PaymentApi.createReceipt(payload)
      ElMessage.success('收款单草稿创建成功')
    }

    if (submitAfterSave && receiptId) {
      await PaymentApi.submitReceipt(receiptId)
      ElMessage.success('收款单已提交审批')
    }

    visible.value = false
    emit('success')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存收款单失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
