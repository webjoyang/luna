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
          <el-button
            v-if="currentStatus === 'COMPLETED'"
            type="primary"
            :loading="createInspectionLoading"
            @click="handleCreateFinishedInspection"
            v-hasPermi="['lunaerp:production:release-production:release']"
          >
            生成成品检验单
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
      <el-tab-pane v-if="!isCreateMode" label="进度反馈" name="progress" lazy>
        <ProgressFeedbackTab
          :order-id="orderId"
          :order-status="currentStatus"
        />
      </el-tab-pane>
      <el-tab-pane v-if="!isCreateMode" label="发染记录" name="dispatch" lazy>
        <DispatchRecordTab
          :order-id="orderId"
          :contract-no="currentContractNo"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import BasicInfoTab from './BasicInfoTab.vue'
import ProgressFeedbackTab from './ProgressFeedbackTab.vue'
import DispatchRecordTab from './DispatchRecordTab.vue'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import * as ReleaseProductionApi from '@/api/lunaerp-elmui/production/release-production'

defineOptions({ name: 'LunaerpElmuiProductionDyeingOrderWorkspace' })

const router = useRouter()
const route = useRoute()
const message = useMessage()

const activeTab = ref('basic')
const orderId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')
const currentStatus = ref<string>()
const currentContractNo = ref<string>()
const createInspectionLoading = ref(false)

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
  currentContractNo.value = order.contractNo
}

const handleCreateFinishedInspection = async () => {
  if (!orderId.value) return
  try {
    await message.confirm('确认基于该染色单生成成品检验单吗？')
  } catch {
    return
  }

  createInspectionLoading.value = true
  try {
    const inspectionOrderId = await ReleaseProductionApi.createFinishedInspection(Number(orderId.value))
    message.success('生成成品检验单成功')
    router.push(`/production/inspection-order/workspace/${inspectionOrderId}`)
  } finally {
    createInspectionLoading.value = false
  }
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
