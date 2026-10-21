<template>
  <div class="h5-inspection-tab-page min-h-screen bg-gray-50 pb-20">
    <!-- 1. 顶部 Header -->
    <div class="header-card bg-white px-4 pt-4 pb-3 shadow-xs border-b border-gray-100">
      <div class="flex items-center justify-between mb-3">
        <div>
          <h1 class="text-18px font-bold text-gray-900">移动质检</h1>
          <p class="text-12px text-gray-500 mt-0.5">检验单列表与现场等级录入</p>
        </div>
        <div
          class="inspector-badge flex items-center bg-green-50 text-green-700 px-2.5 py-1 rounded-full text-12px font-medium"
        >
          <van-icon name="user-o" class="mr-1" />
          <span>{{ currentInspectorName }}</span>
        </div>
      </div>

      <!-- 2. 常驻扫码与键盘楔入区（Spec §4.3: 摄像头与常驻自动聚焦输入框双路径） -->
      <div class="scan-section mb-1">
        <H5Scanner
          placeholder="扫码布票或输入检验单号回车"
          default-mode="qr"
          @scan="handleScannedCode"
        />
      </div>
    </div>

    <!-- 3. 状态与类型筛选 Tabs -->
    <div class="filter-section sticky top-0 z-10 bg-white border-b border-gray-100 shadow-xs">
      <van-tabs v-model:active="activeStatusTab" shrink line-width="24px" @change="handleTabChange">
        <van-tab title="可录检单" name="DRAFT,ACTIVE" />
        <van-tab title="进行中" name="ACTIVE" />
        <van-tab title="草稿" name="DRAFT" />
        <van-tab title="已完成" name="COMPLETED" />
        <van-tab title="全部" name="" />
      </van-tabs>

      <!-- 类型快捷切换 -->
      <div class="type-filter-bar flex items-center space-x-2 px-3 py-2 bg-gray-50 text-12px">
        <span class="text-gray-400">类型:</span>
        <span
          class="filter-chip px-2.5 py-0.5 rounded-full cursor-pointer transition-colors"
          :class="
            !selectedType
              ? 'bg-primary text-white font-medium'
              : 'bg-white text-gray-600 border border-gray-200'
          "
          @click="handleTypeChange('')"
        >
          全部
        </span>
        <span
          class="filter-chip px-2.5 py-0.5 rounded-full cursor-pointer transition-colors"
          :class="
            selectedType === 'GREIGE'
              ? 'bg-primary text-white font-medium'
              : 'bg-white text-gray-600 border border-gray-200'
          "
          @click="handleTypeChange('GREIGE')"
        >
          胚布检验
        </span>
        <span
          class="filter-chip px-2.5 py-0.5 rounded-full cursor-pointer transition-colors"
          :class="
            selectedType === 'FINISHED'
              ? 'bg-primary text-white font-medium'
              : 'bg-white text-gray-600 border border-gray-200'
          "
          @click="handleTypeChange('FINISHED')"
        >
          成品检验
        </span>
      </div>
    </div>

    <!-- 4. 可录检单列表 -->
    <div class="order-list-section p-3">
      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list
          v-model:loading="loading"
          :finished="finished"
          :finished-text="orderList.length > 0 ? '没有更多检验单了' : ''"
          @load="onLoad"
        >
          <!-- 检验单卡片 -->
          <div
            v-for="order in orderList"
            :key="order.id"
            class="order-card bg-white rounded-xl p-4 mb-3 shadow-xs border border-gray-100 transition-all active:bg-gray-50 cursor-pointer"
            @click="goToDetail(order.id)"
          >
            <!-- 头部：单号 + 类型 + 状态 -->
            <div class="flex items-center justify-between pb-2.5 border-b border-gray-100">
              <div class="flex items-center space-x-2">
                <span class="text-15px font-bold text-gray-900 font-mono">{{ order.code }}</span>
                <van-tag
                  :color="order.type === 'GREIGE' ? '#1989fa' : '#7232dd'"
                  plain
                  round
                  size="medium"
                >
                  {{ order.type === 'GREIGE' ? '胚布' : '成品' }}
                </van-tag>
              </div>
              <van-tag :type="getStatusTagType(order.status)" round size="medium">
                {{ getStatusLabel(order.status) }}
              </van-tag>
            </div>

            <!-- 主体信息：品名 + 承检方 + 包装模式 -->
            <div class="order-info py-2.5 space-y-1.5 text-13px">
              <div class="flex items-start">
                <span class="text-gray-400 text-12px min-w-[60px]">产品品名:</span>
                <div class="text-gray-800 font-medium">
                  <span>{{ order.productName || '—' }}</span>
                  <span v-if="order.productCode" class="text-11px text-gray-400 font-mono ml-1">
                    ({{ order.productCode }})
                  </span>
                </div>
              </div>
              <div class="flex items-start">
                <span class="text-gray-400 text-12px min-w-[60px]">承检方:</span>
                <span class="text-gray-700 font-medium break-all">
                  {{ order.inspectionFactoryName || '本厂' }}
                </span>
              </div>
              <div class="flex items-center text-12px text-gray-500">
                <span class="text-gray-400 min-w-[60px]">包装模式:</span>
                <span>{{ order.packageMode === 'PIECE' ? '匹装 (PIECE)' : '卷装 (ROLL)' }}</span>
                <span v-if="order.orderDate" class="ml-auto text-11px text-gray-400">
                  单据日期: {{ order.orderDate }}
                </span>
              </div>
            </div>

            <!-- 录检进度与进入箭头 -->
            <div
              class="progress-footer bg-gray-50 rounded-lg px-3 py-2 flex items-center justify-between text-12px"
            >
              <div class="flex items-center space-x-3">
                <span class="text-gray-500">已录检:</span>
                <span class="font-bold text-primary font-mono text-14px">
                  {{ order.rollCount || 0 }}
                </span>
                <span class="text-gray-400 text-11px">
                  {{ order.packageMode === 'PIECE' ? '匹' : '卷' }}
                </span>
                <span
                  v-if="order.targetWeight"
                  class="text-gray-400 text-11px border-l border-gray-200 pl-3"
                >
                  目标: {{ order.targetWeight }} kg
                </span>
              </div>
              <div class="flex items-center text-primary text-12px font-medium">
                <span>进入详情</span>
                <van-icon name="arrow" class="ml-0.5" />
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <van-empty
            v-if="!loading && orderList.length === 0"
            image="search"
            description="暂无符合条件的检验单"
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
import { useUserStore } from '@/store/modules/user'
import H5Scanner from '../components/Scanner.vue'
import { getInspectionPage, WorkshopInspectionVO } from '@/api/h5/workshop'

