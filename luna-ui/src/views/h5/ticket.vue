<template>
  <div class="h5-ticket-page min-h-screen bg-gray-50 pb-24 flex flex-col justify-between">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="布票报工卡"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      @click-left="goBack"
    />

    <!-- 内容区域 -->
    <div class="p-3 flex-1">
      <!-- 加载中状态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-gray-400">
        <van-loading type="spinner" color="#1989fa" size="32px" />
        <span class="text-13px mt-3">正在读取布票数据...</span>
      </div>

      <!-- 错误/查无此票状态 -->
      <div v-else-if="errorMessage" class="bg-white rounded-xl p-6 shadow-xs text-center my-4">
        <van-empty image="error" :description="errorMessage" />
        <div class="space-y-2 mt-4 max-w-xs mx-auto">
          <van-button type="primary" block round @click="handleReScan">
            重新扫描 / 输入
          </van-button>
          <van-button block round plain @click="goBack"> 返回上一页 </van-button>
        </div>
      </div>

      <!-- 布票卡展示 -->
      <div v-else-if="ticket" class="space-y-3">
        <!-- 未派工自助认领提示条 -->
        <van-notice-bar
          v-if="ticket.claimable && !ticket.workerUserId"
          left-icon="info-o"
          text="提示：该机台任务尚未派工，开工将自动自助认领该任务"
          color="#1989fa"
          background="#ecf9ff"
          class="rounded-lg shadow-xs"
        />

        <!-- 主卡头：布票号与状态 -->
        <div class="ticket-header-card bg-white rounded-xl p-4 shadow-xs border border-gray-100">
          <div class="flex items-center justify-between pb-3 border-b border-gray-100">
            <div>
              <span class="text-11px text-gray-400">布票编号</span>
              <div class="font-mono text-18px font-bold text-gray-900 mt-0.5 break-all">
                {{ ticket.code }}
              </div>
            </div>
            <van-tag :type="statusTagType" size="medium" round>
              {{ statusLabel }}
            </van-tag>
          </div>

          <div class="grid grid-cols-2 gap-3 pt-3 text-13px">
            <div>
              <span class="text-gray-400 text-12px block">机台号</span>
              <span class="font-semibold text-gray-800">{{
                ticket.loomNo || ticket.plannedMachine || '—'
              }}</span>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">挡车工</span>
              <span class="font-semibold text-gray-800">{{
                ticket.workerName || '未派工（可认领）'
              }}</span>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">织造单号</span>
              <span class="font-mono text-gray-700 text-12px">{{
                ticket.weavingOrderCode || '—'
              }}</span>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">机台任务号</span>
              <span class="font-mono text-gray-700 text-12px">{{
                ticket.machineTaskCode || '—'
              }}</span>
            </div>
          </div>
        </div>

        <!-- 产品与规格卡片 -->
        <div class="bg-white rounded-xl p-4 shadow-xs border border-gray-100">
          <h3 class="text-14px font-bold text-gray-900 mb-3 flex items-center">
            <van-icon name="records-o" class="text-primary mr-1.5" />
            产品与规格信息
          </h3>
          <div class="space-y-2 text-13px">
            <div class="flex justify-between">
              <span class="text-gray-400">品名：</span>
              <span class="font-medium text-gray-900 text-right">{{
                ticket.productName || '—'
              }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">产品编号：</span>
              <span class="font-mono text-gray-700">{{ ticket.productCode || '—' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">成份快照：</span>
              <span class="text-gray-800">{{ ticket.composition || '—' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">克重 / 门幅：</span>
              <span class="text-gray-800">
                {{ ticket.gsm || '—' }} / {{ ticket.width || '—' }}
              </span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">单匹计划重量：</span>
              <span class="font-bold text-primary">{{ ticket.plannedWeight || '—' }} KG</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">原料批号：</span>
              <span class="font-mono text-gray-700">{{ ticket.materialBatchNo || '—' }}</span>
            </div>
          </div>
        </div>

        <!-- 执行流转信息卡片 -->
        <div class="bg-white rounded-xl p-4 shadow-xs border border-gray-100">
          <h3 class="text-14px font-bold text-gray-900 mb-3 flex items-center">
            <van-icon name="underway-o" class="text-primary mr-1.5" />
            执行时间与记录
          </h3>
          <div class="space-y-2 text-13px">
            <div class="flex justify-between">
              <span class="text-gray-400">实际上机机台：</span>
              <span class="text-gray-800">{{ ticket.actualMachine || '—' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">开工时间：</span>
              <span class="font-mono text-gray-700">{{ ticket.startedAt || '未开工' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">完工时间：</span>
              <span class="font-mono text-gray-700">{{ ticket.completedAt || '未完工' }}</span>
            </div>
            <div v-if="ticket.exceptionType" class="pt-2 border-t border-gray-50 text-red-600">
              <div class="flex justify-between">
                <span>异常类型：</span>
                <span class="font-semibold">{{ ticket.exceptionType }}</span>
              </div>
              <div v-if="ticket.exceptionNote" class="mt-1 text-12px text-gray-600">
                说明：{{ ticket.exceptionNote }}
              </div>
            </div>
          </div>
        </div>

        <!-- 切换布票快捷扫码 -->
        <div class="bg-white rounded-xl p-3 shadow-xs border border-gray-100">
          <H5Scanner
            placeholder="扫码或输入其他布票定位"
            default-mode="qr"
            @scan="handleScanOther"
          />
        </div>
      </div>
    </div>

    <!-- 底部固定操作栏（开工/完工/异常登记占位，待后续 Ticket 11/12 业务动作闭环） -->
    <div
      v-if="ticket"
      class="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-2.5 z-20 flex space-x-2"
      style="max-width: 640px; margin: 0 auto"
    >
      <van-button
        type="warning"
        round
        plain
        class="flex-1"
        size="normal"
        @click="handleActionPlaceholder('异常登记')"
      >
        <van-icon name="warning-o" class="mr-1" />
        异常登记
      </van-button>
      <van-button
        type="primary"
        round
        class="flex-1"
        size="normal"
        @click="handleActionPlaceholder('开工')"
      >
        <van-icon name="play-circle-o" class="mr-1" />
        {{ ticket.status === 'STARTED' ? '重新开工' : '开工' }}
      </van-button>
      <van-button
        type="success"
        round
        class="flex-1"
        size="normal"
        @click="handleActionPlaceholder('完工')"
      >
        <van-icon name="passed" class="mr-1" />
        完工
      </van-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getTicketByCode, type WorkshopTicketVO } from '@/api/h5/workshop'
import H5Scanner from './components/Scanner.vue'

defineOptions({ name: 'H5Ticket' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const ticket = ref<WorkshopTicketVO | null>(null)

const STATUS_MAP: Record<
  string,
  { label: string; type: 'default' | 'primary' | 'success' | 'warning' | 'danger' }
> = {
  GENERATED: { label: '待开工', type: 'warning' },
  STARTED: { label: '生产中', type: 'primary' },
  COMPLETED: { label: '已完工', type: 'success' },
  INSPECTED: { label: '已检验', type: 'success' },
  STOCKED: { label: '已入库', type: 'success' },
  EXCEPTION: { label: '异常中', type: 'danger' },
  VOID: { label: '已作废', type: 'danger' }
}

const statusLabel = computed(() => {
  if (!ticket.value?.status) return '未知'
  return STATUS_MAP[ticket.value.status]?.label || ticket.value.status
})

const statusTagType = computed(() => {
  if (!ticket.value?.status) return 'default'
  return STATUS_MAP[ticket.value.status]?.type || 'default'
})

const loadTicket = async (code: string) => {
  const cleanCode = (code || '').trim()
  if (!cleanCode) {
    errorMessage.value = '未指定布票编号，请扫描或输入布票条码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getTicketByCode(cleanCode)
    ticket.value = data
  } catch (err: any) {
    ticket.value = null
    errorMessage.value = err?.msg || err?.message || '读取布票信息失败，可能不存在或已被作废'
  } finally {
    loading.value = false
  }
}

const handleReScan = () => {
  router.push('/h5/scan/weaving-ticket')
}

const handleScanOther = (newCode: string) => {
  if (!newCode || !newCode.trim()) return
  router.replace({
    path: '/h5/ticket',
    query: { code: newCode.trim() }
  })
  loadTicket(newCode)
}

const handleActionPlaceholder = (actionName: string) => {
  showToast(`[${actionName}] 按钮已占位就绪，待后续工单接入实际动作`)
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/h5/report')
  }
}

onMounted(() => {
  const code = (route.query.code as string) || ''
  loadTicket(code)
})
</script>

<style scoped>
.h5-ticket-page {
  max-width: 640px;
  margin: 0 auto;
}
</style>
