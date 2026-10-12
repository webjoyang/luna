<template>
  <div class="portal-layout-container">
    <!-- 顶部导航栏 -->
    <header class="portal-header">
      <div class="portal-title">
        <span class="main-title">{{ title || '客户门户' }}</span>
      </div>
      <div v-if="customerName" class="customer-info">
        <el-tag size="small" type="primary" effect="plain" class="customer-tag">
          {{ customerName }}
        </el-tag>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <main class="portal-content">
      <slot />
    </main>

    <!-- 底部导航栏 -->
    <nav v-if="!hideTabBar" class="portal-tabbar">
      <div
        class="tab-item"
        :class="{ active: isShipmentActive }"
        @click="navigateTo('/portal/shipments')"
      >
        <el-icon :size="20"><Document /></el-icon>
        <span class="tab-label">发货单</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: isStatementActive }"
        @click="navigateTo('/portal/statements')"
      >
        <el-icon :size="20"><Tickets /></el-icon>
        <span class="tab-label">对账单</span>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Tickets } from '@element-plus/icons-vue'

const props = defineProps<{
  title?: string
  customerName?: string
  hideTabBar?: boolean
}>()

const route = useRoute()
const router = useRouter()

const isShipmentActive = computed(() => {
  return route.path.startsWith('/portal/shipment')
})

const isStatementActive = computed(() => {
  return route.path.startsWith('/portal/statement')
})

const navigateTo = (path: string) => {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.portal-layout-container {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 0 16px rgba(0, 0, 0, 0.05);
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: 48px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
}

.portal-title .main-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.customer-info .customer-tag {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portal-content {
  flex: 1;
  padding: 12px 12px 64px 12px;
  overflow-y: auto;
}

.portal-tabbar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 52px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 100;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  cursor: pointer;
  height: 100%;
  transition: color 0.2s ease;
}

.tab-item:hover,
.tab-item.active {
  color: #409eff;
}

.tab-label {
  font-size: 11px;
  margin-top: 2px;
}
</style>
