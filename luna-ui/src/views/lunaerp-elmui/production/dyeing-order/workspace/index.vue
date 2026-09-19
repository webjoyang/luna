<template>
  <ContentWrap>
    <div class="workspace-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
        <template #extra>
          <el-button
            v-if="isViewMode && currentStatus === 'DRAFT'"
            type="primary"
            @click="handleSwitchToEdit"
          >
            编辑
          </el-button>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基础信息" name="basic">
        <BasicInfoTab
          :order-id="orderId"
          :is-view-mode="isViewMode"
          @loaded="handleOrderLoaded"
          @success="handleSuccess"
          @cancel="handleBack"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import BasicInfoTab from './BasicInfoTab.vue'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'

defineOptions({ name: 'LunaerpElmuiProductionDyeingOrderWorkspace' })

const router = useRouter()
const route = useRoute()

const activeTab = ref('basic')
const orderId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')
const currentStatus = ref<string>()

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建染色单'
  if (isViewMode.value || (currentStatus.value && currentStatus.value !== 'DRAFT')) {
    return '查看染色单'
  }
  return '编辑染色单'
})

const handleBack = () => {
  router.push('/production/dyeing-order')
}

const handleSwitchToEdit = () => {
  router.replace({
    path: `/production/dyeing-order/workspace/${orderId.value}`
  })
}

const handleOrderLoaded = (order: DyeingOrderVO) => {
  currentStatus.value = order.status
}

const handleSuccess = (_id: number) => {
  router.push('/production/dyeing-order')
}
</script>

<style lang="scss" scoped>
.workspace-header {
  margin-bottom: 20px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.workspace-tabs {
  :deep(.el-tabs__content) {
    min-height: 320px;
  }
}
</style>
