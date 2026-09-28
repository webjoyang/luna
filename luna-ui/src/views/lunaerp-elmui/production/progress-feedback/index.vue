<template>
  <div class="progress-feedback-container">
    <!-- 顶部统计条 -->
    <div class="statistics-bar">
      <div
        class="stat-card"
        :class="{ 'is-active': queryParams.status === 'NOT_SCHEDULED' }"
        @click="handleStatClick('NOT_SCHEDULED')"
      >
        <div class="stat-icon unscheduled">
          <el-icon><DocumentRemove /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.unscheduledColorCount }}</div>
          <div class="stat-label">未排缸颜色行</div>
        </div>
      </div>

      <div
        class="stat-card"
        :class="{ 'is-active': isProgressStatActive }"
        @click="handleStatClick('IN_PROGRESS')"
      >
        <div class="stat-icon in-progress">
          <el-icon><Odometer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.inProgressVatCount }}</div>
          <div class="stat-label">进行中缸数</div>
        </div>
      </div>

      <div
        class="stat-card"
        :class="{ 'is-active': queryParams.status === 'COMPLETED' }"
        @click="handleStatClick('COMPLETED')"
      >
        <div class="stat-icon completed">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.completedVatCount }}</div>
          <div class="stat-label">已完成缸数</div>
        </div>
      </div>

      <div
        class="stat-card"
        :class="{ 'is-active': queryParams.delayedOnly }"
        @click="handleStatClick('DELAYED')"
      >
        <div class="stat-icon delayed">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.delayedVatCount }}</div>
          <div class="stat-label">延期缸数</div>
        </div>
      </div>

      <div
        class="stat-card"
        :class="{ 'is-active': queryParams.unresolvedOnly }"
        @click="handleStatClick('UNRESOLVED')"
      >
        <div class="stat-icon unresolved">
          <el-icon><ChatDotSquare /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.unresolvedVatCount || 0 }}</div>
          <div class="stat-label">未决问题缸数</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="78px">
        <el-form-item label="染色单" prop="dyeingOrderId">
          <el-select
            v-model="queryParams.dyeingOrderId"
            placeholder="搜索染色单"
            clearable
            filterable
            remote
            :remote-method="searchDyeingOrders"
            :loading="dyeingOrderLoading"
            style="width: 220px"
          >
            <el-option
              v-for="item in dyeingOrderOptions"
              :key="item.id"
              :label="`#${item.id} ${item.contractNo ? '- ' + item.contractNo : ''} (${item.dyeingFactoryName || ''})`"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="销售合同" prop="contractNo">
          <el-input
            v-model="queryParams.contractNo"
            placeholder="请输入合同号"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="承染厂" prop="dyeingFactoryId">
          <el-select
            v-model="queryParams.dyeingFactoryId"
            placeholder="搜索承染方"
            clearable
            filterable
            remote
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            style="width: 200px"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="进度状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关键词" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="色号/颜色/缸号/合同"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="仅看延期" prop="delayedOnly">
          <el-switch v-model="queryParams.delayedOnly" @change="handleQuery" />
        </el-form-item>

        <el-form-item label="仅看未决" prop="unresolvedOnly">
          <el-switch v-model="queryParams.unresolvedOnly" @change="handleQuery" />
        </el-form-item>

        <el-form-item label="含已完成" prop="includeCompleted">
          <el-switch v-model="queryParams.includeCompleted" @change="handleQuery" />
        </el-form-item>

        <el-form-item label="含已失效" prop="includeDeleted">
          <el-switch v-model="queryParams.includeDeleted" @change="handleQuery" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button
            v-hasPermi="['lunaerp:production:progress-feedback:create']"
            type="success"
            plain
            :icon="Plus"
            @click="handleCreateVatStandalone"
          >
            新建缸
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 物料单元快速定位条（按染色单/合同+产品聚合） -->
    <div v-if="materialUnits.length > 0" class="material-locator-card">
      <div class="locator-header">
        <div class="header-left">
          <el-icon class="locator-icon"><Position /></el-icon>
          <span class="locator-title">物料单元快速定位</span>
          <span class="locator-tip">（按染色单/合同+产品聚合，点击切换并滚动定位至目标颜色行节）</span>
        </div>
        <div class="header-right">
          <el-button
            v-if="queryParams.dyeingOrderId"
            size="small"
            text
            type="primary"
            @click="handleSelectMaterialUnit(undefined)"
          >
            显示全部物料 ({{ totalMaterialCount }})
          </el-button>
        </div>
      </div>
      <div class="locator-scroll-box">
        <div
          class="locator-item"
          :class="{ 'is-active': !queryParams.dyeingOrderId }"
          @click="handleSelectMaterialUnit(undefined)"
        >
          <span class="item-title">全部物料</span>
          <span class="item-badge">{{ totalMaterialCount }}</span>
        </div>
        <div
          v-for="unit in materialUnits"
          :key="unit.dyeingOrderId"
          class="locator-item"
          :class="{
            'is-active': queryParams.dyeingOrderId === unit.dyeingOrderId,
            'is-delayed': unit.hasDelayed,
            'is-unresolved': unit.hasUnresolved
          }"
          :title="`${unit.contractNo || '未绑定合同'} · ${unit.productName || unit.productCode || '未知产品'} · ${unit.dyeingFactoryName || '-'}`"
          @click="handleSelectMaterialUnit(unit.dyeingOrderId)"
        >
          <div class="item-top">
            <span class="order-id">#{{ unit.dyeingOrderId }}</span>
            <span class="contract-no" v-if="unit.contractNo">{{ unit.contractNo }}</span>
            <span class="factory-name" v-if="unit.dyeingFactoryName">· {{ unit.dyeingFactoryName }}</span>
          </div>
          <div class="item-bottom">
            <span class="product-name">{{ unit.productName || unit.productCode || '无产品信息' }}</span>
            <span v-if="unit.colorCount" class="color-count">{{ unit.colorCount }}色</span>
            <span v-if="unit.hasDelayed" class="status-dot delayed" title="含延期缸">延期</span>
            <span v-if="unit.hasUnresolved" class="status-dot unresolved" title="含未决问题">未决</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 缸卡墙内容区 -->
    <div v-loading="loading" class="wall-content">
      <template v-if="sectionList.length > 0">
        <ColorSection
          v-for="section in sectionList"
          :key="section.dyeingColorId"
          :section="section"
          @create-vat="handleCreateVatForSection"
          @open-detail="handleOpenVatDetail"
          @open-feedback="handleOpenFeedback"
          @refresh="handleRefresh"
        />

        <!-- 分页条 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="queryParams.pageNo"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </template>

      <el-empty v-else-if="!loading" description="暂无进度反馈数据" :image-size="120" />
    </div>

    <!-- 新建缸弹窗 -->
    <VatCreateDialog ref="vatCreateDialogRef" @success="handleCreateSuccess" />

    <!-- 缸详情与工序时间轴抽屉 -->
    <VatDetailDrawer ref="vatDetailDrawerRef" @success="handleDetailSuccess" />

    <!-- 缸卡快捷反馈弹窗 -->
    <FeedbackDialog ref="feedbackDialogRef" @success="handleFeedbackSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import {
  Search,
  Refresh,
  Odometer,
  CircleCheck,
  Warning,
  DocumentRemove,
  ChatDotSquare,
  Position,
  Plus
} from '@element-plus/icons-vue'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as DyeingOrderApi from '@/api/lunaerp-elmui/production/dyeing-order'
import type { DyeingOrderVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import ColorSection from './components/ColorSection.vue'
import VatCreateDialog from './components/VatCreateDialog.vue'
import VatDetailDrawer from './components/VatDetailDrawer.vue'
import FeedbackDialog from './components/FeedbackDialog.vue'
import {
  getProgressFeedbackPage,
  getProgressFeedbackStatistics,
  type ProgressFeedbackColorSectionVO,
  type ProgressFeedbackStatisticsVO,
  type ProgressFeedbackVatVO,
  type ProgressFeedbackPageParam
} from '@/api/lunaerp-elmui/production/progress-feedback'

defineOptions({ name: 'LunaerpElmuiProductionProgressFeedback' })

const loading = ref(false)
const sectionList = ref<ProgressFeedbackColorSectionVO[]>([])
const total = ref(0)
const vatCreateDialogRef = ref<InstanceType<typeof VatCreateDialog>>()
const vatDetailDrawerRef = ref<InstanceType<typeof VatDetailDrawer>>()
const feedbackDialogRef = ref<InstanceType<typeof FeedbackDialog>>()

const statistics = reactive<ProgressFeedbackStatisticsVO>({
  unscheduledColorCount: 0,
  inProgressVatCount: 0,
  completedVatCount: 0,
  delayedVatCount: 0,
  unresolvedVatCount: 0
})

const queryParams = reactive<ProgressFeedbackPageParam>({
  pageNo: 1,
  pageSize: 10,
  dyeingOrderId: undefined,
  contractNo: undefined,
  dyeingFactoryId: undefined,
  status: undefined,
  keyword: undefined,
  delayedOnly: false,
  unresolvedOnly: false,
  includeCompleted: false,
  includeDeleted: false
})

const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

const dyeingOrderLoading = ref(false)
const dyeingOrderOptions = ref<DyeingOrderVO[]>([])

const isProgressStatActive = computed(() => {
  return queryParams.status === 'IN_PROGRESS'
})

// 物料单元条目接口（按染色单/合同+产品聚合）
interface MaterialUnitItem {
  dyeingOrderId: number
  contractNo?: string
  clientName?: string
  productName?: string
  productCode?: string
  dyeingFactoryName?: string
  colorCount?: number
  hasDelayed?: boolean
  hasUnresolved?: boolean
}

const createMaterialUnit = (
  orderId: number,
  contractNo?: string,
  clientName?: string,
  productName?: string,
  productCode?: string,
  dyeingFactoryName?: string,
  colorCount: number = 0
): MaterialUnitItem => ({
  dyeingOrderId: orderId,
  contractNo,
  clientName,
  productName,
  productCode,
  dyeingFactoryName,
  colorCount,
  hasDelayed: false,
  hasUnresolved: false
})

// 聚合物料单元列表（优先从当前节列表取出实时延期/未决状态，辅以单据选项）
const materialUnits = computed<MaterialUnitItem[]>(() => {
  const map = new Map<number, MaterialUnitItem>()

  // 1. 优先从当前渲染的颜色行节聚合（拥有准确的色数、延期与未决问题状态）
  for (const s of sectionList.value) {
    if (!map.has(s.dyeingOrderId)) {
      map.set(
        s.dyeingOrderId,
        createMaterialUnit(
          s.dyeingOrderId,
          s.contractNo,
          s.clientName,
          s.productName,
          s.productCode,
          s.dyeingFactoryName
        )
      )
    }
    const unit = map.get(s.dyeingOrderId)!
    unit.colorCount = (unit.colorCount || 0) + 1
    if (s.vats && s.vats.some((v) => v.delayed)) {
      unit.hasDelayed = true
    }
    if (s.vats && s.vats.some((v) => (v.unresolvedIssueCount || 0) > 0)) {
      unit.hasUnresolved = true
    }
  }

  // 2. 结合搜索下拉选项补充未在当前切片页呈现的单据（保证跟单员跨单快捷切换）
  for (const o of dyeingOrderOptions.value) {
    if (o.id && !map.has(o.id)) {
      map.set(
        o.id,
        createMaterialUnit(
          o.id,
          o.contractNo,
          o.clientName,
          o.productName,
          o.productCode,
          o.dyeingFactoryName,
          o.colors?.length || 0
        )
      )
    }
  }

  return Array.from(map.values())
})

const totalMaterialCount = computed(() => materialUnits.value.length)

// 加载染色单列表
const searchDyeingOrders = async (query: string) => {
  dyeingOrderLoading.value = true
  try {
    const res = await DyeingOrderApi.getDyeingOrderPage({
      pageNo: 1,
      pageSize: 30,
      keyword: query || undefined
    })
    dyeingOrderOptions.value = res.list || []
  } catch (error) {
    console.error('加载染色单列表失败', error)
  } finally {
    dyeingOrderLoading.value = false
  }
}

// 加载承染方供应商列表
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || undefined
    })
    supplierOptions.value = res.list || []
  } catch (error) {
    console.error('加载供应商列表失败', error)
  } finally {
    supplierLoading.value = false
  }
}

