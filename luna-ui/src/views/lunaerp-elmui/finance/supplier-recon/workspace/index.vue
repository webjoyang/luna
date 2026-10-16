<template>
  <ContentWrap>
    <!-- 1. 工作台头部与操作栏 -->
    <div
      class="workspace-header flex justify-between items-center mb-16px pb-12px border-b border-gray-200"
    >
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <span v-if="formData.reconNo" class="text-gray-500 font-mono text-14px">
              {{ formData.reconNo }}
            </span>
            <DictTag
              v-if="formData.status"
              :type="DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS"
              :value="formData.status"
            />
          </div>
        </template>
      </el-page-header>

      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

        <!-- 草稿状态（新建或草稿编辑） -->
        <template v-if="isDraft">
          <el-button
            type="primary"
            plain
            :loading="previewLoading"
            @click="handlePreview"
            v-hasPermi="['lunaerp:finance:supplierrecon:create']"
          >
            <Icon icon="ep:search" class="mr-1" /> 取数预览
          </el-button>

          <el-button
            v-if="!isCreateMode"
            type="warning"
            plain
            :loading="actionLoading"
            @click="handleReFetch"
            v-hasPermi="['lunaerp:finance:supplierrecon:update']"
          >
            <Icon icon="ep:refresh" class="mr-1" /> 重取
          </el-button>

          <el-button
            type="info"
            plain
            @click="handleAddAdjustLine"
            v-hasPermi="[
              isCreateMode
                ? 'lunaerp:finance:supplierrecon:create'
                : 'lunaerp:finance:supplierrecon:update'
            ]"
          >
            <Icon icon="ep:plus" class="mr-1" /> 添加调整项
          </el-button>

          <el-button
            type="primary"
            :loading="actionLoading"
            @click="handleSave"
            v-hasPermi="[
              isCreateMode
                ? 'lunaerp:finance:supplierrecon:create'
                : 'lunaerp:finance:supplierrecon:update'
            ]"
          >
            <Icon icon="ep:check" class="mr-1" /> 保存草稿
          </el-button>

          <el-button
            v-if="!isCreateMode"
            type="success"
            :loading="actionLoading"
            @click="handleConfirm"
            v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
          >
            <Icon icon="ep:circle-check" class="mr-1" /> 确认对账单
          </el-button>
        </template>

        <!-- 已确认状态：只读，打印、生成付款申请、作废 -->
        <template v-else-if="formData.status === 'CONFIRMED'">
          <el-button
            type="success"
            plain
            @click="handleOpenPaymentApply"
            v-hasPermi="['lunaerp:finance:supplierrecon:generate']"
          >
            <Icon icon="ep:money" class="mr-1" /> 生成付款申请
          </el-button>

          <PrintDispatcher
            v-if="formData.id"
            biz-type="VENDOR_RECON"
            :biz-id="formData.id"
            class="inline-block"
          >
            <template #default="{ open }">
              <el-button
                type="primary"
                plain
                @click="open()"
                v-hasPermi="['lunaerp:finance:supplierrecon:print']"
              >
                <Icon icon="ep:printer" class="mr-1" /> 打印对账单
              </el-button>
            </template>
          </PrintDispatcher>

          <el-button
            type="warning"
            plain
            :loading="actionLoading"
            @click="handleCancel"
            v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
          >
            <Icon icon="ep:circle-close" class="mr-1" /> 作废
          </el-button>
        </template>
      </div>
    </div>

    <!-- 2. 基本表单区（供应商 + 区间 + 备注） -->
    <el-form
      ref="formRef"
      :model="formData"
      label-width="90px"
      class="bg-gray-50 p-4 rounded mb-16px"
    >
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="供应商" required>
            <el-select
              v-model="formData.supplierId"
              placeholder="搜索并选择供应商"
              filterable
              remote
              reserve-keyword
              :remote-method="searchSuppliers"
              :loading="supplierLoading"
              :disabled="!isDraft"
              class="w-full"
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
        </el-col>

        <el-col :span="8">
          <el-form-item label="对账区间" required>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="对账起始日期"
              end-placeholder="对账截止日期"
              :disabled="!isDraft"
              class="!w-full"
              @change="handleDateRangeChange"
            />
          </el-form-item>
        </el-col>

        <el-col :span="8">
          <el-form-item label="备注说明">
            <el-input
              v-model="formData.remark"
              placeholder="对账单备注说明"
              :disabled="!isDraft"
              maxlength="200"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 3. 三类行混排表格 -->
    <div class="mb-16px flex justify-between items-center">
      <div class="font-bold text-15px flex items-center gap-2">
        <span>对账明细 ({{ itemsList.length }} 行)</span>
        <span
          v-if="!isDraft"
          class="text-12px font-normal text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded"
        >
          当前状态已冻结，整单只读
        </span>
      </div>

      <div class="text-13px text-gray-600 flex items-center gap-4">
        <span
          >加工费: <strong>¥ {{ formatMoney(subtotals.processTotal) }}</strong></span
        >
        <span
          >采购货款: <strong>¥ {{ formatMoney(subtotals.purchaseTotal) }}</strong></span
        >
        <span
          >调整项:
          <strong :class="subtotals.adjustTotal < 0 ? 'text-red-600' : ''"
            >¥ {{ formatMoney(subtotals.adjustTotal) }}</strong
          ></span
        >
        <span class="text-15px font-bold text-black ml-2">
          实时合计:
          <span class="text-emerald-700 font-mono text-16px">¥ {{ formatMoney(totalAmount) }}</span>
        </span>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="itemsList"
      border
      stripe
      class="recon-table"
      empty-text="区间内无可对账明细"
    >
      <el-table-column type="index" label="#" width="50" align="center" />

      <!-- 行类型标签 -->
      <el-table-column label="类型" width="95" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.lineType === 'PROCESS'" type="primary" size="small"> 加工费 </el-tag>
          <el-tag v-else-if="row.lineType === 'PURCHASE'" type="success" size="small">
            采购货款
          </el-tag>
          <el-tag v-else-if="row.lineType === 'ADJUST'" type="warning" size="small">
            调整项
          </el-tag>
        </template>
      </el-table-column>

      <!-- 来源单据与单号 -->
      <el-table-column label="来源单号 / 说明" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.lineType === 'PROCESS'">
            <div class="flex items-center gap-1">
              <el-link
                type="primary"
                :underline="true"
                @click="jumpToInspection(row.inspectionOrderId)"
                class="font-mono font-medium"
              >
                {{ row.inspectionOrderNo || '查看检验单' }}
              </el-link>
              <el-tag size="small" type="info">
                {{ row.inspectionType === 'GREIGE' ? '胚布·织费' : '成品·染费' }}
              </el-tag>
            </div>
          </template>

          <template v-else-if="row.lineType === 'PURCHASE'">
            <div>
              <el-link
                type="primary"
                :underline="true"
                @click="jumpToStockIn(row.stockInId)"
                class="font-mono font-medium"
              >
                {{ row.stockInNo || '查看入库单' }}
              </el-link>
              <div v-if="row.yarnOrderNo" class="text-xs text-gray-400 font-mono">
                纱线采购单: {{ row.yarnOrderNo }}
              </div>
            </div>
          </template>

          <!-- 调整项：原因备注必填输入 -->
          <template v-else-if="row.lineType === 'ADJUST'">
            <div v-if="isDraft" class="flex items-center gap-1">
              <span class="text-red-500 font-bold">*</span>
              <el-input
                v-model="row.adjustReason"
                placeholder="必填调整原因，如质量扣款、运费补差"
                size="small"
                class="w-full"
              />
            </div>
            <span v-else class="text-red-600 font-medium">{{ row.adjustReason || '—' }}</span>
          </template>
        </template>
      </el-table-column>

      <!-- 产品 -->
      <el-table-column label="产品" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.lineType !== 'ADJUST'">
            <div class="font-medium">{{ row.productName || '—' }}</div>
            <div v-if="row.productCode" class="text-xs text-gray-400 font-mono">
              {{ row.productCode }}
            </div>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <!-- 批次缸号 -->
      <el-table-column label="批次/缸号" prop="batchNo" width="115" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-mono text-12px">{{ row.batchNo || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 数量明细 -->
      <el-table-column label="数量明细" width="145" align="right">
        <template #default="{ row }">
          <template v-if="row.lineType === 'PROCESS'">
            <div class="text-12px">
              <span>{{ row.qtyKg != null ? `${row.qtyKg} KG` : '—' }}</span>
              <span class="text-gray-400 mx-1">/</span>
              <span>{{ row.qtyM != null ? `${row.qtyM} M` : '—' }}</span>
            </div>
            <div v-if="row.rollCount" class="text-11px text-gray-500"> {{ row.rollCount }} 卷 </div>
          </template>
          <template v-else-if="row.lineType === 'PURCHASE'">
            <span class="font-mono"
              >{{ row.billQty ?? row.qtyKg ?? row.qtyM ?? '—' }} {{ row.qtyUnit || '' }}</span
            >
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <!-- 计价基准切换 (KG / METER) -->
      <el-table-column label="计价基准" width="135" align="center">
        <template #default="{ row }">
          <template v-if="row.lineType === 'PROCESS'">
            <el-select
              v-if="isDraft"
              v-model="row.pricingBasis"
              size="small"
              class="!w-110px"
              @change="handlePricingBasisChange(row)"
            >
              <el-option label="按米 (M)" value="METER" />
              <el-option label="按千克 (KG)" value="KG" />
            </el-select>
            <el-tag
              v-else
              size="small"
              :type="row.pricingBasis === 'METER' ? 'success' : 'primary'"
            >
              {{ row.pricingBasis === 'METER' ? '按米 (M)' : '按千克 (KG)' }}
            </el-tag>
          </template>
          <template v-else-if="row.lineType === 'PURCHASE'">
            <span class="text-gray-600">{{ row.qtyUnit || row.pricingBasis || '—' }}</span>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <!-- 结算量 (billQty) -->
      <el-table-column label="结算量" prop="billQty" width="95" align="right">
        <template #default="{ row }">
          <span v-if="row.lineType !== 'ADJUST'" class="font-mono font-medium">
            {{ row.billQty != null ? row.billQty : '—' }}
          </span>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <!-- 单价与产品参考价预填 -->
      <el-table-column label="单价(元)" width="155" align="right">
        <template #default="{ row }">
          <template v-if="row.lineType !== 'ADJUST'">
            <div v-if="isDraft" class="flex flex-col items-end gap-1">
              <el-input-number
                v-model="row.unitPrice"
                :min="0"
                :precision="4"
                :step="0.1"
                size="small"
                controls-position="right"
                class="!w-120px"
                @change="handleLinePriceChange(row)"
              />
              <!-- 产品档案加工费参考展示与一键带入 -->
              <div
                v-if="row.referenceFee != null"
                class="text-11px text-blue-600 cursor-pointer hover:underline"
                @click="applyReferenceFee(row)"
                :title="`点击预填${row.referenceFeeLabel}参考价 ¥${row.referenceFee}`"
              >
                参考{{ row.referenceFeeLabel }}: ¥{{ row.referenceFee }}
              </div>
            </div>
            <span v-else class="font-mono">
              {{ row.unitPrice != null ? `¥ ${Number(row.unitPrice).toFixed(4)}` : '—' }}
            </span>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <!-- 行金额 (lineAmount) -->
      <el-table-column label="行金额(元)" width="130" align="right">
        <template #default="{ row }">
          <!-- ADJUST 行直接输入正负金额 -->
          <template v-if="row.lineType === 'ADJUST'">
            <el-input-number
              v-if="isDraft"
              v-model="row.lineAmount"
              :precision="2"
              :step="100"
              size="small"
              controls-position="right"
              class="!w-115px"
              placeholder="正/负金额"
              @change="handleAdjustAmountChange(row)"
            />
            <span
              v-else
              class="font-mono font-bold"
              :class="Number(row.lineAmount || 0) < 0 ? 'text-red-600' : 'text-gray-900'"
            >
              ¥ {{ formatMoney(row.lineAmount) }}
            </span>
          </template>
          <!-- PROCESS / PURCHASE 实时重算展示 -->
          <template v-else>
            <span class="font-mono font-bold text-gray-900">
              ¥ {{ formatMoney(row.lineAmount) }}
            </span>
          </template>
        </template>
      </el-table-column>

      <!-- 备注说明 -->
      <el-table-column label="行备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <el-input v-if="isDraft" v-model="row.remark" placeholder="行备注" size="small" />
          <span v-else>{{ row.remark || '—' }}</span>
        </template>
      </el-table-column>

      <!-- 操作列（草稿态可删除明细行） -->
      <el-table-column v-if="isDraft" label="操作" width="70" align="center" fixed="right">
        <template #default="{ $index }">
          <el-button
            link
            type="danger"
            size="small"
            @click="handleRemoveLine($index)"
            v-hasPermi="[
              isCreateMode
                ? 'lunaerp:finance:supplierrecon:create'
                : 'lunaerp:finance:supplierrecon:update'
            ]"
          >
            <Icon icon="ep:delete" />
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <div class="py-10 text-center">
          <el-empty description="区间内无可对账明细，请选择其他区间或供应商重新取数预览" />
        </div>
      </template>
    </el-table>

    <!-- 底部固定栏 -->
    <div
      class="mt-16px p-4 bg-gray-50 border border-gray-200 rounded flex justify-between items-center"
    >
      <div class="text-13px text-gray-600">
        共计 <strong>{{ itemsList.length }}</strong> 行对账明细，保存后由服务端重算最终权威金额。
      </div>

      <div class="flex items-center gap-4">
        <span class="text-16px font-bold text-black">
          确认总金额:
          <span class="text-20px font-mono text-emerald-700 ml-1"
            >¥ {{ formatMoney(totalAmount) }}</span
          >
        </span>

        <el-button
          v-if="isDraft"
          type="primary"
          :loading="actionLoading"
          @click="handleSave"
          v-hasPermi="[
            isCreateMode
              ? 'lunaerp:finance:supplierrecon:create'
              : 'lunaerp:finance:supplierrecon:update'
          ]"
        >
          保存对账单
        </el-button>
      </div>
    </div>

    <!-- 付款申请弹窗 -->
    <SupplierReconPaymentApplyDialog
      ref="paymentApplyDialogRef"
      @success="handlePaymentApplySuccess"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import type {
  SupplierReconRespVO,
  SupplierReconItemSaveVO
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import {
  getSupplierRecon,
  previewSupplierRecon,
  createSupplierRecon,
  updateSupplierRecon,
  reFetchSupplierRecon,
  confirmSupplierRecon,
  cancelSupplierRecon
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import {
  getDefaultPreviousMonthRange,
  calcItemBillQty,
  calcItemLineAmount,
  calcTotalAmount,
  formatMoney,
  getProductReferenceFee,
  validateReconDraft,
  parseReconErrorMessage
} from '../model/supplier-recon'
import { calculatePrintSubtotals } from '../components/print/utils'
import PrintDispatcher from '@/components/Print/src/PrintDispatcher.vue'
import SupplierReconPaymentApplyDialog from '../components/SupplierReconPaymentApplyDialog.vue'

defineOptions({ name: 'LunaerpFinanceSupplierReconWorkspace' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const previewLoading = ref(false)
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])
const paymentApplyDialogRef = ref<InstanceType<typeof SupplierReconPaymentApplyDialog>>()

// 页面模式判断
const reconId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})

