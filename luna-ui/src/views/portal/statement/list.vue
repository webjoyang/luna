<template>
  <PortalLayout title="客户对账单" :customer-name="customerName">
    <div v-loading="loading || listLoading" class="portal-statement-list-page">
      <!-- 状态筛选 Tabs -->
      <div class="tabs-bar">
        <el-tabs v-model="activeTab" class="portal-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="全部" name="" />
          <el-tab-pane label="待确认" name="SENT" />
          <el-tab-pane label="已确认" name="CONFIRMED" />
          <el-tab-pane label="已驳回" name="REJECTED" />
        </el-tabs>
      </div>

      <!-- 对账单卡片列表 -->
      <div class="statement-cards">
        <div
          v-for="item in statementList"
          :key="item.id"
          class="statement-card"
          data-test="statement-card"
          @click="goToDetail(item.id)"
        >
          <div class="card-header">
            <span class="statement-no" data-test="statement-no">{{ item.statementNo }}</span>
            <el-tag :type="getStatusTagType(item.status)" size="small" data-test="status-tag">
              {{ getStatusLabel(item.status) }}
            </el-tag>
          </div>

          <div class="card-body">
            <div class="info-row">
              <span class="label">对账期间</span>
              <span class="value" data-test="statement-period">
                {{ item.periodStart }} ~ {{ item.periodEnd }}
              </span>
            </div>
            <div class="info-row">
              <span class="label">币种</span>
              <span class="value" data-test="statement-currency">{{ item.currency || 'CNY' }}</span>
            </div>
          </div>

          <!-- 财务金额四要素网格 -->
          <div class="financial-grid">
            <div class="grid-col">
              <span class="grid-label">期初余额</span>
              <span class="grid-value" data-test="opening-balance">¥{{ formatAmount(item.openingBalance) }}</span>
            </div>
            <div class="grid-col">
              <span class="grid-label">本期发货(借)</span>
              <span class="grid-value debit" data-test="period-debit">+¥{{ formatAmount(item.periodDebit) }}</span>
            </div>
            <div class="grid-col">
              <span class="grid-label">本期回款(贷)</span>
              <span class="grid-value credit" data-test="period-credit">-¥{{ formatAmount(item.periodCredit) }}</span>
            </div>
            <div class="grid-col">
              <span class="grid-label">期末欠款</span>
              <span class="grid-value closing" data-test="closing-balance">¥{{ formatAmount(item.closingBalance) }}</span>
            </div>
          </div>

          <div class="card-footer">
            <span class="footer-tip">点击查看对账明细与确认操作</span>
            <span class="arrow-text">详情 &gt;</span>
          </div>
        </div>

        <el-empty
          v-if="!listLoading && (!statementList || statementList.length === 0)"
          description="暂无对账单数据"
          data-test="empty-state"
        />
      </div>
    </div>
  </PortalLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PortalLayout from '@/views/portal/PortalLayout.vue'
import { usePortalAuth } from '@/views/portal/usePortalAuth'
import {
  getPortalStatementList,
  type PortalStatementRespVO
} from '@/api/portal/statement'

const router = useRouter()
const { customerName, loading, bootstrapToken } = usePortalAuth()

const activeTab = ref('')
const statementList = ref<PortalStatementRespVO[]>([])
const listLoading = ref(false)

const getStatusTagType = (status?: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  switch (status) {
    case 'SENT':
      return 'warning'
    case 'CONFIRMED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusLabel = (status?: string): string => {
  switch (status) {
    case 'SENT':
      return '待确认'
    case 'CONFIRMED':
      return '已确认'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '未知'
  }
}

const formatAmount = (num?: number | null): string => {
  if (num === undefined || num === null) return '0.00'
  return Number(num).toFixed(2)
}

const loadData = async () => {
  listLoading.value = true
  try {
    const res = await getPortalStatementList({
      status: activeTab.value || undefined
    })
    statementList.value = res || []
  } catch {
    // 拦截器已统一处理 401 与错误提示
  } finally {
    listLoading.value = false
  }
}

const handleTabChange = () => {
  loadData()
}

const goToDetail = (id: number) => {
  router.push(`/portal/statement/${id}`)
}

onMounted(async () => {
  const authOk = await bootstrapToken()
  if (authOk) {
    await loadData()
  }
})
</script>

<style scoped>
.portal-statement-list-page {
  padding: 4px 0 16px;
}

.tabs-bar {
  background: #fff;
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.statement-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.statement-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.statement-card:active {
  transform: scale(0.99);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
  margin-bottom: 10px;
}

.statement-no {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  margin-bottom: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
}

.info-row .label {
  color: #6b7280;
}

.info-row .value {
  color: #374151;
  font-weight: 500;
}

.financial-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 12px;
  background: #f8fafc;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.grid-col {
  display: flex;
  flex-direction: column;
}

.grid-label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}

.grid-value {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.grid-value.debit {
  color: #dc2626;
}

.grid-value.credit {
  color: #16a34a;
}

.grid-value.closing {
  color: #2563eb;
  font-size: 14px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #f0f0f0;
  margin-top: 8px;
  padding-top: 8px;
}

.footer-tip {
  font-size: 11px;
  color: #94a3b8;
}

.arrow-text {
  font-size: 12px;
  color: #2563eb;
  font-weight: 500;
}
</style>
