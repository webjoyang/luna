<template>
  <div class="customer-statement-sheet-wrapper print-paper text-gray-900 mx-auto">
    <div
      v-for="(sheet, sIdx) in sheetList"
      :key="sheet.id || sIdx"
      class="sheet-page"
      :class="{ 'page-break-after': sIdx < sheetList.length - 1 }"
    >
      <!-- 1. 单据抬头与右上角对账二维码 -->
      <div class="print-header text-center mb-12px relative">
        <div class="main-title text-22px font-bold tracking-widest text-black mb-4px">
          {{ sheet.title || '客户对账单' }}
        </div>
        <div class="sub-title text-11px text-gray-500 uppercase tracking-wider">
          CUSTOMER STATEMENT OF ACCOUNT
        </div>

        <!-- 右上角客户门户对账二维码（未配置基址时优雅降级提示，不阻断打印） -->
        <div class="header-qr absolute right-0 top-0">
          <QrSlot :text="sheet.qrUrl || ''" :width="64" :margin="1">
            <template #label>
              <span class="text-9px text-gray-600 block scale-90 whitespace-nowrap">
                {{ sheet.qrUrl ? '扫码对账' : '门户未配' }}
              </span>
            </template>
          </QrSlot>
        </div>
      </div>

      <!-- 2. 核心元数据（单号/区间/客户四要素/状态/确认信息） -->
      <div class="meta-box mb-12px text-11px border border-gray-800 p-8px bg-gray-50">
        <div class="meta-grid grid grid-cols-3 gap-y-6px gap-x-12px">
          <div class="meta-item">
            <span class="label text-gray-600">对账单号：</span>
            <span class="value font-mono font-bold">{{ sheet.statementNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">对账区间：</span>
            <span class="value font-medium">
              {{ sheet.periodStart || '—' }} 至 {{ sheet.periodEnd || '—' }}
            </span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">单据状态：</span>
            <span class="value font-semibold">
              {{ formatStatusText(sheet.status) }}
            </span>
          </div>

          <div class="meta-item span-2">
            <span class="label text-gray-600">客户名称：</span>
            <span class="value font-bold">{{ sheet.customerName || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">对账币种：</span>
            <span class="value font-mono font-medium">{{ sheet.currency || 'CNY' }}</span>
          </div>

          <div class="meta-item span-2">
            <span class="label text-gray-600">纳税人识别号：</span>
            <span class="value font-mono">{{ sheet.taxNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">联系电话：</span>
            <span class="value">{{ sheet.contactPhone || '—' }}</span>
          </div>

          <div class="meta-item span-3">
            <span class="label text-gray-600">开票/公司地址：</span>
            <span class="value">{{ sheet.address || '—' }}</span>
          </div>

          <div class="meta-item span-3">
            <span class="label text-gray-600">开户银行与账号：</span>
            <span class="value font-mono">
              {{ formatBankInfo(sheet.bankName, sheet.bankAccountNo) }}
            </span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">制单人：</span>
            <span class="value">{{ sheet.creator || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">确认人/方式：</span>
            <span class="value">
              {{ formatConfirmInfo(sheet) }}
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

      <!-- 3. 全量行明细表格 -->
      <table class="print-table w-full text-10px border-collapse mb-12px">
        <thead>
          <tr class="bg-gray-100 font-bold">
            <th class="w-32px text-center">#</th>
            <th class="w-70px text-center">业务日期</th>
            <th class="w-55px text-center">方向</th>
            <th class="w-65px text-center">类别</th>
            <th class="w-130px text-left">来源单号</th>
            <th class="w-160px text-left">摘要 / 品名说明</th>
            <th class="w-75px text-right">应收借方(元)</th>
            <th class="w-75px text-right">收款贷方(元)</th>
            <th class="w-55px text-center">签收标记</th>
            <th class="text-left">行备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, idx) in sheet.items || []" :key="item.lineNo || idx">
            <td class="text-center">{{ item.lineNo || idx + 1 }}</td>
            <td class="text-center font-mono">{{ item.bizDate || '—' }}</td>
            <td class="text-center font-medium">
              <span :class="item.direction === 'DEBIT' ? 'text-blue-700' : 'text-emerald-700'">
                {{ item.direction === 'DEBIT' ? '借(应收)' : '贷(核销)' }}
              </span>
            </td>
            <td class="text-center">{{ formatRefTypeLabel(item.refType) }}</td>
            <td class="text-left font-mono">{{ item.refNo || '—' }}</td>
            <td class="text-left">{{ item.summary || '—' }}</td>
            <td class="text-right font-mono font-medium">
              {{ Number(item.debitAmount || 0) > 0 ? formatMoney(item.debitAmount) : '—' }}
            </td>
            <td class="text-right font-mono font-medium">
              {{ Number(item.creditAmount || 0) > 0 ? formatMoney(item.creditAmount) : '—' }}
            </td>
            <td class="text-center">{{ formatSignStatus(item.signStatus) }}</td>
            <td class="text-left text-9px text-gray-600">{{ item.remark || '—' }}</td>
          </tr>

          <tr v-if="!sheet.items || sheet.items.length === 0">
            <td colspan="10" class="text-center py-16px text-gray-400">无可对账明细</td>
          </tr>
        </tbody>
      </table>

      <!-- 4. 金额汇总栏（期初 + 本期借贷 + 期末余额） -->
      <div
        class="amount-summary-box mb-16px border border-gray-800 p-8px bg-gray-50 flex justify-between items-center text-11px"
      >
        <div class="amount-subtotals flex gap-16px text-gray-700">
          <div>
            期初欠款余额：
            <span class="font-mono font-bold text-black">
              ¥ {{ formatMoney(sheet.openingBalance) }}
            </span>
          </div>
          <div>
            本期应收发生额(借)：
            <span class="font-mono font-bold text-blue-800">
              ¥ {{ formatMoney(sheet.periodDebit) }}
            </span>
          </div>
          <div>
            本期核销/冲减额(贷)：
            <span class="font-mono font-bold text-emerald-800">
              ¥ {{ formatMoney(sheet.periodCredit) }}
            </span>
          </div>
        </div>
        <div class="amount-grand-total text-14px font-bold text-black">
          期末应收余额：
          <span class="text-16px font-mono text-indigo-900">
            ¥ {{ formatMoney(sheet.closingBalance) }}
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
            甲方（本单位财务经办盖章）：
          </div>
          <div class="sign-lines text-11px flex flex-col gap-6px">
            <div class="sign-row flex justify-between">
              <span>经办人：___________________</span>
              <span>复核人：___________________</span>
            </div>
            <div class="sign-row flex justify-between">
              <span>盖章日期：______年____月____日</span>
            </div>
          </div>
        </div>

        <div
          class="sign-col border border-gray-600 p-12px min-h-100px flex flex-col justify-between"
        >
          <div
            class="sign-title font-bold text-12px border-b border-dashed border-gray-300 pb-4px mb-8px"
          >
            乙方（客户单位确认签字盖章）：
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
import type { CustomerStatementPrintRespVO } from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { formatMoney } from '../../model/customerStatement'
import QrSlot from '@/components/Print/src/QrSlot.vue'

defineOptions({ name: 'CustomerStatementSheet' })

const props = defineProps<{
  data?: CustomerStatementPrintRespVO
  list?: CustomerStatementPrintRespVO[]
}>()

const sheetList = computed<CustomerStatementPrintRespVO[]>(() => {
  if (props.list && props.list.length > 0) {
    return props.list
  }
  return props.data ? [props.data] : []
})

const formatStatusText = (status?: string) => {
  switch (status) {
    case 'CONFIRMED':
      return '已确认'
    case 'SENT':
      return '已发出'
    case 'REJECTED':
      return '已驳回'
    case 'DRAFT':
      return '草稿'
    case 'VOID':
      return '已作废'
    default:
      return status || '—'
  }
}

const formatBankInfo = (bankName?: string, bankAccountNo?: string) => {
  if (!bankName && !bankAccountNo) return '—'
  return [bankName, bankAccountNo].filter(Boolean).join(' ')
}

const formatConfirmInfo = (sheet: CustomerStatementPrintRespVO) => {
  if (!sheet.confirmByName && !sheet.confirmTime) {
    return '未确认'
  }
  const typeText = sheet.confirmType === 'ON_BEHALF' ? '线下代办' : '门户在线'
  const methodText = sheet.confirmMethod ? ` (${sheet.confirmMethod})` : ''
  const nameText = sheet.confirmByName || ''
  const timeText = sheet.confirmTime ? ` / ${sheet.confirmTime}` : ''
  return `${nameText} [${typeText}${methodText}]${timeText}`
}

const formatRefTypeLabel = (refType?: string) => {
  switch (refType) {
    case 'SHIPMENT_ITEM':
      return '发货明细'
    case 'SHIPMENT_FREIGHT':
      return '发货运费'
    case 'RECEIPT':
      return '收款核销'
    case 'ADVANCE_WRITE_OFF':
      return '预收核销'
    case 'AR_RETURN_REVERSAL':
      return '退货冲减'
    default:
      return refType || '—'
  }
}

const formatSignStatus = (signStatus?: string) => {
  switch (signStatus) {
    case 'SIGNED':
      return '已签收'
    case 'UNSIGNED':
    case 'UN_SIGN':
      return '未签收'
    case 'REJECTED':
      return '拒签'
    default:
      return signStatus || '—'
  }
}

const formatPrintTime = (time?: string) => {
  if (!time) {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  }
  return time
}
</script>
