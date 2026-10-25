<template>
  <div class="portal-statement-list-page">
    <!-- 状态 Tab -->
    <el-tabs v-model="activeTab" class="portal-tabs">
      <el-tab-pane label="待确认" name="SENT" />
      <el-tab-pane label="已确认" name="CONFIRMED" />
      <el-tab-pane label="已驳回" name="REJECTED" />
    </el-tabs>

    <!-- 对账单卡片列表 -->
    <div v-loading="loading" class="statement-card-list">
      <template v-if="filteredList.length > 0">
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="statement-card"
          @click="goToDetail(item.id)"
        >
          <div class="card-header">
            <span class="statement-no">{{ item.statementNo }}</span>
            <el-tag :type="getStatementStatusTagType(item.status)" size="small" effect="light">
              {{ getStatementStatusText(item.status) }}
            </el-tag>
          </div>

          <div v-if="item.periodStart || item.periodEnd" class="card-meta">
            <span class="meta-label">对账期间:</span>
            <span class="meta-value">
              {{ item.periodStart || '' }} ~ {{ item.periodEnd || '' }}
            </span>
          </div>

          <div v-if="item.statementDate || item.createTime" class="card-meta">
            <span class="meta-label">制单时间:</span>
            <span class="meta-value">{{
              item.statementDate || formatDateTime(item.createTime)
            }}</span>
          </div>

          <div
            v-if="item.status === 'CONFIRMED' && item.confirmByName"
            class="card-meta confirm-meta"
          >
            <span class="meta-label">确认人:</span>
            <span class="meta-value">{{ item.confirmByName }}</span>
          </div>

          <div v-if="item.status === 'REJECTED' && item.rejectReason" class="card-meta reject-meta">
            <span class="meta-label">驳回原因:</span>
            <span class="meta-value">{{ item.rejectReason }}</span>
          </div>

          <div class="card-footer">
            <div class="amount-box">
              <span class="amount-label">期末应结:</span>
              <span class="amount-val">{{
                formatAmount(item.closingBalance ?? item.totalAmount, item.currency)
              }}</span>
            </div>
            <div class="view-detail-hint">
              <span>查看详情 &gt;</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else class="empty-box">
        <el-empty
          :description="
            activeTab === 'SENT'
              ? '暂无待确认对账单'
              : activeTab === 'CONFIRMED'
                ? '暂无已确认对账单'
                : '暂无已驳回对账单'
          "
          :image-size="80"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPortalStatementList, type PortalStatementRespVO } from '@/api/portal'
import {
  getStatementStatusText,
  getStatementStatusTagType,
  formatAmount,
  formatDateTime,
  filterStatementListByTab
} from '../model/portalStatement'
import { getPortalToken, handlePortalTokenGuide } from '@/utils/portalAuth'

defineOptions({ name: 'PortalStatementList' })

const route = useRoute()
const router = useRouter()

const activeTab = ref<string>('SENT')
const loading = ref<boolean>(false)
const statementList = ref<PortalStatementRespVO[]>([])

const filteredList = computed<PortalStatementRespVO[]>(() => {
  return filterStatementListByTab(statementList.value, activeTab.value)
})

const goToDetail = (id: number) => {
  router.push(`/portal/statement/${id}`)
}

const loadData = async () => {
  // 1. 优先提取 ?t=
  if (route.query?.t) {
    handlePortalTokenGuide(route.query.t as string)
  }

  // 2. 校验令牌，无有效令牌引导至失效页
  const token = getPortalToken()
  if (!token) {
    router.replace('/portal/invalid')
    return
  }

  loading.value = true
  try {
    const res = await getPortalStatementList()
    statementList.value = Array.isArray(res) ? res : []
  } catch (err) {
    console.error('获取对账单列表失败:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.portal-statement-list-page {
  padding: 12px;
  background-color: #f7f8fa;
  min-height: calc(100vh - 100px);
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

.statement-card-list {
  margin-top: 12px;
}

.statement-card {
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

.statement-card:active {
  transform: scale(0.99);
  background-color: #fafafa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.statement-no {
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

.confirm-meta .meta-value {
  color: #67c23a;
  font-weight: 500;
}

.reject-meta .meta-value {
  color: #f56c6c;
  font-weight: 500;
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

.view-detail-hint {
  font-size: 12px;
  color: #409eff;
}

.empty-box {
  margin-top: 40px;
}
</style>
