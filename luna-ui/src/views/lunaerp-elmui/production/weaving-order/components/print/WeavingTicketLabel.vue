<template>
  <div class="weaving-ticket-labels-wrapper">
    <div
      v-for="(item, index) in tickets"
      :key="item.id || item.code || index"
      class="ticket-label-card"
    >
      <!-- 标签抬头 -->
      <div class="label-header flex items-center justify-between border-b border-black pb-1 mb-1">
        <span class="label-title font-bold text-13px tracking-wider">织造布票标签</span>
        <span class="font-mono text-11px font-bold text-gray-800">
          {{ item.code }}
        </span>
      </div>

      <!-- 核心内容区：左侧二维码 + 右侧明细网格 -->
      <div class="label-body flex gap-2 items-stretch">
        <!-- 左侧二维码 -->
        <div class="qr-col flex flex-col items-center justify-center p-1 border border-black rounded-sm bg-white">
          <QrSlot
            :text="item.qrUrl"
            :width="72"
            :margin="1"
          />
          <div class="qr-hint text-9px text-gray-600 mt-1 scale-90 whitespace-nowrap">
            扫码进报工
          </div>
        </div>

        <!-- 右侧信息表格/网格 -->
        <div class="info-col flex-1 flex flex-col justify-between text-11px">
          <table class="label-table w-full">
            <tbody>
              <tr>
                <td class="lbl text-gray-700">织造单号：</td>
                <td class="val font-mono font-bold">{{ item.weavingOrderCode || '-' }}</td>
              </tr>
              <tr>
                <td class="lbl text-gray-700">机台任务：</td>
                <td class="val font-mono">{{ item.machineTaskCode || '-' }}</td>
              </tr>
              <tr>
                <td class="lbl text-gray-700">机台 / 工人：</td>
                <td class="val">
                  <span class="font-bold">{{ item.loomNo || '-' }}</span>
                  <span v-if="item.workerName" class="ml-1 text-gray-600">({{ item.workerName }})</span>
                </td>
              </tr>
              <tr>
                <td class="lbl text-gray-700">品名：</td>
                <td class="val truncate max-w-160px" :title="item.productName">{{ item.productName || '-' }}</td>
              </tr>
              <tr>
                <td class="lbl text-gray-700">颜色 / 批号：</td>
                <td class="val">
                  <span>{{ item.color || '-' }}</span>
                  <span v-if="item.materialBatchNo" class="ml-1 text-gray-500 font-mono text-10px">[{{ item.materialBatchNo }}]</span>
                </td>
              </tr>
              <tr>
                <td class="lbl text-gray-700">匹序 / 数量：</td>
                <td class="val">
                  <span class="font-bold text-blue-800">第 {{ item.pieceIndex ?? '-' }} 匹</span>
                  <span class="mx-1 text-gray-400">|</span>
                  <span class="font-bold text-green-700">{{ item.quantity ?? '-' }} {{ item.unit || 'KG' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import QrSlot from '@/components/Print/src/QrSlot.vue'
import type { WeavingTicketPrintVO } from '@/api/lunaerp-elmui/production/weaving-ticket'

defineOptions({ name: 'WeavingTicketLabel' })

const props = defineProps({
  /** 单张数据 */
  data: {
    type: Object as PropType<WeavingTicketPrintVO | null>,
    default: null
  },
  /** 批量数据 */
  list: {
    type: Array as PropType<WeavingTicketPrintVO[]>,
    default: () => []
  }
})

const tickets = computed<WeavingTicketPrintVO[]>(() => {
  if (Array.isArray(props.list) && props.list.length > 0) {
    return props.list
  }
  if (props.data) {
    return [props.data]
  }
  return []
})
</script>

<style scoped>
.weaving-ticket-labels-wrapper {
  width: 100%;
}

.ticket-label-card {
  width: 100mm;
  height: 60mm;
  padding: 3mm 4mm;
  box-sizing: border-box;
  background: #fff;
  color: #000;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  page-break-after: always;
  break-after: page;
}

.ticket-label-card:last-child {
  page-break-after: auto;
  break-after: auto;
}

.label-title {
  font-size: 13px;
  line-height: 1.2;
}

.qr-col {
  width: 76px;
  flex-shrink: 0;
}

.label-table {
  border-collapse: collapse;
  line-height: 1.35;
}

.label-table td {
  padding: 1px 0;
  vertical-align: middle;
}

.label-table td.lbl {
  width: 68px;
  white-space: nowrap;
  font-size: 10.5px;
}

.label-table td.val {
  font-size: 11px;
}

/* 打印样式注入 */
@media print {
  @page {
    size: 100mm 60mm;
    margin: 0;
  }
  .ticket-label-card {
    border: none;
    page-break-after: always;
    break-after: page;
  }
  .ticket-label-card:last-child {
    page-break-after: auto;
    break-after: auto;
  }
}
</style>
