<template>
  <el-dialog
    v-model="visible"
    :title="`对账单详情 - ${detail?.statementNo || ''}`"
    width="1000px"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading">
      <template v-if="detail">
        <!-- 主表基本信息 -->
        <el-descriptions :column="3" border class="mb-4">
          <el-descriptions-item label="对账单号">
            <span class="font-bold text-gray-800">{{ detail.statementNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="对账客户">
            {{ detail.customerName }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_STATEMENT_STATUS" :value="detail.status" />
          </el-descriptions-item>

          <el-descriptions-item label="币种">
            <el-tag size="small">{{ detail.currency }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="对账期间" :span="2">
            {{ detail.periodStart }} 至 {{ detail.periodEnd }}
          </el-descriptions-item>

          <el-descriptions-item label="创建时间">
            {{ detail.createTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">
            {{ detail.remark || '-' }}
          </el-descriptions-item>

          <!-- 确认/驳回信息（如有） -->
          <template v-if="detail.confirmType || detail.confirmByName || detail.rejectReason">
            <el-descriptions-item v-if="detail.confirmType" label="确认方式">
              <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_STATEMENT_CONFIRM_TYPE" :value="detail.confirmType" />
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.confirmByName" label="确认人">
              {{ detail.confirmByName }} ({{ detail.confirmTime || '-' }})
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.confirmMethod" label="确认凭证说明">
              {{ detail.confirmMethod }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="3">
              <span class="text-red-500">{{ detail.rejectReason }}</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.replyRemark" label="复核备注" :span="3">
              <span class="text-blue-600">{{ detail.replyRemark }}</span>
            </el-descriptions-item>
          </template>
        </el-descriptions>

        <!-- 往来余额汇总栏 -->
        <div class="grid grid-cols-4 gap-4 p-3 bg-gray-50 rounded border border-gray-200 mb-4">
          <div class="text-center">
            <div class="text-xs text-gray-500">期初余额 (起始日前未核销AR)</div>
            <div class="text-base font-bold text-gray-800 mt-1">
              {{ formatAmount(detail.openingBalance) }} {{ detail.currency }}
            </div>
          </div>
          <div class="text-center">
            <div class="text-xs text-blue-600">本期借方发生额 (发货应收)</div>
            <div class="text-base font-bold text-blue-600 mt-1">
              + {{ formatAmount(detail.periodDebit) }} {{ detail.currency }}
            </div>
          </div>
          <div class="text-center">
            <div class="text-xs text-green-600">本期贷方发生额 (核销/退货)</div>
            <div class="text-base font-bold text-green-600 mt-1">
              - {{ formatAmount(detail.periodCredit) }} {{ detail.currency }}
            </div>
          </div>
          <div class="text-center">
            <div class="text-xs text-amber-600">期末余额 (期初+借-贷)</div>
            <div class="text-base font-bold text-amber-600 mt-1">
              = {{ formatAmount(detail.closingBalance) }} {{ detail.currency }}
            </div>
          </div>
        </div>

        <!-- 明细行列表 -->
        <div class="text-sm font-semibold text-gray-700 mb-2">对账明细分录行列表</div>
        <el-table :data="detail.items || []" border max-height="380px">
          <el-table-column label="行号" prop="lineNo" width="60" align="center" />
          <el-table-column label="业务日期" prop="bizDate" width="110" align="center" />
          <el-table-column label="方向" prop="direction" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.direction === 'DEBIT' ? 'primary' : 'success'" size="small">
                {{ row.direction === 'DEBIT' ? '借方' : '贷方' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="源单单号" prop="refNo" min-width="140" show-overflow-tooltip />
          <el-table-column label="品名事由/摘要" prop="summary" min-width="160" show-overflow-tooltip />
          <el-table-column label="借方金额" prop="debitAmount" width="110" align="right">
            <template #default="{ row }">
              <span v-if="row.direction === 'DEBIT'" class="font-medium text-blue-600">
                {{ formatAmount(row.debitAmount) }}
              </span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="贷方金额" prop="creditAmount" width="110" align="right">
            <template #default="{ row }">
              <span v-if="row.direction === 'CREDIT'" class="font-medium text-green-600">
                {{ formatAmount(row.creditAmount) }}
              </span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="签收标记" prop="signStatus" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.signStatus === 'SIGNED'" type="success" size="small">已签收</el-tag>
              <el-tag v-else-if="row.signStatus === 'REFUSED'" type="danger" size="small">拒签</el-tag>
              <el-tag v-else-if="row.direction === 'DEBIT'" type="info" size="small">未签收</el-tag>
              <span v-else class="text-gray-400">-</span>
            </template>
          </el-table-column>
          <el-table-column label="行备注" prop="remark" min-width="100" show-overflow-tooltip />
        </el-table>
      </template>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { DICT_TYPE } from '@/utils/dict'
import { getCustomerStatement } from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import type { CustomerStatementDetailVO } from '@/api/lunaerp-elmui/finance/customer-reconciliation'

const visible = ref(false)
const loading = ref(false)
const detail = ref<CustomerStatementDetailVO | null>(null)

const formatAmount = (val?: number | string) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    detail.value = await getCustomerStatement(id)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
