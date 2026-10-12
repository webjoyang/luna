<template>
  <div class="customer-statement-print-wrapper print-paper text-gray-900 mx-auto">
    <!-- 1. 对账单抬头与二维码 -->
    <div class="print-header text-center relative mb-16px">
      <div class="text-22px font-bold tracking-widest text-black mb-4px">
        客户对账单
      </div>
      <div class="text-11px text-gray-500 uppercase tracking-wider">
        CUSTOMER RECONCILIATION STATEMENT
      </div>

      <!-- 右上角客户对账二维码（未配置基址时展示降级提示） -->
      <div class="header-qr absolute right-0 top-0">
        <QrSlot
          :text="statement?.qrUrl || ''"
          :width="64"
          :margin="1"
        >
          <template #label>
            <span class="text-9px text-gray-600 block scale-90 whitespace-nowrap">
              {{ statement?.qrUrl ? '扫码对账' : (statement?.qrDegraded ? '门户未配' : '') }}
            </span>
          </template>
        </QrSlot>
      </div>
    </div>

    <!-- 2. 对账单核心元数据 -->
    <div class="meta-box mb-10px text-12px border border-gray-800 p-8px bg-gray-50">
      <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
        <div>
          <span class="text-gray-600">对账单号：</span>
          <span class="font-mono font-bold">{{ statement?.statementNo || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">客户名称：</span>
          <span class="font-bold">{{ statement?.customerName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">对账币种：</span>
          <span class="font-bold font-mono">{{ statement?.currency || 'CNY' }}</span>
        </div>

        <div>
          <span class="text-gray-600">对账区间：</span>
          <span class="font-mono font-medium">
            {{ statement?.periodStart && statement?.periodEnd ? `${statement.periodStart} ~ ${statement.periodEnd}` : '—' }}
          </span>
        </div>
        <div>
          <span class="text-gray-600">单据状态：</span>
          <span class="font-bold" :class="statusClass">{{ statusText }}</span>
        </div>
        <div>
          <span class="text-gray-600">制单时间：</span>
          <span>{{ statement?.createTime ? formatTimeStr(statement.createTime) : '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 客户四要素及账户信息 -->
    <div class="customer-info-box mb-10px text-11px border border-gray-400 p-8px bg-white">
      <div class="grid grid-cols-2 gap-y-4px gap-x-12px">
        <div>
          <span class="text-gray-600">开票抬头：</span>
          <span class="font-medium">{{ statement?.invoiceTitle || statement?.customerName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">联系电话：</span>
          <span>{{ statement?.contactPhone || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">开票地址：</span>
          <span>{{ statement?.invoiceAddress || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">开户银行：</span>
          <span>{{ statement?.bankName || '—' }}</span>
        </div>
        <div class="col-span-2">
          <span class="text-gray-600">银行账号：</span>
          <span class="font-mono">{{ statement?.bankAccountNo || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 4. 余额汇总卡片 -->
    <div class="balance-summary-box mb-12px border border-gray-800 p-8px bg-gray-50 text-12px">
      <div class="grid grid-cols-4 gap-8px text-center">
        <div class="border-r border-gray-300 pr-4px">
          <div class="text-gray-500 text-10px">期初应收余额</div>
          <div class="font-mono font-bold text-13px text-gray-800 mt-2px">
            {{ formatAmount(statement?.openingBalance) }}
          </div>
        </div>
        <div class="border-r border-gray-300 pr-4px">
          <div class="text-gray-500 text-10px">本期借方发生（发货增加）</div>
          <div class="font-mono font-bold text-13px text-blue-700 mt-2px">
            + {{ formatAmount(statement?.periodDebit) }}
          </div>
        </div>
        <div class="border-r border-gray-300 pr-4px">
          <div class="text-gray-500 text-10px">本期贷方发生（收款/冲减）</div>
          <div class="font-mono font-bold text-13px text-green-700 mt-2px">
            - {{ formatAmount(statement?.periodCredit) }}
          </div>
        </div>
        <div>
          <div class="text-gray-500 text-10px">期末应收余额</div>
          <div class="font-mono font-bold text-14px text-red-700 mt-2px">
            {{ formatAmount(statement?.closingBalance) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 5. 对账明细分录表格 -->
    <table class="print-table w-full text-11px border-collapse mb-12px">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-32px text-center">#</th>
          <th class="w-80px text-center">业务日期</th>
          <th class="w-55px text-center">方向</th>
          <th class="w-75px text-center">源单类型</th>
          <th class="w-110px text-left">源单单号</th>
          <th class="text-left">摘要 / 品名</th>
          <th class="w-80px text-right">借方(增加)</th>
          <th class="w-80px text-right">贷方(冲减)</th>
          <th class="w-60px text-center">签收标记</th>
          <th class="w-85px text-left">备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(item, idx) in statement?.items || []" :key="item.id || idx">
          <td class="text-center">{{ item.lineNo || (idx + 1) }}</td>
          <td class="text-center font-mono">{{ item.bizDate || '—' }}</td>
          <td class="text-center">
            <span
              class="px-4px py-1px rounded text-10px font-medium"
              :class="item.direction === 'DEBIT' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'"
            >
              {{ item.direction === 'DEBIT' ? '借方' : '贷方' }}
            </span>
          </td>
          <td class="text-center">{{ formatRefType(item.refType) }}</td>
          <td class="font-mono">{{ item.refNo || '—' }}</td>
          <td>{{ item.summary || '—' }}</td>
          <td class="text-right font-mono">
            {{ Number(item.debitAmount || 0) > 0 ? formatAmount(item.debitAmount) : '—' }}
          </td>
          <td class="text-right font-mono">
            {{ Number(item.creditAmount || 0) > 0 ? formatAmount(item.creditAmount) : '—' }}
          </td>
          <td class="text-center">
            <span v-if="item.signStatus" class="text-10px" :class="item.signStatus === 'SIGNED' ? 'text-green-600' : 'text-gray-500'">
              {{ item.signStatus === 'SIGNED' ? '已签收' : item.signStatus }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </td>
          <td class="text-10px text-gray-600">{{ item.remark || '—' }}</td>
        </tr>
        <tr v-if="!statement?.items || statement.items.length === 0">
          <td colspan="10" class="text-center py-16px text-gray-400">无明细记录</td>
        </tr>
      </tbody>
      <tfoot>
        <tr class="bg-gray-50 font-bold">
          <td colspan="6" class="text-right text-gray-700">本期发生额合计：</td>
          <td class="text-right font-mono text-blue-700">
            {{ formatAmount(statement?.periodDebit) }}
          </td>
          <td class="text-right font-mono text-green-700">
            {{ formatAmount(statement?.periodCredit) }}
          </td>
          <td colspan="2"></td>
        </tr>
      </tfoot>
    </table>

    <!-- 6. 确认信息与双方盖章签字栏 -->
    <div class="sign-block mt-20px text-12px">
      <!-- 确认记录展示 -->
      <div v-if="statement?.status === 'CONFIRMED'" class="confirmed-info-box border border-green-600 p-8px mb-12px bg-green-50 rounded">
        <div class="grid grid-cols-3 gap-6px text-11px text-green-900">
          <div>
            <span class="text-gray-600">确认方式：</span>
            <span class="font-bold">{{ statement.confirmType === 'ON_BEHALF' ? `内部代客确认 (${statement.confirmMethod || '线下凭证'})` : '客户门户在线确认' }}</span>
          </div>
          <div>
            <span class="text-gray-600">确认人：</span>
            <span class="font-bold">{{ statement.confirmByName || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">确认时间：</span>
            <span>{{ statement.confirmTime ? formatTimeStr(statement.confirmTime) : '—' }}</span>
          </div>
          <div v-if="statement.remark" class="col-span-3">
            <span class="text-gray-600">确认备注：</span>
            <span>{{ statement.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 双方签字盖章栏 -->
      <div class="grid grid-cols-2 gap-24px pt-8px border-t border-gray-400">
        <!-- 甲方：本公司 -->
        <div class="border border-dashed border-gray-400 p-12px rounded bg-white">
          <div class="font-bold mb-10px text-13px text-gray-800">本公司财务经办：</div>
          <div class="space-y-8px">
            <div>
              <span class="text-gray-600">制单人：</span>
              <span class="font-medium underline decoration-gray-400 underline-offset-4">
                {{ statement?.creator || '财务部' }}
              </span>
            </div>
            <div>
              <span class="text-gray-600">财务复核：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">公章 / 财务章：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">开单日期：</span>
              <span>{{ statement?.createTime ? formatTimeStr(statement.createTime).substring(0, 10) : '_____年____月____日' }}</span>
            </div>
          </div>
        </div>

        <!-- 乙方：客户单位 -->
        <div class="border border-dashed border-gray-400 p-12px rounded bg-white">
          <div class="font-bold mb-10px text-13px text-gray-800">客户核对确认：</div>
          <div class="space-y-8px">
            <div>
              <span class="text-gray-600">客户经办人：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">联系电话：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">公章 / 财务章：</span>
              <span class="inline-block w-120px border-b border-gray-700"></span>
            </div>
            <div>
              <span class="text-gray-600">确认日期：</span>
              <span>_____年____月____日</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-between items-center text-10px text-gray-500 mt-14px pt-6px border-t border-dashed border-gray-300">
        <div>注：请核对上述往来账务与期末余额，如有异议请在5个工作日内反馈，盖章后原件或扫描件回传有效。</div>
        <div>打印时间：{{ currentPrintTime }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import QrSlot from '@/components/Print/src/QrSlot.vue'
import type { CustomerStatementPrintVO } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

defineOptions({ name: 'CustomerStatementPrintTemplate' })

const props = defineProps({
  /** 单张对账单打印数据 */
  data: {
    type: Object as PropType<CustomerStatementPrintVO | null>,
    default: null
  },
  /** 批量列表（可选） */
  list: {
    type: Array as PropType<CustomerStatementPrintVO[]>,
    default: () => []
  }
})

const statement = computed<CustomerStatementPrintVO | null>(() => {
  if (props.data) return props.data
  if (props.list && props.list.length > 0) return props.list[0]
  return null
})

const statusText = computed(() => {
  const status = statement.value?.status
  if (status === 'CONFIRMED') return '已确认'
  if (status === 'SENT') return '已发送'
  if (status === 'REJECTED') return '已驳回'
  if (status === 'VOID') return '已作废'
  return '草稿'
})

const statusClass = computed(() => {
  const status = statement.value?.status
  if (status === 'CONFIRMED') return 'text-green-700'
  if (status === 'SENT') return 'text-blue-700'
  if (status === 'REJECTED') return 'text-red-700'
  if (status === 'VOID') return 'text-gray-500'
  return 'text-yellow-600'
})

const currentPrintTime = computed(() => {
  const now = new Date()
  return now.toISOString().replace('T', ' ').substring(0, 19)
})

const formatTimeStr = (t?: string) => {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}

const formatAmount = (val?: number | string) => {
  if (val === undefined || val === null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatRefType = (refType?: string) => {
  if (refType === 'SHIPMENT') return '发货单'
  if (refType === 'PAYMENT') return '收款单'
  if (refType === 'RETURN_ORDER') return '销售退货'
  return refType || '—'
}
</script>

<style scoped>
.customer-statement-print-wrapper {
  width: 100%;
  max-width: 820px;
  min-height: 500px;
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
  .customer-statement-print-wrapper {
    width: 100%;
    margin: 0;
  }
}
</style>
