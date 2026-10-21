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
                <span class="font-semibold">{{ exceptionTypeLabel(ticket.exceptionType) }}</span>
              </div>
              <div v-if="ticket.exceptionNote" class="mt-1 text-12px text-gray-600">
                说明：{{ ticket.exceptionNote }}
              </div>
              <div v-if="ticket.exceptionPhotos && ticket.exceptionPhotos.length > 0" class="mt-2">
                <span class="text-12px text-gray-400 block mb-1.5">
                  异常现场照片（{{ ticket.exceptionPhotos.length }}张）：
                </span>
                <div class="flex flex-wrap gap-2">
                  <van-image
                    v-for="(photo, idx) in ticket.exceptionPhotos"
                    :key="idx"
                    :src="photo"
                    width="60"
                    height="60"
                    radius="4"
                    fit="cover"
                    @click="handlePreviewPhotos(ticket.exceptionPhotos, idx)"
                  />
                </div>
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

    <!-- 底部固定操作栏 -->
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
        :disabled="!canException"
        @click="openExceptionPopup"
      >
        <van-icon name="warning-o" class="mr-1" />
        异常登记
      </van-button>
      <van-button
        type="primary"
        round
        class="flex-1"
        size="normal"
        :loading="startSubmitting"
        :disabled="!canStart"
        @click="handleStart"
      >
        <van-icon name="play-circle-o" class="mr-1" />
        {{ ticket.status === 'STARTED' ? '重新开工' : '开工' }}
      </van-button>
      <van-button
        type="success"
        round
        class="flex-1"
        size="normal"
        :loading="completeSubmitting"
        :disabled="!canComplete"
        @click="handleComplete"
      >
        <van-icon name="passed" class="mr-1" />
        完工
      </van-button>
    </div>

    <!-- 异常登记弹窗 -->
    <van-popup
      v-model:show="exceptionPopupVisible"
      position="bottom"
      round
      closeable
      safe-area-inset-bottom
      class="max-h-[85vh] flex flex-col"
    >
      <div class="p-4 pb-2 border-b border-gray-100 font-bold text-16px text-gray-900">
        布票异常登记
      </div>

      <div class="p-4 overflow-y-auto space-y-4 flex-1">
        <div>
          <div class="text-13px font-medium text-gray-700 mb-2">
            异常类型 <span class="text-red-500">*</span>
          </div>
          <van-radio-group v-model="exceptionForm.exceptionType" direction="horizontal">
            <van-radio
              v-for="item in EXCEPTION_TYPES"
              :key="item.value"
              :name="item.value"
              class="mb-2 mr-3 text-13px"
            >
              {{ item.label }}
            </van-radio>
          </van-radio-group>
        </div>

        <div>
          <div class="text-13px font-medium text-gray-700 mb-2">异常说明</div>
          <van-field
            v-model="exceptionForm.exceptionNote"
            type="textarea"
            rows="3"
            autosize
            maxlength="200"
            show-word-limit
            placeholder="请输入异常详细说明（选填）"
            class="bg-gray-50 rounded-lg border border-gray-200"
          />
        </div>

        <div>
          <H5PhotoUploader v-model="exceptionForm.exceptionPhotos" :max-count="6" />
        </div>
      </div>

      <div class="p-4 border-t border-gray-100 flex space-x-2 bg-white">
        <van-button round block plain class="flex-1" @click="exceptionPopupVisible = false">
          取消
        </van-button>
        <van-button
          round
          block
          type="danger"
          class="flex-1"
          :loading="exceptionSubmitting"
          :disabled="!exceptionForm.exceptionType"
          @click="handleExceptionSubmit"
        >
          提交异常
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast, showImagePreview } from 'vant'
import {
  getTicketByCode,
  startTicket,
  completeTicket,
  exceptionTicket,
  type WorkshopTicketVO
} from '@/api/h5/workshop'
import { useH5Draft } from '@/utils/h5-draft'
import { formatSubmitError } from '@/utils/h5-submit'
import H5Scanner from './components/Scanner.vue'
import H5PhotoUploader from './components/PhotoUploader.vue'

