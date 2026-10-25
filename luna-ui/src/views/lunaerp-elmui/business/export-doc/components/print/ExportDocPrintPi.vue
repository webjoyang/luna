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
      PROFORMA INVOICE
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
        <div v-if="exportDoc?.buyerBankInfo" class="text-10px text-gray-600 mt-4px">
          Bank: {{ exportDoc.buyerBankInfo }}
        </div>
      </div>

      <!-- 单据信息框 -->
      <div class="meta-box w-40% border border-black p-6px flex flex-col justify-between">
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">P/I No.:</span>
          <span class="font-mono font-bold">{{ piMeta.piNo || exportDoc?.docNo || '—' }}</span>
        </div>
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">Date:</span>
          <span>{{ formatDate(piMeta.date || exportDoc?.createTime) }}</span>
        </div>
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">Valid Until:</span>
          <span>{{ formatDate(piMeta.validUntil) || 'As specified' }}</span>
        </div>
        <div class="meta-row flex justify-between">
          <span class="meta-label font-bold">Contract No.:</span>
          <span class="font-mono">{{ exportDoc?.contractNo || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 4. 条款框 -->
    <div class="terms-box border border-black p-6px mb-8px text-10.5px flex flex-wrap gap-x-16px gap-y-4px">
      <div>
        <span class="font-bold">Price Term:</span>
        <span class="ml-4px">{{ exportDoc?.incoterm || '—' }}</span>
      </div>
      <div>
        <span class="font-bold">Loading Port:</span>
        <span class="ml-4px">{{ exportDoc?.portOfLoading || '—' }}</span>
      </div>
      <div>
        <span class="font-bold">Destination:</span>
        <span class="ml-4px">{{ exportDoc?.portOfDischarge || exportDoc?.countryOfDest || '—' }}</span>
      </div>
      <div>
        <span class="font-bold">Shipment Date:</span>
        <span class="ml-4px">{{ formatDate(piMeta.shipmentDate) || 'Prompt / As agreed' }}</span>
      </div>
    </div>

    <!-- 5. 货物表格 -->
    <ExportDocGoodsTable
      mode="PI"
      :items="exportDoc?.items || []"
      :currency-symbol="currencySymbol"
      :total-quantity="exportDoc?.totalQuantity"
      :total-amount="exportDoc?.totalAmount"
    />

    <!-- 6. 付款条款与银行信息 -->
    <div class="section-box border border-black p-6px mb-6px text-10.5px">
      <div class="mb-4px">
        <span class="font-bold uppercase">Payment:</span>
        <span class="ml-4px font-medium">
          {{ exportDoc?.paymentTerm || 'T/T' }}
          <template v-if="piMeta.depositPct"> ({{ piMeta.depositPct }}% deposit by T/T, balance before shipment)</template>
        </span>
      </div>
      <div>
        <div class="font-bold uppercase mb-2px">Beneficiary Bank Details:</div>
        <div class="grid grid-cols-2 gap-x-12px gap-y-2px text-10px">
          <div><span class="text-gray-600">Bank Name:</span> {{ exportDoc?.sellerBankName || '—' }}</div>
          <div><span class="text-gray-600">Account No. (A/C):</span> <span class="font-mono font-bold">{{ exportDoc?.sellerBankAccount || '—' }}</span></div>
          <div><span class="text-gray-600">SWIFT Code:</span> <span class="font-mono">{{ exportDoc?.sellerBankSwift || '—' }}</span></div>
          <div><span class="text-gray-600">Bank Address:</span> {{ exportDoc?.sellerBankAddress || '—' }}</div>
        </div>
      </div>
    </div>

    <!-- 7. 运输条款与其他信息 -->
    <div class="section-box border border-black p-6px mb-6px text-10px flex justify-between">
      <div>
        <span class="font-bold">Partial Shipment:</span>
        <span class="ml-4px">{{ piMeta.partialShipment || 'Allowed' }}</span>
      </div>
      <div>
        <span class="font-bold">Transshipment:</span>
        <span class="ml-4px">{{ piMeta.transshipment || 'Allowed' }}</span>
      </div>
      <div>
        <span class="font-bold">Commission:</span>
        <span class="ml-4px">{{ piMeta.commissionPct ? `${piMeta.commissionPct}%` : 'None' }}</span>
      </div>
    </div>

    <!-- 8. 备注信息 -->
    <div v-if="exportDoc?.remarks || piMeta.notes" class="section-box border border-black p-6px mb-6px text-10px">
      <span class="font-bold">Remarks:</span>
      <span class="ml-4px">{{ exportDoc?.remarks || piMeta.notes }}</span>
    </div>

    <!-- 9. 底部签章区与验真二维码 -->
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
import { getCurrencySymbol, safeParseJson, formatDate } from './utils'

defineOptions({ name: 'ExportDocPrintPi' })

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

const piMeta = computed(() => {
  return safeParseJson<{
    piNo?: string
    date?: string
    validUntil?: string
    shipmentDate?: string
    depositPct?: number | string
    commissionPct?: number | string
    partialShipment?: string
    transshipment?: string
    notes?: string
  }>(exportDoc.value?.piMeta)
})

const currencySymbol = computed(() => {
  return getCurrencySymbol(exportDoc.value?.currency)
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
