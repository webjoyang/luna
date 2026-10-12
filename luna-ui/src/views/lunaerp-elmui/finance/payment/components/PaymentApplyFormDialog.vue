<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑付款申请' : '发起付款申请'"
    width="680px"
    append-to-body
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="formLoading"
    >
      <!-- 申请类型 -->
      <el-form-item label="申请类型" prop="applyType">
        <el-radio-group v-model="formData.applyType" :disabled="isEdit" @change="handleTypeChange">
          <el-radio-button value="RECON">对账付款</el-radio-button>
          <el-radio-button value="PREPAY">预付付款</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 对账付款分叉 (RECON) -->
      <template v-if="formData.applyType === 'RECON'">
        <el-form-item label="已确认对账单" prop="reconId">
          <el-select
            v-model="formData.reconId"
            placeholder="请选择已确认的对账单"
            filterable
            class="w-full"
            :disabled="isEdit"
            @change="handleReconSelect"
          >
            <el-option
              v-for="item in confirmedReconOptions"
              :key="item.id"
              :label="`${item.reconNo} (${item.supplierName}) - 确认金额 ￥${formatAmount(item.totalAmount)}`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <!-- 对账单指标卡片 -->
        <div v-if="selectedRecon" class="mb-18px p-12px bg-[#f4f7fa] rounded border border-[#e2e8f0]">
          <div class="text-13px font-bold text-[#334155] mb-8px flex items-center justify-between">
            <span>对账单信息：{{ selectedRecon.reconNo }}</span>
            <span class="text-xs text-gray-500">供应商：{{ selectedRecon.supplierName }}</span>
          </div>
          <div class="grid grid-cols-3 gap-2 text-xs">
            <div class="bg-white p-2 rounded border border-[#e2e8f0]">
              <span class="text-gray-500 block">确认金额合计</span>
              <span class="text-sm font-semibold text-blue-600">￥{{ formatAmount(selectedRecon.totalAmount) }}</span>
            </div>
            <div class="bg-white p-2 rounded border border-[#e2e8f0]">
              <span class="text-gray-500 block">累计已申请</span>
              <span class="text-sm font-semibold text-orange-600">￥{{ formatAmount(selectedRecon.appliedAmount || 0) }}</span>
            </div>
            <div class="bg-white p-2 rounded border border-[#e2e8f0]">
              <span class="text-gray-500 block">剩余可申请上限</span>
              <span class="text-sm font-semibold text-green-600">￥{{ formatAmount(reconRemainingAmount) }}</span>
            </div>
          </div>
        </div>

        <el-form-item label="申请金额" prop="amount">
          <el-input-number
            v-model="formData.amount"
            :min="0.01"
            :max="reconRemainingAmount > 0 ? reconRemainingAmount : undefined"
            :precision="2"
            :step="100"
            class="!w-240px"
          />
          <span class="ml-2 text-xs text-gray-500">
            (上限: ￥{{ formatAmount(reconRemainingAmount) }})
          </span>
        </el-form-item>
      </template>

      <!-- 预付付款分叉 (PREPAY) -->
      <template v-else>
        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="formData.supplierId"
            placeholder="搜索供应商"
            filterable
            remote
            reserve-keyword
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            class="w-full"
            :disabled="isEdit"
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

        <!-- 供应商预付余额卡片 -->
        <div v-if="formData.supplierId" class="mb-18px p-12px bg-[#f4f7fa] rounded border border-[#e2e8f0]">
          <div class="flex items-center justify-between text-xs">
            <span class="text-gray-600">当前供应商预付余额（参考）：</span>
            <span class="text-sm font-bold text-green-600">￥{{ formatAmount(supplierPrepayBalance) }}</span>
          </div>
          <div v-if="selectedSupplierBank" class="text-xs text-gray-500 mt-2 pt-2 border-t border-[#e2e8f0]">
            收款开户行：{{ selectedSupplierBank.bankName || '未维护' }} | 账号：{{ selectedSupplierBank.bankAccount || '未维护' }}
          </div>
        </div>

        <el-form-item label="预付用途" prop="purpose">
          <el-input
            v-model="formData.purpose"
            placeholder="请输入预付款用途，如：纱线采购定金"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="申请金额" prop="amount">
          <el-input-number
            v-model="formData.amount"
            :min="0.01"
            :precision="2"
            :step="100"
            class="!w-240px"
          />
        </el-form-item>
      </template>

      <el-form-item label="备注说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注说明（选填）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import * as PaymentApi from '@/api/lunaerp-elmui/finance/payment'
