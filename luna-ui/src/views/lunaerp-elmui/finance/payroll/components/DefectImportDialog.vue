<template>
  <el-dialog
    v-model="visible"
    title="导入质检次品扣款"
    width="880px"
    destroy-on-close
    append-to-body
    :close-on-click-modal="false"
  >
    <!-- 顶部提示与单价设置栏 -->
    <el-alert type="info" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">次品扣款逐卷追溯与指纹防重</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          系统根据工资期间（{{ period }}）与责任员工自动汇总裁决次品卷数；<br />
          设置统一扣款单价后将按员工生成【DEFECT
          次品扣款】款项行并锁定每卷指纹，已被其他有效工资单占用的记录将自动排除。
        </div>
      </template>
    </el-alert>

    <div
      class="flex items-center gap-16px mb-14px bg-gray-50 p-12px rounded border border-gray-200"
    >
      <div class="flex items-center gap-8px">
        <span class="text-13px font-medium text-gray-700">工资期间：</span>
        <el-tag type="primary" size="default" class="font-mono font-bold">{{ period }}</el-tag>
      </div>

      <div class="flex items-center gap-8px ml-16px">
        <span class="text-13px font-medium text-gray-700">
          <span class="text-red-500 mr-2px">*</span>统一扣款单价(元/卷)：
        </span>
        <el-input-number
          v-model="deductUnitPrice"
          :min="0.01"
          :precision="2"
          :step="10"
          placeholder="请输入单价"
          class="!w-160px"
        />
      </div>

      <div class="text-12px text-gray-500 ml-auto">
        共发现
        <span class="font-bold text-red-600">{{ totalDefectRolls }}</span>
        卷待扣次品
      </div>
    </div>

    <!-- 员工次品聚合分组列表 -->
    <el-table
      v-loading="loading"
      :data="previewGroups"
      border
      style="width: 100%"
      max-height="420px"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column type="expand" width="48">
        <template #default="{ row }">
          <div class="p-12px bg-gray-50">
            <div class="text-12px font-bold text-gray-700 mb-8px">
              责任工人【{{ row.workerName }}】次品明细快照（共 {{ row.records?.length || 0 }} 卷）：
            </div>
            <el-table :data="row.records" size="small" border>
              <el-table-column label="确认单号" prop="confirmationNo" min-width="150" />
              <el-table-column label="布卷号" prop="rollNo" width="110" />
              <el-table-column label="缸号" prop="vatNo" width="90" />
              <el-table-column label="等级" prop="grade" width="70" align="center">
                <template #default="{ row: r }">
                  <el-tag size="small" type="danger">{{ r.grade }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="确认时间" prop="confirmedAt" min-width="150" />
              <el-table-column label="扣款单价" width="100" align="right">
                <template #default>
                  <span class="font-mono text-red-600">¥ {{ formatAmount(deductUnitPrice) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="员工姓名" prop="workerName" min-width="120" />
      <el-table-column label="员工ID" prop="workerUserId" width="100" align="center" />
      <el-table-column label="次品卷数" prop="count" width="110" align="center">
        <template #default="{ row }">
          <el-tag type="danger" effect="plain" class="font-bold">{{ row.count }} 卷</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="单价 (元/卷)" width="120" align="right">
        <template #default>
          <span class="font-mono">¥ {{ formatAmount(deductUnitPrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预计扣款小计 (元)" width="150" align="right">
        <template #default="{ row }">
          <span class="font-mono font-bold text-red-600">
            ¥ {{ formatAmount(calculateSubtotal(row.count)) }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="flex items-center justify-between">
        <div class="text-12px text-gray-500">
          已勾选 <span class="font-bold text-primary">{{ selectedWorkerIds.length }}</span> 名员工，
          共计扣款
          <span class="font-bold text-red-600 font-mono"
            >¥ {{ formatAmount(selectedTotalAmount) }}</span
          >
        </div>
        <div class="flex items-center gap-8px">
          <el-button @click="visible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="confirming"
            :disabled="selectedWorkerIds.length === 0 || !previewGroups.length"
            v-hasPermi="['lunaerp:finance:payroll:import-defect']"
            @click="handleConfirmImport"
          >
            确认导入
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getDefectPreview } from '@/api/lunaerp-elmui/finance/payroll'
import type { DefectWorkerGroup } from '@/api/lunaerp-elmui/finance/payroll'
import {
  formatAmount,
  transformDefectPreviewToItems,
  parsePayrollErrorMessage,
  type PayrollLineItemData
} from '../model/payroll'

defineOptions({ name: 'DefectImportDialog' })

const props = defineProps<{
  modelValue: boolean
  period: string
  payrollId?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  confirm: [importedMap: Map<number, PayrollLineItemData>, workerNameMap: Map<number, string>]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const confirming = ref(false)
const previewGroups = ref<DefectWorkerGroup[]>([])
const selectedRows = ref<DefectWorkerGroup[]>([])
const deductUnitPrice = ref<number>(50) // 默认 50 元/卷

const selectedWorkerIds = computed(() => selectedRows.value.map((r) => r.workerUserId))

const totalDefectRolls = computed(() => {
  return previewGroups.value.reduce((acc, cur) => acc + (cur.count || 0), 0)
})

const calculateSubtotal = (count: number) => {
  const p = Number(deductUnitPrice.value) || 0
  return Math.round(count * p * 100) / 100
}

const selectedTotalAmount = computed(() => {
  const p = Number(deductUnitPrice.value) || 0
  const rolls = selectedRows.value.reduce((acc, cur) => acc + (cur.count || 0), 0)
  return Math.round(rolls * p * 100) / 100
})

const handleSelectionChange = (rows: DefectWorkerGroup[]) => {
  selectedRows.value = rows
}

const loadPreviewData = async () => {
  if (!props.period) return
  loading.value = true
  try {
    const res = await getDefectPreview(props.period, props.payrollId)
    previewGroups.value = res.groups || []
    selectedRows.value = [...previewGroups.value]
    if (previewGroups.value.length === 0) {
      ElMessage.info(`期间【${props.period}】暂无待处理的质检次品记录`)
    }
  } catch (err: any) {
    ElMessage.error(parsePayrollErrorMessage(err))
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      loadPreviewData()
    } else {
      previewGroups.value = []
      selectedRows.value = []
    }
  }
)

const handleConfirmImport = () => {
  const unitPrice = Number(deductUnitPrice.value)
  if (isNaN(unitPrice) || unitPrice <= 0) {
    ElMessage.warning('次品扣款单价必填且必须大于0（025_010）')
    return
  }
  if (selectedWorkerIds.value.length === 0) {
    ElMessage.warning('请至少勾选一名员工')
    return
  }

  const itemsMap = transformDefectPreviewToItems(
    previewGroups.value,
    unitPrice,
    selectedWorkerIds.value
  )

  const workerNameMap = new Map<number, string>()
  for (const group of previewGroups.value) {
    if (selectedWorkerIds.value.includes(group.workerUserId)) {
      workerNameMap.set(group.workerUserId, group.workerName)
    }
  }

  emit('confirm', itemsMap, workerNameMap)
  ElMessage.success(`已成功导入 ${itemsMap.size} 名员工的次品扣款明细`)
  visible.value = false
}
</script>
