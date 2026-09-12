<template>
  <WidgetCard title="我的工作台" :subtitle="subtitle">
    <template #extra>
      <el-input
        v-model="keyword"
        :class="`${prefixCls}__search`"
        size="small"
        placeholder="搜索任务或负责人"
        clearable
      />
    </template>

    <div :class="`${prefixCls}__metrics`">
      <div :class="`${prefixCls}__metric`">
        <span :class="`${prefixCls}__metric-label`">今天要做</span>
        <span :class="`${prefixCls}__metric-num`">{{ summary.todayCount }}</span>
      </div>
      <div :class="`${prefixCls}__metric`">
        <span :class="`${prefixCls}__metric-label`">本周要做</span>
        <span :class="`${prefixCls}__metric-num`">{{ summary.weekCount }}</span>
      </div>
      <div :class="[`${prefixCls}__metric`, 'is-danger']">
        <span :class="`${prefixCls}__metric-label`">逾期与受阻</span>
        <span :class="`${prefixCls}__metric-num`">{{ stuckCount }}</span>
      </div>
    </div>

    <div :class="`${prefixCls}__progress`">
      <el-progress :percentage="percent" :stroke-width="8" :show-text="false" color="#0f172a" />
      <div :class="`${prefixCls}__progress-text`">
        {{ summary.doneSteps }} / {{ summary.totalSteps }} 项已完成 · 还剩
        {{ summary.openSteps }} 步
      </div>
    </div>

    <div :class="`${prefixCls}__brief`">
      <Icon icon="ep:lightning" :size="14" />
      <span>{{ lunaBrief }}</span>
    </div>

    <div :class="`${prefixCls}__register`">
      <el-input
        v-model="registerText"
        size="small"
        placeholder="登记今天做完的事，AI 会归到对应任务"
      />
      <el-button
        type="primary"
        size="small"
        :disabled="!registerText.trim()"
        @click="handleRegister"
      >
        登记
      </el-button>
    </div>

    <div :class="`${prefixCls}__toolbar`">
      <el-segmented
        size="small"
        :model-value="segment"
        :options="segmentOptions"
        @change="handleSegmentChange"
      />
      <span :class="`${prefixCls}__toolbar-count`">{{ visibleTasks }} 项</span>
    </div>

    <div :class="`${prefixCls}__groups`">
      <div v-for="group in visibleGroups" :key="group.key" :class="`${prefixCls}__group`">
        <div :class="`${prefixCls}__group-head`">
          <span :class="`${prefixCls}__group-label`">{{ group.label }}</span>
          <span :class="`${prefixCls}__group-count`">{{ group.tasks.length }}</span>
        </div>

        <ul :class="`${prefixCls}__list`">
          <li
            v-for="task in group.tasks"
            :key="task.id"
            :class="[`${prefixCls}__task`, `is-${task.status}`]"
          >
            <Icon
              :class="`${prefixCls}__task-icon`"
              :icon="taskStatusMeta[task.status].icon"
              :size="14"
            />
            <span :class="`${prefixCls}__task-main`">
              <span :class="`${prefixCls}__task-title`">{{ task.title }}</span>
              <span :class="`${prefixCls}__task-sub`"
                >{{ task.ownerName }} · {{ task.nextPlan }}</span
              >
            </span>
            <span :class="`${prefixCls}__task-steps`">{{ task.stepsDone }}/{{ task.steps }}</span>
          </li>
        </ul>
      </div>

      <el-empty v-if="!visibleGroups.length" description="没有匹配的任务" :image-size="60" />
    </div>
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import { Icon } from '@/components/Icon'
import WidgetCard from './WidgetCard.vue'
import { taskStatusMeta } from '../../data/mockPanels'
import {
  buildGroups,
  lunaBrief,
  workbenchSummary as summary,
  workbenchTasks
} from '../../data/mockWorkbench'

