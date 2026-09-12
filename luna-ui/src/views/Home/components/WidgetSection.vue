<template>
  <section :class="prefixCls">
    <header :class="`${prefixCls}__head`">
      <div :class="`${prefixCls}__eyebrow`">总览 · {{ WIDGET_COUNT }} 个小组件</div>
      <h2 :class="`${prefixCls}__title`">我的小组件</h2>
      <p :class="`${prefixCls}__desc`">按你的习惯固定呈现，每个人的排版互不影响。</p>
    </header>

    <el-row :gutter="18">
      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <CalendarWidget />
      </el-col>
      <el-col :xs="24" :sm="24" :md="24" :lg="12">
        <WorkbenchWidget />
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <TasksSummaryWidget />
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <AiReminderWidget />
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <ExchangeRateWidget />
      </el-col>
      <el-col :xs="24" :sm="12" :md="12" :lg="6">
        <LiquidClockWidget />
      </el-col>
      <el-col :xs="24" :sm="24" :md="24" :lg="24">
        <WorldClockWidget />
      </el-col>
    </el-row>
  </section>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import CalendarWidget from './widgets/CalendarWidget.vue'
import WorkbenchWidget from './widgets/WorkbenchWidget.vue'
import TasksSummaryWidget from './widgets/TasksSummaryWidget.vue'
import AiReminderWidget from './widgets/AiReminderWidget.vue'
import ExchangeRateWidget from './widgets/ExchangeRateWidget.vue'
import LiquidClockWidget from './widgets/LiquidClockWidget.vue'
import WorldClockWidget from './widgets/WorldClockWidget.vue'

defineOptions({ name: 'WidgetSection' })

const WIDGET_COUNT = 7

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-widgets')
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-widgets;

.#{$prefix-cls} {
  margin-top: 24px;

  &__head {
    margin-bottom: 20px;
  }

  &__eyebrow {
    font-size: 12.5px;
    font-weight: 600;
    color: #86868b;
  }

  &__title {
    margin: 6px 0;
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #1d1d1f;
  }

  &__desc {
    font-size: 13.5px;
    color: #6e6e73;
  }

  :deep(.el-col) {
    margin-bottom: 18px;
  }
}

@media (width >= 768px) {
  .#{$prefix-cls} {
    &__title {
      font-size: 32px;
    }
  }
}
</style>
