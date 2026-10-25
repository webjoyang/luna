<template>
  <table class="print-table w-full text-10px border-collapse mb-10px">
    <!-- PI / CI 价格货物表 -->
    <template v-if="mode === 'PI' || mode === 'CI'">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-28px text-center">#</th>
          <th class="text-left">Description</th>
          <th class="w-80px text-center">HS Code</th>
          <th class="w-65px text-right">Quantity</th>
          <th class="w-45px text-center">Unit</th>
          <th class="w-75px text-right">Unit Price</th>
          <th class="w-85px text-right">Amount</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in items" :key="item.id || idx">
          <td class="text-center">{{ idx + 1 }}</td>
          <td class="text-left font-medium">
            <div>{{ item.nameEn || item.nameCn || '—' }}</div>
            <div v-if="item.spec" class="text-9px text-gray-600">{{ item.spec }}</div>
          </td>
          <td class="text-center font-mono">{{ item.hsCode || '—' }}</td>
          <td class="text-right font-mono">{{ formatAmount(item.quantity) }}</td>
          <td class="text-center">{{ item.unit || '—' }}</td>
          <td class="text-right font-mono">{{ currencySymbol }}{{ formatAmount(item.unitPrice) }}</td>
          <td class="text-right font-mono font-medium">{{ currencySymbol }}{{ formatAmount(item.amount) }}</td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="total-row font-bold bg-gray-50">
          <td colspan="3" class="text-center uppercase">TOTAL</td>
          <td class="text-right font-mono">{{ formatAmount(totalQuantity) }}</td>
          <td></td>
          <td></td>
          <td class="text-right font-mono font-bold">{{ currencySymbol }}{{ formatAmount(totalAmount) }}</td>
        </tr>
      </tfoot>
    </template>

    <!-- PL 装箱清单货物表 -->
    <template v-else-if="mode === 'PL'">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-28px text-center">#</th>
          <th class="text-left">Description</th>
          <th class="w-55px text-right">Packages</th>
          <th class="w-65px text-right">Quantity</th>
          <th class="w-45px text-center">Unit</th>
          <th class="w-65px text-right">N.W.(KG)</th>
          <th class="w-65px text-right">G.W.(KG)</th>
          <th class="w-60px text-right">CBM</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in items" :key="item.id || idx">
          <td class="text-center">{{ idx + 1 }}</td>
          <td class="text-left font-medium">
            <div>{{ item.nameEn || item.nameCn || '—' }}</div>
            <div v-if="item.spec" class="text-9px text-gray-600">{{ item.spec }}</div>
          </td>
          <td class="text-right font-mono">{{ item.packages != null ? item.packages : '—' }}</td>
          <td class="text-right font-mono">{{ formatAmount(item.quantity) }}</td>
          <td class="text-center">{{ item.unit || '—' }}</td>
          <td class="text-right font-mono">{{ item.netWeight != null ? formatAmount(item.netWeight) : '—' }}</td>
          <td class="text-right font-mono">{{ item.grossWeight != null ? formatAmount(item.grossWeight) : '—' }}</td>
          <td class="text-right font-mono">{{ item.volume != null ? formatAmount(item.volume, 3) : '—' }}</td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="total-row font-bold bg-gray-50">
          <td colspan="2" class="text-center uppercase">TOTAL</td>
          <td class="text-right font-mono">{{ totalPackages != null ? totalPackages : '—' }}</td>
          <td class="text-right font-mono">{{ formatAmount(totalQuantity) }}</td>
          <td></td>
          <td class="text-right font-mono">{{ totalNetWeight != null ? formatAmount(totalNetWeight) : '—' }}</td>
          <td class="text-right font-mono">{{ totalGrossWeight != null ? formatAmount(totalGrossWeight) : '—' }}</td>
          <td class="text-right font-mono">{{ totalVolume != null ? formatAmount(totalVolume, 3) : '—' }}</td>
        </tr>
      </tfoot>
    </template>
  </table>
</template>

<script lang="ts" setup>
import type { PropType } from 'vue'
import type { ExportDocItemVO } from '@/api/lunaerp-elmui/business/export-doc'
import { formatAmount } from './utils'

defineOptions({ name: 'ExportDocGoodsTable' })

defineProps({
  mode: {
    type: String as PropType<'PI' | 'CI' | 'PL'>,
    default: 'PI'
  },
  items: {
    type: Array as PropType<ExportDocItemVO[]>,
    default: () => []
  },
  currencySymbol: {
    type: String,
    default: '$'
  },
  totalQuantity: {
    type: [Number, String],
    default: 0
  },
  totalAmount: {
    type: [Number, String],
    default: 0
  },
  totalPackages: {
    type: [Number, String],
    default: null
  },
  totalNetWeight: {
    type: [Number, String],
    default: null
  },
  totalGrossWeight: {
    type: [Number, String],
    default: null
  },
  totalVolume: {
    type: [Number, String],
    default: null
  }
})
</script>

<style scoped>
.print-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 10.5px;
  margin-bottom: 10px;
}
.print-table th, .print-table td {
  border: 1px solid #000;
  padding: 4px 6px;
  line-height: 1.3;
}
.print-table th {
  background-color: #f2f2f2;
}
.total-row td {
  font-weight: bold;
  background-color: #fafafa;
}
.text-center { text-align: center; }
.text-left { text-align: left; }
.text-right { text-align: right; }
.font-mono { font-family: monospace, Courier, sans-serif; }
.font-bold { font-weight: bold; }
</style>
