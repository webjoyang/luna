<template>
  <div class="tasks-tab" v-loading="loading">
    <div class="flex justify-between items-center mb-16px">
      <div class="flex items-center gap-2">
        <span class="font-bold text-15px">机台任务列表</span>
        <span class="text-gray-400 text-12px">（派工到织机排产与实织汇总）</span>
      </div>
      <el-button
        type="primary"
        size="small"
        @click="handleOpenTaskDialog()"
        v-hasPermi="['lunaerp:production:weaving-order-task:create']"
      >
        <Icon icon="ep:plus" class="mr-1" /> 新建机台任务
      </el-button>
    </div>

    <el-table :data="taskList" border stripe style="width: 100%">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="code" label="任务编号" min-width="170">
        <template #default="{ row }">
          <span class="font-mono font-bold text-gray-800">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="loomNo" label="织机号" min-width="120" />
      <el-table-column prop="workerName" label="挡车工" min-width="110">
        <template #default="{ row }">
          {{ row.workerName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="plannedQty" label="计划匹数" width="100" align="right" />
      <el-table-column prop="actualQty" label="实织匹数" width="100" align="right">
        <template #default="{ row }">
          <span class="font-bold text-green-600">{{ row.actualQty ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110" align="center">
        <template #default="{ row }">
          <DictTag
            :type="DICT_TYPE.LUNAERP_WEAVING_TASK_STATUS"
            :value="row.status"
          />
        </template>
      </el-table-column>
      <el-table-column label="排期" min-width="180">
        <template #default="{ row }">
          <span v-if="row.plannedStart || row.plannedEnd">
            {{ row.plannedStart || '未定' }} ~ {{ row.plannedEnd || '未定' }}
          </span>
          <span v-else class="text-gray-400">未排期</span>
        </template>
      </el-table-column>
      <el-table-column prop="haltReason" label="停机原因" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="handlePrintTaskTickets(row)"
            v-hasPermi="['lunaerp:production:weaving-ticket:print']"
          >
            打印布票
          </el-button>
          <template v-if="row.status === 'PENDING'">
            <el-button
              type="primary"
              link
              @click="handleOpenTaskDialog(row)"
              v-hasPermi="['lunaerp:production:weaving-order-task:update']"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              @click="handleDeleteTask(row)"
              v-hasPermi="['lunaerp:production:weaving-order-task:delete']"
            >
              删除
            </el-button>
          </template>
          <template v-else-if="row.status === 'IN_PROGRESS'">
            <el-button
              type="danger"
              link
              @click="handleHaltTask(row)"
              v-hasPermi="['lunaerp:production:weaving-order-task:update']"
            >
              标停机
            </el-button>
          </template>
          <template v-else-if="row.status === 'HALTED'">
            <el-button
              type="success"
              link
              @click="handleResumeTask(row)"
              v-hasPermi="['lunaerp:production:weaving-order-task:update']"
            >
              恢复
            </el-button>
          </template>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无机台任务" :image-size="80" />
      </template>
    </el-table>

    <!-- 任务编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="taskForm.id ? '编辑机台任务' : '新建机台任务'"
      width="550px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="taskForm"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item label="任务编号" prop="code">
          <el-input
            v-model="taskForm.code"
            :disabled="!!taskForm.id"
            :placeholder="taskForm.id ? '' : (previewTaskCode || '为空系统自动派生 MT-...') "
          />
        </el-form-item>
        <el-form-item label="选择织机" prop="loomId">
          <el-select v-model="taskForm.loomId" placeholder="请选择织机 (仅活跃)" class="w-full">
            <el-option
              v-for="loom in loomOptions"
              :key="loom.id!"
              :label="loom.loomNo + (loom.model ? ' (' + loom.model + ')' : '')"
              :value="loom.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="挡车工" prop="workerUserId">
          <el-select
            v-model="taskForm.workerUserId"
            placeholder="请选择挡车工（可空）"
            clearable
            filterable
            class="w-full"
            @change="handleWorkerChange"
          >
            <el-option
              v-for="worker in workerOptions"
              :key="worker.id"
              :label="worker.nickname || worker.username"
              :value="worker.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划匹数" prop="plannedQty">
          <el-input-number
            v-model="taskForm.plannedQty"
            :min="1"
            :step="1"
            controls-position="right"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="计划排期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="!w-full"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="taskForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saveLoading" @click="handleSaveTask">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 统一打印分发器 -->
    <PrintDispatcher
      ref="printDispatcherRef"
      biz-type="WEAVING_TICKET"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import { PrintDispatcher } from '@/components/Print'
import { getWeavingTicketPage } from '@/api/lunaerp-elmui/production/weaving-ticket'
import {
  MachineTaskVO,
  MachineTaskSaveReqVO,
  getMachineTaskPage,
  createMachineTask,
  updateMachineTask,
  deleteMachineTask,
  haltMachineTask,
  resumeMachineTask,
  getNextMachineTaskCode
} from '@/api/lunaerp-elmui/production/machine-task'
import { getLoomPage, LoomVO } from '@/api/lunaerp-elmui/production/loom'
import { getSimpleUserList, UserVO } from '@/api/system/user'
import { getSimplePostList, PostVO } from '@/api/system/post'

const props = defineProps<{
  weavingOrderId?: number
}>()

const emit = defineEmits<{
  (e: 'refresh-order'): void
}>()

const loading = ref(false)
const saveLoading = ref(false)
const taskList = ref<MachineTaskVO[]>([])
const loomOptions = ref<LoomVO[]>([])
const workerOptions = ref<UserVO[]>([])
const previewTaskCode = ref('')
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const dateRange = ref<string[]>([])

const taskForm = ref<MachineTaskSaveReqVO>({
  weavingOrderId: props.weavingOrderId,
  plannedQty: 1
})

const formRules = {
  loomId: [{ required: true, message: '请选择织机', trigger: 'change' }],
  plannedQty: [{ required: true, message: '计划匹数不能为空', trigger: 'blur' }]
}

const loadTasks = async () => {
  if (!props.weavingOrderId) return
  loading.value = true
  try {
    const res = await getMachineTaskPage({
      pageNo: 1,
      pageSize: 100,
      weavingOrderId: props.weavingOrderId
    })
    taskList.value = res.list || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载机台任务失败')
  } finally {
    loading.value = false
  }
}

const loadLooms = async () => {
  try {
    const res = await getLoomPage({ pageNo: 1, pageSize: 100, status: 'ACTIVE' })
    loomOptions.value = res.list || []
  } catch (e) {
    console.error('加载织机选项失败', e)
  }
}

// 加载挡车工人员选项（复用 system User + system_post 过滤，ADR-0021）
const loadWorkers = async () => {
  try {
    const [users, posts] = await Promise.all([
      getSimpleUserList(),
      getSimplePostList().catch(() => [] as PostVO[])
    ])

    // 寻找挡车工岗位 ID
    const workerPost = posts.find(p => p.name?.includes('挡车') || p.code?.toLowerCase().includes('worker'))
    if (workerPost && workerPost.id) {
      const postIdStr = String(workerPost.id)
      const filtered = users.filter(u => u.postIds && u.postIds.map(String).includes(postIdStr))
      workerOptions.value = filtered.length > 0 ? filtered : users
    } else {
      workerOptions.value = users || []
    }
  } catch (e) {
    console.error('加载挡车工列表失败', e)
  }
}

const handleWorkerChange = (val?: number) => {
  if (val) {
    const user = workerOptions.value.find(u => u.id === val)
    taskForm.value.workerName = user ? (user.nickname || user.username) : undefined
  } else {
    taskForm.value.workerName = undefined
  }
}

const handleOpenTaskDialog = async (row?: MachineTaskVO) => {
  if (row) {
    taskForm.value = {
      id: row.id,
      code: row.code,
      weavingOrderId: row.weavingOrderId,
      loomId: row.loomId,
      workerUserId: row.workerUserId,
      workerName: row.workerName,
      plannedQty: row.plannedQty,
      plannedStart: row.plannedStart,
      plannedEnd: row.plannedEnd,
      remark: row.remark
    }
    dateRange.value = row.plannedStart && row.plannedEnd ? [row.plannedStart, row.plannedEnd] : []
    previewTaskCode.value = ''
  } else {
    taskForm.value = {
      weavingOrderId: props.weavingOrderId,
      plannedQty: 1
    }
    dateRange.value = []
    if (props.weavingOrderId) {
      try {
        previewTaskCode.value = await getNextMachineTaskCode(props.weavingOrderId)
      } catch (err) {
        previewTaskCode.value = ''
      }
    }
  }
  dialogVisible.value = true
}

const handleDateRangeChange = (val: any) => {
  if (val && val.length === 2) {
    taskForm.value.plannedStart = val[0]
    taskForm.value.plannedEnd = val[1]
  } else {
    taskForm.value.plannedStart = undefined
    taskForm.value.plannedEnd = undefined
  }
}

const handleSaveTask = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  taskForm.value.weavingOrderId = props.weavingOrderId
  saveLoading.value = true
  try {
    if (taskForm.value.id) {
      const res = await updateMachineTask(taskForm.value)
      ElMessage.success('修改成功')
      if (res.warning) ElMessage.warning(res.warning)
    } else {
      const res = await createMachineTask(taskForm.value)
      ElMessage.success('创建成功')
      if (res.warning) ElMessage.warning(res.warning)
    }
    dialogVisible.value = false
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleDeleteTask = async (row: MachineTaskVO) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除机台任务【${row.code}】？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteMachineTask(row.id)
    ElMessage.success('删除成功')
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const handleHaltTask = async (row: MachineTaskVO) => {
  if (!row.id) return
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入停机原因', '标记停机', {
      confirmButtonText: '确认停机',
      cancelButtonText: '取消',
      inputValidator: (v) => (!v || !v.trim() ? '停机原因不能为空' : true)
    })
    await haltMachineTask({ id: row.id, haltReason: reason.trim() })
    ElMessage.success('已标记停机')
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '停机操作失败')
  }
}

const handleResumeTask = async (row: MachineTaskVO) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm('确认恢复该机台任务？', '提示', {
      type: 'info'
    })
    await resumeMachineTask(row.id)
    ElMessage.success('已恢复')
    await loadTasks()
    emit('refresh-order')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '恢复操作失败')
  }
}

const printDispatcherRef = ref<InstanceType<typeof PrintDispatcher> | null>(null)

// 打印指定机台任务的全部布票
const handlePrintTaskTickets = async (row: MachineTaskVO) => {
  if (!row.id) return
  try {
    const res = await getWeavingTicketPage({
      machineTaskId: row.id,
      pageSize: 500
    })
    const tickets = res?.list || []
    if (tickets.length === 0) {
      ElMessage.warning(`机台任务【${row.code}】尚未生成布票`)
      return
    }
    const bizIds = tickets.map((t: any) => t.id)
    printDispatcherRef.value?.open({ bizIds })
  } catch (err: any) {
    ElMessage.error(err?.message || '获取布票数据失败')
  }
}

watch(
  () => props.weavingOrderId,
  (id) => {
    if (id) loadTasks()
  },
  { immediate: true }
)

onMounted(() => {
  loadLooms()
  loadWorkers()
})

defineExpose({
  loadTasks
})
</script>

<style scoped>
.tasks-tab {
  padding: 16px 0;
}
</style>
