<template>
  <div class="shipment-doc-wrapper print-paper text-gray-900 mx-auto">
    <!-- 1. 发货单抬头与右上角签收二维码 -->
    <div class="print-header text-center mb-16px relative">
      <div class="text-22px font-bold tracking-widest text-black mb-4px">销售发货单</div>
      <div class="text-11px text-gray-500 uppercase tracking-wider">随货同行凭单 / DELIVERY NOTE</div>

      <!-- 右上角客户签收二维码（W0 打印，14 号接签收页；缺失时展示提示占位） -->
      <div class="header-qr absolute right-0 top-0">
        <QrSlot
          :text="shipment?.qrUrl || ''"
          :width="64"
          :margin="1"
        >
          <template #label>
            <span class="text-9px text-gray-600 block scale-90 whitespace-nowrap">
              {{ shipment?.qrUrl ? '扫码签收' : '门户未配' }}
            </span>
          </template>
        </QrSlot>
      </div>
    </div>

    <!-- 2. 单据核心元数据 -->
    <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
      <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
        <div>
          <span class="text-gray-600">发货单号：</span>
          <span class="font-mono font-bold">{{ shipment?.shipmentNo || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">发货日期：</span>
          <span class="font-medium">{{ shipment?.shippingDate || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">销售合同：</span>
          <span class="font-mono font-medium">{{ shipment?.contractNo || '—' }}</span>
        </div>

        <div>
          <span class="text-gray-600">客户名称：</span>
          <span class="font-bold">{{ shipment?.customerName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">发货仓库：</span>
          <span class="font-medium">{{ shipment?.warehouseName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">业务人员：</span>
          <span>{{ shipment?.salespersonName || '—' }}</span>
        </div>

        <div>
          <span class="text-gray-600">承运物流：</span>
          <span>{{ shipment?.carrier || '—' }}</span>
        </div>
        <div class="col-span-2">
          <span class="text-gray-600">物流运单号：</span>
          <span class="font-mono">{{ shipment?.trackingNumber || '—' }}</span>
        </div>

        <div class="col-span-3">
          <span class="text-gray-600">发货地址：</span>
          <span>{{ shipment?.shippingAddress || '—' }}</span>
        </div>
        <div class="col-span-3">
          <span class="text-gray-600">收货地址：</span>
          <span class="font-medium">{{ shipment?.receiveAddress || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 明细表格 -->
    <table class="print-table w-full text-11px border-collapse mb-12px">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-32px text-center">#</th>
          <th class="text-left">产品名称</th>
          <th class="w-85px text-left">产品编码</th>
          <th class="w-75px text-left">客户色号</th>
          <th class="w-65px text-left">颜色</th>
          <th class="w-75px text-left">缸号/批次</th>
          <th class="w-75px text-right">发货数量</th>
          <th class="w-45px text-center">单位</th>
          <th class="w-50px text-right">匹数</th>
          <th class="w-65px text-right">单价(元)</th>
          <th class="w-80px text-right">金额(元)</th>
          <th class="w-80px text-left">备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in shipment?.items || []" :key="item.id || idx">
          <td class="text-center">{{ idx + 1 }}</td>
          <td class="font-medium">{{ item.productName || '—' }}</td>
          <td class="font-mono">{{ item.productCode || '—' }}</td>
          <td>{{ item.customerColorNo || '—' }}</td>
          <td>{{ item.colorName || '—' }}</td>
          <td class="font-mono">{{ item.vatNo || '—' }}</td>
          <td class="text-right font-mono font-medium">{{ item.quantity != null ? item.quantity : '—' }}</td>
          <td class="text-center">{{ item.unit || '—' }}</td>
          <td class="text-right font-mono">{{ item.pieces != null ? `${item.pieces}` : '—' }}</td>
          <td class="text-right font-mono">{{ item.unitPrice != null ? Number(item.unitPrice).toFixed(2) : '—' }}</td>
          <td class="text-right font-mono font-bold">{{ item.amount != null ? Number(item.amount).toFixed(2) : '0.00' }}</td>
          <td>{{ item.remark || '' }}</td>
        </tr>
        <tr v-if="!shipment?.items?.length">
          <td colspan="12" class="text-center py-12px text-gray-400">暂无明细数据</td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
          <td colspan="6" class="text-center">合计</td>
          <td class="text-right font-mono">{{ totalQuantityText }}</td>
          <td></td>
          <td class="text-right font-mono">{{ totalPiecesCount }}</td>
          <td></td>
          <td class="text-right font-mono text-12px">¥ {{ (shipment?.totalAmount || itemsTotalAmount).toFixed(2) }}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>

    <!-- 4. 费用与大写金额汇总栏 -->
    <div class="summary-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50">
      <div class="flex justify-between items-center mb-6px">
        <div>
          <span class="text-gray-600">总匹数：</span>
          <span class="font-bold mr-16px">{{ totalPiecesCount }} 匹</span>
          <span class="text-gray-600">货物金额：</span>
          <span class="font-bold mr-16px">¥ {{ (shipment?.totalAmount || itemsTotalAmount).toFixed(2) }}</span>
          <span class="text-gray-600">运费：</span>
          <span class="font-bold mr-16px">¥ {{ Number(shipment?.freightFee || 0).toFixed(2) }}</span>
          <span class="text-gray-600">总计金额（含运费）：</span>
          <span class="font-bold text-14px text-blue-700">¥ {{ grandTotalAmount.toFixed(2) }}</span>
        </div>
      </div>
      <div class="text-12px">
        <span class="text-gray-600">金额大写：</span>
        <span class="font-bold tracking-wider">{{ uppercaseAmount }}</span>
      </div>
      <div v-if="shipment?.remark" class="text-11px text-gray-600 mt-4px">
        <span>单据备注：</span>{{ shipment.remark }}
      </div>
    </div>

    <!-- 5. 签字留痕栏 -->
    <div class="sign-block mt-24px text-12px">
      <div class="grid grid-cols-4 gap-12px pt-10px">
        <div>
          <span class="text-gray-600">制单人：</span>
          <span class="font-medium underline decoration-gray-400 underline-offset-4">{{ shipment?.creator || '系统' }}</span>
        </div>
        <div>
          <span class="text-gray-600">发货仓管：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
        <div>
          <span class="text-gray-600">承运司机：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
        <div>
          <span class="text-gray-600">客户签收：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
      </div>
      <div class="flex justify-between items-center text-10px text-gray-500 mt-20px pt-6px border-t border-dashed border-gray-300">
        <div>签收日期：_____年____月____日</div>
        <div>随货同行白联：客户留存 / 红联：财务记账 / 黄联：仓库留存</div>
        <div>打印时间：{{ printTimeStr }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import QrSlot from '@/components/Print/src/QrSlot.vue'
import type { ShipmentPrintVO } from '@/api/lunaerp-elmui/business/shipment'
import {
  formatRmbUppercase,
  computeGrandTotalAmount,
  computeTotalPieces,
  computeTotalQuantityText,
  computeItemsTotalAmount,
  formatPrintTime
} from './utils'

defineOptions({ name: 'ShipmentDoc' })

const props = defineProps({
  /** 单张发货单数据 */
  data: {
    type: Object as PropType<ShipmentPrintVO | null>,
    default: null
  },
  /** 批量发货单列表（可选） */
  list: {
    type: Array as PropType<ShipmentPrintVO[]>,
    default: () => []
  }
})

const shipment = computed<ShipmentPrintVO | null>(() => {
  if (props.data) return props.data
  if (props.list && props.list.length > 0) return props.list[0]
  return null
})

const itemsTotalAmount = computed(() => {
  return computeItemsTotalAmount(shipment.value?.items)
})

const grandTotalAmount = computed(() => {
  return computeGrandTotalAmount(shipment.value)
})

const totalPiecesCount = computed(() => {
  return computeTotalPieces(shipment.value)
})

const totalQuantityText = computed(() => {
  return computeTotalQuantityText(shipment.value?.items)
})

const uppercaseAmount = computed(() => {
  return formatRmbUppercase(grandTotalAmount.value)
})

const printTimeStr = computed(() => {
  return formatPrintTime(shipment.value?.printTime)
})
</script>

<style scoped>
.shipment-doc-wrapper {
  width: 100%;
  max-width: 820px;
  min-height: 500px;
}

.header-qr {
  position: absolute;
  right: 0;
  top: 0;
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
  .shipment-doc-wrapper {
    width: 100%;
    margin: 0;
  }
}
</style>
