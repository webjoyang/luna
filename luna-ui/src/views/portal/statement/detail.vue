<template>
  <div class="portal-statement-detail-page">
    <div class="detail-header">
      <el-button link type="primary" @click="$router.back()">返回列表</el-button>
    </div>
    <div class="placeholder-content">
      <el-empty description="对账单详情建设中..." :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PortalStatementDetail' })
</script>

<style scoped>
.portal-statement-detail-page {
  padding: 12px;
}

.detail-header {
  margin-bottom: 12px;
}

.placeholder-content {
  margin-top: 40px;
}
</style>
