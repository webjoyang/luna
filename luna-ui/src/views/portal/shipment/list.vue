<template>
  <div class="portal-shipment-list-page">
    <!-- 状态 Tab -->
    <el-tabs v-model="activeTab" class="portal-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="待签收" name="PENDING" />
      <el-tab-pane label="已签收" name="SIGNED" />
      <el-tab-pane label="已拒收" name="REFUSED" />
      <el-tab-pane label="全部" name="ALL" />
    </el-tabs>

    <!-- 发货单卡片列表 -->
    <div v-loading="loading" class="shipment-card-list">
      <template v-if="shipmentList.length > 0">
        <div
          v-for="item in shipmentList"
          :key="item.id"
          class="shipment-card"
          @click="goToDetail(item.id)"
        >
          <div class="card-header">
            <span class="shipment-no">{{ item.shipmentNo }}</span>
            <el-tag :type="getSignStatusTagType(item.signStatus)" size="small" effect="light">
              {{ getSignStatusText(item.signStatus) }}
            </el-tag>
          </div>

          <div class="card-meta">
            <span class="meta-label">发货日期:</span>
            <span class="meta-value">{{ item.shipmentDate || item.shippingDate || '-' }}</span>
          </div>

          <div
            v-if="item.logisticsCompany || item.carrier || item.logisticsNo || item.trackingNumber"
            class="card-meta"
          >
            <span class="meta-label">物流信息:</span>
            <span class="meta-value">
              {{ item.logisticsCompany || item.carrier || '' }}
              {{
                item.logisticsNo || item.trackingNumber
                  ? `(${item.logisticsNo || item.trackingNumber})`
                  : ''
              }}
            </span>
          </div>

          <div class="card-footer">
            <div class="amount-box">
              <span class="amount-label">金额:</span>
              <span class="amount-val">{{ formatAmount(item.totalAmount) }}</span>
              <span
                v-if="item.freightAmount != null || item.freightFee != null"
                class="freight-val"
              >
                (含运费 {{ formatAmount(item.freightAmount ?? item.freightFee) }})
              </span>
            </div>
            <div v-if="item.totalPieces != null" class="pieces-box">
              <span>{{ item.totalPieces }} 匹</span>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="portal-pagination">
          <el-pagination
            v-model:current-page="queryParams.pageNo"
            v-model:page-size="queryParams.pageSize"
            :total="total"
            layout="prev, pager, next"
            small
            @current-change="loadData"
          />
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-wrap">
        <el-empty description="暂无发货单" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPortalShipmentPage, type PortalShipmentRespVO } from '@/api/portal'
import { getSignStatusText, getSignStatusTagType, formatAmount } from '../model/portalShipment'
import { getPortalToken, handlePortalTokenGuide, setPortalCustomerName } from '@/utils/portalAuth'

defineOptions({ name: 'PortalShipmentList' })

const route = useRoute()
const router = useRouter()

const activeTab = ref<string>('PENDING')
const loading = ref<boolean>(false)
const shipmentList = ref<PortalShipmentRespVO[]>([])
const total = ref<number>(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10
})

const handleTabChange = () => {
  queryParams.pageNo = 1
  loadData()
}

const loadData = async () => {
  // 检查会话 token
  const token = getPortalToken()
  if (!token) {
    router.replace('/portal/invalid')
    return
  }

  loading.value = true
  try {
    const res = await getPortalShipmentPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      signStatus: activeTab.value === 'ALL' ? undefined : activeTab.value
    })
    shipmentList.value = res.list || []
    total.value = res.total || 0

    // 若有客户名称，同步更新到 sessionStorage
    if (shipmentList.value.length > 0 && shipmentList.value[0].customerName) {
      setPortalCustomerName(shipmentList.value[0].customerName)
    }
  } catch (err) {
    console.error('加载发货单列表失败:', err)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id: number) => {
  if (id) {
    router.push(`/portal/shipment/${id}`)
  }
}

onMounted(() => {
  // 优先处理 URL 中携带的 ?t= 引导令牌
  if (route.query?.t) {
    handlePortalTokenGuide(route.query.t as string)
  }
  loadData()
})
</script>

<style scoped>
.portal-shipment-list-page {
  padding: 12px;
}

.portal-tabs :deep(.el-tabs__nav) {
  width: 100%;
  display: flex;
}

.portal-tabs :deep(.el-tabs__item) {
  flex: 1;
  text-align: center;
  padding: 0;
  font-size: 14px;
}

.shipment-card-list {
  margin-top: 12px;
}

.shipment-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}

.shipment-card:active {
  transform: scale(0.99);
  background-color: #fafafa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.shipment-no {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-meta {
  display: flex;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  line-height: 1.4;
}

.meta-label {
  color: #909399;
  margin-right: 6px;
  white-space: nowrap;
}

.meta-value {
  color: #303133;
  word-break: break-all;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}

.amount-box {
  display: flex;
  align-items: baseline;
}

.amount-label {
  font-size: 12px;
  color: #909399;
  margin-right: 4px;
}

.amount-val {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.freight-val {
  font-size: 11px;
  color: #909399;
  margin-left: 6px;
}

.pieces-box {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.portal-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  margin-bottom: 24px;
}

.empty-wrap {
  margin-top: 40px;
}
</style>