defineOptions({ name: 'WorkbenchWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-board')
const { success } = useMessage()

type Segment = 'SELF' | 'ASSIGN'

const segment = ref<Segment>('SELF')
const keyword = ref('')
const registerText = ref('')

const segmentOptions = [
  { label: '我要做的', value: 'SELF' },
  { label: '我派出去的', value: 'ASSIGN' }
]

const handleSegmentChange = (value: string | number | boolean) => {
  segment.value = value as Segment
}

const stuckCount = computed(
  () =>
    workbenchTasks.filter((task) => task.status === 'overdue' || task.status === 'blocked').length
)

const percent = computed(() =>
  summary.totalSteps ? Math.round((summary.doneSteps / summary.totalSteps) * 100) : 0
)

const subtitle = computed(
  () => `${summary.unFinished} 项未完成 · 今天 ${summary.todayCount} · 逾期 ${stuckCount.value}`
)

const keywordLower = computed(() => keyword.value.trim().toLowerCase())

const visibleGroups = computed(() =>
  buildGroups(segment.value)
    .map((group) => ({
      ...group,
      tasks: keywordLower.value
        ? group.tasks.filter(
            (task) =>
              task.title.toLowerCase().includes(keywordLower.value) ||
              task.ownerName.toLowerCase().includes(keywordLower.value)
          )
        : group.tasks
    }))
    .filter((group) => group.tasks.length > 0)
)

const visibleTasks = computed(() =>
  visibleGroups.value.reduce((acc, group) => acc + group.tasks.length, 0)
)

const handleRegister = () => {
  success(`已登记：${registerText.value.trim()}，今晚会汇总到对应任务`)
  registerText.value = ''
}
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-board;

.#{$prefix-cls} {
  &__search {
    width: 168px;
  }

  &__metrics {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  &__metric {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px 14px;
    background: rgb(0 0 0 / 3%);
    border-radius: 14px;

    &.is-danger .#{$prefix-cls}__metric-num {
      color: #dc2626;
    }
  }

  &__metric-label {
    font-size: 11.5px;
    font-weight: 600;
    color: #86868b;
  }

  &__metric-num {
    font-size: 24px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.03em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;
  }

  &__progress {
    margin-top: 16px;
  }

  &__progress-text {
    margin-top: 8px;
    font-size: 12px;
    color: #86868b;
  }

  &__brief {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    padding: 10px 12px;
    margin-top: 14px;
    font-size: 12.5px;
    line-height: 1.6;
    color: #4f46e5;
    background: linear-gradient(120deg, rgb(99 102 241 / 9%), rgb(99 102 241 / 2%));
    border: 1px solid rgb(99 102 241 / 14%);
    border-radius: 14px;
  }

  &__register {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }

  &__toolbar {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-top: 16px;
  }

  &__toolbar-count {
    font-size: 12px;
    color: #86868b;
  }

  :deep(.el-segmented) {
    --el-segmented-bg-color: rgb(15 23 42 / 6%);
    --el-segmented-color: #6e6e73;
    --el-segmented-item-selected-color: #fff;
    --el-segmented-item-selected-bg-color: #0f172a;

    padding: 2px;
    border-radius: 999px;
  }

  :deep(.el-segmented__item) {
    padding: 4px 12px;
    font-size: 12px;
    font-weight: 600;
    border-radius: 999px;
  }

  :deep(.el-segmented__item-label) {
    padding: 0;
  }

  &__groups {
    max-height: 300px;
    padding-right: 4px;
    margin-top: 12px;
    overflow-y: auto;
  }

  &__group + &__group {
    margin-top: 12px;
  }

  &__group-head {
    display: flex;
    gap: 8px;
    align-items: center;
    padding: 0 2px 6px;
  }

  &__group-label {
    font-size: 12px;
    font-weight: 700;
    color: #6e6e73;
  }

  &__group-count {
    min-width: 18px;
    padding: 0 5px;
    font-size: 10.5px;
    font-weight: 700;
    line-height: 18px;
    color: #6e6e73;
    text-align: center;
    background: rgb(0 0 0 / 6%);
    border-radius: 9px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 3px;
    padding: 0;
    margin: 0;
    list-style: none;
  }

  &__task {
    display: flex;
    gap: 9px;
    align-items: center;
    padding: 8px 10px;
    border-radius: 12px;
    transition: background 0.3s ease;

    &:hover {
      background: rgb(0 0 0 / 3%);
    }
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
    font-size: 12.5px;
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

  &__task-steps {
    flex: none;
    font-size: 11.5px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: #aeaeb2;
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
    }

    .#{$prefix-cls}__task-title {
      color: #b91c1c;
    }

    .#{$prefix-cls}__task-steps {
      color: #dc2626;
    }
  }
}

/* 窄屏：搜索框独占一行，登记区允许折行 */
@media (width <= 767px) {
  .#{$prefix-cls} {
    &__search {
      width: 100%;
    }

    &__register {
      flex-wrap: wrap;
    }
  }
}
</style>