defineOptions({ name: 'H5Inspection' })

const router = useRouter()
const userStore = useUserStore()

// 当前登录质检员姓名
const currentInspectorName = computed(() => {
  return userStore.getUser?.nickname || '检验员'
})

// 筛选状态
const activeStatusTab = ref('DRAFT,ACTIVE')
const selectedType = ref('')

// 分页列表状态
const orderList = ref<WorkshopInspectionVO[]>([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const pageNo = ref(1)
const pageSize = ref(10)

// 状态映射
const getStatusLabel = (status: string) => {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'ACTIVE':
      return '录检中'
    case 'COMPLETED':
      return '已完成'
    case 'CANCELLED':
      return '已作废'
    default:
      return status || '未知'
  }
}

const getStatusTagType = (
  status: string
): 'default' | 'primary' | 'success' | 'warning' | 'danger' => {
  switch (status) {
    case 'DRAFT':
      return 'warning'
    case 'ACTIVE':
      return 'primary'
    case 'COMPLETED':
      return 'success'
    case 'CANCELLED':
      return 'default'
    default:
      return 'default'
  }
}

// 切换状态 Tab
const handleTabChange = () => {
  pageNo.value = 1
  orderList.value = []
  finished.value = false
  loadData()
}

// 切换类型
const handleTypeChange = (type: string) => {
  if (selectedType.value === type) return
  selectedType.value = type
  pageNo.value = 1
  orderList.value = []
  finished.value = false
  loadData()
}

// 加载分页数据
const loadData = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const res = await getInspectionPage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      status: activeStatusTab.value || undefined,
      type: selectedType.value || undefined
    })

    const list = res?.list || []
    if (pageNo.value === 1) {
      orderList.value = list
    } else {
      orderList.value = [...orderList.value, ...list]
    }

    if (orderList.value.length >= (res?.total || 0) || list.length < pageSize.value) {
      finished.value = true
    } else {
      pageNo.value++
    }
  } catch (err: any) {
    showToast(err?.message || '加载检验单列表失败')
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

// 下拉刷新
const onRefresh = () => {
  finished.value = false
  pageNo.value = 1
  loadData()
}

// 触底加载
const onLoad = () => {
  loadData()
}

// 扫码处理
const handleScannedCode = (code: string) => {
  const cleanCode = (code || '').trim()
  if (!cleanCode) return

  // 若扫描内容为检验单号 (INS-)，尝试匹配并直接进入
  const matched = orderList.value.find((o) => o.code === cleanCode)
  if (matched) {
    goToDetail(matched.id)
    return
  }

  // 否则提示布票扫码录检编排（后续 14 号工单接入 ticket-scan）
  showToast({
    message: `已扫描: ${cleanCode}\n胚布扫码录检与自动交接待 14 号工单接入`,
    duration: 3000
  })
}

// 点击卡片进入详情
const goToDetail = (id: number) => {
  router.push(`/h5/inspection/order?id=${id}`)
}
</script>

<style lang="scss" scoped>
.h5-inspection-tab-page {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
