<template>
  <div class="h5-inspection-order-page min-h-screen bg-gray-50 pb-20">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="检验单详情"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      @click-left="goBack"
    />

    <!-- 内容区域 -->
    <div class="p-3">
      <!-- 1. 加载中状态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-gray-400">
        <van-loading type="spinner" color="#1989fa" size="32px" />
        <span class="text-13px mt-3">正在读取检验单详情...</span>
      </div>

      <!-- 2. 错误/查无此单状态 -->
      <div v-else-if="errorMessage" class="bg-white rounded-xl p-6 shadow-xs text-center my-4">
        <van-empty image="error" :description="errorMessage" />
        <div class="mt-4 max-w-xs mx-auto">
          <van-button type="primary" block round @click="goBack"> 返回质检列表 </van-button>
        </div>
      </div>

      <!-- 3. 检验单详情展示 -->
      <div v-else-if="order" class="space-y-3">
        <!-- 只读提示 Banner（已完成单据只读） -->
        <van-notice-bar
          v-if="order.readonly"
          left-icon="info-o"
          color="#07c160"
          background="#edfbf3"
          text="提示：该检验单已录检完成，当前处于只读模式"
          class="rounded-lg shadow-xs"
        />
        <van-notice-bar
          v-else
          left-icon="records-o"
          color="#1989fa"
          background="#ecf9ff"
          text="提示：检验单录检中，支持现场查看与拍照核验"
          class="rounded-lg shadow-xs"
        />

        <!-- 主表头卡片 -->
        <div class="order-header-card bg-white rounded-xl p-4 shadow-xs border border-gray-100">
          <div class="flex items-center justify-between pb-3 border-b border-gray-100">
            <div>
              <span class="text-11px text-gray-400">检验单号</span>
              <div class="font-mono text-17px font-bold text-gray-900 mt-0.5 break-all">
                {{ order.code }}
              </div>
            </div>
            <div class="flex items-center space-x-1.5">
              <van-tag
                :color="order.type === 'GREIGE' ? '#1989fa' : '#7232dd'"
                plain
                round
                size="medium"
              >
                {{ order.type === 'GREIGE' ? '胚布' : '成品' }}
              </van-tag>
              <van-tag :type="getStatusTagType(order.status)" round size="medium">
                {{ getStatusLabel(order.status) }}
              </van-tag>
            </div>
          </div>

          <!-- 核心字段网格 -->
          <div class="grid grid-cols-2 gap-3 pt-3 text-13px">
            <div>
              <span class="text-gray-400 text-12px block">产品品名</span>
              <div class="font-medium text-gray-800 mt-0.5 truncate">
                {{ order.productName || '—' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">产品编码</span>
              <div class="font-mono text-gray-800 mt-0.5 truncate">
                {{ order.productCode || '—' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">承检方</span>
              <div class="text-gray-800 mt-0.5 truncate">
                {{ order.inspectionFactoryName || '本厂' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">包装模式</span>
              <div class="text-gray-800 mt-0.5">
                {{ order.packageMode === 'PIECE' ? '匹装 (PIECE)' : '卷装 (ROLL)' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">目标重量</span>
              <div class="font-mono text-gray-800 mt-0.5">
                {{ order.targetWeight ? order.targetWeight + ' kg' : '未定' }}
              </div>
            </div>
            <div>
              <span class="text-gray-400 text-12px block">单据日期</span>
              <div class="text-gray-800 mt-0.5 font-mono">
                {{ order.orderDate || '—' }}
              </div>
            </div>
          </div>

          <!-- 备注 -->
          <div v-if="order.remark" class="mt-3 pt-2.5 border-t border-gray-50 text-12px">
            <span class="text-gray-400">备注：</span>
            <span class="text-gray-600">{{ order.remark }}</span>
          </div>
        </div>

        <!-- 胚布扫码录检入口与占位卡片 -->
        <div
          v-if="order.type === 'GREIGE'"
          class="mode-action-card bg-white rounded-xl p-4 shadow-xs border border-primary/30"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center space-x-2">
              <div
                class="w-8 h-8 rounded-full bg-blue-50 text-primary flex items-center justify-center"
              >
                <van-icon name="scan" size="18" />
              </div>
              <div>
                <span class="text-14px font-bold text-gray-900">胚布扫码录检</span>
                <span class="text-11px text-primary block mt-0.5">
                  已接入 14 号 ticket-scan 自动交接编排
                </span>
              </div>
            </div>
            <van-button
              v-hasPermi="['lunaerp:workshop:h5:inspection']"
              v-if="!order.readonly"
              type="primary"
              size="small"
              icon="scan"
              round
              @click="triggerScanner"
            >
              扫布票码
            </van-button>
          </div>

          <!-- 扫码定位到新行占位提示 -->
          <div
            v-if="currentScannedTicket && !currentScannedItem"
            class="scan-hit-alert bg-amber-50 border border-amber-200 rounded-lg p-3 text-12px text-amber-900"
          >
            <div class="flex items-center font-bold mb-1">
              <van-icon name="passed" color="#ed6a0c" class="mr-1" />
              已自动交接建单！待录检新行占位
            </div>
            <div
              >布票编号：<span class="font-mono font-medium">{{ currentScannedTicket }}</span></div
            >
            <div
              >建议匹号：<span class="font-mono font-medium">{{
                suggestedPieceNo || currentScannedTicket
              }}</span></div
            >
            <div class="text-amber-600 mt-1"
              >（录检明细等级、重量、米数与拍照保存表单待 15 号工单接入）</div
            >
          </div>

          <!-- 扫码定位到已有录检行提示 -->
          <div
            v-else-if="currentScannedTicket && currentScannedItem"
            class="scan-hit-alert bg-blue-50 border border-blue-200 rounded-lg p-3 text-12px text-blue-900"
          >
            <div class="flex items-center font-bold mb-1">
              <van-icon name="checked" color="#1989fa" class="mr-1" />
              已定位到布票既有录检行
            </div>
            <div
              >布票编号：<span class="font-mono font-medium">{{ currentScannedTicket }}</span></div
            >
            <div
              >匹号：<span class="font-mono font-medium">{{
                currentScannedItem.pieceNo || currentScannedTicket
              }}</span></div
            >
            <div class="text-blue-600 mt-1"
              >（下方列表已高亮该明细，改录保存表单待 15 号工单接入）</div
            >
          </div>

          <p v-else class="text-12px text-gray-500 bg-gray-50 rounded-lg p-2.5">
            扫描挡车工完工布票二维码，系统自动归一关联胚布检验单并定位录检行。
          </p>
        </div>

        <!-- 成品模式骨架占位 -->
        <div
          v-else
          class="mode-placeholder-card bg-white rounded-xl p-4 shadow-xs border border-dashed border-primary/40"
        >
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center space-x-2">
              <div
                class="w-8 h-8 rounded-full bg-blue-50 text-primary flex items-center justify-center"
              >
                <van-icon name="edit" size="18" />
              </div>
              <div>
                <span class="text-14px font-bold text-gray-900">成品逐卷录检</span>
                <span class="text-11px text-primary block mt-0.5">
                  模式入口骨架（待 16 号 Ticket 接入）
                </span>
              </div>
            </div>
            <van-tag type="primary" plain round size="small">ROLL</van-tag>
          </div>
          <p class="text-12px text-gray-500 bg-gray-50 rounded-lg p-2.5">
            支持现场逐卷录入缸号、卷号、等级、实称重量、米数与瑕疵照片。
          </p>
        </div>

        <!-- 录检明细列表区域（含 photos 照片展示） -->
        <div class="items-section">
          <div class="flex items-center justify-between px-1 mb-2">
            <div class="text-14px font-bold text-gray-900 flex items-center space-x-1.5">
              <span>录检明细</span>
              <span class="text-12px text-gray-400 font-normal">
                (已录 {{ order.items?.length || 0 }}
                {{ order.packageMode === 'PIECE' ? '匹' : '卷' }})
              </span>
            </div>
          </div>

          <!-- 明细列表 -->
          <div v-if="order.items && order.items.length > 0" class="space-y-2.5">
            <div
              v-for="(item, index) in order.items"
              :key="item.id || index"
              :class="[
                'item-card bg-white rounded-xl p-3.5 shadow-xs border transition-all',
                isItemHighlighted(item)
                  ? 'border-primary ring-2 ring-primary/20 bg-blue-50/20'
                  : 'border-gray-100'
              ]"
            >
              <!-- 头部：卷号/匹号 + 等级 -->
              <div class="flex items-center justify-between pb-2 border-b border-gray-100">
                <div class="flex items-center space-x-2">
                  <span class="text-14px font-bold text-gray-900 font-mono">
                    {{
                      item.rollNo
                        ? '卷号: ' + item.rollNo
                        : item.pieceNo
                          ? '匹号: ' + item.pieceNo
                          : '第 ' + (index + 1) + ' 项'
                    }}
                  </span>
                  <span
                    v-if="item.vatNo"
                    class="text-11px text-gray-500 font-mono bg-gray-100 px-1.5 py-0.5 rounded"
                  >
                    缸号: {{ item.vatNo }}
                  </span>
                </div>
                <van-tag :type="getGradeTagType(item.grade)" round size="medium">
                  {{ getGradeLabel(item.grade) }}
                </van-tag>
              </div>

              <!-- 明细物理参数 -->
              <div class="grid grid-cols-3 gap-2 py-2 text-12px">
                <div>
                  <span class="text-gray-400 block">实称重量</span>
                  <span class="font-medium text-gray-800 font-mono">
                    {{ item.measuredWeight != null ? item.measuredWeight + ' kg' : '—' }}
                  </span>
                </div>
                <div>
                  <span class="text-gray-400 block">米数</span>
                  <span class="font-medium text-gray-800 font-mono">
                    {{ item.lengthM != null ? item.lengthM + ' m' : '—' }}
                  </span>
                </div>
                <div>
                  <span class="text-gray-400 block">机台号</span>
                  <span class="font-medium text-gray-800 font-mono">
                    {{ item.machineNo || '—' }}
                  </span>
                </div>
              </div>

              <!-- 备注 -->
              <div v-if="item.remark" class="text-12px text-gray-500 pt-1 border-t border-gray-50">
                <span class="text-gray-400">备注: </span>{{ item.remark }}
              </div>

              <!-- 瑕疵现场照片区域（含 photos 预览） -->
              <div
                v-if="item.photos && item.photos.length > 0"
                class="photos-section mt-2 pt-2 border-t border-gray-50"
              >
                <div class="flex items-center justify-between text-11px text-gray-400 mb-1.5">
                  <span class="flex items-center">
                    <van-icon name="photograph" class="mr-1" />
                    现场照片 ({{ item.photos.length }} 张)
                  </span>
                  <span>点击可放大查看</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <van-image
                    v-for="(photoUrl, pIndex) in item.photos"
                    :key="pIndex"
                    :src="photoUrl"
                    width="64px"
                    height="64px"
                    fit="cover"
                    radius="6px"
                    class="border border-gray-200 cursor-pointer shadow-2xs"
                    @click="handlePreviewPhotos(item.photos, pIndex)"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 空明细状态 -->
          <van-empty
            v-else
            description="暂无录检明细，待现场扫码/逐卷录入"
            class="bg-white rounded-xl shadow-xs py-8"
          />
        </div>
      </div>
    </div>

    <!-- 底部/弹窗扫码组件 -->
    <H5Scanner ref="scannerRef" @scan="handleTicketScan" />
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showImagePreview, showLoadingToast, closeToast } from 'vant'
import H5Scanner from '../components/Scanner.vue'
import {
  getInspection,
  ticketScan,
  WorkshopInspectionDetailVO,
  WorkshopInspectionItemVO
} from '@/api/h5/workshop'

defineOptions({ name: 'H5InspectionOrder' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const order = ref<WorkshopInspectionDetailVO | null>(null)

// 扫码定位上下文
const scannerRef = ref()
const currentScannedTicket = ref<string>('')
const currentWeavingTicketId = ref<number | null>(null)
const currentItemId = ref<number | null>(null)
const suggestedPieceNo = ref<string>('')
const currentScannedItem = ref<WorkshopInspectionItemVO | null>(null)

const triggerScanner = () => {
  scannerRef.value?.openScanner?.()
}

// 判断明细卡片是否为当前扫码定位命中的行（基于实体 ID 精确判定）
const isItemHighlighted = (item: WorkshopInspectionItemVO) => {
  if (currentItemId.value && item.id === currentItemId.value) {
    return true
  }
  if (currentWeavingTicketId.value && item.weavingTicketId === currentWeavingTicketId.value) {
    return true
  }
  return false
}

// 内部扫码布票处理
const handleTicketScan = async (code: string) => {
  const cleanCode = (code || '').trim()
  if (!cleanCode) return

  showLoadingToast({
    message: '正在识别布票并定位...',
    forbidClick: true,
    duration: 0
  })

  try {
    const res = await ticketScan(cleanCode)
    closeToast()
    if (res) {
      currentScannedTicket.value = cleanCode
      currentWeavingTicketId.value = res.weavingTicketId || null
      currentItemId.value = res.item?.id || null
      suggestedPieceNo.value = res.suggestedPieceNo || cleanCode
      currentScannedItem.value = res.item || null

      if (res.inspectionOrderId !== order.value?.id) {
        // 如果扫描的布票归属于另一张检验单，自动路由至对应检验单
        router.push({
          path: '/h5/inspection/order',
          query: {
            id: res.inspectionOrderId,
            ticketCode: cleanCode,
            ticketId: res.weavingTicketId,
            itemId: res.item?.id,
            suggestedPieceNo: res.suggestedPieceNo || cleanCode
          }
        })
      } else {
        await loadOrderDetail(false)
        showToast({
          type: 'success',
          message: res.item ? '已定位到已录检明细' : '已自动交接，定位待录检行'
        })
      }
    }
  } catch (err: any) {
    closeToast()
    showToast({
      type: 'fail',
      message: err?.message || '布票扫码识别失败'
    })
  }
}

// 状态映射
const getStatusLabel = (status?: string) => {
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
      return status || '—'
  }
}

const getStatusTagType = (
  status?: string
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

// 等级映射
const getGradeLabel = (grade?: string) => {
  switch (grade) {
    case 'A':
      return '一等品 (A)'
    case 'B':
      return '二等品 (B)'
    case 'C':
      return '三等品 (C)'
    case 'DEFECTIVE':
      return '等外品 (次品)'
    default:
      return grade || '未评级'
  }
}

const getGradeTagType = (
  grade?: string
): 'default' | 'primary' | 'success' | 'warning' | 'danger' => {
  switch (grade) {
    case 'A':
      return 'success'
    case 'B':
      return 'primary'
    case 'C':
      return 'warning'
    case 'DEFECTIVE':
      return 'danger'
    default:
      return 'default'
  }
}

// 图片预览
const handlePreviewPhotos = (photos?: string[], startPosition = 0) => {
  if (!photos || photos.length === 0) return
  showImagePreview({
    images: photos,
    startPosition
  })
}

// 加载检验单数据
const loadOrderDetail = async (showLoading = true) => {
  const idStr = route.query.id as string
  if (!idStr || isNaN(Number(idStr))) {
    errorMessage.value = '未指定检验单编号，请从列表进入'
    return
  }

  const orderId = Number(idStr)
  if (showLoading) {
    loading.value = true
  }
  errorMessage.value = ''

  try {
    const res = await getInspection(orderId)
    if (!res) {
      errorMessage.value = '检验单不存在或无权访问'
      return
    }
    order.value = res

    // 匹配路由传参带来的扫码定位状态
    const scanTicket = (route.query.ticketCode as string) || (route.query.scanTicketCode as string)
    const ticketIdStr = route.query.ticketId as string
    const itemIdStr = route.query.itemId as string
    if (scanTicket) {
      currentScannedTicket.value = scanTicket
      suggestedPieceNo.value = (route.query.suggestedPieceNo as string) || scanTicket
      currentWeavingTicketId.value = ticketIdStr ? Number(ticketIdStr) : null
      currentItemId.value = itemIdStr ? Number(itemIdStr) : null

      if (res.items && res.items.length > 0) {
        const found = res.items.find(
          (it) =>
            (currentItemId.value && it.id === currentItemId.value) ||
            (currentWeavingTicketId.value && it.weavingTicketId === currentWeavingTicketId.value)
        )
        if (found) {
          currentScannedItem.value = found
        }
      }
    }
  } catch (err: any) {
    errorMessage.value = err?.message || '读取检验单详情失败'
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

// 监听路由 id 变化，支持由当前单跳入另一张检验单时重新加载
watch(
  () => route.query.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      loadOrderDetail()
    }
  }
)

// 返回列表页
const goBack = () => {
  router.push('/h5/inspection')
}

onMounted(() => {
  loadOrderDetail()
})
</script>

<style lang="scss" scoped>
.h5-inspection-order-page {
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
