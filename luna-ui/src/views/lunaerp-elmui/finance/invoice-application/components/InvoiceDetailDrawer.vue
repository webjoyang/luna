<template>
  <el-drawer v-model="visible" title="发票申请详情" size="720px" destroy-on-close>
    <div v-loading="loading" class="p-2 space-y-4">
      <!-- 基础信息卡片 -->
      <el-card shadow="never" class="!border-gray-200">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-bold text-15px">申请基本信息</span>
            <el-tag :type="formatInvoiceStatusTag(detail?.status).type" size="small">
              {{ formatInvoiceStatusTag(detail?.status).label }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请单号">
            <span class="font-bold text-primary">{{ detail?.invoiceNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="币种">
            {{ detail?.currency }}
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            {{ detail?.customerName }}
          </el-descriptions-item>
          <el-descriptions-item label="发票种类">
            {{ getInvoiceKindLabel(detail?.invoiceKind) }}
          </el-descriptions-item>
          <el-descriptions-item label="开票税率"> {{ detail?.taxRate }}% </el-descriptions-item>
          <el-descriptions-item label="介质类型">
            {{ detail?.electronic === 1 ? '电子发票' : '纸质发票' }}
          </el-descriptions-item>
          <el-descriptions-item label="价税合计总额" :span="2">
            <span class="font-bold text-danger text-16px">
              {{ detail?.currency }} {{ formatMoney(detail?.totalAmountInclTax) }}
            </span>
            <span class="text-12px text-gray-500 ml-3">
              (不含税: {{ formatMoney(detail?.totalAmountExclTax) }} + 税额:
              {{ formatMoney(detail?.totalTax) }})
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 抬头四要素 -->
      <el-card shadow="never" class="!border-gray-200">
        <template #header>
          <span class="font-bold text-15px">开票四要素快照</span>
        </template>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="开票抬头">
            {{ detail?.invoiceTitle || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="税号 / 信用代码">
            {{ detail?.taxNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开票地址电话">
            {{ detail?.address || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开户行及账号">
            {{ detail?.bankInfo || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 官方登记信息（已开票） -->
      <el-card v-if="detail?.status === 'ISSUED'" shadow="never" class="!border-gray-200">
        <template #header>
          <span class="font-bold text-15px text-success">官方开票信息</span>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="官方发票号" :span="2">
            <span class="font-mono font-bold text-success">{{ detail?.officialInvoiceNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="开票日期">
            {{ detail?.invoiceDate || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="发票代码">
            {{ detail?.invoiceCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开票登记人">
            {{ detail?.issueBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="开票登记时间">
            {{ detail?.issueTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail?.attachmentUrl" label="电子附件" :span="2">
            <el-link type="primary" :href="detail.attachmentUrl" target="_blank">
              查看 / 下载发票文件
            </el-link>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 明细行列表 -->
      <el-card shadow="never" class="!border-gray-200">
        <template #header>
          <span class="font-bold text-15px"
            >关联发货单及开票明细 ({{ detail?.items?.length || 0 }})</span
          >
        </template>
        <el-table :data="detail?.items || []" border size="small">
          <el-table-column type="index" label="#" width="45" align="center" />
          <el-table-column prop="shipmentNo" label="发货单号" min-width="150" />
          <el-table-column
            prop="shipmentAmount"
            label="发货单价税合计"
            min-width="120"
            align="right"
          >
            <template #default="{ row }">
              {{ formatMoney(row.shipmentAmount) }}
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="本次开票金额" min-width="120" align="right">
            <template #default="{ row }">
              <span class="font-bold text-primary">{{ formatMoney(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="明细备注" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.remark || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 审计与备注 -->
      <el-card shadow="never" class="!border-gray-200">
        <template #header>
          <span class="font-bold text-15px">申请流转与备注</span>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="提交人">
            {{ detail?.submitBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ detail?.submitTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ detail?.createTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">
            {{ detail?.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import {
  getInvoiceApplication,
  type InvoiceApplicationDetailRespVO
} from '@/api/lunaerp-elmui/finance/invoice'
import { formatMoney, formatInvoiceStatusTag, INVOICE_KIND_OPTIONS } from '../model/invoice'

const visible = ref(false)
const loading = ref(false)
const detail = ref<InvoiceApplicationDetailRespVO>()

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    detail.value = await getInvoiceApplication(id)
  } finally {
    loading.value = false
  }
}

const getInvoiceKindLabel = (kind?: string) => {
  const item = INVOICE_KIND_OPTIONS.find((k) => k.value === kind)
  return item ? item.label : kind || '-'
}

defineExpose({ open })
</script>
