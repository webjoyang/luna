<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? `编辑收款单草稿 - ${formData.paymentNo || ''}` : '新增收款单'"
    width="960px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
        size="default"
      >
        <!-- 第一行：客户与账户 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="收款客户" prop="customerId">
              <el-select
                v-model="formData.customerId"
                placeholder="请搜索并选择客户"
                filterable
                remote
                reserve-keyword
                :remote-method="searchCustomers"
                :loading="customerLoading"
                :disabled="isEdit || allocatedItems.length > 0"
                class="!w-full"
                @change="handleCustomerChange"
              >
                <el-option
                  v-for="c in customerOptions"
                  :key="c.id"
                  :label="c.companyName"
                  :value="c.id!"
                />
              </el-select>
              <div
                v-if="allocatedItems.length > 0 && !isEdit"
                class="text-11px text-amber-600 mt-2px"
              >
                已选入核销分录，如需切换客户请先清空分录
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="收款账户" prop="accountId">
              <el-select
                v-model="formData.accountId"
                placeholder="请选择资金收款账户"
                filterable
                class="!w-full"
                @change="handleAccountChange"
              >
                <el-option
                  v-for="acc in accountOptions"
                  :key="acc.id"
                  :label="`${acc.accountName || acc.name || '账户'}${acc.currency ? ` (${acc.currency})` : ''}`"
                  :value="acc.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二行：类型与币种与金额 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="收款类型" prop="receiptType">
              <el-radio-group v-model="formData.receiptType" :disabled="isEdit">
                <el-radio-button label="RECEIPT">客户收款</el-radio-button>
                <el-radio-button label="ADVANCE">预收款</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="收款币种" prop="currency">
              <el-select
                v-model="formData.currency"
                placeholder="币种"
                :disabled="isEdit"
                class="!w-full"
              >
                <el-option label="人民币 (RMB)" value="RMB" />
                <el-option label="美元 (USD)" value="USD" />
                <el-option label="欧元 (EUR)" value="EUR" />
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
                controls-position="right"
                class="!w-full"
                placeholder="0.00"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第三行：日期与经手人与流水号 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="收款日期" prop="receiptDate">
              <el-date-picker
                v-model="formData.receiptDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择收款日期"
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
            <el-form-item label="银行回单号" prop="bankRefNo">
              <el-input v-model="formData.bankRefNo" placeholder="银行流水/回单号" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第四行：单号与备注 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="收款单号" prop="paymentNo">
              <el-input
                v-model="formData.paymentNo"
                :disabled="isEdit"
                placeholder="留空则系统自动编号(RC-yyyyMMdd-xxx)"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注说明" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                placeholder="填写收款备注、汇款附言或核销说明"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 核销明细表格区（仅普通收款单有效，预收单无须选单核销） -->
        <el-card
          v-if="formData.receiptType === 'RECEIPT'"
          shadow="never"
          class="mt-8px !border-gray-200"
        >
          <template #header>
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <span class="font-bold text-14px text-gray-800">核销分配明细</span>
                <span class="text-12px text-gray-400 ml-10px">
                  支持多发货单/多合同明细组合核销，单行核销额不得超出未核销余额
                </span>
              </div>
              <div class="space-x-8px">
                <el-button
                  type="primary"
                  plain
                  size="small"
                  :disabled="!formData.customerId"
                  @click="handleOpenSelectDialog"
                >
                  <Icon icon="ep:plus" class="mr-2px" /> 选单添加分录
                </el-button>
                <el-button
                  type="success"
                  plain
                  size="small"
                  :disabled="allocatedItems.length === 0 || !formData.payAmount"
                  @click="handleFifoAutoAllocate"
                >
                  <Icon icon="ep:magic-stick" class="mr-2px" /> FIFO 自动分配
                </el-button>
                <el-button
                  type="danger"
                  plain
                  size="small"
                  :disabled="allocatedItems.length === 0"
                  @click="handleClearAllocations"
                >
                  <Icon icon="ep:delete" class="mr-2px" /> 清空分配
                </el-button>
              </div>
            </div>
          </template>

          <el-table
            :data="allocatedItems"
            border
            stripe
            size="small"
            empty-text="暂无核销分录，请点击上方「选单添加分录」"
          >
            <el-table-column label="#" type="index" width="45" align="center" />
            <el-table-column label="业务日期" prop="bizDate" width="105" align="center" />
            <el-table-column label="发货单号" prop="refNo" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="font-mono font-medium">{{ row.refNo || row.entryNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column
              label="合同编号"
              prop="contractNo"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span class="font-mono text-gray-600">{{ row.contractNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column
              label="产品摘要"
              prop="productName"
              min-width="120"
              show-overflow-tooltip
            />
            <el-table-column label="未核销余额" prop="unallocatedAmount" width="120" align="right">
              <template #default="{ row }">
                <span class="font-mono text-amber-700 font-medium">
                  ¥ {{ formatMoney(row.unallocatedAmount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="本次分配金额" width="160" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.allocateAmount"
                  :min="0"
                  :max="row.unallocatedAmount"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  size="small"
                  class="!w-full"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="handleRemoveItem($index)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 底部核销汇总对账条 -->
          <div
            class="flex items-center justify-between mt-12px bg-gray-50 p-10px rounded border text-13px"
          >
            <div>
              收款金额：
              <span class="font-mono font-bold text-gray-800">
                ¥ {{ formatMoney(formData.payAmount) }}
              </span>
            </div>
            <div>
              分配合计：
              <span class="font-mono font-bold text-emerald-700">
                ¥ {{ formatMoney(totalAllocated) }}
              </span>
            </div>
            <div>
              未分配金额：
              <span
                class="font-mono font-bold"
                :class="unallocatedPool < 0 ? 'text-red-600' : 'text-blue-600'"
              >
                ¥ {{ formatMoney(unallocatedPool) }}
              </span>
            </div>
          </div>
        </el-card>
      </el-form>
    </div>

    <template #footer>
      <div class="flex items-center justify-between">
        <span class="text-12px text-gray-400">
          {{ isEdit ? '修改草稿后可直接保存或重新提交审批' : '收款单创建后将处于草稿待提交状态' }}
        </span>
        <div class="space-x-8px">
          <el-button @click="visible = false">取消</el-button>
          <el-button
            type="primary"
            plain
            :loading="submitting"
            v-hasPermi="['lunaerp:finance:payment:create', 'lunaerp:finance:payment:update']"
            @click="handleSave(false)"
          >
            保存草稿
          </el-button>
          <el-button
            type="primary"
            :loading="submitting"
            v-hasPermi="['lunaerp:finance:payment:create', 'lunaerp:finance:payment:update']"
            @click="handleSave(true)"
          >
            保存并提交审批
          </el-button>
        </div>
      </div>
    </template>

    <!-- 选单弹窗 -->
    <ReceiptAllocateSelectDialog ref="selectDialogRef" @select="handleSelectEntries" />
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createReceipt,
  updateReceipt,
  submitReceipt,
  getReceipt,
  getFinanceAccountSimpleList,
  allocatePreview,
  type FinanceAccountSimpleRespVO,
  type ArBalanceRespVO
} from '@/api/lunaerp-elmui/finance/payment'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  formatMoney,
  validateReceiptForm,
  distributeReceiptFifo,
  parsePaymentErrorMessage
} from '../model/payment'
import ReceiptAllocateSelectDialog from './ReceiptAllocateSelectDialog.vue'

defineOptions({ name: 'ReceiptFormDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const currentReceiptId = ref<number | null>(null)

const formRef = ref<FormInstance>()
const selectDialogRef = ref<InstanceType<typeof ReceiptAllocateSelectDialog>>()

// 客户与账户下拉
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])
const accountOptions = ref<FinanceAccountSimpleRespVO[]>([])

// 表单状态
const formData = reactive({
  customerId: undefined as number | undefined,
  accountId: undefined as number | undefined,
  currency: 'RMB',
  receiptType: 'RECEIPT',
  payAmount: undefined as number | undefined,
  receiptDate: new Date().toLocaleDateString('en-CA'),
  handlerName: '',
  bankRefNo: '',
  paymentNo: '',
  remark: ''
})

// 核销分录明细行
interface TableItem {
  entryId: number
  bizDate?: string
  refNo?: string
  contractNo?: string
  productName?: string
  originalAmount?: number
  unallocatedAmount: number
  allocateAmount: number
}
const allocatedItems = ref<TableItem[]>([])

const formRules: FormRules = {
  customerId: [{ required: true, message: '请选择收款客户', trigger: 'change' }],
  accountId: [{ required: true, message: '请选择收款账户', trigger: 'change' }],
  currency: [{ required: true, message: '请选择收款币种', trigger: 'change' }],
  payAmount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }]
}

// 汇总计算
const totalAllocated = computed(() => {
  return allocatedItems.value.reduce((sum, item) => sum + Number(item.allocateAmount || 0), 0)
})

const unallocatedPool = computed(() => {
  const pay = Number(formData.payAmount || 0)
  return Number((pay - totalAllocated.value).toFixed(2))
})

// 搜索客户
const searchCustomers = async (query?: string) => {
  customerLoading.value = true
  try {
    const res = await getCustomerPage({
      pageNo: 1,
      pageSize: 30,
      search: query || ''
    })
    customerOptions.value = res.list || []
  } catch {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

// 加载账户精简下拉
const loadAccounts = async () => {
  try {
    const list = await getFinanceAccountSimpleList()
    accountOptions.value = (list || []).filter(
      (acc) => String(acc.status) === 'ENABLE' || acc.status === 1 || acc.status === '0'
    )
  } catch {
    accountOptions.value = []
  }
}

const handleAccountChange = (accId: number) => {
  const chosen = accountOptions.value.find((a) => a.id === accId)
  if (chosen && chosen.currency) {
    formData.currency = chosen.currency
  }
}

const handleCustomerChange = () => {
  allocatedItems.value = []
}

// 选单添加
const handleOpenSelectDialog = () => {
  if (!formData.customerId) {
    ElMessage.warning('请先选择收款客户')
    return
  }
  const chosenIds = allocatedItems.value.map((i) => i.entryId)
  selectDialogRef.value?.open(formData.customerId, formData.currency || 'RMB', chosenIds)
}

const handleSelectEntries = (entries: ArBalanceRespVO[]) => {
  for (const entry of entries) {
    if (!allocatedItems.value.some((i) => i.entryId === entry.entryId)) {
      allocatedItems.value.push({
        entryId: entry.entryId,
        bizDate: entry.bizDate,
        refNo: entry.refNo,
        contractNo: entry.contractNo,
        productName: entry.productName,
        originalAmount: entry.originalAmount,
        unallocatedAmount: entry.unallocatedAmount,
        allocateAmount: 0
      })
    }
  }
}

const handleRemoveItem = (index: number) => {
  allocatedItems.value.splice(index, 1)
}

const handleClearAllocations = () => {
  allocatedItems.value.forEach((i) => (i.allocateAmount = 0))
}

// FIFO 自动分配
const handleFifoAutoAllocate = async () => {
  if (!formData.customerId || !formData.payAmount) {
    ElMessage.warning('请输入收款金额以进行 FIFO 自动分配')
    return
  }

  try {
    // 优先调用后端自动分配试算
    const previewList = await allocatePreview({
      customerId: formData.customerId,
      currency: formData.currency,
      payAmount: formData.payAmount
    })

    const previewMap = new Map<number, number>()
    previewList.forEach((p) => previewMap.set(p.entryId, p.allocateAmount))

    allocatedItems.value.forEach((item) => {
      if (previewMap.has(item.entryId)) {
        item.allocateAmount = previewMap.get(item.entryId)!
      } else {
        item.allocateAmount = 0
      }
    })
    ElMessage.success('已按发货日升序（FIFO）完成自动分配试算')
  } catch {
    // 后端降级：本地前端 FIFO 算子
    const allocated = distributeReceiptFifo(allocatedItems.value, formData.payAmount)
    const allocMap = new Map<number, number>()
    allocated.forEach((a) => allocMap.set(a.entryId, a.allocateAmount))
    allocatedItems.value.forEach((item) => {
      item.allocateAmount = allocMap.get(item.entryId) || 0
    })
    ElMessage.success('已使用前端 FIFO 算法完成自动分配')
  }
}

// 保存/提交
const handleSave = async (submitAfterSave: boolean) => {
  if (!formRef.value) return
  await formRef.value.validate()

  // 领域校验
  const validation = validateReceiptForm({
    customerId: formData.customerId,
    accountId: formData.accountId,
    currency: formData.currency,
    receiptType: formData.receiptType,
    payAmount: formData.payAmount!,
    items:
      formData.receiptType === 'RECEIPT'
        ? allocatedItems.value
            .filter((i) => Number(i.allocateAmount) > 0)
            .map((i) => ({
              entryId: i.entryId,
              entryNo: i.refNo,
              allocateAmount: i.allocateAmount,
              unallocatedAmount: i.unallocatedAmount
            }))
        : []
  })

  if (!validation.valid) {
    ElMessage.error(validation.error || '表单校验不通过')
    return
  }

  submitting.value = true
  try {
    const itemsPayload =
      formData.receiptType === 'RECEIPT'
        ? allocatedItems.value
            .filter((i) => Number(i.allocateAmount) > 0)
            .map((i) => ({
              entryId: i.entryId,
              allocateAmount: i.allocateAmount
            }))
        : []

    let receiptId = currentReceiptId.value

    if (isEdit.value && receiptId) {
      await updateReceipt({
        id: receiptId,
        accountId: formData.accountId,
        receiptType: formData.receiptType,
        payAmount: formData.payAmount,
        receiptDate: formData.receiptDate,
        bankRefNo: formData.bankRefNo,
        handlerName: formData.handlerName,
        remark: formData.remark,
        items: itemsPayload
      })
      ElMessage.success('收款单草稿更新成功')
    } else {
      receiptId = await createReceipt({
        customerId: formData.customerId!,
        accountId: formData.accountId!,
        currency: formData.currency,
        receiptType: formData.receiptType,
        payAmount: formData.payAmount!,
        receiptDate: formData.receiptDate,
        bankRefNo: formData.bankRefNo,
        handlerName: formData.handlerName,
        paymentNo: formData.paymentNo || undefined,
        remark: formData.remark,
        items: itemsPayload
      })
      ElMessage.success('收款单草稿创建成功')
    }

    if (submitAfterSave && receiptId) {
      await submitReceipt(receiptId)
      ElMessage.success('收款单已提交审批')
    }

    visible.value = false
    emit('success')
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

// 打开弹窗入口
const open = async (id?: number) => {
  visible.value = true
  isEdit.value = !!id
  currentReceiptId.value = id || null
  allocatedItems.value = []

  // 重置表单
  formData.customerId = undefined
  formData.accountId = undefined
  formData.currency = 'RMB'
  formData.receiptType = 'RECEIPT'
  formData.payAmount = undefined
  formData.receiptDate = new Date().toLocaleDateString('en-CA')
  formData.handlerName = ''
  formData.bankRefNo = ''
  formData.paymentNo = ''
  formData.remark = ''

  await Promise.all([loadAccounts(), searchCustomers()])

  if (id) {
    loading.value = true
    try {
      const res = await getReceipt(id)
      formData.customerId = res.customerId
      formData.accountId = res.accountId
      formData.currency = res.currency || 'RMB'
      formData.receiptType = res.receiptType || 'RECEIPT'
      formData.payAmount = res.payAmount
      formData.receiptDate = res.receiptDate
      formData.handlerName = res.handlerName || ''
      formData.bankRefNo = res.bankRefNo || ''
      formData.paymentNo = res.paymentNo || ''
      formData.remark = res.remark || ''

      // 确保客户下拉中有该客户
      if (res.customerId && res.customerName) {
        if (!customerOptions.value.some((c) => c.id === res.customerId)) {
          customerOptions.value.push({ id: res.customerId, companyName: res.customerName })
        }
      }

      if (res.items && res.items.length > 0) {
        allocatedItems.value = res.items.map((i) => ({
          entryId: i.entryId,
          bizDate: i.bizDate,
          refNo: i.refNo || i.entryNo,
          contractNo: i.contractNo,
          productName: i.productName,
          originalAmount: i.originalAmount,
          unallocatedAmount:
            (i.unallocatedAmount !== undefined ? i.unallocatedAmount : 0) +
            Number(i.allocateAmount || 0),
          allocateAmount: i.allocateAmount
        }))
      }
    } catch (err: any) {
      ElMessage.error(err.message || '加载收款单详情失败')
      visible.value = false
    } finally {
      loading.value = false
    }
  }
}

defineExpose({ open })
</script>