import type { PaymentApplyVO, PaymentApplySaveReqVO } from '@/api/lunaerp-elmui/finance/payment'
import * as SupplierReconApi from '@/api/lunaerp-elmui/finance/supplier-recon'
import type { SupplierReconVO } from '@/api/lunaerp-elmui/finance/supplier-recon'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import { formatAmount } from '../utils'

const emit = defineEmits(['success'])
const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref()

interface FormDataState {
  id?: number
  applyType: string
  supplierId: number | undefined
  reconId: number | undefined
  amount: number | undefined
  purpose: string
  remark: string
}

const formData = reactive<FormDataState>({
  id: undefined,
  applyType: 'RECON',
  supplierId: undefined,
  reconId: undefined,
  amount: undefined,
  purpose: '',
  remark: ''
})

const confirmedReconOptions = ref<SupplierReconVO[]>([])
const selectedRecon = ref<SupplierReconVO | null>(null)
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const supplierPrepayBalance = ref<number>(0)
const selectedSupplierBank = ref<{ bankName?: string; bankAccount?: string } | null>(null)

// 剩余可申请上限
const reconRemainingAmount = computed(() => {
  if (!selectedRecon.value) return 0
  const total = selectedRecon.value.totalAmount || 0
  const applied = selectedRecon.value.appliedAmount || 0
  // 编辑态时加回当前申请额
  const currentSelf = isEdit.value ? (formData.amount || 0) : 0
  const remaining = total - applied + currentSelf
  return Math.max(0, Number(remaining.toFixed(2)))
})

