<template>
  <div class="export-doc-print-wrapper print-paper text-gray-900 mx-auto">
    <!-- 1. 卖方抬头 -->
    <div class="seller-header text-center mb-6px">
      <div class="seller-name text-18px font-bold uppercase tracking-wide">
        {{ exportDoc?.sellerName || 'EXPORT SELLER' }}
      </div>
      <div v-if="exportDoc?.sellerAddress" class="seller-address text-10px text-gray-700 mt-2px">
        {{ exportDoc?.sellerAddress }}
      </div>
    </div>

    <!-- 2. 单据大标题 -->
    <div class="doc-title text-center text-16px font-bold uppercase tracking-widest py-4px my-6px border-t-2 border-b-2 border-black">
      PACKING LIST
    </div>

    <!-- 3. 买方与单据元信息网格 -->
    <div class="header-grid flex justify-between mb-8px text-11px">
      <!-- 买方框 -->
      <div class="buyer-box w-58% border border-black p-6px">
        <div class="font-bold text-gray-700 mb-2px">Messrs / To:</div>
        <div class="font-bold text-12px">{{ exportDoc?.buyerName || '—' }}</div>
        <div class="text-10.5px text-gray-800 whitespace-pre-line mt-2px">
          {{ exportDoc?.buyerAddress || '—' }}
        </div>
      </div>

      <!-- 单据信息框 -->
      <div class="meta-box w-40% border border-black p-6px flex flex-col justify-between">
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">P/L No.:</span>
          <span class="font-mono font-bold">{{ plMeta.plNo || exportDoc?.docNo || '—' }}</span>
        </div>
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">Date:</span>
          <span>{{ formatDate(plMeta.plDate || exportDoc?.createTime) }}</span>
        </div>
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">Contract No.:</span>
          <span class="font-mono">{{ exportDoc?.contractNo || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 4. 唛头与港口框 -->
    <div class="terms-box border border-black p-6px mb-8px text-10.5px flex justify-between">
      <div class="w-55%">
        <span class="font-bold">Shipping Marks:</span>
        <div class="font-mono whitespace-pre-line text-10px mt-2px">
          {{ exportDoc?.shippingMark || 'N/M' }}
        </div>
      </div>
      <div class="w-40% flex flex-col justify-between">
        <div>
          <span class="font-bold">Port of Loading:</span>
          <span class="ml-4px">{{ exportDoc?.portOfLoading || '—' }}</span>
        </div>
        <div class="mt-4px">
          <span class="font-bold">Port of Discharge:</span>
          <span class="ml-4px">{{ exportDoc?.portOfDischarge || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 5. 货物表格（含 TOTAL 行：手填优先、空则求和） -->
    <ExportDocGoodsTable
      mode="PL"
      :items="exportDoc?.items || []"
      :total-packages="exportDoc?.totalPackages"
      :total-quantity="exportDoc?.totalQuantity"
      :total-net-weight="exportDoc?.totalNetWeight"
      :total-gross-weight="exportDoc?.totalGrossWeight"
      :total-volume="exportDoc?.totalVolume"
    />

    <!-- 6. 备注信息 -->
    <div v-if="exportDoc?.remarks || plMeta.remark" class="section-box border border-black p-6px mb-6px text-10px">
      <span class="font-bold">Remarks:</span>
      <span class="ml-4px">{{ exportDoc?.remarks || plMeta.remark }}</span>
    </div>

    <!-- 7. 底部签章区与验真二维码 -->
    <div class="footer-sign-qr flex justify-between items-end mt-12px pt-6px">
      <!-- 签章区 -->
      <div class="sign-area w-60%">
        <div class="sign-title text-11px font-bold">
          For and on behalf of
        </div>
        <div class="seller-sign-company text-12px font-bold uppercase mt-2px">
          {{ exportDoc?.sellerName || 'SELLER' }}
        </div>
        <div class="sign-box relative h-55px border-b border-black my-4px">
          <span class="sign-hint absolute bottom-2px left-4px text-9px text-gray-400">
            Authorized Signature & Stamp
          </span>
        </div>
        <div class="sign-date text-10px mt-4px">
          Date: ________________________
        </div>
      </div>

      <!-- 右下角二维码 -->
      <div class="qr-area w-35% flex justify-end">
        <QrSlot
          :text="exportDoc?.qrUrl || ''"
          :width="72"
          :margin="1"
        >
          <template #label>
            <span class="text-9px text-gray-600 block scale-90 whitespace-nowrap">
              {{ exportDoc?.qrUrl ? '验真二维码' : '门户地址未配置' }}
            </span>
          </template>
        </QrSlot>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, type PropType } from 'vue'
import type { ExportDocPrintVO } from '@/api/lunaerp-elmui/business/export-doc'
import QrSlot from '@/components/Print/src/QrSlot.vue'
import ExportDocGoodsTable from './ExportDocGoodsTable.vue'
import { safeParseJson, formatDate } from './utils'

defineOptions({ name: 'ExportDocPrintPl' })

const props = defineProps({
  data: {
    type: Object as PropType<ExportDocPrintVO | null>,
    default: null
  },
  list: {
    type: Array as PropType<ExportDocPrintVO[]>,
    default: () => []
  }
})

const exportDoc = computed<ExportDocPrintVO | null>(() => {
  if (props.data) return props.data
  if (props.list && props.list.length > 0) return props.list[0]
  return null
})

const plMeta = computed(() => {
  return safeParseJson<{
    plNo?: string
    plDate?: string
    remark?: string
  }>(exportDoc.value?.plMeta)
})
</script>

<style scoped>
.export-doc-print-wrapper {
  width: 760px;
  max-width: 760px;
  min-height: 600px;
  font-family: Arial, "Helvetica Neue", Helvetica, sans-serif;
}
.border-b-2 { border-bottom-width: 2px; }
.border-t-2 { border-top-width: 2px; }
@media print {
  @page {
    size: portrait;
    margin: 10mm;
  }
  .export-doc-print-wrapper {
    width: 100%;
    margin: 0;
  }
}
</style>
