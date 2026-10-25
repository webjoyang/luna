<template>
  <div class="scheduling-container p-4 flex flex-col gap-3">
    <!-- 顶部统一筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" class="flex flex-wrap items-center justify-between">
        <div class="flex flex-wrap items-center gap-y-2">
          <el-form-item label="日期窗口">
            <el-date-picker
              v-model="dateWindow"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              :clearable="false"
              @change="handleDateWindowChange"
            />
          </el-form-item>

          <el-form-item label="织机">
            <el-select
              v-model="queryParams.loomIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="全部启用织机"
              clearable
              class="!w-64"
            >
              <el-option
                v-for="loom in activeLooms"
                :key="loom.id!"
                :label="`${loom.loomNo}${loom.model ? ' (' + loom.model + ')' : ''}`"
                :value="loom.id!"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="任务状态">
            <el-select
              v-model="queryParams.statuses"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="全部状态"
              clearable
              class="!w-56"
            >
              <el-option
                v-for="(meta, statusKey) in TASK_STATUS_THEME_MAP"
                :key="statusKey"
                :label="meta.label"
                :value="statusKey"
              />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleQuery">
              <Icon icon="ep:search" class="mr-1" /> 查询
            </el-button>
            <el-button @click="resetQuery">
              <Icon icon="ep:refresh-right" class="mr-1" /> 重置
            </el-button>
          </el-form-item>
        </div>

        <div class="flex items-center gap-3">
          <!-- 日/周 粒度切换 -->
          <el-radio-group v-model="precision" size="default">
            <el-radio-button label="day">日视图</el-radio-button>
            <el-radio-button label="week">周视图</el-radio-button>
          </el-radio-group>

          <!-- 派工按钮 -->
          <el-button
            type="primary"
            @click="handleOpenDispatchDialog()"
            v-hasPermi="['lunaerp:production:weaving-order-task:create']"
          >
            <Icon icon="ep:plus" class="mr-1" /> 新建派工
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 主视图 Tabs：排产甘特 / 跨单任务列表 -->
    <el-tabs v-model="activeTab" class="scheduling-tabs">
      <el-tab-pane label="排产甘特" name="gantt">
        <!-- 待排池区 -->
        <PendingPoolSection
          :un-scheduled-orders="pendingPoolData.unScheduledOrders"
          :un-dated-tasks="pendingPoolData.unDatedTasks"
          :loading="loadingPendingPool"
          @dispatch-order="handleDispatchFromOrder"
          @dispatch-task="handleDispatchFromTask"
          @refresh="fetchPendingPool"
        />

        <!-- 甘特图区 -->
        <div class="gantt-chart-wrapper mt-2" v-loading="loadingGantt">
          <div v-if="ganttRows.length > 0">
            <g-gantt-chart
              :chart-start="chartStartFormatted"
              :chart-end="chartEndFormatted"
              :precision="precision"
              bar-start="startDate"
              bar-end="endDate"
              date-format="YYYY-MM-DD"
              :color-scheme="elementPlusGanttTheme"
              grid
              :row-height="42"
              label-column-title="织机台号"
              label-column-width="190px"
              @dragend-bar="handleDragEndBar"
              @click-bar="handleClickBar"
            >
              <g-gantt-row
                v-for="row in ganttRows"
                :key="row.loomId"
                :label="row.label"
                :bars="row.bars"
              />
            </g-gantt-chart>
          </div>
          <el-empty v-else description="暂无符合条件的织机或排产任务" :image-size="100" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="跨单任务列表" name="taskList">
        <TaskListSection
          ref="taskListSectionRef"
          :shared-filter="sharedFilter"
          :loom-list="activeLooms"
          :worker-list="workerList"
          @task-updated="handleTaskUpdated"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 派工弹窗 -->
    <DispatchDialog
      ref="dispatchDialogRef"
      :loom-list="activeLooms"
      :worker-list="workerList"
      @success="handleDispatchSuccess"
    />

    <!-- 任务详情抽屉（含转机） -->
    <TaskDetailDrawer
      ref="taskDetailDrawerRef"
      :loom-list="activeLooms"
      @transfer-success="handleTransferSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { GGanttChart, GGanttRow, type GanttBarObject } from 'vue-ganttastic'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  elementPlusGanttTheme,
  TASK_STATUS_THEME_MAP,
  isTaskDraggable,
  getTaskDragDisabledReason
} from './gantt-theme'
import {
  buildGanttRows,
  optimisticBarUpdate,
  type LoomGanttRow,
  type MachineTaskGanttBar
} from './model/gantt-adapter'
import {
  createEmptyDispatchForm,
  initDispatchFromPendingOrder,
  initDispatchFromPendingTask
} from './model/pending-pool'
import {
  getGantt,
  getPendingPool,
  type GanttLoomVO,
  type GanttBarVO,
  type PendingOrderVO,
  type PendingTaskVO
} from '@/api/lunaerp-elmui/production/scheduling'
import { getLoomListAll, type LoomVO } from '@/api/lunaerp-elmui/production/loom'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { updateMachineTask } from '@/api/lunaerp-elmui/production/machine-task'

