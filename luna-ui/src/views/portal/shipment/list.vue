<template>
  <PortalLayout title="发货单" :customer-name="customerName">
    <div v-loading="loading" class="portal-shipment-list-page">
      <el-card shadow="never" class="placeholder-card">
        <template #header>
          <div class="card-header">
            <span>发货单列表</span>
            <el-tag size="small" type="success">骨架就绪</el-tag>
          </div>
        </template>
        <el-empty description="暂无发货单数据（业务功能将在 Issue 03 完善）" />
      </el-card>
    </div>
  </PortalLayout>
</template>

<script setup lang="ts">
import PortalLayout from '@/views/portal/PortalLayout.vue'
import { usePortalAuth } from '@/views/portal/usePortalAuth'

const { customerName, loading } = usePortalAuth()
</script>

<style scoped>
.portal-shipment-list-page {
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
