<template>
  <div class="supplier-recon-workspace p-4">
    <!-- 顶部状态栏与操作栏 -->
    <el-card shadow="never" class="mb-4">
      <div class="flex justify-between items-center">
        <div class="flex items-center space-x-3">
          <el-button @click="handleBack">
            <Icon icon="ep:arrow-left" class="mr-1" />返回列表
          </el-button>
          <span class="text-lg font-bold">
            {{ pageTitle }}
          </span>
          <span v-if="reconForm.reconNo" class="font-mono text-sm text-gray-500">
            【{{ reconForm.reconNo }}】
          </span>
          <DictTag
            v-if="reconForm.status"
            :type="DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS"
            :value="reconForm.status"
          />
        </div>

        <div class="flex items-center space-x-2">
          <!-- 草稿 / 新建态操作 -->
          <template v-if="isDraftOrNew && !isViewMode">
            <el-button
              type="primary"
              :loading="saving"
              @click="handleSave"
              v-hasPermi="['lunaerp:finance:supplierrecon:create', 'lunaerp:finance:supplierrecon:update']"
            >
              <Icon icon="ep:check" class="mr-1" />保存
            </el-button>
            <el-button
              v-if="reconId"
              type="warning"
              plain
              :loading="reFetching"
              @click="handleReFetch"
              v-hasPermi="['lunaerp:finance:supplierrecon:update']"
            >
              <Icon icon="ep:refresh" class="mr-1" />重取
            </el-button>
            <el-button
              v-if="reconId"
              type="success"
              :loading="confirming"
              @click="handleConfirm"
              v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
            >
              <Icon icon="ep:finished" class="mr-1" />确认
            </el-button>
          </template>

          <!-- 已确认态操作 -->
          <template v-if="reconForm.status === 'CONFIRMED'">
            <el-button
              type="primary"
              plain
              @click="handlePrint"
              v-hasPermi="['lunaerp:finance:supplierrecon:print']"
            >
              <Icon icon="ep:printer" class="mr-1" />打印对账单
            </el-button>
            <el-button
              type="danger"
              plain
              @click="handleCancel"
              v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
            >
              <Icon icon="ep:circle-close" class="mr-1" />作废
            </el-button>
          </template>
        </div>
      </div>
    </el-card>

    <!-- 基本信息与取数条件区 -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="font-bold text-gray-700">基本信息与对账区间</span>
      </template>

      <el-form :model="reconForm" ref="formRef" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                v-model="reconForm.supplierId"
                placeholder="搜索并选择供应商"
                filterable
                remote
                reserve-keyword
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                :disabled="!isEditable"
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

          <el-col :span="10">
            <el-form-item label="对账区间" prop="periodRange">
              <el-date-picker
                v-model="reconForm.periodRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                :disabled="!isEditable"
                class="!w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="6">
            <el-button
              v-if="isEditable"
              type="primary"
              plain
              :loading="previewLoading"
              @click="handlePreview"
              v-hasPermi="['lunaerp:finance:supplierrecon:query']"
            >
              <Icon icon="ep:download" class="mr-1" />取数预览
            </el-button>
          </el-col>

          <el-col :span="24">
            <el-form-item label="备注说明" prop="remark">
              <el-input
                v-model="reconForm.remark"
                placeholder="请输入对账单备注说明（选填）"
                :disabled="!isEditable"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 对账明细工作区 -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-2">
            <span class="font-bold text-gray-700">对账明细</span>
            <span class="text-xs text-gray-500">
              （三类行混排：加工费、采购货款、调整项）
            </span>
          </div>
          <div v-if="isEditable" class="space-x-2">
            <el-button size="small" type="primary" plain @click="handleAddAdjustRow">
              <Icon icon="ep:plus" class="mr-1" />添加调整项
            </el-button>
          </div>
        </div>
      </template>

      <!-- 明细表格 -->
      <el-table
        :data="itemList"
        border
        stripe
        size="small"
        v-loading="tableLoading"
        empty-text="暂无明细"
      >
        <el-table-column type="index" label="#" width="45" align="center" />

        <!-- 行类型标签 -->
        <el-table-column label="类型" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="getLineTypeTagType(row.lineType)" size="small">
              {{ getLineTypeLabel(row.lineType) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 来源单号与类型 -->
        <el-table-column label="来源单号" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.lineType === 'PROCESS'">
              <el-link
                type="primary"
                :underline="false"
                @click="openInspectionOrder(row.inspectionOrderId)"
                class="font-mono text-xs"
              >
                <Icon icon="ep:link" class="mr-1 text-xs" />
                {{ row.inspectionOrderNo || '检验单' }}
              </el-link>
              <el-tag size="small" class="ml-1" effect="plain" :type="row.inspectionType === 'GREIGE' ? 'info' : 'success'">
                {{ row.inspectionType === 'GREIGE' ? '织费(胚布)' : '染费(成品)' }}
              </el-tag>
            </div>
            <div v-else-if="row.lineType === 'PURCHASE'">
              <el-link
                type="primary"
                :underline="false"
                @click="openStockIn(row.stockInId)"
                class="font-mono text-xs"
              >
                <Icon icon="ep:link" class="mr-1 text-xs" />
                {{ row.stockInNo || '入库单' }}
              </el-link>
              <span v-if="row.yarnOrderNo" class="text-xs text-gray-500 font-mono block">
                纱线单: {{ row.yarnOrderNo }}
              </span>
            </div>
            <span v-else class="text-gray-400 text-xs">手工调整</span>
          </template>
        </el-table-column>

        <!-- 产品 / 批次 -->
        <el-table-column label="产品 / 规格 / 批次" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.productName" class="font-medium text-xs">{{ row.productName }}</div>
            <div v-if="row.productCode" class="text-gray-400 text-10px font-mono">{{ row.productCode }}</div>
            <div v-if="row.batchNo" class="text-gray-500 text-10px font-mono">批次/缸号: {{ row.batchNo }}</div>
            <span v-if="!row.productName && !row.productCode && row.lineType === 'ADJUST'" class="text-gray-400 text-xs">—</span>
          </template>
        </el-table-column>

        <!-- 数量统计 -->
        <el-table-column label="数量统计" width="130" align="right">
          <template #default="{ row }">
            <div v-if="row.lineType === 'PROCESS'" class="font-mono text-xs">
              <span v-if="row.qtyKg != null">{{ Number(row.qtyKg).toFixed(2) }} KG</span>
              <span v-if="row.qtyM != null" class="text-gray-500 text-10px block">
                {{ Number(row.qtyM).toFixed(2) }} M
              </span>
              <span v-if="row.rollCount != null" class="text-gray-400 text-10px block">
                ({{ row.rollCount }} 卷)
              </span>
            </div>
            <div v-else-if="row.lineType === 'PURCHASE'" class="font-mono text-xs">
              {{ Number(row.quantity || row.billQty || 0).toFixed(2) }} {{ row.unit || row.qtyUnit || 'KG' }}
            </div>
            <span v-else class="text-gray-400 text-xs">—</span>
          </template>
        </el-table-column>

        <!-- 计价基准切换 -->
        <el-table-column label="计价基准" width="110" align="center">
          <template #default="{ row }">
            <el-select
              v-if="row.lineType === 'PROCESS' && isEditable"
              v-model="row.pricingBasis"
              size="small"
              class="w-85px"
              @change="handlePricingBasisChange(row)"
            >
              <el-option label="M (米)" value="METER" />
              <el-option label="KG (公斤)" value="KG" />
            </el-select>
            <span v-else-if="row.lineType === 'PROCESS'" class="font-mono text-xs">
              {{ row.pricingBasis === 'METER' ? '米 (M)' : '公斤 (KG)' }}
            </span>
            <span v-else-if="row.lineType === 'PURCHASE'" class="font-mono text-xs text-gray-500">
              {{ row.pricingBasis || row.unit || 'KG' }}
            </span>
            <span v-else class="text-gray-400 text-xs">—</span>
          </template>
        </el-table-column>

        <!-- 结算计费数量 -->
        <el-table-column label="结算数量" width="95" align="right">
          <template #default="{ row }">
            <span class="font-mono text-xs font-medium">
              {{ row.billQty != null ? Number(row.billQty).toFixed(2) : '—' }}
            </span>
          </template>
        </el-table-column>

        <!-- 结算单价（可编辑） -->
        <el-table-column label="单价(元)" width="120" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="row.lineType !== 'ADJUST' && isEditable"
              v-model="row.unitPrice"
              :precision="4"
              :step="0.1"
              :min="0"
              :controls="false"
              size="small"
              placeholder="单价"
              class="!w-full"
              @change="handleRowPriceChange(row)"
            />
            <span v-else-if="row.lineType !== 'ADJUST'" class="font-mono text-xs">
              {{ row.unitPrice != null ? Number(row.unitPrice).toFixed(4) : '—' }}
            </span>
            <span v-else class="text-gray-400 text-xs">—</span>
          </template>
        </el-table-column>

        <!-- 行金额 -->
        <el-table-column label="行金额(元)" width="130" align="right">
          <template #default="{ row }">
            <!-- 调整项：金额直接手工录入（正/负） -->
            <el-input-number
              v-if="row.lineType === 'ADJUST' && isEditable"
              v-model="row.lineAmount"
              :precision="2"
              :step="100"
              :controls="false"
              size="small"
              placeholder="+/- 金额"
              class="!w-full"
              @change="recalculateTotals"
            />
            <span
              v-else
              class="font-mono font-bold text-xs"
              :class="row.lineAmount && row.lineAmount < 0 ? 'text-red-600' : ''"
            >
              ¥ {{ formatAmount(row.lineAmount) }}
            </span>
          </template>
        </el-table-column>

        <!-- 调整原因 / 备注 -->
        <el-table-column label="调整原因 / 备注" min-width="180">
          <template #default="{ row }">
            <div v-if="row.lineType === 'ADJUST' && isEditable" class="space-y-1">
              <el-input
                v-model="row.adjustReason"
                size="small"
                placeholder="调整原因（必填）*"
                maxlength="100"
                class="!w-full"
              />
              <el-input
                v-model="row.remark"
                size="small"
                placeholder="补充备注（选填）"
                maxlength="200"
                class="!w-full"
              />
            </div>
            <div v-else-if="row.lineType === 'ADJUST'">
              <span class="text-red-700 font-medium text-xs">[{{ row.adjustReason || '无原因' }}]</span>
              <span class="text-xs ml-1">{{ row.remark || '' }}</span>
            </div>
            <div v-else-if="isEditable">
              <el-input
                v-model="row.remark"
                size="small"
                placeholder="行备注（选填）"
                maxlength="200"
                class="!w-full"
              />
            </div>
            <div v-else class="text-xs text-gray-500">
              {{ row.remark || '—' }}
            </div>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column v-if="isEditable" label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button
              link
              type="danger"
              size="small"
              @click="handleRemoveRow($index)"
            >
              <Icon icon="ep:delete" />
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="py-8 text-center text-gray-400">
            <div v-if="hasPreviewed">区间内无可对账明细</div>
            <div v-else>请选择供应商与对账区间后，点击【取数预览】获取待对账明细</div>
          </div>
        </template>
      </el-table>

      <!-- 底部汇总统计栏 -->
      <div class="summary-bar mt-4 p-4 border rounded bg-gray-50 flex justify-between items-center">
        <div class="text-xs text-gray-600 space-x-4">
          <span>总行数：<strong class="text-gray-900 font-mono">{{ itemList.length }}</strong> 行</span>
          <span>加工费：<strong class="text-blue-700 font-mono">{{ processSummary.count }}</strong> 行 / ¥ {{ formatAmount(processSummary.amount) }}</span>
          <span>采购货款：<strong class="text-yellow-700 font-mono">{{ purchaseSummary.count }}</strong> 行 / ¥ {{ formatAmount(purchaseSummary.amount) }}</span>
          <span>调整项：<strong class="text-red-700 font-mono">{{ adjustSummary.count }}</strong> 行 / ¥ {{ formatAmount(adjustSummary.amount) }}</span>
        </div>

        <div class="flex items-center space-x-3">
          <span class="text-sm text-gray-700">确认金额合计：</span>
          <span class="text-xl font-bold font-mono text-blue-800">
            ¥ {{ formatAmount(totalAmount) }}
          </span>
          <span class="text-xs text-gray-500">（{{ uppercaseTotalAmount }}）</span>
        </div>
      </div>
    </el-card>

    <!-- 打印预览组件 -->
    <PrintDispatcher biz-type="VENDOR_RECON" ref="printDispatcherRef" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import { useMessage } from '@/hooks/web/useMessage'
import * as SupplierReconApi from '@/api/lunaerp-elmui/finance/supplier-recon'
import type {
  SupplierReconVO,
  SupplierReconItemVO,
  SupplierReconItemSaveReqVO,
  SupplierReconSaveReqVO
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import { formatAmount, formatRmbUppercase } from '../components/print/utils'
import { PrintDispatcher } from '@/components/Print'

defineOptions({ name: 'LunaerpElmuiFinanceSupplierReconWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const reconId = computed(() => {
  const p = route.params.id
  return p ? Number(p) : undefined
})

const isViewMode = computed(() => route.query.mode === 'view')

// 缺省上月 1 日至上月最后一日
const getDefaultLastMonthRange = (): [string, string] => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() // 0-based
  const lastMonthDate = new Date(year, month - 1, 1)
  const lmYear = lastMonthDate.getFullYear()
  const lmMonth = lastMonthDate.getMonth()
  const start = new Date(lmYear, lmMonth, 1)
  const end = new Date(lmYear, lmMonth + 1, 0)
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return [
    `${start.getFullYear()}-${pad(start.getMonth() + 1)}-${pad(start.getDate())}`,
    `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`
  ]
}

const formRef = ref()
const printDispatcherRef = ref()

interface ReconFormState {
  id?: number
  reconNo?: string
  supplierId: number | undefined
  supplierName?: string
  periodRange: [string, string]
  status: string
  remark: string
}

const reconForm = reactive<ReconFormState>({
  id: undefined,
  reconNo: '',
  supplierId: undefined,
  supplierName: '',
  periodRange: getDefaultLastMonthRange(),
  status: 'DRAFT',
  remark: ''
})

const rules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  periodRange: [{ required: true, message: '请选择对账区间', trigger: 'change' }]
}

const itemList = ref<SupplierReconItemVO[]>([])
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const tableLoading = ref(false)
const previewLoading = ref(false)
const saving = ref(false)
const reFetching = ref(false)
const confirming = ref(false)
const hasPreviewed = ref(false)

// 产品档案缓存，避免重复请求
const productCache = new Map<number, any>()

const isDraftOrNew = computed(() => {
  return !reconForm.status || reconForm.status === 'DRAFT'
})

const isEditable = computed(() => {
  return isDraftOrNew.value && !isViewMode.value
})

const pageTitle = computed(() => {
  if (isViewMode.value) return '查看供应商对账单'
  if (reconId.value) return '编辑供应商对账单'
  return '新建供应商对账单'
})

// 计算汇总指标
const processSummary = computed(() => {
  const rows = itemList.value.filter((i) => i.lineType === 'PROCESS')
  const amount = rows.reduce((acc, cur) => acc + (cur.lineAmount || 0), 0)
  return { count: rows.length, amount }
})

const purchaseSummary = computed(() => {
  const rows = itemList.value.filter((i) => i.lineType === 'PURCHASE')
  const amount = rows.reduce((acc, cur) => acc + (cur.lineAmount || 0), 0)
  return { count: rows.length, amount }
})

const adjustSummary = computed(() => {
  const rows = itemList.value.filter((i) => i.lineType === 'ADJUST')
  const amount = rows.reduce((acc, cur) => acc + (cur.lineAmount || 0), 0)
  return { count: rows.length, amount }
})

const totalAmount = computed(() => {
  return processSummary.value.amount + purchaseSummary.value.amount + adjustSummary.value.amount
})

const uppercaseTotalAmount = computed(() => {
  return formatRmbUppercase(totalAmount.value)
})

/** 供应商远程搜索 */
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

const handleSupplierChange = () => {
  const found = supplierOptions.value.find((s) => s.id === reconForm.supplierId)
  if (found) {
    reconForm.supplierName = found.companyName
  }
}

/** 获取并预填产品参考加工费 */
const fetchProductRefPrice = async (productId?: number, basis?: string): Promise<number | undefined> => {
  if (!productId) return undefined
  try {
    let p = productCache.get(productId)
    if (!p) {
      p = await ProductApi.getProduct(productId)
      productCache.set(productId, p)
    }
    if (basis === 'METER') {
      return p?.weavingFeePerMeter != null ? Number(p.weavingFeePerMeter) : undefined
    } else {
      return p?.knitProcessingFeePerKg != null ? Number(p.knitProcessingFeePerKg) : undefined
    }
  } catch {
    return undefined
  }
}

/** 计价基准切换重算数量与金额 */
const handlePricingBasisChange = async (row: SupplierReconItemVO) => {
  if (row.pricingBasis === 'METER') {
    row.billQty = row.qtyM != null ? Number(row.qtyM) : 0
  } else {
    row.billQty = row.qtyKg != null ? Number(row.qtyKg) : 0
  }

  // 若单价未填，尝试以新基准匹配产品档案加工费
  if (row.unitPrice == null && row.productId) {
    const refPrice = await fetchProductRefPrice(row.productId, row.pricingBasis)
    if (refPrice != null) {
      row.unitPrice = refPrice
    }
  }

  handleRowPriceChange(row)
}

/** 单价编辑重算金额 */
const handleRowPriceChange = (row: SupplierReconItemVO) => {
  const qty = row.billQty != null ? Number(row.billQty) : 0
  const price = row.unitPrice != null ? Number(row.unitPrice) : 0
  row.lineAmount = Math.round(qty * price * 100) / 100
}

const recalculateTotals = () => {
  // trigger reactivity
}

/** 取数预览 */
const handlePreview = async () => {
  if (!reconForm.supplierId) {
    message.warning('请先选择供应商')
    return
  }
  if (!reconForm.periodRange || reconForm.periodRange.length < 2) {
    message.warning('请先选择对账区间')
    return
  }

  previewLoading.value = true
  hasPreviewed.value = true
  try {
    const data = await SupplierReconApi.previewSupplierRecon({
      supplierId: reconForm.supplierId,
      periodStart: reconForm.periodRange[0],
      periodEnd: reconForm.periodRange[1]
    })

    const candidateItems = data.items || []

    if (candidateItems.length === 0) {
      message.info('区间内无可对账明细')
    }

    // 保留现有的 ADJUST 调整项
    const existingAdjustItems = itemList.value.filter((i) => i.lineType === 'ADJUST')

    // 处理加工费候选明细预填单价
    for (const item of candidateItems) {
      if (item.lineType === 'PROCESS') {
        if (item.unitPrice == null && item.productId) {
          const refPrice = await fetchProductRefPrice(item.productId, item.pricingBasis)
          if (refPrice != null) {
            item.unitPrice = refPrice
            item.lineAmount = Math.round(Number(item.billQty || 0) * refPrice * 100) / 100
          }
        }
      }
    }

    itemList.value = [...candidateItems, ...existingAdjustItems]
    message.success(`取数完成，共 ${candidateItems.length} 条待对账明细`)
  } catch (err: any) {
    const msg = err?.response?.data?.msg || err?.message || '取数预览失败'
    message.error(msg)
  } finally {
    previewLoading.value = false
  }
}

/** 添加调整项行 */
const handleAddAdjustRow = () => {
  itemList.value.push({
    lineType: 'ADJUST',
    lineAmount: 0,
    adjustReason: '',
    remark: '',
    _rowKey: `adjust_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`
  })
}

/** 移除行 */
const handleRemoveRow = (index: number) => {
  itemList.value.splice(index, 1)
}

/** 保存 / 创建 */
const handleSave = async () => {
  await formRef.value?.validate()

  if (itemList.value.length === 0) {
    message.warning('当前明细为空，请先点击【取数预览】获取明细')
    return
  }

  // 校验调整项
  for (let idx = 0; idx < itemList.value.length; idx++) {
    const item = itemList.value[idx]
    if (item.lineType === 'ADJUST') {
      if (!item.adjustReason || !item.adjustReason.trim()) {
        message.warning(`第 ${idx + 1} 行调整项必须填写调整原因`)
        return
      }
      if (item.lineAmount == null || Number(item.lineAmount) === 0) {
        message.warning(`第 ${idx + 1} 行调整项金额不能为 0`)
        return
      }
    }
  }

  saving.value = true
  try {
    const saveItems: SupplierReconItemSaveReqVO[] = itemList.value.map((i) => ({
      id: i.id,
      lineType: i.lineType,
      sourceRefType: i.sourceRefType,
      sourceRefId: i.sourceRefId,
      sourceRefItemId: i.sourceRefItemId,
      inspectionOrderId: i.inspectionOrderId,
      inspectionOrderNo: i.inspectionOrderNo,
      inspectionType: i.inspectionType,
      stockInId: i.stockInId,
      stockInNo: i.stockInNo,
      yarnOrderId: i.yarnOrderId,
      yarnOrderNo: i.yarnOrderNo,
      productId: i.productId,
      productCode: i.productCode,
      productName: i.productName,
      batchNo: i.batchNo,
      qtyKg: i.qtyKg,
      qtyM: i.qtyM,
      qtyUnit: i.qtyUnit || i.unit,
      pricingBasis: i.pricingBasis,
      billQty: i.billQty,
      unitPrice: i.unitPrice,
      lineAmount: i.lineAmount,
      adjustReason: i.adjustReason,
      remark: i.remark
    }))

    const reqData: SupplierReconSaveReqVO = {
      id: reconForm.id,
      supplierId: reconForm.supplierId!,
      periodStart: reconForm.periodRange[0],
      periodEnd: reconForm.periodRange[1],
      remark: reconForm.remark,
      items: saveItems
    }

    if (reconForm.id) {
      await SupplierReconApi.updateSupplierRecon(reqData)
      message.success('保存成功')
      await loadReconDetail(reconForm.id)
    } else {
      const newId = await SupplierReconApi.createSupplierRecon(reqData)
      message.success('对账单创建成功')
      router.replace(`/finance/supplier-recon/workspace/${newId}`)
    }
  } catch (err: any) {
    const errCode = err?.response?.data?.code
    const errMsg = err?.response?.data?.msg || err?.message || '保存失败'
    // 004 错误防重守卫友好提示
    if (errCode === 1030022004 || errMsg.includes('已被对账单')) {
      await ElMessageBox.alert(errMsg, '明细已被占用', {
        type: 'error',
        confirmButtonText: '我知道了'
      })
    } else {
      message.error(errMsg)
    }
  } finally {
    saving.value = false
  }
}

/** 重取草稿 */
const handleReFetch = async () => {
  if (!reconForm.id) return
  try {
    await ElMessageBox.confirm('将替换全部取数行、调整项保留，确定重新取数吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定重取',
      cancelButtonText: '取消'
    })
    reFetching.value = true
    await SupplierReconApi.reFetchSupplierRecon(reconForm.id)
    message.success('重取成功')
    await loadReconDetail(reconForm.id)
  } catch (err: any) {
    if (err !== 'cancel') {
      const msg = err?.response?.data?.msg || err?.message || '重取失败'
      message.error(msg)
    }
  } finally {
    reFetching.value = false
  }
}