// 查询列表数据
const getList = async () => {
  loading.value = true
  try {
    const res = await getProgressFeedbackPage(queryParams)
    sectionList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('查询进度反馈覆盖层失败', error)
  } finally {
    loading.value = false
  }
}

// 查询统计条数据
const getStatistics = async () => {
  try {
    const statsParam = { ...queryParams }
    // 统计条指标不受自身的 status/delayed/unresolved 切换影响，保持与单据/供应商/合同/关键词筛选同口径
    delete statsParam.pageNo
    delete statsParam.pageSize
    delete statsParam.status
    delete statsParam.delayedOnly
    delete statsParam.unresolvedOnly
    const res = await getProgressFeedbackStatistics(statsParam)
    statistics.unscheduledColorCount = res.unscheduledColorCount || 0
    statistics.inProgressVatCount = res.inProgressVatCount || 0
    statistics.completedVatCount = res.completedVatCount || 0
    statistics.delayedVatCount = res.delayedVatCount || 0
    statistics.unresolvedVatCount = res.unresolvedVatCount || 0
  } catch (error) {
    console.error('查询统计条失败', error)
  }
}

// 统计条卡片点击联动
const handleStatClick = (
  type: 'NOT_SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED' | 'UNRESOLVED'
) => {
  if (type === 'NOT_SCHEDULED') {
    queryParams.status = queryParams.status === 'NOT_SCHEDULED' ? undefined : 'NOT_SCHEDULED'
    queryParams.delayedOnly = false
    queryParams.unresolvedOnly = false
  } else if (type === 'COMPLETED') {
    if (queryParams.status === 'COMPLETED') {
      queryParams.status = undefined
      queryParams.includeCompleted = false
    } else {
      queryParams.status = 'COMPLETED'
      queryParams.includeCompleted = true
    }
    queryParams.delayedOnly = false
    queryParams.unresolvedOnly = false
  } else if (type === 'DELAYED') {
    queryParams.delayedOnly = !queryParams.delayedOnly
    queryParams.status = undefined
    queryParams.unresolvedOnly = false
  } else if (type === 'IN_PROGRESS') {
    queryParams.status = queryParams.status === 'IN_PROGRESS' ? undefined : 'IN_PROGRESS'
    queryParams.delayedOnly = false
    queryParams.unresolvedOnly = false
  } else if (type === 'UNRESOLVED') {
    queryParams.unresolvedOnly = !queryParams.unresolvedOnly
    queryParams.delayedOnly = false
    queryParams.status = undefined
  }
  handleQuery()
}

