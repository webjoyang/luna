<template>
  <div class="task-list-section flex flex-col gap-3">
    <!-- 局部过滤与操作栏 -->
    <div
      class="flex flex-wrap items-center justify-between gap-3 bg-[var(--el-bg-color)] p-3 rounded border border-[var(--el-border-color-lighter)]"
    >
      <div class="flex flex-wrap items-center gap-3">
        <!-- 任务编号搜索 -->
        <el-input
          v-model="localFilter.code"
          placeholder="搜索任务编号"
          clearable
          class="!w-56"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <Icon icon="ep:search" class="text-gray-400" />
          </template>
        </el-input>

        <!-- 只看冲突红标开关 -->
        <el-checkbox v-model="localFilter.conflictOnly" @change="handleSearch" class="!mr-2">
          <span class="flex items-center gap-1 font-medium text-red-600">
            <Icon icon="ep:warning-filled" />
            <span>只看冲突任务</span>
          </span>
        </el-checkbox>

        <el-button type="primary" @click="handleSearch">
          <Icon icon="ep:search" class="mr-1" /> 查询
        </el-button>
        <el-button @click="resetLocalFilter">
          <Icon icon="ep:refresh-right" class="mr-1" /> 重置
        </el-button>
      </div>

      <div class="flex items-center gap-3">
        <span class="text-xs text-gray-400"> 共 {{ total }} 条机台任务 </span>
        <el-button link type="primary" @click="fetchData">
          <Icon icon="ep:refresh" class="mr-1" /> 刷新
        </el-button>
      </div>
    </div>

    <!-- 跨单机台任务表格 -->
    <el-table
      v-loading="loading"
      :data="taskList"
      border
      stripe
      style="width: 100%"
      class="task-table"
    >
      <el-table-column type="index" label="序号" width="60" align="center" />

      <!-- 任务号 -->
      <el-table-column prop="code" label="任务号" min-width="170">
        <template #default="{ row }">
          <div class="flex items-center gap-1">
            <span class="font-mono font-bold text-gray-800">{{ row.code }}</span>
            <el-tag
              v-if="row.conflict"
              type="danger"
              size="small"
              effect="dark"
              class="!text-[11px]"
            >
              冲突
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <!-- 织造单 -->
      <el-table-column prop="weavingOrderCode" label="关联织造单" min-width="160">
        <template #default="{ row }">
          <el-link
            v-if="row.weavingOrderId"
            type="primary"
            :underline="false"
            @click="handleJumpToWorkspace(row.weavingOrderId)"
          >
            <span class="font-mono font-semibold">{{
              row.weavingOrderCode || `WO#${row.weavingOrderId}`
            }}</span>
            <Icon icon="ep:top-right" class="ml-1 text-xs" />
          </el-link>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>

      <!-- 产品 -->
      <el-table-column label="产品" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ formatRow(row).productDisplay }}</span>
        </template>
      </el-table-column>

      <!-- 织机 -->
      <el-table-column prop="loomNo" label="织机" min-width="110">
        <template #default="{ row }">
          <span class="font-medium">{{ row.loomNo || '-' }}</span>
        </template>
      </el-table-column>

      <!-- 挡车工 -->
      <el-table-column prop="workerName" label="挡车工" min-width="100">
        <template #default="{ row }">
          {{ row.workerName || '-' }}
        </template>
      </el-table-column>

      <!-- 计划匹数 -->
      <el-table-column prop="plannedQty" label="计划匹数" width="95" align="right">
        <template #default="{ row }">
          <span class="font-semibold">{{ row.plannedQty }}</span>
        </template>
      </el-table-column>

      <!-- 实织匹数 -->
      <el-table-column prop="actualQty" label="实织" width="90" align="right">
        <template #default="{ row }">
          <span class="font-bold text-green-600">{{ row.actualQty ?? 0 }}</span>
        </template>
      </el-table-column>

      <!-- 计划起止 -->
      <el-table-column label="计划起止" min-width="190">
        <template #default="{ row }">
          <span
            v-if="row.plannedStart || row.plannedEnd"
            class="font-mono text-xs"
            :class="{ 'text-red-500 font-medium': row.conflict }"
          >
            {{ formatRow(row).scheduleDisplay }}
          </span>
          <span v-else class="text-gray-400 text-xs">未排期</span>
        </template>
      </el-table-column>

      <!-- 状态标签 -->
      <el-table-column prop="status" label="状态" width="105" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusTheme(row.status)?.tagType || 'info'" size="small">
            {{ getStatusTheme(row.status)?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 冲突标记 -->
      <el-table-column label="冲突标记" width="105" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.conflict" type="danger" size="small" effect="dark"> 时段冲突 </el-tag>
          <el-tag v-else type="success" size="small" effect="plain"> 正常 </el-tag>
        </template>
      </el-table-column>

      <!-- 操作列：仅编辑与跳转工作台，严禁删除/停机 -->
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            size="small"
            :disabled="!getRowActions(row).canEdit"
            @click="handleOpenEdit(row)"
            v-hasPermi="['lunaerp:production:weaving-order-task:update']"
          >
            编辑
          </el-button>
          <el-button
            type="primary"
            link
            size="small"
            :disabled="!getRowActions(row).canJump"
            @click="handleJumpToWorkspace(row.weavingOrderId)"
          >
            织造单
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty
          :description="
            localFilter.conflictOnly ? '暂无时段冲突机台任务' : '暂无符合条件的机台任务'
          "
          :image-size="90"
        />
      </template>
    </el-table>

    <!-- 分页 -->
    <div class="flex justify-end mt-2">
      <el-pagination
        v-model:current-page="pagination.pageNo"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 编辑弹窗（复用 DispatchDialog，冲突 000 硬拦截红字展示与保持弹窗） -->
    <DispatchDialog
      ref="editDialogRef"
      :loom-list="loomList"
      :worker-list="workerList"
      @success="handleEditSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { LoomVO } from '@/api/lunaerp-elmui/production/loom'
import type { UserVO } from '@/api/system/user'
import { getMachineTaskPage, type MachineTaskVO } from '@/api/lunaerp-elmui/production/machine-task'
import { TASK_STATUS_THEME_MAP } from '../gantt-theme'
import {
  buildTaskPageParams,
  getTaskRowActions,
  buildWeavingOrderWorkspacePath,
  formatTaskRow,
  type SharedFilterState,
  type TaskListLocalFilter,
  type PaginationState
} from '../model/task-list'
import DispatchDialog from './DispatchDialog.vue'

defineOptions({ name: 'TaskListSection' })

const props = withDefaults(
  defineProps<{
    sharedFilter?: SharedFilterState
    loomList?: LoomVO[]
    workerList?: UserVO[]
  }>(),
  {
    sharedFilter: () => ({ loomIds: [], statuses: [] }),
    loomList: () => [],
    workerList: () => []
  }
)

const emit = defineEmits<{
  (e: 'task-updated'): void
}>()

const router = useRouter()
const loading = ref(false)
const taskList = ref<MachineTaskVO[]>([])
const total = ref(0)
const editDialogRef = ref<InstanceType<typeof DispatchDialog> | null>(null)

const localFilter = reactive<TaskListLocalFilter>({
  code: '',
  conflictOnly: false
})

const pagination = reactive<PaginationState>({
  pageNo: 1,
  pageSize: 20
})

function getStatusTheme(status?: string) {
  if (!status) return undefined
  return TASK_STATUS_THEME_MAP[status]
}

function formatRow(row: MachineTaskVO) {
  return formatTaskRow(row)
}

function getRowActions(row: MachineTaskVO) {
  return getTaskRowActions(row)
}

async function fetchData() {
  loading.value = true
  try {
    const params = buildTaskPageParams(props.sharedFilter, localFilter, pagination)
    const res = await getMachineTaskPage(params)
    taskList.value = res.list || []
    total.value = res.total || 0
  } catch (err: any) {
    ElMessage.error(err?.message || '拉取机台任务列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchData()
}

function resetLocalFilter() {
  localFilter.code = ''
  localFilter.conflictOnly = false
  handleSearch()
}

function handleSizeChange(val: number) {
  pagination.pageSize = val
  pagination.pageNo = 1
  fetchData()
}

function handleCurrentChange(val: number) {
  pagination.pageNo = val
  fetchData()
}

function handleOpenEdit(row: MachineTaskVO) {
  editDialogRef.value?.open(
    {
      id: row.id,
      code: row.code,
      weavingOrderId: row.weavingOrderId,
      weavingOrderCode: row.weavingOrderCode,
      loomId: row.loomId,
      loomNo: row.loomNo,
      workerUserId: row.workerUserId,
      workerName: row.workerName,
      plannedQty: row.plannedQty,
      plannedStart: row.plannedStart,
      plannedEnd: row.plannedEnd,
      remark: row.remark
    },
    true
  )
}

function handleJumpToWorkspace(weavingOrderId?: number) {
  if (!weavingOrderId) return
  const path = buildWeavingOrderWorkspacePath(weavingOrderId)
  router.push({ path })
}

function handleEditSuccess() {
  fetchData()
  emit('task-updated')
}

// 暴露 fetchData 方法供父组件调用（顶部查询/重置）
defineExpose({
  fetchData,
  handleSearch,
  resetLocalFilter
})

// 监听顶部共享筛选变化
watch(
  () => props.sharedFilter,
  () => {
    pagination.pageNo = 1
    fetchData()
  },
  { deep: true }
)
</script>

<style scoped lang="scss">
.task-list-section {
  .task-table :deep(.el-table__cell) {
    padding: 8px 0;
  }
}
</style>