import PendingPoolSection from './components/PendingPoolSection.vue'
import DispatchDialog from './components/DispatchDialog.vue'
import TaskDetailDrawer from './components/TaskDetailDrawer.vue'
import TaskListSection from './components/TaskListSection.vue'
import type { SharedFilterState } from './model/task-list'

defineOptions({ name: 'ProductionSchedulingCenter' })

const activeTab = ref<'gantt' | 'taskList'>('gantt')
const precision = ref<'day' | 'week'>('day')

// 默认日期窗口：前 7 天 ~ 后 35 天（42 天跨度，在 ≤ 92 天限制内）
const defaultStart = dayjs().subtract(7, 'day').format('YYYY-MM-DD')
const defaultEnd = dayjs().add(35, 'day').format('YYYY-MM-DD')
const dateWindow = ref<string[]>([defaultStart, defaultEnd])

const queryParams = ref<{
  loomIds: number[]
  statuses: string[]
}>({
  loomIds: [],
  statuses: []
})

const loadingGantt = ref(false)
const loadingPendingPool = ref(false)
const activeLooms = ref<LoomVO[]>([])
const workerList = ref<UserVO[]>([])

const rawLooms = ref<GanttLoomVO[]>([])
const rawBars = ref<GanttBarVO[]>([])
const ganttRows = ref<LoomGanttRow[]>([])

const pendingPoolData = ref<{
  unScheduledOrders: PendingOrderVO[]
  unDatedTasks: PendingTaskVO[]
}>({
  unScheduledOrders: [],
  unDatedTasks: []
})

const dispatchDialogRef = ref()
const taskDetailDrawerRef = ref()
const taskListSectionRef = ref<InstanceType<typeof TaskListSection> | null>(null)

const sharedFilter = computed<SharedFilterState>(() => ({
  dateWindow: dateWindow.value,
  loomIds: queryParams.value.loomIds,
  statuses: queryParams.value.statuses
}))

const chartStartFormatted = computed(() => {
  return dateWindow.value[0] || defaultStart
})

const chartEndFormatted = computed(() => {
  return dateWindow.value[1] || defaultEnd
})

/**
 * 校验并限制日期窗口 ≤ 92 天
 */
function handleDateWindowChange(val: string[]) {
  if (!val || val.length !== 2) return
  const start = dayjs(val[0])
  const end = dayjs(val[1])
  const diffDays = end.diff(start, 'day')

  if (diffDays > 92) {
    ElMessage.warning('排产甘特窗口跨度不能超过 92 天，已自动截取至 92 天')
    dateWindow.value = [val[0], start.add(92, 'day').format('YYYY-MM-DD')]
  } else if (diffDays < 0) {
    dateWindow.value = [val[0], val[0]]
  }
}

/**
 * 拉取基础字典数据（活跃织机与用户列表）
 */
async function loadBaseOptions() {
  try {
    const [looms, users] = await Promise.all([getLoomListAll('ACTIVE'), getSimpleUserList()])
    activeLooms.value = looms || []
    workerList.value = users || []
  } catch (err) {
    console.error('加载织机或用户选项失败:', err)
  }
}

/**
 * 查询排产甘特数据
 */
async function fetchGanttData(showLoading = true) {
  if (showLoading) loadingGantt.value = true
  try {
    const res = await getGantt({
      start: chartStartFormatted.value,
      end: chartEndFormatted.value,
      loomIds: queryParams.value.loomIds.length ? queryParams.value.loomIds : undefined,
      statuses: queryParams.value.statuses.length ? queryParams.value.statuses : undefined
    })

    rawLooms.value = res.looms || []
    rawBars.value = res.bars || []
    ganttRows.value = buildGanttRows(rawLooms.value, rawBars.value)
  } catch (err: any) {
    ElMessage.error(err?.message || '拉取排产甘特数据失败')
  } finally {
    if (showLoading) loadingGantt.value = false
  }
}