// 快速定位物料单元，与筛选条件双向同步
const handleSelectMaterialUnit = async (orderId?: number) => {
  if (queryParams.dyeingOrderId === orderId) {
    // 再次点击取消筛选，恢复全部
    queryParams.dyeingOrderId = undefined
  } else {
    queryParams.dyeingOrderId = orderId
  }
  queryParams.pageNo = 1
  await getList()
  getStatistics()
  if (orderId) {
    await nextTick()
    scrollToOrderSection(orderId)
  }
}

// 平滑滚动定位目标染色单颜色行节并添加高亮脉冲动画
const scrollToOrderSection = (orderId: number) => {
  const el = document.querySelector(`[data-order-id='${orderId}']`) as HTMLElement
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    el.classList.add('highlight-pulse')
    setTimeout(() => {
      el.classList.remove('highlight-pulse')
    }, 1600)
  }
}

// 查询
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  getStatistics()
  if (queryParams.dyeingOrderId) {
    nextTick(() => {
      scrollToOrderSection(queryParams.dyeingOrderId!)
    })
  }
}

// 重置
const resetQuery = () => {
  queryParams.dyeingOrderId = undefined
  queryParams.contractNo = undefined
  queryParams.dyeingFactoryId = undefined
  queryParams.status = undefined
  queryParams.keyword = undefined
  queryParams.delayedOnly = false
  queryParams.unresolvedOnly = false
  queryParams.includeCompleted = false
  queryParams.includeDeleted = false
  handleQuery()
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNo = val
  getList()
}