const isCreateMode = computed(() => !reconId.value)
const isDraft = computed(() => isCreateMode.value || formData.status === 'DRAFT')

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建供应商对账单'
  if (formData.status === 'DRAFT') return '编辑供应商对账单'
  return '供应商对账单详情'
})

// 表单主体数据
const formData = reactive({
  id: undefined as number | undefined,
  reconNo: '',
  supplierId: undefined as number | undefined,
  supplierName: '',
  periodStart: '',
  periodEnd: '',
  status: 'DRAFT',
  totalAmount: 0,
  appliedAmount: 0,
  remark: ''
})

// 区间选择器绑定
const dateRange = ref<[string, string]>(getDefaultPreviousMonthRange())

// 明细行列表（带有前端扩展字段如 referenceFee）
interface WorkbenchItem extends SupplierReconItemSaveVO {
  referenceFee?: number | null
  referenceFeeLabel?: string
  inspectionOrderNo?: string
  stockInNo?: string
  batchNo?: string
  rollCount?: number
}

const itemsList = ref<WorkbenchItem[]>([])

// 汇总计算
const totalAmount = computed(() => {
  return calcTotalAmount(itemsList.value)
})

const subtotals = computed(() => {
  return calculatePrintSubtotals(itemsList.value)
})

// 初始化与加载
onMounted(async () => {
  const [defStart, defEnd] = getDefaultPreviousMonthRange()
  formData.periodStart = defStart
  formData.periodEnd = defEnd

  if (reconId.value) {
    await loadReconDetail(reconId.value)
  }
})

