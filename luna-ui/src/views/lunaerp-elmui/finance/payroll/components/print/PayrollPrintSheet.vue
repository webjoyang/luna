<template>
  <div class="payroll-sheet-wrapper print-paper text-gray-900 mx-auto">
    <div
      v-for="(sheet, sIdx) in sheetList"
      :key="sheet.id || sIdx"
      class="sheet-page"
      :class="{ 'page-break-after': sIdx < sheetList.length - 1 }"
    >
      <!-- 1. 抬头 -->
      <div class="print-header text-center mb-16px">
        <div class="main-title text-22px font-bold tracking-widest text-black mb-4px">
          {{ sheet.title || `${sheet.period || ''} 员工工资表` }}
        </div>
        <div class="sub-title text-11px text-gray-500 uppercase tracking-wider">
          EMPLOYEE PAYROLL SUMMARY
        </div>
      </div>

      <!-- 2. 主表元数据 -->
      <div class="meta-box mb-14px text-12px border border-gray-700 p-10px bg-gray-50">
        <div class="meta-grid grid grid-cols-3 gap-y-8px gap-x-16px">
          <div class="meta-item">
            <span class="label text-gray-600">工资单号：</span>
            <span class="value font-mono font-bold">{{ sheet.payrollNo || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">工资期间：</span>
            <span class="value font-mono font-bold">{{ sheet.period || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">审批状态：</span>
            <span class="value font-bold text-green-700">已审批 (APPROVED)</span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">应发总计：</span>
            <span class="value font-mono font-bold">¥ {{ formatAmount(sheet.earningTotal) }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">扣款总计：</span>
            <span class="value font-mono font-bold text-red-600">
              ¥ {{ formatAmount(sheet.deductionTotal) }}
            </span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">实发总计：</span>
            <span class="value font-mono font-bold text-blue-700">
              ¥ {{ formatAmount(sheet.netAmount) }}
            </span>
          </div>

          <div class="meta-item">
            <span class="label text-gray-600">提交时间：</span>
            <span class="value font-mono">{{ sheet.submitTime || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">审批通过：</span>
            <span class="value font-mono">{{ sheet.approveTime || '—' }}</span>
          </div>
          <div class="meta-item">
            <span class="label text-gray-600">打印时间：</span>
            <span class="value font-mono">{{ sheet.printTime || currentPrintTime }}</span>
          </div>

          <div v-if="sheet.remark" class="meta-item col-span-3">
            <span class="label text-gray-600">备注说明：</span>
            <span class="value">{{ sheet.remark }}</span>
          </div>
        </div>
      </div>

      <!-- 3. 员工工资明细表（每人一行 应发/扣款/实发 + 签收栏） -->
      <div class="table-box mb-14px">
        <table class="data-table">
          <thead>
            <tr>
              <th style="width: 50px">序号</th>
              <th style="width: 100px">员工姓名</th>
              <th style="width: 120px" class="text-right">应发合计 (¥)</th>
              <th style="width: 120px" class="text-right">扣款合计 (¥)</th>
              <th style="width: 130px" class="text-right">实发合计 (¥)</th>
              <th style="width: 110px">员工签收</th>
              <th>备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in sheet.items" :key="item.lineNo || idx">
              <td class="text-center font-mono">{{ item.lineNo || idx + 1 }}</td>
              <td class="text-center font-medium">{{ item.nickname || '—' }}</td>
              <td class="text-right font-mono">{{ formatAmount(item.earningTotal) }}</td>
              <td class="text-right font-mono text-red-600">
                {{ formatAmount(item.deductionTotal) }}
              </td>
              <td class="text-right font-mono font-bold text-blue-800">
                {{ formatAmount(item.netAmount) }}
              </td>
              <td class="text-center">
                <span v-if="item.signature">{{ item.signature }}</span>
                <span v-else class="sign-line inline-block w-80px border-b border-gray-400"></span>
              </td>
              <td>{{ item.remark || '—' }}</td>
            </tr>
            <!-- 合计汇总行 -->
            <tr class="total-row bg-gray-100 font-bold">
              <td colspan="2" class="text-center font-bold">合 计</td>
              <td class="text-right font-mono">{{ formatAmount(sheet.earningTotal) }}</td>
              <td class="text-right font-mono text-red-600">
                {{ formatAmount(sheet.deductionTotal) }}
              </td>
              <td class="text-right font-mono font-bold text-13px text-blue-900">
                {{ formatAmount(sheet.netAmount) }}
              </td>
              <td colspan="2"></td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 4. 签章与责任栏 -->
      <div class="sign-box mt-16px border border-gray-700 p-12px bg-white">
        <div class="sign-grid grid grid-cols-4 gap-16px text-12px">
          <div class="sign-item">
            <span class="sign-label text-gray-700">制表人：</span>
            <div class="sign-line border-b border-gray-400 h-24px mt-4px"></div>
          </div>
          <div class="sign-item">
            <span class="sign-label text-gray-700">人事审核：</span>
            <div class="sign-line border-b border-gray-400 h-24px mt-4px"></div>
          </div>
          <div class="sign-item">
            <span class="sign-label text-gray-700">财务主管：</span>
            <div class="sign-line border-b border-gray-400 h-24px mt-4px"></div>
          </div>
          <div class="sign-item">
            <span class="sign-label text-gray-700">出纳发放：</span>
            <div class="sign-line border-b border-gray-400 h-24px mt-4px"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatAmount } from '../../model/payroll'
import type { PayrollPrintRespVO } from '@/api/lunaerp-elmui/finance/payroll'

defineOptions({ name: 'PayrollPrintSheet' })

const props = defineProps<{
  docData?: PayrollPrintRespVO | PayrollPrintRespVO[]
}>()

const sheetList = computed<PayrollPrintRespVO[]>(() => {
  if (!props.docData) return []
  return Array.isArray(props.docData) ? props.docData : [props.docData]
})

const currentPrintTime = computed(() => {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(
    now.getHours()
  )}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
})
</script>

<style scoped>
.payroll-sheet-wrapper {
  width: 100%;
  max-width: 180mm;
  margin: 0 auto;
}
.meta-box {
  background: #fafafa;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th,
.data-table td {
  border: 1px solid #333;
  padding: 6px 8px;
}
.page-break-after {
  page-break-after: always;
}
</style>
