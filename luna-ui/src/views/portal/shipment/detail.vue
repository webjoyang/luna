<template>
  <PortalLayout title="发货单详情" :customer-name="customerName">
    <div v-loading="loading || detailLoading" class="portal-shipment-detail-page">
      <!-- 顶部操作条（返回） -->
      <div class="top-nav">
        <el-button link type="primary" class="back-btn" @click="goBack">
          &lt; 返回发货单列表
        </el-button>
      </div>

      <template v-if="detail">
        <!-- 单据主信息卡片 -->
        <div class="detail-card main-card">
          <div class="card-header">
            <div class="title-wrap">
              <span class="shipment-no" data-test="detail-shipment-no">{{ detail.shipmentNo }}</span>
              <el-tag :type="getSignStatusTagType(detail.signStatus)" size="small" data-test="detail-sign-tag">
                {{ getSignStatusLabel(detail.signStatus) }}
              </el-tag>
            </div>
            <span class="shipping-date" data-test="detail-shipping-date">{{ detail.shippingDate }}</span>
          </div>

          <div class="card-body">
            <div v-if="detail.logisticsCompany || detail.logisticsTrackingNo" class="info-row">
              <span class="label">承运物流</span>
              <span class="value" data-test="detail-logistics">
                {{ detail.logisticsCompany || '无承运商' }}
                {{ detail.logisticsTrackingNo ? `(${detail.logisticsTrackingNo})` : '' }}
              </span>
            </div>

            <!-- 签收信息（若已签收或拒签） -->
            <div v-if="detail.signedByName || detail.signedAt" class="sign-info-box">
              <div class="info-row">
                <span class="label">签收人</span>
                <span class="value" data-test="detail-signed-by">{{ detail.signedByName || '-' }}</span>
              </div>
              <div v-if="detail.signedAt" class="info-row">
                <span class="label">签收时间</span>
                <span class="value" data-test="detail-signed-at">{{ detail.signedAt }}</span>
              </div>
              <div v-if="detail.signRemark" class="info-row">
                <span class="label">签收备注</span>
                <span class="value" data-test="detail-sign-remark">{{ detail.signRemark }}</span>
              </div>
            </div>
          </div>

          <!-- 金额汇总（透明透出，不脱敏） -->
          <div class="amounts-summary">
            <div class="amount-card-item">
              <span class="lbl">货物金额合计</span>
              <span class="val" data-test="detail-total-amount">¥{{ formatAmount(detail.totalAmount) }}</span>
            </div>
            <div class="amount-card-item">
              <span class="lbl">运费</span>
              <span class="val sub" data-test="detail-freight-fee">¥{{ formatAmount(detail.freightFee) }}</span>
            </div>
            <div v-if="detail.totalPieces !== undefined && detail.totalPieces !== null" class="amount-card-item">
              <span class="lbl">总匹数</span>
              <span class="val sub" data-test="detail-total-pieces">{{ detail.totalPieces }} 匹</span>
            </div>
          </div>
        </div>

        <!-- 发货明细列表 -->
        <div class="detail-section">
          <div class="section-title">
            <span>发货明细</span>
            <span class="item-count">共 {{ detail.items ? detail.items.length : 0 }} 项</span>
          </div>

          <div class="items-list">
            <div
              v-for="(item, idx) in detail.items"
              :key="item.id || idx"
              class="item-card"
              data-test="item-card"
            >
              <div class="item-header">
                <span class="item-name" data-test="item-product-name">{{ item.productName || '未命名产品' }}</span>
                <span class="item-code" data-test="item-product-code">{{ item.productCode }}</span>
              </div>

              <div class="item-body">
                <div class="item-grid">
                  <div class="grid-cell">
                    <span class="label">客户色号:</span>
                    <span class="val" data-test="item-color-no">{{ item.customerColorNo || '-' }}</span>
                  </div>
                  <div class="grid-cell">
                    <span class="label">颜色名称:</span>
                    <span class="val" data-test="item-color-name">{{ item.colorName || '-' }}</span>
                  </div>
                  <div class="grid-cell">
                    <span class="label">缸号/批号:</span>
                    <span class="val" data-test="item-vat-no">{{ item.vatNo || '-' }}</span>
                  </div>
                  <div class="grid-cell">
                    <span class="label">匹数:</span>
                    <span class="val" data-test="item-pieces">{{ item.pieces ?? '-' }}</span>
                  </div>
                </div>

                <div class="item-amount-row">
                  <div class="qty-info">
                    <span class="qty-val" data-test="item-qty">{{ item.quantity }}</span>
                    <span class="qty-unit">{{ item.unit || 'KG' }}</span>
                    <span class="price-val" data-test="item-price">× ¥{{ formatAmount(item.unitPrice) }}</span>
                  </div>
                  <div class="row-total" data-test="item-amount">
                    ¥{{ formatAmount(item.amount) }}
                  </div>
                </div>

                <!-- 卷清单展开 -->
                <div v-if="item.rolls && item.rolls.length > 0" class="rolls-section">
                  <div class="rolls-header">
                    <span>卷明细（{{ item.rolls.length }} 卷）</span>
                  </div>
                  <div class="rolls-tags">
                    <div
                      v-for="r in item.rolls"
                      :key="r.id"
                      class="roll-chip"
                      data-test="roll-item"
                    >
                      <span class="roll-no">{{ r.rollNo || '卷' }}</span>
                      <span class="roll-wt">{{ r.weight }}KG</span>
                      <span v-if="r.lengthM" class="roll-len">{{ r.lengthM }}M</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 空数据状态 -->
      <div v-else-if="!detailLoading" class="empty-wrap">
        <el-empty description="未找到发货单数据或无权查看" />
      </div>
    </div>
  </PortalLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PortalLayout from '@/views/portal/PortalLayout.vue'