// 加载已有对账单详情
const loadReconDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getSupplierRecon(id)
    formData.id = res.id
    formData.reconNo = res.reconNo
    formData.supplierId = res.supplierId
    formData.supplierName = res.supplierName
    formData.periodStart = res.periodStart
    formData.periodEnd = res.periodEnd
    formData.status = res.status
    formData.totalAmount = res.totalAmount
    formData.appliedAmount = res.appliedAmount
    formData.remark = res.remark || ''

    dateRange.value = [res.periodStart, res.periodEnd]

    // 预填供应商选项保持 label 不脱落
    if (res.supplierId && res.supplierName) {
      supplierOptions.value = [{ id: res.supplierId, companyName: res.supplierName } as SupplierVO]
    }

    // 装配明细行
    itemsList.value = (res.items || []).map((it) => ({
      id: it.id,
      lineType: it.lineType,
      sourceRefType: it.sourceRefType,
      sourceRefId: it.sourceRefId,
      sourceRefItemId: it.sourceRefItemId,
      inspectionOrderId: it.inspectionOrderId,
      inspectionOrderNo: it.inspectionOrderNo,
      inspectionType: it.inspectionType,
      stockInId: it.stockInId,
      stockInNo: it.stockInNo,
      stockInItemId: it.stockInItemId,
      yarnOrderId: it.yarnOrderId,
      yarnOrderNo: it.yarnOrderNo,
      productId: it.productId,
      productCode: it.productCode,
      productName: it.productName,
      batchNo: it.batchNo,
      qtyKg: it.qtyKg,
      qtyM: it.qtyM,
      qtyUnit: it.qtyUnit,
      pricingBasis: it.pricingBasis,
      billQty: it.billQty,
      unitPrice: it.unitPrice,
      lineAmount: it.lineAmount,
      adjustReason: it.adjustReason,
      remark: it.remark
    }))

    // 预加载产品加工费参考
    await preloadProductReferenceFees()
  } catch (err: any) {
    ElMessage.error(err?.msg || err?.message || '加载对账单失败')
  } finally {
    loading.value = false
  }
}

