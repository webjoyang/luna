<template>
  <ContentWrap>
    <div class="workspace-header mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="font-bold text-16px">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <el-empty description="检验单工作台建设中（将在后续 Issue 落地）" :image-size="120">
      <template #extra>
        <el-button type="primary" @click="handleBack">返回检验单列表</el-button>
      </template>
    </el-empty>
  </ContentWrap>
</template>

<script lang="ts" setup>
defineOptions({ name: 'LunaerpElmuiProductionInspectionOrderWorkspace' })

const route = useRoute()
const router = useRouter()

const orderId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建检验单'
  return isViewMode.value ? '查看检验单' : '编辑检验单'
})

const handleBack = () => {
  router.push('/production/inspection-order')
}
</script>

<style lang="scss" scoped>
.workspace-header {
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 12px;
}
</style>
