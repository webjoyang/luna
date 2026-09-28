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
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Search,
  Refresh,
  Odometer,
  CircleCheck,
  Warning,
  DocumentRemove,
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
  delayedVatCount: 0
})

const queryParams = reactive<ProgressFeedbackPageParam>({
  pageNo: 1,
  pageSize: 10,
  dyeingOrderId: undefined,
  contractNo: undefined,
  dyeingFactoryId: undefined,
  status: undefined,
  keyword: undefined,
  delayedOnly: false
})

const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

const dyeingOrderLoading = ref(false)
const dyeingOrderOptions = ref<DyeingOrderVO[]>([])

const isProgressStatActive = computed(() => {
  return queryParams.status === 'IN_PROGRESS'
})

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
    console.error('加载染厂列表失败', error)
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
    // 统计条指标不受自身的 status/delayed 切换影响，保持与单据/供应商/合同筛选同口径
    delete statsParam.pageNo
    delete statsParam.pageSize
    const res = await getProgressFeedbackStatistics(statsParam)
    statistics.unscheduledColorCount = res.unscheduledColorCount || 0
    statistics.inProgressVatCount = res.inProgressVatCount || 0
    statistics.completedVatCount = res.completedVatCount || 0
    statistics.delayedVatCount = res.delayedVatCount || 0
  } catch (error) {
    console.error('查询统计条失败', error)
  }
}

// 统计条卡片点击联动
const handleStatClick = (type: 'NOT_SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED') => {
  if (type === 'NOT_SCHEDULED') {
    queryParams.status = queryParams.status === 'NOT_SCHEDULED' ? undefined : 'NOT_SCHEDULED'
    queryParams.delayedOnly = false
  } else if (type === 'COMPLETED') {
    queryParams.status = queryParams.status === 'COMPLETED' ? undefined : 'COMPLETED'
    queryParams.delayedOnly = false
  } else if (type === 'DELAYED') {
    queryParams.delayedOnly = !queryParams.delayedOnly
  } else if (type === 'IN_PROGRESS') {
    queryParams.status = queryParams.status === 'IN_PROGRESS' ? undefined : 'IN_PROGRESS'
    queryParams.delayedOnly = false
  }
  handleQuery()
}

// 查询
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  getStatistics()
}

// 重置
const resetQuery = () => {
  queryParams.dyeingOrderId = undefined
  queryParams.contractNo = undefined
  queryParams.dyeingFactoryId = undefined
  queryParams.status = undefined
  queryParams.keyword = undefined
  queryParams.delayedOnly = false
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
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 900px) {
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
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding-bottom: 2px;
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