// 独立菜单建缸
const handleCreateVatStandalone = () => {
  vatCreateDialogRef.value?.open()
}

// 从指定颜色行节建缸
const handleCreateVatForSection = (section: ProgressFeedbackColorSectionVO) => {
  vatCreateDialogRef.value?.open(section)
}

// 建缸成功刷新列表与统计条（保持当前页码，新缸即刻上屏）
const handleCreateSuccess = () => {
  getList()
  getStatistics()
}

// 打开缸详情与工序时间轴抽屉
const handleOpenVatDetail = (vat: ProgressFeedbackVatVO) => {
  vatDetailDrawerRef.value?.open(vat.id)
}

// 抽屉内推进、回退或保存成功刷新列表与统计条
const handleDetailSuccess = () => {
  getList()
  getStatistics()
}

// 缸卡推进操作成功刷新
const handleRefresh = () => {
  getList()
  getStatistics()
}

// 打开缸卡快捷反馈弹窗
const handleOpenFeedback = (vat: ProgressFeedbackVatVO) => {
  feedbackDialogRef.value?.open(vat.id, vat.vatNo)
}

// 快速反馈成功刷新列表与未决问题徽标
const handleFeedbackSuccess = () => {
  getList()
  getStatistics()
}

onMounted(() => {
  getList()
  getStatistics()
  searchSuppliers('')
  searchDyeingOrders('')
})
</script>

