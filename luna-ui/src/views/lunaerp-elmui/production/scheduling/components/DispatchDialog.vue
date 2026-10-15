<template>
  <el-dialog
    v-model="dialogVisible"
    :title="form.id ? '机台任务排期 / 修改' : '新建机台任务派工'"
    width="580px"
    append-to-body
    destroy-on-close
    @closed="handleDialogClosed"
  >
    <!-- 冲突硬拦截红字警示框（000 错误时保持弹窗打开） -->
    <el-alert v-if="conflictMessage" type="error" :closable="false" show-icon class="mb-4">
      <template #title>
        <span class="font-bold">机台排产时段冲突拦截 (1-030-036-000)</span>
      </template>
      <div class="text-sm mt-1 text-red-600 font-medium">
        {{ conflictMessage }}
      </div>
      <div class="text-xs text-gray-500 mt-1">
        当前织机在所选起止时段内已有生产任务占用，请调整起止日期或选择其它可用机台。
      </div>
    </el-alert>

    <el-form
      ref="formRef"
      :model="form"
      label-width="110px"
      label-position="right"
      v-loading="submitting"
    >
      <!-- 织造单选择 -->
      <el-form-item label="关联织造单" required>
        <div v-if="lockedWeavingOrder" class="flex items-center gap-2 w-full">
          <el-input
            :model-value="form.weavingOrderCode || ''"
            disabled
            class="flex-1 font-mono font-bold"
          />
          <el-button
            v-if="!form.id"
            link
            type="primary"
            size="small"
            @click="lockedWeavingOrder = false"
          >
            更换
          </el-button>
        </div>
        <el-select
          v-else
          v-model="form.weavingOrderId"
          filterable
          remote
          reserve-keyword
          placeholder="输入单号或产品搜索织造单"
          :remote-method="searchWeavingOrders"
          :loading="searchingOrders"
          class="w-full"
          @change="handleWeavingOrderChange"
        >
          <el-option
            v-for="order in orderOptions"
            :key="order.id!"
            :label="`${order.code} (${order.productName || order.productCode || '-'})`"
            :value="order.id!"
          />
        </el-select>
      </el-form-item>

      <!-- 目标织机 -->
      <el-form-item label="生产织机" required>
        <el-select
          v-model="form.loomId"
          placeholder="请选择织机 (仅启用机台)"
          filterable
          class="w-full"
          @change="handleLoomChange"
        >
          <el-option
            v-for="loom in loomList"
            :key="loom.id!"
            :label="`${loom.loomNo}${loom.model ? ' (' + loom.model + ')' : ''}`"
            :value="loom.id!"
          />
        </el-select>
      </el-form-item>

      <!-- 计划排期 -->
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

      <!-- 计划匹数 -->
      <el-form-item label="计划匹数" required>
        <el-input-number
          v-model="form.plannedQty"
          :min="1"
          :step="1"
          controls-position="right"
          class="!w-full"
          @change="conflictMessage = ''"
        />
      </el-form-item>

      <!-- 挡车工 -->
      <el-form-item label="挡车工">
        <el-select
          v-model="form.workerUserId"
          placeholder="请选择挡车工（可空）"
          clearable
          filterable
          class="w-full"
          @change="handleWorkerChange"
        >
          <el-option
            v-for="worker in workerList"
            :key="worker.id"
            :label="worker.nickname || worker.username"
            :value="worker.id"
          />
        </el-select>
      </el-form-item>

      <!-- 任务编号（可选） -->
      <el-form-item label="任务编号">
        <el-input
          v-model="form.code"
          :disabled="!!form.id"
          placeholder="留空由系统自动派生（例如 MT-WO...-01）"
        />
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="填写排产备注信息" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          v-hasPermi="[
            'lunaerp:production:weaving-order-task:create',
            'lunaerp:production:weaving-order-task:update'
          ]"
          type="primary"
          :loading="submitting"
          @click="handleConfirmDispatch"
        >
          {{ form.id ? '保存修改' : '确认派工' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { LoomVO } from '@/api/lunaerp-elmui/production/loom'
import type { UserVO } from '@/api/system/user'
import type { WeavingOrderVO } from '@/api/lunaerp-elmui/production/weaving-order'
import { getWeavingOrderPage } from '@/api/lunaerp-elmui/production/weaving-order'
import { createMachineTask, updateMachineTask } from '@/api/lunaerp-elmui/production/machine-task'
import { validateDispatchForm, submitDispatch, type DispatchFormData } from '../model/pending-pool'

const props = withDefaults(
  defineProps<{
    loomList?: LoomVO[]
    workerList?: UserVO[]
  }>(),
  {
    loomList: () => [],
    workerList: () => []
  }
)

const emit = defineEmits<{
  (e: 'success'): void
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const searchingOrders = ref(false)
const lockedWeavingOrder = ref(false)
const conflictMessage = ref('')
const dateRange = ref<string[]>([])
const orderOptions = ref<WeavingOrderVO[]>([])

const form = ref<DispatchFormData>({
  plannedQty: 1
})

/**
 * 打开弹窗并预填数据
 */
function open(initialData?: Partial<DispatchFormData>, isLockedOrder: boolean = false) {
  conflictMessage.value = ''
  lockedWeavingOrder.value = isLockedOrder
  form.value = {
    plannedQty: 1,
    ...initialData
  }

  if (initialData?.plannedStart || initialData?.plannedEnd) {
    const s = initialData.plannedStart || initialData.plannedEnd!
    const e = initialData.plannedEnd || initialData.plannedStart!
    dateRange.value = [s, e]
  } else {
    dateRange.value = []
  }

  if (form.value.weavingOrderId && form.value.weavingOrderCode) {
    orderOptions.value = [
      {
        id: form.value.weavingOrderId,
        code: form.value.weavingOrderCode
      }
    ]
  } else {
    searchWeavingOrders('')
  }

  dialogVisible.value = true
}

defineExpose({
  open
})

/**
 * 远程搜索未完工织造单（ACTIVE/DRAFT）
 */
async function searchWeavingOrders(query: string) {
  searchingOrders.value = true
  try {
    const res = await getWeavingOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || undefined
    })
    orderOptions.value = (res.list || []).filter(
      (o: WeavingOrderVO) => o.status !== 'COMPLETED' && o.status !== 'CANCELLED'
    )
  } catch (e) {
    orderOptions.value = []
  } finally {
    searchingOrders.value = false
  }
}

function handleWeavingOrderChange(val: number) {
  conflictMessage.value = ''
  const selected = orderOptions.value.find((o) => o.id === val)
  if (selected) {
    form.value.weavingOrderCode = selected.code
  }
}

function handleLoomChange(val: number) {
  conflictMessage.value = ''
  const loom = props.loomList.find((l) => l.id === val)
  if (loom) {
    form.value.loomNo = loom.loomNo
  }
}

function handleDateRangeChange(val: any) {
  conflictMessage.value = ''
  if (val && val.length === 2) {
    form.value.plannedStart = val[0]
    form.value.plannedEnd = val[1]
  } else {
    form.value.plannedStart = undefined
    form.value.plannedEnd = undefined
  }
}

function handleWorkerChange(val: number) {
  const worker = props.workerList.find((w) => w.id === val)
  form.value.workerName = worker ? worker.nickname || worker.username : undefined
}

function handleDialogClosed() {
  conflictMessage.value = ''
  dateRange.value = []
  lockedWeavingOrder.value = false
}

/**
 * 提交派工
 */
async function handleConfirmDispatch() {
  const validation = validateDispatchForm(form.value)
  if (!validation.valid) {
    ElMessage.warning(validation.errorMsg || '请完善表单信息')
    return
  }

  submitting.value = true
  conflictMessage.value = ''

  try {
    // 若带 taskId 则走 updateMachineTask，否则走 createMachineTask
    const apiFn = form.value.id ? updateMachineTask : createMachineTask
    const result = await submitDispatch(form.value, apiFn)

    if (result.success) {
      if (result.warning) {
        ElMessage.warning(result.warning)
      }
      ElMessage.success(form.value.id ? '修改排期成功' : '派工成功')
      dialogVisible.value = false
      emit('success')
    } else {
      if (result.isConflict) {
        // 关键需求：硬校验拦截保持弹窗打开，红字展示冲突任务号与时段
        conflictMessage.value = result.conflictMessage || '织机排产时段冲突，请改期或换机'
      } else {
        ElMessage.error(result.error || '保存失败')
      }
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>