/**
 * 查询待排池数据
 */
async function fetchPendingPool() {
  loadingPendingPool.value = true
  try {
    const res = await getPendingPool()
    pendingPoolData.value = {
      unScheduledOrders: res.unScheduledOrders || [],
      unDatedTasks: res.unDatedTasks || []
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '拉取待排池数据失败')
  } finally {
    loadingPendingPool.value = false
  }
}

function handleQuery() {
  if (activeTab.value === 'gantt') {
    fetchGanttData()
  } else {
    taskListSectionRef.value?.handleSearch()
  }
}

function resetQuery() {
  dateWindow.value = [defaultStart, defaultEnd]
  queryParams.value = {
    loomIds: [],
    statuses: []
  }
  if (activeTab.value === 'gantt') {
    fetchGanttData()
  } else {
    taskListSectionRef.value?.resetLocalFilter()
  }
}

/**
 * 甘特条带拖拽改期（平移或拉伸）乐观更新
 */
async function handleDragEndBar({
  bar,
  movedBars
}: {
  bar: GanttBarObject
  e: MouseEvent
  movedBars?: Map<GanttBarObject, { oldStart: string; oldEnd: string }>
}) {
  const target = bar as MachineTaskGanttBar
  let oldStart = target.startDate
  let oldEnd = target.endDate

  if (movedBars && movedBars.has(bar)) {
    const prev = movedBars.get(bar)!
    oldStart = prev.oldStart
    oldEnd = prev.oldEnd
  }

  // 状态守卫拦截
  if (!isTaskDraggable(target.status)) {
    const reason = getTaskDragDisabledReason(target.status)
    if (reason) ElMessage.warning(reason)
    target.startDate = oldStart
    target.endDate = oldEnd
    return
  }

  const result = await optimisticBarUpdate({
    bar: target,
    newStart: target.startDate,
    newEnd: target.endDate,
    oldStart,
    oldEnd,
    apiUpdate: updateMachineTask
  })

  if (result.success) {
    if (result.warning) {
      ElMessage.warning(result.warning)
    }
    ElMessage.success(`任务【${target.taskCode}】改期完成`)
    fetchGanttData(false)
  } else {
    // 000 冲突拦截或错误：回滚并弹错误
    ElMessage.error(result.error || '时段冲突或保存失败，条带已自动回滚')
  }
}

/**
 * 点击甘特条带：查看详情或转机
 */
function handleClickBar({ bar }: { bar: GanttBarObject }) {
  const target = bar as MachineTaskGanttBar
  if (target.status === 'HALTED') {
    ElMessage.warning('任务已暂停异常，请先在织造单工作台恢复后再改期')
  }
  taskDetailDrawerRef.value?.open(target)
}

/**
 * 顶部新建派工
 */
function handleOpenDispatchDialog() {
  const emptyForm = createEmptyDispatchForm()
  dispatchDialogRef.value?.open(emptyForm, false)
}

/**
 * 从待排池未排满织造单发起派工
 */
function handleDispatchFromOrder(order: PendingOrderVO) {
  const form = initDispatchFromPendingOrder(order)
  dispatchDialogRef.value?.open(form, true)
}

/**
 * 从待排池未排期任务发起派工
 */
function handleDispatchFromTask(task: PendingTaskVO) {
  const form = initDispatchFromPendingTask(task)
  dispatchDialogRef.value?.open(form, false)
}

/**
 * 跨单任务列表数据更新回调
 */
function handleTaskUpdated() {
  fetchGanttData(false)
  fetchPendingPool()
}

/**
 * 派工成功回调
 */
function handleDispatchSuccess() {
  fetchGanttData()
  fetchPendingPool()
  taskListSectionRef.value?.fetchData()
}

/**
 * 转机成功回调
 */
function handleTransferSuccess() {
  fetchGanttData()
  taskListSectionRef.value?.fetchData()
}

watch(activeTab, (tab) => {
  if (tab === 'gantt') {
    fetchGanttData()
    fetchPendingPool()
  } else if (tab === 'taskList') {
    taskListSectionRef.value?.fetchData()
  }
})

onMounted(() => {
  loadBaseOptions()
  fetchGanttData()
  fetchPendingPool()
})
</script>

<style scoped lang="scss">
.scheduling-container {
  .filter-card :deep(.el-card__body) {
    padding: 14px 16px;
  }

  .gantt-chart-wrapper {
    min-height: 480px;
    padding: 12px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: var(--el-border-radius-base, 4px);
  }
}
</style>
