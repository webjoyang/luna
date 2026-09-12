<template>
  <WidgetCard title="今日任务">
    <div :class="`${prefixCls}__value`"> {{ stat.total }}<small>项</small> </div>

    <div :class="`${prefixCls}__sub`">
      已完成 <b>{{ stat.done }}</b> 项
    </div>

    <el-progress
      :class="`${prefixCls}__bar`"
      :percentage="percent"
      :stroke-width="8"
      :show-text="false"
      color="#059669"
    />
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import WidgetCard from './WidgetCard.vue'
import { todayTaskStat } from '../../data/mockWorkbench'

defineOptions({ name: 'TasksSummaryWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-today-tasks')

const stat = todayTaskStat

const percent = computed(() => (stat.total ? Math.round((stat.done / stat.total) * 100) : 0))
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-today-tasks;

.#{$prefix-cls} {
  &__value {
    font-size: 46px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.04em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;

    small {
      margin-left: 6px;
      font-size: 15px;
      font-weight: 600;
      color: #86868b;
    }
  }

  &__sub {
    margin-top: 10px;
    font-size: 12.5px;
    color: #6e6e73;

    b {
      font-weight: 700;
      color: #059669;
    }
  }

  &__bar {
    margin-top: 18px;
  }

  :deep(.el-progress-bar__outer) {
    background: rgb(0 0 0 / 6%);
  }
}
</style>
