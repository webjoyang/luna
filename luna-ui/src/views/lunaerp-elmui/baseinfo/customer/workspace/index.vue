<template>
  <ContentWrap>
    <div class="workspace-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">
            {{ isCreateMode ? '新建客户' : '客户工作台' }}
          </span>
          <span v-if="!isCreateMode && formData?.company" class="header-sub">
            · {{ formData.company }}
          </span>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基础档案" name="basic">
        <BasicProfileTab :customer-id="customerId" @saved="onSaved" />
      </el-tab-pane>
      <el-tab-pane label="联系人" name="contacts">
        <ContactsTab :customer-id="customerId" />
      </el-tab-pane>
      <el-tab-pane label="合同" name="contracts">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="寄样" name="samples">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="画像分析" name="profile">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="位置分布" name="location">
        <PlaceholderTab />
      </el-tab-pane>
      <el-tab-pane label="拜访记录" name="visits">
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
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpElmuiBaseinfoCustomerWorkspace' })

const route = useRoute()
const router = useRouter()

const activeTab = ref('basic')
const formData = ref<CustomerVO | null>(null)

const customerId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !customerId.value)

const handleBack = () => {
  router.push('/baseinfo/customer')
}

const onSaved = (customer: CustomerVO) => {
  formData.value = customer
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
