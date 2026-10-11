<template>
  <div class="scheduling-container">
    <!-- 顶部统一筛选区 -->
    <ContentWrap>
      <el-form :inline="true" class="-mb-15px">
        <el-form-item label="计划日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :clearable="false"
            class="!w-240px"
            @change="handleDateRangeChange"
          />
        </el-form-item>

        <el-form-item label="织机">
          <el-select
            v-model="queryParams.loomIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部启用织机"
            class="!w-220px"
            @change="loadGantt"
          >
            <el-option
              v-for="loom in loomList"
              :key="loom.id"
              :label="loom.model ? `${loom.loomNo} (${loom.model})` : loom.loomNo"
              :value="loom.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="任务状态">
          <el-select
            v-model="queryParams.statuses"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部状态"
            class="!w-200px"
            @change="loadGantt"
          >
            <el-option label="待开始 (PENDING)" value="PENDING" />
            <el-option label="生产中 (IN_PROGRESS)" value="IN_PROGRESS" />
            <el-option label="暂停异常 (HALTED)" value="HALTED" />
            <el-option label="已完工 (COMPLETED)" value="COMPLETED" />
          </el-select>
        </el-form-item>

        <el-form-item label="视图粒度">
          <el-radio-group v-model="precision" size="default">
            <el-radio-button label="day">日</el-radio-button>
            <el-radio-button label="week">周</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="handleReset">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
          <el-button type="success" plain @click="openDispatchDialog()">
            <Icon icon="ep:plus" class="mr-5px" /> 新建派工
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 视图切换 Tab -->
    <ContentWrap class="mt-4">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="甘特排产看板" name="gantt">
          <!-- 待排池（可折叠横条） -->
          <el-collapse v-model="activeCollapse" class="mb-4 pool-collapse">
            <el-collapse-item name="pool">
              <template #title>
                <div class="flex items-center gap-3 font-semibold text-gray-700">
                  <Icon icon="ep:box" class="text-blue-500" />
                  <span>排产待排池</span>
                  <el-badge
                    :value="pendingPool.unDatedTasks.length"
                    :hidden="pendingPool.unDatedTasks.length === 0"
                    type="warning"
                    class="ml-2"
                  >
                    <el-tag size="small" type="info">无日期任务</el-tag>
                  </el-badge>
                  <el-badge
                    :value="pendingPool.unScheduledOrders.length"
                    :hidden="pendingPool.unScheduledOrders.length === 0"
                    type="danger"
                    class="ml-2"
                  >
                    <el-tag size="small" type="warning">未排满织造单</el-tag>
                  </el-badge>
                </div>
              </template>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4 p-2">
                <!-- 组一：无日期任务 -->
                <div class="border rounded-md p-3 bg-gray-50">
                  <div class="flex justify-between items-center mb-2">
                    <span class="font-medium text-sm text-gray-700">组一：未排日期任务 ({{ pendingPool.unDatedTasks.length }})</span>
                    <span class="text-xs text-gray-400">已建任务但无起止日期</span>
                  </div>
                  <el-table
                    :data="pendingPool.unDatedTasks"
                    size="small"
                    max-height="200"
                    empty-text="暂无无日期任务"
                  >
                    <el-table-column label="任务号" prop="taskCode" min-width="140" />
                    <el-table-column label="织造单" prop="weavingOrderCode" min-width="120" />
                    <el-table-column label="织机" prop="loomNo" width="90" />
                    <el-table-column label="计划匹数" prop="plannedQty" width="80" align="right" />
                    <el-table-column label="操作" width="90" align="center">
                      <template #default="{ row }">
                        <el-button link type="primary" size="small" @click="openScheduleTaskDialog(row)">
                          排期
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>

                <!-- 组二：未排满织造单 -->
                <div class="border rounded-md p-3 bg-gray-50">
                  <div class="flex justify-between items-center mb-2">
                    <span class="font-medium text-sm text-gray-700">组二：未排满织造单 ({{ pendingPool.unScheduledOrders.length }})</span>
                    <span class="text-xs text-gray-400">进行中/草稿织造单有剩余待排匹数</span>
                  </div>
                  <el-table
                    :data="pendingPool.unScheduledOrders"
                    size="small"
                    max-height="200"
                    empty-text="所有织造单已排满"
                  >
                    <el-table-column label="织造单号" prop="weavingOrderCode" min-width="130" />
                    <el-table-column label="产品" prop="productName" min-width="110" />
                    <el-table-column label="总匹数" prop="plannedQty" width="70" align="right" />
                    <el-table-column label="已排" prop="scheduledQty" width="60" align="right" />
                    <el-table-column label="剩余待排" prop="remainingQty" width="80" align="right">
                      <template #default="{ row }">
                        <span class="text-red-500 font-semibold">{{ row.remainingQty }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="80" align="center">
                      <template #default="{ row }">
                        <el-button link type="success" size="small" @click="openDispatchFromOrder(row)">
                          派工
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>

          <!-- 甘特区 -->
          <div v-loading="loading" class="gantt-chart-wrapper min-h-400px">
            <div v-if="ganttRows.length === 0 && !loading" class="text-center py-10 text-gray-400">
              暂无织机数据
            </div>
            <g-gantt-chart
              v-else
              :chart-start="queryParams.start"
              :chart-end="queryParams.end"
              :precision="precision"
              bar-start="startDate"
              bar-end="endDate"
              date-format="YYYY-MM-DD"
              :grid="true"
              :push-on-overlap="false"
              :row-height="46"
              label-column-title="织机 (台号/型号)"
              label-column-width="160px"
              @dragend-bar="handleDragEnd"
              @click-bar="handleClickBar"
            >
              <g-gantt-row
                v-for="row in ganttRows"
                :key="row.loomId"
                :label="row.label"
                :bars="row.bars"
                :highlight-on-hover="true"
              >
                <template #bar-label="{ bar }">
                  <div class="flex items-center gap-1 overflow-hidden px-1">
                    <Icon
                      v-if="bar.conflict"
                      icon="ep:warning-filled"
                      class="text-red-500 flex-shrink-0"
                    />
                    <span class="truncate">{{ bar.ganttBarConfig.label }}</span>
                  </div>
                </template>
              </g-gantt-row>
            </g-gantt-chart>
          </div>
        </el-tab-pane>

        <!-- 任务列表 Tab（跨单机台任务列表） -->
        <el-tab-pane label="跨单任务列表" name="tasks">
          <!-- 任务列表筛选工具条 -->
          <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
            <div class="flex flex-wrap items-center gap-3">
              <el-input
                v-model="taskFilter.code"
                placeholder="搜索任务编号"
                clearable
                class="!w-200px"
                @keyup.enter="handleTaskSearch"
                @clear="handleTaskSearch"
              >
                <template #prefix>
                  <Icon icon="ep:search" />
                </template>
              </el-input>

              <el-select
                v-model="taskFilter.loomId"
                placeholder="机台筛选"
                clearable
                filterable
                class="!w-180px"
                @change="handleTaskSearch"
              >
                <el-option
                  v-for="l in loomList"
                  :key="l.id"
                  :label="l.model ? `${l.loomNo} (${l.model})` : l.loomNo"
                  :value="l.id"
                />
              </el-select>

              <el-select
                v-model="taskFilter.status"
                placeholder="状态筛选"
                clearable
                class="!w-160px"
                @change="handleTaskSearch"
              >
                <el-option label="待开始 (PENDING)" value="PENDING" />
                <el-option label="生产中 (IN_PROGRESS)" value="IN_PROGRESS" />
                <el-option label="暂停异常 (HALTED)" value="HALTED" />
                <el-option label="已完工 (COMPLETED)" value="COMPLETED" />
              </el-select>

              <el-checkbox
                v-model="taskFilter.conflictOnly"
                @change="handleTaskSearch"
              >
                <span class="flex items-center gap-1 font-medium text-red-500">
                  <Icon icon="ep:warning-filled" />
                  只看冲突红标任务
                </span>
              </el-checkbox>
            </div>

            <div class="flex items-center gap-2">
              <el-button type="primary" @click="handleTaskSearch">
                <Icon icon="ep:search" class="mr-5px" /> 搜索
              </el-button>
              <el-button @click="handleTaskReset">
                <Icon icon="ep:refresh" class="mr-5px" /> 重置
              </el-button>
            </div>
          </div>

          <!-- 跨单机台任务表格 -->
          <el-table
            v-loading="taskLoading"
            :data="taskList"
            border
            stripe
            size="default"
            class="w-full"
          >
            <el-table-column label="任务编号" prop="code" min-width="170">
              <template #default="{ row }">
                <span class="font-mono font-medium">{{ row.code }}</span>
              </template>
            </el-table-column>

            <el-table-column label="关联织造单" prop="weavingOrderCode" min-width="140">
              <template #default="{ row }">
                <el-link
                  v-if="row.weavingOrderId"
                  type="primary"
                  @click="jumpToWeavingOrder(row.weavingOrderId)"
                >
                  {{ row.weavingOrderCode || `织造单 #${row.weavingOrderId}` }}
                </el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>

            <el-table-column label="产品" min-width="140">
              <template #default="{ row }">
                <span v-if="row.productName">{{ row.productName }}</span>
                <span v-if="row.productCode" class="text-xs text-gray-400 block">({{ row.productCode }})</span>
                <span v-if="!row.productName && !row.productCode">-</span>
              </template>
            </el-table-column>

            <el-table-column label="织机机台" prop="loomNo" min-width="110" align="center">
              <template #default="{ row }">
                <span class="font-semibold">{{ row.loomNo || getLoomName(row.loomId) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="挡车工" prop="workerName" min-width="100" align="center">
              <template #default="{ row }">
                <span>{{ row.workerName || '-' }}</span>
              </template>
            </el-table-column>

            <el-table-column label="计划匹数" prop="plannedQty" width="90" align="right">
              <template #default="{ row }">
                <span class="font-medium">{{ row.plannedQty ?? 0 }}</span>
              </template>
            </el-table-column>

            <el-table-column label="实织匹数" prop="actualQty" width="90" align="right">
              <template #default="{ row }">
                <span>{{ row.actualQty ?? 0 }}</span>
              </template>
            </el-table-column>

            <el-table-column label="计划起止" min-width="190" align="center">
              <template #default="{ row }">
                <span v-if="row.plannedStart || row.plannedEnd">
                  {{ row.plannedStart || '-' }} 至 {{ row.plannedEnd || '-' }}
                </span>
                <span v-else class="text-gray-400">未排日期</span>
              </template>
            </el-table-column>

            <el-table-column label="状态" prop="status" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="冲突标记" prop="conflict" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.conflict" type="danger" effect="dark">
                  冲突红标
                </el-tag>
                <el-tag v-else type="success" size="small">
                  正常
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="openEditTaskDialog(row)"
                >
                  编辑
                </el-button>
                <el-button
                  link
                  type="info"
                  size="small"
                  @click="jumpToWeavingOrder(row.weavingOrderId)"
                >
                  去工作台
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="mt-4 flex justify-end">
            <Pagination
              :total="taskTotal"
              v-model:page="taskPageParams.pageNo"
              v-model:limit="taskPageParams.pageSize"
              @pagination="loadTaskList"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>

    <!-- 任务详情侧拉抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      title="机台任务详情"
      direction="rtl"
      size="420px"
    >
      <div v-if="currentBar" class="space-y-4">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="任务编号">
            <span class="font-mono font-medium">{{ currentBar.taskCode }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="关联织造单">
            <el-link type="primary" @click="jumpToWeavingOrder(currentBar.weavingOrderId)">
              {{ currentBar.weavingOrderCode || '-' }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="产品名称">
            {{ currentBar.productName || '-' }} ({{ currentBar.productCode || '-' }})
          </el-descriptions-item>
          <el-descriptions-item label="织机机台">
            <span class="font-semibold">{{ getLoomName(currentBar.loomId) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="计划匹数">
            {{ currentBar.plannedQty }} 匹
          </el-descriptions-item>
          <el-descriptions-item label="完工匹数">
            {{ currentBar.actualQty ?? 0 }} 匹
          </el-descriptions-item>
          <el-descriptions-item label="计划起止">
            {{ currentBar.startDate }} 至 {{ currentBar.endDate }}
          </el-descriptions-item>
          <el-descriptions-item label="挡车工">
            {{ currentBar.workerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag :type="getStatusTagType(currentBar.status)">
              {{ getStatusLabel(currentBar.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="冲突状态">
            <el-tag v-if="currentBar.conflict" type="danger" effect="dark">
              时段冲突 (双占)
            </el-tag>
            <el-tag v-else type="success">
              正常无冲突
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="flex gap-2 pt-4">
          <el-button
            v-if="currentBar.status !== 'COMPLETED'"
            type="warning"
            plain
            @click="openTurnLoomDialog(currentBar)"
          >
            转机换台
          </el-button>
          <el-button
            type="primary"
            plain
            @click="jumpToWeavingOrder(currentBar.weavingOrderId)"
          >
            前往织造单工作台
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 转机弹窗（Issue 03 spike 降级方案：条带点击弹窗转机） -->
    <el-dialog
      v-model="turnLoomVisible"
      title="任务转机（更换织机）"
      width="450px"
      append-to-body
    >
      <el-form :model="turnLoomForm" label-width="100px">
        <el-form-item label="任务编号">
          <span>{{ turnLoomForm.taskCode }}</span>
        </el-form-item>
        <el-form-item label="原织机">
          <span>{{ getLoomName(turnLoomForm.originLoomId) }}</span>
        </el-form-item>
        <el-form-item label="目标织机" required>
          <el-select
            v-model="turnLoomForm.targetLoomId"
            filterable
            placeholder="请选择目标织机"
            class="w-full"
          >
            <el-option
              v-for="l in loomList"
              :key="l.id"
              :label="l.model ? `${l.loomNo} (${l.model})` : l.loomNo"
              :value="l.id"
              :disabled="l.id === turnLoomForm.originLoomId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="turnLoomVisible = false">取消</el-button>
        <el-button type="primary" :loading="turnLoomLoading" @click="submitTurnLoom">
          确认转机
        </el-button>
      </template>
    </el-dialog>

    <!-- 派工 / 改期弹窗 -->
    <el-dialog
      v-model="dispatchVisible"
      :title="dispatchForm.id ? '机台任务排期 / 修改' : '新建派工任务'"
      width="540px"
      append-to-body
    >
      <el-alert
        v-if="conflictErrorMessage"
        :title="conflictErrorMessage"
        type="error"
        show-icon
        :closable="false"
        class="mb-4"
      />

      <el-form
        ref="dispatchFormRef"
        :model="dispatchForm"
        :rules="dispatchRules"
        label-width="110px"
      >
        <el-form-item label="织造单" prop="weavingOrderId">
          <el-select
            v-model="dispatchForm.weavingOrderId"
            filterable
            :disabled="Boolean(dispatchForm.id)"
            placeholder="请选择或搜索织造单"
            class="w-full"
          >
            <el-option
              v-for="order in activeWeavingOrders"
              :key="order.id"
              :label="`${order.code} - ${order.productName || ''}`"
              :value="order.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="织机" prop="loomId">
          <el-select
            v-model="dispatchForm.loomId"
            filterable
            placeholder="请选择织机"
            class="w-full"
          >
            <el-option
              v-for="l in loomList"
              :key="l.id"
              :label="l.model ? `${l.loomNo} (${l.model})` : l.loomNo"
              :value="l.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="计划日期" prop="dateRange">
          <el-date-picker
            v-model="dispatchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日"
            end-placeholder="结束日"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </el-form-item>

        <el-form-item label="计划匹数" prop="plannedQty">
          <el-input-number
            v-model="dispatchForm.plannedQty"
            :min="1"
            :precision="0"
            class="!w-full"
          />
        </el-form-item>

        <el-form-item label="挡车工姓名" prop="workerName">
          <el-input v-model="dispatchForm.workerName" placeholder="选填挡车工姓名" />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="dispatchForm.remark" type="textarea" placeholder="选填备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="dispatchLoading" @click="submitDispatch">
          确认保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElNotification, type FormInstance } from 'element-plus'
import { GGanttChart, GGanttRow } from '@infectoone/vue-ganttastic'
import {
  getGanttData,
  getPendingPool,
  type GanttLoomVO,
  type GanttBarVO,
  type PendingTaskVO,
  type PendingOrderVO
} from '@/api/lunaerp-elmui/production/scheduling'
import {
  createMachineTask,
  updateMachineTask,
  getMachineTaskPage,
  type MachineTaskVO,
  type MachineTaskSaveReqVO
} from '@/api/lunaerp-elmui/production/machine-task'
import { getLoomListAll, type LoomVO } from '@/api/lunaerp-elmui/production/loom'
import { getWeavingOrderPage, type WeavingOrderVO } from '@/api/lunaerp-elmui/production/weaving-order'
import {
  buildGanttRows,
  getDefaultWindow,
  rollbackBar,
  buildTaskPageQuery,
  type FormattedGanttBar,
  type LoomGanttRow
} from './helper'

defineOptions({ name: 'LunaerpElmuiScheduling' })

const router = useRouter()

// 状态
const loading = ref(false)
const activeTab = ref('gantt')
const activeCollapse = ref(['pool'])
const precision = ref<'day' | 'week'>('day')

// 日期窗口默认今天起 30 天
const defaultWindow = getDefaultWindow(30)
const dateRange = ref<[string, string]>([defaultWindow.start, defaultWindow.end])

const queryParams = reactive<{
  start: string
  end: string
  loomIds: number[]
  statuses: string[]
}>({
  start: defaultWindow.start,
  end: defaultWindow.end,
  loomIds: [],
  statuses: []
})

const loomList = ref<LoomVO[]>([])
const activeWeavingOrders = ref<WeavingOrderVO[]>([])

// 甘特数据
const rawLooms = ref<GanttLoomVO[]>([])
const rawBars = ref<GanttBarVO[]>([])
const ganttRows = ref<LoomGanttRow[]>([])

// 待排池
const pendingPool = reactive<{
  unDatedTasks: PendingTaskVO[]
  unScheduledOrders: PendingOrderVO[]
}>({
  unDatedTasks: [],
  unScheduledOrders: []
})

// 跨单任务列表状态 (Issue 08)
const taskLoading = ref(false)
const taskList = ref<MachineTaskVO[]>([])
const taskTotal = ref(0)
const taskPageParams = reactive({
  pageNo: 1,
  pageSize: 10
})
const taskFilter = reactive<{
  code: string
  loomId: number | undefined
  status: string
  conflictOnly: boolean
}>({
  code: '',
  loomId: undefined,
  status: '',
  conflictOnly: false
})

// 抽屉详情
const detailDrawer = ref(false)
const currentBar = ref<FormattedGanttBar | null>(null)

// 转机弹窗
const turnLoomVisible = ref(false)
const turnLoomLoading = ref(false)
const turnLoomForm = reactive<{
  taskId: number
  taskCode: string
  originLoomId: number
  targetLoomId: number | undefined
}>({
  taskId: 0,
  taskCode: '',
  originLoomId: 0,
  targetLoomId: undefined
})

// 派工弹窗
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const dispatchFormRef = ref<FormInstance>()
const conflictErrorMessage = ref('')

const dispatchForm = reactive<{
  id?: number
  weavingOrderId?: number
  loomId?: number
  dateRange: [string, string] | null
  plannedQty: number
  workerName: string
  remark: string
}>({
  id: undefined,
  weavingOrderId: undefined,
  loomId: undefined,
  dateRange: null,
  plannedQty: 1,
  workerName: '',
  remark: ''
})

const dispatchRules = {
  weavingOrderId: [{ required: true, message: '请选择织造单', trigger: 'change' }],
  loomId: [{ required: true, message: '请选择织机', trigger: 'change' }],
  plannedQty: [{ required: true, message: '请输入计划匹数', trigger: 'blur' }]
}

// 辅助方法
const getLoomName = (loomId: number) => {
  const loom = loomList.value.find((l) => l.id === loomId)
  if (!loom) return `织机 #${loomId}`
  return loom.model ? `${loom.loomNo} (${loom.model})` : loom.loomNo || `织机 #${loomId}`
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待开始',
    IN_PROGRESS: '生产中',
    HALTED: '暂停异常',
    COMPLETED: '已完工'
  }
  return map[status] || status
}

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'info',
    IN_PROGRESS: 'primary',
    HALTED: 'danger',
    COMPLETED: 'success'
  }
  return (map[status] || 'info') as 'info' | 'primary' | 'danger' | 'success'
}

const handleDateRangeChange = (val: [string, string]) => {
  if (val && val.length === 2) {
    queryParams.start = val[0]
    queryParams.end = val[1]
    loadGantt()
  }
}

const handleSearch = () => {
  loadGantt()
  loadPool()
  if (activeTab.value === 'tasks') {
    loadTaskList()
  }
}

const handleReset = () => {
  const win = getDefaultWindow(30)
  dateRange.value = [win.start, win.end]
  queryParams.start = win.start
  queryParams.end = win.end
  queryParams.loomIds = []
  queryParams.statuses = []
  loadGantt()
  loadPool()
  if (activeTab.value === 'tasks') {
    handleTaskReset()
  }
}

// 加载甘特数据
const loadGantt = async () => {
  loading.value = true
  try {
    const res = await getGanttData({
      start: queryParams.start,
      end: queryParams.end,
      loomIds: queryParams.loomIds.length > 0 ? queryParams.loomIds : undefined,
      statuses: queryParams.statuses.length > 0 ? queryParams.statuses : undefined
    })
    rawLooms.value = res.looms || []
    rawBars.value = res.bars || []
    ganttRows.value = buildGanttRows(rawLooms.value, rawBars.value)
  } catch (err) {
    console.error('加载排产甘特失败:', err)
  } finally {
    loading.value = false
  }
}

// 加载待排池数据
const loadPool = async () => {
  try {
    const res = await getPendingPool()
    pendingPool.unDatedTasks = res.unDatedTasks || []
    pendingPool.unScheduledOrders = res.unScheduledOrders || []
  } catch (err) {
    console.error('加载排产待排池失败:', err)
  }
}

// 加载织机与织造单
const loadBaseData = async () => {
  try {
    const looms = await getLoomListAll('ACTIVE')
    loomList.value = looms || []
    const ordersRes = await getWeavingOrderPage({ pageNo: 1, pageSize: 100 })
    activeWeavingOrders.value = ordersRes.list || []
  } catch (err) {
    console.error('加载基础数据失败:', err)
  }
}

// 甘特条带拖拽结束（乐观更新）
const handleDragEnd = async ({
  bar,
  movedBars
}: {
  bar: FormattedGanttBar
  movedBars?: Map<FormattedGanttBar, { oldStart: string; oldEnd: string }>
}) => {
  if (bar.status === 'COMPLETED' || bar.status === 'HALTED') {
    ElMessage.warning(`任务处于 ${getStatusLabel(bar.status)} 状态，不可拖动调整`)
    return
  }

  const oldDates = movedBars?.get(bar)
  const oldStart = oldDates?.oldStart || bar.startDate
  const oldEnd = oldDates?.oldEnd || bar.endDate

  try {
    const res = await updateMachineTask({
      id: bar.taskId,
      loomId: bar.loomId,
      plannedStart: bar.startDate,
      plannedEnd: bar.endDate
    })
    if (res.warning) {
      ElNotification({
        title: '软警告提示',
        message: res.warning,
        type: 'warning'
      })
    }
    ElMessage.success('排期调整已保存')
    loadGantt()
    loadPool()
  } catch (error: any) {
    // 乐观回滚
    rollbackBar(bar, oldStart, oldEnd)
    const errorMsg = error?.message || '同织机计划时段冲突，已回滚条带位置'
    ElMessage.error(errorMsg)
  }
}

// 点击条带
const handleClickBar = ({ bar }: { bar: FormattedGanttBar }) => {
  currentBar.value = bar
  detailDrawer.value = true
}

// 跳转织造单工作台
const jumpToWeavingOrder = (weavingOrderId: number) => {
  if (!weavingOrderId) return
  router.push({
    path: '/lunaerp/production/weaving-order',
    query: { id: String(weavingOrderId) }
  })
}

// 打开转机弹窗
const openTurnLoomDialog = (bar: FormattedGanttBar) => {
  turnLoomForm.taskId = bar.taskId
  turnLoomForm.taskCode = bar.taskCode
  turnLoomForm.originLoomId = bar.loomId
  turnLoomForm.targetLoomId = undefined
  turnLoomVisible.value = true
}

// 提交转机
const submitTurnLoom = async () => {
  if (!turnLoomForm.targetLoomId) {
    ElMessage.warning('请选择目标织机')
    return
  }
  turnLoomLoading.value = true
  try {
    await updateMachineTask({
      id: turnLoomForm.taskId,
      loomId: turnLoomForm.targetLoomId,
      plannedStart: currentBar.value?.startDate,
      plannedEnd: currentBar.value?.endDate
    })
    ElMessage.success('转机成功')
    turnLoomVisible.value = false
    detailDrawer.value = false
    loadGantt()
  } catch (error: any) {
    ElMessage.error(error?.message || '转机冲突，请选择其他织机或调整时段')
  } finally {
    turnLoomLoading.value = false
  }
}

// 打开派工弹窗
const openDispatchDialog = () => {
  conflictErrorMessage.value = ''
  dispatchForm.id = undefined
  dispatchForm.weavingOrderId = undefined
  dispatchForm.loomId = undefined
  dispatchForm.dateRange = null
  dispatchForm.plannedQty = 1
  dispatchForm.workerName = ''
  dispatchForm.remark = ''
  dispatchVisible.value = true
}

// 从未排满织造单直接派工
const openDispatchFromOrder = (order: PendingOrderVO) => {
  conflictErrorMessage.value = ''
  dispatchForm.id = undefined
  dispatchForm.weavingOrderId = order.weavingOrderId
  dispatchForm.loomId = undefined
  dispatchForm.dateRange = null
  dispatchForm.plannedQty = order.remainingQty || 1
  dispatchForm.workerName = ''
  dispatchForm.remark = ''
  dispatchVisible.value = true
}

// 从无日期任务排期
const openScheduleTaskDialog = (task: PendingTaskVO) => {
  conflictErrorMessage.value = ''
  dispatchForm.id = task.taskId
  dispatchForm.weavingOrderId = task.weavingOrderId
  dispatchForm.loomId = task.loomId
  dispatchForm.dateRange = null
  dispatchForm.plannedQty = task.plannedQty
  dispatchForm.workerName = ''
  dispatchForm.remark = ''
  dispatchVisible.value = true
}

// 提交派工 / 排期
const submitDispatch = async () => {
  if (!dispatchFormRef.value) return
  await dispatchFormRef.value.validate(async (valid) => {
    if (!valid) return
    dispatchLoading.value = true
    conflictErrorMessage.value = ''
    try {
      const payload: MachineTaskSaveReqVO = {
        id: dispatchForm.id,
        weavingOrderId: dispatchForm.weavingOrderId,
        loomId: dispatchForm.loomId,
        plannedQty: dispatchForm.plannedQty,
        workerName: dispatchForm.workerName,
        remark: dispatchForm.remark,
        plannedStart: dispatchForm.dateRange?.[0] || undefined,
        plannedEnd: dispatchForm.dateRange?.[1] || undefined
      }

      if (dispatchForm.id) {
        const res = await updateMachineTask(payload)
        if (res.warning) {
          ElNotification({ title: '软警告提示', message: res.warning, type: 'warning' })
        }
      } else {
        const res = await createMachineTask(payload)
        if (res.warning) {
          ElNotification({ title: '软警告提示', message: res.warning, type: 'warning' })
        }
      }

      ElMessage.success('保存成功')
      dispatchVisible.value = false
      loadGantt()
      loadPool()
      loadTaskList()
    } catch (error: any) {
      // 保持弹窗打开，回显冲突信息
      conflictErrorMessage.value = error?.message || '派工冲突，请检查机台时段'
    } finally {
      dispatchLoading.value = false
    }
  })
}

// 加载跨单任务列表 (Issue 08)
const loadTaskList = async () => {
  taskLoading.value = true
  try {
    const query = buildTaskPageQuery({
      pageNo: taskPageParams.pageNo,
      pageSize: taskPageParams.pageSize,
      loomId: taskFilter.loomId,
      status: taskFilter.status,
      conflictOnly: taskFilter.conflictOnly,
      code: taskFilter.code
    })
    const res = await getMachineTaskPage(query)
    taskList.value = res.list || []
    taskTotal.value = res.total || 0
  } catch (err) {
    console.error('加载跨单任务列表失败:', err)
  } finally {
    taskLoading.value = false
  }
}

const handleTaskSearch = () => {
  taskPageParams.pageNo = 1
  loadTaskList()
}

const handleTaskReset = () => {
  taskFilter.code = ''
  taskFilter.loomId = undefined
  taskFilter.status = ''
  taskFilter.conflictOnly = false
  taskPageParams.pageNo = 1
  loadTaskList()
}

// 打开编辑任务弹窗（来自任务列表）
const openEditTaskDialog = (row: MachineTaskVO) => {
  conflictErrorMessage.value = ''
  dispatchForm.id = row.id
  dispatchForm.weavingOrderId = row.weavingOrderId
  dispatchForm.loomId = row.loomId
  dispatchForm.dateRange =
    row.plannedStart && row.plannedEnd
      ? [row.plannedStart, row.plannedEnd]
      : null
  dispatchForm.plannedQty = row.plannedQty || 1
  dispatchForm.workerName = row.workerName || ''
  dispatchForm.remark = row.remark || ''
  dispatchVisible.value = true
}

// 监听 Tab 切换，切换到任务列表时加载数据
watch(activeTab, (tab) => {
  if (tab === 'tasks') {
    loadTaskList()
  }
})

onMounted(() => {
  loadBaseData()
  loadGantt()
  loadPool()
})
</script>

<style scoped>
.pool-collapse :deep(.el-collapse-item__header) {
  background-color: var(--el-fill-color-light);
  padding: 0 16px;
  border-radius: var(--el-border-radius-base);
}

.gantt-chart-wrapper :deep(.g-gantt-bar.is-conflict) {
  border: 2px dashed var(--el-color-error, #f56c6c) !important;
  box-shadow: 0 0 6px var(--el-color-error-light-5, rgba(245, 108, 108, 0.4)) !important;
}
</style>
