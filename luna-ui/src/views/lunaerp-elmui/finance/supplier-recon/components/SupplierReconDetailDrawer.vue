<template>
  <el-drawer
    v-model="visible"
    :title="`供应商对账单详情 - ${detail?.reconNo || ''}`"
    size="75%"
    destroy-on-close
    direction="rtl"
  >
    <div v-loading="loading" class="detail-container p-4">
      <!-- 1. 顶部操作栏与状态 -->
      <div class="flex justify-between items-center mb-16px pb-12px border-b border-gray-200">
        <div class="flex items-center gap-3">
          <span class="text-18px font-bold font-mono">{{ detail?.reconNo }}</span>
          <DictTag
            v-if="detail?.status"
            :type="DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS"
            :value="detail.status"
          />
        </div>

        <div class="action-buttons flex items-center gap-2">
          <!-- 草稿态：可编辑、删除 -->
          <template v-if="detail?.status === 'DRAFT'">
            <el-button
              type="primary"
              plain
              @click="handleGoWorkbench(detail?.id)"
              v-hasPermi="['lunaerp:finance:supplierrecon:update']"
            >
              <Icon icon="ep:edit" class="mr-1" /> 编辑工作台
            </el-button>
            <el-button
              type="success"
              @click="handleConfirmRecon(detail?.id)"
              v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
            >
              <Icon icon="ep:check" class="mr-1" /> 确认对账单
            </el-button>
          </template>

          <!-- 已确认态：生成付款申请、打印、作废 -->
          <template v-else-if="detail?.status === 'CONFIRMED'">
            <el-button
              type="success"
              plain
              @click="handleOpenPaymentApply"
              v-hasPermi="['lunaerp:finance:supplierrecon:generate']"
            >
              <Icon icon="ep:money" class="mr-1" /> 生成付款申请
            </el-button>

            <PrintDispatcher
              v-if="detail?.id"
              biz-type="VENDOR_RECON"
              :biz-id="detail.id"
              class="inline-block"
            >
              <template #default="{ open: triggerPrint }">
                <el-button
                  type="primary"
                  plain
                  @click="triggerPrint()"
                  v-hasPermi="['lunaerp:finance:supplierrecon:print']"
                >
                  <Icon icon="ep:printer" class="mr-1" /> 打印对账单
                </el-button>
              </template>
            </PrintDispatcher>

            <el-button
              type="warning"
              plain
              @click="handleCancelRecon(detail?.id)"
              v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
            >
              <Icon icon="ep:circle-close" class="mr-1" /> 作废
            </el-button>
          </template>

          <el-button
            type="info"
            plain
            @click="handleGoWorkbench(detail?.id)"
            v-hasPermi="['lunaerp:finance:supplierrecon:query']"
          >
            <Icon icon="ep:view" class="mr-1" /> 完整工作台
          </el-button>
        </div>
      </div>

      <!-- 2. 基本信息卡片 -->
      <el-descriptions title="基本信息" :column="3" border class="mb-20px">
        <el-descriptions-item label="对账单号">
          <span class="font-mono font-medium">{{ detail?.reconNo || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="供应商">
          <span class="font-bold text-gray-800">{{ detail?.supplierName || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="对账区间">
          <span class="font-medium"
            >{{ detail?.periodStart || '—' }} 至 {{ detail?.periodEnd || '—' }}</span
          >
        </el-descriptions-item>

        <el-descriptions-item label="对账确认金额">
          <span class="font-mono font-bold text-16px text-emerald-800">
            ¥ {{ formatMoney(detail?.totalAmount) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="已申请金额（实时）">
          <span class="font-mono font-bold text-blue-700">
            ¥ {{ formatMoney(detail?.appliedAmount) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          <span>{{ detail?.createTime || '—' }}</span>
        </el-descriptions-item>

        <el-descriptions-item label="确认人 / 时间">
          <span>{{ detail?.confirmedAt ? `${detail.confirmedBy ? `UID:${detail.confirmedBy} ` : ''}(${detail.confirmedAt})` : '未确认' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="作废人 / 时间">
          <span>{{ detail?.cancelledAt ? `已作废 (${detail.cancelledAt})` : '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注说明" :span="1">
          <span>{{ detail?.remark || '无' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 3. 明细行分类金额卡片 -->
      <div class="grid grid-cols-4 gap-4 mb-16px">
        <div class="bg-blue-50 border border-blue-200 rounded p-3">
          <div class="text-12px text-blue-600">加工费小计 ({{ processCount }} 行)</div>
          <div class="text-16px font-bold font-mono text-blue-900 mt-1">
            ¥ {{ formatMoney(subtotals.processTotal) }}
          </div>
        </div>
        <div class="bg-emerald-50 border border-emerald-200 rounded p-3">
          <div class="text-12px text-emerald-600">采购货款小计 ({{ purchaseCount }} 行)</div>
          <div class="text-16px font-bold font-mono text-emerald-900 mt-1">
            ¥ {{ formatMoney(subtotals.purchaseTotal) }}
          </div>
        </div>
        <div class="bg-amber-50 border border-amber-200 rounded p-3">
          <div class="text-12px text-amber-600">调整项小计 ({{ adjustCount }} 行)</div>
          <div
            class="text-16px font-bold font-mono mt-1"
            :class="subtotals.adjustTotal < 0 ? 'text-red-700' : 'text-amber-900'"
          >
            ¥ {{ formatMoney(subtotals.adjustTotal) }}
          </div>
        </div>
        <div class="bg-gray-100 border border-gray-300 rounded p-3">
          <div class="text-12px text-gray-600">对账总金额（服务端权威值）</div>
          <div class="text-18px font-bold font-mono text-black mt-1">
            ¥ {{ formatMoney(detail?.totalAmount) }}
          </div>
        </div>
      </div>

      <!-- 4. 明细表格（含来源单据点击跳转） -->
      <div class="font-bold text-15px mb-8px flex items-center justify-between">
        <span>对账明细 ({{ detail?.items?.length || 0 }} 行)</span>
        <span class="text-12px font-normal text-gray-500"
          >点击来源单号可跳转检验单/入库单工作台</span
        >
      </div>

      <el-table :data="detail?.items || []" border stripe>
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="类型" prop="lineType" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.lineType === 'PROCESS'" type="primary" size="small"> 加工费 </el-tag>
            <el-tag v-else-if="row.lineType === 'PURCHASE'" type="success" size="small">
              采购货款
            </el-tag>
            <el-tag v-else-if="row.lineType === 'ADJUST'" type="warning" size="small">
              调整项
            </el-tag>
            <span v-else>{{ row.lineType }}</span>
          </template>
        </el-table-column>

        <el-table-column label="来源单号 / 来源" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <!-- 加工费行：检验单 -->
            <template v-if="row.lineType === 'PROCESS'">
              <el-link
                type="primary"
                :underline="true"
                @click="jumpToInspection(row.inspectionOrderId)"
                class="font-mono font-medium"
              >
                {{ row.inspectionOrderNo || '查看检验单' }}
              </el-link>
              <span class="text-xs text-gray-500 ml-1">
                ({{ row.inspectionType === 'GREIGE' ? '胚布织费' : '染费' }})
              </span>
            </template>
            <!-- 采购货款行：入库单 -->
            <template v-else-if="row.lineType === 'PURCHASE'">
              <el-link
                type="primary"
                :underline="true"
                @click="jumpToStockIn(row.stockInId)"
                class="font-mono font-medium"
              >
                {{ row.stockInNo || '查看入库单' }}
              </el-link>
              <div v-if="row.yarnOrderNo" class="text-xs text-gray-400 font-mono">
                纱线单: {{ row.yarnOrderNo }}
              </div>
            </template>
            <!-- 调整项行 -->
            <template v-else-if="row.lineType === 'ADJUST'">
              <span class="font-medium text-red-600">{{ row.adjustReason || '调整项' }}</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="产品" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.lineType !== 'ADJUST'">
              <div>{{ row.productName || '—' }}</div>
              <div v-if="row.productCode" class="text-xs text-gray-400 font-mono">
                {{ row.productCode }}
              </div>
            </div>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="批次/缸号" prop="batchNo" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-mono text-12px">{{ row.batchNo || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="数量明细" width="140" align="right">
          <template #default="{ row }">
            <template v-if="row.lineType === 'PROCESS'">
              <div class="text-12px">
                <span>{{ row.qtyKg != null ? `${row.qtyKg} KG` : '—' }}</span>
                <span class="text-gray-400 mx-1">/</span>
                <span>{{ row.qtyM != null ? `${row.qtyM} M` : '—' }}</span>
              </div>
              <div v-if="row.rollCount" class="text-11px text-gray-500">
                {{ row.rollCount }} 卷
              </div>
            </template>
            <template v-else-if="row.lineType === 'PURCHASE'">
              <span class="font-mono"
                >{{ row.billQty ?? row.qtyKg ?? row.qtyM ?? '—' }} {{ row.qtyUnit || '' }}</span
              >
            </template>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="计价基准" prop="pricingBasis" width="90" align="center">
          <template #default="{ row }">
            <template v-if="row.lineType === 'PROCESS'">
              <el-tag size="small" :type="row.pricingBasis === 'METER' ? 'success' : 'primary'">
                {{ row.pricingBasis === 'METER' ? '按米(M)' : '按公斤(KG)' }}
              </el-tag>
            </template>
            <template v-else-if="row.lineType === 'PURCHASE'">
              <span class="text-gray-600">{{ row.qtyUnit || row.pricingBasis || '—' }}</span>
            </template>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="结算数量" prop="billQty" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.lineType !== 'ADJUST'" class="font-mono">
              {{ row.billQty != null ? row.billQty : '—' }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="结算单价(元)" prop="unitPrice" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.lineType !== 'ADJUST'" class="font-mono">
              {{ row.unitPrice != null ? `¥ ${Number(row.unitPrice).toFixed(4)}` : '—' }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="行金额(元)" prop="lineAmount" width="120" align="right">
          <template #default="{ row }">
            <span
              class="font-mono font-bold"
              :class="Number(row.lineAmount || 0) < 0 ? 'text-red-600' : 'text-gray-900'"
            >
              ¥ {{ formatMoney(row.lineAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 付款申请弹窗 -->
    <SupplierReconPaymentApplyDialog
      ref="paymentApplyDialogRef"
      @success="handlePaymentApplySuccess"
    />
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import type { SupplierReconRespVO } from '@/api/lunaerp-elmui/finance/supplier-recon'
import {
  getSupplierRecon,
  confirmSupplierRecon,
  cancelSupplierRecon
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import { formatMoney, parseReconErrorMessage } from '../model/supplier-recon'
import { calculatePrintSubtotals } from './print/utils'
import PrintDispatcher from '@/components/Print/src/PrintDispatcher.vue'
import SupplierReconPaymentApplyDialog from './SupplierReconPaymentApplyDialog.vue'

defineOptions({ name: 'SupplierReconDetailDrawer' })

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const router = useRouter()
const visible = ref(false)
const loading = ref(false)
const detail = ref<SupplierReconRespVO | null>(null)
const paymentApplyDialogRef = ref<InstanceType<typeof SupplierReconPaymentApplyDialog>>()

const processCount = computed(() => {
  return (detail.value?.items || []).filter((it) => it.lineType === 'PROCESS').length
})

const purchaseCount = computed(() => {
  return (detail.value?.items || []).filter((it) => it.lineType === 'PURCHASE').length
})

const adjustCount = computed(() => {
  return (detail.value?.items || []).filter((it) => it.lineType === 'ADJUST').length
})

const subtotals = computed(() => {
  return calculatePrintSubtotals(detail.value?.items || [])
})

const open = async (id: number) => {
  visible.value = true
  await loadDetail(id)
}

const loadDetail = async (id: number) => {
  loading.value = true
  try {
    detail.value = await getSupplierRecon(id)
  } catch (err: any) {
    ElMessage.error(err?.msg || err?.message || '加载对账单详情失败')
  } finally {
    loading.value = false
  }
}

// 跳转检验单工作台
const jumpToInspection = (inspectionOrderId?: number) => {
  if (!inspectionOrderId) return
  router.push(`/production/inspection-order/workspace/${inspectionOrderId}`)
}

// 跳转入库单工作台
const jumpToStockIn = (stockInId?: number) => {
  if (!stockInId) return
  router.push(`/stock/stock-in/workspace/${stockInId}`)
}

// 跳转对账工作台
const handleGoWorkbench = (id?: number) => {
  if (!id) return
  router.push(`/finance/supplier-recon/workspace/${id}`)
}

// 确认对账单
const handleConfirmRecon = async (id?: number) => {
  if (!id) return
  try {
    await ElMessageBox.confirm(
      '确认后整单行数据将冻结，不可再编辑明细。是否确认该对账单？',
      '确认对账单',
      {
        confirmButtonText: '确定确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await confirmSupplierRecon(id)
    ElMessage.success('对账单已确认')
    await loadDetail(id)
    emit('refresh')
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  }
}

// 作废对账单
const handleCancelRecon = async (id?: number) => {
  if (!id) return
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
    await cancelSupplierRecon(id)
    ElMessage.success('对账单已作废')
    await loadDetail(id)
    emit('refresh')
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  }
}

// 打开生成付款申请弹窗
const handleOpenPaymentApply = () => {
  if (!detail.value) return
  paymentApplyDialogRef.value?.open(detail.value)
}

const handlePaymentApplySuccess = async () => {
  if (detail.value?.id) {
    await loadDetail(detail.value.id)
  }
  emit('refresh')
}

defineExpose({ open })
</script>