// 供应商远程检索
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 50,
      search: query || ''
    })
    const list = res?.list || []
    // 保留当前已选供应商防脱落
    if (formData.supplierId && !list.some((s: SupplierVO) => s.id === formData.supplierId)) {
      list.unshift({ id: formData.supplierId, companyName: formData.supplierName } as SupplierVO)
    }
    supplierOptions.value = list
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (val?: number) => {
  const selected = supplierOptions.value.find((s) => s.id === val)
  formData.supplierName = selected?.companyName || ''
}

const handleDateRangeChange = (val?: [string, string]) => {
  if (val && val.length === 2) {
    formData.periodStart = val[0]
    formData.periodEnd = val[1]
  } else {
    formData.periodStart = ''
    formData.periodEnd = ''
  }
}

// 计价基准切换
const handlePricingBasisChange = (row: WorkbenchItem) => {
  row.billQty = calcItemBillQty(row)
  updateReferenceFeeForRow(row)
  row.lineAmount = calcItemLineAmount(row)
}

// 单价变更
const handleLinePriceChange = (row: WorkbenchItem) => {
  row.lineAmount = calcItemLineAmount(row)
}

// 调整项金额变更
const handleAdjustAmountChange = (row: WorkbenchItem) => {
  row.lineAmount = Number(Number(row.lineAmount || 0).toFixed(2))
}

