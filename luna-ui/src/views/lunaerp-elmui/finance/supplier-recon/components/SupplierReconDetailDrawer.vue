<template>
  <el-drawer
    v-model="visible"
    :title="`对账单详情 - ${recon?.reconNo || ''}`"
    size="850px"
    destroy-on-close
  >
    <div v-loading="loading" class="p-4 space-y-4">
      <!-- 基础信息卡片 -->
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="对账单号">
          <span class="font-mono font-bold text-primary">{{ recon?.reconNo || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="单据状态">
          <DictTag :type="DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS" :value="recon?.status" />
        </el-descriptions-item>
        <el-descriptions-item label="供应商名称">
          <span class="font-bold">{{ recon?.supplierName || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="对账区间">
          <span class="font-mono">
            {{ recon?.periodStart && recon?.periodEnd ? `${recon.periodStart} ~ ${recon.periodEnd}` : '—' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="确认总金额">
          <span class="font-mono font-bold text-base text-blue-700">
            ¥ {{ formatAmount(recon?.totalAmount) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="已申请金额（实时聚合）">
          <span class="font-mono font-bold text-base text-green-700">
            ¥ {{ formatAmount(recon?.appliedAmount) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ recon?.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 审计留痕信息 -->
      <el-descriptions :column="2" border size="small" class="bg-gray-50">
        <el-descriptions-item label="确认人">
          {{ recon?.confirmedByName || recon?.confirmedBy || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="确认时间">
          {{ recon?.confirmedAt || '—' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="recon?.cancelledAt" label="作废人">
          {{ recon?.cancelledBy || '—' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="recon?.cancelledAt" label="作废时间">
          {{ recon?.cancelledAt || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ recon?.createTime || '—' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 明细列表 -->
      <div>
        <div class="flex justify-between items-center mb-2">
          <span class="font-bold text-gray-700 text-sm">
            对账明细列表（共 {{ recon?.items?.length || 0 }} 行）
          </span>
          <div class="text-xs text-gray-500">
            加工费: {{ processCount }} 行 | 采购货款: {{ purchaseCount }} 行 | 调整项: {{ adjustCount }} 行
          </div>
        </div>

        <el-table :data="recon?.items || []" border stripe size="small" max-height="460">
          <el-table-column type="index" label="#" width="45" align="center" />
          <el-table-column label="行类型" width="85" align="center">
            <template #default="{ row }">
              <el-tag :type="getLineTypeTagType(row.lineType)" size="small">
                {{ getLineTypeLabel(row.lineType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源单据" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <div v-if="row.lineType === 'PROCESS'">
                <el-link
                  type="primary"
                  :underline="false"
                  @click="openInspectionOrder(row.inspectionOrderId)"
                  class="font-mono text-xs"
                >
                  <Icon icon="ep:link" class="mr-1 text-xs" />
                  {{ row.inspectionOrderNo || '检验单' }}
                </el-link>
                <span class="text-gray-400 text-xs ml-1">
                  ({{ row.inspectionType === 'GREIGE' ? '织费' : '染费' }})
                </span>
              </div>
              <div v-else-if="row.lineType === 'PURCHASE'">
                <el-link
                  type="primary"
                  :underline="false"
                  @click="openStockIn(row.stockInId)"
                  class="font-mono text-xs"
                >
                  <Icon icon="ep:link" class="mr-1 text-xs" />
                  {{ row.stockInNo || '入库单' }}
                </el-link>
                <div v-if="row.yarnOrderNo" class="text-xs text-gray-500 font-mono">
                  纱采购: {{ row.yarnOrderNo }}
                </div>
              </div>
              <span v-else class="text-gray-400 text-xs">手工调整</span>
            </template>
          </el-table-column>
          <el-table-column label="产品 / 批次" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <div v-if="row.productName" class="font-medium text-xs">{{ row.productName }}</div>
              <div v-if="row.productCode" class="text-gray-400 text-10px font-mono">{{ row.productCode }}</div>
              <div v-if="row.batchNo" class="text-gray-500 text-10px font-mono">批次: {{ row.batchNo }}</div>
              <span v-if="!row.productName && !row.productCode && row.lineType === 'ADJUST'" class="text-gray-400 text-xs">—</span>
            </template>
          </el-table-column>
          <el-table-column label="数量 / 基准" width="120" align="right">
            <template #default="{ row }">
              <div v-if="row.lineType === 'PROCESS'" class="font-mono text-xs">
                <span v-if="row.pricingBasis === 'KG'">{{ Number(row.qtyKg || 0).toFixed(2) }} KG</span>
                <span v-else>{{ Number(row.qtyM || 0).toFixed(2) }} M</span>
                <span class="text-gray-400 text-10px block">基准: {{ row.pricingBasis }}</span>
              </div>
              <div v-else-if="row.lineType === 'PURCHASE'" class="font-mono text-xs">
                {{ Number(row.quantity || row.billQty || 0).toFixed(2) }} {{ row.unit || row.qtyUnit || 'KG' }}
              </div>
              <span v-else class="text-gray-400 text-xs">—</span>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="85" align="right">
            <template #default="{ row }">
              <span v-if="row.unitPrice != null" class="font-mono text-xs">
                ¥ {{ Number(row.unitPrice).toFixed(4) }}
              </span>
              <span v-else class="text-gray-400 text-xs">—</span>
            </template>
          </el-table-column>
          <el-table-column label="行金额" width="105" align="right">
            <template #default="{ row }">
              <span
                class="font-mono font-bold text-xs"
                :class="row.lineAmount && row.lineAmount < 0 ? 'text-red-600' : ''"
              >
                ¥ {{ formatAmount(row.lineAmount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="原因 / 备注" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.adjustReason" class="text-red-700 font-medium text-xs mr-1">
                [{{ row.adjustReason }}]
              </span>
              <span class="text-xs">{{ row.remark || '' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-between items-center">
        <div class="text-xs text-gray-500">
          合计确认金额：<span class="font-bold text-blue-700 text-sm font-mono">¥ {{ formatAmount(recon?.totalAmount) }}</span>
        </div>
        <div class="space-x-2">
          <el-button
            v-if="recon?.status === 'DRAFT'"
            type="primary"
            @click="gotoWorkspace(recon?.id)"
          >
            去工作台编辑
          </el-button>
          <el-button
            v-if="recon?.status === 'CONFIRMED'"
            type="primary"
            plain
            @click="emit('print', recon?.id)"
          >
            打印对账单
          </el-button>
          <el-button @click="visible = false">关闭</el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { DICT_TYPE } from '@/utils/dict'
import * as SupplierReconApi from '@/api/lunaerp-elmui/finance/supplier-recon'
import type { SupplierReconVO } from '@/api/lunaerp-elmui/finance/supplier-recon'
import { formatAmount } from './print/utils'

defineOptions({ name: 'SupplierReconDetailDrawer' })

const emit = defineEmits(['print'])
const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const recon = ref<SupplierReconVO | null>(null)

const processCount = computed(() => recon.value?.items?.filter((i) => i.lineType === 'PROCESS').length || 0)
const purchaseCount = computed(() => recon.value?.items?.filter((i) => i.lineType === 'PURCHASE').length || 0)
const adjustCount = computed(() => recon.value?.items?.filter((i) => i.lineType === 'ADJUST').length || 0)

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  recon.value = null
  try {
    recon.value = await SupplierReconApi.getSupplierRecon(id)
  } finally {
    loading.value = false
  }
}

const gotoWorkspace = (id?: number) => {
  visible.value = false
  if (id) {
    router.push(`/finance/supplier-recon/workspace/${id}`)
  }
}

const openInspectionOrder = (id?: number) => {
  if (!id) return
  router.push({
    path: `/production/inspection-order/workspace/${id}`,
    query: { mode: 'view' }
  })
}

const openStockIn = (id?: number) => {
  if (!id) return
  router.push({
    path: `/stock/stock-in/workspace/${id}`,
    query: { mode: 'view' }
  })
}

const getLineTypeLabel = (lineType?: string) => {
  if (lineType === 'PROCESS') return '加工费'
  if (lineType === 'PURCHASE') return '采购货款'
  if (lineType === 'ADJUST') return '调整项'
  return lineType || '—'
}

const getLineTypeTagType = (lineType?: string): 'primary' | 'warning' | 'danger' | 'info' => {
  if (lineType === 'PROCESS') return 'primary'
  if (lineType === 'PURCHASE') return 'warning'
  if (lineType === 'ADJUST') return 'danger'
  return 'info'
}

defineExpose({
  open
})
</script>
