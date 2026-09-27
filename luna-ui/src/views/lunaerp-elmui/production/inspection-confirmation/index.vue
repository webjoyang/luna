<template>
  <ContentWrap>
    <!-- 搜索区（7 个条件） -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="卷号 / 缸号 / 产品名称 / 检验单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-260px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="待确认 + 已认定"
          clearable
          class="!w-180px"
        >
          <el-option label="待确认 + 已认定（有效行）" value="" />
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_CONFIRMATION_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="责任类型" prop="responsibility">
        <el-select
          v-model="queryParams.responsibility"
          placeholder="全部"
          clearable
          class="!w-180px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_RESPONSIBILITY)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" placeholder="全部" clearable class="!w-150px">
          <el-option label="检验单引入" value="INSPECTION" />
          <el-option label="手工登记" value="MANUAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="挡车工" prop="workerUserId">
        <el-select
          v-model="queryParams.workerUserId"
          placeholder="全部"
          clearable
          filterable
          class="!w-160px"
        >
          <el-option v-for="w in workerOptions" :key="w.id" :label="w.nickname" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="检验单" prop="inspectionOrderId">
        <el-select
          v-model="queryParams.inspectionOrderId"
          placeholder="搜索检验单号"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchInspectionOrders"
          :loading="orderLoading"
          class="!w-200px"
        >
          <el-option
            v-for="item in orderOptions"
            :key="item.id"
            :label="item.code"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="发现日" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          type="daterange"
          value-format="YYYY-MM-DD HH:mm:ss"
          :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-1" /> 搜索
        </el-button>
        <el-button @click="resetQuery"> <Icon icon="ep:refresh" class="mr-1" /> 重置 </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <!-- 顶部操作栏 -->
    <div class="mb-14px flex justify-between items-center">
      <div class="flex gap-2">
        <el-button
          type="primary"
          plain
          v-hasPermi="['lunaerp:production:inspection-confirmation:create']"
          @click="openManualCreateDialog"
        >
          <Icon icon="ep:plus" class="mr-1" /> 手工登记
        </el-button>
      </div>
    </div>

    <!-- 质检确认记录表格（13 列 + 操作列占位） -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column
        label="检验单号"
        prop="inspectionOrderCode"
        min-width="160"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span class="cell-code">{{ row.inspectionOrderCode || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="卷号" prop="rollNo" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-semibold">{{ row.rollNo || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="缸号" prop="vatNo" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.vatNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="等级" prop="grade" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_INSPECTION_GRADE" :value="row.grade" />
        </template>
      </el-table-column>
      <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.productName || '—' }}</template>
      </el-table-column>
      <el-table-column label="机台号" prop="machineNo" width="110" align="center">
        <template #default="{ row }">{{ row.machineNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="来源" prop="source" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 'INSPECTION' ? 'primary' : 'info'">
            {{
              row.source === 'INSPECTION'
                ? '检验单引入'
                : row.source === 'MANUAL'
                  ? '手工登记'
                  : row.source || '—'
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="责任类型" prop="responsibility" min-width="140" align="center">
        <template #default="{ row }">
          <DictTag
            v-if="row.responsibility"
            :type="DICT_TYPE.LUNAERP_INSPECTION_RESPONSIBILITY"
            :value="row.responsibility"
          />
          <span v-else class="text-gray-400">待认定</span>
        </template>
      </el-table-column>
      <el-table-column label="挡车工" prop="workerName" width="110" align="center">
        <template #default="{ row }">{{ row.workerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_INSPECTION_CONFIRMATION_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="认定人" prop="confirmedByName" width="110" align="center">
        <template #default="{ row }">{{ row.confirmedByName || '—' }}</template>
      </el-table-column>
      <el-table-column
        label="认定时间"
        prop="confirmedAt"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />
      <el-table-column
        label="创建时间"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center justify-center gap-1">
            <!-- 待确认行：[认定] -->
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              link
              size="small"
              v-hasPermi="['lunaerp:production:inspection-confirmation:confirm']"
              @click="handleConfirm(row)"
            >
              认定
            </el-button>

            <!-- 已认定行 / 已撤销行：[查看链] -->
            <el-button
              v-if="row.status === 'CONFIRMED' || row.status === 'REVOKED'"
              type="primary"
              link
              size="small"
              v-hasPermi="['lunaerp:production:inspection-confirmation:query']"
              @click="handleViewChain(row)"
            >
              查看链
            </el-button>

            <!-- 已认定行：[撤销重录] -->
            <el-button
              v-if="row.status === 'CONFIRMED'"
              type="danger"
              link
              size="small"
              v-hasPermi="['lunaerp:production:inspection-confirmation:confirm']"
              @click="handleRevoke(row)"
            >
              撤销重录
            </el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无质检确认数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 手工登记弹窗 -->
    <ManualCreateDialog
      ref="manualCreateDialogRef"
      :worker-options="workerOptions"
      @success="handleManualCreateSuccess"
    />

    <!-- 责任认定弹窗 -->
    <ConfirmDialog
      ref="confirmDialogRef"
      :worker-options="workerOptions"
      @success="handleConfirmSuccess"
    />

    <!-- 认定链详情抽屉 -->
    <ChainDrawer ref="chainDrawerRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import type {
  InspectionConfirmationVO,
  InspectionConfirmationPageParam,
  WorkerOptionVO
} from '@/api/lunaerp-elmui/production/inspection-confirmation'
import * as InspectionOrderApi from '@/api/lunaerp-elmui/production/inspection-order'
import type { InspectionOrderVO } from '@/api/lunaerp-elmui/production/inspection-order'
import ManualCreateDialog from './ManualCreateDialog.vue'
import ConfirmDialog from './ConfirmDialog.vue'
import ChainDrawer from './ChainDrawer.vue'

defineOptions({ name: 'LunaerpElmuiProductionInspectionConfirmation' })

const route = useRoute()

const loading = ref(true)
const total = ref(0)
const list = ref<InspectionConfirmationVO[]>([])
const queryFormRef = ref()
const manualCreateDialogRef = ref()
const confirmDialogRef = ref()
const chainDrawerRef = ref()

/** 打开手工登记弹窗 */
const openManualCreateDialog = () => {
  manualCreateDialogRef.value?.open()
}

/** 手工登记成功回调 */
const handleManualCreateSuccess = () => {
  getList()
}

/** 打开责任认定弹窗 */
const handleConfirm = (row: InspectionConfirmationVO) => {
  confirmDialogRef.value?.open(row)
}

/** 责任认定成功回调 */
const handleConfirmSuccess = () => {
  getList()
}

/** 撤销重录操作 */
const handleRevoke = async (row: InspectionConfirmationVO) => {
  try {
    await ElMessageBox.confirm(
      `确定撤销卷号【${row.rollNo}】的责任认定吗？原记录将冻结留痕，并生成一条新的待确认记录。`,
      '撤销重录确认',
      {
        confirmButtonText: '确定撤销',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await InspectionConfirmationApi.revokeInspectionConfirmation({ id: row.id })
    ElMessage.success('撤销重录成功，已生成新待确认记录')
    getList()
  } catch (e: any) {
    if (e !== 'cancel') {
      // 非用户点击取消的异常由请求拦截或兜底处理
    }
  }
}

/** 查看认定链抽屉 */
const handleViewChain = (row: InspectionConfirmationVO) => {
  chainDrawerRef.value?.open(row.id)
}

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  status: string
  source: string
  responsibility: string
  workerUserId: number | undefined
  inspectionOrderId: number | undefined
  createTime: string[] | undefined
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  status: '',
  source: '',
  responsibility: '',
  workerUserId: undefined,
  inspectionOrderId: undefined,
  createTime: undefined
})

// 挡车工候选列表
const workerOptions = ref<WorkerOptionVO[]>([])

/** 加载挡车工候选列表 */
const loadWorkerOptions = async () => {
  try {
    const data = await InspectionConfirmationApi.getWorkerOptions()
    workerOptions.value = data || []
  } catch {
    workerOptions.value = []
  }
}

// 检验单远程搜索
const orderOptions = ref<InspectionOrderVO[]>([])
const orderLoading = ref(false)
const searchInspectionOrders = async (query: string) => {
  orderLoading.value = true
  try {
    const res = await InspectionOrderApi.getInspectionOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    orderOptions.value = res.list || []
  } catch {
    orderOptions.value = []
  } finally {
    orderLoading.value = false
  }
}

/** 查询质检确认分页列表 */
const getList = async () => {
  loading.value = true
  try {
    const params: InspectionConfirmationPageParam = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status || undefined,
      source: queryParams.source || undefined,
      responsibility: queryParams.responsibility || undefined,
      workerUserId: queryParams.workerUserId || undefined,
      inspectionOrderId: queryParams.inspectionOrderId || undefined,
      createTime: queryParams.createTime || undefined
    }
    const data = await InspectionConfirmationApi.getInspectionConfirmationPage(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 搜索：回车或点按钮触发，回到第 1 页 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

/** 初始化 */
onMounted(async () => {
  if (route.query.inspectionOrderId) {
    const orderId = Number(route.query.inspectionOrderId)
    queryParams.inspectionOrderId = orderId
    try {
      const order = await InspectionOrderApi.getInspectionOrder(orderId)
      if (order && !orderOptions.value.some((o) => o.id === order.id)) {
        orderOptions.value = [order, ...orderOptions.value]
      }
    } catch {}
  }
  getList()
  loadWorkerOptions()
  searchInspectionOrders('')
})
</script>

<style lang="scss" scoped>
.cell-code {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