// 应用产品档案加工费参考
const applyReferenceFee = (row: WorkbenchItem) => {
  if (row.referenceFee != null) {
    row.unitPrice = row.referenceFee
    row.lineAmount = calcItemLineAmount(row)
    ElMessage.success(`已预填${row.referenceFeeLabel}参考价 ¥${row.referenceFee}`)
  }
}

// 添加调整项
const handleAddAdjustLine = () => {
  itemsList.value.push({
    lineType: 'ADJUST',
    adjustReason: '',
    lineAmount: 0,
    remark: ''
  })
}

// 移除明细行
const handleRemoveLine = (idx: number) => {
  itemsList.value.splice(idx, 1)
}

// 取数预览
const handlePreview = async () => {
  if (!formData.supplierId) {
    ElMessage.warning('请先选择供应商')
    return
  }
  if (!formData.periodStart || !formData.periodEnd) {
    ElMessage.warning('请选择完整的对账区间')
    return
  }

  previewLoading.value = true
  try {
    const candidates = await previewSupplierRecon({
      supplierId: formData.supplierId,
      periodStart: formData.periodStart,
      periodEnd: formData.periodEnd
    })

    // 保留已有的 ADJUST 行
    const existingAdjustItems = itemsList.value.filter((it) => it.lineType === 'ADJUST')

    if (!candidates || candidates.length === 0) {
      itemsList.value = existingAdjustItems
      ElMessage.warning('所选区间内无可对账明细')
      return
    }

    // 转换候选行
    const newItems: WorkbenchItem[] = candidates.map((cand) => {
      const basis = cand.pricingBasis || (cand.inspectionType === 'GREIGE' ? 'METER' : 'KG')
      const item: WorkbenchItem = {
        lineType: cand.lineType,
        sourceRefType: cand.sourceRefType,
        sourceRefId: cand.sourceRefId,
        sourceRefItemId: cand.sourceRefItemId,
        inspectionOrderId: cand.inspectionOrderId,
        inspectionOrderNo: cand.inspectionOrderNo,
        inspectionType: cand.inspectionType,
        stockInId: cand.stockInId,
        stockInNo: cand.stockInNo,
        stockInItemId: cand.stockInItemId,
        yarnOrderId: cand.yarnOrderId,
        yarnOrderNo: cand.yarnOrderNo,
        productId: cand.productId,
        productCode: cand.productCode,
        productName: cand.productName,
        batchNo: cand.batchNo,
        qtyKg: cand.qtyKg,
        qtyM: cand.qtyM,
        rollCount: cand.rollCount,
        qtyUnit: cand.qtyUnit,
        pricingBasis: basis,
        billQty:
          cand.billQty != null
            ? cand.billQty
            : calcItemBillQty({
                lineType: cand.lineType,
                pricingBasis: basis,
                qtyKg: cand.qtyKg,
                qtyM: cand.qtyM
              }),
        unitPrice: cand.unitPrice != null ? cand.unitPrice : null,
        lineAmount: cand.lineAmount || 0,
        remark: ''
      }
      return item
    })

    itemsList.value = [...newItems, ...existingAdjustItems]
    await preloadProductReferenceFees()
    ElMessage.success(`预览取数完成，共载入 ${candidates.length} 条待对账明细`)
  } catch (err: any) {
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  } finally {
    previewLoading.value = false
  }
}

