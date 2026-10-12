<template>
  <PortalLayout title="发货单" :customer-name="customerName">
    <div v-loading="loading || listLoading" class="portal-shipment-list-page">
      <!-- 状态筛选 Tabs -->
      <div class="tabs-bar">
        <el-tabs v-model="activeTab" class="portal-tabs" @tab-change="handleTabChange">
          <el-tab-pane label="全部" name="" />
          <el-tab-pane label="待签收" name="PENDING" />
          <el-tab-pane label="已签收" name="SIGNED" />
          <el-tab-pane label="已拒收" name="REFUSED" />
        </el-tabs>
      </div>

      <!-- 发货单卡片列表 -->
      <div class="shipment-cards">
        <div
          v-for="item in shipmentList"
          :key="item.id"
          class="shipment-card"
          data-test="shipment-card"
          @click="goToDetail(item.id)"
        >
          <div class="card-header">
            <span class="shipment-no" data-test="shipment-no">{{ item.shipmentNo }}</span>
            <el-tag :type="getSignStatusTagType(item.signStatus)" size="small" data-test="sign-tag">
              {{ getSignStatusLabel(item.signStatus) }}
            </el-tag>
          </div>

          <div class="card-body">
            <div class="info-row">
              <span class="label">发货日期</span>
              <span class="value">{{ item.shippingDate || '-' }}</span>
            </div>
            <div v-if="item.logisticsCompany || item.logisticsTrackingNo" class="info-row">
              <span class="label">物流信息</span>
              <span class="value">
                {{ item.logisticsCompany || '' }}
                {{ item.logisticsTrackingNo ? `(${item.logisticsTrackingNo})` : '' }}
              </span>
            </div>
          </div>

          <div class="card-footer">
            <div class="amount-group">
              <div class="amount-item">
                <span class="amount-label">货物金额</span>
                <span class="amount-value" data-test="total-amount">¥{{ formatAmount(item.totalAmount) }}</span>
              </div>
              <div class="amount-item freight">
                <span class="amount-label">运费</span>
                <span class="amount-value" data-test="freight-fee">¥{{ formatAmount(item.freightFee) }}</span>
              </div>
            </div>
            <div class="arrow">
              <span class="arrow-text">详情 &gt;</span>
            </div>
          </div>
        </div>

        <el-empty
          v-if="!listLoading && (!shipmentList || shipmentList.length === 0)"
          description="暂无发货单数据"
          data-test="empty-state"
        />
      </div>

      <!-- 分页组件 -->
      <div v-if="total > pageSize" class="pagination-container">
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          small
          @current-change="handlePageChange"
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
  getPortalShipmentPage,
  type PortalShipmentPageItemRespVO
} from '@/api/portal/shipment'

const router = useRouter()
const { customerName, loading, bootstrapToken } = usePortalAuth()

const activeTab = ref('')
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const shipmentList = ref<PortalShipmentPageItemRespVO[]>([])
const listLoading = ref(false)

const getSignStatusTagType = (status?: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'SIGNED':
      return 'success'
    case 'REFUSED':
      return 'danger'
    default:
      return 'info'
  }
}

const getSignStatusLabel = (status?: string): string => {
  switch (status) {
    case 'PENDING':
      return '待签收'
    case 'SIGNED':
      return '已签收'
    case 'REFUSED':
      return '已拒收'
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
    const res = await getPortalShipmentPage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      signStatus: activeTab.value || undefined
    })
    if (res) {
      shipmentList.value = res.list || []
      total.value = res.total || 0
    }
  } catch {
    // 拦截器已统一处理提示与 401 跳转
  } finally {
    listLoading.value = false
  }
}

const handleTabChange = () => {
  pageNo.value = 1
  loadData()
}

const handlePageChange = (newPage: number) => {
  pageNo.value = newPage
  loadData()
}

const goToDetail = (id: number) => {
  router.push(`/portal/shipment/${id}`)
}

onMounted(async () => {
  const authOk = await bootstrapToken()
  if (authOk) {
    await loadData()
  }
})
</script>

<style scoped>
.portal-shipment-list-page {
  padding: 4px 0 16px;
}

.tabs-bar {
  background: #fff;
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.shipment-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shipment-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.shipment-card:active {
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

.shipment-no {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
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

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #f0f0f0;
  margin-top: 10px;
  padding-top: 10px;
}

.amount-group {
  display: flex;
  gap: 16px;
}

.amount-item {
  display: flex;
  flex-direction: column;
}

.amount-label {
  font-size: 11px;
  color: #9ca3af;
}

.amount-value {
  font-size: 15px;
  font-weight: 600;
  color: #e11d48;
}

.amount-item.freight .amount-value {
  color: #4b5563;
  font-size: 13px;
}

.arrow {
  font-size: 12px;
  color: #2563eb;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
