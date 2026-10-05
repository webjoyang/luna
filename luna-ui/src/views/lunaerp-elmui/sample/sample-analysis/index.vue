<template>
  <!-- 顶部付款统计卡 -->
  <el-row :gutter="16" class="mb-16px">
    <el-col :span="8">
      <el-card
        shadow="never"
        class="stat-card"
        :class="{ 'is-active': queryParams.isPaid === false }"
        @click="handleFilterByPaid(false)"
      >
        <div class="flex items-center gap-4">
          <div class="stat-icon unpaid">
            <Icon icon="ep:warning" class="text-24px text-amber-500" />
          </div>
          <div class="stat-info">
            <div class="stat-value text-amber-600">¥{{ formatAmount(stats.unpaidAmount) }}</div>
            <div class="stat-label">未付金额 (月结)</div>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card
        shadow="never"
        class="stat-card"
        :class="{ 'is-active': queryParams.isPaid === true }"
        @click="handleFilterByPaid(true)"
      >
        <div class="flex items-center gap-4">
          <div class="stat-icon paid">
            <Icon icon="ep:circle-check" class="text-24px text-emerald-500" />
          </div>
          <div class="stat-info">
            <div class="stat-value text-emerald-600">¥{{ formatAmount(stats.paidAmount) }}</div>
            <div class="stat-label">已付金额 (月结)</div>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card
        shadow="never"
        class="stat-card"
        :class="{ 'is-active': queryParams.isPaid === undefined }"
        @click="handleFilterByPaid(undefined)"
      >
        <div class="flex items-center gap-4">
          <div class="stat-icon total">
            <Icon icon="ep:document" class="text-24px text-blue-500" />
          </div>
          <div class="stat-info">
            <div class="stat-value text-blue-600">{{ stats.totalCount ?? 0 }}</div>
            <div class="stat-label">任务总数</div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>

  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="样品编号 / 客户 / 产品 / 款号 / 成分"
          clearable
          @keyup.enter="handleQuery"
          class="!w-280px"
        />
      </el-form-item>
      <el-form-item label="付款状态" prop="isPaid">
        <el-select
          v-model="queryParams.isPaid"
          placeholder="全部"
          clearable
          class="!w-120px"
          @change="handleQuery"
        >
          <el-option label="全部" :value="undefined" />
          <el-option label="未付款" :value="false" />
          <el-option label="已付款" :value="true" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:sample:sample-analysis:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新建样品分析
        </el-button>
        <el-button
          type="success"
          plain
          :disabled="selectedRows.length === 0"
          @click="handleBatchPayment(true)"
          v-hasPermi="['lunaerp:sample:sample-analysis:pay']"
        >
          <Icon icon="ep:check" class="mr-5px" /> 标记已付款
        </el-button>
        <el-button
          type="warning"
          plain
          :disabled="selectedRows.length === 0"
          @click="handleBatchPayment(false)"
          v-hasPermi="['lunaerp:sample:sample-analysis:pay']"
        >
          <Icon icon="ep:close" class="mr-5px" /> 标记未付款
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="list"
      border
      stripe
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
      class="cursor-pointer"
    >
      <!-- 批量选择列 -->
      <el-table-column type="selection" width="48" align="center" />
      <!-- 照片缩略图列 -->
      <el-table-column label="照片" width="80" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.firstPhotoUrl"
            :src="row.firstPhotoUrl"
            class="w-40px h-40px rounded border border-gray-200"
            fit="cover"
            preview-teleported
            :preview-src-list="[row.firstPhotoUrl]"
            @click.stop
          >
            <template #error>
              <div class="w-40px h-40px flex items-center justify-center bg-gray-100 rounded text-gray-400">
                <Icon icon="ep:picture" class="text-16px" />
              </div>
            </template>
          </el-image>
          <div v-else class="w-40px h-40px mx-auto flex items-center justify-center bg-gray-100 rounded text-gray-300">
            <Icon icon="ep:picture" class="text-18px" />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="样品编号" prop="sampleAnalysisNo" min-width="140">
        <template #default="{ row }">
          <span class="cell-no">{{ row.sampleAnalysisNo || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="客户" prop="clientName" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.clientName || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="产品名称" prop="productName" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.productName || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="客户款号" prop="clientStyleNo" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.clientStyleNo || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="门幅(CM)" prop="width" width="100" align="right">
        <template #default="{ row }">
          {{ row.width != null ? row.width : '—' }}
        </template>
      </el-table-column>

      <el-table-column label="克重(GSM)" prop="weight" width="100" align="right">
        <template #default="{ row }">
          {{ row.weight != null ? row.weight : '—' }}
        </template>
      </el-table-column>

      <el-table-column label="登记人" prop="creatorName" width="100" align="center">
        <template #default="{ row }">
          {{ row.creatorName || '—' }}
        </template>
      </el-table-column>

      <!-- 月结单价列 -->
      <el-table-column label="月结单价(元)" prop="unitPrice" width="120" align="right">
        <template #default="{ row }">
          {{ row.unitPrice != null ? `¥${Number(row.unitPrice).toFixed(2)}` : '¥30.00' }}
        </template>
      </el-table-column>

      <!-- 付款状态标签列 -->
      <el-table-column label="付款状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isPaid" type="success" size="small">已付款</el-tag>
          <el-tag v-else type="danger" size="small">未付款</el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="登记日期"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />

      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click.stop="openWorkspace(row.id, true)"
            v-hasPermi="['lunaerp:sample:sample-analysis:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click.stop="openWorkspace(row.id, false)"
            v-hasPermi="['lunaerp:sample:sample-analysis:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click.stop="handleDelete(row.id)"
            v-hasPermi="['lunaerp:sample:sample-analysis:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无样品分析记录" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as SampleAnalysisApi from '@/api/lunaerp-elmui/sample/sample-analysis'
import type {
  SampleAnalysisVO,
  SampleAnalysisPageReqVO,
  SampleAnalysisPaymentStatsRespVO
} from '@/api/lunaerp-elmui/sample/sample-analysis'

defineOptions({ name: 'LunaerpElmuiSampleSampleAnalysis' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<SampleAnalysisVO[]>([])
const queryFormRef = ref()
const tableRef = ref()
const selectedRows = ref<SampleAnalysisVO[]>([])

const stats = ref<SampleAnalysisPaymentStatsRespVO>({
  unpaidAmount: 0,
  paidAmount: 0,
  totalCount: 0
})

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  isPaid?: boolean
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  isPaid: undefined
})

/** 格式化金额 */
const formatAmount = (amount?: number) => {
  if (amount == null) return '0.00'
  return Number(amount).toFixed(2)
}

/** 查询付款统计（三值口径：与当前查询同口径，不受分页影响） */
const getStats = async () => {
  try {
    const params: SampleAnalysisPageReqVO = {
      keyword: queryParams.keyword || undefined,
      isPaid: queryParams.isPaid
    }
    const data = await SampleAnalysisApi.getSampleAnalysisPaymentStats(params)
    stats.value = data || { unpaidAmount: 0, paidAmount: 0, totalCount: 0 }
  } catch {}
}

/** 查询样品分析分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: SampleAnalysisPageReqVO = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      isPaid: queryParams.isPaid
    }
    const data = await SampleAnalysisApi.getSampleAnalysisPage(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
  getStats()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.isPaid = undefined
  getList()
  getStats()
}

/** 点击统计卡快捷过滤 */
const handleFilterByPaid = (isPaid?: boolean) => {
  if (queryParams.isPaid === isPaid) {
    queryParams.isPaid = undefined
  } else {
    queryParams.isPaid = isPaid
  }
  handleQuery()
}

/** 多选变化 */
const handleSelectionChange = (rows: SampleAnalysisVO[]) => {
  selectedRows.value = rows
}

/** 批量标记付款状态（需收费独立权限） */
const handleBatchPayment = async (isPaid: boolean) => {
  if (selectedRows.value.length === 0) {
    message.warning('请先勾选需要操作的样品分析记录')
    return
  }
  const actionText = isPaid ? '已付款' : '未付款'
  try {
    await message.confirm(`确认将选中的 ${selectedRows.value.length} 条记录批量标记为「${actionText}」吗？`)
    await SampleAnalysisApi.updateSampleAnalysisPayment({
      ids: selectedRows.value.map((row) => row.id!),
      isPaid
    })
    message.success(`批量标记${actionText}成功`)
    tableRef.value?.clearSelection()
    selectedRows.value = []
    await Promise.all([getList(), getStats()])
  } catch {}
}

/** 行点击进入只读工作台 */
const handleRowClick = (row: SampleAnalysisVO) => {
  if (row.id) {
    openWorkspace(row.id, true)
  }
}

/** 打开工作台 */
const openWorkspace = (id?: number, viewOnly: boolean = false) => {
  if (!id) {
    router.push('/sample/sample-analysis/workspace')
  } else {
    router.push(
      viewOnly
        ? `/sample/sample-analysis/workspace/${id}?mode=view`
        : `/sample/sample-analysis/workspace/${id}`
    )
  }
}

/** 删除样品分析（逻辑删除） */
const handleDelete = async (id: number) => {
  try {
    await message.confirm('确认删除该样品分析记录吗？删除后编号不释放、不重用。')
    await SampleAnalysisApi.deleteSampleAnalysis(id)
    message.success('删除成功')
    await Promise.all([getList(), getStats()])
  } catch {}
}

onMounted(() => {
  getList()
  getStats()
})
</script>

<style lang="scss" scoped>
.cell-no {
  font-weight: 600;
  color: var(--el-color-primary);
}

.stat-card {
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &.is-active {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary);
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;

    &.unpaid {
      background-color: #fef3c7;
    }
    &.paid {
      background-color: #d1fae5;
    }
    &.total {
      background-color: #dbeafe;
    }
  }

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 13px;
    color: #6b7280;
    margin-top: 4px;
  }
}
</style>
