<template>
  <div class="stats-tab" v-loading="loading">
    <div class="flex justify-between items-center mb-16px">
      <div class="flex items-center gap-2">
        <span class="font-bold text-16px text-gray-800">织造执行进度与统计概览</span>
        <span class="text-gray-400 text-12px">（数据实时源自布票流转与机台任务汇报）</span>
      </div>
      <el-button type="primary" plain size="small" @click="loadStats">
        <Icon icon="ep:refresh" class="mr-1" /> 刷新统计
      </el-button>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="mb-20px">
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">计划匹数</div>
          <div class="stat-value text-blue-600">{{ statsData?.plannedQty ?? 0 }} <span class="text-13px text-gray-500">匹</span></div>
          <div class="stat-sub text-gray-400 mt-1">计划重: {{ statsData?.plannedWeight != null ? statsData.plannedWeight + ' KG' : '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">完工匹数</div>
          <div class="stat-value text-green-600">{{ statsData?.completedTickets ?? 0 }} <span class="text-13px text-gray-500">匹</span></div>
          <div class="stat-sub text-gray-500 mt-1">含检验与入库</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">有效布票数</div>
          <div class="stat-value text-indigo-600">{{ statsData?.validTickets ?? 0 }} <span class="text-13px text-gray-500">张</span></div>
          <div class="stat-sub text-gray-400 mt-1">总票数: {{ statsData?.totalTickets ?? 0 }} 张</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">作废布票数</div>
          <div class="stat-value text-red-500">{{ statsData?.voidTickets ?? 0 }} <span class="text-13px text-gray-500">张</span></div>
          <div class="stat-sub text-gray-400 mt-1">作废占号不重用</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">完工进度</div>
          <div class="stat-value text-teal-600">{{ statsData?.completionRate ?? 0 }}%</div>
          <div class="mt-2">
            <el-progress
              :percentage="Math.min(100, Math.max(0, statsData?.completionRate ?? 0))"
              :color="customColorMethod"
              :stroke-width="8"
              :show-text="false"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 布票各状态计数分段卡片 -->
    <el-card shadow="never" class="mb-20px !border-gray-200">
      <template #header>
        <div class="font-bold text-14px">布票各状态数量分布</div>
      </template>
      <div class="grid grid-cols-7 gap-3 text-center">
        <div class="status-box p-3 bg-gray-50 rounded border border-gray-100">
          <div class="text-gray-500 text-12px mb-1">已生成 (待开工)</div>
          <div class="font-bold text-18px text-gray-700">{{ statsData?.statusCounts?.['GENERATED'] ?? 0 }}</div>
        </div>
        <div class="status-box p-3 bg-blue-50 rounded border border-blue-100">
          <div class="text-blue-600 text-12px mb-1">已开工 (在产)</div>
          <div class="font-bold text-18px text-blue-700">{{ statsData?.statusCounts?.['STARTED'] ?? 0 }}</div>
        </div>
        <div class="status-box p-3 bg-green-50 rounded border border-green-100">
          <div class="text-green-600 text-12px mb-1">已完工</div>
          <div class="font-bold text-18px text-green-700">{{ statsData?.statusCounts?.['COMPLETED'] ?? 0 }}</div>
        </div>
        <div class="status-box p-3 bg-yellow-50 rounded border border-yellow-100">
          <div class="text-yellow-700 text-12px mb-1">已检验</div>
          <div class="font-bold text-18px text-yellow-800">{{ statsData?.statusCounts?.['INSPECTED'] ?? 0 }}</div>
        </div>
        <div class="status-box p-3 bg-emerald-50 rounded border border-emerald-100">
          <div class="text-emerald-700 text-12px mb-1">已入库</div>
          <div class="font-bold text-18px text-emerald-800">{{ statsData?.statusCounts?.['STOCKED'] ?? 0 }}</div>
        </div>
        <div class="status-box p-3 bg-orange-50 rounded border border-orange-100">
          <div class="text-orange-600 text-12px mb-1">异常中</div>
          <div class="font-bold text-18px text-orange-700">{{ statsData?.statusCounts?.['EXCEPTION'] ?? 0 }}</div>
        </div>
        <div class="status-box p-3 bg-red-50 rounded border border-red-100">
          <div class="text-red-500 text-12px mb-1">已作废</div>
          <div class="font-bold text-18px text-red-600">{{ statsData?.statusCounts?.['VOID'] ?? 0 }}</div>
        </div>
      </div>
    </el-card>

    <!-- 各机台任务排产与实织进度表 -->
    <el-card shadow="never" class="!border-gray-200">
      <template #header>
        <div class="font-bold text-14px">机台任务实织汇报与进度</div>
      </template>
      <el-table :data="statsData?.taskStats || []" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="taskCode" label="任务编号" min-width="160">
          <template #default="{ row }">
            <span class="font-mono font-bold">{{ row.taskCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="loomNo" label="织机号" width="110" />
        <el-table-column prop="workerName" label="挡车工" width="110">
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
        <el-table-column label="任务进度" min-width="160">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-progress
                :percentage="Math.min(100, Math.max(0, row.taskCompletionRate ?? 0))"
                :stroke-width="6"
                class="flex-1"
              />
              <span class="text-12px text-gray-500">{{ row.taskCompletionRate ?? 0 }}%</span>
            </div>
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
        <template #empty>
          <el-empty description="暂无排产机台任务" :image-size="70" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import {
  WeavingOrderStatsVO,
  getWeavingOrderStats
} from '@/api/lunaerp-elmui/production/weaving-order'

const props = defineProps<{
  weavingOrderId?: number
}>()

const loading = ref(false)
const statsData = ref<WeavingOrderStatsVO>()

const customColorMethod = (percentage: number) => {
  if (percentage < 30) return '#e6a23c'
  if (percentage < 70) return '#409eff'
  return '#67c23a'
}

const loadStats = async () => {
  if (!props.weavingOrderId) return
  loading.value = true
  try {
    const res = await getWeavingOrderStats(props.weavingOrderId)
    statsData.value = res
  } catch (e: any) {
    ElMessage.error(e?.message || '获取织造单统计数据失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.weavingOrderId,
  (id) => {
    if (id) loadStats()
  },
  { immediate: true }
)

defineExpose({
  loadStats
})
</script>

<style scoped>
.stats-tab {
  padding: 16px 0;
}
.stat-card {
  text-align: center;
  border-radius: 6px;
}
.stat-title {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-sub {
  font-size: 12px;
}
</style>
