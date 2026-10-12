<template>
  <PortalLayout title="对账单" :customer-name="customerName">
    <div v-loading="loading" class="portal-statement-list-page">
      <el-card shadow="never" class="placeholder-card">
        <template #header>
          <div class="card-header">
            <span>对账单列表</span>
            <el-tag size="small" type="success">骨架就绪</el-tag>
          </div>
        </template>
        <el-empty description="暂无对账单数据（业务功能将在 Issue 05 完善）" />
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
.portal-statement-list-page {
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
