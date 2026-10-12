<template>
  <PortalLayout title="发货单详情" :customer-name="customerName">
    <div v-loading="loading" class="portal-shipment-detail-page">
      <el-card shadow="never" class="placeholder-card">
        <template #header>
          <div class="card-header">
            <span>发货单详情</span>
            <el-tag size="small" type="info">
              {{ shipmentIdentifier }}
            </el-tag>
          </div>
        </template>
        <el-empty description="发货单详情内容（业务功能将在 Issue 03 完善）" />
      </el-card>
    </div>
  </PortalLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PortalLayout from '@/views/portal/PortalLayout.vue'
import { usePortalAuth } from '@/views/portal/usePortalAuth'

const route = useRoute()
const { customerName, loading } = usePortalAuth()

const shipmentIdentifier = computed(() => {
  const id = route.params.id as string
  const no = route.query.no as string
  if (id) return `ID: ${id}`
  if (no) return `单号: ${no}`
  return '未指定'
})
</script>

<style scoped>
.portal-shipment-detail-page {
  padding-top: 4px;
}

.placeholder-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  font-size: 14px;
}
</style>
