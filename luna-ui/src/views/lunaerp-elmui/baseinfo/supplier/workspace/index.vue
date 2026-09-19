<template>
  <ContentWrap>
    <div class="workspace-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">
            {{ isCreateMode ? '新建供应商' : '供应商工作台' }}
          </span>
          <span v-if="!isCreateMode && formData?.companyName" class="header-sub">
            · {{ formData.companyName }}
          </span>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基础档案" name="basic">
        <BasicProfileTab :supplier-id="supplierId" @saved="onSaved" />
      </el-tab-pane>
      <el-tab-pane label="联系人" name="contacts">
        <ContactsTab :supplier-id="supplierId" />
      </el-tab-pane>
      <el-tab-pane label="入库记录" name="inbound">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="付款记录" name="payment">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="关联品种" name="varieties">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="位置分布" name="location">
        <PlaceholderTab />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { useRoute } from 'vue-router'
import BasicProfileTab from './BasicProfileTab.vue'
import ContactsTab from './ContactsTab.vue'
import PlaceholderTab from './PlaceholderTab.vue'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiBaseinfoSupplierWorkspace' })

const route = useRoute()
const router = useRouter()

const activeTab = ref('basic')
const formData = ref<SupplierVO | null>(null)

const supplierId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !supplierId.value)

const handleBack = () => {
  router.push('/baseinfo/supplier')
}

const onSaved = (supplier: SupplierVO) => {
  formData.value = supplier
}
</script>

<style lang="scss" scoped>
.workspace-header {
  margin-bottom: 12px;

  .header-title {
    font-size: 16px;
    font-weight: 600;
  }

  .header-sub {
    margin-left: 6px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.workspace-tabs {
  :deep(.el-tabs__content) {
    min-height: 320px;
  }
}
</style>
