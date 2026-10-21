<template>
  <div class="h5-requisition-detail-page min-h-screen bg-gray-50 pb-20">
    <!-- 顶部导航栏 -->
    <van-nav-bar
      title="领纱单详情"
      left-arrow
      fixed
      placeholder
      safe-area-inset-top
      @click-left="goBack"
    />

    <!-- 顶部黄条提示：未配置默认仓库时显式提示管理员配置 -->
    <van-notice-bar
      v-if="requisition && shouldShowWarehouseWarning"
      left-icon="warning-o"
      color="#ed6a0c"
      background="#fffbe8"
      :text="warehouseWarningText"
      class="text-12px"
    />

    <!-- 内容区域 -->
    <div class="p-3">
      <!-- 1. 加载中状态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-gray-400">
        <van-loading type="spinner" color="#ff976a" size="32px" />
        <span class="text-13px mt-3">正在读取领纱单详情...</span>
      </div>

      <!-- 2. 错误/查无此单/无权操作状态 -->
      <div v-else-if="errorMessage" class="bg-white rounded-xl p-6 shadow-xs text-center my-4">
        <van-empty image="error" :description="errorMessage" />
        <div class="mt-4 max-w-xs mx-auto">
          <van-button type="primary" block round @click="goBack">返回领纱列表</van-button>
        </div>
      </div>

      <!-- 3. 领纱单详情展示 -->
      <div v-else-if="requisition" class="space-y-3">
        <!-- 主表头卡片 -->
        <div
          class="requisition-header-card bg-white rounded-xl p-4 shadow-xs border border-gray-100"
        >
          <div class="flex items-center justify-between pb-3 border-b border-gray-100">
            <div>
              <span class="text-11px text-gray-400">领纱单号</span>
              <div class="font-mono text-17px font-bold text-gray-900 mt-0.5 break-all">
                {{ requisition.code }}
              </div>
            </div>
            <van-tag :type="getStatusTagType(requisition.status)" round size="medium">
              {{ getStatusLabel(requisition.status) }}
            </van-tag>
          </div>

          <div class="grid grid-cols-2 gap-y-2.5 pt-3 text-13px">
            <div>
              <span class="text-gray-400">关联织造单：</span>
              <span class="font-medium text-gray-800 font-mono">{{
                requisition.weavingOrderCode || '-'
              }}</span>
            </div>
            <div>
              <span class="text-gray-400">创建时间：</span>
              <span class="text-gray-700">{{ requisition.createTime || '-' }}</span>
            </div>
            <div v-if="requisition.remark" class="col-span-2">
              <span class="text-gray-400">备注：</span>
              <span class="text-gray-700">{{ requisition.remark }}</span>
            </div>
          </div>
        </div>

        <!-- 仓库选择区 -->
        <div class="warehouse-card bg-white rounded-xl p-4 shadow-xs border border-gray-100">
          <div class="text-14px font-bold text-gray-900 mb-2">发纱仓库</div>

          <van-field
            v-model="selectedWarehouseName"
            is-link
            readonly
            label="当前仓库"
            placeholder="点击选择发纱仓库"
            class="p-0 text-14px"
            @click="showWarehousePicker = true"
          />
        </div>

        <!-- 领纱规格明细列表 -->
        <div class="items-section">
          <div class="flex items-center justify-between px-1 mb-2">
            <span class="text-14px font-bold text-gray-900">
              领纱规格明细
              <span class="text-12px font-normal text-gray-500"
                >({{ requisition.items ? requisition.items.length : 0 }}项)</span
              >
            </span>
          </div>

          <div class="space-y-3">
            <div
              v-for="item in requisition.items"
              :key="item.id"
              class="item-card bg-white rounded-xl p-4 shadow-xs border border-gray-100"
            >
              <!-- 规格标题与查余额按钮 -->
              <div class="flex items-center justify-between pb-2 border-b border-gray-50">
                <span class="font-bold text-15px text-gray-900">{{ item.yarnSpecName }}</span>
                <van-button
                  size="mini"
                  type="primary"
                  plain
                  round
                  :loading="balanceLoadingMap[item.yarnSpecId]"
                  @click="handleQueryBalance(item.yarnSpecId)"
                >
                  查余额
                </van-button>
              </div>

              <!-- 数量统计网格 -->
              <div class="grid grid-cols-3 gap-2 py-3 text-center bg-gray-50 rounded-lg my-2.5">
                <div>
                  <div class="text-11px text-gray-400">需求量</div>
                  <div class="text-14px font-bold text-gray-900 mt-0.5">
                    {{ item.requiredQty }}
                    <span class="text-10px font-normal text-gray-500">{{ item.unit }}</span>
                  </div>
                </div>
                <div>
                  <div class="text-11px text-gray-400">已发量</div>
                  <div class="text-14px font-bold text-green-600 mt-0.5">
                    {{ item.dispatchedQty }}
                    <span class="text-10px font-normal text-gray-500">{{ item.unit }}</span>
                  </div>
                </div>
                <div>
                  <div class="text-11px text-gray-400">待发量</div>
                  <div class="text-14px font-bold text-orange-600 mt-0.5">
                    {{ calculateRemainingQty(item.requiredQty, item.dispatchedQty) }}
                    <span class="text-10px font-normal text-gray-500">{{ item.unit }}</span>
                  </div>
                </div>
              </div>

              <!-- 库存余额反馈 -->
              <div
                v-if="itemBalanceMap[item.yarnSpecId] !== undefined"
                class="flex items-center justify-between px-2 py-1.5 bg-blue-50 text-blue-800 rounded text-12px mb-2"
              >
                <span>仓库可用余额：</span>
                <span class="font-bold font-mono text-13px"
                  >{{ itemBalanceMap[item.yarnSpecId] }} {{ item.unit }}</span
                >
              </div>

              <!-- 备注（如有） -->
              <div v-if="item.remark" class="text-12px text-gray-500 mb-2">
                备注：{{ item.remark }}
              </div>

              <!-- 逐行发纱表单 -->
              <div class="pt-2 border-t border-gray-50">
                <div class="text-12px font-medium text-gray-700 mb-1.5">本次发纱出库：</div>
                <div class="flex items-center space-x-2">
                  <van-field
                    v-model="dispatchQtyMap[item.id]"
                    type="number"
                    :disabled="isItemCompleted(item) || isOrderClosed"
                    :placeholder="isItemCompleted(item) ? '已全部发纱' : '输入发纱量（KG）'"
                    class="bg-gray-50 rounded-lg p-2 text-13px flex-1"
                    clearable
                    @update:model-value="() => onQtyChange(item.id)"
                  />
                  <van-button
                    v-hasPermi="['lunaerp:workshop:h5:dispatch']"
                    size="small"
                    type="primary"
                    :loading="dispatchLoadingMap[item.id]"
                    :disabled="isItemCompleted(item) || isOrderClosed"
                    round
                    class="shrink-0"
                    @click="handleDispatch(item)"
                  >
                    {{ isItemCompleted(item) ? '已发完' : '发纱出库' }}
                  </van-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 仓库选择弹出层 -->
    <van-popup v-model:show="showWarehousePicker" round position="bottom">
      <van-picker
        :columns="warehousePickerColumns"
        @confirm="onWarehouseConfirm"
        @cancel="showWarehousePicker = false"
      />
    </van-popup>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showNotify } from 'vant'
