<template>
  <el-dialog
    v-model="dialogVisible"
    title="工资扣款汇总（按确认日）"
    width="980px"
    top="6vh"
    :close-on-click-modal="false"
  >
    <!-- 口径说明显著标注 -->
    <el-alert
      title="口径说明：按【确认日】（责任认定时间）汇总当天已认定、操作失误（扣款）、未撤销的有效记录。本汇总纯只读，供人事核对并手工录入工资系统。"
      type="warning"
      show-icon
      :closable="false"
      class="!mb-4"
    />

    <!-- 工具栏：日期选择 + 查询 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <span class="text-sm font-medium text-gray-700">确认日期：</span>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          placeholder="选择确认日期"
          class="!w-160px"
        />
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-1" /> 查询
        </el-button>
      </div>
      <div class="text-sm text-gray-600">
        共 <span class="font-bold text-gray-900">{{ groups.length }}</span> 位挡车工， 累计扣款
        <span class="font-bold text-red-600">{{ totalCount }}</span> 卷
      </div>
    </div>

    <!-- 按挡车工分组表格（可展开明细，纯只读） -->
    <el-table
      :key="tableKey"
      v-loading="loading"
      :data="groups"
      border
      stripe
      size="small"
      max-height="480px"
      row-key="workerUserId"
      :default-expand-all="true"
    >

      <!-- 展开行：展示逐卷扣款明细 -->
      <el-table-column type="expand" width="50">
        <template #default="{ row }">
          <div class="p-3 bg-gray-50 rounded">
            <div class="mb-2 text-xs font-semibold text-gray-500 flex items-center gap-1">
              <Icon icon="ep:list" /> 【{{ row.workerName }}】扣款卷明细清单（{{ row.records.length }} 卷）
            </div>
            <el-table :data="row.records" border size="small" :header-cell-style="{ background: '#f8fafc' }">
              <el-table-column type="index" label="#" width="45" align="center" />
              <el-table-column label="卷号" prop="rollNo" min-width="110">
                <template #default="{ row: subRow }">
                  <span class="font-semibold">{{ subRow.rollNo }}</span>
                </template>
              </el-table-column>
              <el-table-column label="缸号" prop="vatNo" min-width="100">
                <template #default="{ row: subRow }">{{ subRow.vatNo || '—' }}</template>
              </el-table-column>
              <el-table-column label="等级" prop="grade" width="85" align="center">
                <template #default="{ row: subRow }">
                  <DictTag :type="DICT_TYPE.LUNAERP_INSPECTION_GRADE" :value="subRow.grade" />
                </template>
              </el-table-column>
              <el-table-column label="检验单号" prop="inspectionOrderCode" min-width="140">
                <template #default="{ row: subRow }">
                  <span class="font-mono text-xs">{{ subRow.inspectionOrderCode || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="来源" prop="source" width="100" align="center">
                <template #default="{ row: subRow }">
                  <el-tag size="small" :type="subRow.source === 'INSPECTION' ? 'primary' : 'info'">
                    {{ subRow.source === 'INSPECTION' ? '检验单' : '手工' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                label="认定时间"
                prop="confirmedAt"
                width="145"
                align="center"
                :formatter="dateFormatter"
              />
              <el-table-column label="原因备注" prop="note" min-width="130" show-overflow-tooltip>
                <template #default="{ row: subRow }">{{ subRow.note || '—' }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>

      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column label="挡车工姓名" prop="workerName" min-width="140" align="center">
        <template #default="{ row }">
          <span class="font-semibold text-gray-800 text-sm">{{ row.workerName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="挡车工用户ID" prop="workerUserId" width="130" align="center">
        <template #default="{ row }">
          <span class="font-mono text-xs text-gray-500">{{ row.workerUserId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="扣款卷数" prop="count" width="120" align="center">
        <template #default="{ row }">
          <el-tag type="danger" effect="plain" class="font-bold">
            {{ row.count }} 卷
          </el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="该确认日暂无操作失误扣款记录" :image-size="80" />
      </template>
    </el-table>

    <!-- 弹窗底部操作（纯只读，仅保留关闭按钮） -->
    <template #footer>
      <div class="flex justify-between items-center">
        <span class="text-xs text-gray-400">
          * 数据纯只读核对，未与工资系统自动关联，请参照明细进行手工录入。
        </span>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import type { PayrollWorkerGroupVO } from '@/api/lunaerp-elmui/production/inspection-confirmation'

defineOptions({ name: 'InspectionConfirmationPayrollSummaryDialog' })

const dialogVisible = ref(false)
const loading = ref(false)
const selectedDate = ref(formatDate(new Date(), 'YYYY-MM-DD'))
const groups = ref<PayrollWorkerGroupVO[]>([])
const tableKey = ref(0)

/** 累计扣款总卷数 */
const totalCount = computed(() => {
  return groups.value.reduce((acc, curr) => acc + (curr.count || 0), 0)
})

/** 加载汇总数据 */
const loadData = async () => {
  loading.value = true
  try {
    const data = await InspectionConfirmationApi.getPayrollSummary({ date: selectedDate.value })
    groups.value = data?.groups || []
    tableKey.value++
  } finally {
    loading.value = false
  }
}


const handleQuery = () => {
  loadData()
}

/** 打开弹窗 */
const open = (dateStr?: string) => {
  selectedDate.value = dateStr || formatDate(new Date(), 'YYYY-MM-DD')
  dialogVisible.value = true
  loadData()
}

defineExpose({ open })
</script>
