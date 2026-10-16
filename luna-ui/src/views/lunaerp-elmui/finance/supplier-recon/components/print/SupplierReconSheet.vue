<template>
  <div class="supplier-recon-sheet-wrapper print-paper text-gray-900 mx-auto">
    <div
      v-for="(sheet, sIdx) in sheetList"
      :key="sheet.id || sIdx"
      class="sheet-page"
      :class="{ 'page-break-after': sIdx < sheetList.length - 1 }"
    >
      <!-- 1. 单据抬头 -->
      <div class="print-header text-center mb-12px">
        <div class="main-title text-22px font-bold tracking-widest text-black mb-4px">
          {{ sheet.title || '供应商对账单' }}
        </div>
        <div class="sub-title text-11px text-gray-500 uppercase tracking-wider">
          SUPPLIER RECONCILIATION STATEMENT
        </div>
      </div>

      <!-- 2. 核心元数据（单号/区间/供应商/状态/确认信息） -->
      <div class="meta-box mb-12px text-11px border border-gray-800 p-8px bg-gray-50">
        <div class="meta-grid grid grid-cols-3 gap-y-6px gap-x-12px">
          <div class="meta-item">
            <span class="label text-gray-600">对账单号：</span>
            <span class="value font-mono font-bold">{{ sheet.reconNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">对账区间：</span>
            <span class="value font-medium"
              >{{ sheet.periodStart || '—' }} 至 {{ sheet.periodEnd || '—' }}</span
            >
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">单据状态：</span>
            <span class="value font-semibold">
              {{ formatStatusText(sheet.status) }}
            </span>
          </div>

          <div class="meta-item span-2">
            <span class="label text-gray-600">供应商名称：</span>
            <span class="value font-bold">{{ sheet.supplierName || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">联系电话：</span>
            <span class="value">{{ sheet.supplierPhone || sheet.supplierContact || '—' }}</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">制单人：</span>
            <span class="value">{{ sheet.creator || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">确认人/时间：</span>
            <span class="value">
              {{
                sheet.confirmedByName
                  ? `${sheet.confirmedByName} / ${sheet.confirmedAt || ''}`
                  : '未确认'
              }}
            </span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">打印时间：</span>
            <span class="value">{{ formatPrintTime(sheet.printTime) }}</span>
          </div>

          <div v-if="sheet.remark" class="meta-item span-3">
            <span class="label text-gray-600">对账备注：</span>
            <span class="value">{{ sheet.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 3. 三类行表格（加工费/采购货款/调整项） -->
      <table class="print-table w-full text-10px border-collapse mb-12px">
        <thead>
          <tr class="bg-gray-100 font-bold">
            <th class="w-32px text-center">#</th>
            <th class="w-65px text-center">类型</th>
            <th class="w-130px text-left">来源单号 / 说明</th>
            <th class="w-130px text-left">产品信息</th>
            <th class="w-75px text-left">批次/缸号</th>
            <th class="w-95px text-right">数量明细</th>
            <th class="w-65px text-center">计价基准</th>
            <th class="w-65px text-right">结算量</th>
            <th class="w-65px text-right">单价(元)</th>
            <th class="w-75px text-right">金额(元)</th>
            <th class="text-left">备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in sheet.items || []" :key="item.id || idx">
            <td class="text-center">{{ idx + 1 }}</td>
            <td class="text-center font-medium">
              <span
                :class="{
                  'text-blue-700': item.lineType === 'PROCESS',
                  'text-emerald-700': item.lineType === 'PURCHASE',
                  'text-amber-700': item.lineType === 'ADJUST'
                }"
              >
                {{ formatLineTypePrintLabel(item.lineType) }}
              </span>
            </td>
            <td class="text-left font-mono">
              <template v-if="item.lineType === 'PROCESS'">
                <div>{{ item.inspectionOrderNo || '—' }}</div>
                <div class="text-9px text-gray-500">{{
                  formatInspectionTypeLabel(item.inspectionType)
                }}</div>
              </template>
              <template v-else-if="item.lineType === 'PURCHASE'">
                <div>{{ item.stockInNo || '—' }}</div>
                <div v-if="item.yarnOrderNo" class="text-9px text-gray-500"
                  >纱线单: {{ item.yarnOrderNo }}</div
                >
              </template>
              <template v-else-if="item.lineType === 'ADJUST'">
                <div class="text-red-600 font-bold">{{ item.adjustReason || '调整扣补' }}</div>
              </template>
            </td>
            <td class="text-left">
              <template v-if="item.lineType !== 'ADJUST'">
                <div class="font-medium">{{ item.productName || '—' }}</div>
                <div v-if="item.productCode" class="text-9px text-gray-500 font-mono">{{
                  item.productCode
                }}</div>
              </template>
              <span v-else class="text-gray-400">—</span>
            </td>
            <td class="text-left font-mono text-9px">
              {{ item.batchNo || '—' }}
            </td>
            <td class="text-right">
              <template v-if="item.lineType === 'PROCESS'">
                <div>{{ item.qtyKg != null ? `${item.qtyKg} KG` : '—' }}</div>
                <div class="text-9px text-gray-500">{{
                  item.qtyM != null ? `${item.qtyM} M` : ''
                }}</div>
              </template>
              <template v-else-if="item.lineType === 'PURCHASE'">
                <div
                  >{{ item.billQty ?? item.qtyKg ?? item.qtyM ?? '—' }}
                  {{ item.qtyUnit || '' }}</div
                >
              </template>
              <span v-else class="text-gray-400">—</span>
            </td>
            <td class="text-center">
              <template v-if="item.lineType === 'PROCESS'">
                {{ formatPricingBasisLabel(item.pricingBasis) }}
              </template>
              <template v-else-if="item.lineType === 'PURCHASE'">
                {{ item.qtyUnit || formatPricingBasisLabel(item.pricingBasis) }}
              </template>
              <span v-else class="text-gray-400">—</span>
            </td>
            <td class="text-right font-mono">
              <template v-if="item.lineType !== 'ADJUST'">
                {{ item.billQty != null ? item.billQty : '—' }}
              </template>
              <span v-else class="text-gray-400">—</span>
            </td>
            <td class="text-right font-mono">
              <template v-if="item.lineType !== 'ADJUST'">
                {{ item.unitPrice != null ? Number(item.unitPrice).toFixed(4) : '—' }}
              </template>
              <span v-else class="text-gray-400">—</span>
            </td>
            <td
              class="text-right font-mono font-semibold"
              :class="{ 'text-red-600': Number(item.lineAmount || 0) < 0 }"
            >
              {{ formatMoney(item.lineAmount) }}
            </td>
            <td class="text-left text-9px text-gray-600">
              {{ item.remark || '—' }}
            </td>
          </tr>

          <tr v-if="!sheet.items || sheet.items.length === 0">
            <td colspan="11" class="text-center py-16px text-gray-400">无可对账明细</td>
          </tr>
        </tbody>
      </table>

      <!-- 4. 金额汇总栏（分类小计 + 总确认金额） -->
      <div
        class="amount-summary-box mb-16px border border-gray-800 p-8px bg-gray-50 flex justify-between items-center text-11px"
      >
        <div class="amount-subtotals flex gap-16px text-gray-700">
          <div>
            加工费小计：
            <span class="font-mono font-bold text-black">
              ¥
              {{
                formatMoney(
                  sheet.totalProcessAmount ?? calculateSubtotals(sheet.items).processTotal
                )
              }}
            </span>
          </div>
          <div>
            采购货款小计：
            <span class="font-mono font-bold text-black">
              ¥
              {{
                formatMoney(
                  sheet.totalPurchaseAmount ?? calculateSubtotals(sheet.items).purchaseTotal
                )
              }}
            </span>
          </div>
          <div>
            调整项小计：
            <span
              class="font-mono font-bold text-black"
              :class="{ 'text-red-600': Number(sheet.totalAdjustAmount || 0) < 0 }"
            >
              ¥
              {{
                formatMoney(sheet.totalAdjustAmount ?? calculateSubtotals(sheet.items).adjustTotal)
              }}
            </span>
          </div>
        </div>
        <div class="amount-grand-total text-14px font-bold text-black">
          对账总金额：
          <span class="text-16px font-mono text-emerald-800">
            ¥ {{ formatMoney(sheet.totalAmount) }}
          </span>
        </div>
      </div>

      <!-- 5. 签章区（甲乙双方签字盖章） -->
      <div class="signature-box grid grid-cols-2 gap-20px mt-20px">
        <div
          class="sign-col border border-gray-600 p-12px min-h-100px flex flex-col justify-between"
        >
          <div
            class="sign-title font-bold text-12px border-b border-dashed border-gray-300 pb-4px mb-8px"
          >
            甲方（确认单位盖章）：
          </div>
          <div class="sign-lines text-11px flex flex-col gap-6px">
            <div class="sign-row flex justify-between">
              <span>财务经办人：___________________</span>
              <span>审核人：___________________</span>
            </div>
            <div class="sign-row flex justify-between">
              <span>日期：______年____月____日</span>
            </div>
          </div>
        </div>

        <div
          class="sign-col border border-gray-600 p-12px min-h-100px flex flex-col justify-between"
        >
          <div
            class="sign-title font-bold text-12px border-b border-dashed border-gray-300 pb-4px mb-8px"
          >
            乙方（供应商确认盖章）：
          </div>
          <div class="sign-lines text-11px flex flex-col gap-6px">
            <div class="sign-row flex justify-between">
              <span>授权代表签字：___________________</span>
              <span>联系电话：___________________</span>
            </div>
            <div class="sign-row flex justify-between">
              <span>确认盖章日期：______年____月____日</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type {
  SupplierReconPrintRespVO,
  SupplierReconPrintItemRespVO
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import { formatMoney } from '../../model/supplier-recon'
import {
  formatLineTypePrintLabel,
  formatInspectionTypeLabel,
  formatPricingBasisLabel,
  calculatePrintSubtotals,
  formatPrintTime
} from './utils'

defineOptions({ name: 'SupplierReconSheet' })

const props = defineProps<{
  data?: SupplierReconPrintRespVO
  list?: SupplierReconPrintRespVO[]
}>()

const sheetList = computed<SupplierReconPrintRespVO[]>(() => {
  if (props.list && props.list.length > 0) {
    return props.list
  }
  return props.data ? [props.data] : []
})

const formatStatusText = (status?: string) => {
  switch (status) {
    case 'CONFIRMED':
      return '已确认'
    case 'DRAFT':
      return '草稿'
    case 'CANCELLED':
      return '已作废'
    default:
      return status || '—'
  }
}

const calculateSubtotals = (items?: SupplierReconPrintItemRespVO[]) => {
  return calculatePrintSubtotals(items || [])
}
</script>
