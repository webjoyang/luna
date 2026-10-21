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

          <!-- 未配置默认仓库提示 -->
          <van-notice-bar
            v-if="!requisition.defaultWarehouseId"
            left-icon="warning-o"
            color="#ed6a0c"
            background="#fffbe8"
            text="未配置车间默认发纱仓库，请手动选择发纱仓库"
            class="rounded-lg shadow-xs mb-3 text-12px"
          />

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
import { showToast } from 'vant'
import { generateUUID } from '@/utils'
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

// 发纱操作状态
const dispatchQtyMap = ref<Record<number, string>>({})
const dispatchLoadingMap = ref<Record<number, boolean>>({})

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

    // 默认仓库设置：优先保留已选手选仓库；未手选时若返回了有效 defaultWarehouseId 则预选
    if (selectedWarehouseId.value) {
      const matched = warehouseList.value.find((w) => w.id === selectedWarehouseId.value)
      selectedWarehouseName.value = matched ? matched.name : `仓库 #${selectedWarehouseId.value}`
    } else if (detailRes.defaultWarehouseId) {
      selectedWarehouseId.value = detailRes.defaultWarehouseId
      const matched = warehouseList.value.find((w) => w.id === detailRes.defaultWarehouseId)
      selectedWarehouseName.value = matched ? matched.name : `仓库 #${detailRes.defaultWarehouseId}`
    } else {
      selectedWarehouseId.value = null
      selectedWarehouseName.value = ''
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
  if (!selectedWarehouseId.value) {
    showToast('请先选择发纱仓库')
    showWarehousePicker.value = true
    return
  }

  const qtyStr = dispatchQtyMap.value[item.id]
  const qty = Number(qtyStr)
  if (!qtyStr || isNaN(qty) || qty <= 0) {
    showToast('请输入合法的发纱数量')
    return
  }

  dispatchLoadingMap.value[item.id] = true
  try {
    // 每次点击一次性生成全新 UUID 作为 requestId 确保幂等拦截
    const requestId = generateUUID()
    await dispatchRequisitionItem({
      itemId: item.id,
      warehouseId: selectedWarehouseId.value,
      dispatchQty: qty,
      requestId
    })
    showToast('发纱出库成功')
    dispatchQtyMap.value[item.id] = ''
    // 重新拉取最新领纱单明细与已发数量，并更新余额
    await loadData()
    if (selectedWarehouseId.value) {
      await handleQueryBalance(item.yarnSpecId)
    }
  } catch (e: any) {
    showToast(e?.message || '发纱出库失败')
  } finally {
    dispatchLoadingMap.value[item.id] = false
  }
}

const onWarehouseConfirm = ({ selectedOptions }: any) => {
  if (selectedOptions && selectedOptions[0]) {
    selectedWarehouseId.value = selectedOptions[0].value
    selectedWarehouseName.value = selectedOptions[0].text
    // 切换仓库时清空原余额缓存
    itemBalanceMap.value = {}
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