/** 确认对账单 */
const handleConfirm = async () => {
  if (!reconForm.id) return
  try {
    await ElMessageBox.confirm(
      '确定要确认该对账单吗？确认后单据将冻结为只读，不可再修改。',
      '确认对账单',
      {
        type: 'warning',
        confirmButtonText: '确定确认',
        cancelButtonText: '取消'
      }
    )
    confirming.value = true
    await SupplierReconApi.confirmSupplierRecon(reconForm.id)
    message.success('对账单已确认')
    await loadReconDetail(reconForm.id)
  } catch (err: any) {
    if (err !== 'cancel') {
      const msg = err?.response?.data?.msg || err?.message || '确认失败'
      message.error(msg)
    }
  } finally {
    confirming.value = false
  }
}

/** 作废对账单 */
const handleCancel = async () => {
  if (!reconForm.id) return
  try {
    await ElMessageBox.confirm(
      '确定要作废该对账单吗？作废后不可恢复，引用的源明细将释放回待对账池。',
      '作废对账单',
      {
        type: 'warning',
        confirmButtonText: '确定作废',
        cancelButtonText: '取消'
      }
    )
    await SupplierReconApi.cancelSupplierRecon(reconForm.id)
    message.success('作废成功')
    await loadReconDetail(reconForm.id)
  } catch (err: any) {
    if (err !== 'cancel') {
      const errCode = err?.response?.data?.code
      const msg = err?.response?.data?.msg || err?.message || '作废失败'
      if (errCode === 1030022006 || msg.includes('存在付款申请')) {
        await ElMessageBox.alert(msg, '禁止作废', {
          type: 'error',
          confirmButtonText: '确定'
        })
      } else {
        message.error(msg)
      }
    }
  }
}

