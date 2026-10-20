<template>
  <el-drawer
    v-model="drawerVisible"
    :title="drawerTitle"
    size="85%"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div v-loading="loading" class="flex flex-col h-full">
      <!-- 1. 负值严重警告横幅（025_011 门禁） -->
      <el-alert
        v-if="negativeWarning"
        type="error"
        :closable="false"
        show-icon
        class="mb-14px !border-red-400"
      >
        <template #title>
          <span class="font-bold text-13px">{{ negativeWarning }}</span>
        </template>
        <template #default>
          <div class="text-12px mt-2px">
            员工实发合计（应发 -
            扣款）不可为负数；请调整扣款金额、计件单价或增加补贴后方可保存提交。
          </div>
        </template>
      </el-alert>

      <!-- 2. 状态与金额指标看板 -->
      <div class="bg-gray-50 border border-gray-200 rounded p-14px mb-16px">
        <div class="flex items-center justify-between mb-12px">
          <div class="flex items-center gap-12px">
            <span v-if="form.payrollNo" class="font-mono text-16px font-bold text-gray-800">
              {{ form.payrollNo }}
            </span>
            <el-tag
              v-if="form.approvalStatus"
              :type="
                form.approvalStatus === 'APPROVED'
                  ? 'success'
                  : form.approvalStatus === 'REJECTED'
                    ? 'danger'
                    : 'info'
              "
              size="default"
              class="font-bold"
            >
              {{
                form.approvalStatus === 'APPROVED'
                  ? '已通过'
                  : form.approvalStatus === 'APPROVING'
                    ? '审批中'
                    : form.approvalStatus === 'REJECTED'
                      ? '已驳回'
                      : form.approvalStatus === 'CANCELLED'
                        ? '已作废'
                        : '草稿'
              }}
            </el-tag>
            <el-tag
              v-if="form.approvalStatus === 'APPROVED'"
              :type="
                form.paymentStatus === 'PAID'
                  ? 'success'
                  : form.paymentStatus === 'PARTIAL'
                    ? 'warning'
                    : 'info'
              "
              size="default"
            >
              {{
                form.paymentStatus === 'PAID'
                  ? '已发清'
                  : form.paymentStatus === 'PARTIAL'
                    ? '部分发放'
                    : '未发放'
              }}
            </el-tag>
          </div>

          <!-- 工具按钮栏 -->
          <div class="flex items-center gap-8px">
            <el-button
              v-if="isEditable"
              type="danger"
              plain
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:import-defect']"
              @click="openDefectImport"
            >
              <Icon icon="ep:download" class="mr-4px" />
              导入次品扣款
            </el-button>
            <el-button
              v-if="isEditable"
              type="primary"
              plain
              size="small"
              @click="handleAddEmployee"
            >
              <Icon icon="ep:plus" class="mr-4px" />
              添加员工行
            </el-button>
            <el-button
              v-if="form.id && form.approvalStatus === 'APPROVED'"
              type="success"
              size="small"
              v-hasPermi="['lunaerp:finance:payroll:pay']"
              :disabled="form.paymentStatus === 'PAID'"
              @click="showPayDialog = true"
            >
              <Icon icon="ep:money" class="mr-4px" />
              发放登记
            </el-button>
            <el-button
              v-if="form.id && (form.payments?.length || form.approvalStatus === 'APPROVED')"
              type="info"
              plain
              size="small"
              @click="showPaymentList = true"
            >
              <Icon icon="ep:list" class="mr-4px" />
              发放批次 ({{ form.payments?.length || 0 }})
            </el-button>
            <PrintDispatcher
              v-if="form.id && form.approvalStatus === 'APPROVED'"
              biz-type="PAYROLL"
              :biz-id="form.id"
              class="inline-block"
            >
              <template #default="{ open }">
                <el-button
                  type="warning"
                  plain
                  size="small"
                  v-hasPermi="['lunaerp:finance:payroll:print']"
                  @click="open()"
                >
                  <Icon icon="ep:printer" class="mr-4px" />
                  套打工资表
                </el-button>
              </template>
            </PrintDispatcher>
          </div>
        </div>

        <!-- 汇总指标卡片 -->
        <div class="grid grid-cols-5 gap-12px text-center">
          <div class="bg-white p-10px rounded border border-gray-200">
            <div class="text-11px text-gray-500 mb-2px">员工总人数</div>
            <div class="text-18px font-bold text-gray-800 font-mono">
              {{ form.employees?.length || 0 }} 人
            </div>
          </div>
          <div class="bg-white p-10px rounded border border-gray-200">
            <div class="text-11px text-gray-500 mb-2px">应发总计</div>
            <div class="text-18px font-bold text-gray-800 font-mono">
              ¥ {{ formatAmount(payrollTotals.earningTotal) }}
            </div>
          </div>
          <div class="bg-white p-10px rounded border border-gray-200">
            <div class="text-11px text-gray-500 mb-2px">扣款总计</div>
            <div class="text-18px font-bold text-red-600 font-mono">
              ¥ {{ formatAmount(payrollTotals.deductionTotal) }}
            </div>
          </div>
          <div class="bg-white p-10px rounded border border-gray-200">
            <div class="text-11px text-gray-500 mb-2px">实发总计</div>
            <div
              class="text-18px font-bold font-mono"
              :class="payrollTotals.netAmount < 0 ? 'text-red-600 underline' : 'text-blue-700'"
            >
              ¥ {{ formatAmount(payrollTotals.netAmount) }}
            </div>
          </div>
          <div class="bg-white p-10px rounded border border-gray-200">
            <div class="text-11px text-gray-500 mb-2px">
              {{ form.approvalStatus === 'APPROVED' ? '未发余额 / 已发' : '状态' }}
            </div>
            <div
              v-if="form.approvalStatus === 'APPROVED'"
              class="text-14px font-bold text-green-700 font-mono"
            >
              未发: ¥{{ formatAmount(payrollTotals.unpaidAmount) }}
            </div>
            <div v-else class="text-14px font-medium text-gray-400 mt-2px"> 审批通过后可发 </div>
          </div>
        </div>
      </div>

      <!-- 3. 主表信息行（期间、标题、备注） -->
      <el-form :model="form" label-width="80px" class="mb-14px">
        <div class="grid grid-cols-3 gap-16px">
          <el-form-item label="工资期间" required>
            <el-date-picker
              v-if="isCreate"
              v-model="form.period"
              type="month"
              value-format="YYYY-MM"
              placeholder="选择月份"
              class="!w-1/1"
              @change="handlePeriodChange"
            />
            <el-tag v-else type="primary" class="font-mono font-bold">{{ form.period }}</el-tag>
          </el-form-item>

          <el-form-item label="工资标题">
            <el-input
              v-if="isEditable"
              v-model="form.title"
              placeholder="例如：2026-10 工资表"
              maxlength="100"
            />
            <span v-else class="text-13px text-gray-800">{{ form.title }}</span>
          </el-form-item>

          <el-form-item label="备注说明">
            <el-input
              v-if="isEditable"
              v-model="form.remark"
              placeholder="备注说明"
              maxlength="200"
            />
            <span v-else class="text-13px text-gray-600">{{ form.remark || '—' }}</span>
          </el-form-item>
        </div>
      </el-form>

      <!-- 4. 员工汇总与款项明细展开表格 -->
      <div class="flex-1 overflow-y-auto">
        <el-table
          :data="form.employees"
          border
          style="width: 100%"
          row-key="userId"
          default-expand-all
        >
          <!-- 展开款项明细行 -->
          <el-table-column type="expand" width="48">
            <template #default="{ row: emp }">
              <div class="p-12px bg-gray-50 border-t border-b border-gray-200">
                <div class="flex items-center justify-between mb-8px">
                  <div class="text-12px font-bold text-gray-700 flex items-center gap-6px">
                    <Icon icon="ep:list" class="text-primary" />
                    <span>员工【{{ emp.nickname || `用户${emp.userId || ''}` }}】款项明细列表</span>
                  </div>
                  <el-button
                    v-if="isEditable"
                    type="primary"
                    link
                    size="small"
                    @click="handleAddLineItem(emp)"
                  >
                    <Icon icon="ep:plus" class="mr-2px" />
                    添加款项行
                  </el-button>
                </div>

                <!-- 款项明细表格 -->
                <el-table :data="emp.items" size="small" border class="bg-white">
                  <el-table-column label="款项大类" width="130">
                    <template #default="{ row: item }">
                      <el-select
                        v-if="isEditable && item.itemType !== 'DEFECT'"
                        v-model="item.itemKind"
                        size="small"
                        class="!w-1/1"
                        @change="handleItemKindChange(item)"
                      >
                        <el-option label="收入项 (EARNING)" value="EARNING" />
                        <el-option label="扣款项 (DEDUCTION)" value="DEDUCTION" />
                      </el-select>
                      <el-tag
                        v-else
                        :type="item.itemKind === 'EARNING' ? 'success' : 'danger'"
                        size="small"
                      >
                        {{ item.itemKind === 'EARNING' ? '收入项' : '扣款项' }}
                      </el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column label="款项类型" width="160">
                    <template #default="{ row: item }">
                      <el-select
                        v-if="isEditable && item.itemType !== 'DEFECT'"
                        v-model="item.itemType"
                        size="small"
                        class="!w-1/1"
                      >
                        <template v-if="item.itemKind === 'EARNING'">
                          <el-option
                            v-for="d in getDictOptions(DICT_TYPE.LUNAERP_PAYROLL_EARNING_TYPE)"
                            :key="d.value"
                            :label="d.label"
                            :value="d.value"
                          />
                        </template>
                        <template v-else>
                          <el-option
                            v-for="d in getDictOptions(
                              DICT_TYPE.LUNAERP_PAYROLL_DEDUCTION_TYPE
                            ).filter((x) => x.value !== 'DEFECT')"
                            :key="d.value"
                            :label="d.label"
                            :value="d.value"
                          />
                        </template>
                      </el-select>
                      <el-tag
                        v-else
                        :type="item.itemType === 'DEFECT' ? 'danger' : 'info'"
                        size="small"
                        class="font-bold"
                      >
                        {{ formatItemTypeLabel(item) }}
                      </el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column label="计件数量" width="110">
                    <template #default="{ row: item }">
                      <el-input-number
                        v-if="isEditable && item.itemType !== 'DEFECT'"
                        v-model="item.qty"
                        :min="0"
                        :precision="2"
                        :controls="false"
                        size="small"
                        placeholder="数量"
                        class="!w-1/1"
                        @input="handleQtyPriceChange(item)"
                      />
                      <span v-else class="font-mono">{{ item.qty ?? '—' }}</span>
                    </template>
                  </el-table-column>

                  <el-table-column label="计件单价 (元)" width="110">
                    <template #default="{ row: item }">
                      <el-input-number
                        v-if="isEditable && item.itemType !== 'DEFECT'"
                        v-model="item.unitPrice"
                        :min="0"
                        :precision="2"
                        :controls="false"
                        size="small"
                        placeholder="单价"
                        class="!w-1/1"
                        @input="handleQtyPriceChange(item)"
                      />
                      <span v-else class="font-mono">{{
                        item.unitPrice ? `¥${formatAmount(item.unitPrice)}` : '—'
                      }}</span>
                    </template>
                  </el-table-column>

                  <el-table-column label="款项金额 (元)" width="140">
                    <template #default="{ row: item }">
                      <el-input-number
                        v-if="isEditable"
                        v-model="item.amount"
                        :min="0.01"
                        :precision="2"
                        :controls="false"
                        size="small"
                        placeholder="金额"
                        class="!w-1/1"
                      />
                      <span
                        v-else
                        class="font-mono font-bold"
                        :class="item.itemKind === 'EARNING' ? 'text-gray-900' : 'text-red-600'"
                      >
                        ¥ {{ formatAmount(item.amount) }}
                      </span>
                    </template>
                  </el-table-column>

                  <el-table-column label="款项说明 / 追溯" min-width="180">
                    <template #default="{ row: item }">
                      <div class="flex items-center gap-8px">
                        <el-input
                          v-if="isEditable"
                          v-model="item.remark"
                          size="small"
                          placeholder="说明"
                          class="!w-1/1"
                        />
                        <span v-else class="text-12px text-gray-600">{{ item.remark || '—' }}</span>

                        <!-- 次品引用逐卷追溯 Popover -->
                        <el-popover
                          v-if="item.itemType === 'DEFECT' && item.defectRefs?.length"
                          placement="top"
                          :width="540"
                          trigger="click"
                        >
                          <template #reference>
                            <el-button link type="danger" size="small" class="whitespace-nowrap">
                              逐卷明细 ({{ item.defectRefs.length }})
                            </el-button>
                          </template>
                          <div class="text-12px font-bold text-gray-700 mb-6px">
                            次品责任布卷追溯快照：
                          </div>
                          <el-table :data="item.defectRefs" size="small" border max-height="240px">
                            <el-table-column
                              label="确认单号"
                              prop="confirmationNo"
                              min-width="120"
                            />
                            <el-table-column label="布卷号" prop="rollNo" width="100" />
                            <el-table-column label="等级" prop="grade" width="60" align="center">
                              <template #default="{ row: r }">
                                <el-tag size="small" type="danger">{{ r.grade }}</el-tag>
                              </template>
                            </el-table-column>
                            <el-table-column label="确认时间" prop="confirmedAt" min-width="140" />
                            <el-table-column label="扣款单价" width="80" align="right">
                              <template #default="{ row: r }">
                                <span class="font-mono text-red-600"
                                  >¥{{ formatAmount(r.deductPrice) }}</span
                                >
                              </template>
                            </el-table-column>
                          </el-table>
                        </el-popover>
                      </div>
                    </template>
                  </el-table-column>

                  <el-table-column v-if="isEditable" label="操作" width="70" align="center">
                    <template #default="{ $index: itemIdx }">
                      <el-button
                        type="danger"
                        link
                        size="small"
                        @click="handleRemoveLineItem(emp, itemIdx)"
                      >
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
          </el-table-column>

          <!-- 员工姓名与选择 -->
          <el-table-column label="员工姓名" min-width="180">
            <template #default="{ row: emp }">
              <UserSelect
                v-if="isEditable"
                v-model="emp.userId"
                placeholder="请选择员工"
                class="!w-1/1"
                @change="handleUserSelectChange(emp, $event)"
              />
              <span v-else class="font-medium text-gray-900">{{
                emp.nickname || `用户${emp.userId}`
              }}</span>
            </template>
          </el-table-column>

          <el-table-column label="应发合计 (元)" width="130" align="right">
            <template #default="{ row: emp }">
              <span class="font-mono font-bold text-gray-800">
                ¥ {{ formatAmount(calculateEmployeeTotals(emp.items).earningTotal) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="扣款合计 (元)" width="130" align="right">
            <template #default="{ row: emp }">
              <span class="font-mono font-bold text-red-600">
                ¥ {{ formatAmount(calculateEmployeeTotals(emp.items).deductionTotal) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="实发工资 (元)" width="150" align="right">
            <template #default="{ row: emp }">
              <div
                class="font-mono font-bold text-14px"
                :class="
                  calculateEmployeeTotals(emp.items).netAmount < 0
                    ? 'text-red-600 bg-red-50 p-2px rounded border border-red-300 inline-block px-6px'
                    : 'text-blue-800'
                "
              >
                ¥ {{ formatAmount(calculateEmployeeTotals(emp.items).netAmount) }}
              </div>
              <div
                v-if="calculateEmployeeTotals(emp.items).netAmount < 0"
                class="text-10px text-red-600"
              >
                负值阻断
              </div>
            </template>
          </el-table-column>

          <el-table-column label="员工备注" min-width="140">
            <template #default="{ row: emp }">
              <el-input
                v-if="isEditable"
                v-model="emp.remark"
                size="small"
                placeholder="员工备注"
              />
              <span v-else class="text-gray-500">{{ emp.remark || '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column v-if="isEditable" label="操作" width="80" align="center">
            <template #default="{ $index: empIdx }">
              <el-button type="danger" link size="small" @click="handleRemoveEmployee(empIdx)">
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 底部抽屉保存提交栏 -->
      <div class="mt-16px pt-12px border-t border-gray-200 flex items-center justify-between">
        <div class="text-12px text-gray-500">
          共 {{ form.employees?.length || 0 }} 名员工明细； 应发: ¥{{
            formatAmount(payrollTotals.earningTotal)
          }}， 扣款: ¥{{ formatAmount(payrollTotals.deductionTotal) }}， 实发: ¥{{
            formatAmount(payrollTotals.netAmount)
          }}
        </div>
        <div class="flex items-center gap-8px">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button
            v-if="isEditable"
            type="primary"
            plain
            :loading="saving"
            v-hasPermi="['lunaerp:finance:payroll:create', 'lunaerp:finance:payroll:update']"
            @click="handleSaveDraft"
          >
            保存草稿
          </el-button>
          <el-button
            v-if="isEditable"
            type="primary"
            :loading="submitting"
            v-hasPermi="['lunaerp:finance:payroll:submit']"
            @click="handleSubmit"
          >
            提交审批
          </el-button>
        </div>
      </div>
    </div>

    <!-- 弹窗组件挂载 -->
    <DefectImportDialog
      v-model="showDefectImport"
      :period="form.period"
      :payroll-id="form.id"
      @confirm="handleDefectImportConfirm"
    />

    <PaymentRegisterDialog
      v-model="showPayDialog"
      :payroll="currentPayrollResp"
      @success="handlePaySuccess"
    />

    <PaymentListDialog v-model="showPaymentList" :payroll="currentPayrollResp" />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import { PrintDispatcher } from '@/components/Print'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import {
  getPayroll,
  createPayroll,
  updatePayroll,
  submitPayroll
} from '@/api/lunaerp-elmui/finance/payroll'
import type {
  PayrollRespVO,
  PayrollEmployeeSaveReqVO,
  PayrollLineItemSaveReqVO
} from '@/api/lunaerp-elmui/finance/payroll'
import {
  formatAmount,
  calculateLineItemAmount,
  calculateEmployeeTotals,
  calculatePayrollTotals,
  validateEmployeeNetAmounts,
  validatePayrollForm,
  getPayrollActionFlags,
  parsePayrollErrorMessage,
  type PayrollEmployeeData,
  type PayrollLineItemData
} from '../model/payroll'
import DefectImportDialog from './DefectImportDialog.vue'
import PaymentRegisterDialog from './PaymentRegisterDialog.vue'
import PaymentListDialog from './PaymentListDialog.vue'

defineOptions({ name: 'PayrollWorkspaceDrawer' })

const props = defineProps<{
  modelValue: boolean
  payrollId?: number
  mode: 'create' | 'edit' | 'view'
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  success: []
}>()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const showDefectImport = ref(false)
const showPayDialog = ref(false)
const showPaymentList = ref(false)
const currentPayrollResp = ref<PayrollRespVO>()

const isCreate = computed(() => props.mode === 'create')
const isEditable = computed(() => {
  if (props.mode === 'create') return true
  if (props.mode === 'edit') {
    const st = form.approvalStatus || 'PENDING'
    return st === 'PENDING' || st === 'REJECTED'
  }
  return false
})

const drawerTitle = computed(() => {
  if (isCreate.value) return '新建员工工资单'
  if (isEditable.value) return `编辑工资单工作台 - ${form.payrollNo || ''}`
  return `工资单详情工作台 - ${form.payrollNo || ''}`
})

const defaultPeriod = () => {
  const d = new Date()
  const m = d.getMonth() + 1
  return `${d.getFullYear()}-${String(m).padStart(2, '0')}`
}

const form = reactive({
  id: undefined as number | undefined,
  payrollNo: '',
  period: defaultPeriod(),
  title: `${defaultPeriod()} 工资表`,
  remark: '',
  approvalStatus: 'PENDING',
  paymentStatus: 'UNPAID',
  paidAmount: 0,
  employees: [] as PayrollEmployeeData[],
  payments: [] as any[]
})

const payrollTotals = computed(() => {
  return calculatePayrollTotals(form.employees, form.paidAmount)
})

const negativeWarning = computed(() => {
  return validateEmployeeNetAmounts(form.employees)
})

const handlePeriodChange = (val: string) => {
  if (val && (!form.title || form.title.includes('工资表'))) {
    form.title = `${val} 工资表`
  }
}

const formatItemTypeLabel = (item: PayrollLineItemData) => {
  if (item.itemType === 'DEFECT') return '次品扣款'
  const dictList =
    item.itemKind === 'EARNING'
      ? getDictOptions(DICT_TYPE.LUNAERP_PAYROLL_EARNING_TYPE)
      : getDictOptions(DICT_TYPE.LUNAERP_PAYROLL_DEDUCTION_TYPE)
  const found = dictList.find((d) => d.value === item.itemType)
  return found?.label || item.itemType
}

const handleItemKindChange = (item: PayrollLineItemData) => {
  if (item.itemKind === 'EARNING') {
    item.itemType = 'BASE'
  } else {
    item.itemType = 'OTHER'
  }
}

const handleQtyPriceChange = (item: PayrollLineItemData) => {
  if (item.qty && item.unitPrice) {
    item.amount = calculateLineItemAmount(item.qty, item.unitPrice)
  }
}

const handleUserSelectChange = (emp: PayrollEmployeeData, userItem: any) => {
  if (userItem) {
    emp.userId = userItem.id
    emp.nickname = userItem.nickname || userItem.username || ''
  }
}

const handleAddEmployee = () => {
  form.employees.push({
    userId: undefined as any,
    nickname: '',
    remark: '',
    items: [
      {
        itemKind: 'EARNING',
        itemType: 'BASE',
        amount: 3000,
        remark: '基本工资'
      }
    ]
  })
}

const handleRemoveEmployee = (idx: number) => {
  form.employees.splice(idx, 1)
}

const handleAddLineItem = (emp: PayrollEmployeeData) => {
  if (!emp.items) emp.items = []
  emp.items.push({
    itemKind: 'EARNING',
    itemType: 'BASE',
    amount: 1000,
    remark: ''
  })
}

const handleRemoveLineItem = (emp: PayrollEmployeeData, idx: number) => {
  emp.items.splice(idx, 1)
}

const openDefectImport = () => {
  if (!form.period) {
    ElMessage.warning('请先选择工资期间')
    return
  }
  showDefectImport.value = true
}

const handleDefectImportConfirm = (
  importedMap: Map<number, PayrollLineItemData>,
  workerNameMap: Map<number, string>
) => {
  for (const [userId, defectItem] of importedMap.entries()) {
    const existingEmp = form.employees.find((e) => e.userId === userId)
    if (existingEmp) {
      // 移除原有的 DEFECT 行，并追加最新的
      existingEmp.items = existingEmp.items.filter((i) => i.itemType !== 'DEFECT')
      existingEmp.items.push(defectItem)
    } else {
      form.employees.push({
        userId,
        nickname: workerNameMap.get(userId) || `用户${userId}`,
        remark: '次品扣款导入',
        items: [defectItem]
      })
    }
  }
}

const buildSavePayload = () => {
  return {
    period: form.period,
    title: form.title,
    remark: form.remark,
    employees: form.employees.map((emp) => ({
      userId: emp.userId,
      remark: emp.remark,
      items: emp.items.map((item) => ({
        itemKind: item.itemKind,
        itemType: item.itemType,
        qty: item.qty,
        unitPrice: item.unitPrice,
        amount: item.amount,
        remark: item.remark,
        defectRefs: item.defectRefs?.map((r) => ({
          userId: r.userId,
          nickname: r.nickname,
          confirmationId: r.confirmationId,
          confirmationNo: r.confirmationNo,
          rollNo: r.rollNo,
          grade: r.grade,
          confirmedAt: r.confirmedAt,
          deductPrice: r.deductPrice
        }))
      }))
    }))
  }
}

const handleSaveDraft = async () => {
  const err = validatePayrollForm(form)
  if (err) {
    ElMessage.warning(err)
    return
  }

  saving.value = true
  try {
    const payload = buildSavePayload()
    if (isCreate.value) {
      const newId = await createPayroll(payload)
      ElMessage.success('工资单草稿创建成功')
      form.id = newId
    } else {
      await updatePayroll({
        id: form.id!,
        ...payload
      })
      ElMessage.success('工资单保存成功')
    }
    emit('success')
    drawerVisible.value = false
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  const err = validatePayrollForm(form)
  if (err) {
    ElMessage.warning(err)
    return
  }

  await ElMessageBox.confirm('确认提交当前工资单进入审批流程？提交后将无法修改。', '提交确认', {
    confirmButtonText: '确定提交',
    cancelButtonText: '取消',
    type: 'warning'
  })

  submitting.value = true
  try {
    const payload = buildSavePayload()
    let targetId = form.id
    if (isCreate.value) {
      targetId = await createPayroll(payload)
      form.id = targetId
    } else {
      await updatePayroll({
        id: form.id!,
        ...payload
      })
    }

    await submitPayroll(targetId!)
    ElMessage.success('工资单已提交审批')
    emit('success')
    drawerVisible.value = false
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

const handlePaySuccess = () => {
  if (form.id) {
    loadPayrollData(form.id)
  }
  emit('success')
}

const loadPayrollData = async (id: number) => {
  loading.value = true
  try {
    const res = await getPayroll(id)
    currentPayrollResp.value = res
    form.id = res.id
    form.payrollNo = res.payrollNo
    form.period = res.period
    form.title = res.title
    form.remark = res.remark || ''
    form.approvalStatus = res.approvalStatus
    form.paymentStatus = res.paymentStatus
    form.paidAmount = Number(res.paidAmount) || 0
    form.payments = res.payments || []

    form.employees = (res.employees || []).map((emp) => ({
      id: emp.id,
      userId: emp.userId,
      nickname: emp.nickname,
      earningTotal: Number(emp.earningTotal) || 0,
      deductionTotal: Number(emp.deductionTotal) || 0,
      netAmount: Number(emp.netAmount) || 0,
      remark: emp.remark || '',
      items: (emp.items || []).map((i) => ({
        id: i.id,
        employeeRowId: i.employeeRowId,
        itemKind: i.itemKind,
        itemType: i.itemType,
        qty: i.qty ? Number(i.qty) : undefined,
        unitPrice: i.unitPrice ? Number(i.unitPrice) : undefined,
        amount: Number(i.amount) || 0,
        remark: i.remark || '',
        defectRefs: (i.defectRefs || []).map((r) => ({
          id: r.id,
          lineItemId: r.lineItemId,
          userId: r.userId,
          nickname: r.nickname,
          confirmationId: r.confirmationId,
          confirmationNo: r.confirmationNo,
          rollNo: r.rollNo,
          grade: r.grade,
          confirmedAt: r.confirmedAt,
          deductPrice: Number(r.deductPrice) || 0
        }))
      }))
    }))
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      if (props.payrollId) {
        loadPayrollData(props.payrollId)
      } else {
        // 新建初始状态
        currentPayrollResp.value = undefined
        form.id = undefined
        form.payrollNo = ''
        form.period = defaultPeriod()
        form.title = `${defaultPeriod()} 工资表`
        form.remark = ''
        form.approvalStatus = 'PENDING'
        form.paymentStatus = 'UNPAID'
        form.paidAmount = 0
        form.payments = []
        form.employees = [
          {
            userId: undefined as any,
            nickname: '',
            remark: '',
            items: [
              {
                itemKind: 'EARNING',
                itemType: 'BASE',
                amount: 3000,
                remark: '基本工资'
              }
            ]
          }
        ]
      }
    }
  }
)
</script>