// 预加载所有行对应的产品参考价
const productCache = new Map<number, any>()

const preloadProductReferenceFees = async () => {
  const prodIds = Array.from(
    new Set(
      itemsList.value
        .filter((it) => it.lineType === 'PROCESS' && it.productId)
        .map((it) => it.productId!)
    )
  )

  for (const pid of prodIds) {
    if (!productCache.has(pid)) {
      try {
        const prod = await ProductApi.getProduct(pid)
        productCache.set(pid, prod)
      } catch {
        // ignore load error for reference price
      }
    }
  }

  // 绑定参考价到各行
  for (const item of itemsList.value) {
    if (item.lineType === 'PROCESS') {
      updateReferenceFeeForRow(item)
      // 若单价未填，自动预填参考价
      if (item.unitPrice == null && item.referenceFee != null) {
        item.unitPrice = item.referenceFee
        item.lineAmount = calcItemLineAmount(item)
      }
    }
  }
}

const updateReferenceFeeForRow = (row: WorkbenchItem) => {
  if (row.productId && productCache.has(row.productId)) {
    const prod = productCache.get(row.productId)
    const ref = getProductReferenceFee(prod, row.pricingBasis || 'KG')
    row.referenceFee = ref.fee
    row.referenceFeeLabel = ref.label
  }
}

