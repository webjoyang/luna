<template>
  <ContentWrap>
    <div class="edit-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <el-empty description="胚布单表单开发中，新建/编辑/查看随 02、03 票开放" :image-size="120" />
  </ContentWrap>
</template>

<script lang="ts" setup>
defineOptions({ name: 'LunaerpElmuiProductionGreigeOrderEdit' })

const route = useRoute()
const router = useRouter()

const orderId = computed(() => (route.params.id as string) || null)
const isViewMode = computed(() => route.query.mode === 'view')

const pageTitle = computed(() => {
  if (isViewMode.value) return '查看胚布单'
  return orderId.value ? '编辑胚布单' : '新建胚布单'
})

const handleBack = () => {
  router.push('/production/greige-order')
}
</script>

<style lang="scss" scoped>
.edit-header {
  margin-bottom: 16px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