const formRules = reactive({
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  reconId: [{ required: true, message: '请选择已确认对账单', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入申请金额', trigger: 'blur' },
    {
      validator: (_rule: any, value: number, callback: any) => {
        if (!value || value <= 0) {
          callback(new Error('申请金额必须大于 0'))
        } else if (formData.applyType === 'RECON' && reconRemainingAmount.value > 0 && value > reconRemainingAmount.value) {
          callback(new Error(`申请金额不能超过剩余可申请上限(￥${formatAmount(reconRemainingAmount.value)})`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  purpose: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (formData.applyType === 'PREPAY' && (!value || !value.trim())) {
          callback(new Error('预付付款必须填写预付用途'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

/** 切换类型 */
const handleTypeChange = () => {
  formData.reconId = undefined
  selectedRecon.value = null
  formData.amount = undefined
  formData.purpose = ''
  if (formData.applyType === 'RECON') {
    loadConfirmedRecons()
  } else if (formData.supplierId) {
    handleSupplierChange(formData.supplierId)
  }
}

/** 供应商搜索 */
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}

/** 选定供应商变更 */
const handleSupplierChange = async (supplierId: number) => {
  if (!supplierId) {
    supplierPrepayBalance.value = 0
    selectedSupplierBank.value = null
    return
  }
  try {
    supplierPrepayBalance.value = await PaymentApi.getPrepayBalance(supplierId)
    const sup = supplierOptions.value.find((s) => s.id === supplierId)
    if (sup) {
      selectedSupplierBank.value = {
        bankName: sup.bankName,
        bankAccount: sup.bankAccount
      }
    }
  } catch {}
}

/** 加载已确认的对账单列表 */
const loadConfirmedRecons = async (supplierId?: number) => {
  try {
    const res = await SupplierReconApi.getSupplierReconPage({
      pageNo: 1,
      pageSize: 50,
      status: 'CONFIRMED',
      supplierId
    })
    confirmedReconOptions.value = res.list
  } catch {}
}

/** 选中对账单 */
const handleReconSelect = (reconId: number) => {
  const recon = confirmedReconOptions.value.find((r) => r.id === reconId)
  if (recon) {
    selectedRecon.value = recon
    formData.supplierId = recon.supplierId
    // 默认预填剩余可申请额
    const total = recon.totalAmount || 0
    const applied = recon.appliedAmount || 0
    const remaining = Math.max(0, Number((total - applied).toFixed(2)))
    formData.amount = remaining
  }
}

/** 打开弹窗 */
const open = async (type: 'create' | 'edit', data?: Partial<PaymentApplyVO> & { initialRecon?: SupplierReconVO }) => {
  dialogVisible.value = true
  isEdit.value = type === 'edit'
  formLoading.value = true
  try {
    await searchSuppliers('')
    await loadConfirmedRecons(data?.supplierId)

    if (type === 'create') {
      formData.id = undefined
      formData.applyType = data?.applyType || 'RECON'
      formData.supplierId = data?.supplierId
      formData.reconId = data?.reconId
      formData.amount = data?.amount
      formData.purpose = data?.purpose || ''
      formData.remark = data?.remark || ''

      if (formData.reconId) {
        // 如果传入了 reconId，自动选中
        const matched = confirmedReconOptions.value.find((r) => r.id === formData.reconId)
        if (matched) {
          selectedRecon.value = matched
          formData.supplierId = matched.supplierId
          if (!formData.amount) {
            const remaining = (matched.totalAmount || 0) - (matched.appliedAmount || 0)
            formData.amount = Math.max(0, Number(remaining.toFixed(2)))
          }
        }
      }

      if (formData.applyType === 'PREPAY' && formData.supplierId) {
        await handleSupplierChange(formData.supplierId)
      }
    } else if (data) {
      formData.id = data.id
      formData.applyType = data.applyType || 'RECON'
      formData.supplierId = data.supplierId
      formData.reconId = data.reconId
      formData.amount = data.amount
      formData.purpose = data.purpose || ''
      formData.remark = data.remark || ''

      if (formData.applyType === 'RECON' && formData.reconId) {
        try {
          selectedRecon.value = await SupplierReconApi.getSupplierRecon(formData.reconId)
        } catch {}
      } else if (formData.applyType === 'PREPAY' && formData.supplierId) {
        await handleSupplierChange(formData.supplierId)
      }
    }
  } finally {
    formLoading.value = false
  }
}

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    const payload: PaymentApplySaveReqVO = {
      id: formData.id,
      applyType: formData.applyType,
      supplierId: formData.supplierId,
      reconId: formData.applyType === 'RECON' ? formData.reconId : undefined,
      amount: formData.amount!,
      purpose: formData.applyType === 'PREPAY' ? formData.purpose : undefined,
      remark: formData.remark
    }

    if (isEdit.value) {
      await PaymentApi.updatePaymentApply(payload)
      message.success('更新付款申请成功')
    } else {
      await PaymentApi.createPaymentApply(payload)
      message.success('创建付款申请成功')
    }
    dialogVisible.value = false
    emit('success')
  } catch (err: any) {
    const errCode = err?.response?.data?.code
    const msg = err?.response?.data?.msg || err?.message || '保存失败'
    // 022_007 守卫报错：累计申请金额超出对账确认金额
    if (errCode === 1030022007 || msg.includes('超出对账确认金额')) {
      await ElMessageBox.alert(msg, '申请金额超额', {
        type: 'error',
        confirmButtonText: '我知道了'
      })
    } else {
      message.error(msg)
    }
  } finally {
    submitLoading.value = false
  }
}

defineExpose({ open })
</script>