import { NETWORK_ERROR_MESSAGE } from '@/utils/h5-submit'
import {
  resolveDefaultWarehouse,
  getRequisitionItemDraft,
  getRequisitionDraft,
  saveRequisitionItemDraft,
  clearRequisitionItemDraft,
  getOrCreateItemRequestId,
  executeItemDispatch,
  DEFAULT_WAREHOUSE_WARNING_MESSAGE
} from './requisition-detail-helper'
import {
  getRequisition,
  getWarehouseSimpleList,
  getYarnBalance,
  dispatchRequisitionItem,
  type WorkshopRequisitionDetailVO,
  type WorkshopWarehouseSimpleVO,
  type WorkshopRequisitionItemVO
} from '@/api/h5/workshop'

defineOptions({ name: 'H5RequisitionDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const requisition = ref<WorkshopRequisitionDetailVO | null>(null)

// 默认仓库与顶部黄条提示
const shouldShowWarehouseWarning = ref(false)
const warehouseWarningText = ref(DEFAULT_WAREHOUSE_WARNING_MESSAGE)

// 发纱操作状态与持久化 requestId 映射
const dispatchQtyMap = ref<Record<number, string>>({})
const dispatchLoadingMap = ref<Record<number, boolean>>({})
const itemRequestIdMap = ref<Record<number, string>>({})

// 仓库选择
const warehouseList = ref<WorkshopWarehouseSimpleVO[]>([])
const selectedWarehouseId = ref<number | null>(null)
const selectedWarehouseName = ref('')
const showWarehousePicker = ref(false)

// 余额查询与缓存
const balanceLoadingMap = ref<Record<number, boolean>>({})
const itemBalanceMap = ref<Record<number, number>>({})

const warehousePickerColumns = computed(() => {
  return warehouseList.value.map((wh) => ({
    text: wh.name,
    value: wh.id
  }))
})

// 状态映射
const REQUISITION_STATUS_CONFIG: Record<
  string,
  { label: string; type: 'primary' | 'warning' | 'default' }
> = {
  ACTIVE: { label: '待发纱', type: 'primary' },
  PARTIAL: { label: '部分发纱', type: 'warning' }
}

const getStatusLabel = (status?: string) => {
  if (!status) return '-'
  return REQUISITION_STATUS_CONFIG[status]?.label || status
}

const getStatusTagType = (status?: string) => {
  if (!status) return 'default'
  return REQUISITION_STATUS_CONFIG[status]?.type || 'default'
}

const calculateRemainingQty = (requiredQty: number, dispatchedQty: number) => {
  const diff = Number(requiredQty || 0) - Number(dispatchedQty || 0)
  return diff > 0 ? Number(diff.toFixed(2)) : 0
}

const isItemCompleted = (item: WorkshopRequisitionItemVO) => {
  return Number(item.dispatchedQty || 0) >= Number(item.requiredQty || 0)
}

const isOrderClosed = computed(() => {
  const status = requisition.value?.status
  return status === 'DISPATCHED' || status === 'CLOSED'
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/h5/requisition')
  }
}

const onQtyChange = (itemId: number) => {
  const reqId = requisition.value?.id
  if (!reqId) return
  const qty = dispatchQtyMap.value[itemId]
  if (qty && qty.trim()) {
    const requestId = getOrCreateItemRequestId(reqId, itemId)
    itemRequestIdMap.value[itemId] = requestId
    saveRequisitionItemDraft({
      requisitionId: reqId,
      itemId,
      dispatchQty: qty,
      requestId,
      warehouseId: selectedWarehouseId.value
    })
  } else {
    clearRequisitionItemDraft(reqId, itemId)
    delete itemRequestIdMap.value[itemId]
  }
}

const loadData = async () => {
  const idStr = route.query.id as string
  if (!idStr) {
    errorMessage.value = '缺少领纱单ID参数'
    return
  }
  const requisitionId = Number(idStr)
  if (isNaN(requisitionId)) {
    errorMessage.value = '领纱单ID不合法'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    // 并行拉取领纱单详情与启用中仓库列表
    const [detailRes, warehouseRes] = await Promise.all([
      getRequisition(requisitionId),
      getWarehouseSimpleList()
    ])

    requisition.value = detailRes
    warehouseList.value = warehouseRes || []

    // 默认仓库设置：取 luna.workshop.default-warehouse-id，存在且启用才预选；未配置或未启用则不预选，顶部黄条提示管理员配置
    const whResult = resolveDefaultWarehouse({
      defaultWarehouseId: detailRes.defaultWarehouseId,
      warehouseList: warehouseList.value,
      currentSelectedWarehouseId: selectedWarehouseId.value
    })
    selectedWarehouseId.value = whResult.selectedWarehouseId
    selectedWarehouseName.value = whResult.selectedWarehouseName
    shouldShowWarehouseWarning.value = whResult.shouldShowWarningBanner
    warehouseWarningText.value = whResult.warningBannerText

    // 断网重试 / 刷新恢复表单 localStorage 草稿（按 id 分键，重试沿用同一 requestId）
    const reqDraft = getRequisitionDraft(requisitionId)
    if (!selectedWarehouseId.value && reqDraft?.warehouseId) {
      selectedWarehouseId.value = reqDraft.warehouseId
      const matched = warehouseList.value.find((w) => w.id === reqDraft.warehouseId)
      selectedWarehouseName.value = matched ? matched.name : `仓库 #${reqDraft.warehouseId}`
    }

    if (detailRes.items && detailRes.items.length > 0) {
      detailRes.items.forEach((item) => {
        // 若行项目已全部发纱完成，清理残留草稿
        if (isItemCompleted(item)) {
          clearRequisitionItemDraft(requisitionId, item.id)
          delete dispatchQtyMap.value[item.id]
          delete itemRequestIdMap.value[item.id]
          return
        }

        const draft = getRequisitionItemDraft(requisitionId, item.id)
        if (draft) {
          if (draft.dispatchQty && !dispatchQtyMap.value[item.id]) {
            dispatchQtyMap.value[item.id] = draft.dispatchQty
          }
          if (draft.requestId) {
            itemRequestIdMap.value[item.id] = draft.requestId
          }
        }
      })
    }
  } catch (e: any) {
    const msg = e?.message || '读取领纱单失败'
    errorMessage.value = msg
    showToast(msg)
  } finally {
    loading.value = false
  }
}

const handleDispatch = async (item: WorkshopRequisitionItemVO) => {
  if (!requisition.value) return

  // 1. 提交按钮提交中禁用防连点
  if (dispatchLoadingMap.value[item.id]) {
    return
  }

  // 2. warehouseId 必填校验（未配置或未预选时强制手选）
  if (!selectedWarehouseId.value) {
    showToast('请先选择发纱仓库')
    showWarehousePicker.value = true
    return
  }

  // 3. 数量必填校验
  const qtyStr = dispatchQtyMap.value[item.id]
  const qty = Number(qtyStr)
  if (!qtyStr || isNaN(qty) || qty <= 0) {
    showToast('请输入合法的发纱数量')
    return
  }

  // 4. 执行发纱出库（requestId 一次生成随表单持久化，重试沿用同一 id；与后端 @Idempotent 协同弱网双击只扣一次）
  const result = await executeItemDispatch({
    requisitionId: requisition.value.id,
    itemId: item.id,
    dispatchQty: qtyStr,
    warehouseId: selectedWarehouseId.value,
    dispatchApi: dispatchRequisitionItem,
    submittingMap: dispatchLoadingMap.value
  })

  if (result.ignored) {
    return
  }

  if (result.success) {
    showNotify({
      type: 'success',
      message: '发纱出库成功',
      duration: 2000
    })
    dispatchQtyMap.value[item.id] = ''
    delete itemRequestIdMap.value[item.id]
    // 重新拉取最新领纱单明细与已发数量，并更新余额
    await loadData()
    if (selectedWarehouseId.value) {
      await handleQueryBalance(item.yarnSpecId)
    }
  } else {
    // 网络错误与业务错误分色提示，网络错误文案"网络异常，单据未提交，可点重试"，不暗示已成功
    const errInfo = result.errorInfo
    if (errInfo?.isNetwork) {
      showNotify({
        type: 'warning',
        message: NETWORK_ERROR_MESSAGE,
        duration: 3500
      })
    } else {
      showNotify({
        type: 'danger',
        message: errInfo?.message || '发纱出库失败',
        duration: 2500
      })
    }
  }
}

const onWarehouseConfirm = ({ selectedOptions }: any) => {
  if (selectedOptions && selectedOptions[0]) {
    selectedWarehouseId.value = selectedOptions[0].value
    selectedWarehouseName.value = selectedOptions[0].text
    // 切换仓库时清空原余额缓存
    itemBalanceMap.value = {}

    // 同步更新已存草稿的 warehouseId
    const reqId = requisition.value?.id
    if (reqId) {
      Object.keys(dispatchQtyMap.value).forEach((itemIdStr) => {
        const itemId = Number(itemIdStr)
        const qty = dispatchQtyMap.value[itemId]
        if (qty && qty.trim()) {
          const reqItemDraft = getRequisitionItemDraft(reqId, itemId)
          const requestId = reqItemDraft?.requestId || getOrCreateItemRequestId(reqId, itemId)
          itemRequestIdMap.value[itemId] = requestId
          saveRequisitionItemDraft({
            requisitionId: reqId,
            itemId,
            dispatchQty: qty,
            requestId,
            warehouseId: selectedWarehouseId.value
          })
        }
      })
    }
  }
  showWarehousePicker.value = false
}

const handleQueryBalance = async (yarnSpecId: number) => {
  if (!selectedWarehouseId.value) {
    showToast('请先选择发纱仓库')
    showWarehousePicker.value = true
    return
  }

  balanceLoadingMap.value[yarnSpecId] = true
  try {
    const balance = await getYarnBalance(selectedWarehouseId.value, yarnSpecId)
    itemBalanceMap.value[yarnSpecId] = Number(balance ?? 0)
  } catch (e: any) {
    showToast(e?.message || '查询库存余额失败')
  } finally {
    balanceLoadingMap.value[yarnSpecId] = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.requisition-header-card,
.warehouse-card,
.item-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
</style>
