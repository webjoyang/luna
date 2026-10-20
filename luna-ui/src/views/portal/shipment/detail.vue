<template>
  <div v-loading="loading" class="portal-shipment-detail-page">
    <!-- 头部返回栏 -->
    <div class="detail-nav-header">
      <el-button link type="primary" :icon="ArrowLeft" @click="handleBack"> 返回列表 </el-button>
      <span class="nav-title">发货单详情</span>
      <div class="nav-placeholder"></div>
    </div>

    <template v-if="detail">
      <!-- 汇总主卡片 -->
      <div class="summary-card">
        <div class="card-row top-row">
          <span class="shipment-no">{{ detail.shipmentNo }}</span>
          <el-tag :type="getSignStatusTagType(detail.signStatus)" size="small" effect="light">
            {{ getSignStatusText(detail.signStatus) }}
          </el-tag>
        </div>

        <div class="card-row info-row">
          <span class="info-label">发货日期</span>
          <span class="info-val">{{ detail.shippingDate || '-' }}</span>
        </div>

        <div v-if="detail.carrier || detail.trackingNumber" class="card-row info-row">
          <span class="info-label">物流信息</span>
          <span class="info-val">
            {{ detail.carrier || '' }}
            {{ detail.trackingNumber ? `(${detail.trackingNumber})` : '' }}
          </span>
        </div>

        <div v-if="detail.receiveAddress" class="card-row info-row">
          <span class="info-label">收货地址</span>
          <span class="info-val">{{ detail.receiveAddress }}</span>
        </div>

        <div v-if="detail.totalPieces != null" class="card-row info-row">
          <span class="info-label">总卷/匹数</span>
          <span class="info-val">{{ detail.totalPieces }} 匹</span>
        </div>

        <!-- 签收留痕信息（仅在非 PENDING 时展示） -->
        <template v-if="detail.signStatus && detail.signStatus !== 'PENDING'">
          <div class="sign-history-box">
            <div class="card-row info-row">
              <span class="info-label">签收人</span>
              <span class="info-val">{{ detail.signedByName || '-' }}</span>
            </div>
            <div v-if="detail.signedAt" class="card-row info-row">
              <span class="info-label">签收时间</span>
              <span class="info-val">{{ formatDateTime(detail.signedAt) }}</span>
            </div>
            <div v-if="detail.signRemark" class="card-row info-row">
              <span class="info-label">签收备注</span>
              <span class="info-val">{{ detail.signRemark }}</span>
            </div>
          </div>
        </template>

        <!-- 金额明细 -->
        <div class="amount-summary-box">
          <div class="amount-row">
            <span class="amt-lbl">货物金额:</span>
            <span class="amt-val">{{ formatAmount(detail.totalAmount) }}</span>
          </div>
          <div class="amount-row">
            <span class="amt-lbl">运费:</span>
            <span class="amt-val">{{ formatAmount(detail.freightFee) }}</span>
          </div>
          <div class="amount-row total-row">
            <span class="amt-lbl">总计金额:</span>
            <span class="amt-val highlight">{{ formatAmount(grandTotal) }}</span>
          </div>
        </div>
      </div>

      <!-- 逐行发货明细 -->
      <div class="items-card">
        <div class="items-card-title">发货明细 ({{ detail.items?.length || 0 }}项)</div>
        <div v-if="detail.items && detail.items.length > 0" class="items-list">
          <div v-for="(item, idx) in detail.items" :key="item.id || idx" class="item-row">
            <div class="item-row-header">
              <span class="item-product-name">{{ item.productName || '未命名产品' }}</span>
              <span class="item-amount">{{ formatAmount(item.amount) }}</span>
            </div>
            <div class="item-meta-grid">
              <div v-if="item.productCode" class="item-sub">
                <span class="sub-label">编码:</span> {{ item.productCode }}
              </div>
              <div v-if="item.customerColorNo || item.colorName" class="item-sub">
                <span class="sub-label">色号:</span>
                {{ [item.customerColorNo, item.colorName].filter(Boolean).join(' / ') }}
              </div>
              <div v-if="item.vatNo" class="item-sub">
                <span class="sub-label">缸号:</span> {{ item.vatNo }}
              </div>
              <div class="item-sub">
                <span class="sub-label">数量:</span>
                {{ item.quantity != null ? item.quantity : 0 }} {{ item.unit || '件' }}
                {{ item.pieces != null ? `(${item.pieces} 匹)` : '' }}
              </div>
              <div v-if="item.unitPrice != null" class="item-sub">
                <span class="sub-label">单价:</span> {{ formatAmount(item.unitPrice) }}
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-items">暂无明细数据</div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getPortalShipment, type PortalShipmentDetailRespVO } from '@/api/portal'
import {
  getSignStatusText,
  getSignStatusTagType,
  formatAmount,
  resolveShipmentDetailParams
} from '../model/portalShipment'
import { getPortalToken, handlePortalTokenGuide, setPortalCustomerName } from '@/utils/portalAuth'

defineOptions({ name: 'PortalShipmentDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref<boolean>(false)
const detail = ref<PortalShipmentDetailRespVO | null>(null)

const grandTotal = computed<number>(() => {
  if (!detail.value) return 0
  const goods = detail.value.totalAmount != null ? Number(detail.value.totalAmount) : 0
  const freight = detail.value.freightFee != null ? Number(detail.value.freightFee) : 0
  return goods + freight
})

const formatDateTime = (val?: string | Date) => {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const handleBack = () => {
  router.push('/portal/shipments')
}

const loadDetail = async () => {
  // 1. 优先提取 ?t=
  if (route.query?.t) {
    handlePortalTokenGuide(route.query.t as string)
  }

  // 2. 无会话直达（无 token）→ 引导至 invalid 页
  const token = getPortalToken()
  if (!token) {
    router.replace('/portal/invalid')
    return
  }

  // 3. 解析双形态入参
  const { id, no, isValid } = resolveShipmentDetailParams(
    { id: route.params.id as string },
    { no: route.query.no as string }
  )

  if (!isValid) {
    router.replace('/portal/invalid')
    return
  }

  loading.value = true
  try {
    const res = await getPortalShipment({ id, no })
    detail.value = res
    if (res?.customerName) {
      setPortalCustomerName(res.customerName)
    }
  } catch (err) {
    console.error('加载发货单详情失败:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.portal-shipment-detail-page {
  padding: 12px;
  padding-bottom: 30px;
}

.detail-nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.nav-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.nav-placeholder {
  width: 50px;
}

.summary-card,
.items-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f2f5;
}

.shipment-no {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-row {
  display: flex;
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 6px;
}

.info-label {
  width: 72px;
  color: #909399;
  flex-shrink: 0;
}

.info-val {
  color: #303133;
  word-break: break-all;
}

.sign-history-box {
  background-color: #f8f9fa;
  border-radius: 6px;
  padding: 8px 10px;
  margin: 10px 0;
  border-left: 3px solid #67c23a;
}

.amount-summary-box {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin-bottom: 4px;
}

.amt-lbl {
  color: #909399;
}

.amt-val {
  color: #606266;
}

.total-row {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #f2f6fc;
}

.highlight {
  font-size: 17px;
  font-weight: 700;
  color: #f56c6c;
}

.items-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f2f5;
}

.item-row {
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.item-row:last-child {
  border-bottom: none;
}

.item-row-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}

.item-product-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.item-amount {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}

.item-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 8px;
  font-size: 12px;
  color: #606266;
}

.sub-label {
  color: #909399;
}

.empty-items {
  text-align: center;
  color: #909399;
  padding: 20px 0;
  font-size: 13px;
}
</style>
