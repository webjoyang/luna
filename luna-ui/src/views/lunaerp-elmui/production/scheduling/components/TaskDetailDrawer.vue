<template>
  <el-drawer v-model="drawerVisible" title="机台任务详情" size="520px" destroy-on-close>
    <div v-if="task" class="task-detail-content flex flex-col gap-5">
      <!-- 异常状态提示 -->
      <el-alert
        v-if="task.status === 'HALTED'"
        type="warning"
        :closable="false"
        show-icon
        title="任务已暂停异常"
        description="当前任务处于停机状态，排期与转机已被锁定。请先前往织造单工作台处理异常并恢复任务。"
      />

      <el-alert
        v-if="task.conflict"
        type="error"
        :closable="false"
        show-icon
        title="时段冲突预警"
        description="该任务与同织机其它任务在计划时段上存在重叠（存量或并发数据），建议通过转机或拖拽改期进行避让。"
      />

      <!-- 基本信息 -->
      <el-descriptions title="任务概要" :column="2" border size="small">
        <el-descriptions-item label="任务编号" :span="2">
          <span class="font-mono font-bold text-gray-800">{{ task.taskCode }}</span>
        </el-descriptions-item>

        <el-descriptions-item label="任务状态">
          <el-tag :type="statusTheme?.tagType || 'info'" size="small">
            {{ statusTheme?.label || task.status }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="冲突状态">
          <el-tag v-if="task.conflict" type="danger" size="small" effect="dark"> 时段冲突 </el-tag>
          <el-tag v-else type="success" size="small" effect="plain"> 正常 </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="关联织造单" :span="2">
          <div class="flex items-center justify-between">
            <span class="font-mono font-semibold">{{ task.weavingOrderCode }}</span>
            <el-button type="primary" link size="small" @click="handleJumpToWorkspace">
              <Icon icon="ep:top-right" class="mr-0.5" /> 跳转工作台
            </el-button>
          </div>
        </el-descriptions-item>

        <el-descriptions-item label="产品名称" :span="2">
          <span
            >{{ task.productCode ? `[${task.productCode}] ` : ''
            }}{{ task.productName || '-' }}</span
          >
        </el-descriptions-item>

        <el-descriptions-item label="当前织机">
          <span class="font-bold">{{ currentLoomName }}</span>
        </el-descriptions-item>

        <el-descriptions-item label="挡车工">
          {{ task.workerName || '未指定' }}
        </el-descriptions-item>

        <el-descriptions-item label="计划匹数">
          <span class="font-semibold">{{ task.plannedQty }} 匹</span>
        </el-descriptions-item>

        <el-descriptions-item label="实织匹数">
          <span class="text-green-600 font-bold">{{ task.actualQty ?? 0 }} 匹</span>
        </el-descriptions-item>

        <el-descriptions-item label="计划排期" :span="2">
          <div class="flex items-center gap-1 font-mono text-xs">
            <Icon icon="ep:calendar" class="text-gray-400" />
            <span>{{ task.startDate }} ~ {{ task.endDate }}</span>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 转机操作区（按 spike 定案：条带点击弹窗/抽屉下拉转机） -->
      <div
        v-if="canTransfer"
        class="transfer-section p-3 border border-[var(--el-border-color-light)] rounded-md bg-[var(--el-fill-color-blank)]"
      >
        <div class="font-bold text-sm text-gray-700 mb-2 flex items-center gap-1">
          <Icon icon="ep:switch" class="text-primary" />
          <span>转机调度（改派目标织机）</span>
        </div>
        <div class="text-xs text-gray-400 mb-3">
          平移与拉伸改期直接在甘特图上拖动；改换织机请在此选择目标机台，保存将执行同织机时段冲突硬校验。
        </div>

        <el-alert
          v-if="transferConflictError"
          type="error"
          :closable="false"
          show-icon
          class="mb-3"
        >
          <template #title>
            <span class="font-bold">转机冲突拦截</span>
          </template>
          <div class="text-xs text-red-600">{{ transferConflictError }}</div>
        </el-alert>

        <div class="flex items-center gap-2">
          <el-select v-model="targetLoomId" placeholder="选择目标织机" filterable class="flex-1">
            <el-option
              v-for="loom in availableLooms"
              :key="loom.id!"
              :label="`${loom.loomNo}${loom.model ? ' (' + loom.model + ')' : ''}`"
              :value="loom.id!"
            />
          </el-select>
          <el-button
            type="primary"
            :loading="transferring"
            :disabled="!targetLoomId || targetLoomId === task.loomId"
            @click="handleTransferLoom"
          >
            确认转机
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { LoomVO } from '@/api/lunaerp-elmui/production/loom'
import { updateMachineTask } from '@/api/lunaerp-elmui/production/machine-task'
import { TASK_STATUS_THEME_MAP, type MachineTaskStatus } from '../gantt-theme'
import type { MachineTaskGanttBar } from '../model/gantt-adapter'

const props = withDefaults(
  defineProps<{
    loomList?: LoomVO[]
  }>(),
  {
    loomList: () => []
  }
)

const emit = defineEmits<{
  (e: 'transfer-success'): void
}>()

const router = useRouter()
const drawerVisible = ref(false)
const task = ref<MachineTaskGanttBar | null>(null)
const targetLoomId = ref<number | undefined>(undefined)
const transferring = ref(false)
const transferConflictError = ref('')

const statusTheme = computed(() => {
  if (!task.value) return null
  return TASK_STATUS_THEME_MAP[task.value.status as MachineTaskStatus]
})

const currentLoomName = computed(() => {
  if (!task.value) return '-'
  const loom = props.loomList.find((l) => l.id === task.value?.loomId)
  if (loom) {
    return `${loom.loomNo}${loom.model ? ' (' + loom.model + ')' : ''}`
  }
  return `机台 #${task.value.loomId}`
})

const canTransfer = computed(() => {
  if (!task.value) return false
  return task.value.status === 'PENDING' || task.value.status === 'IN_PROGRESS'
})

const availableLooms = computed(() => {
  return props.loomList.filter((l) => l.id !== task.value?.loomId)
})

function open(targetTask: MachineTaskGanttBar) {
  task.value = targetTask
  targetLoomId.value = undefined
  transferConflictError.value = ''
  drawerVisible.value = true
}

defineExpose({
  open
})

function handleJumpToWorkspace() {
  if (!task.value?.weavingOrderId) return
  router.push({
    path: `/production/weaving-order/workspace/${task.value.weavingOrderId}`
  })
}

async function handleTransferLoom() {
  if (!task.value || !targetLoomId.value) return
  transferring.value = true
  transferConflictError.value = ''

  try {
    const res = await updateMachineTask({
      id: task.value.taskId,
      loomId: targetLoomId.value,
      plannedStart: task.value.startDate,
      plannedEnd: task.value.endDate,
      plannedQty: task.value.plannedQty
    })

    if (res.warning) {
      ElMessage.warning(res.warning)
    }
    ElMessage.success('转机调度成功')
    drawerVisible.value = false
    emit('transfer-success')
  } catch (err: any) {
    const errorMsg = err?.message || err?.msg || (typeof err === 'string' ? err : '转机操作失败')
    if (err?.code === 1030036000 || errorMsg.includes('冲突')) {
      transferConflictError.value = errorMsg
    } else {
      ElMessage.error(errorMsg)
    }
  } finally {
    transferring.value = false
  }
}
</script>

<style scoped lang="scss">
.task-detail-content {
  padding: 4px;
}
</style>
