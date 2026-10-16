<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? `修改付款申请 - 【${editApplyNo}】` : '发起付款申请'"
    width="680px"
    append-to-body
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="115px"
      label-position="right"
    >
      <!-- 申请类型（编辑态只读） -->
      <el-form-item label="申请类型" prop="applyType">
        <el-radio-group v-model="formData.applyType" :disabled="isEdit" @change="handleTypeChange">
          <el-radio-button label="RECON">对账付款 (挂靠对账单)</el-radio-button>
          <el-radio-button label="PREPAY">预付付款 (预付定金/货款)</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- ==================== RECON 对账付款分支 ==================== -->
      <template v-if="formData.applyType === 'RECON'">
        <el-form-item label="选择对账单" prop="reconId">
          <el-select
            v-if="!isEdit"
            v-model="formData.reconId"
            placeholder="搜索并选择已确认的对账单"
            filterable
            remote
            reserve-keyword
            :remote-method="searchReconOrders"
            :loading="reconLoading"
            class="!w-full"
            @change="handleReconChange"
          >
            <el-option
              v-for="item in reconOptions"
              :key="item.id"
              :label="`${item.reconNo} - ${item.supplierName} (确认: ¥${formatMoney(item.totalAmount)} / 余: ¥${formatMoney(calcReconRemainingLimit(item.totalAmount, item.appliedAmount))})`"
              :value="item.id"
              :disabled="calcReconRemainingLimit(item.totalAmount, item.appliedAmount) <= 0"
            />
          </el-select>
          <span v-else class="font-mono font-medium text-gray-800">{{
            selectedRecon?.reconNo || formData.reconNo || '—'
          }}</span>
        </el-form-item>

        <!-- 关联合同对账单信息卡片 -->
        <div
          v-if="selectedRecon"
          class="bg-gray-50 border border-gray-200 rounded p-12px mb-18px text-13px"
        >
          <div class="grid grid-cols-2 gap-y-8px">
            <div
              ><span class="text-gray-500">供应商：</span
              ><span class="font-medium">{{ selectedRecon.supplierName }}</span></div
            >
            <div
              ><span class="text-gray-500">对账单号：</span
              ><span class="font-mono font-medium">{{ selectedRecon.reconNo }}</span></div
            >
            <div
              ><span class="text-gray-500">确认总额：</span
              ><span class="font-mono font-bold text-gray-800"
                >¥ {{ formatMoney(selectedRecon.totalAmount) }}</span
              ></div
            >
            <div
              ><span class="text-gray-500">已申请额：</span
              ><span class="font-mono text-gray-600"
                >¥ {{ formatMoney(selectedRecon.appliedAmount) }}</span
              ></div
            >
            <div class="col-span-2 flex items-center">
              <span class="text-gray-500">可申请上限：</span>
              <span class="font-mono font-bold text-emerald-700 text-15px"
                >¥ {{ formatMoney(remainingLimit) }}</span
              >
              <span v-if="remainingLimit <= 0" class="text-red-500 ml-10px text-12px font-medium"
                >（该对账单已全额申请，不可重复申请）</span
              >
            </div>
          </div>
        </div>

        <el-form-item label="申请金额" prop="amount">
          <el-input-number
            v-model="formData.amount"
            :min="0.01"
            :max="remainingLimit > 0 ? remainingLimit : undefined"
            :precision="2"
            :step="100"
            controls-position="right"
            class="!w-full"
            :disabled="!isEdit && remainingLimit <= 0"
            placeholder="请输入本次申请金额"
          />
          <div class="text-12px text-gray-400 mt-4px">
            默认预填对账单剩余可申请上限，可改小；系统严格校验累计申请额不超过对账单确认金额。
          </div>
        </el-form-item>
      </template>

      <!-- ==================== PREPAY 预付付款分支 ==================== -->
      <template v-if="formData.applyType === 'PREPAY'">
        <div class="grid grid-cols-12 gap-16px mb-12px">
          <div class="col-span-8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                v-if="!isEdit"
                v-model="formData.supplierId"
                placeholder="搜索并选择供应商"
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
                />
              </el-select>
              <span v-else class="font-medium text-gray-800">{{
                formData.supplierName || '—'
              }}</span>
            </el-form-item>

            <el-form-item label="申请金额" prop="amount">
              <el-input-number
                v-model="formData.amount"
                :min="0.01"
                :precision="2"
                :step="500"
                controls-position="right"
                class="!w-full"
                placeholder="请输入预付申请付款金额"
              />
              <div class="text-12px text-gray-400 mt-2px">
                审批通过后可在【付款登记】中执行扣款。
              </div>
            </el-form-item>

            <el-form-item label="预付用途" prop="purpose">
              <el-input
                v-model="formData.purpose"
                placeholder="如：纱线采购定金、大货订金"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="收款开户行" prop="bankName">
              <el-input
                v-model="formData.bankName"
                placeholder="供应商收款开户行快照（可选）"
                maxlength="100"
                :disabled="isEdit"
              />
            </el-form-item>

            <el-form-item label="收款银行账号" prop="bankAccount">
              <el-input
                v-model="formData.bankAccount"
                placeholder="供应商收款银行账号快照（可选）"
                maxlength="50"
                :disabled="isEdit"
              />
            </el-form-item>
          </div>

          <!-- 表单侧栏展示该供应商当前预付余额（只读）与银行信息快照 -->
          <div class="col-span-4">
            <div
              class="bg-amber-50/70 border border-amber-200 rounded p-12px text-13px h-full flex flex-col justify-between"
            >
              <div>
                <div class="flex items-center justify-between mb-8px">
                  <span class="font-bold text-gray-800 text-13px">供应商预付参考（只读）</span>
                  <el-tooltip content="刷新预付余额" placement="top">
                    <el-button
                      v-if="formData.supplierId"
                      link
                      type="primary"
                      size="small"
                      :loading="balanceLoading"
                      @click="fetchPrepayBalance(formData.supplierId)"
                    >
                      <Icon icon="ep:refresh" />
                    </el-button>
                  </el-tooltip>
                </div>

                <div class="mb-12px bg-white/90 p-8px rounded border border-amber-100">
                  <div class="text-gray-500 text-12px mb-2px">当前可用预付余额：</div>
                  <div class="font-mono font-bold text-amber-700 text-16px">
                    ¥ {{ formatMoney(currentPrepayBalance) }}
                  </div>
                </div>

                <div
                  class="bg-white/90 p-8px rounded border border-amber-100 text-12px space-y-4px"
                >
                  <div>
                    <span class="text-gray-500">开户行：</span>
                    <span class="text-gray-800">{{ formData.bankName || '—' }}</span>
                  </div>
                  <div>
                    <span class="text-gray-500">账号脱敏：</span>
                    <span class="font-mono text-gray-800">{{
                      maskBankAccount(formData.bankAccount)
                    }}</span>
                  </div>
                </div>
              </div>

              <div class="text-11px text-gray-400 mt-8px leading-relaxed">
                预付款由出纳在付款登记时执行抵扣；审批通过前不发生资金账户变动。
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 公共备注说明 -->
      <el-form-item label="申请备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="可选输入本次申请的说明与备注"
          maxlength="300"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-x-8px">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          v-if="isEdit && isRejected"
          type="success"
          :loading="submitting"
          v-hasPermi="['lunaerp:finance:payment:apply']"
          @click="handleSubmit(true)"
        >
          保存并提交审批
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="formData.applyType === 'RECON' && !isEdit && remainingLimit <= 0"
          v-hasPermi="['lunaerp:finance:payment:apply']"
          @click="handleSubmit(false)"
        >
          {{ isEdit ? '保存修改' : '确认发起' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type {
  PaymentApplyRespVO,
  PaymentApplyCreateReqVO,
  PaymentApplyUpdateReqVO
} from '@/api/lunaerp-elmui/finance/payment'
import {
  createPaymentApply,
  updatePaymentApply,
  submitPaymentApply,
  getPrepayBalance
} from '@/api/lunaerp-elmui/finance/payment'
import {
  getSupplierReconPage,
  type SupplierReconRespVO
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import {
  formatMoney,
  maskBankAccount,
  calcReconRemainingLimit,
  parsePaymentErrorMessage
} from '../model/payment'

defineOptions({ name: 'PaymentApplyDialog' })

const emit = defineEmits<{
  (e: 'success'): void
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const isRejected = ref(false)
const editId = ref<number>()
const editApplyNo = ref<string>('')

// 对账单相关
const reconLoading = ref(false)
const reconOptions = ref<SupplierReconRespVO[]>([])
const selectedRecon = ref<SupplierReconRespVO | null>(null)

// 供应商相关
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])
const currentPrepayBalance = ref<number>(0)
const balanceLoading = ref(false)

const formData = reactive<{
  applyType: 'RECON' | 'PREPAY'
  supplierId?: number
  supplierName?: string
  reconId?: number
  reconNo?: string
  amount: number
  purpose?: string
  bankName?: string
  bankAccount?: string
  remark?: string
}>({
  applyType: 'RECON',
  supplierId: undefined,
  supplierName: '',
  reconId: undefined,
  reconNo: '',
  amount: 0,
  purpose: '',
  bankName: '',
  bankAccount: '',
  remark: ''
})

const remainingLimit = computed(() => {
  if (selectedRecon.value) {
    return calcReconRemainingLimit(
      selectedRecon.value.totalAmount,
      selectedRecon.value.appliedAmount
    )
  }
  return 0
})

const formRules = reactive<FormRules>({
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  reconId: [
    {
      validator: (_rule, value, callback) => {
        if (formData.applyType === 'RECON' && !value) {
          callback(new Error('请选择关联合同对账单'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  supplierId: [
    {
      validator: (_rule, value, callback) => {
        if (formData.applyType === 'PREPAY' && !value) {
          callback(new Error('请选择供应商'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  purpose: [
    {
      validator: (_rule, value, callback) => {
        if (formData.applyType === 'PREPAY' && (!value || !value.trim())) {
          callback(new Error('预付用途不能为空（如纱线采购定金）'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  amount: [
    { required: true, message: '请输入申请付款金额', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || Number(value) <= 0) {
          callback(new Error('申请付款金额必须大于 0'))
        } else if (
          formData.applyType === 'RECON' &&
          !isEdit.value &&
          remainingLimit.value > 0 &&
          Number(value) > remainingLimit.value
        ) {
          callback(new Error(`申请金额不能超出剩余上限 ¥${formatMoney(remainingLimit.value)}`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

const handleTypeChange = () => {
  formData.reconId = undefined
  selectedRecon.value = null
  formData.supplierId = undefined
  formData.supplierName = ''
  formData.amount = 0
  formData.purpose = ''
  formData.bankName = ''
  formData.bankAccount = ''
  currentPrepayBalance.value = 0
  formRef.value?.clearValidate()
}

/** 搜索 CONFIRMED 对账单 */
const searchReconOrders = async (query: string) => {
  reconLoading.value = true
  try {
    const res = await getSupplierReconPage({
      pageNo: 1,
      pageSize: 30,
      status: 'CONFIRMED',
      keyword: query ? query.trim() : undefined
    })
    reconOptions.value = res.list || []
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    reconLoading.value = false
  }
}

const handleReconChange = (val?: number) => {
  const recon = reconOptions.value.find((r) => r.id === val)
  if (recon) {
    selectedRecon.value = recon
    formData.reconNo = recon.reconNo
    formData.supplierId = recon.supplierId
    formData.supplierName = recon.supplierName
    const limit = calcReconRemainingLimit(recon.totalAmount, recon.appliedAmount)
    formData.amount = limit
  } else {
    selectedRecon.value = null
    formData.amount = 0
  }
}

/** 搜索供应商 */
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query ? query.trim() : undefined
    })
    supplierOptions.value = res.list || []
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = async (val?: number) => {
  if (val) {
    const sup = supplierOptions.value.find((s) => s.id === val)
    if (sup) {
      formData.supplierName = sup.companyName
    }
    await Promise.all([
      fetchPrepayBalance(val),
      SupplierApi.getSupplier(val)
        .then((detail: any) => {
          if (detail && !isEdit.value) {
            if (detail.bankName && !formData.bankName) formData.bankName = detail.bankName
            if (detail.bankAccount && !formData.bankAccount)
              formData.bankAccount = detail.bankAccount
          }
        })
        .catch(() => {})
    ])
  } else {
    currentPrepayBalance.value = 0
  }
}

const fetchPrepayBalance = async (supplierId: number) => {
  balanceLoading.value = true
  try {
    const balance = await getPrepayBalance(supplierId)
    currentPrepayBalance.value = Number(balance || 0)
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    balanceLoading.value = false
  }
}

/** 打开弹窗（新建或编辑） */
const open = async (row?: PaymentApplyRespVO) => {
  dialogVisible.value = true
  submitting.value = false
  selectedRecon.value = null
  currentPrepayBalance.value = 0

  if (row) {
    // 编辑模式
    isEdit.value = true
    isRejected.value = row.approvalStatus === 'REJECTED'
    editId.value = row.id
    editApplyNo.value = row.applyNo
    formData.applyType = (row.applyType as 'RECON' | 'PREPAY') || 'RECON'
    formData.supplierId = row.supplierId
    formData.supplierName = row.supplierName
    formData.reconId = row.reconId
    formData.reconNo = row.reconNo
    formData.amount = Number(row.amount || 0)
    formData.purpose = row.purpose || ''
    formData.bankName = row.bankName || ''
    formData.bankAccount = row.bankAccount || ''
    formData.remark = row.remark || ''

    if (row.applyType === 'PREPAY' && row.supplierId) {
      await fetchPrepayBalance(row.supplierId)
    }
  } else {
    // 新建模式
    isEdit.value = false
    isRejected.value = false
    editId.value = undefined
    editApplyNo.value = ''
    formData.applyType = 'RECON'
    formData.supplierId = undefined
    formData.supplierName = ''
    formData.reconId = undefined
    formData.reconNo = ''
    formData.amount = 0
    formData.purpose = ''
    formData.bankName = ''
    formData.bankAccount = ''
    formData.remark = ''

    // 初始化对账单选项
    searchReconOrders('')
    searchSuppliers('')
  }
}

const handleSubmit = async (autoSubmitApproval = false) => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      const updateData: PaymentApplyUpdateReqVO = {
        id: editId.value,
        amount: formData.amount,
        purpose: formData.applyType === 'PREPAY' ? formData.purpose : undefined,
        remark: formData.remark
      }
      await updatePaymentApply(updateData)
      if (autoSubmitApproval) {
        await submitPaymentApply(editId.value)
        ElMessage.success('付款申请已修改并成功提交审批')
      } else {
        ElMessage.success('付款申请已修改')
      }
    } else {
      const createData: PaymentApplyCreateReqVO = {
        applyType: formData.applyType,
        supplierId: formData.supplierId,
        reconId: formData.applyType === 'RECON' ? formData.reconId : undefined,
        amount: formData.amount,
        purpose: formData.applyType === 'PREPAY' ? formData.purpose : undefined,
        bankName: formData.applyType === 'PREPAY' ? formData.bankName : undefined,
        bankAccount: formData.applyType === 'PREPAY' ? formData.bankAccount : undefined,
        remark: formData.remark
      }
      const newId = await createPaymentApply(createData)
      if (autoSubmitApproval && newId) {
        await submitPaymentApply(newId)
        ElMessage.success('付款申请已创建并提交审批')
      } else {
        ElMessage.success('付款申请发起成功')
      }
    }
    dialogVisible.value = false
    emit('success')
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
