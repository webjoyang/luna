<template>
  <div
    class="pending-pool-section rounded-md border border-[var(--el-border-color-lighter)] bg-[var(--el-bg-color)] p-3 mb-4 shadow-sm"
  >
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="flex items-center gap-1.5 font-bold text-gray-800 text-sm">
          <Icon icon="ep:folder-checked" class="text-primary text-base" />
          <span>待排池</span>
        </div>
        <el-badge :value="totalPendingCount" :max="99" class="pending-total-badge" type="warning" />
        <span class="text-xs text-gray-400">
          包含未排满织造单与未设置起止日期的任务，可直接点击行内"派工"安排机台与时段
        </span>
      </div>

      <div class="flex items-center gap-2">
        <el-button link type="primary" size="small" @click="emit('refresh')" :loading="loading">
          <Icon icon="ep:refresh" class="mr-1" /> 刷新待排池
        </el-button>
        <el-button link type="info" size="small" @click="isCollapsed = !isCollapsed">
          <Icon :icon="isCollapsed ? 'ep:arrow-down' : 'ep:arrow-up'" class="mr-1" />
          {{ isCollapsed ? '展开' : '收起' }}
        </el-button>
      </div>
    </div>

    <!-- 展开后的表格面板 -->
    <div
      v-show="!isCollapsed"
      class="mt-3 pt-3 border-t border-[var(--el-border-color-extra-light)]"
    >
      <el-tabs v-model="activeTab" class="pending-pool-tabs" type="card">
        <!-- 分组一：未排满织造单 -->
        <el-tab-pane name="orders">
          <template #label>
            <span class="flex items-center gap-1">
              未排满织造单
              <el-tag size="small" type="warning" effect="plain" round class="ml-1">
                {{ unScheduledOrders.length }}
              </el-tag>
            </span>
          </template>

          <el-table
            :data="unScheduledOrders"
            size="small"
            border
            stripe
            max-height="240"
            v-loading="loading"
          >
            <el-table-column prop="weavingOrderCode" label="织造单号" min-width="150">
              <template #default="{ row }">
                <span class="font-mono font-bold text-gray-800">{{ row.weavingOrderCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="产品" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span
                  >{{ row.productCode ? `[${row.productCode}] ` : ''
                  }}{{ row.productName || '-' }}</span
                >
              </template>
            </el-table-column>
            <el-table-column prop="plannedQty" label="计划匹数" width="90" align="right" />
            <el-table-column prop="scheduledQty" label="已排匹数" width="90" align="right">
              <template #default="{ row }">
                <span class="text-blue-600 font-semibold">{{ row.scheduledQty ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remainingQty" label="待排匹数" width="100" align="right">
              <template #default="{ row }">
                <span class="text-orange-600 font-bold">{{ row.remainingQty }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="emit('dispatch-order', row)"
                  v-hasPermi="['lunaerp:production:weaving-order-task:create']"
                >
                  <Icon icon="ep:plus" class="mr-0.5" /> 派工
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无待排满织造单" :image-size="60" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- 分组二：未排期任务 -->
        <el-tab-pane name="tasks">
          <template #label>
            <span class="flex items-center gap-1">
              未排期任务
              <el-tag size="small" type="info" effect="plain" round class="ml-1">
                {{ unDatedTasks.length }}
              </el-tag>
            </span>
          </template>

          <el-table
            :data="unDatedTasks"
            size="small"
            border
            stripe
            max-height="240"
            v-loading="loading"
          >
            <el-table-column prop="taskCode" label="任务编号" min-width="160">
              <template #default="{ row }">
                <span class="font-mono font-bold text-gray-800">{{ row.taskCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="weavingOrderCode" label="关联织造单" min-width="150" />
            <el-table-column prop="loomNo" label="预定织机" min-width="110">
              <template #default="{ row }">
                {{ row.loomNo || '未指定' }}
              </template>
            </el-table-column>
            <el-table-column prop="plannedQty" label="计划匹数" width="90" align="right" />
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click="emit('dispatch-task', row)"
                  v-hasPermi="['lunaerp:production:weaving-order-task:create']"
                >
                  <Icon icon="ep:calendar" class="mr-0.5" /> 排期
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无未排期任务" :image-size="60" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { PendingOrderVO, PendingTaskVO } from '@/api/lunaerp-elmui/production/scheduling'

const props = withDefaults(
  defineProps<{
    unScheduledOrders?: PendingOrderVO[]
    unDatedTasks?: PendingTaskVO[]
    loading?: boolean
  }>(),
  {
    unScheduledOrders: () => [],
    unDatedTasks: () => [],
    loading: false
  }
)

const emit = defineEmits<{
  (e: 'dispatch-order', order: PendingOrderVO): void
  (e: 'dispatch-task', task: PendingTaskVO): void
  (e: 'refresh'): void
}>()

const isCollapsed = ref(false)
const activeTab = ref<'orders' | 'tasks'>('orders')

const totalPendingCount = computed(() => props.unScheduledOrders.length + props.unDatedTasks.length)
</script>

<style scoped lang="scss">
.pending-pool-section {
  transition: all 0.2s ease;
}

.pending-pool-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
</style>