import { usePortalAuth } from '@/views/portal/usePortalAuth'
import {
  getPortalShipmentDetail,
  type PortalShipmentDetailRespVO
} from '@/api/portal/shipment'

const route = useRoute()
const router = useRouter()
const { customerName, loading, bootstrapToken } = usePortalAuth()

const detail = ref<PortalShipmentDetailRespVO | null>(null)
const detailLoading = ref(false)

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

const goBack = () => {
  router.push('/portal/shipments')
}

const loadDetail = async () => {
  const id = route.params.id as string | undefined
  const no = route.query.no as string | undefined
  if (!id && !no) {
    return
  }

  detailLoading.value = true
  try {
    const res = await getPortalShipmentDetail({ id, no })
    if (res) {
      detail.value = res
    }
  } catch {
    // 拦截器已统一处理 401/403 跳转 invalid
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  const authOk = await bootstrapToken()
  if (authOk) {
    await loadDetail()
  }
})
</script>

<style scoped>
.portal-shipment-detail-page {
  padding: 4px 0 20px;
}

.top-nav {
  margin-bottom: 8px;
}

.back-btn {
  font-size: 13px;
  padding: 0;
}

.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 14px;
}

.card-header {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
  margin-bottom: 10px;
}

.title-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.shipment-no {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.shipping-date {
  font-size: 12px;
  color: #6b7280;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
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
  color: #1f2937;
  font-weight: 500;
}

.sign-info-box {
  background: #f9fafb;
  border-radius: 6px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.amounts-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 10px;
  background: #fef2f2;
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 12px;
}

.amount-card-item {
  display: flex;
  flex-direction: column;
}

.amount-card-item .lbl {
  font-size: 11px;
  color: #991b1b;
}

.amount-card-item .val {
  font-size: 16px;
  font-weight: 700;
  color: #b91c1c;
}

.amount-card-item .val.sub {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.detail-section {
  margin-top: 14px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
  padding: 0 4px;
}

.item-count {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 400;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 6px;
  margin-bottom: 8px;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.item-code {
  font-size: 12px;
  color: #6b7280;
}

.item-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  font-size: 12px;
  margin-bottom: 8px;
}

.grid-cell {
  display: flex;
  gap: 4px;
}

.grid-cell .label {
  color: #9ca3af;
}

.grid-cell .val {
  color: #374151;
  font-weight: 500;
}

.item-amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f9fafb;
  border-radius: 4px;
  padding: 6px 8px;
  margin-bottom: 8px;
}

.qty-info {
  font-size: 13px;
  color: #4b5563;
}

.qty-val {
  font-weight: 600;
  color: #111827;
}

.qty-unit {
  margin-right: 6px;
}

.price-val {
  color: #6b7280;
  font-size: 12px;
}

.row-total {
  font-size: 14px;
  font-weight: 600;
  color: #e11d48;
}

.rolls-section {
  border-top: 1px dashed #e5e7eb;
  padding-top: 8px;
}

.rolls-header {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 6px;
}

.rolls-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.roll-chip {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 11px;
  display: flex;
  gap: 4px;
  color: #1e40af;
}

.roll-no {
  font-weight: 500;
}

.roll-wt {
  font-weight: 600;
}

.roll-len {
  color: #60a5fa;
}

.empty-wrap {
  padding: 40px 0;
}
</style>
