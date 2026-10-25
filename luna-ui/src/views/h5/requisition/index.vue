<template>
  <div class="h5-requisition-tab-page min-h-screen bg-gray-50 pb-20">
    <!-- 1. 顶部 Header -->
    <div class="header-card bg-white px-4 pt-4 pb-3 shadow-xs border-b border-gray-100">
      <div class="flex items-center justify-between mb-1">
        <div>
          <h1 class="text-18px font-bold text-gray-900">移动发纱</h1>
          <p class="text-12px text-gray-500 mt-0.5">我的待领纱单与车间发纱</p>
        </div>
        <div
          class="worker-badge flex items-center bg-orange-50 text-orange-700 px-2.5 py-1 rounded-full text-12px font-medium"
        >
          <van-icon name="user-o" class="mr-1" />
          <span>{{ currentWorkerName }}</span>
        </div>
      </div>
    </div>

    <!-- 2. 状态筛选 Tabs -->
    <div class="filter-section sticky top-0 z-10 bg-white border-b border-gray-100 shadow-xs">
      <van-tabs v-model:active="activeTab" shrink line-width="24px" @change="handleTabChange">
        <van-tab title="全部待领纱" name="ACTIVE,PARTIAL" />
        <van-tab title="待发纱" name="ACTIVE" />
        <van-tab title="部分发纱" name="PARTIAL" />
      </van-tabs>
    </div>

    <!-- 3. 我的待领纱单列表 -->
    <div class="requisition-list-section p-3">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          :finished-text="requisitionList.length > 0 ? '没有更多待领纱单了' : ''"
          @load="onLoad"
        >
          <!-- 领纱单卡片 -->
          <div
            v-for="item in requisitionList"
            :key="item.id"
            class="requisition-card bg-white rounded-xl p-4 mb-3 shadow-xs border border-gray-100 transition-all active:bg-gray-50 cursor-pointer"
            @click="handleOpenDetail(item.id)"
          >
            <!-- 头部：领纱单号 + 状态 Tag -->
            <div class="flex items-center justify-between pb-2 border-b border-gray-50">
              <div class="flex items-center space-x-2">
                <van-icon name="logistics" class="text-orange-500 text-16px" />
                <span class="text-15px font-bold text-gray-900 font-mono">{{ item.code }}</span>
              </div>
              <van-tag :type="getStatusTagType(item.status)" round size="medium">
                {{ getStatusLabel(item.status) }}
              </van-tag>
            </div>

            <!-- 主体信息 -->
            <div class="space-y-1.5 py-2.5 text-13px text-gray-600">
              <div class="flex items-center justify-between">
                <span class="text-gray-400">关联织造单：</span>
                <span class="font-medium text-gray-800 font-mono">{{
                  item.weavingOrderCode || '-'
                }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-gray-400">创建时间：</span>
                <span class="text-gray-700">{{ item.createTime || '-' }}</span>
              </div>
              <div v-if="item.remark" class="flex items-start justify-between">
                <span class="text-gray-400 shrink-0">备注：</span>
                <span class="text-gray-700 text-right truncate max-w-60">{{ item.remark }}</span>
              </div>
            </div>

            <!-- 卡片操作指引 -->
            <div
              class="card-action-bar flex items-center justify-between pt-2 border-t border-gray-50 text-12px"
            >
              <span class="text-gray-400">点击查看规格明细与发纱</span>
              <span class="text-primary font-medium flex items-center">
                前往发纱
                <van-icon name="arrow" class="ml-0.5" />
              </span>
            </div>
          </div>

          <!-- 空状态 -->
          <van-empty
            v-if="!loading && requisitionList.length === 0"
            image="search"
            description="暂无待领纱单"
          />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStoreWithOut } from '@/store/modules/user'
import { getRequisitionPage, type WorkshopRequisitionVO } from '@/api/h5/workshop'

defineOptions({ name: 'H5Requisition' })

const router = useRouter()
const userStore = useUserStoreWithOut()

const currentWorkerName = computed(() => {
  const user = userStore.getUser
  return user?.nickname || user?.username || '挡车工'
})

// 状态管理
const activeTab = ref<string>('ACTIVE,PARTIAL')
const requisitionList = ref<WorkshopRequisitionVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNo = ref(1)
const pageSize = 10

// 状态标签映射
const REQUISITION_STATUS_CONFIG: Record<
  string,
  { label: string; type: 'primary' | 'warning' | 'default' }
> = {
  ACTIVE: { label: '待发纱', type: 'primary' },
  PARTIAL: { label: '部分发纱', type: 'warning' }
}

const getStatusLabel = (status: string) => {
  return REQUISITION_STATUS_CONFIG[status]?.label || status
}

const getStatusTagType = (status: string) => {
  return REQUISITION_STATUS_CONFIG[status]?.type || 'default'
}

const handleTabChange = () => {
  pageNo.value = 1
  finished.value = false
  requisitionList.value = []
  onLoad()
}

const onLoad = async () => {
  if (refreshing.value) {
    requisitionList.value = []
    refreshing.value = false
  }

  loading.value = true
  try {
    const res = await getRequisitionPage({
      pageNo: pageNo.value,
      pageSize,
      status: activeTab.value
    })

    const list = res.list || []
    if (pageNo.value === 1) {
      requisitionList.value = list
    } else {
      requisitionList.value.push(...list)
    }

    if (requisitionList.value.length >= res.total || list.length < pageSize) {
      finished.value = true
    } else {
      pageNo.value++
    }
  } catch (e: any) {
    showToast(e?.message || '加载领纱单失败')
    finished.value = true
  } finally {
    loading.value = false
  }
}

const onRefresh = () => {
  finished.value = false
  pageNo.value = 1
  onLoad()
}

const handleOpenDetail = (id: number) => {
  router.push({
    path: '/h5/requisition/detail',
    query: { id: String(id) }
  })
}
</script>

<style scoped>
.requisition-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
</style>
