<template>
  <div class="expense-sheet-wrapper print-paper text-gray-900 mx-auto">
    <div
      v-for="(sheet, sIdx) in sheetList"
      :key="sheet.id || sIdx"
      class="sheet-page"
      :class="{ 'page-break-after': sIdx < sheetList.length - 1 }"
    >
      <!-- 1. 抬头 -->
      <div class="print-header text-center mb-16px">
        <div class="main-title text-22px font-bold tracking-widest text-black mb-4px">
          {{ sheet.title || '员工费用报销单' }}
        </div>
        <div class="sub-title text-11px text-gray-500 uppercase tracking-wider">
          EMPLOYEE EXPENSE REIMBURSEMENT
        </div>
      </div>

      <!-- 2. 主表元数据 -->
      <div class="meta-box mb-14px text-12px border border-gray-700 p-10px bg-gray-50">
        <div class="meta-grid grid grid-cols-3 gap-y-8px gap-x-16px">
          <div class="meta-item">
            <span class="label text-gray-600">报销单号：</span>
            <span class="value font-mono font-bold">{{ sheet.expenseNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">报销人：</span>
            <span class="value font-bold">{{ sheet.applicantName || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">付款账户：</span>
            <span class="value font-medium">{{ sheet.accountName || '—' }}</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">币种：</span>
            <span class="value font-mono font-medium">{{ sheet.currency || 'RMB' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">业务日期：</span>
            <span class="value font-mono">{{ sheet.expenseDate || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">审批状态：</span>
            <span class="value font-bold text-green-700">已审批 (APPROVED)</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">提交时间：</span>
            <span class="value font-mono">{{ sheet.submitTime || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">通过时间：</span>
            <span class="value font-mono">{{ sheet.approveTime || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">打印时间：</span>
            <span class="value font-mono">{{ sheet.printTime || formatPrintTime() }}</span>
          </div>

          <div v-if="sheet.remark" class="meta-item col-span-3">
            <span class="label text-gray-600">报销备注：</span>
            <span class="value">{{ sheet.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 3. 明细行表 -->
      <div class="table-box mb-14px">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px">序号</th>
              <th style="width: 110px">费用类别</th>
              <th>费用说明</th>
              <th style="width: 100px">发生日期</th>
              <th style="width: 120px" class="text-right"
                >原币金额 ({{ sheet.currency || 'RMB' }})</th
              >
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in sheet.items" :key="item.lineNo || idx">
              <td class="text-center font-mono">{{ item.lineNo || idx + 1 }}</td>
              <td class="text-center">{{ formatCategoryText(item.category) }}</td>
              <td>{{ item.remark || '—' }}</td>
              <td class="text-center font-mono">{{ item.occurDate || '—' }}</td>
              <td class="text-right font-mono">{{ formatAmount(item.originalAmount) }}</td>
            </tr>
            <!-- 合计行 -->
            <tr class="total-row">
              <td colspan="4" class="text-right font-bold">
                原币总金额：
                <span v-if="sheet.currency && sheet.currency !== 'RMB' && sheet.exchangeRate">
                  (汇率: {{ sheet.exchangeRate }}，折合本位币: ¥{{ formatAmount(sheet.baseTotal) }})
                </span>
              </td>
              <td class="text-right font-mono font-bold text-13px">
                {{ formatAmount(sheet.originalTotal || sheet.totalAmount) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. 附件清单清单 -->
      <div
        class="attachments-box text-11px text-gray-700 bg-white border border-gray-700 p-8px mb-14px"
      >
        <span class="font-bold text-gray-800"
          >附件清单（共 {{ sheet.attachmentNames?.length || 0 }} 份）：</span
        >
        <span v-if="sheet.attachmentNames && sheet.attachmentNames.length > 0">
          {{ sheet.attachmentNames.join('、') }}
        </span>
        <span v-else class="text-gray-400">无附件</span>
      </div>

      <!-- 5. 五级签章区 -->
      <div class="sign-box mt-16px border border-gray-700">
        <div class="sign-grid grid grid-cols-5 text-center">
          <div
            class="sign-col border-r border-gray-700 p-8px min-h-70px flex flex-col justify-between"
          >
            <div class="sign-title font-semibold text-12px text-gray-700">报销人</div>
            <div class="sign-name font-medium text-12px">{{
              sheet.applicantSign || sheet.applicantName || '—'
            }}</div>
          </div>
          <div
            class="sign-col border-r border-gray-700 p-8px min-h-70px flex flex-col justify-between"
          >
            <div class="sign-title font-semibold text-12px text-gray-700">部门主管</div>
            <div class="sign-name font-medium text-12px">{{ sheet.departmentSign || '—' }}</div>
          </div>
          <div
            class="sign-col border-r border-gray-700 p-8px min-h-70px flex flex-col justify-between"
          >
            <div class="sign-title font-semibold text-12px text-gray-700">财务审核</div>
            <div class="sign-name font-medium text-12px">{{ sheet.financeSign || '—' }}</div>
          </div>
          <div
            class="sign-col border-r border-gray-700 p-8px min-h-70px flex flex-col justify-between"
          >
            <div class="sign-title font-semibold text-12px text-gray-700">总经理/审批人</div>
            <div class="sign-name font-medium text-12px">{{ sheet.managerSign || '—' }}</div>
          </div>
          <div class="sign-col p-8px min-h-70px flex flex-col justify-between">
            <div class="sign-title font-semibold text-12px text-gray-700">出纳付款</div>
            <div class="sign-name font-medium text-12px">{{ sheet.cashierSign || '—' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExpensePrintRespVO } from '@/api/lunaerp-elmui/finance/expense'
import { formatAmount } from '../../model/expense'

defineOptions({ name: 'ExpensePrintSheet' })

const props = defineProps<{
  data?: ExpensePrintRespVO
  list?: ExpensePrintRespVO[]
}>()

const sheetList = computed<ExpensePrintRespVO[]>(() => {
  if (props.list && props.list.length > 0) {
    return props.list
  }
  return props.data ? [props.data] : []
})

const CATEGORY_MAP: Record<string, string> = {
  TRAVEL: '差旅费',
  OFFICE: '办公费',
  TRANSPORT: '运输费',
  ENTERTAIN: '业务招待费',
  RENT: '房租',
  UTILITY: '水电费',
  COMM: '通讯费',
  OTHER: '其他'
}

const formatCategoryText = (cat?: string) => {
  if (!cat) return '—'
  return CATEGORY_MAP[cat] || cat
}

const formatPrintTime = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const mi = String(now.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${mi}`
}
</script>