defineOptions({ name: 'H5Ticket' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const startSubmitting = ref(false)
const completeSubmitting = ref(false)
const exceptionSubmitting = ref(false)
const exceptionPopupVisible = ref(false)
const errorMessage = ref('')
const ticket = ref<WorkshopTicketVO | null>(null)

const EXCEPTION_TYPES = [
  { label: '断纱', value: 'YARN_BREAK' },
  { label: '坏布', value: 'CLOTH_DAMAGE' },
  { label: '缺纱停机', value: 'YARN_SHORTAGE' },
  { label: '换机', value: 'MACHINE_CHANGE' },
  { label: '换人', value: 'WORKER_CHANGE' }
]

const exceptionForm = ref({
  exceptionType: '',
  exceptionNote: '',
  exceptionPhotos: [] as string[]
})

const { load: loadExceptionDraft, clear: clearExceptionDraft } = useH5Draft({
  namespace: 'ticket-exception',
  key: () => ticket.value?.code,
  formData: exceptionForm,
  autoSave: true
})

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

const canStart = computed(() => {
  if (!ticket.value?.status) return false
  return ['GENERATED', 'STARTED', 'EXCEPTION'].includes(ticket.value.status)
})

const canException = computed(() => {
  if (!ticket.value?.status) return false
  return ['GENERATED', 'STARTED'].includes(ticket.value.status)
})

const canComplete = computed(() => {
  if (!ticket.value?.status) return false
  return ['STARTED', 'COMPLETED', 'INSPECTED', 'STOCKED'].includes(ticket.value.status)
})

const exceptionTypeLabel = (type: string) => {
  const matched = EXCEPTION_TYPES.find((item) => item.value === type)
  return matched ? matched.label : type
}

const handlePreviewPhotos = (photos?: string[], startPosition = 0) => {
  if (!photos || photos.length === 0) return
  showImagePreview({
    images: photos,
    startPosition
  })
}

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

const handleStart = async () => {
  if (!ticket.value || startSubmitting.value) return
  if (!canStart.value) {
    showToast('当前布票状态不允许开工')
    return
  }

  const isUnassigned = ticket.value.claimable && !ticket.value.workerUserId
  try {
    if (isUnassigned) {
      await showConfirmDialog({
        title: '自助认领并开工',
        message: '当前机台任务尚未派工，开工将自动由您认领该任务并开始作业，是否确认开工？',
        confirmButtonText: '确认认领并开工',
        cancelButtonText: '取消'
      })
    } else {
      await showConfirmDialog({
        title: '确认开工',
        message: `确认对布票【${ticket.value.code}】执行开工操作？`,
        confirmButtonText: '确认开工',
        cancelButtonText: '取消'
      })
    }
  } catch {
    // 用户取消确认
    return
  }

  startSubmitting.value = true
  try {
    await startTicket({
      code: ticket.value.code
    })
    showToast({
      type: 'success',
      message: isUnassigned ? '已成功认领并开工！' : '开工成功！'
    })
    await loadTicket(ticket.value.code)
  } catch (err: any) {
    const errInfo = formatSubmitError(err)
    showToast({
      type: 'fail',
      message: errInfo.message
    })
  } finally {
    startSubmitting.value = false
  }
}

const handleComplete = async () => {
  if (!ticket.value || completeSubmitting.value) return
  if (!canComplete.value) {
    showToast('当前布票状态不允许完工操作')
    return
  }

  const isAlreadyCompleted = ['COMPLETED', 'INSPECTED', 'STOCKED'].includes(ticket.value.status)
  try {
    await showConfirmDialog({
      title: '确认完工',
      message: isAlreadyCompleted
        ? `布票【${ticket.value.code}】当前已在完工状态，确认再次提交完工？`
        : `确认对布票【${ticket.value.code}】执行完工操作？`,
      confirmButtonText: '确认完工',
      cancelButtonText: '取消'
    })
  } catch {
    // 用户取消确认
    return
  }

  completeSubmitting.value = true
  try {
    const res = await completeTicket({
      code: ticket.value.code
    })
    if (res?.alreadyCompleted) {
      showToast({
        type: 'success',
        message: '布票已处于完工状态（幂等成功）'
      })
    } else {
      showToast({
        type: 'success',
        message: '完工成功！'
      })
    }
    await loadTicket(ticket.value.code)
  } catch (err: any) {
    const errInfo = formatSubmitError(err)
    showToast({
      type: 'fail',
      message: errInfo.message
    })
  } finally {
    completeSubmitting.value = false
  }
}

const openExceptionPopup = () => {
  if (!ticket.value) return
  loadExceptionDraft()
  exceptionPopupVisible.value = true
}

const handleExceptionSubmit = async () => {
  if (!ticket.value || exceptionSubmitting.value) return
  if (!exceptionForm.value.exceptionType) {
    showToast('请选择异常类型')
    return
  }

  exceptionSubmitting.value = true
  try {
    await exceptionTicket({
      code: ticket.value.code,
      exceptionType: exceptionForm.value.exceptionType,
      exceptionNote: exceptionForm.value.exceptionNote || undefined,
      exceptionPhotos:
        exceptionForm.value.exceptionPhotos.length > 0
          ? exceptionForm.value.exceptionPhotos
          : undefined
    })
    showToast({
      type: 'success',
      message: '异常登记成功！'
    })
    clearExceptionDraft()
    exceptionForm.value = {
      exceptionType: '',
      exceptionNote: '',
      exceptionPhotos: []
    }
    exceptionPopupVisible.value = false
    await loadTicket(ticket.value.code)
  } catch (err: any) {
    const errInfo = formatSubmitError(err)
    showToast({
      type: 'fail',
      message: errInfo.message
    })
  } finally {
    exceptionSubmitting.value = false
  }
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
