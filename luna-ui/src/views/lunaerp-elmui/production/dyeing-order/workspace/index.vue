<template>
  <ContentWrap>
    <div class="workspace-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基础信息" name="basic">
        <BasicInfoTab
          :order-id="orderId"
          :is-view-mode="isViewMode"
          @success="handleSuccess"
          @cancel="handleBack"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import BasicInfoTab from './BasicInfoTab.vue'

defineOptions({ name: 'LunaerpElmuiProductionDyeingOrderWorkspace' })

const router = useRouter()
const route = useRoute()

const activeTab = ref('basic')
const orderId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建染色单'
  return isViewMode.value ? '查看染色单' : '编辑染色单'
})

const handleBack = () => {
  router.push('/production/dyeing-order')
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