/** 打印对账单 */
const handlePrint = () => {
  if (!reconForm.id) return
  printDispatcherRef.value?.open({ bizId: reconForm.id })
}

/** 返回列表 */
const handleBack = () => {
  router.push('/finance/supplier-recon')
}

const openInspectionOrder = (id?: number) => {
  if (!id) return
  router.push({
    path: `/production/inspection-order/workspace/${id}`,
    query: { mode: 'view' }
  })
}

const openStockIn = (id?: number) => {
  if (!id) return
  router.push({
    path: `/stock/stock-in/workspace/${id}`,
    query: { mode: 'view' }
  })
}

const getLineTypeLabel = (lineType?: string) => {
  if (lineType === 'PROCESS') return '加工费'
  if (lineType === 'PURCHASE') return '采购货款'
  if (lineType === 'ADJUST') return '调整项'
  return lineType || '—'
}

const getLineTypeTagType = (lineType?: string): 'primary' | 'warning' | 'danger' | 'info' => {
  if (lineType === 'PROCESS') return 'primary'
  if (lineType === 'PURCHASE') return 'warning'
  if (lineType === 'ADJUST') return 'danger'
  return 'info'
}

/** 加载详情 */
const loadReconDetail = async (id: number) => {
  tableLoading.value = true
  try {
    const data = await SupplierReconApi.getSupplierRecon(id)
    reconForm.id = data.id
    reconForm.reconNo = data.reconNo
    reconForm.supplierId = data.supplierId
    reconForm.supplierName = data.supplierName
    reconForm.periodRange = [data.periodStart, data.periodEnd]
    reconForm.status = data.status || 'DRAFT'
    reconForm.remark = data.remark || ''

    if (data.supplierId && data.supplierName) {
      supplierOptions.value = [
        { id: data.supplierId, companyName: data.supplierName } as SupplierVO
      ]
    }

    itemList.value = data.items || []
  } finally {
    tableLoading.value = false
  }
}

onMounted(() => {
  searchSuppliers('')
  if (reconId.value) {
    loadReconDetail(reconId.value)
  }
})
</script>

<style scoped>
.supplier-recon-workspace {
  min-height: calc(100vh - 84px);
}
</style>
