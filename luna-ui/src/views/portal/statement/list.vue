<template>
  <div class="portal-statement-list-page">
    <el-tabs v-model="activeTab" class="portal-tabs">
      <el-tab-pane label="待确认" name="SENT" />
      <el-tab-pane label="已确认" name="CONFIRMED" />
      <el-tab-pane label="已驳回" name="REJECTED" />
    </el-tabs>

    <div class="placeholder-content">
      <el-empty description="对账单列表建设中..." :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineOptions({ name: 'PortalStatementList' })

const activeTab = ref<string>('SENT')
</script>

<style scoped>
.portal-statement-list-page {
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
