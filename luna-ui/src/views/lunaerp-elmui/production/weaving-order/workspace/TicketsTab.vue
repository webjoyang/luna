<template>
  <div class="tickets-tab" v-loading="loading">
    <!-- 顶部扫码枪录入与快捷工具条 -->
    <div class="top-toolbar bg-gray-50 p-3 rounded mb-16px border border-gray-200 flex flex-wrap justify-between items-center gap-3">
      <!-- 模拟扫码枪输入框 -->
      <div class="flex items-center gap-2">
        <span class="text-13px font-bold text-gray-700 flex items-center gap-1">
          <Icon icon="ep:aim" class="text-primary text-16px" /> 扫码枪快速录入：
        </span>
        <el-input
          v-model="scanBarcode"
          placeholder="输入或扫描布票号回车 (BT-...) "
          clearable
          class="!w-320px"
          @keyup.enter="handleBarcodeScan"
        >
          <template #prefix>
            <Icon icon="ep:camera" />
          </template>
        </el-input>
        <el-button type="primary" plain @click="handleBarcodeScan">
          识别布票
        </el-button>
      </div>

      <!-- 批量生成布票入口 -->
      <el-button
        type="primary"
        @click="handleOpenCreateBatchDialog"
        v-hasPermi="['lunaerp:production:weaving-order-ticket:create', 'lunaerp:production:weaving-order:update']"
      >
        <Icon icon="ep:tickets" class="mr-1" /> 批量生成布票
      </el-button>
    </div>

    <!-- 筛选表单与批量操作工具栏 -->
    <div class="flex flex-wrap justify-between items-center mb-12px gap-2">
      <!-- 搜索筛选 -->
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="-mb-15px">
        <el-form-item label="任务筛选" prop="machineTaskId">
          <el-select v-model="queryParams.machineTaskId" placeholder="全部机台任务" clearable class="!w-180px" @change="handleQuery">
            <el-option
              v-for="task in taskOptions"
              :key="task.id"
              :label="`${task.code} (${task.loomNo})`"
              :value="task.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="票态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-140px" @change="handleQuery">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_WEAVING_TICKET_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="布票号" prop="code">
          <el-input
            v-model="queryParams.code"
            placeholder="精确/模糊票号"
            clearable
            class="!w-180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-1" /> 搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-1" /> 重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作按钮条（多选时激活） -->
      <div class="flex items-center gap-2">
        <span v-if="selectedRows.length > 0" class="text-13px text-gray-500 mr-1">
          已勾选 <strong class="text-primary">{{ selectedRows.length }}</strong> 匹
        </span>
        <el-button
          type="primary"
          plain
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchAction('start')"
        >
          批量开工
        </el-button>
        <el-button
          type="success"
          plain
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchAction('complete')"
        >
          批量完工
        </el-button>
        <el-button
          type="warning"
          plain
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchAction('exception')"
        >
          批量异常
        </el-button>
        <el-button
          type="danger"
          plain
          size="small"
          :disabled="selectedRows.length === 0"
          @click="handleBatchAction('void')"
        >
          批量作废
        </el-button>
      </div>
    </div>

    <!-- 布票表格 -->
    <el-table
      :data="ticketList"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="code" label="布票编号" min-width="210">
        <template #default="{ row }">
          <span class="font-mono font-bold text-gray-800">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="plannedMachine" label="预排机台" width="100">
        <template #default="{ row }">
          {{ row.plannedMachine || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="actualMachine" label="上机机台" width="100">
        <template #default="{ row }">
          <span :class="row.actualMachine ? 'font-bold text-blue-600' : 'text-gray-400'">
            {{ row.actualMachine || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="workerName" label="挡车工" width="100">
        <template #default="{ row }">
          {{ row.workerName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="plannedWeight" label="计划重量" width="100" align="right">
        <template #default="{ row }">
          {{ row.plannedWeight != null ? `${row.plannedWeight} KG` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="materialBatchNo" label="原料批号" min-width="120">
        <template #default="{ row }">
          {{ row.materialBatchNo || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <DictTag
            :type="DICT_TYPE.LUNAERP_WEAVING_TICKET_STATUS"
            :value="row.status"
          />
        </template>
      </el-table-column>
      <el-table-column label="异常信息" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <template v-if="row.exceptionType">
            <DictTag
              :type="DICT_TYPE.LUNAERP_WEAVING_EXCEPTION_TYPE"
              :value="row.exceptionType"
            />
            <span v-if="row.exceptionNote" class="text-gray-500 text-12px ml-1">
              ({{ row.exceptionNote }})
            </span>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="startedAt" label="开工时间" width="150" align="center">
        <template #default="{ row }">
          <span class="text-12px">{{ formatDateTime(row.startedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="completedAt" label="完工时间" width="150" align="center">
        <template #default="{ row }">
          <span class="text-12px">{{ formatDateTime(row.completedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="作废说明" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.status === 'VOID'" class="text-red-500 text-12px">
            {{ row.voidReason || '已作废' }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <!-- 开工：允许 GENERATED / STARTED / EXCEPTION -->
          <el-button
            v-if="['GENERATED', 'STARTED', 'EXCEPTION'].includes(row.status)"
            type="primary"
            link
            @click="handleSingleAction('start', row)"
          >
            开工
          </el-button>
          <!-- 完工：允许 STARTED -->
          <el-button
            v-if="row.status === 'STARTED'"
            type="success"
            link
            @click="handleSingleAction('complete', row)"
          >
            完工
          </el-button>
          <!-- 异常：允许 GENERATED / STARTED -->
          <el-button
            v-if="['GENERATED', 'STARTED'].includes(row.status)"
            type="warning"
            link
            @click="handleSingleAction('exception', row)"
          >
            异常
          </el-button>
          <!-- 作废：允许 GENERATED / STARTED / EXCEPTION -->
          <el-button
            v-if="['GENERATED', 'STARTED', 'EXCEPTION'].includes(row.status)"
            type="danger"
            link
            @click="handleSingleAction('void', row)"
          >
            作废
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无布票记录" :image-size="80" />
      </template>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="loadTickets"
    />

    <!-- 弹窗 1：批量生成布票 -->
    <el-dialog
      v-model="createBatchDialogVisible"
      title="按机台任务批量生成布票"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="createBatchFormRef"
        :model="createBatchForm"
        :rules="createBatchRules"
        label-width="110px"
      >
        <el-form-item label="机台任务" prop="machineTaskId">
          <el-select
            v-model="createBatchForm.machineTaskId"
            placeholder="请选择机台任务"
            class="w-full"
            @change="handleCreateTaskChange"
          >
            <el-option
              v-for="task in taskOptions"
              :key="task.id"
              :label="`${task.code} - 织机【${task.loomNo}】(计划${task.plannedQty}匹 / 实织${task.actualQty || 0}匹)`"
              :value="task.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生成匹数" prop="count">
          <el-input-number
            v-model="createBatchForm.count"
            :min="1"
            :max="1000"
            :step="1"
            controls-position="right"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="原料批号" prop="materialBatchNo">
          <el-input v-model="createBatchForm.materialBatchNo" placeholder="可填原料批次号" />
        </el-form-item>
        <el-form-item label="挡车工">
          <el-select
            v-model="createBatchForm.workerUserId"
            placeholder="默认带出任务挡车工，可手改"
            clearable
            filterable
            class="w-full"
            @change="handleBatchWorkerChange"
          >
            <el-option
              v-for="worker in workerOptions"
              :key="worker.id"
              :label="worker.nickname || worker.username"
              :value="worker.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="createBatchDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="actionSubmitting" @click="submitCreateBatch">
            立即生成
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗 2：开工操作弹窗（单匹/批量） -->
    <el-dialog
      v-model="startDialogVisible"
      :title="isBatchMode ? `批量开工 (${operatingIds.length} 匹)` : `布票开工 【${currentOperatingTicket?.code || ''}】`"
      width="480px"
      append-to-body
      destroy-on-close
    >
      <el-form :model="startForm" label-width="100px">
        <el-form-item label="实际上机机台">
          <el-input v-model="startForm.actualMachine" placeholder="默认预排机台，可修改" />
        </el-form-item>
        <el-form-item label="挡车工">
          <el-select
            v-model="startForm.workerUserId"
            placeholder="请选择挡车工"
            clearable
            filterable
            class="w-full"
            @change="handleStartWorkerChange"
          >
            <el-option
              v-for="worker in workerOptions"
              :key="worker.id"
              :label="worker.nickname || worker.username"
              :value="worker.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开工时间">
          <el-date-picker
            v-model="startForm.startedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="默认当前时间"
            class="!w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="startDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="actionSubmitting" @click="submitStartAction">
            确认开工
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗 3：异常登记弹窗（单匹/批量） -->
    <el-dialog
      v-model="exceptionDialogVisible"
      :title="isBatchMode ? `批量登记异常 (${operatingIds.length} 匹)` : `异常登记 【${currentOperatingTicket?.code || ''}】`"
      width="480px"
      append-to-body
      destroy-on-close
    >
      <el-form ref="exceptionFormRef" :model="exceptionForm" :rules="exceptionRules" label-width="100px">
        <el-form-item label="异常类型" prop="exceptionType">
          <el-select v-model="exceptionForm.exceptionType" placeholder="请选择异常类型" class="w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_WEAVING_EXCEPTION_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异常说明" prop="exceptionNote">
          <el-input v-model="exceptionForm.exceptionNote" type="textarea" :rows="3" placeholder="请输入异常详细情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="exceptionDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="actionSubmitting" @click="submitExceptionAction">
            提交异常
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗 4：作废弹窗（单匹/批量） -->
    <el-dialog
      v-model="voidDialogVisible"
      :title="isBatchMode ? `批量作废 (${operatingIds.length} 匹)` : `作废确认 【${currentOperatingTicket?.code || ''}】`"
      width="480px"
      append-to-body
      destroy-on-close
    >
      <el-form ref="voidFormRef" :model="voidForm" :rules="voidRules" label-width="100px">
        <el-form-item label="作废原因" prop="voidReason">
          <el-input
            v-model="voidForm.voidReason"
            type="textarea"
            :rows="3"
            placeholder="请输入作废原因（必填，终态不可逆）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="voidDialogVisible = false">取消</el-button>
          <el-button type="danger" :loading="actionSubmitting" @click="submitVoidAction">
            确认作废
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import dayjs from 'dayjs'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import {
  WeavingTicketVO,
  WeavingTicketPageParam,
  getWeavingTicketPage,
  getWeavingTicketByCode,
  createWeavingTickets,
  startWeavingTicket,
  completeWeavingTicket,
  exceptionWeavingTicket,
  voidWeavingTicket
} from '@/api/lunaerp-elmui/production/weaving-ticket'
import { MachineTaskVO, getMachineTaskPage } from '@/api/lunaerp-elmui/production/machine-task'
import { getSimpleUserList, UserVO } from '@/api/system/user'

const props = defineProps<{
  weavingOrderId?: number
}>()

const emit = defineEmits<{
  (e: 'refresh-order'): void
}>()

const loading = ref(false)
const actionSubmitting = ref(false)
const total = ref(0)
const ticketList = ref<WeavingTicketVO[]>([])
const selectedRows = ref<WeavingTicketVO[]>([])
const taskOptions = ref<MachineTaskVO[]>([])
const workerOptions = ref<UserVO[]>([])
const queryFormRef = ref<FormInstance>()
const scanBarcode = ref('')

const queryParams = ref<WeavingTicketPageParam>({
  pageNo: 1,
  pageSize: 10,
  weavingOrderId: props.weavingOrderId,
  machineTaskId: undefined,
  status: undefined,
  code: undefined
})

// 状态与操作弹窗控制
const isBatchMode = ref(false)
const operatingIds = ref<number[]>([])
const currentOperatingTicket = ref<WeavingTicketVO>()

// 批量生成弹窗
const createBatchDialogVisible = ref(false)
const createBatchFormRef = ref<FormInstance>()
const createBatchForm = ref({
  machineTaskId: undefined as number | undefined,
  count: 1,
  materialBatchNo: '',
  workerUserId: undefined as number | undefined,
  workerName: undefined as string | undefined
})
const createBatchRules = {
  machineTaskId: [{ required: true, message: '请选择机台任务', trigger: 'change' }],
  count: [{ required: true, message: '生成匹数必须大于0', trigger: 'blur' }]
}

// 开工弹窗
const startDialogVisible = ref(false)
const startForm = ref({
  actualMachine: '',
  workerUserId: undefined as number | undefined,
  workerName: undefined as string | undefined,
  startedAt: ''
})

// 异常弹窗
const exceptionDialogVisible = ref(false)
const exceptionFormRef = ref<FormInstance>()
const exceptionForm = ref({
  exceptionType: '',
  exceptionNote: ''
})
const exceptionRules = {
  exceptionType: [{ required: true, message: '请选择异常类型', trigger: 'change' }]
}

// 作废弹窗
const voidDialogVisible = ref(false)
const voidFormRef = ref<FormInstance>()
const voidForm = ref({
  voidReason: ''
})
const voidRules = {
  voidReason: [{ required: true, message: '请输入作废原因', trigger: 'blur' }]
}

const formatDateTime = (val?: string) => {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm:ss')
}

// 加载布票分页
const loadTickets = async () => {
  if (!props.weavingOrderId) return
  loading.value = true
  queryParams.value.weavingOrderId = props.weavingOrderId
  try {
    const res = await getWeavingTicketPage(queryParams.value)
    ticketList.value = res.list || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载布票列表失败')
  } finally {
    loading.value = false
  }
}

// 加载机台任务选项
const loadTasks = async () => {
  if (!props.weavingOrderId) return
  try {
    const res = await getMachineTaskPage({
      pageNo: 1,
      pageSize: 100,
      weavingOrderId: props.weavingOrderId
    })
    taskOptions.value = res.list || []
  } catch (e) {
    console.error('加载机台任务失败', e)
  }
}

// 加载工人列表
const loadWorkers = async () => {
  try {
    const users = await getSimpleUserList()
    workerOptions.value = users || []
  } catch (e) {
    console.error('加载人员列表失败', e)
  }
}

const handleSelectionChange = (rows: WeavingTicketVO[]) => {
  selectedRows.value = rows
}

const handleQuery = () => {
  queryParams.value.pageNo = 1
  loadTickets()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 顶部扫码枪录入识别
const handleBarcodeScan = async () => {
  const code = scanBarcode.value.trim()
  if (!code) {
    ElMessage.warning('请输入或扫描布票编号')
    return
  }
  try {
    const ticket = await getWeavingTicketByCode(code)
    if (!ticket || !ticket.id) {
      ElMessage.error(`未找到布票【${code}】`)
      return
    }
    if (ticket.weavingOrderId !== props.weavingOrderId) {
      ElMessage.warning(`布票【${code}】属于其它织造单 (ID: ${ticket.weavingOrderId})`)
      return
    }

    ElMessage.success(`成功识别布票【${code}】，当前状态：${ticket.status}`)
    // 根据票态合理引导操作：已生成→开工，已开工→完工，异常中→重新开工/恢复，已终态→定位展示
    if (ticket.status === 'GENERATED') {
      handleSingleAction('start', ticket)
    } else if (ticket.status === 'STARTED') {
      handleSingleAction('complete', ticket)
    } else if (ticket.status === 'EXCEPTION') {
      handleSingleAction('start', ticket)
    } else {
      ElMessage.info(`布票【${code}】当前处于【${ticket.status}】终态`)
      queryParams.value.code = code
      handleQuery()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '识别布票失败')
  }
}

// 打开批量生成布票弹窗
const handleOpenCreateBatchDialog = () => {
  createBatchForm.value = {
    machineTaskId: taskOptions.value.length > 0 ? taskOptions.value[0].id : undefined,
    count: 1,
    materialBatchNo: '',
    workerUserId: undefined,
    workerName: undefined
  }
  if (createBatchForm.value.machineTaskId) {
    handleCreateTaskChange(createBatchForm.value.machineTaskId)
  }
  createBatchDialogVisible.value = true
}

const handleCreateTaskChange = (taskId?: number) => {
  if (!taskId) return
  const task = taskOptions.value.find(t => t.id === taskId)
  if (task) {
    createBatchForm.value.workerUserId = task.workerUserId
    createBatchForm.value.workerName = task.workerName
    // 计算剩余可生成数
    if (task.plannedQty) {
      const remain = Math.max(1, (task.plannedQty - (task.actualQty || 0)))
      createBatchForm.value.count = remain
    }
  }
}

const handleBatchWorkerChange = (val?: number) => {
  if (val) {
    const user = workerOptions.value.find(u => u.id === val)
    createBatchForm.value.workerName = user ? (user.nickname || user.username) : undefined
  } else {
    createBatchForm.value.workerName = undefined
  }
}

// 提交批量生成
const submitCreateBatch = async () => {
  if (!createBatchFormRef.value) return
  await createBatchFormRef.value.validate()

  const selectedTask = taskOptions.value.find(t => t.id === createBatchForm.value.machineTaskId)
  if (selectedTask?.plannedQty) {
    const remain = Math.max(0, selectedTask.plannedQty - (selectedTask.actualQty || 0))
    if (createBatchForm.value.count > remain) {
      ElMessage.warning(`提示：生成匹数 (${createBatchForm.value.count}) 超出机台任务剩余量 (${remain} 匹)`)
    }
  }

  actionSubmitting.value = true
  try {
    const res = await createWeavingTickets({
      machineTaskId: createBatchForm.value.machineTaskId!,
      count: createBatchForm.value.count,
      materialBatchNo: createBatchForm.value.materialBatchNo || undefined,
      workerUserId: createBatchForm.value.workerUserId,
      workerName: createBatchForm.value.workerName
    })
    ElMessage.success(`成功生成 ${res?.length || createBatchForm.value.count} 张布票`)
    createBatchDialogVisible.value = false
    await loadTickets()
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    ElMessage.error(e?.message || '生成布票失败')
  } finally {
    actionSubmitting.value = false
  }
}

// 打开单票操作弹窗
const handleSingleAction = (action: 'start' | 'complete' | 'exception' | 'void', row: WeavingTicketVO) => {
  isBatchMode.value = false
  currentOperatingTicket.value = row
  operatingIds.value = [row.id!]

  if (action === 'start') {
    startForm.value = {
      actualMachine: row.actualMachine || row.plannedMachine || '',
      workerUserId: row.workerUserId,
      workerName: row.workerName,
      startedAt: dayjs().format('YYYY-MM-DD HH:mm:ss')
    }
    startDialogVisible.value = true
  } else if (action === 'complete') {
    handleCompleteConfirm([row.id!])
  } else if (action === 'exception') {
    exceptionForm.value = {
      exceptionType: row.exceptionType || '',
      exceptionNote: row.exceptionNote || ''
    }
    exceptionDialogVisible.value = true
  } else if (action === 'void') {
    voidForm.value = {
      voidReason: ''
    }
    voidDialogVisible.value = true
  }
}

// 打开批量操作
const handleBatchAction = (action: 'start' | 'complete' | 'exception' | 'void') => {
  if (selectedRows.value.length === 0) return
  isBatchMode.value = true
  currentOperatingTicket.value = undefined
  operatingIds.value = selectedRows.value.map(r => r.id!)

  if (action === 'start') {
    startForm.value = {
      actualMachine: '',
      workerUserId: undefined,
      workerName: undefined,
      startedAt: dayjs().format('YYYY-MM-DD HH:mm:ss')
    }
    startDialogVisible.value = true
  } else if (action === 'complete') {
    handleCompleteConfirm(operatingIds.value)
  } else if (action === 'exception') {
    exceptionForm.value = {
      exceptionType: '',
      exceptionNote: ''
    }
    exceptionDialogVisible.value = true
  } else if (action === 'void') {
    voidForm.value = {
      voidReason: ''
    }
    voidDialogVisible.value = true
  }
}

const handleStartWorkerChange = (val?: number) => {
  if (val) {
    const user = workerOptions.value.find(u => u.id === val)
    startForm.value.workerName = user ? (user.nickname || user.username) : undefined
  } else {
    startForm.value.workerName = undefined
  }
}

// 提交开工
const submitStartAction = async () => {
  actionSubmitting.value = true
  try {
    await startWeavingTicket({
      ids: operatingIds.value,
      actualMachine: startForm.value.actualMachine || undefined,
      workerUserId: startForm.value.workerUserId,
      workerName: startForm.value.workerName,
      startedAt: startForm.value.startedAt || undefined
    })
    ElMessage.success(`开工成功（${operatingIds.value.length} 匹）`)
    startDialogVisible.value = false
    await loadTickets()
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    ElMessage.error(e?.message || '开工操作失败')
  } finally {
    actionSubmitting.value = false
  }
}

// 完工确认
const handleCompleteConfirm = async (ids: number[]) => {
  try {
    await ElMessageBox.confirm(`确认完工 ${ids.length} 匹布票？`, '完工确认', {
      type: 'info',
      confirmButtonText: '确认完工',
      cancelButtonText: '取消'
    })
    actionSubmitting.value = true
    await completeWeavingTicket({ ids, completedAt: dayjs().format('YYYY-MM-DD HH:mm:ss') })
    ElMessage.success('完工记录已更新')
    await loadTickets()
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '完工操作失败')
  } finally {
    actionSubmitting.value = false
  }
}

// 提交异常
const submitExceptionAction = async () => {
  if (!exceptionFormRef.value) return
  await exceptionFormRef.value.validate()

  actionSubmitting.value = true
  try {
    await exceptionWeavingTicket({
      ids: operatingIds.value,
      exceptionType: exceptionForm.value.exceptionType,
      exceptionNote: exceptionForm.value.exceptionNote || undefined
    })
    ElMessage.success(`登记异常成功（${operatingIds.value.length} 匹）`)
    exceptionDialogVisible.value = false
    await loadTickets()
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    ElMessage.error(e?.message || '异常登记失败')
  } finally {
    actionSubmitting.value = false
  }
}

// 提交作废
const submitVoidAction = async () => {
  if (!voidFormRef.value) return
  await voidFormRef.value.validate()

  actionSubmitting.value = true
  try {
    await voidWeavingTicket({
      ids: operatingIds.value,
      voidReason: voidForm.value.voidReason
    })
    ElMessage.success(`布票作废成功（${operatingIds.value.length} 匹）`)
    voidDialogVisible.value = false
    await loadTickets()
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    ElMessage.error(e?.message || '作废操作失败')
  } finally {
    actionSubmitting.value = false
  }
}


watch(
  () => props.weavingOrderId,
  (id) => {
    if (id) {
      loadTickets()
      loadTasks()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadWorkers()
})

defineExpose({
  loadTickets
})
</script>

<style scoped>
.tickets-tab {
  padding: 16px 0;
}
</style>
