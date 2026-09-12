<template>
  <WidgetCard eyebrow="Calendar & Agenda" title="日历与待办事项">
    <template #extra>
      <button type="button" :class="`${prefixCls}__nav`" aria-label="往前一周" @click="shift(-7)">
        <Icon icon="ep:arrow-left" :size="14" />
      </button>
      <button type="button" :class="`${prefixCls}__nav`" aria-label="往后一周" @click="shift(7)">
        <Icon icon="ep:arrow-right" :size="14" />
      </button>
    </template>

    <el-scrollbar :class="`${prefixCls}__strip`">
      <div :class="`${prefixCls}__days`">
        <button
          v-for="day in days"
          :key="day.key"
          type="button"
          :class="[
            `${prefixCls}__day`,
            { 'is-selected': day.key === selectedKey, 'is-today': day.isToday }
          ]"
          :aria-label="`${day.month} 月 ${day.day} 日 星期${day.weekday}`"
          @click="selectedKey = day.key"
        >
          <span :class="`${prefixCls}__day-week`">{{
            day.isToday ? '今天' : `周${day.weekday}`
          }}</span>
          <span :class="`${prefixCls}__day-num`">{{ day.day }}</span>
          <span :class="`${prefixCls}__day-lunar`">{{ day.festival || day.lunar }}</span>
          <span :class="`${prefixCls}__day-dots`">
            <i
              v-for="(event, index) in day.events"
              :key="index"
              :class="[`${prefixCls}__dot`, `is-${event.type}`]"
              :title="event.label"
            ></i>
          </span>
          <span v-if="day.taskCount" :class="`${prefixCls}__day-count`">{{ day.taskCount }}</span>
        </button>
      </div>
    </el-scrollbar>

    <div :class="`${prefixCls}__legend`">
      <span
        v-for="item in calendarEventLegend"
        :key="item.type"
        :class="`${prefixCls}__legend-item`"
      >
        <i :class="[`${prefixCls}__dot`, `is-${item.type}`]"></i>{{ item.label }}
      </span>
    </div>

    <div :class="`${prefixCls}__agenda-head`">
      <span :class="`${prefixCls}__agenda-title`">{{ agendaTitle }}</span>
      <span :class="`${prefixCls}__agenda-count`">{{ selectedTasks.length }} 项</span>
    </div>

    <ul v-if="selectedTasks.length" :class="`${prefixCls}__tasks`">
      <li
        v-for="task in selectedTasks"
        :key="task.id"
        :class="[`${prefixCls}__task`, `is-${task.status}`]"
      >
        <Icon
          :class="`${prefixCls}__task-icon`"
          :icon="taskStatusMeta[task.status].icon"
          :size="15"
        />
        <span :class="`${prefixCls}__task-main`">
          <span :class="`${prefixCls}__task-title`">{{ task.title }}</span>
          <span :class="`${prefixCls}__task-sub`">{{ task.ownerName }} · {{ task.nextPlan }}</span>
        </span>
        <el-tag :type="tagType(task.status)" size="small" effect="light" round>
          {{ taskStatusMeta[task.status].label }}
        </el-tag>
      </li>
    </ul>

    <el-empty v-else description="该日期暂无协作任务" :image-size="68" />

    <div :class="`${prefixCls}__suggest`">
      <div :class="`${prefixCls}__suggest-title`">
        <Icon icon="ep:magic-stick" :size="15" />
        AI 智能决策建议
      </div>
      <ul :class="`${prefixCls}__suggest-list`">
        <li v-for="(tip, index) in suggestions" :key="index">{{ tip }}</li>
      </ul>
    </div>
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import { Icon } from '@/components/Icon'
import WidgetCard from './WidgetCard.vue'
import {
  aiSuggestions,
  buildCalendarDays,
  calendarEventLegend,
  taskStatusMeta
} from '../../data/mockPanels'
import { workbenchTasks } from '../../data/mockWorkbench'
import type { TaskStatus } from '../../data/types'

