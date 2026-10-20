<template>
  <div class="portal-shipment-detail-page">
    <div class="detail-header">
      <el-button link type="primary" @click="$router.back()">返回列表</el-button>
    </div>
    <div class="placeholder-content">
      <el-empty description="发货单详情建设中..." :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PortalShipmentDetail' })
</script>

<style scoped>
.portal-shipment-detail-page {
  padding: 12px;
}

.detail-header {
  margin-bottom: 12px;
}

.placeholder-content {
  margin-top: 40px;
}
</style>