<style scoped lang="scss">
.progress-feedback-container {
  padding: 20px;
}

.statistics-bar {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card {
    background: #ffffff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      transform: translateY(-2px);
    }

    &.is-active {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;

      &.unscheduled {
        background: #f4f4f5;
        color: #909399;
      }

      &.in-progress {
        background: #ecf5ff;
        color: #409eff;
      }

      &.completed {
        background: #f0f9eb;
        color: #67c23a;
      }

      &.delayed {
        background: #fef0f0;
        color: #f56c6c;
      }

      &.unresolved {
        background: #fdf6ec;
        color: #e6a23c;
      }
    }

    .stat-info {
      display: flex;
      flex-direction: column;

      .stat-value {
        font-size: 24px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

.filter-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.material-locator-card {
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  .locator-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 6px;

      .locator-icon {
        font-size: 16px;
        color: #409eff;
      }

      .locator-title {
        font-size: 13px;
        font-weight: 600;
        color: #303133;
      }

      .locator-tip {
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .locator-scroll-box {
    display: flex;
    align-items: center;
    gap: 10px;
    overflow-x: auto;
    padding-bottom: 6px;

    &::-webkit-scrollbar {
      height: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 2px;
    }

    .locator-item {
      flex-shrink: 0;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      padding: 6px 12px;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      gap: 3px;
      transition: all 0.2s ease;
      min-width: 140px;

      &:hover {
        border-color: #409eff;
        background: #f0f7ff;
        transform: translateY(-1px);
      }

      &.is-active {
        background: #ecf5ff;
        border-color: #409eff;
        box-shadow: 0 0 0 1px #409eff;

        .order-id, .contract-no {
          color: #409eff;
          font-weight: 600;
        }
      }

      &.is-delayed {
        border-left: 3px solid #f56c6c;
      }

      &.is-unresolved:not(.is-delayed) {
        border-left: 3px solid #e6a23c;
      }

      .item-top {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #606266;

        .order-id {
          font-weight: 600;
          color: #303133;
        }

        .contract-no {
          color: #409eff;
        }

        .factory-name {
          color: #909399;
          font-size: 11px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 90px;
        }
      }

      .item-bottom {
        display: flex;
        align-items: center;
        gap: 6px;

        .product-name {
          font-size: 12px;
          color: #303133;
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 130px;
        }

        .color-count {
          font-size: 11px;
          color: #909399;
          background: #eef2f7;
          padding: 0 4px;
          border-radius: 3px;
        }

        .status-dot {
          font-size: 10px;
          padding: 0 4px;
          border-radius: 2px;
          line-height: 14px;
          font-weight: 600;

          &.delayed {
            background: #fef0f0;
            color: #f56c6c;
            border: 1px solid #fde2e2;
          }

          &.unresolved {
            background: #fdf6ec;
            color: #e6a23c;
            border: 1px solid #faecd8;
          }
        }
      }

      .item-title {
        font-size: 13px;
        font-weight: 600;
        color: #303133;
      }

      .item-badge {
        font-size: 11px;
        color: #909399;
      }
    }
  }
}

.wall-content {
  min-height: 300px;

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
    padding-bottom: 20px;
  }
}
</style>
