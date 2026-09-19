<template>
  <ContentWrap>
    <div class="workspace-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>
    <el-empty description="染色单工作台（开发中）" :image-size="120" />
  </ContentWrap>
</template>

<script lang="ts" setup>
defineOptions({ name: 'LunaerpElmuiProductionDyeingOrderWorkspace' })

const router = useRouter()
const route = useRoute()

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
</script>

<style lang="scss" scoped>
.workspace-header {
  margin-bottom: 20px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
