<template>
  <ContentWrap>
    <div class="workspace-header flex justify-between items-center mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <span v-if="currentOrder?.code" class="text-gray-500 font-mono text-14px">
              {{ currentOrder.code }}
            </span>
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

        <!-- 草稿态 / 新建态操作：[删除][保存草稿][完成][取消] -->
        <template v-if="!isAllReadOnly && isDraftOrNew">
          <el-button
            v-if="numericOrderId"
            type="danger"
            plain
            :loading="actionLoading"
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
            v-if="numericOrderId"
            type="success"
            plain
            :loading="actionLoading"
            @click="handleComplete"
            v-hasPermi="['lunaerp:production:inspection-order:update']"
          >
            完成
          </el-button>
          <el-button
            v-if="numericOrderId"
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
        <template v-else-if="!isAllReadOnly && currentOrder?.status === 'ACTIVE'">
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
          :is-view-mode="isAllReadOnly"
          @loaded="handleOrderLoaded"
          @saved="handleOrderSaved"
          @cancel="handleBack"
        />
      </el-tab-pane>

      <el-tab-pane label="录检明细" name="items">
        <ItemsTab
          ref="itemsTabRef"
          :order-id="numericOrderId"
          :order-status="currentOrder?.status"
          :is-view-mode="isAllReadOnly"
          :default-product-code="currentOrder?.productCode"
          :default-product-name="currentOrder?.productName"
          @item-changed="handleItemChanged"
          @switch-to-basic="activeTab = 'basic'"
        />
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <LogsTab />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import BasicInfoTab from './BasicInfoTab.vue'
import ItemsTab from './ItemsTab.vue'
import LogsTab from './LogsTab.vue'
import * as InspectionOrderApi from '@/api/lunaerp-elmui/production/inspection-order'
import type { InspectionOrderVO } from '@/api/lunaerp-elmui/production/inspection-order'

defineOptions({ name: 'LunaerpElmuiProductionInspectionOrderWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const activeTab = ref('basic')
const basicInfoTabRef = ref<InstanceType<typeof BasicInfoTab>>()
const itemsTabRef = ref<InstanceType<typeof ItemsTab>>()
const actionLoading = ref(false)

const orderId = computed(() => (route.params.id as string) || null)
const numericOrderId = computed(() => (orderId.value ? Number(orderId.value) : null))
const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')

const currentOrder = ref<InspectionOrderVO | null>(null)

const isAllReadOnly = computed(() => {
  if (isViewMode.value) return true
  const st = currentOrder.value?.status
  return st === 'COMPLETED' || st === 'CANCELLED'
})

const isDraftOrNew = computed(() => {
  if (isCreateMode.value) return true
  return currentOrder.value?.status === 'DRAFT'
})

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建检验单'
  if (isAllReadOnly.value) return '查看检验单'
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
  try {
    actionLoading.value = true
    await basicInfoTabRef.value?.submit()
  } catch (err) {
    activeTab.value = 'basic'
  } finally {
    actionLoading.value = false
  }
}

const reloadOrder = async (id?: number) => {
  const targetId = id || numericOrderId.value
  if (targetId) {
    try {
      const data = await InspectionOrderApi.getInspectionOrder(targetId)
      currentOrder.value = data
      // 触发基本信息 Tab 同步重载回显与状态
      if (basicInfoTabRef.value?.init) {
        await basicInfoTabRef.value.init()
      }
    } catch {}
  }
}

const handleItemChanged = async () => {
  await reloadOrder()
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
    await message.delConfirm('确认删除该检验单吗？')
    actionLoading.value = true
    await InspectionOrderApi.deleteInspectionOrder(numericOrderId.value)
    message.success('删除成功')
    handleBack()
  } catch {
  } finally {
    actionLoading.value = false
  }
}

watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      await reloadOrder(Number(newId))
    } else {
      currentOrder.value = null
    }
  }
)
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
