<template>
  <div class="quotation-print-wrapper print-paper text-gray-900 mx-auto">
    <!-- 1. 抬头与大标题 -->
    <div class="print-header text-center mb-16px">
      <div class="text-22px font-bold tracking-widest text-black mb-4px">产品报价单</div>
      <div class="text-11px text-gray-500 uppercase tracking-wider">QUOTATION SHEET</div>
    </div>

    <!-- 2. 单据元信息网格 -->
    <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
      <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
        <div>
          <span class="text-gray-600">报价单号：</span>
          <span class="font-mono font-bold">{{ quotation?.displayNo || quotation?.quotationNo || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">报价日期：</span>
          <span class="font-medium">{{ quotation?.quotationDate || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">有效期至：</span>
          <span class="font-medium">{{ quotation?.validTo || '长期有效' }}</span>
        </div>

        <div>
          <span class="text-gray-600">客户名称：</span>
          <span class="font-bold">{{ quotation?.clientName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">联系人员：</span>
          <span>{{ quotation?.clientContact || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">联系电话：</span>
          <span>{{ quotation?.phone || '—' }}</span>
        </div>

        <div>
          <span class="text-gray-600">报价币种：</span>
          <span class="font-bold">{{ quotation?.currency || 'RMB' }}</span>
        </div>
        <div>
          <span class="text-gray-600">业务代表：</span>
          <span>{{ quotation?.salespersonName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">有效期自：</span>
          <span>{{ quotation?.validFrom || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 产品明细表格 -->
    <table class="print-table w-full text-11px border-collapse mb-12px">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-32px text-center">#</th>
          <th class="w-85px text-left">产品编号</th>
          <th class="text-left">品名</th>
          <th class="w-75px text-left">客户色号</th>
          <th class="w-80px text-left">成分</th>
          <th class="w-60px text-center">门幅</th>
          <th class="w-60px text-center">克重</th>
          <th class="w-65px text-right">数量</th>
          <th class="w-40px text-center">单位</th>
          <th class="w-75px text-right">单价 ({{ quotation?.currency || '元' }})</th>
          <th class="w-85px text-right">金额 ({{ quotation?.currency || '元' }})</th>
          <th class="w-75px text-left">备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in quotation?.items || []" :key="item.id || idx">
          <td class="text-center">{{ item.lineNo || idx + 1 }}</td>
          <td class="font-mono">{{ item.productCode || '—' }}</td>
          <td class="font-medium">{{ item.productName || '—' }}</td>
          <td>{{ item.customerColor || '—' }}</td>
          <td>{{ item.composition || '—' }}</td>
          <td class="text-center">{{ item.width || '—' }}</td>
          <td class="text-center">{{ item.weight || '—' }}</td>
          <td class="text-right font-mono font-medium">{{ item.quantity != null ? item.quantity : '—' }}</td>
          <td class="text-center">{{ item.unit || 'KG' }}</td>
          <td class="text-right font-mono">{{ item.unitPrice != null ? Number(item.unitPrice).toFixed(4) : '—' }}</td>
          <td class="text-right font-mono font-bold">{{ item.amount != null ? Number(item.amount).toFixed(2) : '0.00' }}</td>
          <td>{{ item.remark || '' }}</td>
        </tr>
        <tr v-if="!quotation?.items?.length">
          <td colspan="12" class="text-center py-12px text-gray-400">暂无明细数据</td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="bg-gray-50 font-bold border-t-2 border-gray-800">
          <td colspan="7" class="text-center">合计</td>
          <td class="text-right font-mono">{{ totalQuantityText }}</td>
          <td></td>
          <td></td>
          <td class="text-right font-mono text-12px">{{ quotation?.currency || 'RMB' }} {{ totalAmountText }}</td>
          <td></td>
        </tr>
      </tfoot>
    </table>

    <!-- 4. 汇总与备注栏 -->
    <div class="summary-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50">
      <div class="flex justify-between items-center mb-6px">
        <div>
          <span class="text-gray-600">总数量：</span>
          <span class="font-bold mr-20px">{{ totalQuantityText }}</span>
          <span class="text-gray-600">总金额：</span>
          <span class="font-bold text-14px text-blue-800">{{ quotation?.currency || 'RMB' }} {{ totalAmountText }}</span>
        </div>
      </div>
      <div v-if="quotation?.remarks" class="text-11px text-gray-600 mt-4px">
        <span>备注说明：</span>{{ quotation.remarks }}
      </div>
    </div>

    <!-- 5. 签章区 -->
    <div class="sign-block mt-24px text-12px">
      <div class="grid grid-cols-2 gap-24px pt-10px">
        <div class="border border-gray-400 p-12px min-h-100px flex flex-col justify-between">
          <div class="font-bold text-13px">报价方（盖章/签字）：</div>
          <div class="mt-40px text-gray-600 flex justify-between">
            <span>经手人：{{ quotation?.salespersonName || '—' }}</span>
            <span>日期：_____年____月____日</span>
          </div>
        </div>
        <div class="border border-gray-400 p-12px min-h-100px flex flex-col justify-between">
          <div class="font-bold text-13px">客户确认（盖章/签字）：</div>
          <div class="mt-40px text-gray-600 flex justify-between">
            <span>确认人：___________</span>
            <span>日期：_____年____月____日</span>
          </div>
        </div>
      </div>
      <div class="flex justify-between items-center text-10px text-gray-500 mt-16px pt-6px border-t border-dashed border-gray-300">
        <div>注：本报价单需在有效期内确认生效，如有任何疑问请及时联系业务人员。</div>
        <div>打印时间：{{ printTimeStr }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import type { QuotationPrintVO } from '@/api/lunaerp/business/quotation'

defineOptions({ name: 'QuotationPrintTemplate' })

const props = defineProps({
  data: {
    type: Object as PropType<QuotationPrintVO>,
    default: null
  },
  list: {
    type: Array as PropType<QuotationPrintVO[]>,
    default: () => []
  }
})

const quotation = computed<QuotationPrintVO | null>(() => {
  if (props.data) return props.data
  if (props.list && props.list.length > 0) return props.list[0]
  return null
})

const totalQuantityText = computed(() => {
  if (quotation.value?.totalQuantity != null) {
    return Number(quotation.value.totalQuantity).toFixed(2)
  }
  const sum = (quotation.value?.items || []).reduce((acc, it) => acc + (Number(it.quantity) || 0), 0)
  return sum.toFixed(2)
})

const totalAmountText = computed(() => {
  if (quotation.value?.totalAmount != null) {
    return Number(quotation.value.totalAmount).toFixed(2)
  }
  const sum = (quotation.value?.items || []).reduce((acc, it) => acc + (Number(it.amount) || 0), 0)
  return sum.toFixed(2)
})

const printTimeStr = computed(() => {
  const d = new Date()
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
})
</script>

<style scoped>
.quotation-print-wrapper {
  max-width: 800px;
  padding: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 4px 6px;
}

.print-table th {
  background-color: #f3f4f6 !important;
  font-weight: 600;
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}

.meta-box,
.summary-box {
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}
</style>
