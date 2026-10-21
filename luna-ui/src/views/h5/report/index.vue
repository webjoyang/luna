<template>
  <div class="h5-report-tab-page min-h-screen bg-gray-50 pb-20">
    <!-- 1. 顶部 Header -->
    <div class="header-card bg-white px-4 pt-4 pb-3 shadow-xs border-b border-gray-100">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-18px font-bold text-gray-900">车间报工</h1>
          <p class="text-12px text-gray-500 mt-0.5">我的机台任务与布票扫码报工</p>
        </div>
        <div
          class="worker-badge flex items-center bg-blue-50 text-primary px-2.5 py-1 rounded-full text-12px font-medium"
        >
          <van-icon name="user-o" class="mr-1" />
          <span>{{ currentWorkerName }}</span>
        </div>
      </div>

      <!-- 2. 大扫码按钮区 (Prominent Big Scan Button) -->
      <div
        class="scan-banner mt-3 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-xl p-4 text-white shadow-sm flex items-center justify-between"
      >
        <div class="flex-1 mr-3">
          <div class="text-16px font-bold flex items-center">
            <van-icon name="scan" class="text-20px mr-1.5" />
            <span>布票扫码报工</span>
          </div>
          <div class="text-12px text-white/80 mt-1"> 支持机台扫码、摄像头识别与扫码枪键盘回车 </div>
        </div>
        <van-button
          round
          size="normal"
          class="!bg-white !text-blue-600 !border-0 font-bold px-4 shadow-sm"
          @click="handleOpenScanner"
        >
          立即扫码
        </van-button>
      </div>
    </div>

    <!-- 3. 状态筛选 Tabs -->
    <div class="tab-filter sticky top-0 z-10 bg-white border-b border-gray-100 shadow-xs">
      <van-tabs v-model:active="activeTab" shrink line-width="24px" @change="handleTabChange">
        <van-tab title="全部待办" name="" />
        <van-tab title="进行中" name="IN_PROGRESS" />
        <van-tab title="待排产" name="PENDING" />
        <van-tab title="已暂停" name="HALTED" />
        <van-tab title="已完工" name="COMPLETED" />
      </van-tabs>
    </div>

    <!-- 4. 我的机台任务列表 -->
    <div class="task-list-section p-3">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          :finished-text="taskList.length > 0 ? '没有更多任务了' : ''"
          @load="onLoad"
        >
          <!-- 任务卡片 -->
          <div
            v-for="task in taskList"
            :key="task.id"
            class="task-card bg-white rounded-xl p-4 mb-3 shadow-xs border border-gray-100 transition-all active:bg-gray-50"
          >
            <!-- 头部：机台号 + 状态 -->
            <div class="flex items-center justify-between pb-2 border-b border-gray-50">
              <div class="flex items-center space-x-2">
                <span class="text-16px font-bold text-gray-900">{{
                  task.loomNo || '未知机台'
                }}</span>
                <span class="text-11px text-gray-400 font-mono">{{ task.code }}</span>
              </div>
              <van-tag :type="getStatusTagType(task.status)" round size="medium">
                {{ getStatusLabel(task.status) }}
              </van-tag>
            </div>

            <!-- 主体信息：织造单号 + 品名 -->
            <div class="task-info py-2.5 space-y-1.5 text-13px">
              <div class="flex items-start">
                <span class="text-gray-400 text-12px min-w-[60px]">织造单：</span>
                <span class="font-mono text-gray-800 font-medium break-all">{{
                  task.weavingOrderCode || '—'
                }}</span>
              </div>
              <div class="flex items-start">
                <span class="text-gray-400 text-12px min-w-[60px]">产品品名：</span>
                <div class="text-gray-800 font-medium">
                  <span>{{ task.productName || '—' }}</span>
                  <span v-if="task.productCode" class="text-11px text-gray-400 font-mono ml-1"
                    >({{ task.productCode }})</span
                  >
                </div>
              </div>
              <div
                v-if="task.plannedStart || task.plannedEnd"
                class="flex items-center text-11px text-gray-400"
              >
                <span class="min-w-[60px]">计划排期：</span>
                <span>{{ task.plannedStart || '未定' }} ~ {{ task.plannedEnd || '未定' }}</span>
              </div>
            </div>

            <!-- 进度条：实织匹数 / 计划匹数 -->
            <div class="progress-box bg-gray-50 rounded-lg p-2.5 my-1">
              <div class="flex justify-between items-center text-12px mb-1.5">
                <span class="text-gray-500">实织进度</span>
                <span
                  class="font-semibold"
                  :class="task.actualQty >= task.plannedQty ? 'text-green-600' : 'text-primary'"
                >
                  {{ task.actualQty || 0 }} / {{ task.plannedQty || 0 }} 匹 ({{
                    calcProgressPercent(task.actualQty, task.plannedQty)
                  }}%)
                </span>
              </div>
              <van-progress
                :percentage="calcProgressPercent(task.actualQty, task.plannedQty)"
                stroke-width="6"
                :show-pivot="false"
                :color="task.status === 'COMPLETED' ? '#07c160' : '#1989fa'"
              />
            </div>

            <!-- 布票状态统计细项 -->
            <div
              v-if="task.ticketCounts"
              class="ticket-status-row flex flex-wrap gap-1.5 pt-2 text-11px"
            >
              <span class="bg-blue-50 text-blue-700 px-1.5 py-0.5 rounded"
                >待开工 {{ task.ticketCounts.generated }}</span
              >
              <span class="bg-indigo-50 text-indigo-700 px-1.5 py-0.5 rounded"
                >进行中 {{ task.ticketCounts.started }}</span
              >
              <span class="bg-green-50 text-green-700 px-1.5 py-0.5 rounded"
                >已完工 {{ task.ticketCounts.completed }}</span
              >
              <span class="bg-emerald-50 text-emerald-700 px-1.5 py-0.5 rounded"
                >已检验 {{ task.ticketCounts.inspected }}</span
              >
              <span
                v-if="task.ticketCounts.exception > 0"
                class="bg-red-50 text-red-600 px-1.5 py-0.5 rounded font-medium"
              >
                异常 {{ task.ticketCounts.exception }}
              </span>
            </div>

            <!-- 操作按钮 -->
            <div class="card-action pt-3 mt-1 flex justify-end">
              <van-button
                size="small"
                type="primary"
                plain
                round
                icon="scan"
                @click="handleOpenScanner"
              >
                扫码此任务报工
              </van-button>
            </div>
          </div>

          <!-- 空状态 -->
          <van-empty
            v-if="!loading && taskList.length === 0"
            image="search"
            description="暂无符合条件的机台任务"
          />
        </van-list>
      </van-pull-refresh>
    </div>

    <!-- 5. 扫码弹层（嵌入 H5Scanner 支持摄像头扫码与键盘回车双路径） -->
    <van-popup
      v-model:show="showScannerPopup"
      position="bottom"
      round
      closeable
      :style="{ height: '80%' }"
    >
      <div class="scanner-popup-content p-4 pt-10">
        <h3 class="text-16px font-bold text-gray-900 mb-1">扫描布票二维码 / 条码</h3>
        <p class="text-12px text-gray-500 mb-4">扫码后将自动跳转至布票报工详情与开工流程</p>
        <H5Scanner placeholder="扫描或输入布票编号" default-mode="qr" @scan="handleScannedCode" />
      </div>
    </van-popup>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStoreWithOut } from '@/store/modules/user'
