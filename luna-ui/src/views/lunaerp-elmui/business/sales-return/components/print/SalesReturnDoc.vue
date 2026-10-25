<template>
  <div class="sales-return-doc-wrapper print-paper text-gray-900 mx-auto">
    <div
      v-for="(sheet, sIdx) in sheetList"
      :key="sheet.id || sIdx"
      class="sheet-page relative"
      :class="{ 'page-break-after': sIdx < sheetList.length - 1 }"
    >
      <!-- 草稿态水印 -->
      <div v-if="sheet.status === 'DRAFT'" class="draft-watermark"> 草稿 DRAFT </div>

      <!-- 1. 单据抬头 -->
      <div class="print-header text-center mb-12px">
        <div class="main-title text-22px font-bold tracking-widest text-black mb-4px">
          销售退货单
        </div>
        <div class="sub-title text-11px text-gray-500 uppercase tracking-wider">
          SALES RETURN NOTE
        </div>
      </div>

      <!-- 2. 单据核心元数据 -->
      <div class="meta-box mb-12px text-11px border border-gray-800 p-8px bg-gray-50">
        <div class="meta-grid grid grid-cols-3 gap-y-6px gap-x-12px">
          <div class="meta-item">
            <span class="label text-gray-600">退货单号：</span>
            <span class="value font-mono font-bold">{{ sheet.returnNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">退货日期：</span>
            <span class="value font-medium">{{ sheet.returnDate || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">单据状态：</span>
            <span class="value font-semibold">
              {{ formatStatusText(sheet.status) }}
              <span v-if="sheet.approvalStatus" class="text-gray-500 text-10px">
                ({{ formatApprovalStatusText(sheet.approvalStatus) }})
              </span>
            </span>
          </div>

          <div class="meta-item span-2">
            <span class="label text-gray-600">客户名称：</span>
            <span class="value font-bold">{{ sheet.customerName || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">联系电话：</span>
            <span class="value">{{ sheet.contactPhone || '—' }}</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">联系人：</span>
            <span class="value">{{ sheet.contactName || '—' }}</span>
          </div>
          <div class="meta-item span-2">
            <span class="label text-gray-600">客户地址：</span>
            <span class="value">{{ sheet.customerAddress || '—' }}</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">原发货单：</span>
            <span class="value font-mono font-medium">{{ sheet.shipmentNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">关联合同：</span>
            <span class="value font-mono">{{ sheet.contractNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">退入仓库：</span>
            <span class="value font-medium">{{ sheet.warehouseName || '—' }}</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">结算币种：</span>
            <span class="value font-bold">{{ sheet.currency || 'RMB' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">制单人：</span>
            <span class="value">{{ sheet.creator || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">打印时间：</span>
            <span class="value">{{ formatPrintTime(sheet.printTime) }}</span>
          </div>

          <div v-if="sheet.stockInNo" class="meta-item">
            <span class="label text-gray-600">关联入库：</span>
            <span class="value font-mono"
              >{{ sheet.stockInNo }} ({{ sheet.stockInStatus || '' }})</span
            >
          </div>

          <div v-if="sheet.remark" class="meta-item span-3">
            <span class="label text-gray-600">退货备注：</span>
            <span class="value">{{ sheet.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 3. 退货明细与卷清单 -->
      <table class="print-table w-full text-10px border-collapse mb-12px">
        <thead>
          <tr class="bg-gray-100 font-bold">
            <th class="w-28px text-center">#</th>
            <th class="w-110px text-left">产品编码 / 名称</th>
            <th class="w-75px text-left">客户色号</th>
            <th class="w-65px text-left">颜色</th>
            <th class="w-70px text-left">缸号</th>
            <th class="w-65px text-right">退货数量</th>
            <th class="w-36px text-center">单位</th>
            <th class="w-40px text-right">匹数</th>
            <th class="w-65px text-right">单价</th>
            <th class="w-75px text-right">退货金额</th>
            <th class="w-65px text-center">退货原因</th>
            <th class="text-left">卷明细 (卷号/数量/质检)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in sheet.items || []" :key="item.id || idx">
            <td class="text-center">{{ idx + 1 }}</td>
            <td>
              <div class="font-mono text-9.5px text-gray-600">{{ item.productCode || '—' }}</div>
              <div class="font-medium">{{ item.productName || '—' }}</div>
            </td>
            <td>{{ item.customerColorNo || '—' }}</td>
            <td>{{ item.colorName || '—' }}</td>
            <td class="font-mono">{{ item.vatNo || '—' }}</td>
            <td class="text-right font-mono font-medium">
              {{ item.returnQty != null ? item.returnQty : '—' }}
            </td>
            <td class="text-center">{{ item.unit || '—' }}</td>
            <td class="text-right font-mono">{{ item.pieces != null ? item.pieces : 1 }}</td>
            <td class="text-right font-mono">
              {{ item.unitPrice != null ? Number(item.unitPrice).toFixed(2) : '—' }}
            </td>
            <td class="text-right font-mono font-bold">
              {{ item.amount != null ? Number(item.amount).toFixed(2) : '0.00' }}
            </td>
            <td class="text-center">
              {{ formatReasonText(item.returnReason) }}
            </td>
            <td>
              <div v-if="item.rolls && item.rolls.length > 0" class="roll-tags-wrap">
                <span v-for="roll in item.rolls" :key="roll.id || roll.rollNo" class="roll-tag">
                  {{ roll.rollNo }} ({{ formatRollQty(item.unit, roll) }})
                  <template v-if="roll.qcResult">
                    [{{ roll.qcResult === 'RESTOCK' ? '入库' : '报废' }}]
                  </template>
                </span>
              </div>
              <span v-else class="text-gray-400">—</span>
            </td>
          </tr>
          <tr v-if="!sheet.items || sheet.items.length === 0">
            <td colspan="12" class="text-center py-12px text-gray-400">暂无退货明细数据</td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
            <td colspan="5" class="text-center">合计</td>
            <td colspan="2" class="text-right font-mono">{{
              computeSalesReturnTotalQtyText(sheet.items)
            }}</td>
            <td class="text-right font-mono">{{
              sheet.totalPieces ?? calcTotalPieces(sheet.items)
            }}</td>
            <td></td>
            <td class="text-right font-mono text-12px">
              {{ sheet.currency || 'RMB' }}
              {{ Number(sheet.totalAmount ?? calcTotalAmount(sheet.items)).toFixed(2) }}
            </td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>

      <!-- 4. 汇总与大写金额 -->
      <div class="summary-box border border-gray-800 p-8px mb-14px text-11px bg-gray-50">
        <div class="flex justify-between items-center mb-4px">
          <div>
            <span class="text-gray-600">总退货量：</span>
            <span class="font-bold mr-16px font-mono">{{
              computeSalesReturnTotalQtyText(sheet.items)
            }}</span>
            <span class="text-gray-600">总匹数：</span>
            <span class="font-bold mr-16px"
              >{{ sheet.totalPieces ?? calcTotalPieces(sheet.items) }} 匹</span
            >
            <span class="text-gray-600">退货总金额：</span>
            <span class="font-bold text-13px text-blue-800">
              {{ sheet.currency || 'RMB' }}
              {{ Number(sheet.totalAmount ?? calcTotalAmount(sheet.items)).toFixed(2) }}
            </span>
          </div>
        </div>
        <div>
          <span class="text-gray-600">金额大写：</span>
          <span class="font-bold tracking-wider">
            {{ formatChineseAmount(sheet.totalAmount ?? calcTotalAmount(sheet.items)) }}
          </span>
        </div>
      </div>

      <!-- 5. 签字留痕区 -->
      <div class="sign-area text-11px">
        <div class="sign-item">
          <span>制单人员：</span>
          <span class="sign-line">{{ sheet.creator || '' }}</span>
        </div>
        <div class="sign-item">
          <span>客户签章：</span>
          <span class="sign-line"></span>
        </div>
        <div class="sign-item">
          <span>质检核验：</span>
          <span class="sign-line">{{ sheet.arrivedBy ? `UID:${sheet.arrivedBy}` : '' }}</span>
        </div>
        <div class="sign-item">
          <span>财务审核：</span>
          <span class="sign-line">{{ sheet.postedBy ? `UID:${sheet.postedBy}` : '' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import type { SalesReturnPrintVO } from '@/api/lunaerp-elmui/business/sales-return'
import {
  formatStatusText,
  formatApprovalStatusText,
  formatReasonText,
  formatRollQty,
  formatPrintTime,
  computeSalesReturnTotalQtyText,
  calcTotalPieces,
  calcTotalAmount,
  formatChineseAmount
} from './utils'

defineOptions({ name: 'SalesReturnDoc' })

const props = defineProps({
  data: {
    type: Object as PropType<SalesReturnPrintVO>,
    default: null
  },
  list: {
    type: Array as PropType<SalesReturnPrintVO[]>,
    default: () => []
  }
})

const sheetList = computed<SalesReturnPrintVO[]>(() => {
  if (props.data) {
    return [props.data]
  }
  return props.list || []
})
</script>