// 重取（仅草稿，提示替换全部取数行、保留调整项）
const handleReFetch = async () => {
  if (!formData.id) return
  try {
    await ElMessageBox.confirm(
      '重新取数将替换当前全部取数行（检验单加工费与入库明细），已添加的调整项行将保留。是否继续？',
      '重新取数确认',
      {
        confirmButtonText: '确定重取',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    actionLoading.value = true
    await reFetchSupplierRecon(formData.id)
    ElMessage.success('已重新取数')
    await loadReconDetail(formData.id)
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    if (parsed.isReferenced) {
      ElMessageBox.alert(parsed.message, '明细已被占用', {
        confirmButtonText: '我知道了',
        type: 'warning'
      })
    } else {
      ElMessage.error(parsed.message)
    }
  } finally {
    actionLoading.value = false
  }
}

// 保存草稿
const handleSave = async (silent = false): Promise<boolean> => {
  const validation = validateReconDraft({
    supplierId: formData.supplierId,
    periodStart: formData.periodStart,
    periodEnd: formData.periodEnd,
    items: itemsList.value
  })

  if (!validation.valid) {
    ElMessage.warning(validation.message || '请检查表单完整性')
    return false
  }

  actionLoading.value = true
  try {
    const payloadItems: SupplierReconItemSaveVO[] = itemsList.value.map((it) => ({
      id: it.id,
      lineType: it.lineType,
      sourceRefType: it.sourceRefType,
      sourceRefId: it.sourceRefId,
      sourceRefItemId: it.sourceRefItemId,
      inspectionOrderId: it.inspectionOrderId,
      inspectionOrderNo: it.inspectionOrderNo,
      inspectionType: it.inspectionType,
      stockInId: it.stockInId,
      stockInNo: it.stockInNo,
      stockInItemId: it.stockInItemId,
      yarnOrderId: it.yarnOrderId,
      yarnOrderNo: it.yarnOrderNo,
      productId: it.productId,
      productCode: it.productCode,
      productName: it.productName,
      batchNo: it.batchNo,
      qtyKg: it.qtyKg,
      qtyM: it.qtyM,
      qtyUnit: it.qtyUnit,
      pricingBasis: it.pricingBasis,
      billQty: it.billQty,
      unitPrice: it.unitPrice,
      lineAmount: it.lineAmount,
      adjustReason: it.adjustReason,
      remark: it.remark
    }))

    if (isCreateMode.value) {
      const newId = await createSupplierRecon({
        supplierId: formData.supplierId!,
        periodStart: formData.periodStart,
        periodEnd: formData.periodEnd,
        remark: formData.remark,
        items: payloadItems
      })
      if (!silent) {
        ElMessage.success('供应商对账单创建成功')
      }
      router.replace(`/finance/supplier-recon/workspace/${newId}`)
      await loadReconDetail(newId)
    } else {
      await updateSupplierRecon({
        id: formData.id!,
        remark: formData.remark,
        items: payloadItems
      })
      if (!silent) {
        ElMessage.success('对账单保存成功')
      }
      await loadReconDetail(formData.id!)
    }
    return true
  } catch (err: any) {
    const parsed = parseReconErrorMessage(err)
    if (parsed.isReferenced) {
      ElMessageBox.alert(parsed.message, '明细已被占用', {
        confirmButtonText: '重新取数',
        type: 'warning',
        callback: () => {
          if (!isCreateMode.value) {
            handleReFetch()
          } else {
            handlePreview()
          }
        }
      })
    } else {
      ElMessage.error(parsed.message)
    }
    return false
  } finally {
    actionLoading.value = false
  }
}

// 确认对账单
const handleConfirm = async () => {
  if (!formData.id && !isCreateMode.value) {
    ElMessage.warning('请先保存对账单草稿')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认后对账单行数据将冻结，整单转为只读，不可再修改明细。是否确认该对账单？',
      '确认对账单',
      {
        confirmButtonText: '确认冻结',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    actionLoading.value = true
    // 确认前先自动保存工作台最新编辑内容，防止内存修改丢失
    const saved = await handleSave(true)
    if (!saved || !formData.id) {
      return
    }

    actionLoading.value = true
    await confirmSupplierRecon(formData.id)
    ElMessage.success('对账单已确认，已冻结为只读凭证')
    await loadReconDetail(formData.id)
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  } finally {
    actionLoading.value = false
  }
}

// 作废对账单
const handleCancel = async () => {
  if (!formData.id) return
  try {
    await ElMessageBox.confirm(
      '作废后对账单将置为终态，其引用的源单明细将释放回候选池。是否确认作废该对账单？',
      '作废对账单',
      {
        confirmButtonText: '确定作废',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    actionLoading.value = true
    await cancelSupplierRecon(formData.id)
    ElMessage.success('对账单已作废')
    await loadReconDetail(formData.id)
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  } finally {
    actionLoading.value = false
  }
}

// 打开生成付款申请
const handleOpenPaymentApply = () => {
  paymentApplyDialogRef.value?.open(formData as unknown as SupplierReconRespVO)
}

const handlePaymentApplySuccess = async () => {
  if (formData.id) {
    await loadReconDetail(formData.id)
  }
}

// 路由跳转
const jumpToInspection = (orderId?: number) => {
  if (!orderId) return
  router.push(`/production/inspection-order/workspace/${orderId}`)
}

const jumpToStockIn = (stockInId?: number) => {
  if (!stockInId) return
  router.push(`/stock/stock-in/workspace/${stockInId}`)
}

const handleBack = () => {
  router.push('/finance/supplier-reconciliation')
}
</script>
