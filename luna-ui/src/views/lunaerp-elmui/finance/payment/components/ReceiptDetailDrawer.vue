<template>
  <el-drawer
    v-model="visible"
    :title="`收款单详情 - ${receipt?.paymentNo || ''}`"
    size="720px"
    destroy-on-close
  >
    <div v-loading="loading" class="p-4px">
      <!-- 基础信息卡片 -->
      <el-card shadow="never" class="mb-16px !border-gray-200">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-bold text-14px text-gray-800">基本信息</span>
            <div class="space-x-8px">
              <el-tag :type="getApprovalStatusTagType(receipt?.approvalStatus)">
                {{ getApprovalStatusText(receipt?.approvalStatus) }}
              </el-tag>
              <el-tag v-if="receipt?.status === 'COMPLETED'" type="success" effect="plain">
                已过账完成
              </el-tag>
              <el-tag v-else type="info" effect="plain"> 待过账 </el-tag>
            </div>
          </div>
        </template>

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="收款单号">
            <span class="font-mono font-medium text-gray-800">{{ receipt?.paymentNo || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收款类型">
            <el-tag size="small" :type="receipt?.receiptType === 'ADVANCE' ? 'warning' : 'primary'">
              {{ receipt?.receiptType === 'ADVANCE' ? '预收款' : '客户收款' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">
            <span class="font-medium text-gray-800">{{ receipt?.customerName || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收款账户">
            <span>{{ receipt?.accountName || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收款币种">
            <el-tag size="small">{{ receipt?.currency || 'RMB' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收款金额">
            <span class="font-mono font-bold text-emerald-700 text-15px">
              ¥ {{ formatMoney(receipt?.payAmount) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="已核销金额">
            <span class="font-mono text-gray-700">
              ¥ {{ formatMoney(receipt?.allocatedTotalAmount) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="未核销金额">
            <span class="font-mono text-amber-600">
              ¥ {{ formatMoney(receipt?.unallocatedAmount) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="收款日期">
            <span>{{ receipt?.receiptDate || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="经手人">
            <span>{{ receipt?.handlerName || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="银行流水/回单号" :span="2">
            <span class="font-mono">{{ receipt?.bankRefNo || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="制单人">
            <span>{{ receipt?.creator || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            <span class="font-mono">{{ receipt?.createTime || '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">
            <span class="text-gray-600">{{ receipt?.remark || '无' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 核销分配明细表格 -->
      <el-card shadow="never" class="!border-gray-200">
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <span class="font-bold text-14px text-gray-800">核销明细列表</span>
              <span class="text-12px text-gray-400 ml-8px">
                共 {{ receipt?.items?.length || 0 }} 笔分配分录
              </span>
            </div>
            <div class="text-13px">
              分配合计：
              <span class="font-mono font-bold text-emerald-700">
                ¥ {{ formatMoney(totalAllocated) }}
              </span>
            </div>
          </div>
        </template>

        <el-table :data="receipt?.items || []" border stripe size="small" empty-text="暂无核销分录">
          <el-table-column label="#" type="index" width="45" align="center" />
          <el-table-column label="业务日期" prop="bizDate" width="105" align="center" />
          <el-table-column label="发货单号" prop="refNo" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="font-mono">{{ row.refNo || row.entryNo || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="合同单号" prop="contractNo" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="font-mono text-gray-600">{{ row.contractNo || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="摘要/产品"
            prop="productName"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column label="分录总额" prop="originalAmount" width="110" align="right">
            <template #default="{ row }">
              <span class="font-mono text-gray-600">¥ {{ formatMoney(row.originalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="核销金额" prop="allocateAmount" width="120" align="right">
            <template #default="{ row }">
              <span class="font-mono font-bold text-emerald-700">
                ¥ {{ formatMoney(row.allocateAmount) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getReceipt, type ReceiptRespVO } from '@/api/lunaerp-elmui/finance/payment'
import { formatMoney, getApprovalStatusText, getApprovalStatusTagType } from '../model/payment'

defineOptions({ name: 'ReceiptDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const receipt = ref<ReceiptRespVO | null>(null)

const totalAllocated = computed(() => {
  if (!receipt.value?.items) return 0
  return receipt.value.items.reduce((sum, item) => sum + Number(item.allocateAmount || 0), 0)
})

const open = async (id: number) => {
  visible.value = true
  loading.value = true
  try {
    const res = await getReceipt(id)
    receipt.value = res
  } catch (err: any) {
    ElMessage.error(err.message || '获取收款单详情失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
