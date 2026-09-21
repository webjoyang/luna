<template>
  <ContentWrap>
    <div class="workspace-header flex justify-between items-center mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <DictTag
              v-if="currentOrder?.status"
              :type="DICT_TYPE.LUNAERP_INSPECTION_ORDER_STATUS"
              :value="currentOrder.status"
            />
          </div>
        </template>
      </el-page-header>

      <!-- 操作栏：按状态显示按钮 -->
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

        <!-- 草稿态 / 新建态操作：[保存草稿][完成][取消][删除] -->
        <template v-if="isDraftOrNew">
          <el-button
            v-if="orderId"
            type="danger"
            plain
            @click="handleDelete"
            v-hasPermi="['lunaerp:production:inspection-order:delete']"
          >
            删除
          </el-button>
          <el-button
            type="primary"
            :loading="actionLoading"
            @click="handleSave"
            v-hasPermi="['lunaerp:production:inspection-order:create', 'lunaerp:production:inspection-order:update']"
          >
            保存草稿
          </el-button>
          <el-button
            v-if="orderId"
            type="success"
            plain
            :loading="actionLoading"
            @click="handleComplete"
            v-hasPermi="['lunaerp:production:inspection-order:update']"
          >
            完成
          </el-button>
          <el-button
            v-if="orderId"
            type="warning"
            plain
            :loading="actionLoading"
            @click="handleCancel"
            v-hasPermi="['lunaerp:production:inspection-order:update']"
          >
            取消
          </el-button>
        </template>

        <!-- 录检中态操作：[完成][取消] -->
        <template v-else-if="currentOrder?.status === 'ACTIVE'">
          <el-button
            type="success"
            plain
            :loading="actionLoading"
            @click="handleComplete"
            v-hasPermi="['lunaerp:production:inspection-order:update']"
          >
            完成
          </el-button>
          <el-button
            type="warning"
            plain
            :loading="actionLoading"
            @click="handleCancel"
            v-hasPermi="['lunaerp:production:inspection-order:update']"
          >
            取消
          </el-button>
        </template>
      </div>
    </div>

    <!-- 3 Tab 容器 -->
    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基本信息" name="basic">
        <BasicInfoTab
          ref="basicInfoTabRef"
          :order-id="numericOrderId"
          :is-view-mode="isViewMode"
          @loaded="handleOrderLoaded"
          @saved="handleOrderSaved"
          @cancel="handleBack"
        />
      </el-tab-pane>

      <el-tab-pane label="录检明细" name="items">
        <el-empty
          :description="isCreateMode ? '请先保存基本信息后再录检' : '录检明细功能建设中（将在后续 Issue 04 落地）'"
          :image-size="100"
        />
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <el-empty description="操作日志功能建设中" :image-size="100" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import BasicInfoTab from './BasicInfoTab.vue'
import * as InspectionOrderApi from '@/api/lunaerp-elmui/production/inspection-order'
import type { InspectionOrderVO } from '@/api/lunaerp-elmui/production/inspection-order'

defineOptions({ name: 'LunaerpElmuiProductionInspectionOrderWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const activeTab = ref('basic')
const basicInfoTabRef = ref<InstanceType<typeof BasicInfoTab>>()
const actionLoading = ref(false)

const orderId = computed(() => (route.params.id as string) || null)
const numericOrderId = computed(() => (orderId.value ? Number(orderId.value) : null))
const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')

const currentOrder = ref<InspectionOrderVO | null>(null)

const isDraftOrNew = computed(() => {
  if (isCreateMode.value) return true
  return currentOrder.value?.status === 'DRAFT'
})

const isReadOnly = computed(() => {
  if (isViewMode.value) return true
  if (!isCreateMode.value && currentOrder.value?.status && currentOrder.value.status !== 'DRAFT') {
    return true
  }
  return false
})

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建检验单'
  if (isReadOnly.value) return '查看检验单'
  return '编辑检验单'
})

const handleBack = () => {
  router.push('/production/inspection-order')
}

const handleOrderLoaded = (order: InspectionOrderVO) => {
  currentOrder.value = order
}

const handleOrderSaved = async (id: number) => {
  if (isCreateMode.value) {
    await router.replace(`/production/inspection-order/workspace/${id}`)
  }
  await reloadOrder(id)
}

const handleSave = async () => {
  await basicInfoTabRef.value?.submit()
}

const reloadOrder = async (id?: number) => {
  const targetId = id || numericOrderId.value
  if (targetId) {
    try {
      const data = await InspectionOrderApi.getInspectionOrder(targetId)
      currentOrder.value = data
    } catch {}
  }
}

const handleComplete = async () => {
  if (!numericOrderId.value) return
  try {
    await message.confirm('确认完成该检验单吗？')
    actionLoading.value = true
    await InspectionOrderApi.completeInspectionOrder(numericOrderId.value)
    message.success('检验单已完成')
    await reloadOrder()
  } catch {
  } finally {
    actionLoading.value = false
  }
}

const handleCancel = async () => {
  if (!numericOrderId.value) return
  try {
    await message.confirm('确认取消该检验单吗？')
    actionLoading.value = true
    await InspectionOrderApi.cancelInspectionOrder(numericOrderId.value)
    message.success('检验单已取消')
    await reloadOrder()
  } catch {
  } finally {
    actionLoading.value = false
  }
}

const handleDelete = async () => {
  if (!numericOrderId.value) return
  try {
    await message.delConfirm()
    actionLoading.value = true
    await InspectionOrderApi.deleteInspectionOrder(numericOrderId.value)
    message.success('删除成功')
    handleBack()
  } catch {
  } finally {
    actionLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.workspace-header {
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 12px;
}
.workspace-tabs {
  :deep(.el-tabs__content) {
    min-height: 400px;
  }
}
</style>
