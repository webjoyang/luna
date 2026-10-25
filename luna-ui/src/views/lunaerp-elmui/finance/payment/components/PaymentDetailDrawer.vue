<template>
  <el-drawer
    v-model="drawerVisible"
    :title="`付款单详情 - ${detail?.paymentNo || ''}`"
    size="680px"
    destroy-on-close
  >
    <div v-loading="loading" class="p-10px">
      <template v-if="detail">
        <!-- 基础信息卡片 -->
        <el-card shadow="never" class="mb-16px !border-gray-200">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-bold text-15px">基本信息</span>
              <el-tag :type="detail.status === 'COMPLETED' ? 'success' : 'info'" effect="light">
                {{ detail.status === 'COMPLETED' ? '已完成' : detail.status }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="付款单号">
              <span class="font-mono font-bold">{{ detail.paymentNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="供应商">
              <span class="font-medium text-gray-800">{{ detail.supplierName || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="资金账户">
              <span>{{ detail.accountName || `账户 #${detail.accountId}` }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="收付款方向">
              <el-tag type="warning" size="small">付款 (OUT)</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="本次资金实付">
              <span class="font-mono font-bold text-red-600">
                ¥ {{ formatMoney(detail.payAmount) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="预付抵扣金额">
              <span class="font-mono font-medium text-amber-700">
                ¥ {{ formatMoney(detail.prepayUsedAmount) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="本次结清总额" :span="2">
              <span class="font-mono font-bold text-primary text-15px">
                ¥
                {{
                  formatMoney(Number(detail.payAmount || 0) + Number(detail.prepayUsedAmount || 0))
                }}
              </span>
              <span class="text-12px text-gray-400 ml-8px">（实付金额 + 预付抵扣）</span>
            </el-descriptions-item>
            <el-descriptions-item label="登记人">
              <span>{{ detail.creator || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="登记时间">
              <span>{{ detail.createTime || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="备注说明" :span="2">
              <span class="text-gray-600">{{ detail.remark || '无' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 核销明细表格 -->
        <el-card shadow="never" class="!border-gray-200">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-bold text-15px">核销申请明细</span>
              <span class="text-12px text-gray-400">共 {{ detail.items?.length || 0 }} 笔申请</span>
            </div>
          </template>

          <el-table :data="detail.items || []" border stripe size="small">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column label="关联付款申请单号" prop="applyNo" min-width="160">
              <template #default="{ row }">
                <span class="font-mono font-medium text-primary">{{
                  row.applyNo || `ID #${row.refId}`
                }}</span>
              </template>
            </el-table-column>
            <el-table-column label="核销业务类型" prop="refType" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.refType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              label="本次分配核销金额"
              prop="allocateAmount"
              min-width="140"
              align="right"
            >
              <template #default="{ row }">
                <span class="font-mono font-bold text-gray-800">
                  ¥ {{ formatMoney(row.allocateAmount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="核销时间" prop="createTime" width="160" align="center">
              <template #default="{ row }">
                <span class="text-12px text-gray-500">{{ row.createTime || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </template>
    </div>

    <template #footer>
      <div class="flex justify-end">
        <el-button @click="drawerVisible = false">关闭</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { PaymentRespVO } from '@/api/lunaerp-elmui/finance/payment'
import { getPayment } from '@/api/lunaerp-elmui/finance/payment'
import { formatMoney, parsePaymentErrorMessage } from '../model/payment'

defineOptions({ name: 'PaymentDetailDrawer' })

const drawerVisible = ref(false)
const loading = ref(false)
const detail = ref<PaymentRespVO | null>(null)

const open = async (id: number) => {
  drawerVisible.value = true
  loading.value = true
  detail.value = null
  try {
    const res = await getPayment(id)
    detail.value = res
  } catch (err: any) {
    ElMessage.error(parsePaymentErrorMessage(err))
    drawerVisible.value = false
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
