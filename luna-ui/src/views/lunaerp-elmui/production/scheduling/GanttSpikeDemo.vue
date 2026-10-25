<template>
  <div class="gantt-spike-demo">
    <div class="gantt-spike-toolbar">
      <el-tag type="primary">vue-ganttastic Spike 验证看板</el-tag>
      <el-button type="primary" plain size="small" @click="load500TasksBenchmark">
        加载 50 织机 × 500 任务压力测试数据 (当前: {{ totalTasksCount }} 条)
      </el-button>
      <span class="tip">
        平移/拉伸已验证可用；跨行改机定案为点击弹窗下拉转机；左边缘缩放已屏蔽
      </span>
    </div>

    <g-gantt-chart
      :chart-start="chartStart"
      :chart-end="chartEnd"
      precision="day"
      bar-start="startDate"
      bar-end="endDate"
      date-format="YYYY-MM-DD"
      :color-scheme="elementPlusGanttTheme"
      grid
      :row-height="40"
      label-column-title="织机台号"
      label-column-width="180px"
      @dragend-bar="onDragEndBar"
      @click-bar="onClickBar"
    >
      <g-gantt-row v-for="row in loomRows" :key="row.loomId" :label="row.label" :bars="row.bars" />
    </g-gantt-chart>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { GGanttChart, GGanttRow } from 'vue-ganttastic'
import type { GanttBarObject } from 'vue-ganttastic'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  elementPlusGanttTheme,
  getGanttBarClasses,
  isTaskDraggable,
  getTaskDragDisabledReason,
  type MachineTaskStatus
} from './gantt-theme'

const chartStart = ref('2026-10-01')
const chartEnd = ref('2026-10-31')

interface DemoBar extends GanttBarObject {
  taskId: number
  taskCode: string
  status: MachineTaskStatus
  startDate: string
  endDate: string
}

interface DemoRow {
  loomId: number
  label: string
  bars: DemoBar[]
}

const loomRows = ref<DemoRow[]>([
  {
    loomId: 1,
    label: 'L-01 (喷气 230)',
    bars: [
      {
        taskId: 101,
        taskCode: 'MT-01',
        status: 'IN_PROGRESS',
        startDate: '2026-10-05',
        endDate: '2026-10-10',
        ganttBarConfig: {
          id: 'task-101',
          label: 'WO01 / 30匹 [生产中]',
          hasHandles: true,
          immobile: false,
          class: getGanttBarClasses('IN_PROGRESS', false)
        }
      } as DemoBar
    ]
  },
  {
    loomId: 2,
    label: 'L-02 (剑杆 190)',
    bars: [
      {
        taskId: 102,
        taskCode: 'MT-02',
        status: 'PENDING',
        startDate: '2026-10-08',
        endDate: '2026-10-14',
        ganttBarConfig: {
          id: 'task-102',
          label: 'WO02 / 50匹 [待开始·冲突]',
          hasHandles: true,
          immobile: false,
          class: getGanttBarClasses('PENDING', true)
        }
      } as DemoBar
    ]
  },
  {
    loomId: 3,
    label: 'L-03 (喷气 280)',
    bars: [
      {
        taskId: 103,
        taskCode: 'MT-03',
        status: 'HALTED',
        startDate: '2026-10-12',
        endDate: '2026-10-18',
        ganttBarConfig: {
          id: 'task-103',
          label: 'WO03 / 20匹 [暂停异常]',
          hasHandles: false,
          immobile: true,
          class: getGanttBarClasses('HALTED', false, true)
        }
      } as DemoBar
    ]
  },
  {
    loomId: 4,
    label: 'L-04 (剑杆 230)',
    bars: [
      {
        taskId: 104,
        taskCode: 'MT-04',
        status: 'COMPLETED',
        startDate: '2026-10-02',
        endDate: '2026-10-06',
        ganttBarConfig: {
          id: 'task-104',
          label: 'WO04 / 40匹 [已完工]',
          hasHandles: false,
          immobile: true,
          class: getGanttBarClasses('COMPLETED', false, true)
        }
      } as DemoBar
    ]
  }
])

const totalTasksCount = computed(() =>
  loomRows.value.reduce((acc, row) => acc + row.bars.length, 0)
)

function onDragEndBar({
  bar,
  movedBars
}: {
  bar: GanttBarObject
  e: MouseEvent
  movedBars?: Map<GanttBarObject, { oldStart: string; oldEnd: string }>
}) {
  const target = bar as DemoBar
  ElMessage.success(
    `拖拽改期完成: 任务【${target.taskCode}】新时段 ${target.startDate} ~ ${target.endDate}`
  )
  console.log('拖拽结束 (平移或拉伸):', bar, movedBars)
}

function onClickBar({ bar }: { bar: GanttBarObject }) {
  const target = bar as DemoBar
  if (!isTaskDraggable(target.status)) {
    const reason = getTaskDragDisabledReason(target.status)
    if (reason) {
      ElMessage.warning(reason)
    }
    return
  }
  ElMessage.info(`点击条带【${target.taskCode}】：弹出转机弹窗选择目标织机（降级定案交互）`)
}

function load500TasksBenchmark() {
  const t0 = performance.now()
  const rows: DemoRow[] = []
  let taskId = 1000

  for (let i = 1; i <= 50; i++) {
    const bars: DemoBar[] = []
    for (let j = 0; j < 10; j++) {
      const startDay = 1 + ((j * 3) % 25)
      const duration = 2 + (j % 3)
      const start = dayjs('2026-10-01').add(startDay, 'day').format('YYYY-MM-DD')
      const end = dayjs(start).add(duration, 'day').format('YYYY-MM-DD')
      const statusList: MachineTaskStatus[] = ['PENDING', 'IN_PROGRESS', 'HALTED', 'COMPLETED']
      const status = statusList[j % 4]
      const isConflict = j === 1
      const isImmobile = !isTaskDraggable(status)

      bars.push({
        taskId,
        taskCode: `MT-${taskId}`,
        status,
        startDate: start,
        endDate: end,
        ganttBarConfig: {
          id: `task-${taskId}`,
          label: `T${taskId}`,
          hasHandles: !isImmobile,
          immobile: isImmobile,
          class: getGanttBarClasses(status, isConflict, isImmobile)
        }
      })
      taskId++
    }
    rows.push({
      loomId: i,
      label: `L-${String(i).padStart(2, '0')}`,
      bars
    })
  }

  loomRows.value = rows
  const t1 = performance.now()
  ElMessage.success(`50 织机 × 500 任务数据加载完成，耗时 ${(t1 - t0).toFixed(2)} ms`)
}
</script>

<style scoped lang="scss">
.gantt-spike-demo {
  padding: 16px;
  background-color: var(--el-bg-color);

  .gantt-spike-toolbar {
    display: flex;
    margin-bottom: 12px;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .tip {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
