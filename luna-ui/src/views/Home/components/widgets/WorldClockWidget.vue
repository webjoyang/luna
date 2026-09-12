<template>
  <WidgetCard title="世界时钟">
    <template #extra>
      <Icon icon="ep:compass" :size="18" color="#aeaeb2" />
    </template>

    <el-row :gutter="12">
      <el-col v-for="row in rows" :key="row.city" :xs="24" :sm="12" :md="8">
        <div :class="`${prefixCls}__item`">
          <span :class="`${prefixCls}__flag`">{{ row.flag }}</span>
          <span :class="`${prefixCls}__city`">{{ row.city }}</span>
          <span :class="`${prefixCls}__time`">{{ row.timeText }}</span>
          <span :class="`${prefixCls}__date`">{{ row.dateText }}</span>
        </div>
      </el-col>
    </el-row>
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useNow } from '@vueuse/core'
import { useDesign } from '@/hooks/web/useDesign'
import { Icon } from '@/components/Icon'
import WidgetCard from './WidgetCard.vue'
import { worldClockCities } from '../../data/mockPanels'

defineOptions({ name: 'WorldClockWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-world-clock')

/** 格式化器只构造一次，避免每秒重建 */
const formatters = worldClockCities.map((city) => ({
  ...city,
  time: new Intl.DateTimeFormat('zh-CN', {
    timeZone: city.timeZone,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }),
  date: new Intl.DateTimeFormat('zh-CN', {
    timeZone: city.timeZone,
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  })
}))

const now = useNow({ interval: 1000 })

const rows = computed(() =>
  formatters.map((item) => ({
    city: item.city,
    flag: item.flag,
    timeText: item.time.format(now.value),
    dateText: item.date.format(now.value)
  }))
)
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-world-clock;

.#{$prefix-cls} {
  &__item {
    display: flex;
    padding: 10px 12px;
    margin-bottom: 10px;
    background: rgb(0 0 0 / 3%);
    border-radius: 14px;
    transition: background 0.3s ease;
    gap: 10px;
    align-items: center;

    &:hover {
      background: rgb(0 0 0 / 5.5%);
    }
  }

  &__flag {
    flex: none;
    font-size: 16px;
    line-height: 1;
  }

  &__city {
    flex: none;
    min-width: 3.6em;
    font-size: 13px;
    font-weight: 600;
    color: #1d1d1f;
  }

  &__time {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 15px;
    font-weight: 600;
    color: #1d1d1f;
    text-align: right;
    flex: 1;
    font-variant-numeric: tabular-nums;
  }

  &__date {
    flex: none;
    font-size: 11.5px;
    color: #86868b;
  }
}
</style>
