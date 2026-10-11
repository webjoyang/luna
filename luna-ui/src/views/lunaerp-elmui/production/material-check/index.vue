<template>
  <div class="material-check-container">
    <!-- 顶部筛选区 -->
    <ContentWrap>
      <el-form :inline="true" class="-mb-15px">
        <el-form-item label="织造单">
          <div class="flex items-center gap-2">
            <el-select
              v-model="queryParams.weavingOrderIds"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              :loading="ordersLoading"
              placeholder="选择织造单（默认试算全部未完工）"
              class="!w-320px"
              @change="handleFilterChange"
            >
              <el-option
                v-for="order in unfinishedOrders"
                :key="order.id"
                :label="`${order.code} - ${order.productName || '未命名'} (${order.status === 'DRAFT' ? '待排产' : '生产中'})`"
                :value="order.id"
              />
            </el-select>
            <el-button
              type="primary"
              plain
              size="default"
              @click="toggleSelectAllUnfinished"
            >
              {{ isAllUnfinishedSelected ? '清空织造单' : '全选未完工' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="仓库">
          <el-select
            v-model="queryParams.warehouseIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部仓库"
            class="!w-220px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="w in warehouseList"
              :key="w.id"
              :label="w.name"
              :value="w.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="含在途">
          <div class="flex items-center">
            <el-switch
              v-model="queryParams.includeInTransit"
              active-text="含在途"
              inactive-text="不含在途"
              inline-prompt
              @change="handleIncludeInTransitChange"
            />
            <el-tooltip placement="top" effect="dark" :raw-content="true">
              <template #content>
                <div class="leading-relaxed max-w-350px">
                  <div class="font-bold mb-1">在途口径已知限制说明：</div>
                  <div v-for="(limit, idx) in inTransitLimits" :key="idx" class="mb-1">
                    {{ limit }}
                  </div>
                </div>
              </template>
              <span class="inline-flex items-center ml-1 cursor-pointer text-gray-400 hover:text-primary">
                <Icon icon="ep:info-filled" />
              </span>
            </el-tooltip>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRunCheck">
            <Icon icon="ep:search" class="mr-1" /> 试算
          </el-button>
          <el-button @click="handleReset">
            <Icon icon="ep:refresh" class="mr-1" /> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 顶部汇总徽标区 -->
    <ContentWrap class="mt-4">
      <div class="flex items-center gap-6 py-1">
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-500 font-medium">缺料规格数：</span>
          <el-tag
            :type="summaryBadges.shortageSpecCount > 0 ? 'danger' : 'success'"
            effect="dark"
            size="large"
            class="font-bold"
          >
            {{ summaryBadges.shortageSpecCount }} 种
          </el-tag>
          <span class="text-xs text-gray-400">
            ({{ queryParams.includeInTransit ? '含在途口径' : '不含在途口径' }})
          </span>
        </div>

        <el-divider direction="vertical" class="h-6" />

        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-500 font-medium">缺料合计 KG：</span>
          <el-tag
            :type="summaryBadges.totalShortageKg > 0 ? 'danger' : 'success'"
            effect="dark"
            size="large"
            class="font-bold"
          >
            {{ summaryBadges.totalShortageKg.toFixed(2) }} KG
          </el-tag>
          <span class="text-xs text-gray-400">
            ({{ queryParams.includeInTransit ? '含在途口径' : '不含在途口径' }})
          </span>
        </div>

        <el-divider direction="vertical" class="h-6" />

        <div class="flex items-center gap-2 text-sm text-gray-500">
          <span>试算规格总数：</span>
          <span class="font-bold text-gray-800">{{ resultRows.length }} 种</span>
        </div>
      </div>
    </ContentWrap>

    <!-- 无法试算警示分区（el-alert 列表） -->
    <div v-if="missingBomOrders.length > 0 || missingWeightOrders.length > 0" class="mt-4 space-y-3">
      <!-- 缺配比 BOM -->
      <el-alert
        v-if="missingBomOrders.length > 0"
        type="warning"
        show-icon
        :closable="false"
        class="!border-amber-300 !bg-amber-50"
      >
        <template #title>
          <div class="font-bold text-amber-800 text-sm">
            以下 {{ missingBomOrders.length }} 张织造单缺少纱线配比 BOM，无法折算纱线需求，未参与试算聚合：
          </div>
        </template>
        <div class="mt-2 space-y-1.5">
          <div
            v-for="item in missingBomOrders"
            :key="item.weavingOrderId"
            class="flex items-center gap-3 text-xs text-gray-700 bg-white/70 px-2 py-1 rounded"
          >
            <span>织造单号：<strong>{{ item.weavingOrderCode }}</strong></span>
            <span>产品：{{ item.productName || item.productCode || '未命名产品' }}</span>
            <span class="text-gray-400">({{ item.reason || '未配置配比BOM' }})</span>
            <el-button
              link
              type="primary"
              size="small"
              class="!p-0 ml-auto"
              @click="jumpToBom(item)"
            >
              去配比 BOM <Icon icon="ep:arrow-right" class="ml-0.5" />
            </el-button>
          </div>
        </div>
      </el-alert>

      <!-- 缺计划重量/匹数 -->
      <el-alert
        v-if="missingWeightOrders.length > 0"
        type="warning"
        show-icon
        :closable="false"
        class="!border-amber-300 !bg-amber-50"
      >
        <template #title>
          <div class="font-bold text-amber-800 text-sm">
            以下 {{ missingWeightOrders.length }} 张织造单缺少计划重量或计划匹数，无法计算单匹需求，未参与试算聚合：
          </div>
        </template>
        <div class="mt-2 space-y-1.5">
          <div
            v-for="item in missingWeightOrders"
            :key="item.weavingOrderId"
            class="flex items-center gap-3 text-xs text-gray-700 bg-white/70 px-2 py-1 rounded"
          >
            <span>织造单号：<strong>{{ item.weavingOrderCode }}</strong></span>
            <span>原因：<span class="text-amber-700">{{ item.reason || '缺少计划重量或匹数' }}</span></span>
            <el-button
              link
              type="primary"
              size="small"
              class="!p-0 ml-auto"
              @click="jumpToWeavingOrder(item)"
            >
              去织造单 <Icon icon="ep:arrow-right" class="ml-0.5" />
            </el-button>
          </div>
        </div>
      </el-alert>
    </div>

    <!-- 结果表 -->
    <ContentWrap class="mt-4">
      <el-table
        v-loading="loading"
        :data="resultRows"
        row-key="yarnSpecId"
        border
        stripe
        style="width: 100%"
      >
        <!-- 展开行：展示各织造单贡献明细 -->
        <el-table-column type="expand" width="50">
          <template #default="{ row }">
            <div class="p-3 bg-gray-50/80 rounded">
              <div class="text-xs font-bold text-gray-600 mb-2 flex items-center">
                <Icon icon="ep:list" class="mr-1" />
                【{{ row.yarnSpecName || '该纱线规格' }}】各织造单需求贡献分解：
              </div>
              <el-table
                :data="row.contributions || []"
                size="small"
                border
                class="bg-white"
              >
                <el-table-column label="织造单号" prop="weavingOrderCode" min-width="150" align="center" />
                <el-table-column label="产品名称" prop="productName" min-width="160" align="center">
                  <template #default="cScope">
                    <span>{{ cScope.row.productName || '-' }}</span>
                    <span v-if="cScope.row.productCode" class="text-gray-400 text-xs ml-1">
                      ({{ cScope.row.productCode }})
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="计划重量 (KG)" prop="plannedWeight" width="130" align="right">
                  <template #default="cScope">
                    <span>{{ Number(cScope.row.plannedWeight ?? 0).toFixed(2) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="配比比例" prop="ratio" width="110" align="center">
                  <template #default="cScope">
                    <span>{{ cScope.row.ratio }}%</span>
                  </template>
                </el-table-column>
                <el-table-column label="本单需求 (KG)" prop="demandQty" width="130" align="right">
                  <template #default="cScope">
                    <span class="font-semibold text-gray-800">
                      {{ Number(cScope.row.demandQty ?? 0).toFixed(2) }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="纱线材质" prop="yarnTypeName" min-width="110" align="center">
          <template #default="{ row }">
            <span>{{ row.yarnTypeName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="纱线规格" prop="yarnSpecName" min-width="160" align="center">
          <template #default="{ row }">
            <span class="font-medium">{{ row.yarnSpecName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="需求量 (KG)" prop="demandQty" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-semibold">{{ Number(row.demandQty ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="可用库存 (KG)" prop="stockQty" min-width="120" align="right">
          <template #default="{ row }">
            <span>{{ Number(row.availableStock ?? row.stockQty ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="在途量 (KG)" prop="inTransitQty" min-width="120" align="right">
          <template #default="{ row }">
            <span :class="Number(row.inTransitQty ?? 0) > 0 ? 'text-blue-600' : 'text-gray-400'">
              {{ Number(row.inTransitQty ?? 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="缺口（不含在途）" prop="shortageWithoutInTransit" min-width="135" align="right">
          <template #default="{ row }">
            <span
              :class="Number(row.shortageWithoutInTransit ?? 0) > 0 ? 'text-red-500 font-bold' : 'text-gray-400'"
            >
              {{ Number(row.shortageWithoutInTransit ?? 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="缺口（含在途）" prop="shortageWithInTransit" min-width="135" align="right">
          <template #default="{ row }">
            <span
              :class="Number(row.shortageWithInTransit ?? 0) > 0 ? 'text-red-600 font-bold' : 'text-gray-400'"
            >
              {{ Number(row.shortageWithInTransit ?? 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="建议采购量" prop="suggestedPurchaseQty" min-width="130" align="right">
          <template #default="{ row }">
            <el-tag
              v-if="formatSuggestedQty(row.suggestedPurchaseQty).isZero"
              type="info"
              size="small"
            >
              已齐套/在途中
            </el-tag>
            <span v-else class="text-red-600 font-bold">
              {{ formatSuggestedQty(row.suggestedPurchaseQty).text }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="deriveMaterialStatus(row).tagType">
              {{ deriveMaterialStatus(row).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="Number(row.shortageWithInTransit ?? 0) > 0"
              link
              type="primary"
              size="small"
              @click="jumpToYarnOrder(row)"
            >
              去纱线采购单
            </el-button>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  runMaterialCheck,
  type MaterialCheckReqVO,
  type MaterialCheckRowVO,
  type MissingBomOrderVO,
  type MissingWeightOrderVO
} from '@/api/lunaerp-elmui/production/material-check'
import { getWeavingOrderPage, type WeavingOrderVO } from '@/api/lunaerp-elmui/production/weaving-order'
import { getWarehouseListAll, type WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import {
  deriveMaterialStatus,
  formatSuggestedQty,
  calculateSummaryBadges,
  IN_TRANSIT_LIMITS,
  getJumpToBomLocation,
  getJumpToWeavingOrderLocation,
  getJumpToYarnOrderLocation
} from './helper'

defineOptions({ name: 'LunaerpElmuiMaterialCheck' })

const router = useRouter()

// 状态定义
const loading = ref(false)
const ordersLoading = ref(false)
const unfinishedOrders = ref<WeavingOrderVO[]>([])
const warehouseList = ref<WarehouseVO[]>([])

const queryParams = reactive<{
  weavingOrderIds: number[]
  warehouseIds: number[]
  includeInTransit: boolean
}>({
  weavingOrderIds: [],
  warehouseIds: [],
  includeInTransit: false
})

const inTransitLimits = IN_TRANSIT_LIMITS

// 试算结果数据
const resultRows = ref<MaterialCheckRowVO[]>([])
const missingBomOrders = ref<MissingBomOrderVO[]>([])
const missingWeightOrders = ref<MissingWeightOrderVO[]>([])

// 顶部徽标统计：纯响应式随开关切换
const summaryBadges = computed(() => {
  return calculateSummaryBadges(resultRows.value, queryParams.includeInTransit)
})

// 是否全选了所有未完工单
const isAllUnfinishedSelected = computed(() => {
  if (unfinishedOrders.value.length === 0) return false
  return (
    queryParams.weavingOrderIds.length > 0 &&
    queryParams.weavingOrderIds.length === unfinishedOrders.value.length
  )
})

/** 全选/反选未完工织造单 */
const toggleSelectAllUnfinished = () => {
  if (isAllUnfinishedSelected.value) {
    queryParams.weavingOrderIds = []
  } else {
    queryParams.weavingOrderIds = unfinishedOrders.value
      .map((o) => o.id!)
      .filter((id): id is number => id !== undefined)
  }
  handleRunCheck()
}

/** 加载未完工织造单选项（DRAFT 待排产 + ACTIVE 生产中） */
const loadUnfinishedOrders = async () => {
  ordersLoading.value = true
  try {
    const [draftRes, activeRes] = await Promise.all([
      getWeavingOrderPage({ pageNo: 1, pageSize: 200, status: 'DRAFT' }),
      getWeavingOrderPage({ pageNo: 1, pageSize: 200, status: 'ACTIVE' })
    ])
    const draftList = draftRes?.list || []
    const activeList = activeRes?.list || []
    unfinishedOrders.value = [...draftList, ...activeList]
  } catch (err) {
    console.error('加载未完工织造单失败:', err)
  } finally {
    ordersLoading.value = false
  }
}

/** 加载仓库下拉列表 */
const loadWarehouses = async () => {
  try {
    const data = await getWarehouseListAll('ACTIVE')
    warehouseList.value = data || []
  } catch (err) {
    console.error('加载仓库列表失败:', err)
  }
}

/** 执行齐套检查试算 */
const handleRunCheck = async () => {
  loading.value = true
  try {
    const req: MaterialCheckReqVO = {
      weavingOrderIds:
        queryParams.weavingOrderIds.length > 0 ? queryParams.weavingOrderIds : undefined,
      warehouseIds:
        queryParams.warehouseIds.length > 0 ? queryParams.warehouseIds : undefined,
      includeInTransit: queryParams.includeInTransit
    }
    const resp = await runMaterialCheck(req)
    resultRows.value = resp?.rows || []
    missingBomOrders.value = resp?.missingBomOrders || []
    missingWeightOrders.value = resp?.missingWeightOrders || []
  } catch (err) {
    console.error('试算齐套检查失败:', err)
  } finally {
    loading.value = false
  }
}

/** 筛选变化处理 */
const handleFilterChange = () => {
  handleRunCheck()
}

/** 含在途开关切换处理 */
const handleIncludeInTransitChange = () => {
  // 徽标通过 computed 响应式切换；同时也可更新后端缓存或保持同步
}

/** 重置筛选 */
const handleReset = () => {
  queryParams.weavingOrderIds = []
  queryParams.warehouseIds = []
  queryParams.includeInTransit = false
  handleRunCheck()
}

/** 跳转配比 BOM 页面 */
const jumpToBom = (order: MissingBomOrderVO) => {
  const loc = getJumpToBomLocation(order)
  router.push(loc)
}

/** 跳转织造单工作台 */
const jumpToWeavingOrder = (order: MissingWeightOrderVO) => {
  const loc = getJumpToWeavingOrderLocation(order)
  router.push(loc)
}

/** 跳转纱线采购单 */
const jumpToYarnOrder = (row: MaterialCheckRowVO) => {
  const loc = getJumpToYarnOrderLocation(row)
  router.push(loc)
}

onMounted(async () => {
  await Promise.all([loadUnfinishedOrders(), loadWarehouses()])
  await handleRunCheck()
})
</script>

<style scoped>
.material-check-container {
  padding: 0;
}
</style>
