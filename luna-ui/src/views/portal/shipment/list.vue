<template>
  <div class="portal-shipment-list-page">
    <el-tabs v-model="activeTab" class="portal-tabs">
      <el-tab-pane label="待签收" name="PENDING" />
      <el-tab-pane label="已签收" name="SIGNED" />
      <el-tab-pane label="已拒收" name="REFUSED" />
      <el-tab-pane label="全部" name="ALL" />
    </el-tabs>

    <div class="placeholder-content">
      <el-empty description="发货单列表建设中..." :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineOptions({ name: 'PortalShipmentList' })

const activeTab = ref<string>('PENDING')
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
}

.placeholder-content {
  margin-top: 40px;
}
</style>
