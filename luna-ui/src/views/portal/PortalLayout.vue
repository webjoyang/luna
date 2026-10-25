<template>
  <div class="portal-layout-wrapper">
    <div class="portal-container">
      <!-- 顶部标题栏 -->
      <header class="portal-header">
        <div class="portal-title">客户门户</div>
        <div v-if="customerName" class="portal-customer-name" :title="customerName">
          {{ customerName }}
        </div>
      </header>

      <!-- 中间内容区 -->
      <main class="portal-main">
        <router-view />
      </main>

      <!-- 底部导航栏 -->
      <nav class="portal-bottom-nav">
        <div
          class="portal-tab-item"
          :class="{ active: currentTab === 'shipment' }"
          @click="switchTab('shipment')"
        >
          <el-icon :size="20"><Van /></el-icon>
          <span class="tab-label">发货单</span>
        </div>
        <div
          class="portal-tab-item"
          :class="{ active: currentTab === 'statement' }"
          @click="switchTab('statement')"
        >
          <el-icon :size="20"><Tickets /></el-icon>
          <span class="tab-label">对账单</span>
        </div>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Van, Tickets } from '@element-plus/icons-vue'
import { getPortalCustomerName } from '@/utils/portalAuth'

defineOptions({ name: 'PortalLayout' })

const route = useRoute()
const router = useRouter()

const customerName = ref<string>('')

onMounted(() => {
  customerName.value = getPortalCustomerName()
})

const currentTab = computed<'shipment' | 'statement'>(() => {
  const path = route.path
  if (path.startsWith('/portal/statement')) {
    return 'statement'
  }
  return 'shipment'
})

const switchTab = (tab: 'shipment' | 'statement') => {
  if (tab === 'shipment') {
    if (!route.path.startsWith('/portal/shipment')) {
      router.push('/portal/shipments')
    }
  } else {
    if (!route.path.startsWith('/portal/statement')) {
      router.push('/portal/statements')
    }
  }
}
</script>

<style scoped>
.portal-layout-wrapper {
  min-height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
}

.portal-container {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 0 12px rgba(0, 0, 0, 0.05);
}

.portal-header {
  height: 48px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 10;
}

.portal-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.portal-customer-name {
  font-size: 13px;
  color: #409eff;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.portal-main {
  flex: 1;
  padding-bottom: 56px;
  overflow-y: auto;
}

.portal-bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 52px;
  background: #ffffff;
  display: flex;
  border-top: 1px solid #ebeef5;
  z-index: 20;
}

.portal-tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #909399;
  font-size: 11px;
  gap: 2px;
  user-select: none;
  transition: color 0.2s;
}

.portal-tab-item.active {
  color: #409eff;
  font-weight: 500;
}

.tab-label {
  font-size: 11px;
  line-height: 1;
}
</style>
