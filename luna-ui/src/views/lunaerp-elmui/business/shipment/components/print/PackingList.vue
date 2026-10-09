<template>
  <div class="packing-list-wrapper print-paper text-gray-900 mx-auto">
    <!-- 1. 装箱清单抬头与右上角签收二维码 -->
    <div class="print-header text-center mb-16px relative">
      <div class="text-22px font-bold tracking-widest text-black mb-4px">发货装箱清单</div>
      <div class="text-11px text-gray-500 uppercase tracking-wider">细码清单 / PACKING LIST</div>

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

    <!-- 2. 装箱清单元数据 -->
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
          <span class="text-gray-600">承运商/单号：</span>
          <span>{{ shipment?.carrier || '—' }} {{ shipment?.trackingNumber ? `(${shipment.trackingNumber})` : '' }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 逐卷细码表格 -->
    <table class="print-table w-full text-11px border-collapse mb-12px">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-40px text-center">#</th>
          <th class="w-110px text-left">卷号</th>
          <th class="text-left">所属产品</th>
          <th class="w-80px text-left">客户色号</th>
          <th class="w-80px text-left">缸号/批次</th>
          <th class="w-85px text-right">重量 (KG)</th>
          <th class="w-85px text-right">米数 (M)</th>
          <th class="w-110px text-left">备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(roll, idx) in flatRollList" :key="idx">
          <td class="text-center">{{ idx + 1 }}</td>
          <td class="font-mono font-bold">{{ roll.rollNo }}</td>
          <td>{{ roll.productName }} <span v-if="roll.productCode" class="text-gray-400">({{ roll.productCode }})</span></td>
          <td>{{ roll.customerColorNo || '—' }}</td>
          <td class="font-mono">{{ roll.vatNo || '—' }}</td>
          <td class="text-right font-mono font-medium">{{ roll.weight != null ? Number(roll.weight).toFixed(2) : '—' }}</td>
          <td class="text-right font-mono">{{ roll.lengthM != null ? Number(roll.lengthM).toFixed(2) : '—' }}</td>
          <td>{{ roll.remark || '' }}</td>
        </tr>
        <tr v-if="!flatRollList.length">
          <td colspan="8" class="text-center py-16px text-gray-400">
            发货单明细未录入细卷清单数据
          </td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
          <td colspan="5" class="text-center">合计</td>
          <td class="text-right font-mono">{{ totalRollsWeight.toFixed(2) }}</td>
          <td class="text-right font-mono">{{ totalRollsLength.toFixed(2) }}</td>
          <td>共 {{ flatRollList.length }} 卷</td>
        </tr>
      </tfoot>
    </table>

    <!-- 4. 汇总信息 -->
    <div class="summary-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50 flex justify-between items-center">
      <div>
        <span class="text-gray-600">装箱总卷数：</span>
        <span class="font-bold mr-20px">{{ flatRollList.length }} 卷</span>
        <span class="text-gray-600">累计总重量：</span>
        <span class="font-bold mr-20px">{{ totalRollsWeight.toFixed(2) }} KG</span>
        <span class="text-gray-600">累计总米数：</span>
        <span class="font-bold">{{ totalRollsLength.toFixed(2) }} M</span>
      </div>
      <div class="text-gray-500 text-11px">
        细码数据均取自出库卷清单登记
      </div>
    </div>

    <!-- 5. 签字留痕栏 -->
    <div class="sign-block mt-24px text-12px">
      <div class="grid grid-cols-4 gap-12px pt-10px">
        <div>
          <span class="text-gray-600">装箱/打包：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
        <div>
          <span class="text-gray-600">复核人：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
        <div>
          <span class="text-gray-600">仓库发货：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
        <div>
          <span class="text-gray-600">客户验收入库：</span>
          <span class="inline-block w-100px border-b border-gray-700"></span>
        </div>
      </div>
      <div class="flex justify-between items-center text-10px text-gray-500 mt-20px pt-6px border-t border-dashed border-gray-300">
        <div>验收日期：_____年____月____日</div>
        <div>请当面验视包裹外观及卷数，确认无误后签字归档</div>
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
  flattenRollList,
  formatPrintTime
} from './utils'
import type { FlatRoll } from './utils'

defineOptions({ name: 'PackingList' })

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

const flatRollList = computed<FlatRoll[]>(() => {
  return flattenRollList(shipment.value?.items)
})

const totalRollsWeight = computed(() => {
  return flatRollList.value.reduce((acc, cur) => acc + (cur.weight || 0), 0)
})

const totalRollsLength = computed(() => {
  return flatRollList.value.reduce((acc, cur) => acc + (cur.lengthM || 0), 0)
})

const printTimeStr = computed(() => {
  return formatPrintTime(shipment.value?.printTime)
})
</script>

<style scoped>
.packing-list-wrapper {
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
  .packing-list-wrapper {
    width: 100%;
    margin: 0;
  }
}
</style>
