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
              :type="DICT_TYPE.LUNAERP_WEAVING_ORDER_STATUS"
              :value="currentOrder.status"
            />
          </div>
        </template>
      </el-page-header>

      <!-- 操作栏 -->
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回列表</el-button>

        <template v-if="numericOrderId && currentOrder && currentOrder.status === 'DRAFT'">
          <el-button
            type="danger"
            plain
            :loading="actionLoading"
            @click="handleDeleteOrder"
            v-hasPermi="['lunaerp:production:weaving-order:delete']"
          >
            删除织造单
          </el-button>
        </template>

        <template v-if="numericOrderId && currentOrder && currentOrder.status !== 'CANCELLED' && currentOrder.status !== 'COMPLETED'">
          <el-button
            type="warning"
            plain
            :loading="actionLoading"
            @click="handleCancelOrder"
            v-hasPermi="['lunaerp:production:weaving-order:cancel']"
          >
            取消织造单
          </el-button>
        </template>
      </div>
    </div>

    <!-- 5 Tab 容器 -->
    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基本信息" name="basic">
        <BasicInfoTab
          ref="basicInfoTabRef"
          :order-id="numericOrderId"
          @loaded="handleOrderLoaded"
          @saved="handleOrderLoaded"
        />
      </el-tab-pane>

      <el-tab-pane label="机台任务" name="tasks" :disabled="!numericOrderId">
        <TasksTab
          :weaving-order-id="numericOrderId"
          @refresh-order="handleRefreshOrder"
        />
      </el-tab-pane>

      <el-tab-pane label="布票凭证" name="tickets" :disabled="!numericOrderId">
        <TicketsTab
          :weaving-order-id="numericOrderId"
          @refresh-order="handleRefreshOrder"
        />
      </el-tab-pane>

      <el-tab-pane label="领纱管理" name="requisition" :disabled="!numericOrderId">
        <RequisitionTab
          :weaving-order-id="numericOrderId"
          :weaving-order="currentOrder"
        />
      </el-tab-pane>

      <el-tab-pane label="统计分析" name="stats" :disabled="!numericOrderId">
        <StatsTab :weaving-order-id="numericOrderId" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import BasicInfoTab from './BasicInfoTab.vue'
import TasksTab from './TasksTab.vue'
import TicketsTab from './TicketsTab.vue'
import RequisitionTab from './RequisitionTab.vue'
import StatsTab from './StatsTab.vue'
import {
  WeavingOrderVO,
  deleteWeavingOrder,
  cancelWeavingOrder
} from '@/api/lunaerp-elmui/production/weaving-order'

const route = useRoute()
const router = useRouter()

const activeTab = ref((route.query.tab as string) || 'basic')
const actionLoading = ref(false)
const currentOrder = ref<WeavingOrderVO>()
const basicInfoTabRef = ref<InstanceType<typeof BasicInfoTab>>()

const numericOrderId = computed(() => {
  const idStr = route.params.id || route.query.id
  return idStr ? Number(idStr) : 0
})

const pageTitle = computed(() => {
  if (!numericOrderId.value) return '新建织造单'
  return currentOrder.value?.code ? '织造单工作台' : '织造单'
})

const handleOrderLoaded = (order: WeavingOrderVO) => {
  currentOrder.value = order
}

const handleRefreshOrder = () => {
  if (basicInfoTabRef.value) {
    basicInfoTabRef.value.loadData()
  }
}

const handleBack = () => {
  router.push('/production/weaving-order')
}

const handleDeleteOrder = async () => {
  if (!numericOrderId.value) return
  try {
    await ElMessageBox.confirm('确认删除该织造单？下属任务与布票将一并级联删除。', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    actionLoading.value = true
    await deleteWeavingOrder(numericOrderId.value)
    ElMessage.success('织造单已删除')
    router.push('/production/weaving-order')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  } finally {
    actionLoading.value = false
  }
}

const handleCancelOrder = async () => {
  if (!numericOrderId.value) return
  try {
    await ElMessageBox.confirm('确认取消该织造单？取消为终态操作。', '取消确认', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '取消'
    })
    actionLoading.value = true
    await cancelWeavingOrder(numericOrderId.value)
    ElMessage.success('织造单已取消')
    if (basicInfoTabRef.value) {
      basicInfoTabRef.value.loadData()
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '取消失败')
  } finally {
    actionLoading.value = false
  }
}
</script>

<style scoped>
.workspace-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}
</style>
