<template>
  <div class="supplier-recon-print-wrapper print-paper text-gray-900 mx-auto">
    <!-- 1. 对账单抬头 -->
    <div class="print-header text-center mb-16px">
      <div class="text-22px font-bold tracking-widest text-black mb-4px">
        {{ recon?.companyTitle || '供应商对账单' }}
      </div>
      <div class="text-11px text-gray-500 uppercase tracking-wider">
        SUPPLIER RECONCILIATION STATEMENT
      </div>
    </div>

    <!-- 2. 对账单核心元数据 -->
    <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
      <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
        <div>
          <span class="text-gray-600">对账单号：</span>
          <span class="font-mono font-bold">{{ recon?.reconNo || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">供应商名称：</span>
          <span class="font-bold">{{ recon?.supplierName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">对账区间：</span>
          <span class="font-mono font-medium">
            {{ recon?.periodStart && recon?.periodEnd ? `${recon.periodStart} ~ ${recon.periodEnd}` : '—' }}
          </span>
        </div>

        <div>
          <span class="text-gray-600">单据状态：</span>
          <span class="font-bold" :class="statusClass">{{ statusText }}</span>
        </div>
        <div>
          <span class="text-gray-600">确认人：</span>
          <span>{{ recon?.confirmedByName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">确认时间：</span>
          <span>{{ recon?.confirmedAt ? formatTimeStr(recon.confirmedAt) : '—' }}</span>
        </div>

        <div v-if="recon?.remark" class="col-span-3">
          <span class="text-gray-600">单据备注：</span>
          <span>{{ recon.remark }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 三类行明细表格 -->
    <table class="print-table w-full text-11px border-collapse mb-12px">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-32px text-center">#</th>
          <th class="w-65px text-center">行类型</th>
          <th class="w-110px text-left">来源单号</th>
          <th class="text-left">产品 / 批次缸号</th>
          <th class="w-90px text-right">数量明细</th>
          <th class="w-65px text-center">计价基准</th>
          <th class="w-70px text-right">结算量</th>
          <th class="w-70px text-right">单价(元)</th>
          <th class="w-85px text-right">金额(元)</th>
          <th class="w-100px text-left">调整原因 / 备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in recon?.items || []" :key="item.id || idx">
          <td class="text-center">{{ idx + 1 }}</td>
          <td class="text-center">
            <span
              class="px-4px py-1px rounded text-10px font-medium"
              :class="getLineTypeBadgeClass(item.lineType)"
            >
              {{ getLineTypeLabel(item.lineType) }}
            </span>
          </td>
          <!-- 来源单号 -->
          <td class="font-mono">
            <template v-if="item.lineType === 'PROCESS'">
              {{ item.inspectionOrderNo || '—' }}
              <span v-if="item.inspectionType" class="text-gray-500 text-10px block">
                ({{ item.inspectionType === 'GREIGE' ? '织费' : '染费' }})
              </span>
            </template>
            <template v-else-if="item.lineType === 'PURCHASE'">
              {{ item.stockInNo || '—' }}
              <span v-if="item.yarnOrderNo" class="text-gray-500 text-10px block">
                {{ item.yarnOrderNo }}
              </span>
            </template>
            <template v-else>
              <span class="text-gray-400">手工调整</span>
            </template>
          </td>
          <!-- 产品 / 批次 -->
          <td>
            <div class="font-medium">{{ item.productName || item.productCode || (item.lineType === 'ADJUST' ? '—' : '—') }}</div>
            <div v-if="item.batchNo" class="text-gray-500 text-10px font-mono">
              批次/缸号: {{ item.batchNo }}
            </div>
          </td>
          <!-- 数量明细 -->
          <td class="text-right font-mono">
            <template v-if="item.lineType === 'PROCESS'">
              <div v-if="item.qtyKg != null">{{ Number(item.qtyKg).toFixed(2) }} KG</div>
              <div v-if="item.qtyM != null" class="text-gray-500 text-10px">{{ Number(item.qtyM).toFixed(2) }} M</div>
            </template>
            <template v-else-if="item.lineType === 'PURCHASE'">
              {{ item.qtyKg != null ? `${Number(item.qtyKg).toFixed(2)} KG` : (item.qtyM != null ? `${Number(item.qtyM).toFixed(2)} M` : '—') }}
            </template>
            <template v-else>
              —
            </template>
          </td>
          <!-- 计价基准 -->
          <td class="text-center font-mono">
            <span v-if="item.pricingBasis">{{ item.pricingBasis }}</span>
            <span v-else>—</span>
          </td>
          <!-- 结算量 -->
          <td class="text-right font-mono font-medium">
            {{ item.billQty != null ? Number(item.billQty).toFixed(2) : '—' }}
          </td>
          <!-- 单价 -->
          <td class="text-right font-mono">
            {{ item.unitPrice != null ? Number(item.unitPrice).toFixed(4) : '—' }}
          </td>
          <!-- 金额 -->
          <td class="text-right font-mono font-bold" :class="item.lineAmount && item.lineAmount < 0 ? 'text-red-600' : ''">
            {{ item.lineAmount != null ? formatAmount(item.lineAmount) : '0.00' }}
          </td>
          <!-- 调整原因 / 备注 -->
          <td class="text-10px">
            <span v-if="item.adjustReason" class="text-red-700 font-medium mr-4px">
              [{{ item.adjustReason }}]
            </span>
            <span>{{ item.remark || '' }}</span>
          </td>
        </tr>
        <tr v-if="!recon?.items?.length">
          <td colspan="10" class="text-center py-16px text-gray-400">暂无对账明细数据</td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
          <td colspan="4" class="text-center">
            合计（加工费 {{ processCount }} 行，采购货款 {{ purchaseCount }} 行，调整项 {{ adjustCount }} 行）
          </td>
          <td colspan="4" class="text-right text-gray-600">确认金额合计：</td>
          <td class="text-right font-mono text-12px">
            ¥ {{ formatAmount(recon?.totalAmount) }}
          </td>
          <td></td>
        </tr>
      </tfoot>
    </table>

    <!-- 4. 汇总与大写金额 -->
    <div class="summary-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50">
      <div class="flex justify-between items-center mb-6px">
        <div>
          <span class="text-gray-600">明细行总数：</span>
          <span class="font-bold mr-16px">{{ recon?.items?.length || 0 }} 行</span>
          <span class="text-gray-600">对账确认金额：</span>
          <span class="font-bold text-14px text-blue-800 font-mono mr-16px">
            ¥ {{ formatAmount(recon?.totalAmount) }}
          </span>
        </div>
      </div>
      <div class="text-12px">
        <span class="text-gray-600">金额大写：</span>
        <span class="font-bold tracking-wider">{{ uppercaseAmount }}</span>
      </div>
    </div>

    <!-- 5. 双方盖章与签字确认栏 -->
    <div class="sign-block mt-24px text-12px">
      <div class="grid grid-cols-2 gap-24px pt-8px border-t border-gray-400">
        <!-- 甲方：本公司 -->
        <div class="border border-dashed border-gray-400 p-12px rounded bg-white">
          <div class="font-bold mb-12px text-13px text-gray-800">本公司财务确认：</div>
          <div class="space-y-8px">
            <div>
              <span class="text-gray-600">对账经办：</span>
              <span class="font-medium underline decoration-gray-400 underline-offset-4">
                {{ recon?.confirmedByName || '系统经办' }}
              </span>
            </div>
            <div>
              <span class="text-gray-600">财务复核：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">盖章确认：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">确认日期：</span>
              <span>{{ recon?.confirmedAt ? formatTimeStr(recon.confirmedAt).substring(0, 10) : '_____年____月____日' }}</span>
            </div>
          </div>
        </div>

        <!-- 乙方：供应商 -->
        <div class="border border-dashed border-gray-400 p-12px rounded bg-white">
          <div class="font-bold mb-12px text-13px text-gray-800">供应商核对确认：</div>
          <div class="space-y-8px">
            <div>
              <span class="text-gray-600">供应商对账人：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">联系电话：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">公章 / 财务专用章：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">确认日期：</span>
              <span>_____年____月____日</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-between items-center text-10px text-gray-500 mt-16px pt-6px border-t border-dashed border-gray-300">
        <div>注：请供应商核对上述明细与结算金额，盖章确认后请于5个工作日内回传。</div>
        <div>打印时间：{{ printTimeStr }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import type { SupplierReconPrintVO } from '@/api/lunaerp-elmui/finance/supplier-recon'
import { formatRmbUppercase, formatAmount, formatPrintTime } from './utils'

defineOptions({ name: 'SupplierReconPrintTemplate' })

const props = defineProps({
  /** 单张对账单数据 */
  data: {
    type: Object as PropType<SupplierReconPrintVO | null>,
    default: null
  },
  /** 批量对账单列表（可选） */
  list: {
    type: Array as PropType<SupplierReconPrintVO[]>,
    default: () => []
  }
})

const recon = computed<SupplierReconPrintVO | null>(() => {
  if (props.data) return props.data
  if (props.list && props.list.length > 0) return props.list[0]
  return null
})

const statusText = computed(() => {
  const status = recon.value?.status
  if (status === 'CONFIRMED') return '已确认'
  if (status === 'CANCELLED') return '已作废'
  return '草稿'
})

const statusClass = computed(() => {
  const status = recon.value?.status
  if (status === 'CONFIRMED') return 'text-green-700'
  if (status === 'CANCELLED') return 'text-gray-500'
  return 'text-yellow-600'
})

const processCount = computed(() => {
  return recon.value?.items?.filter((i) => i.lineType === 'PROCESS').length || 0
})

const purchaseCount = computed(() => {
  return recon.value?.items?.filter((i) => i.lineType === 'PURCHASE').length || 0
})

const adjustCount = computed(() => {
  return recon.value?.items?.filter((i) => i.lineType === 'ADJUST').length || 0
})

const uppercaseAmount = computed(() => {
  return formatRmbUppercase(recon.value?.totalAmount)
})

const printTimeStr = computed(() => {
  return formatPrintTime(recon.value?.printTime)
})

const formatTimeStr = (t?: string | Date) => {
  return formatPrintTime(t)
}

const getLineTypeLabel = (lineType?: string) => {
  if (lineType === 'PROCESS') return '加工费'
  if (lineType === 'PURCHASE') return '采购货款'
  if (lineType === 'ADJUST') return '调整项'
  return lineType || '—'
}

const getLineTypeBadgeClass = (lineType?: string) => {
  if (lineType === 'PROCESS') return 'bg-blue-100 text-blue-800'
  if (lineType === 'PURCHASE') return 'bg-yellow-100 text-yellow-800'
  if (lineType === 'ADJUST') return 'bg-red-100 text-red-800'
  return 'bg-gray-100 text-gray-800'
}
</script>

<style scoped>
.supplier-recon-print-wrapper {
  width: 100%;
  max-width: 820px;
  min-height: 500px;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 4px 6px;
}

@media print {
  @page {
    size: portrait;
    margin: 10mm;
  }
  .supplier-recon-print-wrapper {
    width: 100%;
    margin: 0;
  }
}
</style>