import H5Scanner from '@/views/h5/components/Scanner.vue'
import { getMyTasks, type WorkshopMyTaskVO } from '@/api/h5/workshop'

defineOptions({ name: 'H5Report' })

const router = useRouter()
const userStore = useUserStoreWithOut()

const currentWorkerName = computed(() => {
  const user = userStore.getUser
  return user?.nickname || user?.username || '挡车工'
})

// 状态管理
const activeTab = ref<string>('')
const taskList = ref<WorkshopMyTaskVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNo = ref(1)
const pageSize = 10

// 扫码弹层
const showScannerPopup = ref(false)

const handleOpenScanner = () => {
  showScannerPopup.value = true
}

const handleScannedCode = (code: string) => {
  if (!code || !code.trim()) return
  showScannerPopup.value = false
  // 跳转到冻结的布票扫码落地路由
  router.push({
    path: '/h5/scan/weaving-ticket',
    query: { code: code.trim() }
  })
}

// 状态标签转换
const getStatusLabel = (status?: string) => {
  switch (status) {
    case 'PENDING':
      return '待排产'
    case 'IN_PROGRESS':
      return '进行中'
    case 'HALTED':
      return '已暂停'
    case 'COMPLETED':
      return '已完工'
    default:
      return status || '待排产'
  }
}

const getStatusTagType = (status?: string) => {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'IN_PROGRESS':
      return 'primary'
    case 'HALTED':
      return 'danger'
    case 'COMPLETED':
      return 'success'
    default:
      return 'default'
  }
}

// 进度计算
const calcProgressPercent = (actual?: number, planned?: number): number => {
  const act = actual || 0
  const plan = planned || 0
  if (plan <= 0) return 0
  const percent = Math.round((act / plan) * 100)
  return Math.min(percent, 100)
}

// 加载任务列表
const loadTasks = async (isRefresh = false) => {
  if (isRefresh) {
    pageNo.value = 1
    finished.value = false
  }

  loading.value = true
  try {
    const res = await getMyTasks({
      pageNo: pageNo.value,
      pageSize,
      status: activeTab.value ? activeTab.value : undefined
    })

    const list = res?.list || []
    if (isRefresh) {
      taskList.value = list
    } else {
      taskList.value = [...taskList.value, ...list]
    }

    const total = res?.total || 0
    if (taskList.value.length >= total || list.length < pageSize) {
      finished.value = true
    } else {
      pageNo.value++
    }
  } catch (error: any) {
    showToast(error?.message || '加载机台任务失败')
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onLoad = () => {
  loadTasks(false)
}

const onRefresh = () => {
  refreshing.value = true
  loadTasks(true)
}

const handleTabChange = () => {
  taskList.value = []
  pageNo.value = 1
  finished.value = false
  loadTasks(true)
}
</script>

<style scoped>
.h5-report-tab-page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}
</style>
