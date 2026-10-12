<template>
  <el-drawer
    v-model="visible"
    title="发票申请详情"
    size="720px"
    append-to-body
    :close-on-click-modal="true"
  >
    <div v-loading="loading" class="p-2">
      <template v-if="detail">
        <!-- 头部核心概览 -->
        <div class="flex items-center justify-between border-b pb-4 mb-4">
          <div>
            <div class="text-xs text-gray-500 mb-1">发票申请单号</div>
            <div class="text-xl font-bold text-gray-800">{{ detail.invoiceNo }}</div>
          </div>
          <div>
            <el-tag :type="getStatusTagType(detail.status)" size="large" effect="dark">
              {{ getStatusLabel(detail.status) }}
            </el-tag>
          </div>
        </div>

        <!-- 驳回提示 -->
        <el-alert
          v-if="detail.status === 'REJECTED' && detail.rejectReason"
          :title="`驳回原因：${detail.rejectReason}`"
          type="error"
          show-icon
          :closable="false"
          class="mb-4"
        />

        <!-- 金额概览卡片 -->
        <div class="grid grid-cols-3 gap-4 bg-blue-50/50 p-4 rounded-lg mb-6 border border-blue-100">
          <div>
            <div class="text-xs text-gray-500 mb-1">价税合计</div>
            <div class="text-lg font-bold text-red-600">￥{{ formatAmount(detail.totalAmountInclTax) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">不含税金额</div>
            <div class="text-lg font-semibold text-gray-800">￥{{ formatAmount(detail.totalAmountExclTax) }}</div>
          </div>
          <div>
            <div class="text-xs text-gray-500 mb-1">税额合计（{{ detail.taxRate }}%）</div>
            <div class="text-lg font-semibold text-gray-800">￥{{ formatAmount(detail.totalTax) }}</div>
          </div>
        </div>

        <!-- 基本信息 -->
        <el-descriptions title="基础信息" :column="2" border class="mb-6">
          <el-descriptions-item label="客户名称">{{ detail.customerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ detail.currency || '—' }}</el-descriptions-item>
          <el-descriptions-item label="发票种类">{{ getInvoiceKindLabel(detail.invoiceKind) }}</el-descriptions-item>
          <el-descriptions-item label="电子发票">{{ detail.electronic === 1 ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="提交信息">
            <span v-if="detail.submitBy">{{ detail.submitBy }} ({{ detail.submitTime }})</span>
            <span v-else>—</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 开票四要素快照 -->
        <el-descriptions title="开票抬头快照" :column="1" border class="mb-6">
          <el-descriptions-item label="开票抬头">{{ detail.invoiceTitle || '—' }}</el-descriptions-item>
          <el-descriptions-item label="纳税人识别号/税号">{{ detail.taxNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="开票地址电话">{{ detail.address || '—' }}</el-descriptions-item>
          <el-descriptions-item label="开户银行及账号">{{ detail.bankInfo || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 正式发票信息（若已开票） -->
        <el-descriptions v-if="detail.status === 'ISSUED'" title="正式发票信息" :column="2" border class="mb-6">
          <el-descriptions-item label="官方发票号码" :span="2">
            <span class="font-bold text-green-700 tracking-wider">{{ detail.officialInvoiceNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="开票日期">{{ detail.invoiceDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="开票人">{{ detail.issueBy || '—' }} ({{ detail.issueTime }})</el-descriptions-item>
          <el-descriptions-item label="发票附件" :span="2">
            <el-link v-if="detail.attachmentUrl" :href="detail.attachmentUrl" target="_blank" type="primary">
              查看/下载发票附件
            </el-link>
            <span v-else>无</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 关联发货明细 -->
        <div class="mb-2 font-bold text-gray-800">发货单开票明细</div>
        <el-table :data="detail.items || []" border style="width: 100%" class="mb-4">
          <el-table-column label="序号" prop="lineNo" width="60" align="center" />
          <el-table-column label="发货单号" prop="shipmentNo" min-width="160" />
          <el-table-column label="发货单价税合计" width="140" align="right">
            <template #default="{ row }">
              {{ formatAmount(row.shipmentAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="本次申请开票额" width="140" align="right">
            <template #default="{ row }">
              <span class="font-bold text-blue-600">{{ formatAmount(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        </el-table>

        <div v-if="detail.remark" class="text-sm text-gray-500">
          <span class="font-medium text-gray-700">申请备注：</span>{{ detail.remark }}
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import * as InvoiceApi from '@/api/lunaerp-elmui/finance/invoice'
import type { InvoiceVO } from '@/api/lunaerp-elmui/finance/invoice'

defineOptions({ name: 'InvoiceDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<InvoiceVO | null>(null)

function formatAmount(val?: number | string | null): string {
  if (val == null || val === '') return '0.00'
  const num = Number(val)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getStatusTagType(status?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'DRAFT':
      return 'info'
    case 'SUBMITTED':
      return 'warning'
    case 'ISSUED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

function getStatusLabel(status?: string): string {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'SUBMITTED':
      return '已提交'
    case 'ISSUED':
      return '已开票'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '—'
  }
}

function getInvoiceKindLabel(kind?: string): string {
  switch (kind) {
    case 'ELEC_SPECIAL':
      return '数电专票'
    case 'ELEC_NORMAL':
      return '数电普票'
    case 'VAT_SPECIAL':
      return '增值税专票'
    case 'VAT_NORMAL':
      return '增值税普票'
    default:
      return kind || '—'
  }
}

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    detail.value = await InvoiceApi.getInvoice(id)
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
