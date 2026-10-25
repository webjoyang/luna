<template>
  <div class="material-check-container p-4 flex flex-col gap-3">
    <!-- 顶部统一筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" class="flex flex-wrap items-center justify-between">
        <div class="flex flex-wrap items-center gap-y-2">
          <!-- 织造单多选 -->
          <el-form-item label="织造单">
            <div class="flex items-center gap-2">
              <el-select
                v-model="queryParams.weavingOrderIds"
                multiple
                filterable
                remote
                reserve-keyword
                collapse-tags
                collapse-tags-tooltip
                clearable
                placeholder="全部未完工织造单（可输入单号搜索）"
                :remote-method="searchWeavingOrders"
                :loading="searchingOrders"
                class="!w-72"
              >
                <el-option
                  v-for="order in weavingOrderOptions"
                  :key="order.id!"
                  :label="`${order.code}${order.productName ? ' (' + order.productName + ')' : ''}`"
                  :value="order.id!"
                />
              </el-select>
              <el-button type="primary" link @click="handleSelectAllActive"> 全选未完工 </el-button>
            </div>
          </el-form-item>

          <!-- 仓库多选 -->
          <el-form-item label="仓库">
            <el-select
              v-model="queryParams.warehouseIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="全部仓库"
              class="!w-56"
            >
              <el-option
                v-for="wh in warehouseOptions"
                :key="wh.id!"
                :label="wh.name"
                :value="wh.id!"
              />
            </el-select>
          </el-form-item>

          <!-- 含在途开关 -->
          <el-form-item label="含在途">
            <div class="flex items-center gap-1">
              <el-switch
                v-model="queryParams.includeInTransit"
                inline-prompt
                active-text="开"
                inactive-text="关"
                @change="handleIncludeInTransitChange"
              />
              <el-tooltip placement="top" effect="dark">
                <template #content>
                  <div class="max-w-360px leading-relaxed p-1">
                    <div class="font-bold mb-1">在途口径与已知限制（spec §4.4）：</div>
                    <ol class="list-decimal pl-4 space-y-1 text-12px">
                      <li v-for="(restriction, idx) in IN_TRANSIT_TOOLTIP_RESTRICTIONS" :key="idx">
                        {{ restriction }}
                      </li>
                    </ol>
                  </div>
                </template>
                <el-icon class="cursor-pointer text-gray-400 hover:text-primary">
                  <Icon icon="ep:info-filled" />
                </el-icon>
              </el-tooltip>
            </div>
          </el-form-item>

          <!-- 按钮组 -->
          <el-form-item>
            <el-button
              type="primary"
              :loading="calculating"
              @click="handleRunCheck"
              v-hasPermi="['lunaerp:production:material-check:query']"
            >
              <Icon icon="ep:search" class="mr-1" /> 试算
            </el-button>
            <el-button @click="resetQuery">
              <Icon icon="ep:refresh-right" class="mr-1" /> 重置
            </el-button>
          </el-form-item>
        </div>
      </el-form>
    </el-card>

    <!-- 顶部汇总徽标 -->
    <el-card shadow="never" class="summary-card">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-6">
          <div class="stat-item flex items-baseline gap-2">
            <span class="text-gray-500 text-14px">缺料规格数：</span>
            <span
              class="stat-val font-bold text-22px"
              :class="summaryStats.shortageSpecCount > 0 ? 'text-red-600' : 'text-green-600'"
            >
              {{ summaryStats.shortageSpecCount }}
            </span>
            <span class="text-gray-400 text-12px">种</span>
          </div>

          <div class="stat-item flex items-baseline gap-2">
            <span class="text-gray-500 text-14px">缺料合计：</span>
            <span
              class="stat-val font-bold text-22px"
              :class="summaryStats.totalShortageQty > 0 ? 'text-red-600' : 'text-green-600'"
            >
              {{ summaryStats.totalShortageQty.toFixed(2) }}
            </span>
            <span class="text-gray-400 text-12px">KG</span>
          </div>
        </div>

        <div class="flex items-center gap-2 text-13px text-gray-500">
          <span>当前统计口径：</span>
          <el-tag :type="queryParams.includeInTransit ? 'warning' : 'info'" size="small">
            {{ queryParams.includeInTransit ? '含在途采购量' : '不含在途采购量' }}
          </el-tag>
          <span class="text-12px text-gray-400">（两缺口列始终常驻展示）</span>
        </div>
      </div>
    </el-card>

    <!-- 无法试算分区 1：缺配比 BOM -->
    <el-alert
      v-if="resultData.missingBomOrders && resultData.missingBomOrders.length > 0"
      type="warning"
      show-icon
      :closable="false"
      class="missing-orders-alert"
    >
      <template #title>
        <span class="font-bold">
          以下 {{ resultData.missingBomOrders.length }} 张织造单缺少配比 BOM，未参与试算：
        </span>
      </template>
      <div class="mt-2 flex flex-col gap-1 max-h-48 overflow-y-auto">
        <div
          v-for="item in resultData.missingBomOrders"
          :key="item.weavingOrderId"
          class="flex items-center justify-between text-13px bg-amber-50/70 p-2 rounded"
        >
          <div class="flex items-center gap-3">
            <span class="font-mono font-medium">{{ item.weavingOrderCode }}</span>
            <span class="text-gray-600">
              产品：{{ item.productName || item.productCode || '未知产品' }}
            </span>
            <span class="text-amber-800 text-12px">（{{ item.reason || '未配置配比BOM' }}）</span>
          </div>
          <el-button link type="primary" size="small" @click="handleJumpToBom(item)">
            去配置 BOM
            <Icon icon="ep:arrow-right" class="ml-1" />
          </el-button>
        </div>
      </div>
    </el-alert>

    <!-- 无法试算分区 2：缺计划重量/匹数 -->
    <el-alert
      v-if="resultData.missingWeightOrders && resultData.missingWeightOrders.length > 0"
      type="warning"
      show-icon
      :closable="false"
      class="missing-orders-alert"
    >
      <template #title>
        <span class="font-bold">
          以下
          {{ resultData.missingWeightOrders.length }} 张织造单缺少计划重量或计划匹数，未参与试算：
        </span>
      </template>
      <div class="mt-2 flex flex-col gap-1 max-h-48 overflow-y-auto">
        <div
          v-for="item in resultData.missingWeightOrders"
          :key="item.weavingOrderId"
          class="flex items-center justify-between text-13px bg-amber-50/70 p-2 rounded"
        >
          <div class="flex items-center gap-3">
            <span class="font-mono font-medium">{{ item.weavingOrderCode }}</span>
            <span class="text-gray-600">
              计划匹数：{{ item.plannedQty ?? '未录入' }}，计划重量：{{
                item.plannedWeight != null ? item.plannedWeight + ' KG' : '未录入'
              }}
            </span>
            <span class="text-amber-800 text-12px"
              >（{{ item.reason || '缺少计划重量或匹数' }}）</span
            >
          </div>
          <el-button
            link
            type="primary"
            size="small"
            @click="handleJumpToWeavingOrder(item.weavingOrderId)"
          >
            去织造单补录
            <Icon icon="ep:arrow-right" class="ml-1" />
          </el-button>
        </div>
      </div>
    </el-alert>

    <!-- 试算结果表 -->
    <el-card shadow="never" class="result-table-card">
      <el-table
        v-loading="calculating"
        :data="resultData.rows"
        border
        stripe
        row-key="yarnSpecId"
        style="width: 100%"
      >
        <!-- 展开行：织造单贡献明细 -->
        <el-table-column type="expand" width="50">
          <template #default="{ row }">
            <div class="p-3 bg-gray-50/80 rounded">
              <div class="text-13px font-bold text-gray-700 mb-2">
                织造单需求贡献明细【{{ row.yarnSpecName }}】：
              </div>
              <el-table
                :data="row.contributions || []"
                size="small"
                border
                class="contribution-table"
              >
                <el-table-column label="织造单号" prop="weavingOrderCode" min-width="160">
                  <template #default="sub">
                    <span class="font-mono font-medium text-gray-700">
                      {{ sub.row.weavingOrderCode }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="产品编码" prop="productCode" min-width="120" />
                <el-table-column label="产品名称" prop="productName" min-width="140" />
                <el-table-column label="计划匹数" prop="plannedQty" width="100" align="right">
                  <template #default="sub">
                    {{ sub.row.plannedQty ?? '-' }}
                  </template>
                </el-table-column>
                <el-table-column
                  label="计划重量 (KG)"
                  prop="plannedWeight"
                  width="130"
                  align="right"
                >
                  <template #default="sub">
                    {{
                      sub.row.plannedWeight != null ? Number(sub.row.plannedWeight).toFixed(2) : '-'
                    }}
                  </template>
                </el-table-column>
                <el-table-column label="BOM配比" prop="ratio" width="110" align="right">
                  <template #default="sub">
                    {{ sub.row.ratio != null ? `${sub.row.ratio}%` : '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="本单需求 (KG)" prop="requiredQty" width="130" align="right">
                  <template #default="sub">
                    <span class="font-semibold text-primary">
                      {{ Number(sub.row.requiredQty || 0).toFixed(2) }}
                    </span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="纱线材质" prop="yarnTypeName" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.yarnTypeName || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="纱线规格" prop="yarnSpecName" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium">{{ row.yarnSpecName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="总需求量 (KG)" prop="requiredQty" width="130" align="right">
          <template #default="{ row }">
            <span class="font-semibold">{{ Number(row.requiredQty || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="可用库存 (KG)" prop="availableQty" width="130" align="right">
          <template #default="{ row }">
            <span>{{ Number(row.availableQty || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="在途采购量 (KG)" prop="inTransitQty" width="140" align="right">
          <template #default="{ row }">
            <span>{{ Number(row.inTransitQty || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column
          label="缺口 (不含在途) (KG)"
          prop="shortageExcludeInTransit"
          width="170"
          align="right"
        >
          <template #default="{ row }">
            <span
              :class="
                Number(row.shortageExcludeInTransit || 0) > 0
                  ? 'text-red-600 font-semibold'
                  : 'text-gray-400'
              "
            >
              {{ Number(row.shortageExcludeInTransit || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          label="缺口 (含在途) (KG)"
          prop="shortageIncludeInTransit"
          width="160"
          align="right"
        >
          <template #default="{ row }">
            <span
              :class="
                Number(row.shortageIncludeInTransit || 0) > 0
                  ? 'text-red-600 font-semibold'
                  : 'text-gray-400'
              "
            >
              {{ Number(row.shortageIncludeInTransit || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="建议采购量" prop="suggestedPurchaseQty" width="150" align="right">
          <template #default="{ row }">
            <span v-if="Number(row.suggestedPurchaseQty || 0) <= 0" class="text-gray-400 text-12px">
              {{ formatSuggestedPurchaseQty(row.suggestedPurchaseQty) }}
            </span>
            <span v-else class="font-bold text-red-600">
              {{ formatSuggestedPurchaseQty(row.suggestedPurchaseQty) }} KG
            </span>
          </template>
        </el-table-column>

        <el-table-column label="齐套状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="deriveMaterialCheckStatus(row).tagType" effect="light">
              {{ deriveMaterialCheckStatus(row).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="deriveMaterialCheckStatus(row).code === 'SHORTAGE'"
              link
              type="primary"
              @click="handleJumpToYarnOrder(row)"
            >
              去纱线采购单
              <Icon icon="ep:arrow-right" class="ml-1" />
            </el-button>
            <span v-else class="text-gray-300">—</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无试算结果，请选择织造单或点击试算" :image-size="100" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  runMaterialCheck,
  type MaterialCheckRespVO,
  type MaterialCheckRowVO,
  type MaterialCheckMissingOrderVO
} from '@/api/lunaerp-elmui/production/material-check'
import { getWarehouseListAll, type WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import {
  getWeavingOrderPage,
  type WeavingOrderVO
} from '@/api/lunaerp-elmui/production/weaving-order'
import {
  deriveMaterialCheckStatus,
  formatSuggestedPurchaseQty,
  calculateSummaryStats,
  buildYarnOrderLocation,
  buildYarnRecipeBomLocation,
  buildWeavingOrderWorkspaceLocation,
  IN_TRANSIT_TOOLTIP_RESTRICTIONS
} from './model/material-check'

defineOptions({ name: 'LunaerpElmuiProductionMaterialCheck' })

const router = useRouter()

const calculating = ref(false)
const searchingOrders = ref(false)

const warehouseOptions = ref<WarehouseVO[]>([])
const weavingOrderOptions = ref<WeavingOrderVO[]>([])

const queryParams = ref<{
  weavingOrderIds: number[]
  warehouseIds: number[]
  includeInTransit: boolean
}>({
  weavingOrderIds: [],
  warehouseIds: [],
  includeInTransit: false
})

const resultData = ref<MaterialCheckRespVO>({
  rows: [],
  missingBomOrders: [],
  missingWeightOrders: [],
  shortageSpecCount: 0,
  totalShortageQty: 0
})

/**
 * 汇总徽标计算：随 includeInTransit 开关动态切换口径
 */
const summaryStats = computed(() => {
  return calculateSummaryStats(resultData.value.rows || [], queryParams.value.includeInTransit)
})

/**
 * 远程搜索未完工织造单（DRAFT / ACTIVE）
 */
async function searchWeavingOrders(query: string) {
  searchingOrders.value = true
  try {
    const res = await getWeavingOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || undefined
    })
    const newOptions = (res.list || []).filter(
      (o: WeavingOrderVO) => o.status !== 'COMPLETED' && o.status !== 'CANCELLED'
    )

    // 保留已被勾选的织造单，避免远程搜索替换 options 导致已选标签退化为纯数字 ID
    const selectedMap = new Map<number, WeavingOrderVO>()
    for (const opt of weavingOrderOptions.value) {
      if (opt.id && queryParams.value.weavingOrderIds.includes(opt.id)) {
        selectedMap.set(opt.id, opt)
      }
    }
    for (const opt of newOptions) {
      if (opt.id) {
        selectedMap.set(opt.id, opt)
      }
    }
    weavingOrderOptions.value = Array.from(selectedMap.values())
  } catch (err) {
    // 远程搜索异常时保留已选项，避免清空已选标签
    weavingOrderOptions.value = weavingOrderOptions.value.filter(
      (o) => o.id && queryParams.value.weavingOrderIds.includes(o.id)
    )
  } finally {
    searchingOrders.value = false
  }
}

/**
 * "全选未完工"：清空织造单多选选中（后端契约：空列表即代表全部未完工），并重新试算
 */
function handleSelectAllActive() {
  queryParams.value.weavingOrderIds = []
  ElMessage.info('已切换为试算全部未完工织造单')
  handleRunCheck()
}

/**
 * 加载仓库选项
 */
async function loadWarehouseOptions() {
  try {
    const res = await getWarehouseListAll('ACTIVE')
    warehouseOptions.value = res || []
  } catch (err) {
    console.error('加载仓库选项失败:', err)
  }
}

/**
 * 执行齐套试算
 */
async function handleRunCheck() {
  calculating.value = true
  try {
    const res = await runMaterialCheck({
      weavingOrderIds:
        queryParams.value.weavingOrderIds.length > 0
          ? queryParams.value.weavingOrderIds
          : undefined,
      warehouseIds:
        queryParams.value.warehouseIds.length > 0 ? queryParams.value.warehouseIds : undefined,
      includeInTransit: queryParams.value.includeInTransit
    })

    resultData.value = {
      rows: res.rows || [],
      missingBomOrders: res.missingBomOrders || [],
      missingWeightOrders: res.missingWeightOrders || [],
      shortageSpecCount: res.shortageSpecCount ?? 0,
      totalShortageQty: res.totalShortageQty ?? 0
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '齐套检查试算失败')
  } finally {
    calculating.value = false
  }
}

/**
 * "含在途"开关变更处理（仅影响汇总徽标统计口径，两缺口列始终可见）
 */
function handleIncludeInTransitChange() {
  // summaryStats 具备响应式派生，开关变更即刻本地重算徽标，无需阻塞重载表格
}

/**
 * 重置筛选条件
 */
function resetQuery() {
  queryParams.value = {
    weavingOrderIds: [],
    warehouseIds: [],
    includeInTransit: false
  }
  handleRunCheck()
}

/**
 * 跳转至配比 BOM 管理页
 */
function handleJumpToBom(order: MaterialCheckMissingOrderVO) {
  const loc = buildYarnRecipeBomLocation(order.productId, order.productCode, order.productName)
  router.push(loc)
}

/**
 * 跳转至织造单工作台补录
 */
function handleJumpToWeavingOrder(weavingOrderId: number) {
  const loc = buildWeavingOrderWorkspaceLocation(weavingOrderId)
  router.push(loc)
}

/**
 * 缺料行跳转至纱线采购单
 */
function handleJumpToYarnOrder(row: MaterialCheckRowVO) {
  const loc = buildYarnOrderLocation(row.yarnSpecName)
  router.push(loc)
}

onMounted(() => {
  loadWarehouseOptions()
  searchWeavingOrders('')
  handleRunCheck()
})
</script>

<style scoped lang="scss">
.material-check-container {
  .filter-card :deep(.el-card__body) {
    padding: 14px 16px;
  }

  .summary-card :deep(.el-card__body) {
    padding: 12px 18px;
    background: var(--el-fill-color-light);
  }

  .missing-orders-alert :deep(.el-alert__description) {
    width: 100%;
  }

  .result-table-card :deep(.el-card__body) {
    padding: 12px;
  }
}
</style>