defineOptions({ name: 'CalendarWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-calendar')

const days = buildCalendarDays(new Date())

const selectedKey = ref(days.find((day) => day.isToday)?.key ?? days[0].key)

const selectedDay = computed(() => days.find((day) => day.key === selectedKey.value) ?? days[0])

const selectedTasks = computed(() =>
  workbenchTasks.filter((task) => task.dueOffset === selectedDay.value.offset)
)

const agendaTitle = computed(
  () => `${selectedDay.value.month} 月 ${selectedDay.value.day} 日 · 周${selectedDay.value.weekday}`
)

const suggestions = computed(() => aiSuggestions(selectedTasks.value.length))

const TAG_TYPES: Record<TaskStatus, 'success' | 'danger' | 'warning' | 'primary'> = {
  completed: 'success',
  overdue: 'danger',
  blocked: 'warning',
  in_progress: 'primary'
}

const tagType = (status: TaskStatus) => TAG_TYPES[status]

const shift = (delta: number) => {
  const index = days.findIndex((day) => day.key === selectedKey.value)
  const next = Math.min(Math.max(index + delta, 0), days.length - 1)
  selectedKey.value = days[next].key
}
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-calendar;

.#{$prefix-cls} {
  &__nav {
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    color: #6e6e73;
    cursor: pointer;
    background: rgb(0 0 0 / 4%);
    border: none;
    border-radius: 9px;
    transition: all 0.3s ease;

    &:hover {
      color: #1d1d1f;
      background: rgb(0 0 0 / 8%);
    }
  }

  &__strip {
    margin: 0 -6px;
  }

  :deep(.el-scrollbar__view) {
    display: flex;
  }

  &__days {
    display: flex;
    gap: 8px;
    padding: 6px;
  }

  &__day {
    position: relative;
    display: flex;
    flex: none;
    flex-direction: column;
    gap: 3px;
    align-items: center;
    justify-content: center;
    width: 78px;
    height: 106px;
    padding: 8px 4px;
    cursor: pointer;
    background: rgb(255 255 255 / 62%);
    border: 1px solid rgb(255 255 255 / 80%);
    border-radius: 18px;
    transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);

    &:hover {
      background: rgb(255 255 255 / 88%);
      transform: translateY(-2px);
    }

    &.is-selected {
      color: #fff;
      background: #0f172a;
      border-color: #0f172a;
      box-shadow: 0 14px 28px -16px rgb(15 23 42 / 80%);
    }
  }

  &__day-week {
    font-size: 11.5px;
    font-weight: 600;
    color: #86868b;
  }

  &__day-num {
    font-size: 24px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.03em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;
  }

  &__day-lunar {
    font-size: 10.5px;
    color: #aeaeb2;
    white-space: nowrap;
  }

  &__day-dots {
    display: flex;
    gap: 3px;
    height: 6px;
    margin-top: 2px;
  }

  &__day-count {
    position: absolute;
    top: 6px;
    right: 6px;
    min-width: 17px;
    padding: 0 4px;
    font-size: 10.5px;
    font-weight: 700;
    line-height: 17px;
    color: #1d1d1f;
    text-align: center;
    background: rgb(0 0 0 / 7%);
    border-radius: 9px;
  }

  &__day.is-selected &__day-week,
  &__day.is-selected &__day-num {
    color: #fff;
  }

  &__day.is-selected &__day-lunar {
    color: rgb(255 255 255 / 62%);
  }

  &__day.is-selected &__day-count {
    color: #fff;
    background: rgb(255 255 255 / 22%);
  }

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;

    &.is-contract {
      background: #f43f5e;
    }

    &.is-payment {
      background: #f59e0b;
    }

    &.is-shipment {
      background: #3b82f6;
    }

    &.is-personal {
      background: #10b981;
    }
  }

  &__legend {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 14px;
    padding: 12px 2px 0;
    font-size: 11.5px;
    color: #86868b;
  }

  &__legend-item {
    display: inline-flex;
    gap: 5px;
    align-items: center;
  }

  &__agenda-head {
    display: flex;
    padding: 16px 0 8px;
    margin-top: 14px;
    border-top: 1px solid rgb(0 0 0 / 6%);
    align-items: baseline;
    justify-content: space-between;
  }

  &__agenda-title {
    font-size: 13.5px;
    font-weight: 700;
    color: #1d1d1f;
  }

  &__agenda-count {
    font-size: 12px;
    color: #86868b;
  }

  &__tasks {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-height: 268px;
    padding: 0;
    margin: 0;
    overflow-y: auto;
    list-style: none;
  }

  &__task {
    display: flex;
    padding: 9px 11px;
    background: rgb(0 0 0 / 2.5%);
    border-radius: 14px;
    gap: 10px;
    align-items: center;
  }

  &__task-icon {
    flex: none;
    color: #6366f1;
  }

  &__task-main {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__task-title {
    overflow: hidden;
    font-size: 13px;
    font-weight: 600;
    color: #1d1d1f;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__task-sub {
    overflow: hidden;
    font-size: 11.5px;
    color: #86868b;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__task.is-completed {
    .#{$prefix-cls}__task-icon {
      color: #059669;
    }

    .#{$prefix-cls}__task-title {
      color: #86868b;
      text-decoration: line-through;
    }
  }

  &__task.is-overdue,
  &__task.is-blocked {
    background: rgb(220 38 38 / 6%);

    .#{$prefix-cls}__task-icon {
      color: #dc2626;
      animation: wb-calendar-pulse 1.8s ease-in-out infinite;
    }

    .#{$prefix-cls}__task-title {
      color: #b91c1c;
    }
  }

  &__suggest {
    padding: 12px 14px;
    margin-top: 16px;
    background: linear-gradient(120deg, rgb(99 102 241 / 8%), rgb(99 102 241 / 2%));
    border: 1px solid rgb(99 102 241 / 14%);
    border-radius: 16px;
  }

  &__suggest-title {
    display: flex;
    gap: 6px;
    align-items: center;
    font-size: 12.5px;
    font-weight: 700;
    color: #4f46e5;
  }

  &__suggest-list {
    padding-left: 16px;
    margin: 8px 0 0;
    font-size: 12px;
    line-height: 1.75;
    color: #6e6e73;
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    &__task.is-overdue .#{$prefix-cls}__task-icon,
    &__task.is-blocked .#{$prefix-cls}__task-icon {
      animation: none;
    }

    &__nav,
    &__day {
      transition: none;
    }

    &__day:hover {
      transform: none;
    }
  }
}

@keyframes wb-calendar-pulse {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.4;
  }
}
</style>
